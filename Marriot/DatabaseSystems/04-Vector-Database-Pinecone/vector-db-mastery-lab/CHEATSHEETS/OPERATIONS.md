# Operations Cheatsheet

| Symptom | First Checks |
|---|---|
| low recall | query, embedding version, chunking, filters, topK |
| high p99 | topK, reranker, filters, embedding API, hot tenant |
| stale result | ingestion lag, delete propagation, content hash |
| ACL leak | tenant/ACL filters, stale permissions, reranker inputs |
| dimension mismatch | model change, index dimension, migration plan |

Core SLOs:

- retrieval p99
- recall@K
- delete propagation latency
- ACL propagation latency
- ingestion lag
- cost per query