# Chat System (WebSockets at Scale) - End-to-End System Design

> Goal: practice one complete E2E problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and billion-user scale.

---

## How To Use This File

- Treat this as the repeatable pattern for every E2E problem.
- Start broad with requirements and scale, then zoom into architecture, APIs, data, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Chat specifically, optimize low-latency message delivery, connection scalability, and reliability semantics.

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

| Layer | Interview signal | Chat system focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | 1:1 chat, group chat, delivery/read semantics, online presence, media links |
| HLD | Can design scalable systems | WebSocket gateway, message service, queue/stream, presence service, push fallback |
| LLD | Can model maintainable components | `Conversation`, `Message`, `DeliveryReceipt`, `PresenceState`, `MessageRouter` |
| Machine coding | Can implement critical path | connect, send message, persist, fanout, ack, read receipt |
| Traffic spikes | Can protect production | connection storms, viral group bursts, reconnect floods, backpressure |
| Billion users | Can reason at global scale | region routing, sticky sessions, partitioned conversations, async fanout, offline sync |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can send and receive messages in near real time.
- Support one-to-one chats and group chats.
- Support message states: sent, delivered, read.
- Preserve message history and pagination.
- Show user presence (online/last seen) with privacy controls.
- Support attachments via media links (actual blob storage may be external).
- Support push notifications for offline users.

Optional requirements to clarify:

- Should we support edit/delete message and recall windows?
- Do we need end-to-end encryption or only transport encryption?
- Are reactions, typing indicators, and replies in scope?
- Is cross-device sync required with same account open on many devices?
- Is strict ordering needed globally, per conversation, or per sender?

Out of scope unless interviewer asks:

- Full media transcoding pipeline.
- Full spam moderation and trust/risk engine internals.
- Full key management internals for E2EE.

## 1.2 Non-Functional Requirements

Message path:

- Low end-to-end latency for message delivery.
- High availability for always-on communication.
- Predictable behavior under intermittent mobile networks.
- At-least-once or effectively-once delivery with dedup support.

Connection layer:

- Massive concurrent WebSocket connections.
- Efficient heartbeat and presence updates.
- Fast reconnect behavior after network transitions.

Storage and reliability:

- Durable message persistence.
- Bounded ordering guarantees (per conversation shard).
- Eventually consistent read receipts can be acceptable.

## 1.3 Constraints

- Mobile networks are unstable, so reconnect and duplicate sends are common.
- Group chat fanout can be expensive for large groups.
- Exactly-once delivery is hard in distributed systems.
- Presence updates are high-volume and ephemeral.
- Long-lived connections increase gateway memory and file descriptor pressure.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Registered users | 1 billion |
| DAU | 400 million |
| Peak concurrent connections | 80 million |
| Messages/day | 200 billion |
| Avg message payload | 200 bytes text metadata-only |
| Groups | up to millions of groups, some very large |
| P95 delivery target | under 300 ms within region |
| Availability target | 99.99% chat send/receive API |

Back-of-the-envelope:

- `200B messages/day` is about `2.3M messages/sec` average globally.
- Peak can be 5x to 10x during events.
- If one gateway handles `100K` sockets, tens to hundreds of gateway nodes are needed per region at peak.
- Metadata-only message storage at this scale is massive; retention policy and tiering are required.

## 1.5 Clarifying Questions To Ask

- Are ordering guarantees required per conversation?
- What are delivery semantics: at-most-once, at-least-once, or effectively-once with idempotency?
- Is multi-device sync mandatory and how many devices per user?
- How long should message history be retained?
- Should offline messages be pushed immediately on reconnect?
- Are very large groups handled like channels with different semantics?

Strong interview framing:

> I will design Chat with persistent WebSocket connections for real-time delivery, durable message storage, conversation-partitioned routing for ordering, idempotent client message IDs for deduplication, and push fallback for offline users.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Realtime message flow:
Client
  -> WebSocket Gateway (sticky)
  -> Auth/session validation
  -> Message Service
  -> Conversation-partitioned Stream
  -> Message Store (durable)
  -> Fanout Workers
  -> Recipient Gateway sessions
  -> Ack path (delivered/read)

Offline flow:
Message Service
  -> Notification Service
  -> APNS/FCM push
  -> Client reconnect
  -> Offline sync API
```

Recommended architecture:

```text
Client Apps
  |
  v
+-----------------------+
| Edge + LB + WAF       |
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| WebSocket Gateway     |<------>| Presence Service     |
| (connection manager)  |        | (ephemeral state)    |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Message Service       |------->| Idempotency Store    |
| (send/ack logic)      |        +----------------------+
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| Event Stream          |------->| Fanout Workers       |
| (partitioned by conv) |        +----------+-----------+
+-----------+-----------+                   |
            |                               v
            v                     +----------------------+
