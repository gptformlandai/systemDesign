# Spring Boot Distributed Architecture — Saga, CQRS, Event Sourcing — Platinum Sheet

## What This Covers

- Saga pattern: choreography vs orchestration with compensation
- CQRS (Command Query Responsibility Segregation)
- Event sourcing fundamentals
- Eventual consistency models and UI patterns
- Correlation IDs and distributed tracing across service boundaries
- Idempotency and deduplication strategies
- Circuit breaker integration with Sagas

---

## 1. Mental Model

```text
Distributed transactions are impossible to make atomic across services without tight coupling.

Saga = sequence of local transactions + compensating transactions for rollback
CQRS = separate write model (commands) from read model (queries)
Event Sourcing = state as an ordered log of events, not mutable rows

Correlation ID = the thread that connects all events across services for debugging

Eventual consistency = accept temporary inconsistency; design UI and domain to handle it
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why |
|---|---|---|
| Saga (orchestration vs choreography) | Very high | Senior/MAANG distributed design |
| Compensating transactions | High | Rollback design in distributed systems |
| CQRS | High | Read scalability pattern |
| Correlation ID propagation | High | Production debugging requirement |
| Eventual consistency UI patterns | Medium-high | Frontend awareness |
| Event sourcing basics | Medium-high | Domain modeling for event-driven systems |
| Idempotency + deduplication | High | Exactly-once consumer design |
| Saga timeout handling | Medium-high | Production reliability |

---

## 3. The Distributed Transaction Problem

```text
Scenario: Hotel booking requires:
  1. Reserve room (Room Service)
  2. Create booking record (Booking Service)
  3. Charge payment (Payment Service)
  4. Send confirmation email (Notification Service)

If step 3 fails after steps 1 and 2 succeed:
  - Room is reserved but not paid
  - Booking record exists with wrong status
  - Two-phase commit (2PC) across microservices = too much coupling, coordinator SPOF

Solution: Saga — local transactions + compensating transactions
```

---

## 4. Saga Pattern — Choreography

In choreography, each service publishes events and listens to events. No central orchestrator.

```text
Saga flow (choreography):

BookingService      → publishes BookingCreated
RoomService         → listens BookingCreated → reserves room → publishes RoomReserved
PaymentService      → listens RoomReserved → charges card → publishes PaymentCharged
NotificationService → listens PaymentCharged → sends email → publishes BookingConfirmed

On failure:
PaymentService charges card → fails → publishes PaymentFailed
RoomService listens PaymentFailed → releases reservation → publishes RoomReleased
BookingService listens RoomReleased → cancels booking → publishes BookingCancelled
```

### Spring Boot Choreography with Kafka

```java
// Booking Service publishes initial event
@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void createBooking(CreateBookingCommand command) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString());
        booking.setGuestId(command.getGuestId());
        booking.setRoomId(command.getRoomId());
        booking.setStatus("PENDING");
        booking.setCorrelationId(command.getCorrelationId()); // Thread the correlation ID
        bookingRepository.save(booking);

        kafkaTemplate.send("booking-events", booking.getId(),
            new BookingCreatedEvent(booking.getId(), booking.getRoomId(),
                booking.getGuestId(), command.getCorrelationId()));
    }

    // Compensation: Cancel booking on payment failure
    @KafkaListener(topics = "payment-events", groupId = "booking-saga")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        bookingRepository.findById(event.getBookingId()).ifPresent(booking -> {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);

            MDC.put("correlationId", event.getCorrelationId());
            log.info("Booking {} cancelled due to payment failure", booking.getId());

            kafkaTemplate.send("booking-events", booking.getId(),
                new BookingCancelledEvent(booking.getId(), event.getCorrelationId()));
        });
    }
}
```

```java
// Room Service reacts to booking created
@Component
public class RoomSagaParticipant {

    @KafkaListener(topics = "booking-events", groupId = "room-saga")
    public void handleBookingCreated(BookingCreatedEvent event) {
        try {
            roomService.reserve(event.getRoomId(), event.getBookingId());

            kafkaTemplate.send("room-events", event.getBookingId(),
                new RoomReservedEvent(event.getBookingId(), event.getRoomId(),
                    event.getCorrelationId()));
        } catch (RoomNotAvailableException ex) {
            kafkaTemplate.send("room-events", event.getBookingId(),
                new RoomReservationFailedEvent(event.getBookingId(), ex.getMessage(),
                    event.getCorrelationId()));
        }
    }

