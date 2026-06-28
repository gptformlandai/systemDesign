# Spring Data JPA, Transactions, and HikariCP — Gold Sheet

> Topic: How Spring Boot connects to relational databases — from entity mapping to N+1 traps to connection pool sizing

---

## 1. Intuition

When a Spring Boot service calls the database, something has to translate Java objects to SQL rows and back, manage open connections efficiently (databases can only handle ~100–300 simultaneous connections), and make multi-step operations atomic. Spring Data JPA handles the first, HikariCP handles the second, and `@Transactional` handles the third. Understanding all three — and their interactions — is a MAANG interview staple.

Beginner version:

> JPA maps Java classes to database tables. HikariCP keeps a pool of pre-opened connections so each request doesn't open a new one from scratch. `@Transactional` wraps operations so they either all succeed or all roll back.

---

## 2. Definition

- **JPA (Jakarta Persistence API):** A specification for ORM (Object-Relational Mapping) in Java. Hibernate is the most common implementation.
- **Spring Data JPA:** Spring's abstraction over JPA — generates repository implementations from interface method names, eliminates boilerplate.
- **HikariCP:** The high-performance JDBC connection pool that Spring Boot uses by default since 2.x.
- **`@Transactional`:** Spring AOP-based annotation that wraps a method in a database transaction.

---

## 3. Entity Mapping

```java
@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB auto-increment
    private Long id;

    @Column(name = "hotel_code", nullable = false, unique = true)
    private String hotelCode;

    @Column(nullable = false)
    private String name;

    // One hotel has many bookings
    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

    // Getters/setters omitted for brevity
}

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)         // Don't load hotel unless accessed
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;              // Stored as 'CONFIRMED', not 0/1
}
```

Key annotations:
- `@Entity` → marks this class as a JPA-managed table
- `@Id` → primary key field
- `@GeneratedValue` → how PK is generated (IDENTITY = DB sequence, SEQUENCE = JPA-managed sequence)
- `@Column` → customize column name, constraints
- `@OneToMany`, `@ManyToOne` → relationship mapping
- `@Enumerated(EnumType.STRING)` → store enum by name, not ordinal (ordinal breaks when enum order changes)

---

## 4. Spring Data Repositories

```java
// Extend JpaRepository — Spring generates the implementation at startup
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    // Method name parsed → generates: SELECT * FROM hotels WHERE hotel_code = ?
    Optional<Hotel> findByHotelCode(String hotelCode);

    // AND / OR
    List<Hotel> findByNameContainingIgnoreCase(String namePart);

    // Custom JPQL — reference ENTITY class name, not table name
    @Query("SELECT h FROM Hotel h WHERE h.hotelCode = :code")
    Optional<Hotel> findByCode(@Param("code") String code);

    // Native SQL — use actual table/column names
    @Query(value = "SELECT * FROM hotels WHERE hotel_code = :code",
           nativeQuery = true)
    Optional<Hotel> findByCodeNative(@Param("code") String code);

    // Derived count/delete
    long countByStatus(BookingStatus status);
    void deleteByHotelCode(String hotelCode);
}
```

`JpaRepository<T, ID>` provides:
- `findById`, `findAll`, `save`, `delete`, `deleteById`, `existsById`
- `findAll(Pageable)` → pagination
- `findAll(Sort)` → sorting

---

## 5. The N+1 Problem — MAANG Classic Trap

**Setup:** You want to list all hotels with their bookings.

```java
// NAIVE — triggers N+1
List<Hotel> hotels = hotelRepository.findAll();    // 1 query: SELECT * FROM hotels
for (Hotel hotel : hotels) {
    // LAZY fetch triggers 1 extra query PER HOTEL
    System.out.println(hotel.getBookings().size()); // N queries!
}
// Total queries: 1 + N (where N = number of hotels)
// With 1000 hotels → 1001 database queries
```

**Solution 1: JPQL JOIN FETCH** (most common)

```java
@Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.bookings WHERE h.id IN :ids")
List<Hotel> findWithBookings(@Param("ids") List<Long> ids);
// 1 query with a JOIN — fetches all bookings in one shot
```