+-----------------------+        | Notification Service |
| Message Store         |        | (offline push)       |
| (durable history)     |        +----------------------+
+-----------------------+
```

Request flow for send message:

1. Client sends `SEND_MESSAGE` over WebSocket with `clientMessageId`.
2. Gateway validates auth/session and forwards to Message Service.
3. Message Service checks idempotency key.
4. Message is persisted and appended to conversation stream partition.
5. Fanout workers deliver to active recipient sessions.
6. Offline recipients receive push notification trigger.
7. Sender receives server ack with canonical `messageId` and sequence.

## 2.2 APIs

### WebSocket Connect

```http
GET /v1/ws/connect?token=<jwt>&deviceId=<id>
Upgrade: websocket
```

### WebSocket Send Event

```json
{
  "type": "SEND_MESSAGE",
  "clientMessageId": "cmsg-91f2",
  "conversationId": "conv-123",
  "senderId": "u-11",
  "body": {"text": "hello"},
  "sentAt": "2026-06-17T12:00:00Z"
}
```

### Server Ack Event

```json
{
  "type": "MESSAGE_ACK",
  "clientMessageId": "cmsg-91f2",
  "messageId": "msg-88421",
  "conversationId": "conv-123",
  "sequence": 908122,
  "status": "PERSISTED"
}
```

### Offline Sync API

```http
GET /v1/conversations/{conversationId}/messages?cursor=...&limit=50
Authorization: Bearer <token>
```

### Read Receipt Event

```json
{
  "type": "READ_RECEIPT",
  "conversationId": "conv-123",
  "messageId": "msg-88421",
  "readerId": "u-42",
  "readAt": "2026-06-17T12:01:30Z"
}
```

Important points:

- Use client-generated idempotency IDs for retry safety.
- Keep WebSocket payloads small and versioned.
- Support pull-based history even with realtime push delivery.

## 2.3 Core Components

Think of Chat as three connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Connection plane | WebSocket sessions, heartbeats, gateway routing | keep millions of clients connected efficiently |
| Message plane | validation, idempotency, sequencing, persistence, fanout | deliver durable ordered messages |
| Ephemeral/async plane | presence, typing, receipts, push notifications, analytics | enrich chat without weakening core send |

The most important separation: WebSocket gateways manage connections, but Message Service owns message correctness. A gateway should not be the source of truth for messages.

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Edge/LB | regional routing, TLS, WAF, connection admission | message persistence | connection attempts and regions |
| WebSocket Gateway | socket lifecycle, heartbeat, session routing | canonical message history | concurrent sockets |
| Session Registry | `userId/deviceId -> gateway session` mapping | message bodies | session churn |
| Message Service | authz, idempotency, sequence, persist, publish | raw socket management | send QPS |
| Conversation Service | membership, roles, conversation metadata | delivery transport | membership operations |
| Sequencer | per-conversation sequence assignment | message fanout | hot conversation QPS |
| Event Stream | durable ordered message events | business validation | message volume and partitions |
| Fanout Workers | deliver persisted events to online sessions | create canonical messages | queue lag and recipient count |
| Presence Service | online/last-seen state | message durability | heartbeat/update volume |
| Receipt Service | delivered/read progress | message content | receipt update QPS |
| Notification Service | offline APNS/FCM fallback | in-app realtime delivery | offline notification volume |

### Edge / Load Balancer Layer

Why it exists:

- Chat clients keep long-lived connections.
- Connection storms can happen after app restart, network flap, or regional recovery.
- Users should connect to a nearby healthy region.

Core responsibilities:

- Route clients to the closest healthy gateway cluster.
- Terminate TLS or pass through to gateway depending infrastructure.
- Apply DDoS/WAF controls.
- Support connection admission control during reconnect storms.
- Support draining during deployments.

Sticky routing note:

- A WebSocket connection is naturally sticky to one gateway while it is open.
- After reconnect, the client may land on a different gateway, so session state must be stored outside the gateway or be cheaply reconstructable.

Failure behavior:

- If a gateway node drains, clients reconnect with jitter.
- If a region fails, clients reconnect to another region and repair gaps using offline sync.

Interview signal:

> Edge/LB protects connection establishment and region routing, but durable chat state lives behind the gateways.

### WebSocket Gateway

Why it exists:

- WebSockets are long-lived, high-count connections.
- Application business services should not directly manage millions of sockets.

Core responsibilities:

- Accept WebSocket upgrades after auth/session validation.
- Maintain connection lifecycle: connect, heartbeat, idle timeout, disconnect.
- Track local session IDs and device IDs.
- Forward client events to backend services.
- Push outbound events from Fanout Workers to connected clients.
- Apply per-connection and per-user send limits.

What it should avoid:

- Do not assign canonical message sequence by itself.
- Do not store durable message history.
- Do not treat local memory session map as global truth.

Scaling notes:

- Scale by concurrent socket count and outbound events/sec.
- Use event-driven I/O.
- Keep per-connection memory small.
- Tune heartbeat intervals to avoid unnecessary mobile/network cost.

Failure behavior:

- Gateway crash disconnects local clients, but messages remain durable in Message Store/Event Stream.
- Client reconnects and calls sync API to fetch missed messages.

Interview signal:

> WebSocket Gateway is a connection manager and event pipe, not the message source of truth.

### Session Registry

Why it exists:

- Fanout workers need to know which gateway currently owns a user's active sessions.
- Users may have multiple devices connected at once.

Core responsibilities:

- Store mapping: `userId -> sessionId/deviceId/gatewayId`.
- Expire sessions when heartbeat is missed.
- Support multi-device delivery.
- Remove stale sessions on disconnect or TTL expiry.

Scaling notes:

- Keep session records ephemeral in Redis/KV/in-memory cluster.
- Partition by user ID.
- Use TTL so stale sessions disappear automatically.

Failure behavior:

- If registry is stale, delivery attempt may fail; client sync repairs missed messages.
- If registry is down, send path should still persist messages; realtime fanout may degrade.

Interview signal:

> Session Registry is ephemeral routing metadata. It can be stale; message durability cannot be.

### Conversation Service

Why it exists:

- Every send/read operation depends on conversation membership and roles.
- Authorization cannot be inferred from socket presence.

Core responsibilities:

- Create 1:1 and group conversations.
- Store members, roles, join/leave state, mute settings.
- Validate whether sender can send and recipient can read.
- Handle group membership changes and permissions.

Failure behavior:

- If membership cannot be verified, fail closed for private conversations.
- If membership cache is stale, read from durable membership store for sensitive actions.

Interview signal:

> Conversation Service answers who belongs in a conversation; Message Service uses that before accepting a message.

### Message Service

Why it exists:

- It owns the correctness of send-message workflow.
- It turns client retries into one canonical durable message.

Core responsibilities:

- Validate sender authorization through Conversation Service.
- Check idempotency by `(senderId, clientMessageId)`.
- Assign per-conversation sequence.
- Persist message durably.
- Publish `message.created` event to stream.
- Return ack with canonical `messageId` and `sequence`.

Send path:

```text
Gateway -> Message Service -> membership check -> idempotency check -> sequence -> persist -> publish -> ack
```

Failure behavior:

- Client retries with same `clientMessageId`: return same canonical ack.
- Persist succeeds but ack lost: retry still returns the saved message.
- Stream publish fails after persist: use outbox/retry so fanout eventually happens.

Interview signal:

> Message Service guarantees durable persistence, per-conversation ordering, and retry-safe send semantics.

### Sequencer

Why it exists:

- Chat needs practical ordering inside a conversation.
- Global ordering across all chats is unnecessary and too expensive.

Core responsibilities:

- Assign monotonic sequence numbers per conversation.
- Preserve ordering for messages routed to the same conversation partition.
- Support cursor-based history fetch by sequence.

Scaling notes:

- Partition by conversation ID.
- Very hot group conversations may need special partitioning or channel semantics.
- Avoid a single global sequence generator.

Failure behavior:

- If sequence assignment fails, do not persist message as accepted.
- If duplicate send arrives, do not assign a new sequence; return existing one.

Interview signal:

> Chat ordering should be scoped to a conversation, not the whole system.

### Event Stream and Fanout Workers

Why they exist:

- Message persistence and delivery fanout should be decoupled.
- Recipients can be online, offline, on multiple devices, or in large groups.

Core responsibilities:

- Event Stream stores ordered `message.created` events by conversation partition.
- Fanout Workers consume events and deliver to active recipient sessions.
- Workers retry delivery idempotently.
- Offline users trigger Notification Service and later sync.
- Large groups can use pull-based catch-up rather than direct push to every user.

Fanout strategy:

| Conversation type | Delivery strategy |
|---|---|
| 1:1 | direct online fanout + offline push |
| small group | fanout to active sessions |
| large group/channel | publish event, clients pull/catch up by cursor |

Failure behavior:

- Worker crash: resume from stream offset.
- Gateway unavailable: mark recipient offline/stale and rely on sync.
- Duplicate delivery: client/server dedup by `messageId`.

Interview signal:

> Fanout is asynchronous and replayable. Durable message persistence happens before delivery attempts.

### Presence Service

Why it exists:

- Users expect online/last-seen indicators.
- Presence is high-churn and ephemeral, unlike message history.

Core responsibilities:

- Track online/offline/last-seen by user and device.
- Consume gateway heartbeat/connect/disconnect events.
- Apply privacy rules.
- Throttle presence broadcasts to avoid storms.

Important boundary:

- Presence should not block message send.
- Presence can be stale for a short time.

Failure behavior:

- Presence down: chat still works; show stale/unknown presence.
- Heartbeat storm: batch/throttle updates.

Interview signal:

> Presence is useful but not critical. Durable chat should not depend on fresh presence.

### Receipt Service

Why it exists:

- Delivery/read receipts are separate state from messages.
- Receipts are high-volume and can be eventually consistent.

Core responsibilities:

- Track last delivered sequence per user/device.
- Track last read sequence per user/conversation.
- Apply monotonic updates: never move read progress backward.
- Batch receipt writes under load.

Failure behavior:

- Receipt update lost: client can resend latest read position.
- Multiple devices race: store max sequence.
- Receipt pipeline lag: messages still send and read correctly.

Interview signal:

> Receipts are progress pointers, not message truth. They should be monotonic and retry-safe.

### Notification Service

Why it exists:

- Offline users still need push notifications.
- Push providers have different latency, limits, and failure modes.

Core responsibilities:

- Trigger APNS/FCM push for offline recipients.
- Collapse/batch notifications to reduce spam.
- Respect mute/notification preferences.
- Retry provider failures without blocking send path.

Failure behavior:

- Push provider down: queue retry and rely on offline sync when user opens app.
- User muted conversation: suppress push but keep message durable.

Interview signal:

> Push notification is an offline hint, not the source of message delivery truth.

### Message Store and Offline Sync

Why they exist:

- Clients disconnect often.
- Realtime delivery can fail, but history must repair gaps.

Core responsibilities:

- Store messages by conversation and sequence.
- Support cursor-based pagination.
- Support reconnect sync: `fetch messages after lastSeenSequence`.
- Retain history based on product/compliance policy.

Failure behavior:

- Fanout missed: client sync catches up.
- Client sees out-of-order events: sort/apply by sequence.
- Region failover: reconnect and fetch from replicated store.

Interview signal:

> Offline sync is the repair mechanism that makes realtime delivery reliable in practice.

### Idempotency Store

Why it exists:

- Mobile clients retry sends after timeouts or reconnects.
- Without idempotency, retries create duplicate messages.

Core responsibilities:

- Map `(senderId, clientMessageId)` to canonical `messageId`.
- Keep TTL aligned with retry window.
- Return existing ack on duplicate retry.

Failure behavior:

- Idempotency record exists: return saved message.
- Idempotency store unavailable: use durable message unique constraint if possible, or fail safely.

Interview signal:

> Idempotency converts at-least-once client retries into effectively-once user-visible sends.

### How The Components Work Together

Send path:

```text
WebSocket Gateway -> Message Service -> Conversation authz -> Idempotency -> Sequencer -> Message Store -> Event Stream -> Fanout Workers -> recipient sessions
```

Reconnect path:

```text
Client reconnects -> Gateway registers session -> client sends lastSeenSequence -> Message Store returns missed messages -> realtime resumes
```

One-stop interview answer:

> I split Chat into connection management, durable message handling, and ephemeral signals. Gateways manage sockets; Message Service validates, deduplicates, sequences, persists, and publishes; Fanout Workers deliver asynchronously; offline sync repairs gaps. Presence and receipts can be eventually consistent, but message persistence and membership authorization cannot be compromised.

## 2.4 Data Layer

### Core Data Models

Message record:

```json
{
  "messageId": "msg-88421",
  "conversationId": "conv-123",
  "senderId": "u-11",
  "sequence": 908122,
  "body": {"text": "hello"},
  "createdAt": "2026-06-17T12:00:00Z",
  "status": "ACTIVE"
}
```

Conversation membership:

```json
{
  "conversationId": "conv-123",
  "memberId": "u-42",
  "role": "MEMBER",
  "joinedAt": "2026-05-10T09:00:00Z",
  "state": "ACTIVE"
}
```

Receipt record:

```json
{
  "conversationId": "conv-123",
  "memberId": "u-42",
  "lastDeliveredSeq": 908120,
  "lastReadSeq": 908118,
  "updatedAt": "2026-06-17T12:02:00Z"
}
```

### Store Choices

| Data type | Candidate store | Why |
|---|---|---|
| Messages | wide-column/time-ordered store | append-heavy, paginated reads |
| Membership | relational or KV | consistency for membership/auth checks |
| Presence | in-memory/KV | high-churn ephemeral state |
| Idempotency keys | Redis/KV TTL store | fast dedup on retries |
| Stream | Kafka/Pulsar | ordered partitioned fanout backbone |

### Partitioning

- Partition messages by `conversationId` to preserve local ordering.
- Large-group conversations may need dedicated partitions or split strategy.
- Partition receipts by `conversationId` or `userId` depending read/write pattern.

### Replication

- Multi-AZ replication for durability.
- Cross-region replication for disaster recovery and roaming users.
- Eventual consistency acceptable for some receipt and presence views.

## 2.5 Scalability

### Horizontal Scaling

- Gateway fleet scales by connection count.
- Message service scales by send QPS.
- Fanout workers scale by stream partitions and lag.

### Connection Scaling

- Use event-driven I/O servers for high socket density.
- Tune heartbeat interval to balance freshness and network cost.
- Offload idle connections where possible.

### Group Chat Fanout

- Small groups: direct fanout is fine.
- Large groups/channels: use pub-sub channel model and incremental catch-up.
- Avoid synchronous per-recipient writes on hot paths.

## 2.6 Performance

### Caching Strategy

| Cache layer | What it stores | TTL |
|---|---|---:|
| Presence cache | online/last-seen snapshots | seconds |
| Membership cache | conversation members | short to medium |
| Recent messages cache | most recent N messages per conversation | short |
| User session map | user to gateway sessions | ephemeral |

### Latency Budget Example

| Stage | Target |
|---|---:|
| Gateway validation | 5-15 ms |
| Message persist + stream append | 20-80 ms |
| Fanout dispatch | 20-100 ms |
| End-to-end delivered ack | p95 under 300 ms |

### Optimization Rules

- Keep message write path minimal and asynchronous for non-critical enrichments.
- Batch receipt updates when possible.
- Use binary/compressed framing for mobile efficiency.

## 2.7 Async Systems

Use streams for:

- Message created events.
- Delivery acknowledgment events.
- Read receipt updates.
- Notification triggers.
- Analytics/abuse signals.

Queue notes:

- At-least-once delivery is typical.
- Consumer idempotency is mandatory.
- DLQ for poison events.
- Lag-based autoscaling for fanout consumers.

## 2.8 Reliability

### Retry and Idempotency

- Client retries send with same `clientMessageId`.
- Server deduplicates and returns canonical ack.
- Fanout retries with dedup key `(conversationId, messageId, recipientId)`.

### Circuit Breakers

- If presence service degrades, continue chat delivery and mark presence stale.
- If notification provider fails, queue retries and avoid blocking send path.
- If analytics fails, isolate from core chat flow.

### Failover

- Regional failover with reconnect and session rehydration.
- Replay undelivered events from durable stream offsets.
- Client offline sync endpoint repairs gaps after reconnect.

## 2.9 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Transport | WebSockets | polling/long polling | lower latency + efficiency vs connection complexity |
| Delivery | at-least-once | exactly-once | practicality + scale vs strict semantics complexity |
| Ordering | per conversation | global ordering | feasible correctness vs impossible scale cost |
| Fanout | direct push | pull on demand | low latency vs reduced write amplification |
| Presence | frequent updates | coarser updates | freshness vs network cost |

Interview framing:

> I would guarantee durable persistence, per-conversation ordering, and idempotent retry semantics, while treating presence and some receipts as eventually consistent to maintain scale and availability.

---

# 3. Low-Level Design

LLD goal:

> Model chat as a small set of stateful concepts: conversations own membership, messages own immutable content and sequence, sessions own live connection state, and receipts own delivery/read progress.

Simple rule:

- Do not put WebSocket connection details inside `Message`.
- Do not put message persistence details inside the gateway.
- Keep idempotency and sequencing explicit because retries and reconnects are normal.

Starter map:

| LLD question | Chat answer |
|---|---|
| What is durable? | `Message`, conversation membership, and message history |
| What is ephemeral? | `Session`, `PresenceState`, typing indicators, gateway connection state |
| What gives ordering? | per-conversation `sequence` assigned before publish |
| What prevents duplicate sends? | `IdempotencyStore` keyed by `(senderId, clientMessageId)` |
| What repairs missed realtime delivery? | offline sync/history fetch by cursor or sequence |
| What can be eventually consistent? | presence, read receipts, delivered receipts |

Beginner-friendly design order:

1. Model `Conversation` and `ConversationMember` first, because authorization depends on them.
2. Model immutable `Message` with `messageId`, `conversationId`, `senderId`, and `sequence`.
3. Design `MessageService.sendMessage()` as the critical path: validate, dedup, sequence, persist, publish.
4. Keep `GatewaySessionManager` focused on sockets and sessions.
5. Add `FanoutService` for delivery to online users.
6. Add `ReceiptService` and `PresenceService` as separate, eventually consistent features.

Interview sentence:

> In LLD, I will keep connection state separate from message truth: gateways manage sockets, while MessageService owns authorization, idempotency, sequencing, persistence, and fanout events.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `User` | user identity and account state | disabled users cannot send/receive normally |
| `Conversation` | conversation ID, type, metadata | ordering scope is normally one conversation |
| `ConversationMember` | user role and membership state | non-members cannot send/read |
| `Message` | immutable body, sender, sequence, timestamp | persisted message should not change identity/sequence |
| `DeliveryReceipt` | delivered progress per user/device | progress should move forward only |
| `ReadReceipt` | read progress per user | `lastReadSeq` should be monotonic |
| `PresenceState` | online/last-seen/visibility | ephemeral and allowed to be stale |
| `Session` | WebSocket connection/device mapping | expires when heartbeat fails |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `GatewaySessionManager` | register sockets, heartbeat, route outbound events | decide message persistence |
| `MessageService` | validate membership, dedup, sequence, persist, publish | manage raw socket lifecycle |
| `ConversationService` | manage members, roles, conversation metadata | deliver every message |
| `PresenceService` | maintain online/last-seen state | block message send if stale/down |
| `FanoutService` | deliver persisted message events to active sessions | create canonical messages |
| `NotificationService` | push offline fallback | block core send path |
| `ReceiptService` | update delivered/read progress | rewrite message history |

Core flow:

```text
Send: gateway event -> membership check -> idempotency -> sequence -> persist -> publish -> fanout
Reconnect: restore session -> fetch missed messages by cursor -> resume realtime stream
```

## 3.2 OOP Fundamentals

Encapsulation:

- `Message` owns validation and immutable payload fields.
- `Conversation` owns membership rules.
- `Receipt` models delivery/read transitions safely.

Abstraction:

- `MessageRepository` hides storage details.
- `FanoutService` hides delivery transport details.
- `PresenceRepository` hides in-memory cluster specifics.

Polymorphism:

- Different fanout strategies for direct, group, and channel-style conversations.
- Different notification providers behind one interface.

Composition over inheritance:

- `MessageService` composes repository, idempotency checker, sequencer, and stream publisher.

## 3.3 SOLID Principles

| Principle | Chat system application |
|---|---|
| Single Responsibility | `ReceiptService` handles state transitions only |
| Open/Closed | add new transport provider without changing core message logic |
| Liskov Substitution | any `NotificationProvider` should satisfy delivery contract |
| Interface Segregation | separate interfaces for send, history read, presence, and receipts |
| Dependency Inversion | services depend on abstractions, not concrete Kafka/Redis drivers |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | fanout and retry policy | switch behavior by conversation type |
| Observer/Event Publisher | message events to fanout/analytics | decouple core send from downstream work |
| Factory | create transport adapters | isolate provider wiring |
| Decorator | metrics, tracing, rate-limiting wrappers | cross-cutting concerns |
| State | connection/session lifecycle | explicit transitions for connect/reconnect/disconnect |

## 3.5 UML / Diagrams

### Class Diagram

```text
+-----------------------+        +-----------------------+
| MessageService        |------->| MessageRepository     |
| +sendMessage()        |        | +save()               |
+-----------+-----------+        +-----------------------+
            |
            +-------> +-----------------------+
            |         | Sequencer             |
            |         | +nextSequence()       |
            |         +-----------------------+
            |
            +-------> +-----------------------+
                      | FanoutService         |
                      | +dispatch()           |
                      +-----------------------+