    // Compensation: Release room when booking cancelled
    @KafkaListener(topics = "booking-events", groupId = "room-compensation")
    public void handleBookingCancelled(BookingCancelledEvent event) {
        roomService.release(event.getBookingId());
    }
}
```

**Choreography trade-offs**:

| Pros | Cons |
|---|---|
| No single point of failure | Hard to trace full flow |
| Services loosely coupled | Compensation logic scattered across services |
| Simple to add new participants | Difficult to monitor saga state |
| Good for linear flows | Cyclic dependencies possible |

---

## 5. Saga Pattern — Orchestration

An orchestrator service controls the Saga flow. It knows every step and its compensations.

```java
@Component
public class BookingSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BookingSagaOrchestrator.class);

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Step 1: Start saga
    @Transactional
    public void startBookingSaga(CreateBookingCommand command) {
        SagaState state = new SagaState();
        state.setSagaId(UUID.randomUUID().toString());
        state.setCorrelationId(command.getCorrelationId());
        state.setCurrentStep("RESERVE_ROOM");
        state.setStatus("IN_PROGRESS");
        state.setPayload(objectMapper.writeValueAsString(command));
        sagaStateRepository.save(state);

        kafkaTemplate.send("room-commands", state.getSagaId(),
            new ReserveRoomCommand(state.getSagaId(), command.getRoomId(),
                command.getGuestId(), command.getCorrelationId()));
    }

    // Step 2: Room reserved → proceed to payment
    @KafkaListener(topics = "room-events", groupId = "booking-orchestrator")
    @Transactional
    public void handleRoomReserved(RoomReservedEvent event) {
        SagaState state = sagaStateRepository.findBySagaId(event.getSagaId());
        state.setCurrentStep("CHARGE_PAYMENT");
        sagaStateRepository.save(state);

        kafkaTemplate.send("payment-commands", state.getSagaId(),
            new ChargePaymentCommand(state.getSagaId(), event.getBookingId(),
                event.getAmount(), event.getCorrelationId()));
    }

    // Step 3: Room reservation failed → cancel saga
    @KafkaListener(topics = "room-events", groupId = "booking-orchestrator")
    @Transactional
    public void handleRoomReservationFailed(RoomReservationFailedEvent event) {
        SagaState state = sagaStateRepository.findBySagaId(event.getSagaId());
        state.setStatus("FAILED");
        state.setCurrentStep("FAILED_RESERVE_ROOM");
        sagaStateRepository.save(state);

        // No compensation needed — no steps succeeded yet
        log.error("Saga {} failed at room reservation: {}", state.getSagaId(), event.getReason());
    }

    // Step 4: Payment failed → compensate room reservation
    @KafkaListener(topics = "payment-events", groupId = "booking-orchestrator")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        SagaState state = sagaStateRepository.findBySagaId(event.getSagaId());
        state.setStatus("COMPENSATING");
        state.setCurrentStep("RELEASE_ROOM");
        sagaStateRepository.save(state);

        // Compensate: release the room
        kafkaTemplate.send("room-commands", state.getSagaId(),
            new ReleaseRoomCommand(state.getSagaId(), event.getRoomId(),
                event.getCorrelationId()));
    }

    // Step 5: Room released → saga fully compensated
    @KafkaListener(topics = "room-events", groupId = "booking-orchestrator")
    @Transactional
    public void handleRoomReleased(RoomReleasedEvent event) {
        SagaState state = sagaStateRepository.findBySagaId(event.getSagaId());
        state.setStatus("COMPENSATED");
        sagaStateRepository.save(state);
        log.info("Saga {} fully compensated", state.getSagaId());
    }

    // Step 6: Payment succeeded → create booking + send notification
    @KafkaListener(topics = "payment-events", groupId = "booking-orchestrator")
    @Transactional
    public void handlePaymentCharged(PaymentChargedEvent event) {
        SagaState state = sagaStateRepository.findBySagaId(event.getSagaId());
        state.setStatus("COMPLETED");
        state.setCurrentStep("COMPLETED");
        sagaStateRepository.save(state);

        kafkaTemplate.send("notification-commands", state.getSagaId(),
            new SendConfirmationCommand(state.getSagaId(), event.getGuestId(),
                event.getCorrelationId()));
    }
}
```

**Orchestration trade-offs**:

| Pros | Cons |
|---|---|
| Full visibility of saga state | Orchestrator is a central service |
| Easy to monitor and debug | Coupling to orchestrator |
| Compensation logic centralized | Orchestrator can become a bottleneck |
| Supports complex non-linear flows | More infrastructure complexity |

---

## 6. Saga Timeout Handling

Sagas can get stuck if a participant service is unavailable. Timeouts are critical.

```java
@Component
public class SagaTimeoutMonitor {

