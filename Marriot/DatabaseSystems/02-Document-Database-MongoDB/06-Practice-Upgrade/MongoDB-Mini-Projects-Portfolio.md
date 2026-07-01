    # MongoDB Mini Projects Portfolio - Gold Sheet

    > **Track File #27 of 28 - Group 06: Practice Upgrade**
    > For: backend/database/system design interviews | Level: project-based mastery | Mode: requirements, schemas, indexes, APIs, scaling checklists

    This sheet builds:
    - 10 practical MongoDB projects
- Requirements, schemas, APIs, queries, scaling concerns
- Production checklist per project

Original master-map sections included here:
- 30. Mini Projects

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 30. Mini Projects

### 1. User Profile Service

Requirements:

- create/update user profile
- login lookup by email
- preferences and addresses
- soft delete

Schema:

```javascript
{ tenantId, email, name, profile, preferences, addresses, deletedAt, createdAt, updatedAt }
```

Indexes:

```javascript
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true, partialFilterExpression: { deletedAt: null } })
db.users.createIndex({ tenantId: 1, updatedAt: -1 })
```

APIs:

- `POST /users`
- `GET /users/{id}`
- `PATCH /users/{id}`
- `GET /users?email=`

Production checklist: validation, unique email, PII protection, audit updates.

### 2. E-Commerce Catalog

Requirements: products, variants, filters, search, reviews summary.

Schema: product document with attributes and variants; reviews separate.

Indexes: tenant/category/price, SKU unique, attribute index, search index.

Aggregation: category counts, price buckets.

Scaling: Atlas Search, cache hot products, shard by tenant/sku.

### 3. Order Management System

Requirements: create order, state transitions, payment/inventory integration.

Schema: order aggregate embeds items and snapshots; events separate.

Indexes: order ID unique, customer/date, status/date.

APIs: create order, get order, list customer orders, update status.

Scaling: shard by tenant/orderId; precompute daily stats.

### 4. Chat Message Store

Requirements: send message, list conversation, read receipts, attachments metadata.

Schema: messages separate from conversation metadata.

Indexes: conversation/time, sender/time, unique client message ID.

Aggregation: unread counts by conversation.

Scaling: cursor pagination, bucket hot conversations, archive old messages.

### 5. Audit Log System

Requirements: append audit events, search by actor/target/time, retention.

Schema: append-only audit documents.

Indexes: tenant/time, actor/time, target/time.

Scaling: shard by tenant/time; archive old logs.

Production: majority writes for critical audit, immutable writes, access controls.

### 6. IoT Metrics Store

Requirements: ingest metrics, query device time range, aggregate hourly.

Schema: time-series collection.

Indexes: metadata/time.

Aggregation: average/max per hour.

Scaling: shard by tenant/device, batch writes, retention TTL.

### 7. Real-Time Notification Pipeline With Change Streams

Requirements: order status change triggers notification.

Schema: orders, notifications, processedEvents.

Indexes: user unread notifications, idempotency.

Flow: change stream watches orders, writes notification, persists resume token.

Scaling: queue fanout if volume grows.

### 8. Product Search With Atlas Search Concept

Requirements: keyword, autocomplete, filters, facets.

Schema: products with search fields.

Indexes: Atlas Search index plus regular filter indexes.

Queries: `$search`, `$searchMeta` for facets.

Scaling: search index lag handling, fallback browse.

### 9. RAG Document Metadata + Vector Search Design

Requirements: upload docs, chunk, embed, retrieve with ACL.

Schema: sourceDocuments, ragChunks, ingestionJobs.

Indexes: tenant/source/chunk, tags, createdAt, vector index.

Queries: vector search with tenant and ACL filter.

Production: re-embedding, source deletion, evaluation set, audit retrieval.

### 10. Multi-Tenant SaaS Backend

Requirements: tenant isolation, users, projects, audit logs.

Schema: every collection includes `tenantId`.

Indexes: tenant-prefixed hot queries.

APIs: tenant-aware repositories.

Scaling: shard by tenant plus entity ID; handle noisy tenants.

Production: tenant filter tests, backups, per-tenant export/delete, role-based access.

---

---
