# 10. Production Operations

## Production MongoDB Is More Than CRUD

A production MongoDB system needs:

- schema governance
- index lifecycle
- monitoring
- backups and restore drills
- security controls
- connection pool discipline
- slow query review
- capacity planning
- incident runbooks

## Observability

Track:

- query latency
- slow operations
- connections
- replica lag
- elections
- disk usage
- disk I/O
- CPU
- WiredTiger cache pressure
- index size
- collection growth
- backup status

Useful commands:

```javascript
db.currentOp()
db.serverStatus()
db.stats()
db.orders.stats()
db.orders.aggregate([{ $indexStats: {} }])
rs.status()
```

## Query Profiler

```javascript
db.setProfilingLevel(1, { slowms: 100 })
db.system.profile.find().sort({ ts: -1 }).limit(5).pretty()
db.setProfilingLevel(0)
```

## Backups

Backup tools:

- `mongodump` / `mongorestore`
- filesystem snapshots
- Atlas backups
- point-in-time restore
- oplog-based restore strategies

RPO: how much data loss is acceptable.

RTO: how long recovery can take.

## Security

Production baseline:

- authentication
- least privilege
- TLS
- IP allowlist/private networking
- secret manager
- encryption at rest
- audit logs
- backup encryption

## Connection Pooling

One MongoDB client per process is the normal backend pattern. Do not create a client per request.

## Operational Anti-Patterns

- no restore drills
- credentials in source code
- no slow query monitoring
- unbounded arrays
- too many indexes
- no capacity plan
- no tenant isolation tests
- reading from secondaries without stale-read awareness

## Incident Debugging Questions

1. What changed recently?
2. Which query shape is slow?
3. Is the winning plan using the right index?
4. Did data volume or tenant skew change?
5. Are writes blocked by too many indexes?
6. Is replication lag increasing?
7. Is the working set larger than memory?
8. Are backups healthy and restorable?