    @Scheduled(fixedDelay = 60_000) // Run every minute
    @Transactional
    public void checkSagaTimeouts() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));

        List<SagaState> staleSagas = sagaStateRepository
            .findByStatusAndCreatedAtBefore("IN_PROGRESS", cutoff);

        for (SagaState saga : staleSagas) {
            log.error("Saga {} timed out at step {}", saga.getSagaId(), saga.getCurrentStep());
            saga.setStatus("TIMED_OUT");
            sagaStateRepository.save(saga);

            // Trigger compensation based on current step
            triggerCompensation(saga);
        }
    }

    private void triggerCompensation(SagaState saga) {
        switch (saga.getCurrentStep()) {
            case "CHARGE_PAYMENT" -> {
                // Room was reserved, payment timed out → release room
                kafkaTemplate.send("room-commands", saga.getSagaId(),
                    new ReleaseRoomCommand(saga.getSagaId(), saga.getRoomId(), saga.getCorrelationId()));
            }
            case "RESERVE_ROOM" -> {
                // Nothing succeeded yet, just mark as cancelled
                log.info("Saga {} timed out before any success — no compensation needed", saga.getSagaId());
            }
        }
    }
}
```

---

## 7. CQRS — Command Query Responsibility Segregation

```text
Core insight: Write operations (commands) and read operations (queries) have different
requirements. CQRS splits them into separate models.

Command side (write):
  - Validates business rules
  - Updates the write store (normalized DB)
  - Publishes events

Query side (read):
  - Optimized read projections
  - Denormalized, pre-joined data
  - Can be cached, read replicas, search indexes
```

### Spring Boot CQRS Implementation

```java
// COMMAND SIDE

@Service
public class BookingCommandService {

    @Autowired
    private BookingWriteRepository bookingWriteRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public String createBooking(CreateBookingCommand command) {
        // Business rule validation
        validateAvailability(command.getRoomId(), command.getCheckIn(), command.getCheckOut());

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString());
        booking.setStatus("CREATED");
        bookingWriteRepository.save(booking);

        // Publish event for read model to consume
        kafkaTemplate.send("booking-events", booking.getId(),
            new BookingCreatedEvent(booking.getId(), command.getGuestId(),
                command.getCorrelationId()));

        return booking.getId();
    }
}
```

```java
// QUERY SIDE — Separate read model

@Document(indexName = "bookings")
public class BookingView {
    private String id;
    private String guestId;
    private String guestName;
    private String hotelName;
    private String roomNumber;
    private String status;
    private BigDecimal totalAmount;
    private LocalDate checkIn;
    private LocalDate checkOut;
}

@Service
public class BookingQueryService {

    @Autowired
    private BookingViewRepository bookingViewRepository; // Backed by Elasticsearch or denormalized DB

    public List<BookingView> getGuestBookings(String guestId) {
        return bookingViewRepository.findByGuestId(guestId);
    }

