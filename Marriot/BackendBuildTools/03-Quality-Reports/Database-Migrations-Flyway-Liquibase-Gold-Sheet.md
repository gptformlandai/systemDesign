# Database Migrations: Flyway and Liquibase — Gold Sheet

> Topic: schema versioning, migration lifecycle, Spring Boot integration, CI patterns, zero-downtime strategies

---

## 1. Intuition

Your application code and your database schema must change together. Without migration tooling, schema changes are manual, error-prone, and environment-inconsistent. Flyway and Liquibase automate this: every schema change is a versioned file tracked in source control, applied in order, and recorded in the database so they never run twice.

Beginner version:

> Migration tools apply SQL changes in version order and remember which changes have already run.

---

## 2. Definition

- Definition: Database migration tools (Flyway, Liquibase) manage versioned, incremental schema changes that are applied automatically on startup, tracked in the database, and applied in repeatable order across all environments.
- Category: Database lifecycle management.
- Core idea: Schema version in sync with application version — never drift.

---

## 3. Core Mental Model

```
Source control
  migrations/
    V1__create_orders_table.sql
    V2__add_customer_index.sql
    V3__add_payment_status_column.sql

On startup:
  1. Connect to DB
  2. Read flyway_schema_history table (or create it)
  3. Find unapplied migrations (version > last applied)
  4. Apply in version order
  5. Record each in history table
  6. Application continues starting up
```

```sql
-- flyway_schema_history (auto-managed)
version | description               | success | installed_on
1       | create orders table       | true    | 2026-01-01 09:00
2       | add customer index        | true    | 2026-01-15 10:30
3       | add payment status column | true    | 2026-06-28 14:00
```

---

## 4. Flyway Setup — Spring Boot

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- For PostgreSQL: -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration     # where SQL files live
    baseline-on-migrate: false            # true only for existing DBs
    validate-on-migrate: true             # verify checksums match
    out-of-order: false                   # reject out-of-order migrations
  jpa:
    hibernate:
      ddl-auto: validate                  # NEVER use create/update in prod with Flyway
```

Spring Boot auto-runs Flyway before the application context finishes starting — before any repository beans are used.

---

## 5. Flyway Migration File Naming

```
db/migration/
  V1__create_orders_table.sql
  V2__create_customers_table.sql
  V3__add_order_status_index.sql
  V3.1__add_customer_email_constraint.sql
  R__create_reporting_view.sql           ← repeatable migration (R prefix)
```

**Naming rules:**
- `V{version}__{description}.sql` — versioned (runs once)
- `R__{description}.sql` — repeatable (runs when checksum changes)
- `U{version}__{description}.sql` — undo (Flyway Pro only)
- Double underscore `__` separates version from description
- Version can be `1`, `1.1`, `1.1.1`, or date-based `20260628`

---

## 6. Example Migration Files

```sql
-- V1__create_orders_table.sql
CREATE TABLE orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    total       DECIMAL(10, 2) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
```

```sql
-- V2__add_payment_reference.sql
ALTER TABLE orders
    ADD COLUMN payment_reference VARCHAR(100);

-- Backfill with a placeholder for existing rows
UPDATE orders SET payment_reference = 'LEGACY-' || id::text WHERE payment_reference IS NULL;
```

```sql
-- V3__add_not_null_constraint.sql
-- Only add NOT NULL after backfill is verified
ALTER TABLE orders
    ALTER COLUMN payment_reference SET NOT NULL;
```

---

## 7. Flyway CLI and Maven Plugin

```bash
# Run migrations manually (useful in CI before integration tests)
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate checksums (detect manual changes to applied migrations)
mvn flyway:validate

# Repair the schema history (remove failed entries)
mvn flyway:repair

# Baseline an existing database (first-time adoption)
mvn flyway:baseline -Dflyway.baselineVersion=5
```

---

## 8. Liquibase Alternative

Liquibase uses XML/YAML/SQL changesets instead of plain SQL files:

```yaml
# db/changelog/db.changelog-master.yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: alice
      changes:
        - createTable:
            tableName: orders
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
              - column:
                  name: total
                  type: DECIMAL(10,2)
                  constraints:
                    nullable: false
  - changeSet:
      id: 2
      author: bob
      changes:
        - addColumn:
            tableName: orders
            columns:
              - column:
                  name: payment_reference
                  type: VARCHAR(100)
