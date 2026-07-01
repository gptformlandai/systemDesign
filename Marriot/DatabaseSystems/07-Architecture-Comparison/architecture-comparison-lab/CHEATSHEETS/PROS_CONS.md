# Pros And Cons Cheatsheet

| Store | Pros | Cons |
|---|---|---|
| SQL | transactions, joins, constraints | sharding/cross-region complexity |
| MongoDB | flexible documents, aggregate reads | joins/constraints weaker than SQL |
| Cassandra | high writes, availability | query rigidity, tombstones, partitions |
| Elasticsearch | search, facets, logs | derived index, shard ops |
| Vector DB | semantic retrieval | evaluation, freshness, ACL risks |
| Neo4j | relationship paths | fan-out, supernodes, scaling boundaries |
| Redis | very low latency | memory cost, staleness |
| Object storage | cheap durable blobs | not a low-latency query DB |
| Time-series store | time-window queries, rollups, alerting | cardinality and retention costs |
| Warehouse | analytics scale | not OLTP |