    public BookingView getBookingDetail(String bookingId) {
        return bookingViewRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
```

```java
// Read model projection builder — consumes events and maintains denormalized view
@Component
public class BookingViewProjection {

    @Autowired
    private BookingViewRepository bookingViewRepository;

    @KafkaListener(topics = "booking-events", groupId = "booking-view-projection")
    public void on(BookingCreatedEvent event) {
        BookingView view = new BookingView();
        view.setId(event.getBookingId());
        view.setGuestId(event.getGuestId());
        view.setStatus("CREATED");
        bookingViewRepository.save(view);
    }

    @KafkaListener(topics = "booking-events", groupId = "booking-view-projection")
    public void on(BookingConfirmedEvent event) {
        bookingViewRepository.findById(event.getBookingId()).ifPresent(view -> {
            view.setStatus("CONFIRMED");
            view.setTotalAmount(event.getAmount());
            bookingViewRepository.save(view);
        });
    }
}
```

---

## 8. Event Sourcing

Instead of storing the current state, store every event that led to the current state.

```text
Traditional (state storage):
  booking row: {id: "B1", status: "CONFIRMED", amount: 1200}

Event Sourcing (event log storage):
  Event 1: BookingCreated    {bookingId: "B1", roomId: "R5", guestId: "G1"}
  Event 2: PaymentProcessed  {bookingId: "B1", amount: 1200}
  Event 3: BookingConfirmed  {bookingId: "B1"}

Current state = replay all events
```

### Spring Boot Event Sourcing Example

```java
// Event definitions
public sealed interface BookingEvent
    permits BookingCreated, PaymentProcessed, BookingConfirmed, BookingCancelled {}

public record BookingCreated(String bookingId, String roomId, String guestId, Instant occurredAt)
    implements BookingEvent {}

public record PaymentProcessed(String bookingId, BigDecimal amount, Instant occurredAt)
    implements BookingEvent {}

public record BookingConfirmed(String bookingId, Instant occurredAt)
    implements BookingEvent {}
```

```java
// Aggregate with event sourcing
public class BookingAggregate {

    private String id;
    private String status;
    private BigDecimal amount;
    private final List<BookingEvent> uncommittedEvents = new ArrayList<>();

    // Reconstitute from event history
    public static BookingAggregate reconstitute(List<BookingEvent> events) {
        BookingAggregate aggregate = new BookingAggregate();
        events.forEach(aggregate::apply);
        return aggregate;
    }

    // Command handler
    public void confirmBooking() {
        if (!"PAYMENT_PROCESSED".equals(this.status)) {
            throw new IllegalStateException("Cannot confirm without payment");
        }
        BookingConfirmed event = new BookingConfirmed(this.id, Instant.now());
        apply(event);
        uncommittedEvents.add(event); // Stage for persistence
    }

    // Event applier (pure, no side effects)
    private void apply(BookingEvent event) {
        switch (event) {
            case BookingCreated e -> {
                this.id = e.bookingId();
                this.status = "CREATED";
            }
            case PaymentProcessed e -> {
                this.amount = e.amount();
                this.status = "PAYMENT_PROCESSED";
            }
            case BookingConfirmed e -> this.status = "CONFIRMED";
            case BookingCancelled e -> this.status = "CANCELLED";
        }
    }

    public List<BookingEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }
}
```

```java
// Event Store
@Repository
public class EventStore {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void save(String aggregateId, List<BookingEvent> events, int expectedVersion) {
        // Optimistic concurrency check
        int currentVersion = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_store WHERE aggregate_id = ?",
            Integer.class, aggregateId);

        if (currentVersion != expectedVersion) {
            throw new ConcurrencyException("Aggregate was modified concurrently");
        }

        for (BookingEvent event : events) {
            jdbcTemplate.update(
                "INSERT INTO event_store (aggregate_id, event_type, payload, occurred_at) VALUES (?, ?, ?::jsonb, ?)",
                aggregateId,
                event.getClass().getSimpleName(),
                objectMapper.writeValueAsString(event),
                Instant.now()
            );
        }
    }

    public List<BookingEvent> load(String aggregateId) {
        return jdbcTemplate.query(
            "SELECT event_type, payload FROM event_store WHERE aggregate_id = ? ORDER BY sequence_num",
            (rs, row) -> deserialize(rs.getString("event_type"), rs.getString("payload")),
            aggregateId
        );
    }
}
```

**Event Sourcing benefits**:
- Full audit trail by design
- Ability to replay events to rebuild projections
- Time-travel debugging
- Natural event-driven integration

**Event Sourcing costs**:
- Query complexity (must project for reads)
- Event schema evolution requires care
- Performance: replay can be slow for old aggregates (use snapshots)

---

## 9. Correlation ID Propagation

Correlation ID is the single thread that connects a request's entire journey across services.

```java
// HTTP entry point: Generate or propagate correlation ID
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Propagate to downstream via MDC
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

