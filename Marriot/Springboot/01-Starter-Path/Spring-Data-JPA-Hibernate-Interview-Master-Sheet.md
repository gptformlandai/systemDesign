# Spring Data JPA And Hibernate Interview Master Sheet

Target: Marriott Tech Accelerator / Java Backend / Intervue round.

This sheet covers Spring Data JPA + Hibernate topics that are repeatedly asked in Java backend interviews:
- JPA vs Hibernate vs Spring Data JPA
- Entity lifecycle and persistence context
- First-level and second-level cache
- Entity mappings
- Fetch types and N+1 problem
- Cascading and orphan removal
- Repository methods, JPQL, native queries
- Transactions and dirty checking
- Locking and concurrency
- Pagination and sorting
- Auditing
- Performance tuning
- Hot interview questions and traps

Goal:

```text
After reading this sheet, you should be able to explain how Spring Data JPA talks to
Hibernate, how Hibernate manages entities, why N+1 happens, how caches work, how mappings
are designed, and how to write production-safe repositories.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| JPA vs Hibernate vs Spring Data JPA | Very high | Checks conceptual clarity |
| Entity and table mapping | Very high | Daily backend work |
| Persistence context | Very high | Core Hibernate behavior |
| Entity lifecycle states | Very high | persist/merge/detach/remove clarity |
| First-level cache | Very high | Fundamental Hibernate cache |
| Second-level cache | High | Advanced performance knowledge |
| Lazy vs eager loading | Very high | Performance and N+1 |
| N+1 problem | Very high | Most repeated JPA performance question |
| `@OneToMany`, `@ManyToOne`, `@ManyToMany` | Very high | Mapping correctness |
| Owning side / `mappedBy` | Very high | Common mapping confusion |
| Cascade types | High | Parent-child persistence behavior |
| Orphan removal | High | Child deletion semantics |
| Dirty checking | Very high | How updates happen |
| JPQL vs native query | High | Query clarity |
| Derived queries | High | Spring Data fundamentals |
| Pagination and sorting | High | API-level use |
| Optimistic/pessimistic locking | High | Concurrency correctness |
| Transaction boundaries | Very high | Lazy loading and consistency |
| EntityGraph / fetch join | High | N+1 solution |
| Auditing | Medium | Practical production feature |
| Batch operations | Medium-high | Performance tuning |

---

## 2. JPA vs Hibernate vs Spring Data JPA

### JPA

JPA stands for Java Persistence API.

It is a specification.

It defines:
- Entity mapping annotations
- Entity lifecycle
- EntityManager
- JPQL
- Persistence context
- Transactions integration

JPA is not an implementation by itself.

### Hibernate

Hibernate is an implementation of JPA.

It provides:
- ORM engine
- Entity state tracking
- SQL generation
- Caching
- Lazy loading
- Dirty checking
- Hibernate-specific features

### Spring Data JPA

Spring Data JPA is a Spring abstraction over JPA.

It provides:
- Repository interfaces
- Derived query methods
- Pagination and sorting
- `@Query`
- Auditing
- Reduced boilerplate

### Strong Interview Answer

```text
JPA is the specification, Hibernate is a popular implementation of that specification,
and Spring Data JPA is a Spring abstraction that makes working with JPA easier through
repositories, derived queries, pagination, and integration with Spring transactions.
```

### Mental Model

```text
Your Repository
    -> Spring Data JPA proxy
        -> JPA EntityManager
            -> Hibernate implementation
                -> JDBC
                    -> Database
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Is JPA an ORM? | JPA is ORM specification; Hibernate is ORM implementation |
| Is Spring Data JPA same as Hibernate? | No, it uses JPA provider like Hibernate underneath |
| Can Spring Data JPA work without Hibernate? | Yes, with another JPA provider, though Hibernate is common |
| Why use Spring Data JPA? | Reduces repository boilerplate |

---

## 3. Entity Basics

### Entity Example

```java
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bookingNumber;

    @Column(nullable = false)
    private String status;

    protected Booking() {
        // Required by JPA
    }

    public Booking(String bookingNumber, String status) {
        this.bookingNumber = bookingNumber;
        this.status = status;
    }
}
```

### Important Rules

- Entity must have `@Entity`.
- Entity must have primary key using `@Id`.
- Entity needs no-args constructor, at least protected.
- Entity should not usually be final because proxies may need subclassing.
- Persistent fields should be accessible to JPA through field or property access.

### Field Access vs Property Access

If annotations are on fields:

```java
@Id
private Long id;
```

JPA uses field access.

If annotations are on getters:

```java
@Id
public Long getId() {
    return id;
}
```

JPA uses property access.

Interview line:

```text
Do not mix field and property access randomly in the same entity hierarchy.
```

### Table And Column Mapping

```java
@Entity
@Table(name = "customers")
class Customer {
    @Id
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
}
```

### Enum Mapping

Bad:

```java
@Enumerated(EnumType.ORDINAL)
private BookingStatus status;
```

Problem:

