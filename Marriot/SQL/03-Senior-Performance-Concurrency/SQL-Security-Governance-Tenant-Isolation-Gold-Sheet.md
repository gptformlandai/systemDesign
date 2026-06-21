# SQL Security Governance Tenant Isolation Gold Sheet

Target: backend and MAANG interviews where SQL correctness includes security, privacy, tenant isolation, and safe data access.

This sheet fills the SQL security/governance layer: SQL injection, prepared statements, least privilege, grants, row-level security, tenant filters, PII, masking, encryption, auditing, and safe migrations.

---

## 0. Security Mindset

A database is not only storage. It is a trust boundary.

```text
user input -> application authorization -> SQL shape -> database permissions -> tenant/data constraints -> audit trail
```

Strong answer:

```text
I protect SQL access in layers: prepared statements for injection safety, application-level
authorization, least-privilege database roles, tenant-aware query and index design, database
constraints for invariants, and audit logs for sensitive access.
```

---

# 1. SQL Injection

SQL injection happens when untrusted input becomes executable SQL structure.

Dangerous:

```java
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
```

Safe:

```java
jdbcTemplate.query(
    "SELECT * FROM users WHERE email = ?",
    userRowMapper,
    email
);
```

Prepared statement principle:

```text
SQL text and user values are sent separately, so user input is treated as data, not code.
```

---

# 2. Dynamic SQL Safely

Some parts cannot be parameterized as values:

- table names
- column names
- sort direction
- SQL keywords

Dangerous:

```sql
ORDER BY :user_selected_column
```

Safe allowlist pattern:

```java
Map<String, String> allowedSorts = Map.of(
    "createdAt", "created_at",
    "amount", "total_amount",
    "status", "status"
);

String sortColumn = allowedSorts.getOrDefault(request.sortBy(), "created_at");
String direction = request.desc() ? "DESC" : "ASC";
String sql = "SELECT * FROM orders ORDER BY " + sortColumn + " " + direction + " LIMIT ?";
```

Interview line:

```text
Values use bind parameters. SQL identifiers use strict allowlists, never raw user input.
```

---

# 3. Least Privilege Database Roles

Avoid giving app users owner/superuser privileges.

Example roles:

```sql
CREATE ROLE booking_app LOGIN PASSWORD '...';
CREATE ROLE booking_readonly LOGIN PASSWORD '...';

GRANT CONNECT ON DATABASE booking_db TO booking_app;
GRANT USAGE ON SCHEMA public TO booking_app;
GRANT SELECT, INSERT, UPDATE ON bookings, payments TO booking_app;
GRANT SELECT ON bookings, payments TO booking_readonly;
```

Guidelines:

- app role gets only required tables/actions
- read-only role for dashboards/support tools
- migration role separated from runtime app role
- no superuser for application traffic
- rotate credentials through secret manager

---

# 4. Runtime Role vs Migration Role

Runtime role:

```text
SELECT/INSERT/UPDATE needed by app business flows
```

Migration role:

```text
DDL rights to create/alter/drop tables/indexes/constraints
```

Why separate:

- app compromise cannot modify schema
- DDL is controlled through deployment pipeline
- audit trail is clearer

Strong answer:

```text
The application should not run as schema owner. Runtime and migration privileges should be
separate.
```

---

# 5. Tenant Isolation Models

| Model | Pros | Risks |
|---|---|---|
| shared tables with `tenant_id` | efficient, simple operations | query bugs can leak data |
| schema per tenant | stronger isolation | migration/operations complexity |
| database per tenant | strongest isolation | cost, provisioning, cross-tenant analytics complexity |

Shared table baseline:

```sql
CREATE TABLE bookings (
    tenant_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (tenant_id, booking_id)
);
```

Tenant-aware index:

```sql
CREATE INDEX idx_bookings_tenant_customer_created
ON bookings (tenant_id, customer_id, created_at DESC);
```

Rule:

```text
Tenant ID belongs in primary keys, foreign keys, unique constraints, and indexes for tenant-owned data.
```

---

# 6. Tenant Filter Trap

Dangerous query:

```sql
SELECT *
FROM bookings
WHERE booking_id = :booking_id;
```

Safer query:

```sql
SELECT *
FROM bookings
WHERE tenant_id = :tenant_id
  AND booking_id = :booking_id;
```

Dangerous cache key:

```text
booking:{booking_id}
```

Safer cache key:

```text
tenant:{tenant_id}:booking:{booking_id}
```

Interview line:

```text
Tenant isolation is not only a WHERE clause. It must shape keys, constraints, indexes, cache
keys, search filters, and audit logs.
```

---

# 7. Row-Level Security

PostgreSQL Row-Level Security can enforce per-row access policies inside the database.

Enable:

```sql
ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;
```

Policy idea:

```sql
CREATE POLICY tenant_isolation_policy
ON bookings
USING (tenant_id = current_setting('app.tenant_id')::bigint);
```

Set tenant context for transaction/session:

```sql
SET LOCAL app.tenant_id = '42';
```

When useful:

- extra safety for shared tables
- multiple tools access same database
- strong tenant isolation needs defense in depth

Traps:

- must set tenant context correctly
- superusers/table owners may bypass unless configured carefully
- can complicate debugging/performance
- still need application authorization

---

# 8. PII Classification

Classify data before designing access.

| Data Type | Examples | Handling |
|---|---|---|
| public | product name | normal controls |
| internal | order counts | role-based access |
| confidential | email, phone, address | minimization, masking, audit |
| sensitive | payment tokens, government IDs | encryption/tokenization, strict access |

Design rule:

```text
Do not store data you do not need. Do not expose raw PII where masked data is enough.
```

