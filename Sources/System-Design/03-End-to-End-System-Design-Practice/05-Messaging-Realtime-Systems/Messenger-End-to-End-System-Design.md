# Messenger - End-to-End System Design

> Goal: practice one complete E2E social messaging problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for social-graph-backed messaging systems.
- Start broad with requirements and scale, then zoom into WebSocket delivery, social authorization, message queues, ordering, inbox state, offline sync, safety, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Messenger-style systems, optimize low-latency chat, social-graph permissions, rich inbox UX, multi-device sync, abuse controls, and online/offline delivery.

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

| Layer | Interview signal | Messenger system focus |
|---|---|---|
| Problem understanding | Can clarify scope and semantics | 1:1 chat, groups, social graph, message requests, read receipts, media, reactions |
| HLD | Can design realtime social systems | WebSocket gateway, message service, social graph checks, inbox service, stream, fanout |
| LLD | Can model maintainable components | `Thread`, `Message`, `Participant`, `InboxItem`, `Reaction`, `DeliveryCursor` |
| Machine coding | Can implement critical path | send, dedup, sequence, persist, fanout, update inbox, sync |
| Traffic spikes | Can protect production | viral events, celebrity inbox floods, reconnect storms, spam waves |
| Global scale | Can reason across regions | region routing, social graph cache, thread partitioning, async fanout |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can send and receive 1:1 messages.
- Users can create group threads.
- Support read/delivered receipts.
- Support active status/presence.
- Support media, stickers, reactions, replies, and typing indicators.
- Support message requests for non-friend or low-trust senders if required.
- Support search within conversations if message content is server-readable.
- Support offline sync across multiple devices.

Optional requirements to clarify:

- Is message content end-to-end encrypted or server-readable?
- Is social graph/friendship required for starting a conversation?
- Are business/page conversations in scope?
- Do we need spam filtering and message request ranking?
- Do we support disappearing messages or unsend?
- Are voice/video calls in scope?

Out of scope unless interviewer asks:

- Full social network feed.
- Full ML spam/safety model internals.
- Full voice/video media server stack.
- Full ad/business messaging platform.

## 1.2 Non-Functional Requirements

Realtime:

- Low-latency message delivery for online users.
- Durable accepted messages.
- At-least-once delivery with deduplication.
- Per-thread ordering.

Social correctness:

- Respect blocking, friendship, privacy, and message-request rules.
- Prevent abusive fanout to users who should not receive messages.
- Inbox state should be eventually consistent but converge quickly.

Multi-device:

- Sync across phone, web, tablet, desktop.
- Handle duplicate sends and receipt races.
- Reconnect and fetch missed messages by cursor.

## 1.3 Constraints

- Users may be reachable on many surfaces.
- Social graph checks can be hot and latency-sensitive.
- Message request/spam classification may be asynchronous or probabilistic.
- Rich inbox state is derived from message events.
- Large group fanout can create queue pressure.
- If content is encrypted, server-side search/moderation is limited.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 2 billion |
| DAU | 700 million |
| Peak concurrent connections | 100 million |
| Messages/day | 100 billion |
| Message reactions/day | tens of billions |
| Avg devices per active user | 1.5-3 |
| Group thread max size | hundreds to thousands |
| Online delivery target | p95 under 300 ms |
| Availability target | 99.99% send/receive |

## 1.5 Capacity Math

Back-of-the-envelope:

- `100B messages/day` is about `1.16M messages/sec` average globally.
- Peak traffic can be 5x-10x, so design for millions of sends/sec.
- Inbox updates multiply writes: one message can update unread counts and last-message preview for many participants.
- Reactions and typing indicators add high-volume secondary events.
- Social graph checks must be cached carefully because every send/read may need authorization.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| WebSocket heartbeat | 20-60 seconds |
| Recent thread cache TTL | seconds to minutes |
| Social graph cache TTL | seconds to minutes, with invalidation for blocks |
| Inbox page size | 20-50 threads |
| Message page size | 50-100 messages |
| Idempotency TTL | hours to days |

