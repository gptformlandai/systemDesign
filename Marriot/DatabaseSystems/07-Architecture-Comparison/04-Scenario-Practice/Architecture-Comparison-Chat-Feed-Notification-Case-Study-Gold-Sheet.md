# Architecture Comparison Chat, Feed, and Notification Case Study - Gold Sheet

> Track File #18 of 30 - Group 04: Scenario Practice
> For: social/chat system design interviews | Level: senior | Mode: feed and messaging stores

## 1. Workloads

- send message
- fetch conversation messages
- unread counts
- fanout timeline/feed
- search old messages
- notifications

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| message by conversation/time | Cassandra/DynamoDB-style or SQL at smaller scale | partition by conversation and time |
| user/profile/settings | SQL or MongoDB | correctness and flexible profile shape |
| unread counters | Redis plus durable source | low latency counters |
| feed materialization | wide-column/key-value plus cache | fanout read efficiency |
| message search | Elasticsearch/OpenSearch | full-text search |
| analytics | warehouse/lakehouse | engagement reporting |

---

## 3. Production Risks

- hot celebrity accounts
- out-of-order messages
- duplicate notifications
- cache counter drift
- search lag
- over-fanout write amplification

---

## 4. Strong Interview Answer

```text
For chat and feeds, I would model messages and timelines by access pattern. Conversations can be partitioned by conversation ID and time, high-scale feeds may use fanout-on-write or fanout-on-read hybrids, Redis can serve unread counters and hot timelines, Elasticsearch can index messages for search, and analytics belongs in a warehouse. Ordering, idempotency, hot users, and search freshness are the key production concerns.
```

---

## 5. Revision Notes

- One-line summary: Chat/feed storage is mostly partitioning, ordering, and fanout control.
- Three keywords: conversation, timeline, fanout.
- One trap: one global feed table without hot-key strategy.