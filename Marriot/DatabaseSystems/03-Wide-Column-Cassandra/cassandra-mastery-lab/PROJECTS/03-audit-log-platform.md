# Project 03: Audit Log Platform

Goal: design tenant-scoped audit history with high write volume and reliable retention.

---

## Requirements

- Append audit events by tenant.
- Query events by tenant and day.
- Support compliance retention.
- Export to cold storage.

---

## Tables

```text
audit_events_by_tenant_day ((tenant_id, event_day), event_ts, event_id)
audit_event_by_id (event_id)
```

The lab seeds `audit_events_by_tenant_day`; add `audit_event_by_id` as an extension exercise.

---

## Consistency

Use stronger write consistency for compliance-sensitive audit data. In a real RF=3 production DC, `LOCAL_QUORUM` is a common baseline.

---

## Operations

- monitor write failures
- test backup and restore
- define RPO/RTO
- avoid delete-heavy mutation patterns
- export immutable snapshots to object storage when required

---

## Interview Talking Points

- append-only design
- tenant/day partitioning
- retention and DR
- why search/filtering may need a search or analytics system