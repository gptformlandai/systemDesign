# Project 02: Log Analytics Platform

Goal: model searchable application logs and dashboard aggregations.

---

## Requirements

- Search logs by service, level, trace ID, tenant, and message.
- Aggregate counts over time.
- Retain hot logs for fast search and old logs cheaply.
- Avoid mapping explosion.

---

## Index

```text
logs-app-000001 as a lab version of a data-stream backing index
```

---

## Interview Talking Points

- data streams and ILM in production
- keyword fields for structured filters
- `message` as text
- date histogram for dashboard
- mapping explosion from arbitrary log keys