**Solution 2: `@EntityGraph`** (declarative, no JPQL)

```java
@EntityGraph(attributePaths = {"bookings"})
List<Hotel> findAll();
// Spring generates the LEFT JOIN FETCH automatically
```

**Solution 3: `@BatchSize`** (Hibernate-specific)

```java
@BatchSize(size = 30)   // Hibernate batches N lazy loads into: SELECT * FROM bookings WHERE hotel_id IN (...)
@OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
private List<Booking> bookings;
// Not 1 query, but ceil(N/30) queries instead of N queries
```

**Interview answer:** "The N+1 problem occurs when JPA loads a list of N entities and then issues N additional queries to fetch a lazily-loaded association for each. The fix is JOIN FETCH in JPQL or `@EntityGraph` to load the association in a single query."

---

## 6. Lazy vs Eager Loading

| | LAZY | EAGER |
|---|---|---|
| When loaded | On first access of the collection | Always, at entity load time |
| Default for | `@OneToMany`, `@ManyToMany` | `@ManyToOne`, `@OneToOne` |
| Risk | LazyInitializationException outside transaction | Always loads even when not needed → memory + query overhead |
| Fix for LazyInit | Fetch within transaction, or use JOIN FETCH | Switch to LAZY where eager is expensive |

```java
// LazyInitializationException — the classic mistake
Hotel hotel = hotelRepository.findById(1L).get();
// Transaction closes here (repository method completes)
hotel.getBookings().size();  // LazyInitializationException! Hibernate session closed
```

Fix: either fetch within the same transaction, use JOIN FETCH/EntityGraph, or use a DTO projection.

---

## 7. `@Transactional` — Propagation Behaviors

```java
@Service
public class BookingService {

    @Transactional                          // default: REQUIRED
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepo.findById(id).get();
        booking.setStatus(BookingStatus.CONFIRMED);
        paymentService.chargeCard(booking);  // called within SAME transaction
        return bookingRepo.save(booking);
    }
}
```

**Propagation types:**

| Propagation | Behavior | Use case |
|---|---|---|
| `REQUIRED` (default) | Join existing transaction; create new if none | Standard service methods |
| `REQUIRES_NEW` | Suspend current transaction; start a new one | Audit log — must commit even if caller rolls back |
| `SUPPORTS` | Join if exists; run non-transactionally if not | Read-only methods where transaction is optional |
| `NOT_SUPPORTED` | Suspend transaction; run non-transactionally | Calling a heavy operation that shouldn't be part of any TX |
| `NEVER` | Throw exception if called within a transaction | Utility methods that must never run in a TX |
| `MANDATORY` | Join existing; throw exception if none exists | Enforce that caller must provide a transaction |
| `NESTED` | Create a savepoint inside the existing transaction | Partial rollback scenarios (Hibernate-specific) |

**REQUIRES_NEW example (audit log):**

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logAuditEvent(String action, Long entityId) {
    // This commits regardless of whether the calling method rolls back
    AuditEvent event = new AuditEvent(action, entityId, Instant.now());
    auditRepo.save(event);
}
```

---

## 8. Isolation Levels

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // most common
public BigDecimal getHotelRevenue(Long hotelId) { ... }
```

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Use case |
|---|---|---|---|---|
| `READ_UNCOMMITTED` | Possible | Possible | Possible | Almost never — allows reading uncommitted data |
| `READ_COMMITTED` | Prevented | Possible | Possible | Most applications (PostgreSQL default) |
| `REPEATABLE_READ` | Prevented | Prevented | Possible | Financial summaries requiring consistent reads |
| `SERIALIZABLE` | Prevented | Prevented | Prevented | Audit trails, legal ledgers — high lock contention |

- **Dirty read:** Reading data another transaction hasn't committed yet
- **Non-repeatable read:** Reading the same row twice and getting different values (another TX committed in between)
- **Phantom read:** A range query returns different rows on second read (another TX inserted/deleted rows)

Most microservices run `READ_COMMITTED` (default for PostgreSQL, SQL Server). Rarely need to go higher — use optimistic locking instead.