```java
// Propagate in outgoing HTTP calls
@Bean
public RestClient restClient(RestClient.Builder builder) {
    return builder
        .requestInterceptor((request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                request.getHeaders().add("X-Correlation-Id", correlationId);
            }
            return execution.execute(request, body);
        })
        .build();
}
```

```java
// Propagate in Kafka messages (header)
kafkaTemplate.send(MessageBuilder
    .withPayload(event)
    .setHeader(KafkaHeaders.TOPIC, "booking-events")
    .setHeader("X-Correlation-Id", MDC.get("correlationId"))
    .build());
```

```java
// Kafka consumer: extract correlation ID from header
@KafkaListener(topics = "booking-events")
public void consume(ConsumerRecord<String, BookingEvent> record) {
    String correlationId = new String(
        record.headers().lastHeader("X-Correlation-Id").value(),
        StandardCharsets.UTF_8
    );

    MDC.put("correlationId", correlationId);

    try {
        processEvent(record.value());
    } finally {
        MDC.remove("correlationId");
    }
}
```

### Structured Log with Correlation ID

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "booking-service",
  "correlationId": "abc-123-def-456",
  "traceId": "a0b1c2d3e4f5",
  "spanId": "1234567890abcdef",
  "message": "Booking B1 confirmed",
  "bookingId": "B1",
  "guestId": "G1"
}
```

---

## 10. Eventual Consistency — UI Patterns

When the write completes but the read model hasn't updated yet, the UI shows stale data.

```text
User submits booking → BookingService returns 202 Accepted
                     → Kafka event published
                     → Read model updates (async, ~100ms-1s later)
                     → UI re-fetches bookings → may still see old status
```

### UI Pattern 1: Optimistic Update

```javascript
// Frontend shows confirmation immediately while backend catches up
async function confirmBooking(bookingId) {
    // Optimistic update: show CONFIRMED before server confirms
    setBookings(prev => prev.map(b =>
        b.id === bookingId ? {...b, status: 'CONFIRMED'} : b
    ));

    try {
        await api.post(`/bookings/${bookingId}/confirm`);
        // Backend eventually consistent — no immediate re-fetch needed
    } catch (error) {
        // Rollback optimistic update
        setBookings(prev => prev.map(b =>
            b.id === bookingId ? {...b, status: 'PENDING'} : b
        ));
    }
}
```

### UI Pattern 2: Poll Until Consistent

```java
// Backend: return a job/operation status endpoint
@PostMapping("/bookings/{id}/confirm")
public ResponseEntity<OperationStatus> confirmBooking(@PathVariable String id) {
    String operationId = bookingService.confirmBooking(id);
    return ResponseEntity
        .accepted()
        .header("Location", "/operations/" + operationId)
        .build();
}

