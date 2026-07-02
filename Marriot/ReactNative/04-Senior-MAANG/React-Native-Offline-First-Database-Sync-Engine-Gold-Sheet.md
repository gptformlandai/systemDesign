# React Native Offline-First Database And Sync Engine - Gold Sheet

> Track Module - Group 4: Senior / MAANG Path
> Level: pro mobile distributed-systems design

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Local database selection | High | Mobile apps need durable local state |
| Offline mutation queue | Very high | Real users lose network constantly |
| Idempotency | Very high | Prevents duplicate writes |
| Conflict resolution | High | Multi-device edits are hard |
| Schema migrations | High | Users keep old app versions |
| Partial sync | Medium-high | Full refresh does not scale |

MAANG signal:
You can explain offline mobile as a distributed system with local truth, remote truth, conflict rules, and durable replay.

---

## 2. Mental Model

Offline-first means the UI reads from local durable storage first.

```text
Screen
  -> local database query
  -> render cached/current state immediately
  -> sync engine fetches remote changes
  -> local database updates
  -> UI reacts to local database changes
  -> pending mutations replay when network allows
```

This is different from "cache a fetch response." A serious offline app has:
- local schema
- mutation queue
- sync cursor
- conflict policy
- retry policy
- migration policy

---

## 3. Storage Choices

| Store | Best For | Avoid For |
|---|---|---|
| AsyncStorage | Small preferences, flags | Large/queryable data, secrets |
| SecureStore/Keychain/Keystore | tokens, small secrets | large records, analytics cache |
| MMKV | fast key-value cache | relational queries, complex sync |
| SQLite/OP-SQLite | durable relational local DB | unstructured blob-only workloads |
| Realm | object database, reactive local data | teams not ready for its model/vendor constraints |
| WatermelonDB | large offline-first RN datasets | small/simple apps |
| Filesystem | images, documents, blobs | primary relational app state |

Rule:
Use a real local database when users must create, edit, search, or sync structured data offline.

---

## 4. Sync State Machine

```text
idle
  -> initial_sync
  -> online_fresh
  -> offline_readonly
  -> offline_with_pending_writes
  -> replaying_queue
  -> conflict_detected
  -> sync_failed_retrying
  -> blocked_requires_user_action
```

User-facing states should be explicit:
- "Saved on this device"
- "Waiting for connection"
- "Syncing"
- "Needs review"
- "Could not sync"

Silent failure destroys trust.

---

## 5. Local Schema Pattern

```ts
type LocalTask = {
  id: string;
  serverId?: string;
  title: string;
  status: 'open' | 'done';
  version: number;
  deletedAt?: string;
  updatedAt: string;
  syncState: 'synced' | 'pending_create' | 'pending_update' | 'pending_delete' | 'conflict';
};

type PendingMutation = {
  id: string;
  entityType: 'task';
  entityId: string;
  operation: 'create' | 'update' | 'delete';
  idempotencyKey: string;
  baseVersion?: number;
  payload: unknown;
  retryCount: number;
  nextAttemptAt: string;
  createdAt: string;
};
```

Design choice:
Keep local entity state and pending mutation state separate. That makes replay, conflict handling, and UI display easier.

---

## 6. Write Flow

```text
User edits task
  -> validate locally
  -> write entity to local DB with pending state
  -> append mutation with idempotency key
  -> update UI immediately from local DB
  -> sync worker sends mutation when online
  -> backend applies once
  -> response maps local id to server id/version
  -> mark entity synced or conflict
```

Important:
The UI should not wait for the network for every edit unless the action is high-risk.

High-risk actions:
- payments
- legal acceptance
- inventory reservation
- destructive account changes
- security-sensitive auth changes

---

## 7. Idempotency

Every replayable mutation needs an idempotency key.

```ts
function createMutationId(userId: string, localId: string, operation: string) {
  return `${userId}:${localId}:${operation}:${crypto.randomUUID()}`;
}
```

Backend behavior:

```text
First request with key:
  apply mutation
  store result by key

Retry with same key:
  return same result
  do not apply mutation again
```

