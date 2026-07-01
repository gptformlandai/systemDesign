# Security Cheat Sheet

## Production Baseline

- Enable authentication.
- Use least-privilege users.
- Use TLS in transit.
- Restrict network access.
- Store credentials in a secret manager.
- Encrypt backups and storage.
- Audit privileged operations.
- Rotate credentials.
- Avoid logging connection strings.
- Test restore and incident procedures.

## User Creation

```javascript
db.getSiblingDB('appdb').createUser({
  user: 'orders_app',
  pwd: passwordPrompt(),
  roles: [{ role: 'readWrite', db: 'orders' }]
})
```

## Role Strategy

| Actor | Role |
|---|---|
| API service | readWrite on its own database |
| Analytics job | read on reporting collections |
| Migration job | temporary elevated permissions |
| Admin | MFA-protected audited account |

## Connection String Safety

Bad:

```javascript
const uri = 'mongodb+srv://admin:password@cluster/appdb';
```

Good:

```javascript
const uri = process.env.MONGODB_URI;
```

## Data Protection

| Control | Purpose |
|---|---|
| TLS | Protect data in transit |
| Encryption at rest | Protect disk/snapshot data |
| Field-level encryption | Protect selected sensitive fields client-side |
| Queryable encryption | Query selected encrypted fields with constraints |
| Auditing | Track sensitive/admin activity |

## Local vs Production

Local demo credentials are fine for labs. Production must use private networking, strong credentials, secret rotation, backup policies, monitoring, and least privilege.
