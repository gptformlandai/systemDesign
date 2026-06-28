# Spring Data JPA Advanced — Optimistic Locking, N+1 Solutions, Batch, Projections — Gold Sheet

## What This Covers

- Optimistic locking (@Version, StaleObjectStateException, retry patterns)
- Pessimistic locking (SELECT FOR UPDATE, deadlock scenarios, lock timeouts)
- N+1 problem root cause and all solutions (EntityGraph, fetch join, @BatchSize, subselect)
- Batch insert/update operations (JDBC batching, executeBatch strategies)
- Result set projections (interface projections, class projections, constructor expressions)
- Read replicas and routing
- Stateless sessions for high-volume reads
- Read-only transactions and entities

---

## 1. Mental Model

```text
JPA concurrency = Optimistic for read-heavy, Pessimistic for write-heavy critical sections

N+1 = Hibernate executes 1 query to load parent + N queries to load each child's lazy relation
Solution hierarchy:
  1. EntityGraph or fetch join → load together in one query
  2. @BatchSize or subselect → batch the N queries
  3. Projection + DTO → select only what you need

Batch operations:
  Hibernate buffers inserts/updates → flushes as batch → single DB round trip per N rows
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why |
|---|---|---|
| N+1 root cause and solutions | Very high | Every JPA production interview |
| Optimistic vs pessimistic locking | High | Concurrency correctness |
| @Version and StaleObjectStateException | High | Conflict handling design |
| EntityGraph vs fetch join | High | Performance interview question |
| Batch operations | Medium-high | High-volume data loading |
| Interface projections | Medium-high | Lean DTO queries |
| Stateless sessions | Medium | High-throughput read pipelines |
| Read replicas routing | Medium | Scalability design |

---

## 3. Optimistic Locking with @Version

Optimistic locking assumes conflicts are rare. It detects them at commit time.

### Entity Setup

```java
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version; // Hibernate manages this field

    private String status;
    private String guestId;

    // getters/setters
}
```

### How It Works

```sql
-- Hibernate issues an UPDATE with version check:
UPDATE booking
SET status = 'CONFIRMED', version = 2
WHERE id = 1 AND version = 1;

-- If 0 rows affected → another transaction modified the row → StaleObjectStateException
```

### Handling StaleObjectStateException

```java
@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!"CREATED".equals(booking.getStatus())) {
            throw new IllegalStateException("Booking cannot be confirmed");
        }

        booking.setStatus("CONFIRMED");
        return bookingRepository.save(booking);
    }

    @Recover
    public Booking recoverFromConflict(ObjectOptimisticLockingFailureException ex, Long bookingId) {
        throw new BookingConflictException("Booking " + bookingId + " was modified concurrently. Please retry.");
    }
}
```

**Spring Retry dependency:**

```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableRetry
public class Application { ... }
```

### Optimistic Locking with FETCH

```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Lock at query time
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
```

---

## 4. Pessimistic Locking

Pessimistic locking acquires a database lock before reading. Best for critical sections where conflicts are expected (e.g., inventory decrement, seat reservation).

### SELECT FOR UPDATE

```java
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // → SELECT ... FOR UPDATE
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Long id);
}
```

```java
@Transactional(timeout = 5) // Fail if lock not acquired in 5 seconds
public void reserveSeat(Long seatId, String guestId) {
    Seat seat = seatRepository.findByIdWithLock(seatId)
        .orElseThrow(() -> new SeatNotFoundException(seatId));

    if (seat.isReserved()) {
        throw new SeatAlreadyReservedException(seatId);
    }

    seat.setReservedBy(guestId);
    seat.setReserved(true);
    // Lock released when transaction commits
}
```

### Lock Types

| LockModeType | SQL | Use Case |
|---|---|---|
| `PESSIMISTIC_WRITE` | `SELECT ... FOR UPDATE` | Exclusive lock, prevent concurrent writes |
| `PESSIMISTIC_READ` | `SELECT ... FOR SHARE` | Shared read lock, prevent concurrent writes |
| `PESSIMISTIC_FORCE_INCREMENT` | `SELECT ... FOR UPDATE` + version increment | Pessimistic + version tracking |

### Deadlock Prevention

**Order matters**: Always acquire locks in a consistent order across transactions to prevent deadlocks.

```java
// BAD: Transaction A locks seat 1 then seat 2
//      Transaction B locks seat 2 then seat 1
//      → Deadlock

