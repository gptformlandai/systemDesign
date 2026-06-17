# Discord - End-to-End System Design

> Goal: practice one complete E2E community real-time system from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for community chat plus real-time presence/voice systems.
- Start broad with requirements and scale, then zoom into gateway sharding, guild/channel permissions, message queues, ordering, consistency, presence, voice signaling, offline sync, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Discord-style systems, optimize large community fanout, gateway event delivery, role-based permissions, presence scale, voice signaling, and low-latency chat.

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

| Layer | Interview signal | Discord system focus |
|---|---|---|
| Problem understanding | Can clarify scope and semantics | guilds, channels, DMs, roles, text, reactions, presence, voice |
| HLD | Can design realtime community systems | gateway shards, event dispatcher, message service, permission service, presence, voice signaling |
| LLD | Can model maintainable components | `Guild`, `Channel`, `Role`, `Message`, `GatewaySession`, `PresenceState`, `VoiceSession` |
| Machine coding | Can implement critical path | send message, permission check, sequence, persist, publish event, sync |
| Traffic spikes | Can protect production | large guild events, game launches, voice join storms, bot floods, reconnect storms |
| Global scale | Can reason across regions | gateway shards, guild partitioning, event fanout, regional voice edges |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can join guilds/servers.
- Guilds contain text channels and voice channels.
- Users can send messages in channels and DMs.
- System enforces role-based permissions.
- Users receive realtime events through a gateway connection.
- Support reactions, replies, mentions, and attachments.
- Support presence/online status.
- Support offline sync of missed messages/events.
- Support voice channel join/leave signaling if in scope.

Optional requirements to clarify:

- Are bots and gateway event subscriptions in scope?
- Are voice media packets in scope or only voice signaling?
- Do we support large public guilds with hundreds of thousands/millions of members?
- Are moderation tools/audit logs required?
- Do we need slash commands/interactions?
- Are message search and history retention in scope?

Out of scope unless interviewer asks:

- Full low-level voice codec/media server internals.
- Full bot developer platform.
- Full trust and safety ML internals.
- Full livestream/screen-share implementation.

## 1.2 Non-Functional Requirements

Realtime:

- Low-latency gateway event delivery.
- Durable accepted text messages.
- At-least-once gateway event delivery with dedup/sequence repair.
- Per-channel message ordering.

Community scale:

- Efficient fanout for large guilds.
- Role/permission checks must be correct.
- Bots can generate high-volume events and require rate limits.

Presence and voice:

- Presence is high-volume and eventually consistent.
- Voice signaling should be fast, but voice media path is separate from text messaging.

## 1.3 Constraints

- One user may belong to many guilds.
- Large guilds create enormous fanout if every event goes to every member.
- Bot clients can create command/message/event storms.
- Presence updates are high-churn and should be throttled.
- Voice media requires low latency and different infrastructure than text.
- Permissions can be inherited/overridden by guild, role, and channel.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 500 million |
| DAU | 150 million |
| Peak concurrent gateway sessions | 40 million |
| Messages/day | 20-50 billion |
| Guilds | tens of millions |
| Very large guild size | 100K-1M+ members |
| Presence updates/day | hundreds of billions |
| Voice concurrent users | millions |
| Message delivery target | p95 under 300-500 ms |
| Availability target | 99.99% text/gateway APIs |

## 1.5 Capacity Math

Back-of-the-envelope:

- `50B messages/day` is about `580K messages/sec` average globally.
- Peak can be multiple millions/sec.
- Gateway event fanout can dwarf message writes because one message may be delivered to many online guild members.
- Presence updates can exceed message volume; they need throttling, coalescing, and interest-based fanout.
- Voice join storms hit signaling, session allocation, and regional voice edges.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Gateway heartbeat | 20-60 seconds |
| Channel message page | 50-100 messages |
| Gateway session resume window | minutes |
| Presence freshness | seconds to minutes |
| Idempotency TTL | hours to days |
| Voice signaling latency | low hundreds of ms or less |

## 1.6 Clarifying Questions To Ask

- Are we designing text chat only or text plus voice?
- What max guild/channel sizes should we support?
- Are bots first-class users?
- What permission model depth is required?
- Does gateway need resumable event sequence?
- Do offline users sync only messages or all events?

Strong interview framing:

> I will design Discord-style systems around gateway shards and guild/channel event streams. Text messages are durable and ordered per channel; gateway delivery is at-least-once and resumable; presence is ephemeral and throttled; voice uses separate signaling and regional media infrastructure.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Text message flow:
Client -> Gateway Shard -> Message Service
       -> Permission Service
       -> Idempotency -> per-channel sequence
       -> Message Store + Channel Event Stream
       -> Event Dispatcher -> Gateway shards subscribed to guild/channel
       -> Online clients

Gateway resume flow:
Client reconnects -> Gateway resume(sessionId, lastEventSeq)
                 -> Event Cache/Sync Service returns missed events
                 -> realtime stream resumes

Voice join flow:
Client -> Gateway -> Voice Signaling Service
       -> allocate regional voice server
       -> return voice endpoint/token
       -> client connects to voice media path
```

Recommended architecture:

```text
Client Apps + Bots
  |
  v
+-----------------------+
| Edge + Gateway Router |
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| Gateway Shards        |<------>| Presence Service     |
| event sessions        |        | ephemeral status     |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Message Service       |------->| Permission Service   |
+-----------+-----------+        | roles/overrides      |
            |                    +----------------------+
            v
+-----------------------+        +----------------------+
| Channel Event Stream  |------->| Event Dispatcher     |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Message Store         |        | Gateway Fanout       |
+-----------------------+        +----------------------+