---

## 9. HikariCP — Connection Pool Sizing

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://db-host:5432/marriott
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      pool-name: HikariPool-Marriott
      maximum-pool-size: 10           # Max connections to the DB
      minimum-idle: 5                 # Keep 5 connections warm always
      connection-timeout: 30000       # 30s max wait to borrow connection
      idle-timeout: 600000            # 10min — evict idle connections
      max-lifetime: 1800000           # 30min — rotate connections (prevent stale)
      keepalive-time: 60000           # 60s — test idle connections with keepalive query
      connection-test-query: SELECT 1 # Only for drivers without JDBC 4 isValid()
      leak-detection-threshold: 5000  # Warn if connection held > 5s (catches connection leaks)
```

**Pool sizing formula (from HikariCP author):**

```
pool_size = (core_count × 2) + effective_spindle_count

For an 8-core machine with SSD (spindle count = 1):
  pool_size = (8 × 2) + 1 = 17 → round to 10–20

For an 8-core machine with 2 spinning disks:
  pool_size = (8 × 2) + 2 = 18
```

Counterintuitively: **more connections ≠ faster.** Database connection management overhead and lock contention mean pool sizes > 20–30 often hurt performance. The formula is a starting point — benchmark and tune.

**Connection lifecycle in a request:**

```
Request arrives
    → Thread borrows connection from pool (waits up to connection-timeout)
    → @Transactional begins → JDBC connection.setAutoCommit(false)
    → SQL queries execute
    → @Transactional ends → connection.commit() or rollback()
    → Thread returns connection to pool (NOT closed — stays in pool for next request)
```

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Forgetting `@Transactional` on service methods that do multiple writes | Partial update — half the changes commit, exception leaves DB in inconsistent state | Always annotate write operations with `@Transactional` |
| Calling `@Transactional` method from within the same class | Spring AOP proxy is bypassed — no transaction starts | Extract to a separate bean, or use `self` injection |
| EAGER fetch on `@OneToMany` | Every query loads thousands of child rows even when unused | Use LAZY; fetch explicitly with JOIN FETCH when needed |
| Setting `maximum-pool-size` to 100+ | DB connection limit hit; connection exhaustion | Use the formula; typically 10–20 per application instance |
| Using `@Enumerated(EnumType.ORDINAL)` | Inserting a new enum value in the middle breaks all persisted data | Always use `EnumType.STRING` |
| `@Transactional` on `private` methods | AOP proxy cannot intercept private methods → no transaction | Annotate only public methods |

---

## 11. Interview Insight

Strong answer:

> Spring Data JPA uses Hibernate as the JPA provider. Entities map Java classes to tables. Spring Data repositories generate queries from method names or JPQL. `@Transactional` wraps methods using Spring AOP — propagation defines how nested transactions interact; `REQUIRED` joins existing, `REQUIRES_NEW` suspends and starts fresh. The N+1 problem is the most common JPA performance issue: loading a list of N entities then lazily fetching a collection per entity causes N+1 queries — fixed with JOIN FETCH or EntityGraph. HikariCP is the connection pool — pool size should be `(cores × 2) + spindles`, typically 10–20 per instance.

Follow-up trap:

> Why doesn't adding more connections always improve throughput?

Good answer:

> Databases have a fixed thread pool. Each connection competes for CPU and lock resources. Above the database's thread count, additional connections queue — they don't run in parallel. Connection context switching adds overhead. HikariCP's author's research showed pool sizes of 10–15 outperformed 100+ in benchmarks. The bottleneck is database CPU, not connection availability.

---

## 12. Revision Notes

- One-line summary: Spring Data JPA maps entities to tables and generates repository SQL; `@Transactional` controls atomicity and propagation; HikariCP pools connections with a pool size formula of `(cores×2)+spindles`.
- Three keywords: N+1 problem, transaction propagation, connection pool sizing.
- One interview trap: calling `@Transactional` from within the same class bypasses the AOP proxy — the transaction never starts.
- Memory trick: N+1 kills performance silently — always check Hibernate SQL logs with `spring.jpa.show-sql=true` in development.