// GOOD: Always lock seats in ID order
@Transactional
public void reserveSeats(List<Long> seatIds, String guestId) {
    List<Long> sortedIds = seatIds.stream().sorted().toList(); // Consistent order

    for (Long seatId : sortedIds) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
            .orElseThrow(() -> new SeatNotFoundException(seatId));
        seat.setReservedBy(guestId);
    }
}
```

### Lock Timeout Configuration

```properties
# PostgreSQL: set lock timeout per transaction
spring.jpa.properties.jakarta.persistence.lock.timeout=2000

# MySQL equivalent (ms)
spring.jpa.properties.javax.persistence.lock.timeout=2000
```

---

## 5. N+1 Problem — Root Cause

```java
@Entity
public class Hotel {
    @Id private Long id;
    private String name;

    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY) // Default for @OneToMany
    private List<Room> rooms;
}
```

```java
// N+1 scenario
List<Hotel> hotels = hotelRepository.findAll(); // Query 1: SELECT * FROM hotel

for (Hotel hotel : hotels) {
    System.out.println(hotel.getRooms().size()); // Query per hotel: SELECT * FROM room WHERE hotel_id = ?
}
// If 100 hotels → 101 queries total
```

**Why EAGER doesn't solve it**: Switching to EAGER can create Cartesian product joins or still issue N queries depending on the association type.

---

## 6. N+1 Solutions

### Solution 1: EntityGraph (Recommended for Ad Hoc Loading)

```java
@Entity
@NamedEntityGraph(
    name = "Hotel.withRooms",
    attributeNodes = @NamedAttributeNode("rooms")
)
public class Hotel { ... }
```

```java
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @EntityGraph("Hotel.withRooms")
    List<Hotel> findAll(); // Generates LEFT JOIN FETCH rooms in one query

    // Ad-hoc EntityGraph without named definition
    @EntityGraph(attributePaths = {"rooms", "rooms.amenities"})
    List<Hotel> findByCity(String city);
}
```

**Generated SQL**: `SELECT h.*, r.* FROM hotel h LEFT JOIN room r ON r.hotel_id = h.id`

### Solution 2: JPQL Fetch Join

```java
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @Query("SELECT DISTINCT h FROM Hotel h JOIN FETCH h.rooms WHERE h.city = :city")
    List<Hotel> findByCityWithRooms(@Param("city") String city);
}
```

**DISTINCT** prevents duplicate Hotel instances from the JOIN.

**Trap**: Fetch joins cannot be combined with `setMaxResults()` (pagination). Hibernate will warn and load all results into memory before paginating. Use `@BatchSize` instead for paginated results.

### Solution 3: @BatchSize (Recommended for Pagination + Lazy Loading)

```java
@Entity
public class Hotel {
    @Id private Long id;

    @OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
    @BatchSize(size = 25) // Batch load 25 collections at a time
    private List<Room> rooms;
}
```

**Generated SQL**: Instead of 100 queries, Hibernate generates:
```sql
SELECT * FROM room WHERE hotel_id IN (1, 2, 3, ..., 25); -- batch 1
SELECT * FROM room WHERE hotel_id IN (26, 27, ..., 50);  -- batch 2
```

**Global configuration (fallback)**:

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=25
```

### Solution 4: Subselect Fetching

```java
@OneToMany(mappedBy = "hotel", fetch = FetchType.LAZY)
@Fetch(FetchMode.SUBSELECT) // Load all collections in a subquery of the original query
private List<Room> rooms;
```

**Generated SQL**:
```sql
SELECT * FROM room WHERE hotel_id IN (
    SELECT id FROM hotel WHERE city = 'NYC'
);
```

Best when you need all collections loaded in exactly 2 queries regardless of result size.

### N+1 Solution Decision Matrix

| Scenario | Recommended Solution |
|---|---|
| Load entities with full collections, no pagination | EntityGraph or JPQL fetch join |
| Paginated list + lazy loading | `@BatchSize(size = 25)` |
| All hotels in a region with their rooms | Subselect fetch |
| Only need partial fields (DTO) | Interface projection or constructor query |

