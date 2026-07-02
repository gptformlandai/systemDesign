# Java JDBC, Transactions, And Connection Pooling Gold Sheet

Target: pure Java database access fluency before frameworks. This sheet explains what Spring Data JPA and JdbcTemplate eventually wrap.

---

## 1. Intuition

JDBC is Java's low-level contract for talking to relational databases.

```text
Java code
    -> JDBC API
JDBC driver
    -> database wire protocol
Database
    -> SQL execution, transactions, locks, results
```

Simple analogy:

```text
JDBC is the electrical socket. The database driver is the plug adapter. The connection pool
keeps a small number of sockets ready so each request does not open a brand-new connection.
```

---

## 2. Definition

- Definition: JDBC is the Java Database Connectivity API for executing SQL and reading relational results from Java.
- Category: Java platform API and backend data access.
- Core idea: Use `Connection`, `PreparedStatement`, `ResultSet`, and transactions safely; use a pool for production.

---

## 3. Why It Exists

Java applications need a standard way to connect to different relational databases without rewriting app code for every vendor protocol.

Naive approach:

- Open a new database connection for every request.
- Build SQL with string concatenation.
- Forget to close resources.
- Let every statement auto-commit independently.

What breaks:

- Slow request latency from connection creation.
- SQL injection risk.
- Connection leaks.
- Partial writes when one operation succeeds and another fails.
- Database overload from too many active connections.

---

## 4. Reality

In real backend systems:

- Spring `JdbcTemplate`, JPA, Hibernate, MyBatis, and jOOQ all sit above JDBC.
- HikariCP is a common production connection pool.
- JDBC drivers are usually Maven `runtime` dependencies.
- Transactions are enforced by the database, not by Java collections.
- Pure JDBC still matters for debugging connection pools, transaction boundaries, driver issues, and SQL injection.

Interview line:

```text
Even if I use JPA or Spring repositories, I know the JDBC layer underneath: connections,
prepared statements, result sets, transaction boundaries, driver behavior, and pool limits.
```

---

## 5. How It Works

### Query Flow

1. Application asks a `DataSource` for a `Connection`.
2. The pool gives an existing connection or waits until one is available.
3. Application creates a `PreparedStatement`.
4. Application binds parameters.
5. Database executes SQL.
6. Application reads a `ResultSet`.
7. Application closes resources.
8. Pool returns the connection to idle state.

### Transaction Flow

1. Disable auto-commit.
2. Execute multiple statements.
3. Commit if all succeed.
4. Roll back on failure.
5. Restore connection state before returning to pool.

### Failure Path

1. Query is slow or transaction hangs.
2. Connection stays checked out.
3. Pool active count reaches max.
4. New requests wait for a connection.
5. API p99 latency spikes.

### Recovery Path

1. Inspect pool active/idle/pending metrics.
2. Check slow query logs and database locks.
3. Check application transaction duration.
4. Add timeouts.
5. Fix connection leaks with try-with-resources.
6. Tune pool only after proving the database can handle more concurrency.

---

## 6. What Problem It Solves

- Primary problem solved: Java code can execute SQL against relational databases through a standard API.
- Secondary benefits: vendor driver abstraction, parameter binding, transaction control.
- Systems impact: correctness moves from in-memory state to durable, transactional storage.

---

## 7. When To Rely On It

JDBC is a strong fit when:

- You need direct SQL control.
- You are debugging what JPA/Hibernate actually sends to the database.
- You need simple data access without ORM complexity.
- You are writing infrastructure, migrations, health checks, or diagnostic scripts.
- Interviewer asks how Java talks to a database under the hood.

Keywords:

- `PreparedStatement`
- connection leak
- transaction isolation
- auto-commit
- batch update
- HikariCP
- SQL injection
- `ResultSet`

---

## 8. When Not To Use It

Avoid hand-written JDBC for every feature when:

- Object mapping is complex.
- The team standard is JPA, MyBatis, jOOQ, or Spring JdbcTemplate.
- You need generated type-safe SQL.
- The project needs consistent transaction management across many services.

Better alternatives:

| Need | Consider |
|---|---|
| Simple SQL in Spring | `JdbcTemplate` |
| Object graph persistence | JPA/Hibernate |
| Type-safe SQL | jOOQ |
| Migrations | Flyway or Liquibase |
| Reactive database access | R2DBC, with clear trade-offs |

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Direct SQL control | More boilerplate |
| Clear transaction boundaries | Manual mapping |
| Easy to reason about performance | Easy to leak resources if careless |
| Works across databases through drivers | Vendor SQL differences remain |
| Great for understanding frameworks | Not as productive for large domain models |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- JDBC is explicit and fast to understand, but verbose.
- ORM is productive, but can hide N+1 queries and transaction boundaries.
- Larger connection pools increase concurrency until the database saturates; then they increase queuing and contention.
- Auto-commit is simple for single statements, but wrong for multi-step business invariants.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| String-concatenated SQL | SQL injection | Use `PreparedStatement` parameters |
| Not closing resources | Connection leak | Use try-with-resources |
| One connection per request without pooling | Expensive and slow | Use `DataSource` with pool |
| Huge pool size | Overloads database | Size based on DB capacity and latency |
| Long transactions | Locks held too long | Keep transactions short |
| Catching `SQLException` and continuing | Partial/invisible failure | Roll back and surface meaningful error |

