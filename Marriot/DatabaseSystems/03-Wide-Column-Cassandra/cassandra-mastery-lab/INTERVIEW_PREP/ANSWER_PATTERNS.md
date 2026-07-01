# Cassandra Answer Patterns

## Design Answer

```text
I start from the access pattern. For <query>, I would create <table>. The partition key is <key> because it routes the read directly and distributes load. The clustering keys are <keys> because the API needs <sort/range>. I would use <consistency> for <correctness reason>. I would watch <metrics> and reject Cassandra for <unsupported query> by using <alternative>.
```

## Debugging Answer

```text
I start with the endpoint and exact CQL query. Then I inspect the table, partition key distribution, partition size, consistency level, tombstones, compaction backlog, disk/GC, and client retry behavior. I mitigate the incident first, then fix the table model if the access pattern is wrong.
```

## Tradeoff Answer

```text
Cassandra gives high write throughput, availability, and predictable primary-key-shaped reads. The cost is query inflexibility, denormalized writes, consistency choices, compaction/repair ownership, and operational complexity. If the workload needs joins, ad hoc filtering, strong relational constraints, or global analytics, I would choose another system.
```