# Lab 05: Observability Storage

Goal: compare storage for logs, metrics, traces, and cold archives.

---

## Drill

Choose stores:

| Need | Store |
|---|---|
| search recent logs | Elasticsearch/OpenSearch or log platform |
| query metrics by time | time-series store |
| retain cold logs cheaply | object storage |
| run monthly reports | warehouse/lakehouse |
| inspect service dependencies | trace store or graph projection |

---

## Explain Out Loud

```text
Why is unlimited hot log retention dangerous?
```

Strong answer:

```text
Hot searchable storage is expensive, shard-heavy, and operationally risky. Older data should move to cheaper storage with lifecycle policies unless fast search is truly required.
```

---

## Completion Gate

- You can explain logs vs metrics vs traces.
- You can explain retention tiers.
- You can name cardinality risk.