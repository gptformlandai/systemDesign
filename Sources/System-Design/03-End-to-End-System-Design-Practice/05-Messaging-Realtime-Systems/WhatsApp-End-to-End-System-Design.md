# WhatsApp - End-to-End System Design

> Goal: practice one complete E2E mobile messaging problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for mobile-first real-time messaging systems.
- Start broad with requirements and scale, then zoom into WebSocket gateways, message queues, ordering, delivery guarantees, online/offline sync, privacy, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For WhatsApp-style systems, optimize mobile reliability, low-latency message delivery, offline durability, privacy boundaries, and simple user-facing semantics.

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

| Layer | Interview signal | WhatsApp system focus |
|---|---|---|
| Problem understanding | Can clarify scope and semantics | 1:1 chat, groups, media, receipts, presence, multi-device, E2EE if required |
| HLD | Can design scalable realtime systems | WebSocket gateway, message service, queues, offline inbox, presence, push fallback |
| LLD | Can model maintainable components | `Chat`, `Message`, `DeviceSession`, `DeliveryState`, `OfflineInbox` |
| Machine coding | Can implement critical path | send, dedup, sequence, persist encrypted payload, fanout, ack, sync |
| Traffic spikes | Can protect production | reconnect storms, viral group bursts, regional outages, push provider failures |
| Global scale | Can reason across regions | user affinity, conversation routing, device fanout, async replication, offline repair |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can send and receive 1:1 messages in near real time.
- Users can create groups and send group messages.
- Support multiple devices per user.
- Support delivery states: sent, delivered, read.
- Support offline message delivery when recipient reconnects.
- Support media messages through external blob/object storage links.
- Support push notifications for offline users.
- Support presence/last-seen with privacy controls.

Optional requirements to clarify:

- Is end-to-end encryption required?
- Are voice/video calls in scope?
- Do we support message edit/delete/reactions?
- What is max group size?
- How long should offline messages and history be retained?
- Are disappearing messages required?

Out of scope unless interviewer asks:

- Full voice/video calling stack.
- Full key-management protocol internals.
- Full spam/abuse engine.
- Full contact discovery implementation.

## 1.2 Non-Functional Requirements

Message delivery:

- Low latency for online users.
- Durable storage for messages until delivered/synced.
- At-least-once server delivery with client/server deduplication.
- Per-chat ordering, not global ordering.

Mobile reliability:

- Efficient battery/network usage.
- Robust reconnect after flaky network transitions.
- Idempotent sends because mobile clients retry aggressively.

Privacy and security:

- Strong authentication.
- Transport encryption always.
- If E2EE is required, servers store opaque ciphertext and cannot inspect message body.

## 1.3 Constraints

- Mobile clients go offline often.
- Push notifications are hints, not reliable delivery.
- Exactly-once delivery is unrealistic at distributed scale.
- Presence is high-churn and can be stale.
- Large groups create fanout pressure.
- Multi-device fanout multiplies delivery work.
- E2EE limits server-side search, moderation, and payload inspection.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 2 billion |
| DAU | 800 million |
| Peak concurrent connections | 150 million |
| Messages/day | 100-200 billion |
| Avg message metadata+ciphertext | 300-1000 bytes |
| Peak multiplier | 5x-10x average |
| Max group size | hundreds to thousands |
| P95 online delivery target | under 300 ms in-region |
| Availability target | 99.99% message send/receive |

## 1.5 Capacity Math

Back-of-the-envelope:

- `200B messages/day` is about `2.3M messages/sec` average globally.
- At 5x peak, design for `10M+ messages/sec` across regions.
- If each message fans out to 1.5 devices on average, delivery attempts exceed accepted message rate.
- If one gateway handles `100K-500K` sockets depending memory/runtime, tens to thousands of gateway nodes are needed globally.
- Offline inbox storage grows with undelivered messages, retention policy, and multi-device state.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| WebSocket heartbeat | 20-60 seconds, tuned for mobile |
| Idempotency TTL | hours to days |
| Offline sync page | 50-200 messages |
| Push notification latency | variable, seconds-scale possible |
| Presence TTL | seconds to minutes |
| Read receipt lag tolerance | seconds |

