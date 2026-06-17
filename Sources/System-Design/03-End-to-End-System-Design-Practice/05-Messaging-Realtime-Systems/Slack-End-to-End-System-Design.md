# Slack - End-to-End System Design

> Goal: practice one complete E2E workspace messaging problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for enterprise/workspace collaboration systems.
- Start broad with requirements and scale, then zoom into channels, WebSockets, message queues, ordering, search, notifications, permissions, offline sync, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Slack-style systems, optimize durable searchable history, channel permissions, low-latency realtime updates, integrations, notifications, and workspace-scale collaboration.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Slack system focus |
|---|---|---|
| Problem understanding | Can clarify scope and semantics | workspaces, channels, DMs, threads, files, reactions, search, notifications |
| HLD | Can design collaborative realtime systems | WebSocket gateway, message service, channel service, event log, search index, notification pipeline |
| LLD | Can model maintainable components | `Workspace`, `Channel`, `Message`, `Thread`, `ReadCursor`, `NotificationRule` |
| Machine coding | Can implement critical path | post message, sequence channel, persist, publish, update search/inbox, sync |
| Traffic spikes | Can protect production | incident channels, workspace-wide announcements, integration storms, search bursts |
| Global scale | Can reason across tenants | tenant isolation, channel partitioning, regional workspaces, retention/compliance |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users belong to workspaces.
- Users can create public/private channels.
- Users can send direct messages and channel messages.
- Users can reply in threads.
- Users can react to messages.
- Users can upload/share files through links.
- Users can search message history if retention policy allows.
- Users receive realtime updates while online.
- Offline users can sync missed messages and receive notifications.
- Workspace admins can configure retention, permissions, and integrations if required.

Optional requirements to clarify:

- Are enterprise compliance exports/legal hold in scope?
- Are external shared channels in scope?
- Are bots/apps/webhooks in scope?
- Should search include files and attachments?
- How strict should ordering be across channel messages and thread replies?
- Are voice huddles/calls in scope?

Out of scope unless interviewer asks:

- Full voice/video stack.
- Full app marketplace.
- Full enterprise discovery/legal system.
- Full document collaboration.

## 1.2 Non-Functional Requirements

Collaboration:

- Durable, searchable message history.
- Low-latency realtime delivery for active users.
- Correct channel/workspace permissions.
- Read cursors and unread counts converge quickly.

Reliability:

- Accepted messages must not be lost.
- Realtime fanout is at-least-once.
- Search and notifications can lag.
- Offline sync repairs missed events.

Enterprise:

- Tenant isolation between workspaces.
- Auditability for admin-sensitive actions.
- Retention controls for messages and files.

## 1.3 Constraints

- Channel messages can have many recipients.
- Workspaces vary hugely in size.
- Search indexing is asynchronous and can lag.
- Integrations can create bursty writes.
- Permission checks are more complex than consumer chat.
- Notifications are rule-heavy and user-specific.
- Enterprise retention may conflict with simple deletion semantics.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Organizations/workspaces | millions |
| DAU | 100 million |
| Peak concurrent connections | 30 million |
| Messages/day | 10-50 billion |
| Reactions/day | tens of billions |
| Search queries/day | billions |
| Files/day | hundreds of millions |
| Large channel size | 100K+ members |
| Realtime delivery target | p95 under 500 ms |
| Message API availability | 99.99% |

## 1.5 Capacity Math

Back-of-the-envelope:

- `50B messages/day` is about `580K messages/sec` average globally.
- Large channels amplify one message into many realtime deliveries and unread updates.
- Search indexing every message creates a separate async write pipeline.
- Notification rules can create more work than message persistence.
- Per-workspace tenant isolation helps limit blast radius.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Channel history page | 50-200 messages |
| WebSocket heartbeat | 20-60 seconds |
| Search index lag target | seconds to minutes |
| Notification delay tolerance | seconds |
| Read cursor update interval | immediate or batched |
| Retention policy | days to indefinite depending workspace |

## 1.6 Clarifying Questions To Ask

- Are we designing only messaging or also search and notifications?
- Do private channels and enterprise permissions matter?
- Are external/shared channels in scope?
- Are bot/webhook messages in scope?
- Is message deletion hard delete, soft delete, or retention-governed?
- Do users expect read-after-write in search immediately?