Voice:
Gateway -> Voice Signaling -> Voice Region Allocator -> Voice Edge/Media Servers
```

Request flow for text message:

1. Client sends message command over gateway or REST API.
2. Gateway forwards command to Message Service.
3. Permission Service checks guild/channel role permissions.
4. Idempotency Store dedups client retry.
5. Sequencer assigns `channelSequence`.
6. Message Store persists canonical message.
7. Event Stream publishes `MESSAGE_CREATE`.
8. Event Dispatcher routes event to interested gateway shards.
9. Gateways deliver event to online sessions with gateway event sequence.
10. Offline clients sync messages/events later.

## 2.2 APIs And Gateway Events

### Gateway Connect

```http
GET /v1/gateway/connect?shardId=12&deviceId=dev-1
Authorization: Bearer <token>
Upgrade: websocket
```

### Gateway Identify

```json
{
  "op": "IDENTIFY",
  "token": "jwt",
  "intents": ["GUILDS", "GUILD_MESSAGES", "DIRECT_MESSAGES", "PRESENCE"],
  "lastEventSequence": 99120
}
```

### Send Channel Message

```json
{
  "op": "SEND_MESSAGE",
  "clientMessageId": "cmsg-1",
  "guildId": "g-1",
  "channelId": "ch-1",
  "content": "ready check?",
  "mentions": ["u-2"]
}
```

### Message Create Event

```json
{
  "op": "DISPATCH",
  "type": "MESSAGE_CREATE",
  "eventSequence": 99121,
  "data": {
    "messageId": "msg-1",
    "guildId": "g-1",
    "channelId": "ch-1",
    "channelSequence": 5004,
    "authorId": "u-1",
    "content": "ready check?"
  }
}
```

### Sync Channel

```http
GET /v1/channels/{channelId}/messages?afterSequence=5000&limit=100
Authorization: Bearer <token>
```

### Join Voice Channel

```json
{
  "op": "VOICE_STATE_UPDATE",
  "guildId": "g-1",
  "channelId": "voice-1"
}
```

Important API points:

- Gateway events need event sequence for resume/dedup.
- Message order uses channel sequence.
- Bot/event subscriptions should use intents or filters to reduce fanout.
- Voice signaling is separate from voice media transport.

## 2.3 Core Components

Think of Discord as six connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Gateway plane | WebSocket sessions, shards, event dispatch | realtime client/bot updates |
| Guild plane | guilds, channels, roles, permissions | community authorization |
| Message plane | send, sequence, persist, history | durable text chat |
| Event plane | channel/guild event streams and replay | fanout and resume |
| Presence plane | online/activity/typing state | ephemeral social awareness |
| Voice plane | voice signaling and media allocation | low-latency voice sessions |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Gateway Router | route clients to shards | message persistence | connect/session QPS |
| Gateway Shard | WebSocket session and event seq | canonical messages | active sessions/events |
| Message Service | validate/dedup/sequence/persist | voice media | message QPS |
| Permission Service | role/channel authz | event fanout | permission QPS |
| Guild Service | guild/channel/member/role metadata | message bodies | guild operations |
| Event Stream | ordered channel/guild events | business validation | partitions |
| Event Dispatcher | route events to shards | durable storage | fanout volume |
| Presence Service | status/activity/typing | message durability | update volume |
| Voice Signaling | voice join/leave/session token | media packets | voice session QPS |
| Voice Edge | real-time media forwarding | text messages | voice users/regions |

### Gateway Shards

Why they exist:

- Discord-style systems have many clients and bots consuming realtime events.
- One gateway cluster cannot broadcast all events to all sessions.

Core responsibilities:

- Maintain WebSocket sessions.
- Track subscribed guilds/channels/intents.
- Assign gateway event sequence per session.
- Support heartbeat and resume.
- Deliver dispatch events at least once.
- Apply session and bot rate limits.

Failure behavior:

- Gateway crash disconnects clients.
- Clients reconnect/resume using last event sequence.
- Sync Service fetches missed messages if event cache expired.

Interview signal:

> Gateway event delivery is resumable and at-least-once; durable message history is the repair mechanism.

### Guild, Channel, And Permission Service

Why it exists:

- Access depends on guild membership, roles, channel overrides, and bans.
- Permission correctness is more important than realtime speed.

Core responsibilities:

- Store guilds, channels, roles, members, bans.
- Compute effective permission for user/channel/action.
- Cache permission results with invalidation.
- Enforce bot/app scopes.

Failure behavior:

- Fail closed for private/restricted channels.
- Invalidate permission cache on role/channel/member changes.
- Audit moderation-sensitive actions.

Interview signal:

> Permission checks happen before message write and before history read.

### Message Service And Ordering

Core responsibilities:

- Validate message command.
- Check permissions.
- Dedup `(authorId, clientMessageId)`.
- Assign per-channel sequence.
- Persist message.
- Publish `MESSAGE_CREATE`.

Ordering:

- Per-channel sequence is sufficient for user-visible ordering.
- Gateway event sequence is for session resume, not canonical message order.
- Cross-channel/global ordering is unnecessary.

### Event Dispatcher And Fanout

Why it exists:

- The message stream contains events.
- Only interested sessions should receive each event.

Strategy:

- Maintain mapping from guild/channel to gateway shards with active subscribers.
- Filter by user membership and gateway intents.
- For large guilds, avoid fanout of irrelevant presence/member events.
- Dedup at gateway/client using event IDs and sequence.

Failure behavior:

- Dispatcher lag delays realtime events but messages remain durable.
- Gateway delivery duplicate is accepted; clients dedup.

### Presence Service

Why it exists:

- Users expect online/activity/typing state.
- Presence can be much higher volume than messages.

Core responsibilities:

- Track online/offline/activity per user/device.
- Aggregate presence by guild or friend visibility.
- Throttle and coalesce updates.
- Drop low-priority presence events under load.

Failure behavior:

- Presence down: show stale/unknown.
- Presence event loss is acceptable.
- Chat still works.

### Voice Signaling And Media

Why it exists:

- Voice requires low-latency packet handling and region selection.
- Text chat infrastructure should not carry voice media packets.

Core responsibilities:

- Validate user can join voice channel.
- Allocate regional voice server.
- Issue short-lived voice token/session.
- Broadcast voice state updates to guild.
- Keep voice media path separate.

Failure behavior:

- Voice allocation failure does not break text chat.
- Reconnect to a different voice region if edge fails.
- Voice state events are eventually reflected in gateway.

## 2.4 Data Layer

### Core Data Models

Guild:

```json
{
  "guildId": "g-1",
  "name": "System Design",
  "ownerId": "u-1",
  "state": "ACTIVE"
}
```

Channel:

```json
{
  "channelId": "ch-1",
  "guildId": "g-1",
  "type": "TEXT",
  "name": "general",
  "permissionOverrides": []
}
```

Message:

```json
{
  "messageId": "msg-1",
  "guildId": "g-1",
  "channelId": "ch-1",
  "authorId": "u-1",
  "channelSequence": 5004,
  "content": "ready check?",
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Presence:

```json
{
  "userId": "u-1",
  "status": "ONLINE",
  "activity": "Playing",
  "updatedAt": "2026-06-17T12:00:00Z",
  "ttlSeconds": 90
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Guild/channel metadata | relational/document DB | structured permissions |
| Guild members/roles | relational/KV | authz checks |
| Messages | wide-column/append store | channel history by sequence |
| Event stream | Kafka/Pulsar-style stream | replay/fanout |
| Gateway sessions | in-memory/KV TTL | ephemeral routing |
| Presence | in-memory/KV TTL | high-churn ephemeral |
| Attachments | object storage + CDN | large blobs |
| Audit/mod logs | append store | moderation and compliance |

Relational-style tables:

```sql
guilds(guild_id PK, name, owner_id, state)
channels(channel_id PK, guild_id, type, name, state)
guild_members(guild_id, user_id, state, joined_at)
roles(guild_id, role_id, permissions)
member_roles(guild_id, user_id, role_id)
messages(channel_id, channel_sequence, message_id, author_id, content, created_at)
```

Important indexes:

- `messages(channel_id, channel_sequence)` for history/sync.
- `guild_members(guild_id, user_id)` for access checks.
- `member_roles(guild_id, user_id)` for permission calculation.
- `channels(guild_id)` for guild hydration.

### Partitioning

- Partition messages/events by `channelId`.
- Partition guild metadata by `guildId`.
- Partition gateway shards by user/guild assignment.
- Partition presence by `userId`.
- Isolate very large guilds on dedicated shards/partitions.

### Replication And Consistency

- Message persistence and permissions need correctness.
- Presence, typing, and voice state can be eventually consistent.
- Event delivery is at-least-once.
- Gateway resume cache can be short-lived; message history is durable fallback.

## 2.5 Scalability

### Horizontal Scaling

- Gateway shards scale by sessions and event fanout.
- Message Service scales by command QPS.
- Permission Service scales by authz QPS and cache hit rate.
- Event Dispatcher scales by fanout volume.
- Presence Service scales by updates and subscribers.
- Voice edges scale by concurrent voice users and region.

### Large Guild Strategy

- Use guild/channel subscription filtering.
- Avoid broadcasting all presence to all members.
- Use intents for bots.
- Split very large guild event streams or dedicate shards.
- Pull-based history for offline users.
- Rate-limit mentions and bot events.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Gateway command receive | 5-20 ms |
| Permission check | 5-50 ms |
| Persist + event append | 20-100 ms |
| Event dispatch to shards | 20-150 ms |
| Gateway deliver | 10-100 ms |
| Voice signaling allocation | 50-300 ms |

### Optimization Rules

- Cache guild/channel permissions with invalidation.
- Filter gateway events by subscription/intents.
- Coalesce presence updates.
- Keep voice media out of text gateway path.
- Use recent message cache for hot channels.

## 2.7 Async Systems

Use streams for:

- `MESSAGE_CREATE`
- `MESSAGE_UPDATE`
- `MESSAGE_DELETE`
- `REACTION_ADD`
- `GUILD_MEMBER_UPDATE`
- `PRESENCE_UPDATE`
- `VOICE_STATE_UPDATE`
- moderation/audit events

Queue notes:

- Event consumers are idempotent.
- Event order matters within a channel.
- Gateway delivery may duplicate.
- Event replay supports resume/sync.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated gateway sessions.
- Permission checks on every send/read.
- Bot tokens and scopes.
- Signed attachment URLs.
- Encryption in transit and at rest.

Privacy:

- Private channels must not leak message or membership metadata.
- Presence visibility may be scoped.
- DMs require participant authorization.

Abuse controls:

- Rate-limit users and bots.
- Anti-spam for mentions and invites.
- Moderation tools: delete, timeout, ban, audit log.
- Bot command and webhook limits.
- Large guild raid protection if in scope.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Gateway | active sessions, reconnect rate, resume success, event latency |
| Messaging | send ack latency, delivery latency, duplicate event rate |
| Permissions | authz latency, cache hit rate, denied/allowed mismatch |
| Events | dispatcher lag, fanout volume, DLQ count |
| Presence | update rate, dropped/coalesced updates, staleness |
| Voice | join latency, region allocation failure, voice disconnects |
| Bots | rate-limit hits, event subscription volume |

Alerts:

- Gateway resume success drops.
- Event dispatcher lag grows.
- Permission service errors spike.
- Presence update storm overloads workers.
- Voice join failures increase.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Gateway fanout | push all guild events | filter by intents/subscriptions | simplicity vs fanout cost |
| Ordering | per-channel | global/guild-wide | practical UX vs bottleneck |
| Presence | frequent updates | coalesced/throttled updates | freshness vs scale |
| Voice | same gateway path | separate voice path | simpler control vs media latency/scaling |
| Permissions | cached effective perms | live recompute | latency vs freshness |
| Offline sync | event resume cache | durable message sync | fast reconnect vs complete repair |

Interview framing:

> I would separate durable channel messages from gateway event delivery. Messages are ordered per channel and persisted; gateway events are at-least-once, resumable, filtered, and repaired through sync.

---

# 3. Low-Level Design

LLD goal:

> Model Discord around guilds, channels, roles, permissions, messages, gateway sessions, event sequences, presence, and voice sessions.

Simple rules:

- Channel permissions gate message send/read.
- Channel sequence orders messages.
- Gateway sequence supports event resume.
- Presence and voice state are ephemeral signals.
- Voice media path is separate from text chat.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Guild` | community metadata | members/roles scope permissions |
| `Channel` | type and overrides | channel access follows effective permissions |
| `Role` | permission bits | role changes invalidate permission cache |
| `GuildMember` | membership state | banned/removed users cannot read/post |
| `Message` | content, author, channel sequence | immutable sequence |
| `GatewaySession` | socket, intents, event sequence | expires without heartbeat |
| `PresenceState` | user status/activity | TTL-based ephemeral |
| `VoiceSession` | voice channel and region token | separate from message session |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `GatewayService` | sessions, heartbeats, event dispatch | persist messages |
| `MessageService` | send/edit/delete messages | calculate voice media routing |
| `PermissionService` | effective permissions | deliver WebSocket events |
| `GuildService` | guild/channel/member/role metadata | own message body truth |
| `PresenceService` | online/activity state | block message persistence |
| `VoiceSignalingService` | voice join/leave and token | handle text history |

## 3.2 OOP Fundamentals

Encapsulation:

- `PermissionSet` owns effective allow/deny logic.
- `GatewaySession` owns heartbeat/resume state.
- `Message` owns immutable channel sequence.

Abstraction:

- `EventBus` hides stream implementation.
- `SessionRegistry` hides gateway routing.
- `VoiceAllocator` hides regional voice server selection.

Polymorphism:

- Different channel types: text, DM, voice, stage/forum if in scope.
- Different event subscribers: user client, bot, internal worker.
- Different permission rules for guild/channel/role overrides.

Composition:

- `MessageService` composes permission service, idempotency store, sequencer, repository, and event publisher.

## 3.3 SOLID Principles

| Principle | Discord application |
|---|---|
| Single Responsibility | `PresenceService` only owns ephemeral presence |
| Open/Closed | add channel type without rewriting message service |
| Liskov Substitution | any `VoiceAllocator` returns valid endpoint/token |
| Interface Segregation | separate message, gateway, guild, voice APIs |
| Dependency Inversion | services depend on interfaces, not concrete stores |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | permission evaluation and event filters | different channel/bot policies |
| Observer/Event Publisher | gateway dispatch from events | decouple producers/consumers |
| State | gateway session lifecycle | identify, ready, resume, disconnected |
| Command | message and moderation actions | retry and audit |
| Decorator | rate limits and audit logging | cross-cutting controls |

## 3.5 UML / Diagrams

### Message Send Sequence

```text
Client -> Gateway: SEND_MESSAGE
Gateway -> MessageService: command
MessageService -> PermissionService: canSend(user, channel)
MessageService -> IdempotencyStore: check
MessageService -> Sequencer: next(channelId)
MessageService -> MessageRepository: save
MessageService -> EventStream: MESSAGE_CREATE
EventDispatcher -> GatewayShard: route event
GatewayShard -> Client/Bots: DISPATCH MESSAGE_CREATE
```

### Voice Join Sequence

```text
Client -> Gateway: VOICE_STATE_UPDATE(channelId)
Gateway -> VoiceSignaling: join request
VoiceSignaling -> PermissionService: canJoinVoice
VoiceSignaling -> VoiceAllocator: allocate region/server
VoiceSignaling -> Gateway: VOICE_SERVER_UPDATE
Gateway -> Client: endpoint/token
Client -> VoiceEdge: connect media path
```

## 3.6 Class Design

Interfaces:

```java
interface PermissionService {
    boolean canSendMessage(String userId, String channelId);
    boolean canReadChannel(String userId, String channelId);
    boolean canJoinVoice(String userId, String channelId);
}

interface MessageRepository {
    void save(Message message);
    List<Message> fetchAfter(String channelId, long afterSequence, int limit);
}

interface GatewayEventPublisher {
    void publish(GatewayEvent event);
}

interface VoiceAllocator {
    VoiceEndpoint allocate(String guildId, String channelId, String regionHint);
}
```

Design notes:

- `GatewayEvent` and `Message` have different sequence concepts.
- Permission cache invalidation is required when roles/channel overrides change.
- Voice endpoint tokens should be short-lived.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| gateway duplicate event | client dedups by event ID/sequence |
| client resume cache expired | fetch durable channel messages |
| role removed while user connected | invalidate permissions and stop future sends |
| bot event flood | intents, rate limits, and dispatch backpressure |
| huge guild presence storm | coalesce/drop presence updates |
| voice region fails | reallocate to healthy region |
| channel deleted while message sends | permission/state check rejects or tombstones |
| attachment too large | upload service rejects before message send |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
discord/
  domain/
    Guild.java
    Channel.java
    Role.java
    Message.java
    GatewaySession.java
    PresenceState.java
  service/
    MessageService.java
    PermissionService.java
    GatewayService.java
    PresenceService.java
    VoiceSignalingService.java
  port/
    MessageRepository.java
    GuildRepository.java
    EventPublisher.java
    SessionRegistry.java
  adapter/
    InMemoryMessageRepository.java
    InMemoryGuildRepository.java
  app/
    DiscordDemo.java
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
    author_id: str
    sequence: int
    content: str


class InMemoryDiscord:
    def __init__(self) -> None:
        self.channel_members: dict[str, set[str]] = defaultdict(set)
        self.messages: dict[str, list[Message]] = defaultdict(list)
        self.idempotency: dict[tuple[str, str], str] = {}
        self.sequence: dict[str, int] = defaultdict(int)
        self.events: list[dict] = []
        self.lock = Lock()

    def allow_channel(self, channel_id: str, user_id: str) -> None:
        with self.lock:
            self.channel_members[channel_id].add(user_id)

    def send_message(self, channel_id: str, author_id: str, client_message_id: str, content: str) -> Message:
        key = (author_id, client_message_id)
        with self.lock:
            if author_id not in self.channel_members[channel_id]:
                raise ValueError("missing channel permission")
            if key in self.idempotency:
                msg_id = self.idempotency[key]
                return next(m for m in self.messages[channel_id] if m.message_id == msg_id)
            self.sequence[channel_id] += 1
            seq = self.sequence[channel_id]
            msg = Message(f"msg-{channel_id}-{seq}", channel_id, author_id, seq, content)
            self.messages[channel_id].append(msg)
            self.idempotency[key] = msg.message_id
            self.events.append({"type": "MESSAGE_CREATE", "messageId": msg.message_id, "channelId": channel_id})
            return msg

    def sync_channel(self, channel_id: str, user_id: str, after_sequence: int) -> list[Message]:
        with self.lock:
            if user_id not in self.channel_members[channel_id]:
                raise ValueError("missing channel permission")
            return [m for m in self.messages[channel_id] if m.sequence > after_sequence]


app = InMemoryDiscord()
app.allow_channel("ch-1", "u1")
app.allow_channel("ch-1", "u2")
msg = app.send_message("ch-1", "u1", "cmsg-1", "ready check?")
print(app.sync_channel("ch-1", "u2", 0)[0].message_id == msg.message_id)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[channelId -> list[Message]]` | channel message history |
| `dict[channelId -> set[userId]]` | simplified permissions |
| `dict[(user,clientMessageId) -> messageId]` | send dedup |
| `list[GatewayEvent]` | event dispatch simulation |
| `dict[userId -> PresenceState]` | ephemeral presence |

## 4.4 Concurrency

High-signal concurrency issues:

- Concurrent sends in same channel.
- Gateway duplicate events after reconnect.
- Role changes racing with send.
- Presence updates flooding.
- Voice joins racing for same channel/region.

Handling strategy:

- Per-channel sequencer.
- Idempotency keys.
- Permission check at command time.
- Event sequence for gateway resume.
- Throttle/coalesce presence updates.

## 4.5 Testing Thinking

Unit tests:

- User without permission cannot send/read.
- Duplicate send returns same message.
- Channel sequence is monotonic.
- Sync returns messages after cursor.
- Gateway events are published after message send.

Load tests:

- Large guild fanout.
- Bot message/event flood.
- Presence storm.
- Voice join storm.
- Gateway reconnect/resume storm.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Large guild event | announcement/game launch | gateway fanout overload |
| Bot flood | malfunctioning bot/webhook | message and event queue pressure |
| Reconnect storm | gateway deploy/outage | session/auth overload |
| Presence storm | game/activity changes | ephemeral pipeline overload |
| Voice join storm | live event starts | voice signaling/edge allocation pressure |

## 5.2 Immediate Spike Response

1. Protect message persistence and permission checks.
2. Apply gateway identify/resume rate limits.
3. Use event backpressure and shard isolation.
4. Throttle bot events and webhooks.
5. Coalesce/drop presence updates.
6. Allocate voice regions with admission control.
7. Use sync fallback if gateway resume misses events.

## 5.3 Degradation Policy

Protect in this order:

1. Permission-correct message send/read.
2. Gateway connectivity and essential events.
3. Channel history sync.
4. Voice signaling.
5. Presence/activity.
6. Typing/reaction animations/non-critical events.

Not allowed:

- Leak private channel messages.
- Let banned/unauthorized users send.
- Lose accepted messages.
- Let bot floods collapse human messaging.

## 5.4 Spike Interview Answer

> During spikes I protect durable text messaging and permissions first. Gateway events can lag because clients can resume or sync. Presence is coalesced or dropped, bots are rate-limited, and voice signaling is isolated from the text message path.

---

# 6. Scaling To Global Communities

## 6.1 Global Architecture

```text
Global routing
  -> gateway shard clusters
  -> guild/channel owner shards
  -> channel event streams
  -> event dispatchers
  -> regional voice signaling and voice edges
  -> replicated message stores for sync/DR
```

## 6.2 Multi-Region Strategy

- Route clients to nearby gateway shards.
- Keep channel sequencing in owner shard/region.
- Place voice users on nearby healthy voice edge.
- Replicate messages for read locality and disaster recovery.
- Keep presence regional/ephemeral and eventually consistent.
- Isolate very large guilds/cells.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Gateway | shard by sessions/guilds/intents |
| Message | stateless services with channel sequencers |
| Permissions | cached effective permissions with invalidation |
| Event dispatcher | route by guild/channel subscriptions |
| Presence | TTL store, coalescing, interest filtering |
| Voice | regional edges and admission control |
| Store | channel-sharded append storage |
| Attachments | object storage + CDN |

## 6.4 Global Interview Answer

> I would scale Discord with gateway shards, channel-partitioned message streams, permission-correct message writes, filtered event dispatch, and separate voice infrastructure. Durable channel history repairs missed gateway events, while presence and voice state remain isolated from core text persistence.

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
I will clarify guilds, channels, DMs, roles, bots, presence, voice, search, and retention.
I will estimate gateway sessions, message QPS, event fanout, presence volume, and voice concurrency.
HLD includes gateway shards, message service, guild/permission service, channel event stream, dispatcher, presence, sync, and voice signaling.
I guarantee per-channel message ordering and permission-correct reads/writes, not global ordering.
Gateway events are at-least-once and resumable; durable sync repairs gaps.
For spikes, I throttle bots/presence, isolate voice, and protect message persistence.
For global scale, I shard gateways, partition by channel/guild, and route voice regionally.
```

---

# 8. Fast Recall Rules

- Discord = gateway event system plus durable channel messages.
- Gateway event sequence is not the same as channel message sequence.
- Message ordering is per channel.
- Permissions come from guild roles and channel overrides.
- Presence is high-volume and droppable.
- Bots need intents and rate limits.
- Voice signaling is separate from voice media.
- Offline sync repairs missed gateway events.
- Large guilds require filtered fanout.
- Durable text messaging should not depend on presence or voice systems.