```

### Send Sequence

```text
Client -> Gateway: SEND_MESSAGE(clientMessageId, conversationId, payload)
Gateway -> MessageService: validate/authz/send
MessageService -> IdempotencyStore: check+reserve(clientMessageId)
MessageService -> Sequencer: nextSequence(conversationId)
MessageService -> MessageRepository: save(message)
MessageService -> Stream: publish(message.created)
Stream -> FanoutWorker: consume
FanoutWorker -> RecipientGateway: deliver
RecipientGateway -> Client: MESSAGE_DELIVERED
```

## 3.6 Class Design

Interfaces:

```java
interface MessageRepository {
    void save(Message message);
    List<Message> fetchPage(String conversationId, Cursor cursor, int limit);
}

interface Sequencer {
    long nextSequence(String conversationId);
}

interface FanoutService {
    void dispatch(Message message, List<String> recipientUserIds);
}

interface IdempotencyStore {
    Optional<String> findCanonicalMessageId(String senderId, String clientMessageId);
    void reserve(String senderId, String clientMessageId, String canonicalMessageId);
}
```

Design notes:

- Keep send path deterministic and minimal.
- Sequence assignment should happen before publish.
- Read history uses cursor by `(sequence, messageId)`.

## 3.7 Data Handling

Machine-coding version:

- `ConcurrentHashMap<String, List<Message>>` for conversation history.
- `ConcurrentHashMap<String, Set<String>>` for conversation members.
- `ConcurrentHashMap<String, String>` for idempotency map.
- `ConcurrentHashMap<String, Set<String>>` for user sessions.

Production version:

- Distributed append store for messages.
- In-memory distributed presence/session map.
- Durable stream for fanout pipeline.
- TTL-backed idempotency key store.

## 3.8 Edge Cases

| Case | Handling |
|---|---|
| duplicate client send | dedup by `(senderId, clientMessageId)` and return same ack |
| sender not in conversation | reject before sequencing/persisting |
| message too large | reject at gateway/service boundary |
| message persisted but ack lost | client retry gets canonical message ID and sequence |
| recipient disconnects mid-delivery | offline sync repairs gap by cursor after reconnect |
| out-of-order device delivery | client orders by conversation sequence |
| read receipt races across devices | update `lastReadSeq` with monotonic max only |
| huge group fanout spike | use async fanout/pull model for large groups |
| reconnect storm | jittered backoff, admission control, session rehydration |
| presence storm | throttle/batch presence updates and allow stale presence |

Interview rule:

> Chat LLD is about protecting the send path: validate membership, deduplicate retries, assign per-conversation sequence, persist durably, then fan out asynchronously.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
chat/
  domain/
    Message.java
    Conversation.java
    Receipt.java
    Session.java
  service/
    MessageService.java
    FanoutService.java
    PresenceService.java
    ReceiptService.java
  port/
    MessageRepository.java
    ConversationRepository.java
    IdempotencyStore.java
    SessionRegistry.java
  adapter/
    InMemoryMessageRepository.java
    InMemorySessionRegistry.java
  app/
    ChatDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from threading import Lock


@dataclass(frozen=True)
class Message:
    message_id: str
    conversation_id: str
    sender_id: str
    sequence: int
    text: str
    created_at: datetime


class InMemoryChat:
    def __init__(self) -> None:
        self.members: dict[str, set[str]] = defaultdict(set)
        self.messages: dict[str, list[Message]] = defaultdict(list)
        self.idempotency: dict[tuple[str, str], str] = {}
        self.seq: dict[str, int] = defaultdict(int)
        self.lock = Lock()

    def add_member(self, conversation_id: str, user_id: str) -> None:
        with self.lock:
            self.members[conversation_id].add(user_id)

    def send_message(self, conversation_id: str, sender_id: str, client_message_id: str, text: str) -> Message:
        key = (sender_id, client_message_id)
        with self.lock:
            if sender_id not in self.members[conversation_id]:
                raise ValueError("sender is not a conversation member")

            if key in self.idempotency:
                existing_id = self.idempotency[key]
                for m in self.messages[conversation_id]:
                    if m.message_id == existing_id:
                        return m

            self.seq[conversation_id] += 1
            sequence = self.seq[conversation_id]
            message_id = f"msg-{conversation_id}-{sequence}"
            msg = Message(
                message_id=message_id,
                conversation_id=conversation_id,
                sender_id=sender_id,
                sequence=sequence,
                text=text,
                created_at=datetime.now(timezone.utc),
            )
            self.messages[conversation_id].append(msg)
            self.idempotency[key] = message_id
            return msg

    def get_messages(self, conversation_id: str, after_sequence: int = 0, limit: int = 20) -> list[Message]:
        with self.lock:
            data = [m for m in self.messages[conversation_id] if m.sequence > after_sequence]
            return data[:limit]


chat = InMemoryChat()
chat.add_member("conv-1", "u1")
chat.add_member("conv-1", "u2")
m1 = chat.send_message("conv-1", "u1", "cmsg-1", "hello")
m2 = chat.send_message("conv-1", "u1", "cmsg-1", "hello")  # deduplicated
print(m1.message_id == m2.message_id)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[conversation_id -> list[Message]]` | ordered chat history |
| `dict[conversation_id -> set[user]]` | membership checks |
| `dict[(sender, clientMsgId) -> messageId]` | idempotent retry dedup |
| `dict[user -> set[sessionId]]` | active session mapping |
| `queue/stream` | async fanout and notification pipeline |

