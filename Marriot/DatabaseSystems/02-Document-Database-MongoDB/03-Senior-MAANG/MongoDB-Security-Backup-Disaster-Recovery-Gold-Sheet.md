    # MongoDB Security, Backup and Disaster Recovery - Gold Sheet

    > **Track File #15 of 28 - Group 03: Senior MAANG**
    > For: backend/database/system design interviews | Level: production readiness | Mode: security controls, backups, RPO/RTO, restore drills

    This sheet builds:
    - Authentication, authorization, TLS, encryption, auditing
- mongodump/mongorestore, snapshots, Atlas backups
- RPO, RTO, restore testing, DR mistakes

Original master-map sections included here:
- 14. Security
- 15. Backup, Restore, and Disaster Recovery

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 14. Security

### Authentication

Enable authentication in production. Use database users, not unauthenticated deployments.

```javascript
db.createUser({
  user: "orders_app",
  pwd: passwordPrompt(),
  roles: [{ role: "readWrite", db: "orders" }]
})
```

### Authorization and Least Privilege

Use roles narrowly:

| Actor | Role Pattern |
|---|---|
| App service | Read/write only its database |
| Analytics job | Read specific collections |
| Migration job | Temporary elevated role |
| Admin | Separate audited admin account |

Avoid sharing admin credentials with apps.

### SCRAM

SCRAM is MongoDB's common username/password authentication mechanism. Use strong passwords and rotate credentials.

### x.509 Basics

x.509 uses client certificates for authentication. Useful in stricter enterprise environments with certificate infrastructure.

### TLS/SSL

Use TLS for network encryption.

Connection string:

```text
mongodb+srv://app_user:<password>@cluster.example.mongodb.net/appdb?tls=true
```

### IP Allowlist and Network Security

- In Atlas, restrict network access by IP/CIDR or private connectivity.
- In self-managed deployments, bind to private interfaces.
- Use firewalls/security groups.
- Avoid public unauthenticated MongoDB.

### Encryption

| Type | Meaning |
|---|---|
| Encryption in transit | TLS between client and server |
| Encryption at rest | Disk/storage encryption |
| Field-level encryption | Sensitive fields encrypted client-side |
| Queryable encryption | Query selected encrypted fields with constraints |

### Auditing

Audit:

- admin actions
- user/role changes
- failed auth attempts
- schema/index changes
- unusual data access patterns

### Secret Management

Do not hardcode connection strings.

Use:

- environment variables for local dev
- cloud secret managers
- Kubernetes secrets with external secret operators
- rotation policies

Bad:

```javascript
const uri = "mongodb+srv://admin:password123@cluster/...";
```

Good:

```javascript
const uri = process.env.MONGODB_URI;
```

### Secure Connection Checklist

- Authentication enabled.
- TLS enabled.
- Least-privilege users.
- IP allowlist or private network.
- No credentials in code or logs.
- Backups encrypted.
- Audit logs enabled for sensitive systems.
- Field encryption for high-risk PII.
- Regular dependency and driver updates.
- Alerts on auth failures and unusual traffic.

### Local Dev vs Production

| Area | Local | Production |
|---|---|---|
| Auth | May be simplified | Required |
| TLS | Often disabled | Required |
| Users | Developer user | Least-privilege service users |
| Network | localhost | private network / allowlist |
| Secrets | `.env` excluded from git | secret manager |
| Backups | Optional | tested and monitored |

---

---

## 15. Backup, Restore, and Disaster Recovery

### Backup Types

| Backup Type | Use |
|---|---|
| `mongodump` | Logical backup of BSON data |
| `mongorestore` | Restore logical dump |
| Filesystem snapshot | Faster large backups when coordinated correctly |
| Atlas backup | Managed scheduled backups |
| Point-in-time restore | Restore to specific time using backup plus oplog |
| Oplog backup | Captures changes between snapshots |

### `mongodump`

```bash
mongodump --uri="mongodb://admin:secret@localhost:27017/appdb?authSource=admin" --out=./backup
```

Specific collection:

```bash
mongodump --uri="$MONGODB_URI" --db=appdb --collection=orders --out=./backup
```

### `mongorestore`

```bash
mongorestore --uri="$MONGODB_URI" ./backup
```

Drop before restore:

```bash
mongorestore --uri="$MONGODB_URI" --drop ./backup
```

Be careful with `--drop` in shared environments.

### RPO and RTO

| Term | Meaning | Example |
|---|---|---|
| RPO | Maximum acceptable data loss | 5 minutes |
| RTO | Maximum acceptable recovery time | 30 minutes |

Backup strategy should be chosen from business requirements, not vibes.

### Disaster Recovery Patterns

- regular snapshots
- point-in-time restore
- cross-region backups
- delayed secondary for human error protection
- archive cold data to object storage
- run restore drills
- document recovery runbooks

### Restore Testing

A backup that has never been restored is a hope, not a recovery plan.

Test:

1. Restore to isolated environment.
2. Verify document counts.
3. Verify indexes.
4. Run application smoke tests.
5. Check data consistency invariants.
6. Measure restore duration.

### Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| No restore tests | Backups may be unusable | Scheduled restore drills |
| Backup same region only | Regional outage risk | Cross-region copies |
| No PITR for critical data | Cannot recover from bad deploy time | Enable PITR/continuous backup |
| Ignoring indexes after restore | Slow app after restore | Validate indexes |
| Credentials in backup scripts | Secret leakage | Secret manager |

---

---
