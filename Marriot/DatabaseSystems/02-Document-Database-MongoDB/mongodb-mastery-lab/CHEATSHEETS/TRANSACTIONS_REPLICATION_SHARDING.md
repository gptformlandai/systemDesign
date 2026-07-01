# Transactions, Replication, and Sharding Cheat Sheet

## Transactions

Use transactions when an invariant crosses documents.

```javascript
const session = db.getMongo().startSession();
session.startTransaction({ readConcern: { level: 'snapshot' }, writeConcern: { w: 'majority' } });
try {
  session.getDatabase('app').accounts.updateOne({ _id: 'A' }, { $inc: { balance: -100 } });
  session.getDatabase('app').accounts.updateOne({ _id: 'B' }, { $inc: { balance: 100 } });
  session.commitTransaction();
} catch (error) {
  session.abortTransaction();
  throw error;
} finally {
  session.endSession();
}
```

Prefer single-document atomic design when possible.

## Concerns and Preferences

| Concept | Meaning |
|---|---|
| Read concern | What data a read is allowed to observe |
| Write concern | When a write is acknowledged |
| Read preference | Which replica set member handles reads |
| Retryable writes | Driver can retry safe write operations |
| Causal consistency | Session can read its own writes across members |

## Replication

| Term | Meaning |
|---|---|
| Primary | Accepts writes |
| Secondary | Replicates oplog from primary |
| Oplog | Capped operation log |
| Election | Chooses new primary after failure |
| Majority write | Acknowledged by majority of voting nodes |
| Replication lag | Delay before secondaries apply writes |

Commands:

```javascript
rs.status()
rs.conf()
rs.stepDown(60)
```

## Sharding

Good shard key:

- high cardinality
- even distribution
- query-aligned
- write distribution
- stable value
- avoids hotspots

Bad keys:

- timestamp only
- low-cardinality status
- tenantId only when tenants are skewed
- random key that prevents query targeting

Targeted query includes shard key prefix. Scatter-gather query does not.