---

## 11. Key Numbers

| Item | Typical Range / Rule |
|---|---|
| Connection creation | Expensive compared with borrowing from pool |
| Common app instance pool size | Often 5-30, workload dependent |
| Query timeout | Usually seconds, not minutes, for user APIs |
| Transaction duration | As short as possible |
| Batch insert size | Often 50-1000, driver and DB dependent |
| Pool wait timeout | Must be lower than upstream request timeout |
| DB max connections | Shared by all app instances and admin tools |

Rule:

```text
Do not tune pool size in isolation. App instances * pool size must fit database capacity.
```

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| SQL injection | Unauthorized data access | Concatenated SQL | Prepared statements and allowlists |
| Pool exhaustion | Slow/timeouts | Leaks, slow queries, long transactions | Metrics, timeouts, leak detection |
| Deadlock | Transaction rollback | Conflicting lock order | Retry safe operations and fix lock ordering |
| Dirty/non-repeatable reads | Inconsistent results | Weak isolation | Choose correct isolation level |
| Batch partial failure | Some rows fail | Constraint or data issue | Transaction, batch error handling |
| Driver missing | App cannot connect | Runtime dependency absent | Correct Maven/Gradle scope |
| Auto-commit trap | Partial update | Multiple writes not atomic | Explicit transaction |

---

## 13. Scenario

- Product / system: Room booking API.
- Why this concept fits: booking correctness must be enforced with database transactions and constraints, not only `ConcurrentHashMap`.
- What would go wrong without it: two service instances can both believe a room is free and create duplicate bookings.

---

## 14. Code Sample

```java
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingRepository {
    private final DataSource dataSource;

    public BookingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public BigDecimal totalAmountForUser(String userId) throws SQLException {
        String sql = """
            select coalesce(sum(amount), 0)
            from bookings
            where user_id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
                return BigDecimal.ZERO;
            }
        }
    }
}
```

Key points:

- `?` parameters prevent SQL injection.
- try-with-resources closes `ResultSet`, `PreparedStatement`, and returns the connection.
- `BigDecimal` is better than `double` for money.

---

## 15. Mini Program / Simulation

Transaction skeleton:

```java
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransferService {
    private final DataSource dataSource;

    public TransferService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void transfer(String fromAccount, String toAccount, long cents) throws SQLException {
        String debit = "update account set balance_cents = balance_cents - ? where id = ?";
        String credit = "update account set balance_cents = balance_cents + ? where id = ?";

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement debitStatement = connection.prepareStatement(debit);
                 PreparedStatement creditStatement = connection.prepareStatement(credit)) {

                debitStatement.setLong(1, cents);
                debitStatement.setString(2, fromAccount);
                debitStatement.executeUpdate();

                creditStatement.setLong(1, cents);
                creditStatement.setString(2, toAccount);
                creditStatement.executeUpdate();

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
```

Debrief:

1. What happens if the credit succeeds but debit fails?
2. Why does auto-commit need to be disabled?
3. Why restore connection state before returning it to the pool?
4. What database constraint would protect against negative balances?

---

## 16. Practical Question

> A Java API becomes slow under traffic. Thread dumps show many request threads waiting for a JDBC connection. What do you check before increasing the pool size?

---

## 17. Strong Answer

I would not blindly increase the pool. First I would check Hikari metrics: active, idle, pending, timeout count, and acquisition latency. Then I would inspect slow query logs, transaction duration, database CPU, lock waits, and whether code leaks connections by missing try-with-resources. I would also compare app instance count times pool size against database max connections. If queries are slow or transactions are too long, a larger pool can make the database worse. The fix may be query tuning, shorter transactions, indexes, connection leak fixes, timeouts, or concurrency limits. Only after proving the DB has capacity would I adjust pool size.

---

## 18. Revision Notes

- One-line summary: JDBC is the Java layer for SQL; safe usage means prepared statements, short transactions, and pooled connections.
- Three keywords: `DataSource`, `PreparedStatement`, transaction.
- One interview trap: `ConcurrentHashMap` does not replace database transactions for multi-instance correctness.
- One memory trick: connection is borrowed, statement is prepared, result is read, transaction commits, connection returns.