```

**Flyway vs Liquibase:**

| Feature | Flyway | Liquibase |
|---|---|---|
| Config format | Plain SQL (preferred) | XML, YAML, JSON, SQL |
| Rollback | Pro only | Built-in (rollback changesets) |
| Complexity | Simple, opinionated | More features, more learning curve |
| Spring Boot auto-config | Yes | Yes |
| Multi-DB support | Good | Excellent |
| Best for | Most backend services | Complex enterprise, rollback requirements |

---

## 9. Zero-Downtime Migration Strategies

**The problem:** your app is deployed with a rolling update. For 2-3 minutes, old and new pod versions run simultaneously. The schema must be compatible with both versions.

**Expand and contract pattern:**

```
Phase 1 — Expand (backward compatible, deploy before new app):
  ALTER TABLE orders ADD COLUMN new_field VARCHAR(100);
  -- Both old and new app ignore the new field harmlessly

Phase 2 — Deploy new app:
  -- New app reads/writes new_field
  -- Old app ignores new_field

Phase 3 — Contract (remove old, after all old pods gone):
  ALTER TABLE orders DROP COLUMN old_field;
```

**Never do in zero-downtime:**
```sql
-- These break old pods still running:
ALTER TABLE orders RENAME COLUMN status TO order_status;  -- rename breaks old queries
ALTER TABLE orders DROP COLUMN status;                     -- drop breaks old reads
ALTER TABLE orders ALTER COLUMN name SET NOT NULL;         -- NOT NULL breaks old inserts that omit the column
```

---

## 10. Testcontainers + Flyway in Integration Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class OrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    // Flyway runs migrations against the container DB before tests
    // Tests see exactly the same schema as production
}
```

---

## 11. CI/CD Integration

```yaml
# GitHub Actions — run Flyway migration before integration tests
- name: Run database migrations
  run: mvn flyway:migrate
  env:
    FLYWAY_URL: jdbc:postgresql://localhost:5432/payments_test
    FLYWAY_USER: test
    FLYWAY_PASSWORD: test

- name: Run integration tests
  run: mvn verify -Pfull-integration

# Production: Spring Boot auto-runs migrations on startup
# Ensure migrations complete before readiness probe turns green
```

---

## 12. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| `ddl-auto: update` with Flyway | Hibernate and Flyway fight over schema control | Always use `validate` when Flyway manages schema |
| Editing an applied migration file | Flyway checksum mismatch — app fails to start | Never modify applied migrations; add a new one |
| NOT NULL constraint added before backfill | Existing rows fail constraint | Add column nullable → backfill → add NOT NULL as separate migration |
| Running migration in test without Testcontainers | H2 ≠ PostgreSQL dialect | Use Testcontainers so Flyway runs against real DB engine |
| Giant mega-migration | Slow, risky, hard to rollback | Break into small incremental migrations |

---

## 13. Interview Insight

Strong answer:

> I use Flyway for database migrations — each schema change is a versioned SQL file in source control, and Flyway applies them in order on startup, recording each in the `flyway_schema_history` table. Spring Boot auto-runs Flyway before any JPA beans are used, so by the time the application context is ready, the schema is up to date. For zero-downtime deployments with rolling updates, I follow the expand-and-contract pattern: add columns as nullable first, backfill data, then add constraints in a subsequent deployment after all old pods are gone. I never rename or drop columns until the old code version is fully retired.

Follow-up trap:

> What happens if a Flyway migration fails halfway through on a production deployment?

Good answer:

> The failed migration is recorded in the history table with `success=false`. Flyway will refuse to run any subsequent migrations until the failure is resolved. The fix depends on whether the database supports transactional DDL (Postgres does — the migration is rolled back automatically; re-run after fixing the SQL). For non-transactional DDL (MySQL), you may need to manually undo the partial change and then run `flyway repair` to remove the failed entry before retrying.

---

## 14. Revision Notes

- One-line summary: Flyway applies versioned SQL migrations in order, recorded in a history table, preventing schema drift across environments.
- Three keywords: version, history, expand-contract.
- One interview trap: never modify an applied migration — add a new one.
- Memory trick: Flyway is version control for your database schema.