---

# 9. Masking And Minimization

Support tool example:

```sql
SELECT
    customer_id,
    CONCAT(SUBSTRING(email FROM 1 FOR 2), '***@', SPLIT_PART(email, '@', 2)) AS masked_email,
    status,
    created_at
FROM customers
WHERE customer_id = :customer_id;
```

Better pattern:

- create views for support roles
- expose masked columns
- avoid giving direct table access

Example view:

```sql
CREATE VIEW support_customers AS
SELECT
    customer_id,
    CONCAT(SUBSTRING(email FROM 1 FOR 2), '***@', SPLIT_PART(email, '@', 2)) AS masked_email,
    created_at
FROM customers;

GRANT SELECT ON support_customers TO support_readonly;
```

---

# 10. Encryption And Tokenization

Common layers:

| Layer | Use |
|---|---|
| TLS in transit | protect network traffic |
| disk encryption | protect storage media |
| column/application encryption | protect sensitive fields from broad DB exposure |
| tokenization | replace sensitive values with non-sensitive tokens |

Interview line:

```text
Database encryption at rest is useful, but it does not replace least privilege, masking,
auditing, and application-level protection for highly sensitive fields.
```

---

# 11. Audit Logging

Audit sensitive actions:

- login/security changes
- payment/refund actions
- PII access
- admin/support data access
- permission changes
- export/report generation

Audit table shape:

```sql
CREATE TABLE audit_events (
    audit_event_id BIGINT PRIMARY KEY,
    actor_user_id BIGINT,
    actor_role TEXT NOT NULL,
    tenant_id BIGINT,
    action TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    request_id TEXT,
    ip_address TEXT
);
```

Rules:

- append only
- include actor and reason/context when possible
- do not store secrets in audit payload
- retention policy must be explicit

---

# 12. Soft Delete Security Trap

Soft delete pattern:

```sql
ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMP;
```

Dangerous:

```sql
SELECT * FROM customers;
```

Safer:

```sql
SELECT *
FROM customers
WHERE deleted_at IS NULL;
```

Even safer for common reads:

```sql
CREATE VIEW active_customers AS
SELECT *
FROM customers
WHERE deleted_at IS NULL;
```

Trap:

```text
Soft-deleted rows are still present. They must be excluded from normal queries, search,
analytics, exports, and support tools unless explicitly needed.
```

---

# 13. Secure Reporting And Exports

Risks:

- broad table scans exposing PII
- exports bypass application permissions
- support/reporting roles too powerful
- tenant filter missing in dashboard SQL
- stale copies retained forever

Controls:

- read-only reporting role
- masked views
- tenant-scoped reports
- export audit log
- retention and deletion rules
- row limits for ad-hoc tools
- approval flow for sensitive exports

---

# 14. SQL Injection Interview Scenario

Prompt:

```text
A search API accepts sortBy, direction, and filter text. Make it safe.
```

Strong answer:

```text
Filter values are bind parameters. Sort columns and directions are not passed as raw input;
I map them through an allowlist. I add pagination limits, avoid SELECT *, and test malicious
inputs. At the database layer, the app role has only necessary table privileges.
```

---

# 15. Tenant Leak Interview Scenario

Prompt:

```text
Tenant A can see Tenant B orders after a new reporting query ships.
```

Strong response:

1. Stop/disable the report or route.
2. Identify affected tenants and rows.
3. Audit access logs and query text.
4. Patch query with tenant predicate from trusted context.
5. Add tenant-aware indexes if needed.
6. Add tests that fail without tenant filter.
7. Review cache/report/search paths.
8. Consider RLS or scoped views for defense in depth.

---

# 16. Permission Review Checklist

For every database role:

- Can it connect only where needed?
- Can it access only required schemas?
- Does it have DDL rights accidentally?
- Can it read PII tables directly?
- Can it export large datasets?
- Are grants reviewed in code?
- Are credentials rotated?
- Is access audited?

---

# 17. Governance Checklist For Schema Design

Ask:

1. Which columns are PII?
2. Which tables are tenant-owned?
3. What must be unique per tenant vs globally?
4. Which actions need audit history?
5. Which fields need masking in support views?
6. Which roles need read vs write vs DDL?
7. What is retention/deletion behavior?
8. How do migrations avoid exposing data accidentally?

---

# 18. Common Mistakes

| Mistake | Better Approach |
|---|---|
| string-concatenate user input | bind parameters and allowlists |
| app runs as schema owner | separate runtime and migration roles |
| tenant filter only in UI | enforce in service/query/database layers |
| unique key misses tenant_id | tenant-aware unique constraints |
| support gets raw table access | masked views and audited access |
| logs contain PII/secrets | sanitize logs and audit payloads |
| soft delete forgotten in reports | views/policies/tests for active rows |
| encryption treated as full solution | combine with least privilege and auditing |

---

# 19. Final Rapid Revision

- Prepared statements stop values from becoming SQL code.
- Dynamic identifiers require allowlists.
- Runtime app role should not own schema.
- Tenant ID must shape keys, FKs, constraints, indexes, queries, cache, and reports.
- RLS is defense in depth, not a replacement for application authorization.
- PII needs minimization, masking, access control, retention, and audit.
- Soft-deleted data is still data.
- Security is strongest when app and database constraints reinforce each other.

---

# 20. Official Source Notes

- PostgreSQL Row Security Policies: https://www.postgresql.org/docs/current/ddl-rowsecurity.html
- PostgreSQL GRANT: https://www.postgresql.org/docs/current/sql-grant.html
- OWASP SQL Injection: https://owasp.org/www-community/attacks/SQL_Injection
