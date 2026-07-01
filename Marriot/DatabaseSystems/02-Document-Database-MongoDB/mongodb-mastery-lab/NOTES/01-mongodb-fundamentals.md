# 01. MongoDB Fundamentals

## One-Line Definition

MongoDB is a distributed document database that stores data as BSON documents and is optimized for flexible, high-scale operational workloads.

## Why It Exists

MongoDB exists because many application objects are naturally nested and aggregate-shaped, not flat table rows. Product catalogs, user profiles, CMS pages, event logs, chat messages, and GenAI metadata often fit a document model better than a heavily joined relational model.

## Developer Mental Model

Ask these questions before designing collections:

1. What does the app read most often?
2. What data changes together?
3. What must be atomic?
4. What grows forever?
5. What can be duplicated safely?
6. What indexes support the hot paths?

## MongoDB vs SQL

| Dimension | MongoDB | SQL |
|---|---|---|
| Model | Documents | Tables |
| Schema | Flexible with validation | Fixed schema by default |
| Joins | `$lookup`, references, embedding | First-class joins |
| Atomicity | Single document by default, transactions supported | Multi-row transactions are core |
| Scaling | Built-in sharding | Varies by database |
| Best fit | Aggregate reads and evolving data | Relational integrity and ad hoc joins |

## Good Use Cases

- Product catalog with variable attributes.
- User profiles and preferences.
- Content management.
- Order documents with line items.
- Audit logs and events.
- IoT/time-series data.
- Real-time dashboards with pre-aggregation.
- RAG metadata and conversation memory.
- Microservice-owned databases.

## Bad Use Cases

- Heavy relational reporting with many unpredictable joins.
- Strict foreign-key-centric systems.
- Pure cache workloads better served by Redis.
- Search-only systems better served by Elasticsearch/OpenSearch.
- OLAP workloads better served by warehouse/analytics engines.

## Interview Answer

MongoDB is not simply a NoSQL database. The strong answer is that MongoDB stores aggregate-shaped BSON documents, gives developers flexible schema evolution, supports rich indexing and aggregation, and can scale through replica sets and sharding. The tradeoff is that you must design around access patterns and take responsibility for relationships, duplication, and schema governance.