---

## 7. Result Set Projections

Loading full entities when you only need a few fields wastes memory and network.

### Interface Projection (Closed)

```java
public interface HotelSummary {
    String getName();
    String getCity();
    BigDecimal getAverageRating();
}

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<HotelSummary> findByCity(String city);
    // Hibernate generates: SELECT h.name, h.city, h.average_rating FROM hotel WHERE city = ?
}
```

**No additional code needed** — Spring Data reads the return type and generates the optimal query.

### Interface Projection (Open) — SpEL

```java
public interface HotelView {
    String getName();
    String getCity();

    @Value("#{target.name + ', ' + target.city}")
    String getNameAndCity();
}
```

**Caution**: Open projections load the full entity, then apply SpEL. Prefer closed projections for performance.

### Class (DTO) Projection

```java
public record HotelDto(String name, String city, BigDecimal averageRating) {}

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @Query("SELECT new com.example.HotelDto(h.name, h.city, h.averageRating) FROM Hotel h WHERE h.city = :city")
    List<HotelDto> findDtoByCity(@Param("city") String city);
}
```

**With Spring Data projections (no @Query needed)**:

```java
// Spring Data auto-generates constructor query for record/class projections
List<HotelDto> findByCity(String city);
```

### Native Query with Projection

```java
@Query(value = "SELECT h.name, h.city, AVG(r.rating) as averageRating FROM hotel h " +
               "JOIN review r ON r.hotel_id = h.id WHERE h.city = :city GROUP BY h.id",
       nativeQuery = true)
List<HotelSummary> findTopRatedHotelsByCity(@Param("city") String city);
```

---

## 8. Batch Operations

### JDBC Batch Insert (Hibernate)

```properties
# Enable Hibernate JDBC batching
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

```java
@Transactional
public void bulkInsertRooms(List<Room> rooms) {
    int batchSize = 50;
    for (int i = 0; i < rooms.size(); i++) {
        entityManager.persist(rooms.get(i));

        if (i > 0 && i % batchSize == 0) {
            entityManager.flush();  // Execute batch
            entityManager.clear();  // Free memory
        }
    }
    entityManager.flush(); // Last partial batch
}
```

**Critical trap**: Identity generation strategy (`IDENTITY`) disables Hibernate batching because Hibernate needs the generated ID immediately. Use `SEQUENCE` strategy for batching:

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_seq")
@SequenceGenerator(name = "room_seq", sequenceName = "room_sequence", allocationSize = 50)
private Long id;
```

### Spring Data JPA Batch Save

```java
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {}

@Service
public class RoomBulkService {

    @Autowired
    private RoomRepository roomRepository;

    @Transactional
    public void bulkSave(List<Room> rooms) {
        roomRepository.saveAll(rooms); // Uses Hibernate batching if configured
    }
}
```

### Bulk Update via JPQL

```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' WHERE b.checkoutDate < :date AND b.status = 'CONFIRMED'")
    int expireOldBookings(@Param("date") LocalDate date);
}
```

```java
@Transactional
public int expireBookings(LocalDate cutoff) {
    int updated = bookingRepository.expireOldBookings(cutoff);
    // Note: @Modifying bypasses Hibernate cache - first-level cache may be stale
    entityManager.clear(); // Clear cache after bulk update
    return updated;
}
```

---

## 9. Stateless Sessions

For high-throughput read pipelines where entities don't need to be tracked by the persistence context.

```java
@Service
public class ReportService {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    public List<HotelDto> generateReport() {
        StatelessSession session = ((SessionFactory) entityManagerFactory)
            .openStatelessSession();

        try {
            Query<HotelDto> query = session.createQuery(
                "SELECT new com.example.HotelDto(h.name, h.city) FROM Hotel h",
                HotelDto.class
            );
            return query.getResultList();
        } finally {
            session.close();
        }
    }
}
```

**Benefits of StatelessSession**:
- No first-level cache
- No dirty checking
- No event listeners
- No lazy loading
- Direct DB read → much faster for bulk reads

**Use case**: ETL jobs, reporting queries, data exports.