Without idempotency:
- duplicate orders
- duplicate comments
- double like/unlike bugs
- repeated uploads
- inconsistent local/server state

---

## 8. Conflict Resolution

Common policies:

| Policy | Use When | Risk |
|---|---|---|
| Last write wins | Low-value preferences | User changes can disappear |
| Server wins | Compliance/server authority | Offline edits may be rejected |
| Client wins | Single-user local-first data | Can overwrite remote changes |
| Field-level merge | Profile/forms/document metadata | Complex merge logic |
| User review | High-value conflicting edits | More UX work |
| CRDT/OT | Collaborative text/canvas | High complexity |

Version-based conflict:

```text
Client sends baseVersion=7.
Server current version is 9.
Server rejects with conflict payload.
App stores conflict and asks user or applies merge rule.
```

Do not hide data loss behind "synced successfully."

---

## 9. Pull Sync

Naive:

```text
GET /tasks
Replace everything
```

Better:

```text
GET /tasks/changes?cursor=abc
  -> changed records
  -> deleted tombstones
  -> next cursor
```

Need:
- stable sync cursor
- server timestamps or logical versions
- tombstones for deletes
- pagination
- retry-safe fetch
- local transaction around apply

Tombstone:

```ts
type DeletedRecord = {
  id: string;
  deletedAt: string;
  version: number;
};
```

Without tombstones, deleted remote records reappear locally.

---

## 10. Schema Migrations

Mobile migration rules:
- Users may skip many app versions.
- Migrations must be deterministic.
- Never assume a fresh install.
- Test migrations from old production schemas.
- Avoid destructive migrations without backup or server recovery.
- Gate risky migrations by app version and rollout percentage.

Migration checklist:

```text
1. Add new nullable column.
2. Backfill safely.
3. Update app code to use new field.
4. Release and monitor.
5. Later remove old field if safe.
```

OTA warning:
Do not ship JS that expects a new native database module or irreversible schema migration to binaries that cannot support it.

---

## 11. Sync Worker Pseudocode

```ts
async function replayQueue() {
  const pending = await db.pendingMutations.readyToRun();

  for (const mutation of pending) {
    try {
      const result = await api.sendMutation({
        idempotencyKey: mutation.idempotencyKey,
        operation: mutation.operation,
        payload: mutation.payload,
        baseVersion: mutation.baseVersion,
      });

      await db.transaction(async tx => {
        await tx.applyServerResult(mutation, result);
        await tx.removePendingMutation(mutation.id);
      });
    } catch (error) {
      await db.markRetry(mutation.id, {
        retryCount: mutation.retryCount + 1,
        nextAttemptAt: nextBackoffTime(mutation.retryCount),
      });
    }
  }
}
```

Production additions:
- stop on auth failure
- stop on permanent validation error
- isolate corrupt queue items
- report telemetry with error category
- use network/battery/app-state awareness

---

## 12. Observability

Track:
- local DB size
- migration duration/failure
- pending queue length
- oldest pending mutation age
- sync success/failure counts
- conflict count
- retry count distribution
- data corruption reports
- time from local write to server ack

Alert on:
- queue length growing after release
- migration crash spike
- conflict spike
- sync failures by app version/build

---

## 13. Strong Interview Answer

```text
For offline-first React Native, I would make the local database the read path for
the UI and run a sync engine in the background. Writes update local state
immediately and append durable mutations with idempotency keys. The sync worker
replays those mutations, pulls server changes through a cursor, applies tombstones
for deletes, and uses explicit conflict policies based on entity value. I would
instrument queue length, conflict rate, migration failures, and time-to-sync so
we know whether offline is actually healthy in production.
```

---

## 14. Revision Notes

- One-line summary: Offline-first is local durable truth plus a safe sync protocol.
- Three keywords: idempotency, tombstones, conflicts.
- One interview trap: Calling AsyncStorage response caching "offline-first."
- Memory trick: Read local, write queue, sync remote, resolve conflict.
