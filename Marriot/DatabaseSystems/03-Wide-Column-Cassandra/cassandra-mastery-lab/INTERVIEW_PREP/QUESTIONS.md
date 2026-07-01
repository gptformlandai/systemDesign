# Cassandra Interview Questions

## Foundation

1. What is Cassandra?
2. What is a keyspace?
3. What is a partition key?
4. What are clustering columns?
5. Why is Cassandra query-model-first?

## Modeling

1. Model latest chat messages by room.
2. Model IoT metrics by device/hour.
3. Model audit events by tenant/day.
4. Why is `status` a bad partition key by itself?
5. Why is `ALLOW FILTERING` risky?

## Internals

1. Explain the write path.
2. Explain the read path.
3. What are SSTables?
4. What are tombstones?
5. Why does compaction matter?

## Production

1. How do you debug p99 latency?
2. How do you handle a hot partition?
3. How do you choose consistency levels?
4. How do you design backups and restore?
5. When is Cassandra the wrong database?