## 4.4 Concurrency

High-signal concurrency issues:

- Duplicate sends from reconnect retries.
- Sequence conflicts on concurrent sends in same conversation shard.
- Receipt updates racing across multiple devices.
- Session map churn during reconnect storms.

Handling strategy:

- Idempotency key + canonical message mapping.
- Single-writer or partitioned sequencing per conversation.
- Last-read monotonic updates.
- Atomic session registration and heartbeat expiry.

## 4.5 Performance Optimization

Time complexity (conceptual):

- Send path: near `O(1)` for append + publish on partitioned infrastructure.
- Fanout path: depends on recipient count.
- History fetch: near `O(page_size)` with cursor and clustered storage.

Optimization rules:

- Keep hot conversation heads in cache.
- Compress wire payloads for mobile networks.
- Batch receipt updates when feasible.
- Apply adaptive fanout strategies for massive groups.

## 4.6 Error Handling

| Error | Response |
|---|---|
| invalid auth/token | connection reject |
| sender not member | `403` event/error |
| message too large | `413` equivalent event/error |
| duplicate send | return existing canonical ack |
| gateway overload | backpressure and retry hints |
| downstream notification failure | queue retry, do not fail send path |

## 4.7 Testing Thinking

Unit tests:

- Idempotent message send with same `clientMessageId`.
- Per-conversation sequence monotonicity.
- Membership authorization for send.
- History pagination correctness.