## 1.6 Clarifying Questions To Ask

- Is ordering required per 1:1 chat/group, per sender, or globally?
- What delivery guarantee should users observe?
- Is E2EE mandatory and should the server see message bodies?
- How many active devices per user?
- What should happen to messages when a device is offline for months?
- Are very large groups treated like normal chats or channels?

Strong interview framing:

> I will design WhatsApp-style chat around persistent WebSocket connections, retry-safe sends, per-chat sequencing, durable encrypted message storage, asynchronous fanout, push fallback, and offline sync. I will not claim exactly-once delivery; I will use at-least-once delivery plus idempotency and deduplication.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Send flow:
Client -> WebSocket Gateway -> Message Service
       -> membership/device validation
       -> idempotency check
       -> per-chat sequence
       -> durable message store + message stream
       -> fanout workers -> recipient gateway/device sessions
       -> offline inbox + push fallback

Reconnect sync flow:
Client reconnects -> Gateway registers session
                 -> Sync Service fetches messages after lastAckedSequence
                 -> realtime stream resumes
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
| connection/session    |        | ephemeral state      |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Message Service       |------->| Idempotency Store    |
| send/ack/sync logic   |        +----------------------+
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| Message Stream        |------->| Fanout Workers       |
| partitioned by chat   |        +----------+-----------+
+-----------+-----------+                   |
            |                               v
            v                     +----------------------+
+-----------------------+        | Push Notification    |
| Message Store         |        | offline hint         |
| encrypted payloads    |        +----------------------+
+-----------------------+
            |
            v