```text
If enum order changes, database values become wrong.
```

Preferred:

```java
@Enumerated(EnumType.STRING)
private BookingStatus status;
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why no-arg constructor? | JPA needs it to instantiate entities |
| Can entity be final? | Avoid final; proxies/lazy loading may require subclassing |
| Enum ORDINAL or STRING? | Prefer STRING for safety |
| `@Table` vs `@Column`? | Table-level vs column-level mapping |

---

## 4. Primary Key Generation

### Common Strategies

| Strategy | Meaning |
|---|---|
| `IDENTITY` | Database auto-increment |
| `SEQUENCE` | Database sequence |
| `TABLE` | Table used to generate IDs |
| `AUTO` | Provider chooses |
| UUID | Application/database generated UUID |

### IDENTITY

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Common with MySQL/PostgreSQL identity columns.

Important:

```text
IDENTITY may require immediate insert to get generated ID, which can reduce batching.
```

### SEQUENCE

```java
@SequenceGenerator(
    name = "booking_seq",
    sequenceName = "booking_seq",
    allocationSize = 50
)
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
private Long id;
```

Good for batching when configured well.

### UUID

```java
private UUID id;
```

Useful for distributed systems, but:
- Larger index size.
- Random UUID can fragment B-tree indexes.
- UUIDv7/time-ordered UUIDs can help where supported.

### Hot Question

```text
IDENTITY is simple but can limit insert batching because Hibernate often needs the database
to generate the ID immediately. SEQUENCE with allocation size can be more batching-friendly.
```

---

## 5. Persistence Context

### What Is Persistence Context?

Persistence context is a set of managed entity instances within a transaction/session.

It acts like a first-level cache and tracks entity changes.

### Mental Model

```text
Persistence Context = Hibernate's unit-of-work memory for managed entities.
```

### Strong Interview Answer

```text
Persistence context is the first-level cache managed by EntityManager. Within it, entities
are managed, identity is guaranteed, and Hibernate tracks changes for dirty checking.
```

### Example

```java
@Transactional
public void demo(Long id) {
    Booking b1 = entityManager.find(Booking.class, id);
    Booking b2 = entityManager.find(Booking.class, id);

    System.out.println(b1 == b2); // true
}
```

Why true?

```text
Same entity ID in same persistence context returns same Java object instance.
```

### What Persistence Context Does

- Stores managed entities.
- Guarantees identity within context.
- Tracks changes.
- Delays SQL until flush.
- Provides first-level cache.

---

## 6. Entity Lifecycle States

### States

| State | Meaning |
|---|---|
| Transient | New object, not associated with persistence context |
| Managed/Persistent | Associated with persistence context |
| Detached | Was managed, but context closed or entity detached |
| Removed | Marked for deletion |

### Example

```java
Booking booking = new Booking("B1", "CREATED"); // transient

entityManager.persist(booking); // managed

entityManager.detach(booking); // detached

entityManager.remove(booking); // removed, if managed
```

### persist vs merge

`persist`:
- Makes new transient entity managed.
- Used for new entities.

`merge`:
- Copies detached entity state into managed entity.
- Returns managed instance.
- Original object remains detached.

### Merge Trap

```java
Booking detached = new Booking();
Booking managed = entityManager.merge(detached);

detached.setStatus("CANCELLED"); // not tracked
managed.setStatus("CONFIRMED");  // tracked
```

Strong answer:

```text
merge returns a managed copy. The original detached object does not become managed.
```

---

## 7. Dirty Checking

### What Is Dirty Checking?

Hibernate automatically detects changes to managed entities and generates UPDATE SQL during flush.

Example:

```java
@Transactional
public void updateStatus(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow();

    booking.setStatus("CONFIRMED");

    // No explicit save required for managed entity
}
```

At transaction commit, Hibernate flushes:

```sql
UPDATE bookings SET status = 'CONFIRMED' WHERE id = ?;
```

### Strong Interview Answer

```text
Dirty checking means Hibernate tracks managed entities inside the persistence context.
When a managed entity changes, Hibernate detects it at flush/commit time and issues the
required update SQL.
```

### When Dirty Checking Does Not Work

- Entity is detached.
- Method is outside transaction and persistence context is closed.
- You modify a DTO, not entity.
- Read-only transaction/provider optimization may skip changes.

### `save` Trap

For managed entity inside transaction:

```java
booking.setStatus("CONFIRMED");
bookingRepository.save(booking); // often unnecessary
```

Interview line:

```text
For managed entities inside a transaction, explicit save is usually unnecessary because
dirty checking handles the update.
```

---

## 8. Flush

### What Is Flush?

Flush synchronizes persistence context changes with the database.

It does not necessarily commit the transaction.

### Flush vs Commit

| Flush | Commit |
|---|---|
| Sends SQL to DB | Ends transaction successfully |
| Transaction still active | Transaction complete |
| Can still rollback | Cannot rollback after commit |

### When Flush Happens

Usually:
- Before transaction commit
- Before executing query that needs consistent data
- When `entityManager.flush()` is called

### Example

```java
booking.setStatus("CONFIRMED");
entityManager.flush();
```

SQL is sent, but transaction can still rollback.

### Hot Question

```text
Flush sends pending changes to the database but does not commit. Commit finalizes the
transaction. A flushed change can still be rolled back.
```

---

## 9. First-Level Cache

### What Is First-Level Cache?

First-level cache is the persistence context cache.

It is:
- Enabled by default.
- Mandatory.
- Scoped to EntityManager/session.
- Not shared across transactions.

### Example

```java
@Transactional
public void test(Long id) {
    Booking b1 = bookingRepository.findById(id).orElseThrow();
    Booking b2 = bookingRepository.findById(id).orElseThrow();
}
```

Hibernate usually hits DB once for the entity.

Second lookup returns from persistence context.

### Strong Interview Answer

```text
First-level cache is Hibernate's persistence context cache. It is enabled by default and
works within a single EntityManager/session. It guarantees that the same entity ID maps to
the same object instance within that context.
```

---

## 10. Second-Level Cache

### What Is Second-Level Cache?

Second-level cache is shared across sessions/EntityManagers.

It is:
- Optional.
- Provider-based.
- Shared beyond one persistence context.
- Useful for read-mostly reference data.

### Common Providers

- Ehcache
- Caffeine/JCache integration
- Infinispan

### Entity Cache Example

```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE
)
class RoomType {
    @Id
    private Long id;