---

## 10. Read-Only Transactions

```java
@Transactional(readOnly = true)
public List<HotelSummary> findHotelsByCity(String city) {
    return hotelRepository.findByCity(city);
}
```

**What `readOnly = true` does**:
- Hints Hibernate to skip dirty checking on flush (no change detection)
- Hibernate does not snapshot entity state for change tracking
- Some databases optimize reads (e.g., set isolation level to READ_COMMITTED)
- Routing hint for read replica in multi-datasource setups

**Does NOT automatically route to replica** — routing requires additional configuration.

### Read Replica Routing

```java
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "READ_REPLICA"
            : "PRIMARY";
    }
}

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Qualifier("primary") DataSource primary,
            @Qualifier("replica") DataSource replica) {

        RoutingDataSource routing = new RoutingDataSource();
        routing.setDefaultTargetDataSource(primary);
        routing.setTargetDataSources(Map.of(
            "PRIMARY", primary,
            "READ_REPLICA", replica
        ));
        routing.afterPropertiesSet();
        return routing;
    }
}
```

---

## 11. Common Traps

| Trap | Root Cause | Fix |
|---|---|---|
| N+1 with `@OneToMany` LAZY | Collection accessed in loop | EntityGraph, fetch join, or @BatchSize |
| Fetch join + pagination | Hibernate loads all into memory | Use @BatchSize instead |
| Optimistic lock retry without @Retryable | StaleObjectStateException propagates to caller | Add @Retryable with backoff |
| IDENTITY strategy + batch insert | IDENTITY requires immediate ID → disables batching | Switch to SEQUENCE strategy |
| @Modifying without entityManager.clear() | First-level cache returns stale data | Clear after bulk updates |
| Pessimistic lock without timeout | Long-running lock starves other transactions | Set `jakarta.persistence.lock.timeout` |
| Cartesian join from multiple bag collections | Multiple @OneToMany fetch joins → MultipleBagFetchException | Use @BatchSize for one collection, fetch join for the other |

---

## 12. Strong Interview Answers

### N+1

```text
N+1 happens when Hibernate loads a parent entity collection lazily and then accesses a child
collection in a loop, generating one additional query per parent. I prevent it by using EntityGraph
or JPQL fetch join when all data is needed up front, and @BatchSize(size=25) when working with
paginated results where fetch joins cannot be used. For read-only reports, interface projections
or DTO constructor queries load only needed columns.
```

### Optimistic vs Pessimistic

```text
Optimistic locking is best for read-heavy workloads where conflicts are rare. It uses a @Version
column and detects conflicts at commit time, throwing StaleObjectStateException. I handle it with
retry logic using exponential backoff and a max attempt cap.

Pessimistic locking acquires a SELECT FOR UPDATE lock at read time. It's correct for critical
sections like seat reservation or inventory decrement where conflicts are frequent and retries
are unacceptable. I always set a lock timeout to avoid long waits.
```

### Batch Operations

```text
Hibernate batching requires configuring hibernate.jdbc.batch_size and ordering inserts/updates.
The critical gotcha is that IDENTITY key generation disables batching because the database needs
to return the key immediately. I switch to SEQUENCE strategy with allocationSize matching the
batch size. For bulk updates, I use JPQL @Modifying queries and clear the first-level cache
afterward to avoid stale reads.
```

---

## 13. Final Revision Checklist

```text
□ @Version on entity → optimistic lock → StaleObjectStateException on conflict → retry with backoff
□ @Lock(PESSIMISTIC_WRITE) → SELECT FOR UPDATE → set lock timeout
□ N+1 solutions: EntityGraph, fetch join (no pagination), @BatchSize (paginated), subselect (all at once)
□ Fetch join + pagination → Hibernate loads all in memory → use @BatchSize instead
□ Interface projection: closed = one optimal query; open = loads full entity
□ DTO projection: @Query with constructor expression or Spring Data auto-generation
□ IDENTITY strategy disables batch; SEQUENCE enables it
□ @Modifying bulk update → clear first-level cache afterward
□ StatelessSession for ETL/reporting → no cache, no dirty checking, fast
□ @Transactional(readOnly=true) → skip dirty check, optional replica routing
```