## 1.6 Clarifying Questions To Ask

- Can anyone message anyone, or only friends/mutual contacts?
- Are non-friend messages delivered to a request inbox?
- Is E2EE required for all messages or only some modes?
- What are group size limits?
- Do we need inbox ranking/search?
- How strict are read receipt and active status privacy settings?

Strong interview framing:

> I will design Messenger-style chat as a realtime message system plus a social authorization and inbox layer. The message path guarantees durable per-thread ordering and retry-safe sends, while inbox ranking, message requests, presence, and receipts can be asynchronous or eventually consistent.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Send flow:
Client -> WebSocket Gateway -> Message Service
       -> Thread/Participant validation
       -> Social Graph / Block / Privacy check
       -> Idempotency -> per-thread sequence
       -> Message Store + Message Stream
       -> Fanout Workers -> active device sessions
       -> Inbox Update Workers -> unread counts/last message
       -> Push Service for offline devices

Inbox flow:
Client -> Inbox Service
       -> read user thread list
       -> rank/filter normal inbox vs message requests
       -> hydrate thread cards from cache/metadata
```

Recommended architecture:

```text
Client Apps
  |
  v
+-----------------------+
| Edge + LB + Auth      |
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| WebSocket Gateway     |<------>| Presence Service     |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Message Service       |------->| Social Graph/Privacy |
| send/ack/read         |        | block/friend checks  |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Message Stream        |------->| Fanout Workers       |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Message Store         |        | Inbox Update Workers |
+-----------------------+        +----------+-----------+
                                           |
                                           v
                                +----------------------+
                                | Inbox Store + Cache  |
                                +----------------------+