+-----------------------+
| Sync / Offline Inbox  |
+-----------------------+
```

Request flow for send:

1. Client sends `SEND_MESSAGE` with `clientMessageId`, `chatId`, and encrypted payload.
2. Gateway validates session and forwards to Message Service.
3. Message Service verifies membership and sender device.
4. Idempotency Store checks `(senderDeviceId, clientMessageId)`.
5. Sequencer assigns monotonic `chatSequence`.
6. Message Store persists encrypted payload and metadata.
7. Message Stream publishes `message.created`.
8. Fanout workers deliver to active recipient device sessions.
9. Offline recipients get inbox entries and push notifications.
10. Clients ack receipt/read; server updates progress pointers.

## 2.2 APIs And Events

### WebSocket Connect

```http
GET /v1/ws/connect?deviceId=dev-1
Authorization: Bearer <token>
Upgrade: websocket
```

### Send Message Event

```json
{
  "type": "SEND_MESSAGE",
  "clientMessageId": "cmsg-123",
  "chatId": "chat-9",
  "senderDeviceId": "dev-1",
  "ciphertext": "base64...",
  "messageType": "TEXT",
  "sentAt": "2026-06-17T12:00:00Z"
}
```

### Server Ack Event

```json
{
  "type": "MESSAGE_ACK",
  "clientMessageId": "cmsg-123",
  "messageId": "msg-999",
  "chatId": "chat-9",
  "chatSequence": 8812,
  "status": "PERSISTED"
}
```

### Offline Sync API

```http
GET /v1/chats/{chatId}/messages?afterSequence=8800&limit=100
Authorization: Bearer <token>
```

### Delivery Receipt Event

```json
{
  "type": "DELIVERY_RECEIPT",
  "chatId": "chat-9",
  "messageId": "msg-999",
  "deviceId": "dev-2",
  "deliveredAt": "2026-06-17T12:00:02Z"
}
```

Important API points:

- Client-generated IDs make retries safe.
- Message body may be opaque ciphertext.
- Sync uses cursor/sequence repair, not trust in realtime delivery alone.
- Push notification should not contain sensitive body unless product policy allows.

## 2.3 Core Components

Think of WhatsApp as four connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Connection plane | WebSocket sessions, heartbeats, reconnects | keep devices reachable |
| Message plane | validation, dedup, sequence, persist, fanout | durable ordered chat delivery |
| Sync plane | offline inbox, progress pointers, catch-up | repair missed realtime delivery |
| Ephemeral plane | presence, typing, push hints | useful UX without weakening core |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| WebSocket Gateway | socket lifecycle and local sessions | durable message truth | concurrent connections |
| Session Registry | user/device to gateway mapping | message bodies | session churn |
| Message Service | send validation, idempotency, sequence, persist | raw socket operations | message QPS |
| Chat Service | memberships, group metadata, device list | delivery transport | chat/member operations |
| Message Store | encrypted message history and metadata | presence | storage/write/read volume |
| Message Stream | ordered message events by chat | business validation | partitions and event volume |
| Fanout Workers | deliver persisted messages to devices | create canonical messages | recipient count and lag |
| Sync Service | catch-up by sequence/progress | realtime socket lifecycle | reconnect/sync QPS |
| Presence Service | online/last-seen/typing | durability | heartbeat/update volume |
| Push Service | offline notifications | delivery truth | offline notification volume |

### WebSocket Gateway

Why it exists:

- Mobile chat needs bidirectional low-latency communication.
- Business services should not directly manage millions of sockets.

Core responsibilities:

- Accept authenticated WebSocket connections.
- Maintain heartbeat, idle timeout, and disconnect handling.
- Register device sessions in Session Registry.
- Forward inbound client events to backend services.
- Push outbound events from fanout workers to connected devices.
- Apply per-device and per-user rate limits.

Failure behavior:

- Gateway crash disconnects devices.
- Clients reconnect with jitter.
- Sync Service fetches missed messages by sequence.

Interview signal:

> Gateway is a connection manager, not the source of message truth.

### Message Service

Why it exists:

- It owns the correctness of the send path.
- It converts unreliable mobile retries into one canonical message.

Core responsibilities:

- Validate sender membership/device.
- Deduplicate using `(senderDeviceId, clientMessageId)`.
- Assign per-chat sequence.
- Persist encrypted payload and metadata.
- Publish message event to stream.
- Return canonical ack.

Failure behavior:

- Persist succeeds but ack lost: retry returns existing ack.
- Stream publish fails after persist: outbox pattern republishes.
- Duplicate retry does not allocate a new sequence.

Interview signal:

> The accepted-message boundary is durable persistence plus canonical message ID/sequence.

### Ordering And Delivery Guarantees

Practical guarantees:

| Concern | Guarantee |
|---|---|
| Ordering | monotonic per chat/conversation |
| Send retry | effectively-once user-visible message through idempotency |
| Server delivery | at-least-once to devices |
| Client display | dedup by `messageId` and order by `chatSequence` |
| Receipts | eventually consistent progress pointers |

Why not exactly-once:

- Gateways can crash after sending but before acking.
- Clients can retry after timeout.
- Queues generally deliver at least once.
- Network partitions make perfect delivery acknowledgment impossible.

Better approach:

- Idempotent producers.
- Durable message store.
- At-least-once fanout.
- Client/server dedup.
- Cursor-based offline sync.

### Offline Inbox And Sync Service

Why it exists:

- Recipients are often offline.
- Realtime delivery can fail even when the message is durable.

Core responsibilities:

- Track per-device and per-chat delivered/read progress.
- Fetch messages after last acknowledged sequence.
- Support paginated history.
- Reconcile gaps after reconnect.
- Expire old undelivered data according to retention policy.

Failure behavior:

- Fanout missed: sync repairs.
- Device receives duplicate: client dedups.
- Device offline too long: retention policy decides whether message remains.

Interview signal:

> Offline sync is the safety net that makes realtime messaging reliable in practice.

### Group Messaging

Core challenge:

- Group messages amplify one send into many device deliveries.
- Ordering must remain simple enough to scale.

Strategy:

- Assign one sequence per group chat.
- Persist message once.
- Fanout asynchronously to active member devices.
- For very large groups, use pull-based catch-up or channel semantics.
- Store receipt progress as aggregate or per-member pointers depending product.

### Media Messages

Flow:

```text
Client uploads encrypted media -> Object Store via signed URL
Client sends message with media reference + encrypted metadata
Message Service persists reference
Recipients download media through authorized/signed URL
```

Important points:

- Do not send large media through WebSocket.
- Use object storage and CDN for media bytes.
- Store checksums, object keys, content type, and expiry.
- If E2EE is required, media is encrypted before upload.

## 2.4 Data Layer

### Core Data Models

Message:

```json
{
  "messageId": "msg-999",
  "chatId": "chat-9",
  "senderUserId": "u-1",
  "senderDeviceId": "dev-1",
  "chatSequence": 8812,
  "ciphertext": "base64...",
  "messageType": "TEXT",
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Chat membership:

```json
{
  "chatId": "chat-9",
  "userId": "u-2",
  "role": "MEMBER",
  "state": "ACTIVE",
  "joinedAt": "2026-06-01T00:00:00Z"
}
```

Device sync state:

```json
{
  "chatId": "chat-9",
  "userId": "u-2",
  "deviceId": "dev-2",
  "lastDeliveredSequence": 8812,
  "lastReadSequence": 8809,
  "updatedAt": "2026-06-17T12:00:03Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Messages | wide-column/append store | chat-partitioned writes and cursor reads |
| Chat membership | relational/KV | authorization correctness |
| Device sessions | Redis/KV TTL | ephemeral routing state |
| Idempotency keys | Redis/KV or unique DB constraint | retry dedup |
| Message events | Kafka/Pulsar-style stream | ordered async fanout |
| Media blobs | object storage + CDN | large immutable encrypted assets |
| Receipts | wide-column/KV | progress pointers |

Relational-style tables:

```sql
messages(chat_id, chat_sequence, message_id, sender_user_id, sender_device_id, ciphertext, created_at)
chat_members(chat_id, user_id, role, state, joined_at)
device_sync(chat_id, user_id, device_id, last_delivered_sequence, last_read_sequence, updated_at)
idempotency(sender_device_id, client_message_id, message_id, expires_at)
```

Important indexes:

- `messages(chat_id, chat_sequence)` for chat history and sync.
- `chat_members(chat_id, user_id)` for authorization.
- `device_sync(user_id, device_id)` for reconnect state.
- `idempotency(sender_device_id, client_message_id)` for retry safety.

### Partitioning

- Partition messages and streams by `chatId` for ordering.
- Partition session registry by `userId` or `deviceId`.
- Partition receipts by `chatId` or `userId` depending dominant reads.
- Isolate extremely hot group chats if needed.

### Replication And Consistency

- Message persistence and membership checks need strong-enough correctness.
- Presence and receipts can be eventually consistent.
- Cross-region replication can be async, with home-region ownership per chat for ordering.
- Offline sync repairs gaps after failover.

## 2.5 Scalability

### Horizontal Scaling

- Gateway fleet scales by concurrent sockets.
- Message Service scales by send QPS.
- Stream partitions scale by `chatId`.
- Fanout workers scale by queue lag and recipient count.
- Sync Service scales by reconnect and history-read QPS.

### Hot Group Strategy

- Async fanout.
- Backpressure fanout workers by lag.
- Pull-based catch-up for huge groups.
- Coalesce receipts for large groups.
- Limit noisy ephemeral events like typing indicators.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Gateway receive/forward | 5-20 ms |
| Membership + idempotency | 5-30 ms |
| Persist + stream append | 20-80 ms |
| Fanout to online device | 20-150 ms |
| In-region online delivery | p95 under 300 ms |

### Optimization Rules

- Keep WebSocket messages small.
- Store media outside the message path.
- Cache membership with short TTL and fail closed on sensitive checks.
- Batch receipt writes.
- Tune heartbeat to balance freshness and battery.

## 2.7 Async Systems

Use streams for:

- `message.created`
- `message.delivered`
- `message.read`
- `presence.changed`
- `push.notification.requested`
- abuse/rate-limit signals

Queue notes:

- Consumers are idempotent.
- Fanout is at-least-once.
- Use DLQ for poison events.
- Use outbox pattern between message persistence and publish.

## 2.8 Security, Privacy, And Abuse

Security:

- Strong device authentication and token rotation.
- TLS for all transport.
- Signed media upload/download URLs.
- Optional or required E2EE with server storing ciphertext only.
- Key material should not be logged.

Privacy:

- Presence and read receipts need privacy settings.
- Push notifications should avoid sensitive content if privacy mode requires.
- Metadata minimization matters even when payloads are encrypted.

Abuse controls:

- Per-user/device send rate limits.
- New-account group invite limits.
- Spam detection based on metadata and user reports.
- Attachment scanning only possible before encryption or on metadata unless product policy allows content inspection.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Messaging | send success rate, ack latency, delivery latency, duplicate rate |
| Gateway | active sockets, reconnect rate, heartbeat timeout, memory per connection |
| Queues | stream lag, fanout retry rate, DLQ count |
| Sync | missed-message repair rate, sync latency, pagination errors |
| Presence | update rate, stale session rate |
| Push | provider error rate, notification latency |

Alerts:

- Send success rate drops.
- Reconnect storm spikes.
- Stream lag grows.
- Gateway memory/file descriptors near limit.
- Offline sync error rate rises.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Transport | WebSocket | polling | low latency vs connection complexity |
| Delivery | at-least-once + dedup | exactly-once | practical scale vs impossible strictness |
| Ordering | per chat | global | useful correctness vs global bottleneck |
| Privacy | E2EE ciphertext | server-readable content | privacy vs search/moderation features |
| Group fanout | push to all | pull catch-up | low latency vs fanout cost |
| Receipts | per-device exact | aggregated/eventual | precision vs write amplification |

Interview framing:

> I would guarantee durable accepted messages, per-chat ordering, retry-safe sends, and offline repair. I would not depend on fresh presence or push notifications for correctness.

---

# 3. Low-Level Design

LLD goal:

> Model WhatsApp around chats, device sessions, immutable messages, idempotent sends, per-chat sequences, offline sync pointers, and eventual receipts.

Simple rules:

- Gateway owns connections, not message truth.
- Message Service owns send correctness.
- Sync Service repairs delivery gaps.
- Receipts and presence are progress signals, not message truth.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `User` | account identity | disabled users cannot send normally |
| `Device` | device identity and keys | device must be registered |
| `Chat` | type, members, metadata | non-members cannot send/read |
| `Message` | immutable payload and sequence | sequence never changes |
| `DeviceSession` | WebSocket connection mapping | expires without heartbeat |
| `DeliveryState` | delivered/read progress | moves forward only |
| `OfflineInbox` | pending sync entries | deduped by message ID |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `GatewaySessionManager` | sockets and heartbeats | persist messages |
| `MessageService` | validate, dedup, sequence, persist | own socket lifecycle |
| `ChatService` | membership and group metadata | deliver bytes |
| `FanoutService` | send events to active devices | create messages |
| `SyncService` | fetch missed messages | assign sequences |
| `ReceiptService` | delivered/read progress | rewrite message history |

## 3.2 OOP Fundamentals

Encapsulation:

- `Chat` owns membership rules.
- `Message` owns immutable identity and sequence.
- `DeliveryState` owns monotonic transitions.

Abstraction:

- `MessageRepository` hides storage.
- `SessionRegistry` hides routing state.
- `MessageQueue` hides stream implementation.

Polymorphism:

- Different fanout strategies for 1:1, small group, and large group.
- Different media handlers for image, video, document, audio.

Composition:

- `MessageService` composes chat service, idempotency store, sequencer, repository, and event publisher.

## 3.3 SOLID Principles

| Principle | WhatsApp application |
|---|---|
| Single Responsibility | `ReceiptService` only updates receipt progress |
| Open/Closed | add message type without rewriting send workflow |
| Liskov Substitution | any `MessageRepository` preserves save/fetch contract |
| Interface Segregation | separate send, sync, presence, receipt APIs |
| Dependency Inversion | services depend on ports, not concrete Redis/Kafka drivers |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | group fanout policy | switch by chat size/type |
| State | session and upload lifecycle | explicit transitions |
| Observer/Event Publisher | message events to fanout/push | decouple accepted send from delivery |
| Decorator | metrics/rate-limit wrappers | cross-cutting concerns |
| Command | client message send operation | retry and dedup cleanly |

## 3.5 UML / Diagrams

### Send Sequence

```text
Client -> Gateway: SEND_MESSAGE(clientMessageId)
Gateway -> MessageService: send(command)
MessageService -> ChatService: validate membership
MessageService -> IdempotencyStore: find/reserve
MessageService -> Sequencer: next(chatId)
MessageService -> MessageRepository: save(message)
MessageService -> EventStream: publish message.created
EventStream -> FanoutWorker: consume
FanoutWorker -> SessionRegistry: active devices
FanoutWorker -> Gateway: deliver
Gateway -> RecipientClient: MESSAGE
```

### Reconnect Sequence

```text
Client -> Gateway: connect(deviceId)
Gateway -> SessionRegistry: register
Client -> SyncService: sync(lastSeenByChat)
SyncService -> MessageRepository: fetch after sequence
SyncService -> Client: missed messages
```

## 3.6 Class Design

Interfaces:

```java
interface MessageRepository {
    void save(Message message);
    List<Message> fetchAfter(String chatId, long afterSequence, int limit);
}

interface ChatMembershipService {
    boolean canSend(String chatId, String userId);
    List<DeviceId> recipientDevices(String chatId, String senderUserId);
}

interface IdempotencyStore {
    Optional<Message> find(String senderDeviceId, String clientMessageId);
    void remember(String senderDeviceId, String clientMessageId, String messageId);
}

interface Sequencer {
    long nextSequence(String chatId);
}
```

Design notes:

- Sequence is assigned once.
- Dedup happens before sequence allocation when possible.
- Save and publish should use an outbox pattern in production.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| client retries send after timeout | return same canonical ack |
| gateway sends duplicate delivery | client dedups by message ID |
| recipient offline | persist and sync later; push as hint |
| membership changes during send | validate at send time |
| group has thousands of members | async fanout or pull catch-up |
| device reconnects to different region | session registry updates; sync repairs gaps |
| presence stale | show last known/unknown; do not block send |
| idempotency store down | use durable unique constraint or fail safely |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
whatsapp/
  domain/
    Chat.java
    Message.java
    DeviceSession.java
    DeliveryState.java
  service/
    MessageService.java
    FanoutService.java
    SyncService.java
    ReceiptService.java
  port/
    MessageRepository.java
    ChatRepository.java
    IdempotencyStore.java
    SessionRegistry.java
  adapter/
    InMemoryMessageRepository.java
    InMemorySessionRegistry.java
  app/
    WhatsAppDemo.java
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
    chat_id: str
    sender_id: str
    sequence: int
    ciphertext: str


class InMemoryWhatsApp:
    def __init__(self) -> None:
        self.members: dict[str, set[str]] = defaultdict(set)
        self.messages: dict[str, list[Message]] = defaultdict(list)
        self.idempotency: dict[tuple[str, str], str] = {}
        self.sequence: dict[str, int] = defaultdict(int)
        self.lock = Lock()

    def add_member(self, chat_id: str, user_id: str) -> None:
        with self.lock:
            self.members[chat_id].add(user_id)

    def send(self, chat_id: str, sender_id: str, client_message_id: str, ciphertext: str) -> Message:
        key = (sender_id, client_message_id)
        with self.lock:
            if sender_id not in self.members[chat_id]:
                raise ValueError("sender is not a member")
            if key in self.idempotency:
                existing_id = self.idempotency[key]
                return next(m for m in self.messages[chat_id] if m.message_id == existing_id)
            self.sequence[chat_id] += 1
            seq = self.sequence[chat_id]
            msg = Message(f"msg-{chat_id}-{seq}", chat_id, sender_id, seq, ciphertext)
            self.messages[chat_id].append(msg)
            self.idempotency[key] = msg.message_id
            return msg

    def sync(self, chat_id: str, after_sequence: int) -> list[Message]:
        with self.lock:
            return [m for m in self.messages[chat_id] if m.sequence > after_sequence]


app = InMemoryWhatsApp()
app.add_member("chat-1", "u1")
app.add_member("chat-1", "u2")
first = app.send("chat-1", "u1", "cmsg-1", "encrypted-hello")
retry = app.send("chat-1", "u1", "cmsg-1", "encrypted-hello")
print(first.message_id == retry.message_id)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[chatId -> list[Message]]` | ordered message history |
| `dict[chatId -> set[userId]]` | membership checks |
| `dict[(sender,clientMessageId) -> messageId]` | retry dedup |
| `dict[userId -> set[deviceSession]]` | active device routing |
| `dict[(chatId,deviceId) -> sequence]` | sync progress |

## 4.4 Concurrency

High-signal concurrency issues:

- Concurrent sends in one chat need one sequence order.
- Duplicate retries must not create duplicates.
- Multi-device receipts race.
- Session registry changes during fanout.

Handling strategy:

- Partitioned single-writer sequence per chat.
- Idempotency key before commit.
- Monotonic max receipt updates.
- Treat session registry as best-effort and rely on sync.

## 4.5 Testing Thinking

Unit tests:

- Duplicate send returns same message.
- Non-member cannot send.
- Sequence increases per chat.
- Sync returns only messages after cursor.
- Read/delivery progress never moves backward.

Load tests:

- Reconnect storm.
- Hot group fanout.
- Offline sync burst after regional recovery.
- Push provider outage.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Reconnect storm | mobile network/provider outage recovery | gateway/auth/session overload |
| Viral group burst | large group active at once | fanout lag |
| Regional outage | clients fail over | cross-region sync pressure |
| Push provider failure | APNS/FCM degradation | offline hints delayed |
| Spam wave | automated sends | queue pollution |

## 5.2 Immediate Spike Response

1. Protect durable send path.
2. Apply gateway admission control for reconnects.
3. Use jittered client backoff.
4. Autoscale fanout workers by lag.
5. Degrade typing/presence frequency.
6. Batch receipts.
7. Rate-limit abusive senders.

## 5.3 Degradation Policy

Protect in this order:

1. Durable message accept and sync.
2. Online message fanout.
3. Delivery receipts.
4. Read receipts.
5. Presence and typing.
6. Push notification freshness.

Not allowed:

- Lose accepted messages.
- Allow non-members to read/send.
- Create duplicate user-visible messages on retry.
- Treat push as proof of delivery.

## 5.4 Spike Interview Answer

> During spikes I protect the durable message path first. Realtime fanout can lag because offline sync repairs gaps. I use gateway admission control, jittered reconnects, async fanout, receipt batching, and graceful degradation of presence/typing.

---

# 6. Scaling To Global Users

## 6.1 Global Architecture

```text
Global routing
  -> nearest WebSocket gateway region
  -> user/chat home-region message service
  -> partitioned message streams
  -> regional fanout and push
  -> replicated message stores for sync/DR
```

## 6.2 Multi-Region Strategy

- Keep a home region for each chat when strict per-chat ordering is needed.
- Route users to nearby gateways but route sends to chat owner region.
- Replicate message history asynchronously for disaster recovery.
- Reconnect and sync after failover.
- Keep presence regional and eventually consistent.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Gateway | horizontal scale by sockets |
| Message Service | stateless scale by send QPS |
| Sequencer | partition by chat ID |
| Stream | partition by chat ID and lag |
| Fanout | autoscale by recipient count and lag |
| Store | chat-sharded append storage |
| Sync | cache recent messages and paginate |
| Push | provider-specific queues and retries |

## 6.4 Global Interview Answer

> I would scale WhatsApp-style messaging with regional gateways, chat-partitioned message streams, per-chat sequencing, durable encrypted storage, and async fanout. The system uses at-least-once delivery plus dedup and offline sync rather than exactly-once delivery.

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
I will clarify 1:1, groups, E2EE, media, multi-device, ordering, and retention.
I will estimate connections, messages/sec, fanout factor, storage, and reconnect load.
HLD includes WebSocket gateways, session registry, message service, chat service, stream, store, fanout, sync, presence, and push.
I guarantee per-chat ordering and retry-safe sends, not global exactly-once.
Offline sync repairs missed realtime delivery.
For spikes, I protect durable send and degrade presence/typing/receipts first.
For global scale, I use regional gateways, chat home regions, partitioned streams, and async replication.
```

---

# 8. Fast Recall Rules

- WebSocket gateway manages connections, not message truth.
- Message Service owns validation, idempotency, sequence, persist.
- Delivery is at-least-once; clients dedup by message ID.
- Ordering should be per chat, not global.
- Push notification is only an offline hint.
- Offline sync repairs missed realtime fanout.
- Presence and read receipts can be stale.
- Media goes through object storage, not WebSocket.
- Reconnect storms need jitter and admission control.
- Accepted messages must never be lost.