Concurrency tests:

- Parallel sends in one conversation preserve total order.
- Duplicate retry storms do not create extra messages.
- Multi-device receipt updates remain monotonic.

Load tests:

- Connection spike test.
- Large group fanout test.
- Reconnect flood after simulated region flap.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Connection storm | app open after outage | gateway saturation |
| Reconnect flood | mobile network flap | auth/session thundering herd |
| Viral group burst | huge group active at once | fanout lag and queue growth |
| Bot spam | scripted send abuse | queue pollution and user impact |
| Push provider degradation | APNS/FCM partial outage | offline delivery delay |

## 5.2 Immediate Spike Response

1. Enforce admission control and per-user rate limits at gateway.
2. Prioritize existing stable sessions over new low-priority reconnects.
3. Apply backpressure to fanout workers with lag-aware autoscaling.
4. Degrade non-critical features like typing indicators/presence frequency.
5. Shift large groups to pull-based catch-up where needed.
6. Protect message persist path as highest priority.
7. Queue push notifications for retry without blocking send path.

## 5.3 Reconnect Storm Strategy

For large reconnect waves:

- Stagger reconnect with jittered client backoff.
- Use token bucket admission for connection accepts.
- Cache auth/session validation results briefly.
- Serve partial presence state until steady state recovers.