Strong interview framing:

> I will design Slack-style messaging as a durable workspace event system. The message store and channel event log are source of truth; WebSockets deliver realtime updates, search/inbox/notifications are derived pipelines, and offline sync replays missed events by channel cursor.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Post message flow:
Client -> WebSocket/API Gateway -> Message Service
       -> Workspace/Channel permission check
       -> Idempotency -> per-channel sequence
       -> Message Store + Channel Event Stream
       -> Realtime Fanout Workers
       -> Search Index Workers
       -> Notification Workers
       -> Unread/Inbox Workers

Sync flow:
Client reconnects -> Sync Service
                 -> fetch channel events after last cursor
                 -> hydrate messages, reactions, threads
```

Recommended architecture:

```text
Client Apps
  |
  v
+-----------------------+
| Edge + API/WS Gateway |
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| Message Service       |------->| Permission Service   |
| post/edit/delete      |        | workspace/channel    |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Channel Event Stream  |------->| Realtime Fanout      |
| partitioned by channel|        +----------+-----------+
+-----------+-----------+                   |
            |                               v
            v                     +----------------------+
+-----------------------+        | WebSocket Gateways   |
| Message Store         |        +----------------------+
+-----------+-----------+
            |
            +------------------+------------------+
            |                  |                  |
            v                  v                  v
