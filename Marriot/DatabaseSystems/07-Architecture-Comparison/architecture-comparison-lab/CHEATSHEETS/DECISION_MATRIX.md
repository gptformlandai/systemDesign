# Decision Matrix Cheatsheet

| Need | Choose First | Avoid As Primary |
|---|---|---|
| money/ledger | SQL relational | cache/search/vector |
| document aggregate | MongoDB/document or SQL JSON | graph/vector if no traversal/similarity |
| massive key writes | Cassandra/wide-column | relational joins at huge scale |
| text relevance | Elasticsearch/OpenSearch | SQL LIKE as main search |
| semantic similarity | Vector DB | SQL as pure embedding scanner at scale |
| path traversal | Neo4j/graph | search/vector if explicit path is required |
| hot reads | Redis/cache | cache-only source of truth |
| blobs | object storage | database BLOBs at large scale |
| metrics over time | time-series store | warehouse as alerting primary |
| analytics | warehouse/lakehouse | OLTP database for heavy reports |