## 5.4 Degradation Policy

Protect in this order:

1. Durable message send/receive correctness.
2. Basic message ordering and pagination consistency.
3. Delivery acks.
4. Read receipts and presence freshness.
5. Typing indicators and secondary enrichments.

Allowed degradation:

- Reduce presence update frequency.
- Delay read receipt propagation.
- Temporarily disable typing indicators.
- Slow down non-critical push notification classes.

Not allowed:

- Lose committed messages.
- Break membership/authorization correctness.
- Allow one failing dependency to collapse core chat send path.

## 5.5 Spike Interview Answer

> During abnormal spikes I preserve message durability and core delivery first, then degrade presence and typing signals. I combine gateway admission control, jittered reconnect backoff, fanout backpressure, and lag-based autoscaling so the system remains available while recovering.

---

# 6. Scaling To A Billion Users

## 6.1 Global Architecture

For billion users:

```text
Global routing (Anycast/DNS)
  -> nearest region gateway clusters
  -> regional message services and conversation partitions
  -> durable message stores + stream backbone
  -> cross-region replication for recovery and roaming
```

## 6.2 Partitioning Strategy

- Partition by `conversationId` for sequence locality.
- Place very hot conversations on isolated partitions.
- Keep per-user inbox/sync pointers in user-partitioned store.