+----------------+   +----------------+   +----------------+
| Search Index   |   | Notification   |   | Unread/Cursor  |
| Workers        |   | Pipeline       |   | Pipeline       |
+----------------+   +----------------+   +----------------+
```

Request flow for post message:

1. Client sends message command with `clientMessageId`.
2. Gateway authenticates user and forwards to Message Service.
3. Permission Service validates workspace membership and channel access.
4. Message Service dedups retry.
5. Sequencer assigns `channelSequence`.
6. Message Store persists message.
7. Channel Event Stream publishes `message.posted`.
8. Realtime fanout sends event to online channel members.
9. Search workers index message.
10. Notification workers evaluate mention/channel/user rules.
11. Offline clients later sync events by cursor.

## 2.2 APIs And Events

### WebSocket Connect

```http
GET /v1/ws/connect?workspaceId=w-1&deviceId=dev-1
Authorization: Bearer <token>
Upgrade: websocket
```

### Post Message

```http
POST /v1/workspaces/{workspaceId}/channels/{channelId}/messages
Authorization: Bearer <token>
```

```json
{
  "clientMessageId": "cmsg-1",
  "text": "Deploy is starting",
  "threadRootMessageId": null,
  "mentions": ["u-2"],
  "blocks": []
}
```

Response:

```json
{
  "messageId": "msg-999",
  "channelId": "ch-1",
  "channelSequence": 10042,
  "createdAt": "2026-06-17T12:00:00Z"
}
```

### Sync Channel

```http
GET /v1/channels/{channelId}/events?afterSequence=10000&limit=200
Authorization: Bearer <token>
```

### Update Read Cursor

```http
PUT /v1/channels/{channelId}/read-cursor
```

```json
{
  "lastReadSequence": 10042,
  "updatedAt": "2026-06-17T12:01:00Z"
}
```

### Search Messages

```http
GET /v1/workspaces/{workspaceId}/search/messages?q=deploy&cursor=...
Authorization: Bearer <token>
```

Important API points:

- Posting needs idempotency.
- Search results must be permission-filtered.
- Sync uses channel sequence cursor.
- Read cursors are monotonic per user/channel.
- Notification delivery is async and rule-based.

## 2.3 Core Components

Think of Slack as six connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Workspace plane | tenants, members, roles, policies | permission correctness |
| Message plane | post/edit/delete, sequence, persist | durable channel history |
| Realtime plane | WebSocket fanout, presence, typing | low-latency collaboration |
| Projection plane | unread counts, channel lists, inbox | fast user navigation |
| Search plane | indexing and permission-filtered search | discover historical knowledge |
| Notification plane | mentions, keywords, push/email | alert users without overwhelming them |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Gateway | API/WS sessions and routing | message truth | connections/QPS |
| Message Service | post/edit/delete workflow | search ranking | message command QPS |
| Permission Service | workspace/channel authorization | message storage | authz QPS |
| Channel Service | channel metadata and members | WebSocket sockets | membership operations |
| Message Store | canonical messages | unread counts | storage/read/write volume |
| Event Stream | ordered channel events | permission decisions | partitions |
| Fanout Workers | realtime delivery | canonical persistence | online member count |
| Search Workers | indexing | realtime delivery | indexing lag |
| Notification Service | mention/rule evaluation | message sequencing | notifications |
| Cursor Service | read state/unread count | message body truth | cursor writes |

### Workspace And Channel Permissions

Why it exists:

- Slack-style systems are tenant-scoped.
- Private channels, guests, shared channels, and admin policies affect access.

Core responsibilities:

- Validate workspace membership.
- Validate channel membership/visibility.
- Enforce posting permissions.
- Enforce file-sharing rules.
- Provide permission filters to search.

Failure behavior:

- Fail closed on private channel reads/writes.
- Use short-lived permission cache with invalidation.
- Audit permission-sensitive admin changes.

Interview signal:

> Permission checks happen before message write and again when reading/searching.

### Message Service

Core responsibilities:

- Validate command and permissions.
- Dedup by `(userId, clientMessageId)`.
- Assign per-channel sequence.
- Persist canonical message.
- Publish channel event.
- Support edits/deletes as events rather than rewriting history blindly.

Failure behavior:

- Persist succeeds but event publish fails: outbox republishes.
- Duplicate retry returns existing message.
- Search indexing failure does not roll back message.

### Event Log And Offline Sync

Why it exists:

- Online delivery is not enough.
- Clients need to replay missed messages, edits, deletes, reactions, thread replies, and cursor changes.

Core responsibilities:

- Maintain ordered events per channel.
- Support `fetch after sequence`.
- Allow clients to reconcile local state.
- Feed fanout, search, notifications, and unread projections.

Important distinction:

- Message Store stores canonical message objects.
- Channel Event Stream stores ordered changes.
- Client sync applies events and hydrates messages when needed.

### Search Pipeline

Why it exists:

- Slack is a knowledge base, not just realtime chat.
- Search must respect workspace/channel permissions and retention.

Core responsibilities:

- Consume message events.
- Tokenize/index text and metadata.
- Index files/attachments if in scope.
- Support permission-aware filtering at query time or index time.
- Remove/update indexed messages according to deletion/retention policy.

Failure behavior:

- Search can lag behind message post.
- If search index is down, messaging still works.
- Reindex from message store/event log.

### Notification Pipeline

Why it exists:

- Users should be alerted for mentions, DMs, keywords, threads, and watched channels.
- Notification rules are user/workspace-specific.

Core responsibilities:

- Evaluate mentions and notification settings.
- Suppress notifications for muted channels or active sessions.
- Send push/email/desktop notifications.
- Batch notifications during bursts.

Failure behavior:

- Notification delay does not affect message truth.
- Push provider failure retries independently.
- Avoid duplicate notifications using event IDs.

### Integrations And Bots

Why they matter:

- Bots and webhooks can generate bursty messages.
- Integrations often need rate limits and permission scopes.

Strategy:

- Treat bot messages as message commands from app principals.
- Enforce workspace-installed app permissions.
- Rate-limit per app/workspace/channel.
- Use async retry for outgoing webhooks/events.

## 2.4 Data Layer

### Core Data Models

Workspace:

```json
{
  "workspaceId": "w-1",
  "name": "Acme",
  "plan": "ENTERPRISE",
  "retentionPolicy": "365_DAYS"
}
```

Channel:

```json
{
  "channelId": "ch-1",
  "workspaceId": "w-1",
  "type": "PRIVATE",
  "name": "incident-prod",
  "state": "ACTIVE"
}
```

Message:

```json
{
  "messageId": "msg-999",
  "workspaceId": "w-1",
  "channelId": "ch-1",
  "senderId": "u-1",
  "channelSequence": 10042,
  "text": "Deploy is starting",
  "threadRootMessageId": null,
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Read cursor:

```json
{
  "workspaceId": "w-1",
  "channelId": "ch-1",
  "userId": "u-2",
  "lastReadSequence": 10042,
  "updatedAt": "2026-06-17T12:01:00Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Workspaces/channels | relational/document DB | structured tenant metadata |
| Channel membership | relational/KV | authorization checks |
| Messages | wide-column/append store | ordered channel history |
| Event stream | Kafka/Pulsar-style stream | ordered replay and projections |
| Read cursors | KV/wide-column | user/channel progress |
| Search index | search engine | full-text search |
| Files | object storage + CDN | large immutable blobs |
| Notifications | queue + state store | async delivery and dedup |

Relational-style tables:

```sql
workspaces(workspace_id PK, name, plan, retention_policy)
channels(channel_id PK, workspace_id, name, type, state)
channel_members(channel_id, user_id, role, state, joined_at)
messages(channel_id, channel_sequence, message_id, sender_id, text, created_at)
read_cursors(channel_id, user_id, last_read_sequence, updated_at)
reactions(message_id, user_id, reaction_type, updated_at)
```

Important indexes:

- `messages(channel_id, channel_sequence)` for history/sync.
- `channel_members(channel_id, user_id)` for authorization.
- `read_cursors(user_id, channel_id)` for unread state.
- Search index by workspace with permission filters.

### Partitioning

- Partition messages/events by `channelId`.
- Partition workspace metadata by `workspaceId`.
- Partition read cursors by `userId` or `channelId`.
- Partition search indexes by workspace/tenant or workspace groups.
- Isolate very large enterprise tenants/cells.

### Replication And Consistency

- Message commit and permission check need strong-enough correctness.
- Search, notifications, unread counts, and analytics can be eventually consistent.
- Retention/deletion must eventually propagate to stores and indexes.
- Cross-region workspaces may use home region for channel ordering.

## 2.5 Scalability

### Horizontal Scaling

- Gateway fleet scales by active sockets.
- Message Service scales by post/edit/delete QPS.
- Event stream scales by channel partitions.
- Search indexing scales by stream lag.
- Notification workers scale by mention/rule evaluation volume.
- Cursor Service scales by read updates.

### Large Channel Strategy

- Do not synchronously fan out expensive per-user writes.
- Use event stream plus client pull for very large channels.
- Batch unread count updates.
- Suppress broad notifications unless explicitly mentioned.
- Use channel-level backpressure for integration storms.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Post message permission check | 10-50 ms |
| Idempotency + sequence | 5-30 ms |
| Persist + stream append | 20-100 ms |
| Realtime fanout | 50-300 ms |
| Search indexing | seconds to minutes |
| Notification send | seconds-scale acceptable |

### Optimization Rules

- Cache channel membership with invalidation.
- Keep post path separate from search/notification work.
- Store recent channel heads in cache.
- Batch read cursor writes.
- Use pagination for history and search.

## 2.7 Async Systems

Use streams for:

- `message.posted`
- `message.edited`
- `message.deleted`
- `reaction.added`
- `thread.reply.posted`
- `channel.read`
- `search.index.requested`
- `notification.requested`
- `app.event`

Queue notes:

- Consumers must be idempotent.
- Event order matters per channel.
- Search/notification/unread projections are replayable.
- DLQ poison events and support replay from offset.

## 2.8 Security, Privacy, And Compliance

Security:

- Authentication for API and WebSocket.
- Workspace/channel authorization on reads and writes.
- App/bot scopes for integrations.
- Signed file upload/download URLs.
- Encryption in transit and at rest.

Privacy/compliance:

- Private channel membership must not leak.
- Retention policies apply to messages, files, and search index.
- Admin/audit logs for sensitive actions.
- Legal hold may override user deletion if in scope.
- Search results must be permission-filtered.

Abuse controls:

- Rate-limit bot/webhook messages.
- Detect spammy app integrations.
- Prevent mention abuse in large channels.
- Protect search API from scraping.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Messaging | post success rate, ack latency, duplicate rate |
| Realtime | fanout latency, gateway reconnects, active sockets |
| Permissions | authz latency, denied/allowed mismatch |
| Search | indexing lag, query latency, stale result rate |
| Notifications | notification lag, duplicate notification rate |
| Cursors | unread staleness, cursor write latency |
| Integrations | app rate-limit hits, webhook retry backlog |

Alerts:

- Message post errors increase.
- Channel event stream lag grows.
- Search indexing lag exceeds SLO.
- Notification pipeline backlog grows.
- Permission service errors rise.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Realtime | push to all members | event pull for large channels | lower latency vs fanout cost |
| History | message store source of truth | event log source only | simpler reads vs replay flexibility |
| Search | async indexing | inline indexing | write latency vs search freshness |
| Notifications | evaluate all inline | async rule pipeline | correctness latency vs post latency |
| Permissions | cache membership | live DB check | latency vs freshness |
| Retention | soft delete + purge jobs | immediate hard delete | audit/recovery vs complexity |

Interview framing:

> I would treat Slack as a durable channel event system. Posting a message commits to the message store and event stream; realtime, search, notifications, unread counts, and clients are downstream consumers.

---

# 3. Low-Level Design

LLD goal:

> Model Slack around workspaces, channels, memberships, messages, threads, read cursors, notification rules, and event projections.

Simple rules:

- Channel membership controls access.
- Messages are immutable-ish records; edits/deletes are events.
- Realtime delivery is a projection of the event stream.
- Search and notifications are async consumers.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Workspace` | tenant config and policies | users cannot cross tenants without explicit sharing |
| `Channel` | name/type/members | private channel requires membership |
| `ChannelMember` | role/state | removed members cannot read new messages |
| `Message` | body, sender, sequence | sequence is immutable |
| `Thread` | root message and replies | replies belong to root/channel |
| `ReadCursor` | user/channel progress | monotonic |
| `NotificationRule` | mention/mute/keyword settings | evaluated asynchronously |
| `AppIntegration` | bot/webhook scopes | cannot exceed installed permissions |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `MessageService` | post/edit/delete | serve search ranking |
| `PermissionService` | workspace/channel authz | persist messages |
| `ChannelService` | channel metadata/membership | deliver WebSocket events directly |
| `SearchIndexer` | consume and index messages | block message post |
| `NotificationService` | evaluate notification rules | assign channel sequence |
| `SyncService` | fetch events by cursor | mutate channel membership |

## 3.2 OOP Fundamentals

Encapsulation:

- `Channel` owns membership visibility rules.
- `ReadCursor` owns monotonic update.
- `NotificationRule` owns whether an event should notify.

Abstraction:

- `MessageRepository` hides storage.
- `SearchIndex` hides search engine.
- `EventPublisher` hides stream.

Polymorphism:

- Different channel types: public, private, DM, shared.
- Different notification rules: mention, keyword, thread, mute.

Composition:

- `MessageService` composes permission service, idempotency store, sequencer, repository, and publisher.

## 3.3 SOLID Principles

| Principle | Slack application |
|---|---|
| Single Responsibility | `SearchIndexer` only indexes/searches |
| Open/Closed | add channel type without rewriting message storage |
| Liskov Substitution | any `PermissionChecker` preserves allow/deny contract |
| Interface Segregation | separate message, search, channel, notification APIs |
| Dependency Inversion | core services depend on repositories and ports |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Observer/Event Publisher | message events to projections | decouple consumers |
| Strategy | notification rule evaluation | different rules per user/channel |
| Command | message post/edit/delete | idempotent mutation handling |
| State | channel member lifecycle | active, invited, removed |
| Decorator | rate limits/audit logging | enterprise cross-cutting concerns |

## 3.5 UML / Diagrams

### Post Message Sequence

```text
Client -> Gateway: POST_MESSAGE
Gateway -> MessageService: post(command)
MessageService -> PermissionService: canPost(user, channel)
MessageService -> IdempotencyStore: check
MessageService -> Sequencer: next(channelId)
MessageService -> MessageRepository: save
MessageService -> EventStream: publish message.posted
EventStream -> FanoutWorker: realtime update
EventStream -> SearchIndexer: index
EventStream -> NotificationWorker: evaluate rules
```

### Offline Sync Sequence

```text
Client -> SyncService: sync(channelId, afterSequence)
SyncService -> PermissionService: canRead
SyncService -> EventStore: fetch events
SyncService -> MessageRepository: hydrate messages
SyncService -> Client: events/messages
```

## 3.6 Class Design

Interfaces:

```java
interface PermissionService {
    boolean canPost(String userId, String channelId);
    boolean canRead(String userId, String channelId);
}

interface MessageRepository {
    void save(Message message);
    List<Message> fetchAfter(String channelId, long afterSequence, int limit);
}

interface ChannelSequencer {
    long nextSequence(String channelId);
}

interface NotificationRule {
    boolean shouldNotify(MessageEvent event, UserNotificationSettings settings);
}
```

Design notes:

- `canRead()` must also be used for search result filtering.
- Edits/deletes can publish events that update message store and search index.
- Notification rules should be idempotent by event ID and recipient ID.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| duplicate post retry | return same message/sequence |
| user removed from private channel | permission check blocks future reads/sync |
| search index lags | message is still visible in channel history |
| integration posts too fast | app/workspace/channel rate limits |
| read cursor update arrives old | monotonic max sequence |
| large channel announcement | async fanout and notification suppression rules |
| retention policy deletes message | tombstone message and remove from search/files |
| WebSocket disconnected | sync events after cursor |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
slack/
  domain/
    Workspace.java
    Channel.java
    Message.java
    ReadCursor.java
    NotificationRule.java
  service/
    MessageService.java
    PermissionService.java
    SyncService.java
    NotificationService.java
  port/
    MessageRepository.java
    ChannelRepository.java
    EventPublisher.java
    CursorRepository.java
  adapter/
    InMemoryMessageRepository.java
    InMemoryChannelRepository.java
  app/
    SlackDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from collections import defaultdict
from dataclasses import dataclass
from threading import Lock


@dataclass(frozen=True)
class Message:
    message_id: str
    channel_id: str
    sender_id: str
    sequence: int
    text: str


class InMemorySlack:
    def __init__(self) -> None:
        self.members: dict[str, set[str]] = defaultdict(set)
        self.messages: dict[str, list[Message]] = defaultdict(list)
        self.cursors: dict[tuple[str, str], int] = defaultdict(int)
        self.idempotency: dict[tuple[str, str], str] = {}
        self.sequence: dict[str, int] = defaultdict(int)
        self.lock = Lock()

    def join_channel(self, channel_id: str, user_id: str) -> None:
        with self.lock:
            self.members[channel_id].add(user_id)

    def post_message(self, channel_id: str, sender_id: str, client_message_id: str, text: str) -> Message:
        key = (sender_id, client_message_id)
        with self.lock:
            if sender_id not in self.members[channel_id]:
                raise ValueError("user cannot post to channel")
            if key in self.idempotency:
                msg_id = self.idempotency[key]
                return next(m for m in self.messages[channel_id] if m.message_id == msg_id)
            self.sequence[channel_id] += 1
            seq = self.sequence[channel_id]
            msg = Message(f"msg-{channel_id}-{seq}", channel_id, sender_id, seq, text)
            self.messages[channel_id].append(msg)
            self.idempotency[key] = msg.message_id
            return msg

    def sync(self, channel_id: str, user_id: str, after_sequence: int) -> list[Message]:
        with self.lock:
            if user_id not in self.members[channel_id]:
                raise ValueError("user cannot read channel")
            return [m for m in self.messages[channel_id] if m.sequence > after_sequence]

    def mark_read(self, channel_id: str, user_id: str, sequence: int) -> None:
        with self.lock:
            key = (channel_id, user_id)
            self.cursors[key] = max(self.cursors[key], sequence)


app = InMemorySlack()
app.join_channel("ch-1", "u1")
app.join_channel("ch-1", "u2")
msg = app.post_message("ch-1", "u1", "cmsg-1", "Deploy started")
app.mark_read("ch-1", "u2", msg.sequence)
print(app.sync("ch-1", "u2", 0)[0].text)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[channelId -> list[Message]]` | channel history |
| `dict[channelId -> set[userId]]` | membership |
| `dict[(channel,user) -> sequence]` | read cursors |
| `dict[(user,clientMessageId) -> messageId]` | idempotency |
| `queue[ChannelEvent]` | async projections |

## 4.4 Concurrency

High-signal concurrency issues:

- Concurrent posts in same channel.
- Duplicate retries.
- Cursor updates racing from multiple devices.
- Permission changes racing with sync/search.

Handling strategy:

- Per-channel sequencer.
- Idempotency key.
- Monotonic cursor updates.
- Permission check on every read/search.

## 4.5 Testing Thinking

Unit tests:

- Non-member cannot post/read private channel.
- Duplicate post returns same message.
- Channel sequence is monotonic.
- Sync returns messages after cursor.
- Read cursor never moves backward.

Load tests:

- Large channel fanout.
- Search indexing backlog.
- Integration/webhook storm.
- Workspace incident traffic spike.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Incident channel spike | many users post/read at once | hot channel partition |
| Workspace-wide announcement | large fanout/notifications | notification storm |
| Integration storm | webhook loop | message and queue overload |
| Search burst | outage investigation | search cluster pressure |
| Reconnect storm | desktop/mobile clients reconnect | gateway pressure |

## 5.2 Immediate Spike Response

1. Protect message post and channel history.
2. Rate-limit integrations and noisy channels.
3. Degrade typing/presence and non-critical reactions.
4. Batch notification and cursor updates.
5. Autoscale search/index workers by lag.
6. Use pull-based sync for huge channels.
7. Isolate tenant/cell blast radius.

## 5.3 Degradation Policy

Protect in this order:

1. Permission-correct message posting.
2. Channel history/sync.
3. Realtime fanout.
4. Search freshness.
5. Notifications.
6. Presence/typing/reaction animations.

Not allowed:

- Leak private channel messages.
- Lose accepted messages.
- Corrupt channel order.
- Ignore retention/compliance policies.

## 5.4 Spike Interview Answer

> During spikes I protect channel history and permissions first. Realtime delivery, search indexing, notifications, and unread projections are downstream and can lag or degrade. Large channels use async fanout and pull-based sync to avoid per-user write explosions.

---

# 6. Scaling To Global Workspaces

## 6.1 Global Architecture

```text
Global routing
  -> workspace/cell assigned region
  -> WebSocket/API gateway fleet
  -> channel-partitioned event streams
  -> message store and projections
  -> search/notification/cursor pipelines
```

## 6.2 Multi-Region Strategy

- Assign workspaces to cells/regions for tenant isolation.
- Keep channel sequencing in one owner shard/region.
- Replicate message history for DR/read locality if needed.
- Keep search indexes per workspace/cell.
- Support regional failover with reconnect and cursor sync.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Gateway | scale by connections and workspace routing |
| Message | stateless service + per-channel sequencer |
| Store | channel-sharded append storage |
| Stream | partition by channel |
| Search | workspace/cell-sharded index |
| Notifications | async workers and suppression |
| Cursors | user/channel partitioned KV |
| Files | object storage + CDN |

## 6.4 Global Interview Answer

> I would scale Slack by treating each workspace as a tenant and each channel as an ordered event stream. Message history is durable and permission-checked; realtime delivery, search, notifications, and unread state are projections that can replay from the event log.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

Use this answer flow:

```text
I will clarify workspaces, channels, private access, threads, search, files, notifications, integrations, and retention.
I will estimate messages/sec, channel fanout, search index volume, notification volume, and concurrent sockets.
HLD includes gateway, message service, permission service, channel service, message store, event stream, fanout, search, notifications, cursors, and files.
I guarantee per-channel ordering, idempotent posts, and permission-correct reads/writes.
Search, notifications, unread counts, and realtime delivery are async projections.
For spikes, I protect message history and permissions, then degrade search freshness, notifications, presence, and typing.
For global scale, I use workspace/cell isolation, channel partitioning, and replayable event streams.
```

---

# 8. Fast Recall Rules

- Slack is durable workspace history, not just chat.
- Permissions are checked on post, read, sync, and search.
- Channel sequence gives practical ordering.
- Event stream feeds realtime, search, notifications, unread counts.
- Search can lag; message history remains truth.
- Read cursors are monotonic per user/channel.
- Integrations need strict rate limits.
- Large channels should avoid synchronous per-user writes.
- Tenant/cell isolation limits blast radius.
- Retention/compliance changes storage and deletion semantics.
