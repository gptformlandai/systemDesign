# Backup, Restore, and Disaster Recovery

## Key Terms

| Term | Meaning |
|---|---|
| RPO | Maximum acceptable data loss |
| RTO | Maximum acceptable recovery time |
| Snapshot | Point-in-time backup of data files/storage |
| Logical backup | BSON dump with `mongodump` |
| PITR | Point-in-time recovery using backups plus oplog/continuous backup |

## Logical Backup

```bash
mongodump --uri="mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true" --out=backups/local-dump
```

## Restore

```bash
mongorestore --uri="mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true" backups/local-dump
```

With drop:

```bash
mongorestore --drop --uri="$MONGODB_URI" backups/local-dump
```

Be careful with `--drop`.

## Atlas Backup Strategy

Define:

- snapshot frequency
- retention period
- PITR requirement
- cross-region backup policy
- restore target environment
- restore drill frequency
- ownership and runbook

## Restore Drill

1. Restore to isolated environment.
2. Verify document counts.
3. Verify indexes.
4. Run app smoke tests.
5. Validate critical invariants.
6. Measure restore time.
7. Update RTO if needed.

## Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| No restore test | Backup may be unusable | Schedule drills |
| Same-region only | Regional outage risk | Cross-region copies |
| No PITR | Cannot recover from bad deploy time | Enable PITR for critical data |
| Ignoring indexes | Slow app after restore | Validate indexes |
| Backup secrets in scripts | Credential leak | Use secret manager |

## Interview Answer

A backup plan is not complete until restore is tested. I define RPO/RTO with the business, choose snapshot/PITR strategy accordingly, store backups securely, run restore drills, and validate data plus indexes after restore.