## 6.3 Multi-Region Strategy

- Regional affinity for connection and send path.
- Home-region or leader-region ownership per conversation for ordering simplicity.
- Cross-region replication for history and disaster recovery.
- On region failover, clients reconnect and resync via cursor.

## 6.4 Connection Fleet Scaling

- Separate connection gateways from message business logic.
- Tune kernel/socket limits and event loop efficiency.
- Use lightweight heartbeat and idle timeout policies.
- Perform rolling upgrades with connection draining.

## 6.5 Storage and Retention

- Tiered retention (hot recent messages, warm archival).
- Compact metadata for frequent sync.
- Media stored separately with references in messages.
- Compliance-driven retention and deletion workflows.

## 6.6 Billion-User Capacity Plan

| Layer | Scaling plan |
|---|---|
| Edge/Gateway | massive horizontal scale by connection count |
| Message service | stateless autoscale by send QPS |
| Streams | high partition count by conversation routing |
| Fanout workers | lag-based autoscale + backpressure |
| Message store | conversation-sharded append/read architecture |
| Presence store | in-memory distributed cluster with fast expiry |
| Observability | delivery latency, queue lag, reconnect rate, loss indicators |

## 6.7 Billion-User Interview Answer

> I would scale Chat using region-local WebSocket gateways, conversation-partitioned message sequencing, durable append storage, and asynchronous fanout. Core guarantees stay per-conversation ordering plus idempotent retries. Presence and receipts remain eventually consistent during stress, enabling high availability at billion-user scale.

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
I will clarify chat scope: one-to-one, group size, ordering, delivery semantics, and retention.
I will estimate concurrent connections, messages/sec, and fanout pressure.
HLD includes WebSocket gateways, message service, conversation stream partitions, durable store, fanout workers, presence, and push fallback.
I guarantee per-conversation ordering and idempotent retries, not global exactly-once.
For reliability, I isolate non-critical presence/typing from core message durability.
For spikes, I apply admission control, jittered reconnects, and lag-aware autoscaling.
For billion users, I use regional affinity, partitioned sequencing, and replay-based recovery.
```

---

# 8. Fast Recall Rules

- WebSockets are default for low-latency bidirectional chat.
- Partition by conversation to preserve practical ordering.
- Exactly-once is impractical; use idempotency and dedup.
- Keep send path minimal: validate, persist, publish, ack.
- Presence is high-churn and can be eventually consistent.
- Gateway connection scaling is a first-class architecture concern.
- Use push notifications only as offline fallback path.
- Reconnect storms require admission control and jitter.
- Protect durable message path; degrade typing/presence first.
- Resync gaps through cursor-based history on reconnect.

