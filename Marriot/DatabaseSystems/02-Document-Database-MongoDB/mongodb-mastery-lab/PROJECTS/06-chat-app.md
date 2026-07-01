# Project 06: Chat App

Difficulty: Intermediate

Build a chat backend with conversations, messages, cursor pagination, read receipts, idempotent sends, and hot-conversation scaling.

---

## Goal

Practice avoiding unbounded arrays, designing message indexes, modeling read receipts separately, and reasoning about shard keys for high-write workloads.

---

## Schema Design

Keep conversations small and messages separate. Embed latest-message summary in conversation for inbox performance.

```javascript
// conversations
{
  _id: 'conv_1001',
  tenantId: 'tenant_chat',
  type: 'GROUP',
  participantIds: ['usr_1001', 'usr_1002'],
  title: 'Backend Study Group',
  lastMessage: { messageId: 'msg_9001', senderId: 'usr_1001', preview: 'Explain shard keys?', createdAt: ISODate('2026-07-01T10:00:00Z') },
  createdAt: ISODate('2026-07-01T09:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}

// messages
{
  _id: ObjectId(),
  tenantId: 'tenant_chat',
  conversationId: 'conv_1001',
  messageId: 'msg_9001',
  clientMessageId: 'deviceA-001',
  senderId: 'usr_1001',
  body: 'Explain shard keys?',
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  editedAt: null,
  deletedAt: null
}
```

---

## Sample Data

```javascript
db.conversations.insertOne({
  _id: 'conv_1001', tenantId: 'tenant_chat', type: 'GROUP', participantIds: ['usr_1001', 'usr_1002'], title: 'Backend Study Group',
  lastMessage: { messageId: 'msg_9001', senderId: 'usr_1001', preview: 'Explain shard keys?', createdAt: ISODate('2026-07-01T10:00:00Z') }, createdAt: new Date(), updatedAt: new Date()
})

db.messages.insertMany([
  { tenantId: 'tenant_chat', conversationId: 'conv_1001', messageId: 'msg_9001', clientMessageId: 'deviceA-001', senderId: 'usr_1001', body: 'Explain shard keys?', createdAt: ISODate('2026-07-01T10:00:00Z'), editedAt: null, deletedAt: null },
  { tenantId: 'tenant_chat', conversationId: 'conv_1001', messageId: 'msg_9002', clientMessageId: 'deviceB-001', senderId: 'usr_1002', body: 'Distribution plus query routing.', createdAt: ISODate('2026-07-01T10:01:00Z'), editedAt: null, deletedAt: null }
])
```

---

## CRUD Operations

Send idempotent message:

```javascript
db.messages.insertOne({
  tenantId: 'tenant_chat', conversationId: 'conv_1001', messageId: 'msg_9003', clientMessageId: 'deviceA-002',
  senderId: 'usr_1001', body: 'What about hot conversations?', createdAt: new Date(), editedAt: null, deletedAt: null
})
```

Update conversation summary:

```javascript
db.conversations.updateOne(
  { tenantId: 'tenant_chat', _id: 'conv_1001' },
  { $set: { lastMessage: { messageId: 'msg_9003', senderId: 'usr_1001', preview: 'What about hot conversations?', createdAt: new Date() }, updatedAt: new Date() } }
)
```

Read latest messages:

```javascript
db.messages.find({ tenantId: 'tenant_chat', conversationId: 'conv_1001', deletedAt: null })
  .sort({ createdAt: -1, _id: -1 })
  .limit(50)
```

Record read receipt:

```javascript
db.readReceipts.updateOne(
  { tenantId: 'tenant_chat', conversationId: 'conv_1001', userId: 'usr_1002' },
  { $set: { lastReadMessageId: 'msg_9003', readAt: new Date() } },
  { upsert: true }
)
```

---

## Indexes

```javascript
db.conversations.createIndex({ tenantId: 1, participantIds: 1, updatedAt: -1 })
db.messages.createIndex({ tenantId: 1, conversationId: 1, createdAt: -1, _id: -1 })
db.messages.createIndex({ tenantId: 1, conversationId: 1, clientMessageId: 1 }, { unique: true })
db.readReceipts.createIndex({ tenantId: 1, conversationId: 1, userId: 1 }, { unique: true })
```

---

## Aggregation Queries

Messages per conversation:

```javascript
db.messages.aggregate([
  { $match: { tenantId: 'tenant_chat', createdAt: { $gte: ISODate('2026-07-01T00:00:00Z') } } },
  { $group: { _id: '$conversationId', messages: { $sum: 1 } } },
  { $sort: { messages: -1 } }
])
```

Active senders:

```javascript
db.messages.aggregate([
  { $match: { tenantId: 'tenant_chat' } },
  { $group: { _id: '$senderId', sent: { $sum: 1 } } },
  { $sort: { sent: -1 } }
])
```

---

## Performance Considerations

- Never embed all messages in a conversation document.
- Use cursor pagination by `createdAt` and `_id`.
- Keep read receipts separate because they update frequently.
- Use idempotency keys for retry-safe message sends.

---

## Scaling Considerations

- Shard large systems by `{ tenantId: 1, conversationId: 1, createdAt: 1 }` or bucketed conversation key.
- Use bucket pattern for extremely hot large conversations.
- Fan out inbox updates asynchronously.
- Archive old messages to colder storage if product allows.

---

## Security Considerations

- Verify membership before reading or sending messages.
- Encrypt sensitive message bodies if required.
- Support moderation and deletion audit trails.
- Rate-limit sends per user/conversation.

---

## Optional API Layer

- `POST /conversations`
- `GET /conversations?cursor=`
- `POST /conversations/{conversationId}/messages`
- `GET /conversations/{conversationId}/messages?cursor=`
- `POST /conversations/{conversationId}/read-receipts`

---

## Interview Discussion Points

- Why are messages not embedded in conversations?
- How do cursor pagination and indexes work together?
- What creates a hot partition in chat?
- How would you design unread counts?
- How do you handle duplicate sends from mobile retries?