@GetMapping("/operations/{operationId}")
public OperationStatus checkStatus(@PathVariable String operationId) {
    return operationStatusService.getStatus(operationId);
}
```

### UI Pattern 3: Return Version in Response

```json
{
  "bookingId": "B1",
  "status": "CONFIRMED",
  "version": 3
}
```

Client waits until read model version >= 3 before re-rendering.

---

## 11. Idempotency and Deduplication

Saga participants must be idempotent — the same message may arrive more than once.

```java
// Idempotent consumer with deduplication table
@KafkaListener(topics = "room-commands", groupId = "room-service")
@Transactional
public void handleReserveRoom(ConsumerRecord<String, ReserveRoomCommand> record) {
    String messageId = record.headers().lastHeader("messageId") != null
        ? new String(record.headers().lastHeader("messageId").value())
        : record.key() + "-" + record.offset();

    // Check if already processed
    if (processedMessageRepository.existsByMessageId(messageId)) {
        log.info("Duplicate message detected, skipping: {}", messageId);
        return;
    }

    ReserveRoomCommand command = record.value();

    try {
        roomService.reserve(command.getRoomId(), command.getBookingId());
        processedMessageRepository.save(new ProcessedMessage(messageId));

        kafkaTemplate.send("room-events", command.getSagaId(),
            new RoomReservedEvent(command.getSagaId(), command.getRoomId(),
                command.getCorrelationId()));
    } catch (RoomAlreadyReservedException ex) {
        // Idempotent: room already reserved by this booking → treat as success
        if (ex.getReservedByBookingId().equals(command.getBookingId())) {
            processedMessageRepository.save(new ProcessedMessage(messageId));
        } else {
            kafkaTemplate.send("room-events", command.getSagaId(),
                new RoomReservationFailedEvent(command.getSagaId(), "Room not available",
                    command.getCorrelationId()));
        }
    }
}
```

**Idempotency key strategies**:

| Strategy | When to Use |
|---|---|
| Message ID in header | Explicit idempotency key from producer |
| `sagaId + step` | Saga step idempotency |
| `aggregateId + version` | Event sourcing idempotency |
| Unique constraint in DB | Natural deduplication via unique index |

---

## 12. Common Traps

| Trap | Root Cause | Fix |
|---|---|---|
| Saga state lost on orchestrator restart | Saga state in memory only | Persist saga state to DB |
| Compensation not idempotent | Re-delivering compensation command causes double-release | Make all operations idempotent |
| CQRS read model permanently stale | Projection consumer failure not monitored | Alert on consumer lag, add DLQ |
| No timeout on saga steps | Participant never responds → saga stuck forever | Add timeout monitor with scheduled job |
| Correlation ID not propagated to Kafka | MDC not bridged to Kafka headers | Add header extractor in producer |
| Choreography deadlock | Service A waits for B, B waits for A | Use orchestration for complex flows |
| Event sourcing replay too slow | Long event history | Add snapshots every N events |

---

## 13. Strong Interview Answers

### Saga vs 2PC

```text
Two-phase commit (2PC) provides ACID guarantees across distributed systems but requires all
participants to be locked during the commit phase, creating a coordinator SPOF and tight coupling.

Saga replaces this with a sequence of local transactions plus compensating transactions for
rollback. Each service commits locally and publishes an event. If a later step fails, compensation
events trigger undoing previous steps.

I use orchestration sagas when I need a single point of visibility and the flow is complex or
non-linear. I use choreography when the flow is simple and linear, and I want services to remain
decoupled.
```

### CQRS

```text
CQRS separates writes from reads. The write side validates business rules and updates a normalized
write store, then publishes events. The read side consumes events and maintains denormalized
projections optimized for queries — often Elasticsearch, Redis, or a read replica.

The trade-off is eventual consistency. The read model may lag behind the write model by
milliseconds to seconds. I handle this with optimistic UI updates, poll-until-consistent patterns,
or returning version numbers to the client.
```

### Correlation ID

```text
Correlation IDs are how I trace a single user request across dozens of microservices, Kafka topics,
and async operations. At the HTTP gateway, I extract or generate the correlation ID, store it in MDC,
and inject it into every outgoing HTTP header and Kafka message header.

Each downstream service extracts it from the header, puts it back in MDC, and includes it in every
log line. This means a single grep for a correlation ID in a log aggregation system like Loki shows
the complete journey of a request across all services.
```

---

## 14. Final Revision Checklist

```text
□ Saga problem: distributed transactions need local tx + compensation for rollback
□ Choreography: no central coordinator, services react to events → good for simple linear flows
□ Orchestration: central saga service manages state and compensation → good for complex flows
□ Compensating transactions must be idempotent and reversible
□ Saga timeout monitor: scheduled job to detect and compensate stuck sagas
□ CQRS: command side = write DB + events; query side = denormalized projection DB
□ Read model eventual consistency: optimistic update, poll-until-consistent, version headers
□ Event sourcing: current state = replay of event log; use snapshots for old aggregates
□ Correlation ID: generate at entry point, propagate via HTTP headers + Kafka headers + MDC
□ Idempotency: check deduplication table before processing; natural idempotency via unique constraints
□ Saga state persistence: always store orchestrator state in DB, not in-memory
```
