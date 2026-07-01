# MongoDB Glossary

| Term | Meaning |
|---|---|
| BSON | Binary JSON-like storage format used by MongoDB |
| Collection | Group of documents |
| Document | BSON object; primary unit of storage and atomic update |
| `_id` | Required unique primary key field |
| ObjectId | Common MongoDB identifier type with timestamp-like component |
| Embedded document | Nested object inside a document |
| Multikey index | Index created on array field |
| Covered query | Query answered entirely from an index |
| `IXSCAN` | Index scan stage in explain plan |
| `COLLSCAN` | Collection scan stage in explain plan |
| ESR | Equality, Sort, Range index design guideline |
| Aggregation pipeline | Multi-stage document transformation framework |
| `$lookup` | Aggregation stage for joining collections |
| Replica set | Group of MongoDB nodes with primary/secondary replication |
| Oplog | Operation log used for replication and change streams |
| Write concern | Controls when writes are acknowledged |
| Read concern | Controls what data reads can observe |
| Read preference | Controls which replica set member handles reads |
| Shard | Partition that stores subset of sharded data |
| `mongos` | Query router for sharded clusters |
| Shard key | Field(s) that determine data distribution |
| Scatter-gather | Query routed to many shards |
| Change stream | Resumable stream of database changes |
| TTL index | Index that expires documents automatically |
| Atlas Search | Lucene-powered search in MongoDB Atlas |
| Vector Search | Similarity search over embedding vectors |
