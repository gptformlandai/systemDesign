# MongoDB Security Checklist

## Local Lab

The local lab uses demo credentials:

```text
root/rootpass
app/app_password
```

These are intentionally simple for local learning only.

## Production Authentication

- Authentication enabled.
- No anonymous access.
- Separate users per service.
- Admin users are separate from app users.
- Strong password or certificate-based auth.
- Credential rotation plan exists.

## Authorization

- App user has only required database permissions.
- Analytics user is read-only.
- Migration user is temporary.
- Admin/break-glass access is audited.

## Network

- TLS enabled.
- No public unrestricted access.
- IP allowlist or private networking.
- Kubernetes/cloud security groups are restrictive.
- Compass/admin access is limited.

## Secrets

- No connection strings in code.
- Secrets come from environment or secret manager.
- Logs redact credentials.
- CI/CD secrets are scoped.

## Data Protection

- Encryption at rest enabled.
- Backups encrypted.
- Field-level encryption considered for sensitive PII.
- Queryable encryption considered where supported and justified.

## Auditing

Audit:

- failed logins
- user/role changes
- admin commands
- index/schema changes
- unusual data access

## Tenant Safety

For multi-tenant systems:

- every tenant-owned collection has `tenantId`
- every repository method enforces `tenantId`
- tests fail if tenant filter is missing
- unique indexes are tenant-scoped
- RAG/vector queries include tenant and ACL filters

## Interview Answer

MongoDB security is layered: authentication, authorization, TLS, network controls, secret management, encryption, auditing, backups, and tenant isolation. The most common production mistakes are public exposure, shared admin credentials, credentials in code, and missing tenant filters.
