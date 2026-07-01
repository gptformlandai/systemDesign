# Cassandra Operations Cheatsheet

## Inspect

```bash
docker exec -it cassandra-mastery-lab nodetool status
docker exec -it cassandra-mastery-lab nodetool info
docker exec -it cassandra-mastery-lab nodetool tablestats cassandra_mastery.messages_by_room_day
docker exec -it cassandra-mastery-lab nodetool compactionstats
```

## Watch

| Area | Signal |
|---|---|
| latency | p95/p99 by table/query |
| errors | read/write timeouts and unavailable exceptions |
| tombstones | warning counts and tombstone scans |
| compaction | pending compactions and disk IO |
| repair | repair age and failures |
| disk | utilization, growth, snapshot space |
| client | retries, timeouts, page size, consistency |

## Incident Formula

```text
symptom -> query/table -> partition key -> consistency -> tombstones/compaction/disk -> mitigation -> model fix
```