```

Request flow for send:

1. Client sends message with `clientMessageId`.
2. Gateway validates session and forwards to Message Service.
3. Message Service validates sender is a participant or can create thread.
4. Social/Privacy service checks block, friend, message request, and safety rules.
5. Idempotency Store dedups retries.
6. Sequencer assigns per-thread sequence.
7. Message is persisted and published to stream.
8. Fanout workers deliver to active devices.
9. Inbox update workers update thread cards/unread counts.
10. Offline devices receive push and later sync.

## 2.2 APIs And Events

### WebSocket Connect

```http
GET /v1/ws/connect?deviceId=dev-1
Authorization: Bearer <token>
Upgrade: websocket
```

### Send Message

```json
{
  "type": "SEND_MESSAGE",
  "clientMessageId": "cmsg-88",
  "threadId": "thr-123",
  "senderId": "u-1",
  "body": {"text": "hello"},
  "replyToMessageId": "msg-1",
  "sentAt": "2026-06-17T12:00:00Z"
}
```

### Message Ack

```json
{
  "type": "MESSAGE_ACK",
  "clientMessageId": "cmsg-88",
  "messageId": "msg-999",
  "threadId": "thr-123",
  "sequence": 441,
  "deliveryBucket": "PRIMARY_INBOX"
}
```

### Get Inbox

```http
GET /v1/users/{userId}/inbox?cursor=...&limit=30
Authorization: Bearer <token>
```

### Sync Thread Messages

```http
GET /v1/threads/{threadId}/messages?afterSequence=400&limit=100
```

### Add Reaction

```json
{
  "type": "ADD_REACTION",
  "threadId": "thr-123",
  "messageId": "msg-999",
  "reaction": "LIKE",
  "clientMutationId": "react-1"
}
```

Important API points:

- Message send uses idempotency.
- Inbox and message history are separate read models.
- Reaction/read receipt updates should be monotonic or idempotent.
- Authorization must happen on send and read, not just at connection.

## 2.3 Core Components

Think of Messenger as five connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Connection plane | WebSocket sessions and device routing | realtime reachability |
| Message plane | send validation, sequence, persist, fanout | durable ordered delivery |
| Social plane | friendship, blocks, privacy, message requests | safe allowed communication |
| Inbox plane | thread list, unread counts, last message, ranking | fast user navigation |
| Ephemeral plane | presence, typing, reactions, receipts | rich UX around core chat |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| WebSocket Gateway | socket lifecycle | message correctness | active connections |
| Message Service | send/read/reaction command validation | inbox ranking | command QPS |
| Thread Service | thread metadata and participants | raw socket delivery | thread/member operations |
| Social Graph Service | friend/block/privacy checks | message storage | graph lookup QPS |
| Message Store | ordered thread messages | presence | message volume |
| Message Stream | ordered message events | business decisions | partitions |
| Fanout Workers | active session delivery | canonical message creation | recipient devices |
| Inbox Service | thread cards and unread counts | durable message body | inbox reads/writes |
| Safety Service | spam/message request classification | final message ordering | safety QPS |
| Push Service | offline notification hint | delivery truth | offline devices |

### Social Graph And Privacy Checks

Why it exists:

- Messenger-style systems are not just arbitrary socket delivery.
- Who can message whom depends on relationship, blocks, privacy, and safety rules.

Core responsibilities:

- Check if sender is blocked.
- Determine if a new conversation goes to primary inbox or request inbox.
- Enforce privacy settings.
- Provide cached relationship facts to Message Service.
- Emit invalidation events when block/friendship changes.

Failure behavior:

- For sensitive checks like block status, fail closed or use strongly fresh cache.
- Inbox ranking can degrade, but blocking correctness should not.

Interview signal:

> Social authorization is part of the send path; inbox ranking is a derived read model.

### Message Service

Core responsibilities:

- Validate sender and thread state.
- Call Social/Privacy for new or restricted conversations.
- Deduplicate send command.
- Assign per-thread sequence.
- Persist message.
- Publish message event.
- Return canonical ack and inbox bucket.

Failure behavior:

- Duplicate retry returns existing canonical message.
- Persist succeeds but inbox update fails: inbox workers replay from stream.
- Fanout failure does not lose message; sync repairs.

### Inbox Service

Why it exists:

- A user needs fast access to recent threads without scanning message history.
- Inbox state is derived from messages, receipts, and social/safety classification.

Core responsibilities:

- Store per-user thread list.
- Track unread counts and last message preview.
- Separate primary inbox, archived, muted, spam/request buckets.
- Rank threads by recency, pinning, unread state, and product rules.
- Serve cursor-paginated inbox.

Failure behavior:

- Inbox update lag may show stale unread count.
- Direct thread sync still shows correct messages.
- Workers can rebuild inbox from message stream if needed.

Interview signal:

> Message Store is source of truth for messages; Inbox Store is a denormalized read model.

### Ordering And Consistency

Guarantees:

| Feature | Consistency model |
|---|---|
| Message order | per-thread sequence |
| Send retry | idempotent/effectively once user-visible |
| Online delivery | at-least-once |
| Inbox thread card | eventually consistent from message stream |
| Read receipts | eventually consistent monotonic cursor |
| Presence | best-effort ephemeral |

Important boundary:

- Never derive message truth from inbox.
- Never rely on presence for send success.
- Never promise global message ordering.

### Media, Reactions, And Rich Events

Media:

- Upload media to object storage with signed URL.
- Send message with media reference.
- CDN serves media downloads.

Reactions:

- Reaction is a mutation on message metadata.
- Use idempotency for add/remove.
- Store latest reaction by `(messageId, userId, reactionType)` or event log depending product.

Typing:

- Ephemeral event.
- Do not persist in durable message store.
- Rate-limit and drop under load.

## 2.4 Data Layer

### Core Data Models

Thread:

```json
{
  "threadId": "thr-123",
  "type": "GROUP",
  "createdBy": "u-1",
  "state": "ACTIVE",
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Message:

```json
{
  "messageId": "msg-999",
  "threadId": "thr-123",
  "senderId": "u-1",
  "sequence": 441,
  "body": {"text": "hello"},
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Inbox item:

```json
{
  "userId": "u-2",
  "threadId": "thr-123",
  "bucket": "PRIMARY",
  "lastMessageId": "msg-999",
  "lastSequence": 441,
  "unreadCount": 3,
  "updatedAt": "2026-06-17T12:00:01Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Messages | wide-column/append store | ordered thread history |
| Threads/participants | relational/KV | membership correctness |
| Inbox items | wide-column/KV | user-centric thread list |
| Social graph facts | graph service/cache | relationship/block checks |
| Idempotency | Redis/KV or unique index | retry dedup |
| Events | stream | fanout and inbox rebuild |
| Media | object storage + CDN | large blobs |

Relational-style tables:

```sql
threads(thread_id PK, type, state, created_by, created_at)
thread_participants(thread_id, user_id, role, state, joined_at)
messages(thread_id, sequence, message_id, sender_id, body, created_at)
inbox_items(user_id, bucket, updated_at, thread_id, last_message_id, unread_count)
reactions(message_id, user_id, reaction_type, state, updated_at)
```

Important indexes:

- `messages(thread_id, sequence)` for history/sync.
- `thread_participants(thread_id, user_id)` for authz.
- `inbox_items(user_id, bucket, updated_at DESC)` for inbox.
- `reactions(message_id)` for message hydration.

### Partitioning

- Partition messages by `threadId`.
- Partition inbox by `userId`.
- Partition social graph cache by `userId`.
- Partition reactions by `messageId`.
- Partition streams by `threadId` to preserve ordering.

### Replication And Consistency

- Messages and thread membership require strong-enough correctness.
- Inbox read models can be eventually consistent.
- Social graph updates like blocks need fast invalidation.
- Cross-region thread ordering is easier with thread home region.

## 2.5 Scalability

### Horizontal Scaling

- Gateway scales by active connections.
- Message Service scales by command QPS.
- Thread streams scale by partitions.
- Inbox workers scale by stream lag.
- Inbox Service scales by read QPS.
- Social graph cache scales by relationship lookup QPS.

### Hot Inbox Or Celebrity Strategy

- Message request bucket for unknown senders.
- Rate-limit senders and new accounts.
- Aggregate or delay low-trust notifications.
- Protect inbox update workers from unbounded fanout.
- Use safety classification asynchronously where possible.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Gateway forward | 5-20 ms |
| Thread/social validation | 10-50 ms |
| Idempotency + sequence | 5-30 ms |
| Persist + publish | 20-80 ms |
| Online fanout | 20-150 ms |
| Inbox update | async, usually under seconds |

### Optimization Rules

- Cache social graph facts with invalidation.
- Keep send path minimal; inbox update is async.
- Batch read receipts.
- Drop typing under load.
- Cache recent thread messages and inbox first page.

## 2.7 Async Systems

Use streams for:

- `message.created`
- `message.reacted`
- `thread.read`
- `inbox.item.update`
- `push.notification.requested`
- `safety.classification.requested`
- analytics events

Queue notes:

- Message fanout and inbox updates are replayable.
- Inbox workers must be idempotent.
- DLQ poison events with enough context for replay.
- Use outbox pattern after message commit.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated WebSocket and API sessions.
- Authorization on every thread read/send.
- Signed media URLs.
- Encryption in transit and at rest.
- Optional E2EE mode limits server-side features.

Privacy:

- Enforce blocks immediately.
- Respect active status and read receipt settings.
- Separate request/spam inbox where product requires.
- Avoid leaking private thread metadata in notifications.

Abuse controls:

- Rate-limit sends, invites, and media.
- Reputation scoring for new/unknown senders.
- Safety classification for spam and harmful messages where content visibility allows.
- Report/block flows feed safety systems.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Messaging | send ack latency, delivery latency, duplicate rate |
| Social checks | graph lookup latency, block enforcement failures |
| Inbox | unread staleness, inbox update lag, first-page latency |
| Gateway | active sockets, reconnect rate, heartbeat timeouts |
| Queues | stream lag, fanout retries, DLQ count |
| Safety | classification latency, spam report rate |

Alerts:

- Send ack latency spikes.
- Block/privacy check dependency errors rise.
- Inbox update lag grows.
- Reconnect rate spikes.
- Push provider failures increase.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Social check | live graph lookup | cached relationship facts | freshness vs latency |
| Inbox | denormalized per-user read model | scan messages | fast inbox vs write amplification |
| Delivery | at-least-once | exactly-once | scalable reliability vs strict complexity |
| Ordering | per thread | global | practical UX vs impossible bottleneck |
| Safety | inline classification | async classification | protection vs send latency |
| Encryption | server-readable | E2EE | features/moderation vs privacy |

Interview framing:

> I would keep the message store as source of truth and inbox as a derived read model. The send path enforces social authorization, dedupes retries, sequences per thread, persists, then fans out asynchronously.

---

# 3. Low-Level Design

LLD goal:

> Model Messenger around threads, participants, messages, social authorization, inbox read models, reactions, delivery cursors, and device sessions.

Simple rules:

- Messages are immutable ordered events.
- Inbox is a denormalized projection.
- Social privacy checks are part of authorization.
- Realtime fanout is best-effort; sync repairs.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `User` | identity and settings | blocked users cannot send/read restricted threads |
| `Thread` | type and participants | participant rules define access |
| `Participant` | role/state | removed participant cannot read new messages |
| `Message` | body, sender, sequence | sequence is immutable |
| `InboxItem` | per-user thread card | derived from message/receipt events |
| `Reaction` | user reaction state | one canonical reaction state per user/message |
| `DeliveryCursor` | delivered/read progress | monotonic |
| `DeviceSession` | live routing | ephemeral |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `MessageService` | send/read/reaction command handling | own socket lifecycle |
| `ThreadService` | thread and participants | rank inbox |
| `SocialAuthService` | privacy/block/message-request decision | persist message bodies |
| `InboxService` | per-user inbox projection | be message source of truth |
| `FanoutService` | realtime delivery | allocate sequence |
| `PresenceService` | active status | block message sends |

## 3.2 OOP Fundamentals

Encapsulation:

- `Thread` owns participant rules.
- `InboxItem` owns unread count transitions.
- `DeliveryCursor` owns monotonic progress.

Abstraction:

- `SocialGraphClient` hides graph store.
- `MessageRepository` hides storage engine.
- `InboxProjectionStore` hides denormalized read model.

Polymorphism:

- Different inbox bucket classifiers: primary, request, spam, archived.
- Different message renderers: text, media, sticker, reply.

Composition:

- `MessageService` composes thread service, social auth, idempotency, sequencer, repository, and publisher.

## 3.3 SOLID Principles

| Principle | Messenger application |
|---|---|
| Single Responsibility | `InboxService` owns inbox projections only |
| Open/Closed | add new message type without changing sequencing |
| Liskov Substitution | any `SocialAuthService` returns same decision contract |
| Interface Segregation | separate message, inbox, presence, reaction APIs |
| Dependency Inversion | core services depend on repositories and ports |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | inbox bucket classification | switch by social relationship/safety |
| Observer/Event Publisher | message events to fanout/inbox | decouple projections |
| Command | send/reaction mutations | idempotent retry semantics |
| State | thread participant lifecycle | joined, left, blocked, archived |
| Decorator | metrics/rate limiting | cross-cutting behavior |

## 3.5 UML / Diagrams

### Send Sequence

```text
Client -> Gateway: SEND_MESSAGE
Gateway -> MessageService: send
MessageService -> ThreadService: validate thread/participants
MessageService -> SocialAuthService: block/privacy/request decision
MessageService -> IdempotencyStore: check
MessageService -> Sequencer: next(threadId)
MessageService -> MessageRepository: save
MessageService -> EventStream: publish
EventStream -> FanoutWorker: deliver online
EventStream -> InboxWorker: update inbox items
```

### Inbox Sequence

```text
Client -> InboxService: getInbox(userId)
InboxService -> InboxStore: read page by bucket/update time
InboxService -> ThreadCache: hydrate thread cards
InboxService -> Client: inbox rows
```

## 3.6 Class Design

Interfaces:

```java
interface SocialAuthService {
    DeliveryDecision canDeliver(String senderId, String recipientId, String threadId);
}

interface MessageRepository {
    void save(Message message);
    List<Message> fetch(String threadId, long afterSequence, int limit);
}

interface InboxProjectionStore {
    void applyMessageCreated(MessageCreated event);
    List<InboxItem> getInbox(String userId, InboxBucket bucket, Cursor cursor);
}

interface ReactionRepository {
    void upsertReaction(String messageId, String userId, ReactionType reaction);
}
```

Design notes:

- `DeliveryDecision` can return primary, request, spam, or reject.
- Inbox projection writes must be idempotent by event ID.
- Read authorization should be checked before fetching thread history.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| user blocks sender after inbox cache is warm | invalidate graph/inbox; fail closed on send |
| duplicate send retry | same canonical ack |
| inbox update worker lags | direct thread history remains correct |
| reaction retry | upsert by `(messageId, userId)` |
| participant removed while offline | sync filters messages by membership timeline |
| unknown sender flood | request/spam bucket and rate limits |
| typing storm | drop/throttle ephemeral events |
| stale presence | show unknown/last known; do not block sends |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
messenger/
  domain/
    Thread.java
    Message.java
    InboxItem.java
    Reaction.java
    DeliveryCursor.java
  service/
    MessageService.java
    SocialAuthService.java
    InboxService.java
    FanoutService.java
  port/
    MessageRepository.java
    ThreadRepository.java
    InboxStore.java
    IdempotencyStore.java
  adapter/
    InMemoryMessageRepository.java
    InMemoryInboxStore.java
  app/
    MessengerDemo.java
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
    thread_id: str
    sender_id: str
    sequence: int
    text: str


class InMemoryMessenger:
    def __init__(self) -> None:
        self.participants: dict[str, set[str]] = defaultdict(set)
        self.blocks: set[tuple[str, str]] = set()
        self.messages: dict[str, list[Message]] = defaultdict(list)
        self.inbox: dict[str, dict[str, int]] = defaultdict(dict)
        self.idempotency: dict[tuple[str, str], str] = {}
        self.sequence: dict[str, int] = defaultdict(int)
        self.lock = Lock()

    def add_participant(self, thread_id: str, user_id: str) -> None:
        with self.lock:
            self.participants[thread_id].add(user_id)

    def block(self, blocker: str, blocked: str) -> None:
        with self.lock:
            self.blocks.add((blocker, blocked))

    def send(self, thread_id: str, sender_id: str, client_message_id: str, text: str) -> Message:
        key = (sender_id, client_message_id)
        with self.lock:
            if sender_id not in self.participants[thread_id]:
                raise ValueError("sender is not participant")
            for recipient in self.participants[thread_id]:
                if (recipient, sender_id) in self.blocks:
                    raise ValueError("sender is blocked by participant")
            if key in self.idempotency:
                existing = self.idempotency[key]
                return next(m for m in self.messages[thread_id] if m.message_id == existing)
            self.sequence[thread_id] += 1
            seq = self.sequence[thread_id]
            msg = Message(f"msg-{thread_id}-{seq}", thread_id, sender_id, seq, text)
            self.messages[thread_id].append(msg)
            self.idempotency[key] = msg.message_id
            for user_id in self.participants[thread_id]:
                self.inbox[user_id][thread_id] = seq
            return msg


app = InMemoryMessenger()
app.add_participant("thr-1", "u1")
app.add_participant("thr-1", "u2")
msg = app.send("thr-1", "u1", "cmsg-1", "hello")
print(app.inbox["u2"]["thr-1"] == msg.sequence)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[threadId -> list[Message]]` | ordered messages |
| `dict[threadId -> set[userId]]` | participants |
| `set[(blocker, blocked)]` | block checks |
| `dict[userId -> inbox rows]` | inbox projection |
| `dict[(sender, clientMsgId) -> messageId]` | idempotency |

## 4.4 Concurrency

High-signal concurrency issues:

- Concurrent sends in one thread.
- Duplicate sends after reconnect.
- Inbox updates racing with reads.
- Block changes racing with send.

Handling strategy:

- Partitioned thread sequencer.
- Idempotency key.
- Inbox projection idempotent by event ID.
- Fresh block check or invalidated cache on send.

## 4.5 Testing Thinking

Unit tests:

- Non-participant cannot send.
- Blocked sender cannot deliver.
- Duplicate send returns same message.
- Inbox projection updates on message.
- Message history ordered by sequence.

Load tests:

- Reconnect storm.
- Celebrity inbox flood.
- Inbox first-page read QPS.
- Social graph cache invalidation burst.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Reconnect storm | app/network outage recovery | gateway overload |
| Viral event | many social messages at once | stream/fanout lag |
| Celebrity/page flood | massive unknown senders | inbox/safety pressure |
| Spam wave | bot messages | abuse and queue pollution |
| Graph invalidation storm | privacy/block changes | stale authorization risk |

## 5.2 Immediate Spike Response

1. Protect durable send and authorization path.
2. Apply gateway admission control and jittered reconnect.
3. Rate-limit low-trust senders.
4. Degrade typing/presence and non-critical reactions.
5. Autoscale fanout and inbox workers by lag.
6. Use cached fallback inbox rows when projections lag.
7. Preserve block/privacy correctness.

## 5.3 Degradation Policy

Protect in this order:

1. Message send authorization and persistence.
2. Online/offline sync.
3. Inbox correctness for primary threads.
4. Read receipts/reactions.
5. Presence/typing.
6. Inbox ranking freshness.

Not allowed:

- Deliver messages to blocked users.
- Lose accepted messages.
- Duplicate user-visible sends.
- Let inbox become the only message truth.

## 5.4 Spike Interview Answer

> During spikes I protect message persistence and social authorization first. Inbox updates, presence, typing, and ranking can lag because they are projections or ephemeral signals. Fanout is at-least-once and offline sync repairs missed delivery.

---

# 6. Scaling To Global Users

## 6.1 Global Architecture

```text
Global routing
  -> regional WebSocket gateways
  -> thread home-region message service
  -> partitioned message stream
  -> regional fanout and inbox projections
  -> replicated message and inbox stores
```

## 6.2 Multi-Region Strategy

- Use nearest gateway for connection.
- Route thread sends to thread home region for ordering.
- Cache social graph facts regionally with invalidation.
- Replicate message history for sync and disaster recovery.
- Keep inbox projections region-local but rebuildable.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Gateway | active socket sharding |
| Message | stateless service + thread sequencer |
| Social graph | cache hot relationship facts |
| Stream | partition by thread ID |
| Fanout | autoscale by lag/device count |
| Inbox | user-partitioned read model |
| Safety | async classifiers and rate limits |
| Push | offline queues and provider failover |

## 6.4 Global Interview Answer

> I would scale Messenger-style systems by combining realtime chat infrastructure with social authorization and denormalized inbox projections. Message truth stays in the ordered message store; inbox, ranking, receipts, reactions, and presence are derived or eventual.

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
I will clarify social graph rules, message requests, E2EE, groups, media, reactions, and retention.
I will estimate messages/sec, connections, device fanout, inbox write amplification, and social graph lookup QPS.
HLD includes gateways, message service, thread service, social auth, stream, message store, fanout, inbox service, presence, and push.
I guarantee per-thread ordering, idempotent sends, and offline sync repair.
Inbox is a derived read model, not message truth.
For spikes, I protect send authorization and persistence, then degrade inbox ranking, presence, and typing.
For global scale, I use thread home regions, regional gateways, graph caches, and partitioned streams.
```

---

# 8. Fast Recall Rules

- Messenger = chat system plus social graph and inbox projections.
- Social/block/privacy checks are part of send authorization.
- Messages are ordered per thread.
- Inbox is denormalized and eventually consistent.
- At-least-once fanout requires client/server dedup.
- Offline sync repairs realtime misses.
- Presence and typing are droppable.
- Reactions are idempotent metadata mutations.
- Unknown senders may route to request/spam buckets.
- Block correctness should fail closed.