    private String name;
}
```

### Cache Regions

Second-level cache is organized into regions:
- Entity regions
- Collection regions
- Query cache region

### Cache Strategies

| Strategy | Use |
|---|---|
| READ_ONLY | Data never changes |
| READ_WRITE | Data changes but needs consistency |
| NONSTRICT_READ_WRITE | Occasional stale reads acceptable |
| TRANSACTIONAL | JTA/transactional cache support |

### When To Use

Good for:
- Reference data
- Room types
- Country/state/currency lists
- Read-heavy rarely changing entities

Avoid for:
- Frequently updated data
- Highly transactional data like bookings/payments
- Data where stale reads are unacceptable

### Strong Interview Answer

```text
First-level cache is mandatory and scoped to the persistence context. Second-level cache is
optional, shared across sessions, and useful mainly for read-heavy, rarely changing data.
```

---

## 11. Query Cache

### What Is Query Cache?

Hibernate query cache stores query result identifiers, not full entity state.

It usually works with second-level cache.

### Important Point

```text
Query cache stores IDs/results. Entities still need to be loaded from second-level cache
or database.
```

### When To Use

- Same query repeated often.
- Underlying data changes rarely.
- Query parameters are limited and predictable.

### When Not To Use

- Highly dynamic queries.
- Frequently changing tables.
- Large result sets.
- High-cardinality parameter combinations.

### Hot Question

```text
Query cache is not a magic performance fix. It can hurt if queries are highly variable or
data changes frequently.
```

---

## 12. Entity Relationships

### Relationship Types

| Relationship | Example |
|---|---|
| OneToOne | User -> Profile |
| OneToMany | Customer -> Orders |
| ManyToOne | Order -> Customer |
| ManyToMany | Student -> Courses |

---

## 13. `@ManyToOne`

Most common and usually easiest mapping.

Example:

```java
@Entity
class Booking {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
```

Database:

```text
bookings.customer_id -> customers.id
```

### Important

Many bookings belong to one customer.

Foreign key lives on many side:

```text
bookings table
```

### Fetch Default

`@ManyToOne` default fetch is EAGER.

Interview recommendation:

```text
I usually set ManyToOne to LAZY explicitly to avoid accidental heavy loading.
```

---

## 14. `@OneToMany`

Example:

```java
@Entity
class Customer {
    @Id
    private Long id;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();
}
```

### `mappedBy`

`mappedBy = "customer"` means:

```text
The Booking.customer field owns the relationship.
```

The foreign key is in `bookings.customer_id`.

### Owning Side

The owning side is the side that controls the foreign key.

For bidirectional one-to-many:

```text
ManyToOne side is usually the owning side.
```

### Helper Methods

```java
public void addBooking(Booking booking) {
    bookings.add(booking);
    booking.setCustomer(this);
}

public void removeBooking(Booking booking) {
    bookings.remove(booking);
    booking.setCustomer(null);
}
```

Why?

```text
Keep both sides of bidirectional relationship in sync.
```

### Fetch Default

`@OneToMany` default fetch is LAZY.

### Hot Question

```text
In bidirectional relationships, updating only the inverse side may not update the foreign key.
The owning side controls the database relationship.
```

---

## 15. `@OneToOne`

Example:

```java
@Entity
class User {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}
```

Use cases:
- User and profile
- Booking and invoice
- Employee and badge

### Hot Point

One-to-one can often be modeled as:
- Same table if always used together.
- Separate table if optional/large/security-sensitive.

---

## 16. `@ManyToMany`

Example:

```java
@Entity
class Student {
    @Id
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}
```

### Interview Warning

Direct `@ManyToMany` is often not ideal in real systems.

Better if join table has extra columns:

```text
student_course(enrolled_at, status, grade)
```

Create explicit entity:

```text
Enrollment
```

### Strong Answer

```text
I avoid direct ManyToMany when the join table has business meaning or extra columns.
In that case, I model the join table as a separate entity.
```

---

## 17. Cascade Types

### What Is Cascade?

Cascade means operations on parent propagate to child.

### Cascade Types

| Cascade | Meaning |
|---|---|
| PERSIST | Persist child when parent is persisted |
| MERGE | Merge child when parent is merged |
| REMOVE | Remove child when parent is removed |
| REFRESH | Refresh child when parent is refreshed |
| DETACH | Detach child when parent is detached |
| ALL | All cascade operations |

### Example

```java
@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
private List<BookingItem> items = new ArrayList<>();
```

### Important Warning

Be careful with:

```java
CascadeType.REMOVE
```

Especially on:
- ManyToMany
- Shared child entities

### Strong Interview Answer

```text
Cascade controls which entity operations propagate from parent to child. I use it carefully,
especially REMOVE, because deleting a parent can accidentally delete children that may be
shared or should remain.
```

---

## 18. Orphan Removal

### What Is Orphan Removal?

`orphanRemoval = true` deletes a child when it is removed from parent collection.

Example:

```java
@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
private List<BookingItem> items = new ArrayList<>();
```

If:

```java
booking.getItems().remove(item);
```

Hibernate deletes the item row.

### Cascade REMOVE vs Orphan Removal

| Cascade REMOVE | Orphan Removal |
|---|---|
| Parent deleted -> child deleted | Child removed from collection -> child deleted |
| Triggered by remove parent | Triggered by de-association |

### Strong Answer

```text
Cascade REMOVE deletes children when the parent is removed. Orphan removal deletes a child
when it is removed from the parent's collection and no longer belongs to that aggregate.
```

---

## 19. Fetch Types: Lazy vs Eager

### Lazy Loading

Related entity loaded only when accessed.

```java
@ManyToOne(fetch = FetchType.LAZY)
private Customer customer;
```

### Eager Loading

Related entity loaded immediately with parent.

### Default Fetch Types

| Mapping | Default |
|---|---|
| `@ManyToOne` | EAGER |
| `@OneToOne` | EAGER |
| `@OneToMany` | LAZY |
| `@ManyToMany` | LAZY |

### Recommended Interview Answer

```text
I prefer LAZY by default and fetch explicitly per use case using fetch join, EntityGraph,
or DTO projection. EAGER can cause unexpected heavy queries and performance issues.
```

### LazyInitializationException

Happens when lazy association is accessed after persistence context is closed.

Example:

```java
Booking booking = bookingRepository.findById(id).orElseThrow();
return booking.getCustomer().getName();
```

If outside transaction and customer is lazy:

```text
LazyInitializationException
```

Fixes:
- Fetch required association in query.
- Use DTO projection.
- Use transactional service boundary.
- Avoid relying on Open Session in View.

### Hot Question

```text
Lazy loading is not bad. Accessing lazy associations outside a persistence context is the issue.
Fetch what you need inside the transaction/use case.
```

---

## 20. N+1 Query Problem

### What Is N+1?

One query loads parent rows.

Then N additional queries load child rows for each parent.

Example:

```java
List<Booking> bookings = bookingRepository.findAll();

for (Booking booking : bookings) {
    System.out.println(booking.getCustomer().getName());
}
```

SQL:

```text
1 query  -> select all bookings
N queries -> select customer for each booking
```

### Why It Happens

Lazy association is accessed in loop.

### Fix 1: Fetch Join

```java
@Query("""
    select b
    from Booking b
    join fetch b.customer
    where b.status = :status
    """)
List<Booking> findByStatusWithCustomer(@Param("status") BookingStatus status);
```

### Fix 2: EntityGraph

```java
@EntityGraph(attributePaths = {"customer"})
List<Booking> findByStatus(BookingStatus status);
```

### Fix 3: DTO Projection

```java
@Query("""
    select new com.example.BookingSummaryDto(b.id, c.name, b.status)
    from Booking b
    join b.customer c
    where b.status = :status
    """)
List<BookingSummaryDto> findBookingSummaries(@Param("status") BookingStatus status);
```

### Fix 4: Batch Fetching

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=50
```

### Strong Interview Answer

```text
N+1 happens when we load N parent rows and then lazily load a child association one by one.
I fix it by fetching exactly what the use case needs using fetch join, EntityGraph, DTO
projection, or batch fetching.
```

---

## 21. JPQL, Native SQL, and Derived Queries

### Derived Query

```java
List<Booking> findByStatusAndCustomerId(BookingStatus status, Long customerId);
```

Spring Data parses method name and creates query.

### JPQL

JPQL uses entity names and fields, not table/column names.

```java
@Query("""
    select b
    from Booking b
    where b.status = :status
    """)
List<Booking> findByStatus(@Param("status") BookingStatus status);
```

### Native Query

Native SQL uses table and column names.

```java
@Query(value = """
    select *
    from bookings
    where status = :status
    """, nativeQuery = true)
List<Booking> findNative(@Param("status") String status);
```

### JPQL vs Native SQL

| JPQL | Native SQL |
|---|---|
| Entity-based | Table-based |
| Portable across DBs | DB-specific |
| Works with JPA model | Full SQL power |
| Easier for entity queries | Better for complex DB-specific queries |

### When To Use What

| Need | Use |
|---|---|
| Simple query by fields | Derived method |
| Entity query with joins | JPQL |
| Complex reporting | Native SQL or projection |
| DB-specific feature | Native SQL |
| API response DTO | DTO projection |

---

## 22. Projections

### Why Projections?

Avoid loading full entities when only a few fields are needed.

### Interface Projection

```java
interface BookingView {
    Long getId();
    String getStatus();
}

List<BookingView> findByCustomerId(Long customerId);
```

### DTO Projection

```java
public record BookingSummaryDto(Long id, String customerName, String status) {
}
```

```java
@Query("""
    select new com.example.BookingSummaryDto(b.id, c.name, b.status)
    from Booking b
    join b.customer c
    """)
List<BookingSummaryDto> findSummaries();
```

### Strong Answer

```text
For read-only API responses, I often prefer DTO projections to avoid loading full entities
and unnecessary associations.
```

---

## 23. Repository Interfaces

### Common Interfaces

| Interface | Provides |
|---|---|
| Repository | Marker |
| CrudRepository | Basic CRUD |
| PagingAndSortingRepository | CRUD + pagination/sorting |
| JpaRepository | JPA-specific operations |

### JpaRepository Example

```java
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
}
```

### JpaRepository Useful Methods

| Method | Use |
|---|---|
| `save` | Persist or merge |
| `findById` | Find by primary key |
| `findAll` | Get all |
| `deleteById` | Delete |
| `flush` | Flush persistence context |
| `saveAndFlush` | Save then flush |
| `getReferenceById` | Lazy reference |

### `findById` vs `getReferenceById`

| `findById` | `getReferenceById` |
|---|---|
| Hits DB immediately | Returns lazy proxy |
| Returns Optional | Returns reference |
| Use when data needed now | Use when only reference needed |

Example:

```java
Customer customerRef = customerRepository.getReferenceById(customerId);
booking.setCustomer(customerRef);
```

This can avoid immediate customer SELECT if only FK reference is needed.

---

## 24. Pagination and Sorting

### Pageable

```java
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

Page<Booking> page = bookingRepository.findByStatus(status, pageable);
```

### Page vs Slice vs List

| Type | Meaning |
|---|---|
| Page | Content + total count |
| Slice | Content + hasNext, no total count |
| List | Just content |

### Performance Point

`Page` triggers count query.

For large tables, count can be expensive.

Use `Slice` when total count is not needed.

### Strong Answer

```text
For APIs that need total pages, Page is useful. For infinite scroll or next-page style APIs,
Slice can be more efficient because it avoids the count query.
```

---

## 25. Transactions With JPA

### Service Layer Transaction

```java
@Transactional
public void confirmBooking(Long id) {
    Booking booking = bookingRepository.findById(id).orElseThrow();
    booking.confirm();
}
```

Dirty checking updates at commit.

### Repository Methods Are Transactional?

Spring Data repository methods have transactional behavior by default:
- Read methods often read-only.
- Write methods transactional.

But business transactions should usually be at service layer.

### Strong Answer

```text
I usually put @Transactional at service layer because a business use case may involve
multiple repository operations that should commit or rollback together.
```

### Read-Only Transaction

```java
@Transactional(readOnly = true)
public BookingDetails getDetails(Long id) {
    return bookingRepository.findDetails(id);
}
```

Benefits:
- Communicates intent.
- May optimize flush behavior.
- Helps avoid accidental writes.

---

## 26. Locking

### Optimistic Locking

Use version column.

```java
@Version
private Long version;
```

When updating, Hibernate includes version check.

If another transaction updated first:

```text
OptimisticLockException
```

### When To Use Optimistic Locking

- Conflicts are rare.
- You want high concurrency.
- User edits data that may become stale.

### Pessimistic Locking

Lock row in DB.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select b from Booking b where b.id = :id")
Optional<Booking> findByIdForUpdate(@Param("id") Long id);
```

### When To Use Pessimistic Locking

- Conflicts are likely.
- Must prevent concurrent modification immediately.
- Inventory/booking/payment critical sections.

### Optimistic vs Pessimistic

| Optimistic | Pessimistic |
|---|---|
| Version check at commit/update | Locks row early |
| Better concurrency | Stronger blocking |
| Conflict detected later | Conflict prevented earlier |
| Good when conflicts rare | Good when conflicts likely |

### Booking Interview Answer

```text
For hotel room booking, I would combine database constraints/transaction logic with locking.
Optimistic locking works if conflicts are rare. Pessimistic locking or database-level
overlap constraints may be needed when double booking must be strictly prevented.
```

---

## 27. Auditing

### Enable Auditing

```java
@EnableJpaAuditing
@SpringBootApplication
public class Application {
}
```

### Entity Auditing Fields

```java
@Entity
class Booking {
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
```

### AuditorAware

```java
@Bean
AuditorAware<String> auditorAware() {
    return () -> Optional.of("system");
}
```

### Strong Answer

```text
Spring Data JPA auditing automatically fills fields like createdAt, updatedAt, createdBy,
and updatedBy when configured with @EnableJpaAuditing and AuditorAware.
```

---

## 28. Inheritance Mapping

### Strategies

| Strategy | Meaning |
|---|---|
| SINGLE_TABLE | One table for entire hierarchy |
| JOINED | Parent table + child tables |
| TABLE_PER_CLASS | Separate table per concrete class |
| MAPPED_SUPERCLASS | Parent fields copied into child tables, parent is not entity |

### Single Table

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
abstract class Payment {
    @Id
    private Long id;
}
```

### MappedSuperclass

```java
@MappedSuperclass
abstract class BaseEntity {
    @Id
    private Long id;

    private Instant createdAt;
}
```

### Strong Answer

```text
SINGLE_TABLE is fast and simple but has nullable columns for subclass-specific fields.
JOINED is normalized but requires joins. MAPPED_SUPERCLASS is useful for sharing common
fields without making the parent an entity.
```

---

## 29. Hibernate Performance Checklist

### Most Important Rules

1. Avoid N+1 queries.
2. Prefer LAZY associations by default.
3. Use fetch joins or EntityGraph for required associations.
4. Use DTO projections for read APIs.
5. Avoid loading huge collections.
6. Use pagination.
7. Add database indexes for foreign keys and filters.
8. Avoid EAGER on large relationships.
9. Use batch fetching where useful.
10. Keep transactions short.

### Batch Fetching

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=50
```

### JDBC Batching

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

Note:

```text
IDENTITY ID generation can reduce insert batching effectiveness.
```

### Show SQL For Debugging

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Better for real debugging:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

### `open-in-view`

Spring Boot often has Open Session in View behavior depending version/config.

Problem:

```text
It allows lazy loading during view/controller rendering, which can hide N+1 problems and
keep persistence context open too long.
```

Recommended:

```properties
spring.jpa.open-in-view=false
```

Strong answer:

```text
I prefer fetching required data in the service layer using DTOs, fetch joins, or EntityGraphs
rather than relying on Open Session in View.
```

---

## 30. Common Mapping Traps

| Trap | Correct View |
|---|---|
| Make every relation EAGER to avoid LazyInitializationException | Causes heavy queries and N+1; fetch per use case |
| Use direct ManyToMany everywhere | Use join entity if relationship has fields/business meaning |
| Forget owning side | Owning side controls foreign key |
| Update only inverse side | DB relationship may not update |
| Cascade REMOVE on shared child | Can delete data unexpectedly |
| Use EnumType.ORDINAL | Use STRING for safety |
| Return entities directly from API | Prefer DTOs to avoid lazy loading/leaking model |
| Huge bidirectional object graph in JSON | Can cause recursion and performance issues |
| Use Page when total count not needed | Slice may be cheaper |
| Use entity for read-only report query | DTO/native query may be better |

---

## 31. Hot Interview Questions And Answers

### Q1. JPA vs Hibernate vs Spring Data JPA?

```text
JPA is the specification, Hibernate is an implementation, and Spring Data JPA is a Spring
abstraction that simplifies repository creation and query methods on top of JPA.
```

### Q2. What is persistence context?

```text
Persistence context is the first-level cache managed by EntityManager. It holds managed
entities, guarantees identity, and tracks changes for dirty checking.
```

### Q3. What is first-level cache?

```text
First-level cache is the persistence context cache. It is enabled by default, scoped to one
EntityManager/session, and not shared across transactions.
```

### Q4. What is second-level cache?

```text
Second-level cache is optional and shared across sessions. It is useful for read-heavy,
rarely changing data, but not ideal for frequently updated transactional data.
```

### Q5. What is dirty checking?

```text
Hibernate tracks managed entities and automatically generates update SQL at flush/commit
when it detects changes.
```

### Q6. persist vs merge?

```text
persist makes a new transient entity managed. merge copies detached entity state into a
managed instance and returns that managed instance.
```

### Q7. Flush vs commit?

```text
Flush sends SQL changes to the database but does not end the transaction. Commit finalizes
the transaction. Flushed changes can still be rolled back.
```

### Q8. Lazy vs eager?

```text
Lazy loads associations when accessed. Eager loads immediately. I prefer lazy by default
and fetch required data explicitly per use case.
```

### Q9. What is N+1 problem?

```text
N+1 happens when one query loads parent rows and then N more queries load associations for
each parent. It is fixed using fetch joins, EntityGraph, DTO projections, or batch fetching.
```

### Q10. What is owning side?

```text
The owning side controls the foreign key and database relationship. In bidirectional
OneToMany/ManyToOne, the ManyToOne side is usually owning side.
```

### Q11. What does `mappedBy` mean?

```text
mappedBy marks the inverse side of a bidirectional relationship and points to the field
that owns the relationship.
```

### Q12. Cascade vs orphanRemoval?

```text
Cascade propagates operations from parent to child. Orphan removal deletes a child when it
is removed from the parent's collection.
```

### Q13. Why avoid EAGER?

```text
EAGER can load too much data unexpectedly and cause performance issues. It is better to
fetch exactly what a use case needs.
```

### Q14. What is LazyInitializationException?

```text
It happens when a lazy association is accessed after the persistence context/session is
closed. Fetch required data inside the transaction or use DTO projection.
```

### Q15. JPQL vs native query?

```text
JPQL works with entities and fields and is portable. Native query uses database SQL directly
and is useful for complex or database-specific queries.
```

### Q16. Page vs Slice?

```text
Page includes total count and total pages, which may require an expensive count query.
Slice only tells whether there is a next page and can be more efficient.
```

### Q17. Optimistic vs pessimistic locking?

```text
Optimistic locking uses a version column and detects conflicts later. Pessimistic locking
locks database rows early to prevent concurrent changes.
```

### Q18. Why put `@Transactional` on service layer?

```text
A business use case often involves multiple repository calls that should commit or rollback
together, so the service layer is the right transaction boundary.
```

### Q19. Can we return entities directly from REST APIs?

```text
It is possible but not preferred. DTOs are safer because they avoid lazy loading surprises,
hide internal model details, and prevent recursive serialization issues.
```

### Q20. How do you improve JPA performance?

```text
Avoid N+1, use lazy loading by default, fetch required associations explicitly, use DTO
projections for read APIs, add proper indexes, paginate large results, enable batching
where useful, and verify SQL generated by Hibernate.
```

---

## 32. One-Hour JPA Revision Plan

### First 15 Minutes: Core Model

Revise:
- JPA vs Hibernate vs Spring Data JPA
- Entity rules
- Entity lifecycle
- Persistence context
- Dirty checking

Must say:

```text
Persistence context is the first-level cache and unit-of-work where Hibernate manages
entities and tracks changes.
```

### Next 15 Minutes: Mappings

Revise:
- ManyToOne
- OneToMany
- OneToOne
- ManyToMany
- Owning side
- mappedBy
- Cascade
- orphanRemoval

Must say:

```text
The owning side controls the foreign key. In a bidirectional OneToMany/ManyToOne, the
ManyToOne side is usually the owning side.
```

### Next 15 Minutes: Performance

Revise:
- Lazy/eager
- N+1
- Fetch join
- EntityGraph
- DTO projection
- batch fetching
- open-in-view

Must say:

```text
I prefer lazy associations by default and fetch exactly what the use case needs using
fetch joins, EntityGraphs, or DTO projections.
```

### Final 15 Minutes: Queries and Transactions

Revise:
- Repository methods
- JPQL
- Native queries
- Pagination
- Locking
- @Transactional
- cache levels

Must say:

```text
I place transaction boundaries at the service layer and use optimistic or pessimistic locking
depending on the conflict pattern and business correctness needs.
```

---

## 33. Final Rapid Revision Sheet

| Need | JPA/Hibernate Concept |
|---|---|
| Java object mapped to table | Entity |
| Object state tracking | Persistence context |
| Same entity reused in transaction | First-level cache |
| Shared cache across sessions | Second-level cache |
| Auto-update managed entity | Dirty checking |
| Send SQL before commit | Flush |
| Entity detached update | merge |
| New entity save | persist |
| Avoid loading association immediately | LAZY |
| Load needed association in query | fetch join / EntityGraph |
| Parent-child operation propagation | Cascade |
| Delete child removed from collection | orphanRemoval |
| Relationship owner | Owning side |
| Top API read optimization | DTO projection |
| Detect concurrent update | Optimistic lock with `@Version` |
| Lock row now | Pessimistic lock |
| Avoid total count query | Slice |
| Avoid N+1 | fetch join, EntityGraph, projection, batch fetch |

---

## 34. Strong Closing Answer

If interviewer asks:

```text
How strong are you in Spring Data JPA and Hibernate?
```

Say:

```text
I am comfortable with JPA entity mapping, Hibernate persistence context, entity lifecycle,
dirty checking, first-level cache, relationship mappings, lazy loading, and transaction
boundaries. For performance, I pay close attention to N+1 queries, fetch strategies, DTO
projections, pagination, indexes, and generated SQL. For correctness, I use proper owning
sides, cascade rules, orphan removal only where appropriate, and optimistic or pessimistic
locking depending on the concurrency requirement.
```

---

## 35. How To Use This Guide By Level

| Level | What To Master |
|---|---|
| Starter | entity mapping, repository methods, basic relationships, transactions |
| Intermediate | persistence context, dirty checking, lazy/eager, N+1, fetch joins |
| Senior | locking, batching, pagination, schema constraints, generated SQL review |
| MAANG-ready | concurrency correctness, performance diagnosis, migration-safe model design |

Starter target:

```text
I can map entities, create repositories, write derived queries, and use transactions
correctly at the service layer.
```

Senior target:

```text
I can diagnose N+1 queries, choose fetch strategies, design correct relationships, handle
concurrent updates, and explain how Hibernate turns entity changes into SQL.
```

---

## 36. Modern JPA And Hibernate Production Notes

| Area | Expectation |
|---|---|
| Jakarta namespace | Spring Boot 3+ uses `jakarta.persistence.*`, not `javax.persistence.*` |
| DTOs | Prefer DTOs for API responses instead of exposing entities directly |
| Open Session in View | Avoid relying on it for production API correctness |
| Fetch strategy | LAZY by default, fetch per use case |
| Pagination | Avoid deep offset pagination for large datasets |
| Constraints | Enforce uniqueness and integrity in the database |
| Generated SQL | Review SQL for important queries |
| Transactions | Put business transaction boundaries in service layer |

Strong answer:

```text
I treat JPA as a productivity tool, not magic. I still design database constraints, indexes,
transaction boundaries, fetch plans, and pagination deliberately.
```

---

## 37. Production Debugging Scenarios

### Scenario 1: API Suddenly Slow

Check:
- N+1 queries
- missing index
- too many eager relationships
- large offset pagination
- slow count query from `Page`
- connection pool saturation
- unexpected flush before query

Strong answer:

```text
I enable SQL logging or use database query monitoring, identify query count and slow queries,
then fix fetch plans, indexes, projections, pagination, or transaction boundaries based on
the evidence.
```

### Scenario 2: `LazyInitializationException`

Cause:

```text
Lazy association accessed after persistence context is closed.
```

Fixes:
- fetch required data inside service transaction
- use DTO projection
- use fetch join or EntityGraph
- avoid returning entities directly from controller

Strong answer:

```text
I do not fix LazyInitializationException by making everything EAGER. I fetch exactly what
the use case needs inside the transaction.
```

### Scenario 3: Double Booking Under Concurrency

Check:
- missing unique constraint or exclusion logic
- insufficient isolation
- no optimistic/pessimistic lock
- check-then-insert race
- no idempotency key

Strong answer:

```text
For booking correctness, I combine application checks with database constraints or locking.
The database must protect the invariant because concurrent requests can pass application
checks at the same time.
```

---

## 38. Capstone Practice Questions

### Capstone 1: Design Booking Entities

Prompt:

```text
Model Customer, Booking, Room, Payment, and BookingStatusHistory using JPA.
```

Strong answer should mention:
- aggregate ownership
- `ManyToOne` from Booking to Customer
- avoid careless `ManyToMany`
- status history as separate child table
- cascade only parent-owned children
- indexes on lookup columns
- unique booking number
- DTOs for API output

### Capstone 2: Fix N+1 In Booking List API

Prompt:

```text
GET /bookings loads 100 bookings and then fires 100 customer queries. How do you fix it?
```

Strong answer should mention:
- identify generated SQL
- fetch join if returning entity graph
- EntityGraph as repository-level option
- DTO projection for list API
- batch fetching as broader mitigation
- pagination to limit result size

### Capstone 3: Handle Concurrent Room Reservation

Prompt:

```text
Two users try to reserve the same room for overlapping dates. How do you prevent double
booking?
```

Strong answer should mention:
- database-level invariant
- transaction boundary
- pessimistic lock or optimistic version depending conflict rate
- unique or exclusion constraint where supported
- retry or user-friendly conflict response
- idempotency for duplicate client retries

---

## 39. JPA Gold Checklist

You are strong in JPA/Hibernate if you can explain:

- JPA vs Hibernate vs Spring Data JPA
- entity lifecycle states
- persistence context as first-level cache
- dirty checking
- flush timing
- lazy vs eager defaults
- N+1 and fixes
- owning side and `mappedBy`
- cascade vs orphan removal
- `ManyToMany` risks
- DTO projections
- Page vs Slice
- optimistic vs pessimistic locking
- transaction boundary placement
- Open Session in View trade-off
- generated SQL review
- index and constraint design

---

## 40. Official Source Notes

Useful official references:

- Spring Data JPA Reference: https://docs.spring.io/spring-data/jpa/reference/
- Hibernate ORM Documentation: https://hibernate.org/orm/documentation/
- Spring Framework ORM Data Access: https://docs.spring.io/spring-framework/reference/data-access/orm.html
- Spring Transaction Management: https://docs.spring.io/spring-framework/reference/data-access/transaction.html
- Spring Boot SQL Databases: https://docs.spring.io/spring-boot/reference/data/sql.html
