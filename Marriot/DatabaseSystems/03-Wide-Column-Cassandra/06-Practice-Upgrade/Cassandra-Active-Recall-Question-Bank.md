# Cassandra Active Recall Question Bank

> Track File #22 of 25 - Group 06: Practice Upgrade
> For: backend/database/system design interviews | Level: beginner to MAANG | Mode: retrieval practice, weak-spot detection

Use this sheet after reading topic files. Answer without notes first, then check the source sheets.

---

## 1. Beginner Recall

1. What is Cassandra?
2. What is a keyspace?
3. What is a partition key?
4. What are clustering columns?
5. Why does Cassandra design start from access patterns?
6. What is the difference between a coordinator and a replica?
7. What is CQL and how is it different from SQL?
8. Why is `SimpleStrategy` not a production default?
9. What does `NetworkTopologyStrategy` add?
10. What does `TRACING ON` help you inspect?

---

## 2. Intermediate Recall

1. Design a table for latest messages by room and day.
2. Design a table for orders by customer and day.
3. Why is `status` usually a bad partition key?
4. Why can `tenant_id` alone be a bad partition key?
5. What is a wide partition?
6. How does time bucketing help?
7. Why are secondary indexes risky on hot paths?
8. When might SAI be acceptable?
9. Why are materialized views treated cautiously?
10. What is the difference between STCS, LCS, and TWCS?

---

## 3. Senior Recall

1. Walk through the write path.
2. Walk through the read path.
3. Why are SSTables immutable?
4. How do bloom filters help reads?
5. What are tombstones?
6. Why does TTL create tombstones?
7. What does `gc_grace_seconds` protect?
8. Why is repair necessary?
9. What is quorum for RF=3?
10. Why does `W + R > RF` matter?
11. What is hinted handoff?
12. Why can a timeout mean the write may have succeeded?
13. What metrics would you check for p99 read latency?
14. Why might adding nodes fail to fix a hot partition?
15. How would you test Cassandra-backed repositories?

---

## 4. MAANG Recall

1. Design an IoT metrics platform with Cassandra tables, TTL, and compaction strategy.
2. Design a chat history service and explain hot-room mitigation.
3. Design a multi-region event ingestion platform with local consistency.
4. Compare Cassandra and PostgreSQL for an order service.
5. Compare Cassandra and DynamoDB for an AWS-native workload.
6. Debug a tombstone storm from alert to prevention.
7. Debug stale reads after a regional failover.
8. Explain why Cassandra is not a general search engine.
9. Explain when LWT is worth the latency cost.
10. Give a backup/restore plan with RPO and RTO.

---

## 5. Scorecard

| Score | Meaning |
|---:|---|
| 0 | I cannot answer without notes |
| 1 | I know the definition only |
| 2 | I can explain with an example |
| 3 | I can explain tradeoffs and failure modes |
| 4 | I can answer follow-ups and compare alternatives |

Target:

```text
MAANG-ready = mostly 3s and 4s across modeling, consistency, internals, operations, and system design.
```

---

## 6. Daily Drill

```text
5 beginner/intermediate recalls
3 senior recalls
1 MAANG scenario
1 spoken answer using the interview formula
```

Revision rule:

```text
If you miss a question, go back to the exact topic sheet and write one CQL/table example.
```