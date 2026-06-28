# Spring Kafka — Schema Registry, Exactly-Once, Consumer Rebalancing — Gold Sheet

## What This Covers

- Exactly-once semantics (idempotent producer, transactional producer, transactional consumer)
- Schema Registry (Avro/Protobuf schema evolution, compatibility modes)
- Consumer group rebalancing (partition assignment, rebalance listener, pause/resume)
- Offset management (auto-commit vs manual, reset strategies)
- Consumer lag monitoring and alerting
- High-scale consumer tuning
- Transactional outbox pattern for dual writes

---

## 1. Mental Model

```text
Kafka delivery guarantees:
  At-most-once   → producer fire-and-forget, auto-commit before processing
  At-least-once  → sync producer ack + manual commit after processing → duplicates possible
  Exactly-once   → idempotent producer + transactional API + EOS consumer → no duplicates

Schema Registry:
  Schema stored centrally → producers write schema ID in message header
  Consumers fetch schema by ID → deserialize safely without embedding schema in every message
  Compatibility mode guards against breaking changes

Rebalancing:
  Consumer joins/leaves group → broker triggers partition reassignment
  During rebalance → ALL consumers in group stop consuming
  CooperativeStickyAssignor → incremental rebalance → only revoked partitions pause
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why |
|---|---|---|
| Exactly-once semantics | High | Production correctness design question |
| Schema Registry and Avro | High | Common in data pipeline interviews |
| Consumer group rebalancing | High | Performance and reliability question |
| Manual offset commit | High | Idempotent consumer design |
| Consumer lag monitoring | Medium-high | Production operations |
| Cooperative rebalancing | Medium-high | Reduces "stop-the-world" impact |
| Offset reset strategies | Medium | Data recovery design |
| High-scale consumer tuning | Medium | Performance engineering |

---

## 3. Exactly-Once Semantics

### The Problem

```text
At-least-once scenario:
  1. Consumer reads message
  2. Consumer processes it (charges a card)
  3. Consumer crashes before committing offset
  4. On restart, consumer re-reads and re-charges → duplicate charge
```

### Layer 1: Idempotent Producer

```java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);      // Enable idempotence
    config.put(ProducerConfig.ACKS_CONFIG, "all");                    // Required for idempotence
    config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);     // Required
    config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // Max 5 for idempotence
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
}
```

**Idempotent producer**: Each message has a producer ID + sequence number. Broker deduplicates retries within a session. Prevents duplicates from producer retries.

### Layer 2: Transactional Producer

```java
@Bean
public ProducerFactory<String, Object> transactionalProducerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "booking-service-tx-${spring.application.instance-id}");
    // Each instance needs a unique transactional ID
    return new DefaultKafkaProducerFactory<>(config);
}

@Bean
public KafkaTemplate<String, Object> kafkaTemplate() {
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(transactionalProducerFactory());
    template.setTransactionIdPrefix("booking-tx-");
    return template;
}
```

```java
@Service
public class BookingEventService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    // Atomic: DB save + Kafka publish in one transaction
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow();
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        // Kafka producer transaction synced with DB transaction via KafkaTransactionManager
        kafkaTemplate.send("booking-events",
            booking.getId().toString(),
            new BookingConfirmedEvent(booking.getId(), booking.getGuestId()));
    }
}
```

### Layer 3: Exactly-Once Consumer (EOS)

```java
@Bean
public ConsumerFactory<String, Object> eosConsumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service");
    config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // CRITICAL: skip uncommitted
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new DefaultKafkaConsumerFactory<>(config);
}
```

**`isolation.level=read_committed`**: Consumer only sees messages from committed transactions. Aborted transaction messages are skipped.

### Transactional Outbox Pattern (Safest for Dual Writes)

```text
Problem: Writing to DB and Kafka separately is not atomic.
If Kafka publish fails after DB commit → event lost.
If DB commit fails after Kafka publish → event without DB state.

Solution: Transactional Outbox
  1. DB write + outbox entry in same transaction (guaranteed atomic)
  2. Relay process reads outbox table → publishes to Kafka → marks as sent
```

```java
@Entity
public class OutboxEvent {
    @Id @GeneratedValue
    private Long id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status; // PENDING, SENT

    @CreatedDate
    private Instant createdAt;
}
```

```java
@Service
@Transactional
public class BookingCommandHandler {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setStatus("CONFIRMED");

        // Same transaction: DB update + outbox insert
        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateType("Booking");
        outbox.setAggregateId(bookingId.toString());
        outbox.setEventType("BookingConfirmed");
        outbox.setPayload(objectMapper.writeValueAsString(booking));
        outbox.setStatus("PENDING");
        outboxRepository.save(outbox);
    }
}
```

```java
@Component
public class OutboxRelay {

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByStatus("PENDING");

        for (OutboxEvent event : pending) {
            kafkaTemplate.send("booking-events", event.getAggregateId(), event.getPayload());
            event.setStatus("SENT");
        }
    }
}
```

---

## 4. Schema Registry — Avro Integration

### Why Schema Registry

Without Schema Registry:
- Producer changes field type → consumer crashes reading old messages
- No contract governance → silent data corruption

With Schema Registry:
- Schema stored centrally (Confluent Schema Registry / AWS Glue)
- Producer registers schema → gets schema ID → writes `[magic byte][4-byte schema ID][avro bytes]`
- Consumer fetches schema by ID → deserializes safely

### Avro Schema Example

```json
{
  "type": "record",
  "name": "BookingEvent",
  "namespace": "com.example.events",
  "fields": [
    {"name": "bookingId", "type": "string"},
    {"name": "guestId", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "currency", "type": {"type": "string", "default": "USD"}}
  ]
}
```

### Spring Boot Avro Configuration

```xml
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
    <version>7.5.0</version>
</dependency>
```

```properties
spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer

spring.kafka.properties.schema.registry.url=https://schema-registry.example.com
spring.kafka.properties.specific.avro.reader=true
spring.kafka.properties.auto.register.schemas=false  # Disable in production; register explicitly
```

### Schema Evolution — Compatibility Modes

| Compatibility Mode | What's Allowed | Use Case |
|---|---|---|
| `BACKWARD` (default) | New schema reads old messages | Consumer upgrade before producer |
| `FORWARD` | Old schema reads new messages | Producer upgrade before consumer |
| `FULL` | Both directions | Safe rolling upgrades |
| `NONE` | No compatibility check | Dev/testing only |

**BACKWARD compatible changes** (safe):
- Add optional field with default value ✓
- Remove a field with a default ✓

**BACKWARD breaking changes** (reject):
- Remove required field (no default) ✗
- Change field type ✗
- Rename field ✗

```properties
# Set compatibility at topic level via Schema Registry REST API
PUT /config/booking-events-value
{"compatibility": "FULL"}
```

---

## 5. Consumer Group Rebalancing

### What Triggers Rebalancing

```text
- New consumer joins the group
- Consumer leaves (graceful or crash)
- Consumer fails heartbeat (session.timeout.ms exceeded)
- New partitions added to topic
- Consumer calls unsubscribe()
- Group coordinator is elected
```

### Eager Rebalancing (Default — Stop the World)

```text
1. All consumers revoke ALL partitions (stop consuming)
2. All consumers rejoin group
3. Broker reassigns all partitions
4. Consumers resume
→ Complete stop for all consumers during rebalance
```

### Cooperative Sticky Rebalancing (Recommended)

```java
@Bean
public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
        List.of(CooperativeStickyAssignor.class)); // Incremental rebalancing

    config.put(ConsumerConfig.GROUP_ID_CONFIG, "booking-processor");
    config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
    config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
    return new DefaultKafkaConsumerFactory<>(config);
}
```

**Cooperative rebalancing**:
```text
1. Only partitions that need to move are revoked
2. Other partitions continue being consumed
3. Revoked partitions reassigned in a second round
→ Minimal disruption
```

### Rebalance Listener

```java
@KafkaListener(topics = "booking-events", groupId = "booking-processor")
public class BookingConsumer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    @KafkaListener(topics = "booking-events")
    public void consume(ConsumerRecord<String, String> record,
                        Acknowledgment acknowledgment,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        processBooking(record.value());

        currentOffsets.put(
            new TopicPartition(record.topic(), partition),
            new OffsetAndMetadata(offset + 1)
        );

        acknowledgment.acknowledge();
    }
}
```

```java
// Dedicated rebalance listener
@Component
public class BookingRebalanceListener implements ConsumerRebalanceListener {

    private static final Logger log = LoggerFactory.getLogger(BookingRebalanceListener.class);

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions revoked: {}", partitions);
        // Commit offsets for revoked partitions before they are reassigned
        // Save any in-memory state
        // Pause any batch operations
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("Partitions assigned: {}", partitions);
        // Initialize offset tracking
        // Resume operations
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        log.warn("Partitions lost (unexpected rebalance): {}", partitions);
        // Partitions reassigned without onPartitionsRevoked being called
        // Handle in-flight state carefully — messages may be reprocessed
    }
}
```

```java
@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            BookingRebalanceListener rebalanceListener) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setConsumerRebalanceListener(rebalanceListener);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
```

---

## 6. Offset Management

### Auto-Commit (Default — Dangerous)

```properties
spring.kafka.consumer.enable-auto-commit=true
spring.kafka.consumer.auto-commit-interval=5000 # Every 5 seconds
```

**Risk**: Auto-commit happens on a timer. If consumer crashes after processing but before next auto-commit timer fires — offset still committed. If commit happens BEFORE processing completes — message can be lost.

### Manual Commit (Recommended for Production)

```java
@KafkaListener(topics = "booking-events",
               containerFactory = "kafkaListenerContainerFactory")
public void processBooking(ConsumerRecord<String, String> record,
                           Acknowledgment acknowledgment) {
    try {
        bookingService.process(record.value());
        acknowledgment.acknowledge(); // Commit only after successful processing
    } catch (NonRetryableException ex) {
        log.error("Non-retryable failure, sending to DLQ", ex);
        dlqTemplate.send("booking-events-dlq", record.value());
        acknowledgment.acknowledge(); // Acknowledge to skip the bad message
    }
    // RetryableExceptions: do NOT acknowledge → retry
}
```

### Batch Acknowledgment

```java
@KafkaListener(topics = "booking-events",
               containerFactory = "batchFactory")
public void processBatch(List<ConsumerRecord<String, String>> records,
                         Acknowledgment acknowledgment) {
    for (ConsumerRecord<String, String> record : records) {
        bookingService.process(record.value());
    }
    acknowledgment.acknowledge(); // Commits all offsets in batch at once
}
```

### Offset Reset Strategies

```properties
# What to do when no committed offset exists (new group or offset out of range)
spring.kafka.consumer.auto-offset-reset=earliest  # Process from beginning
# OR
spring.kafka.consumer.auto-offset-reset=latest    # Start from new messages only
# OR
spring.kafka.consumer.auto-offset-reset=none      # Throw exception if no committed offset
```

**Reset to specific offset (operational)**:

```bash
# Stop consumers first, then reset
kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 \
  --group booking-processor \
  --topic booking-events \
  --reset-offsets --to-datetime 2024-01-15T00:00:00.000 \
  --execute
```

---

## 7. Consumer Lag Monitoring

Consumer lag = number of messages not yet processed.

```text
lag = latest_offset - committed_offset

High lag → consumers falling behind producers
        → scale up consumers (up to partition count)
        → or: consumer is too slow → optimize processing
```

### Monitoring with Micrometer

```java
@Bean
public MicrometerConsumerListener<String, String> kafkaMetrics() {
    return new MicrometerConsumerListener<>(meterRegistry);
}
```

**Key metrics to alert on**:

| Metric | Alert Threshold |
|---|---|
| `kafka.consumer.records-lag-max` | > 10,000 (topic-dependent) |
| `kafka.consumer.fetch-latency-avg` | > 500ms |
| `kafka.consumer.commit-latency-avg` | > 1s |
| Consumer group `records-lag` per partition | Growing trend over 5 minutes |

### Kafka Lag Command (Operational)

```bash
kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 \
  --group booking-processor \
  --describe

# Shows: CONSUMER-ID, HOST, CLIENT-ID, TOPIC, PARTITION, CURRENT-OFFSET, LOG-END-OFFSET, LAG
```

### Prometheus + Alerting Rule

```yaml
# Prometheus alert for high consumer lag
- alert: KafkaConsumerLagHigh
  expr: kafka_consumer_group_records_lag_max{group="booking-processor"} > 10000
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Kafka consumer lag is high"
    description: "Consumer group {{ $labels.group }} lag > 10000"
```

---

## 8. High-Scale Consumer Tuning

### Key Consumer Configuration

```java
Map<String, Object> config = new HashMap<>();

// How many records to fetch per poll
config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Default 500

// Time consumer has to process records before considered failed
config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000); // 5 minutes

// Session timeout (heartbeat failure = dead)
config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000);

// Heartbeat frequency
config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10_000); // Must be < session.timeout / 3

// How much data to fetch per partition
config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1_048_576); // 1MB per partition

// Total fetch size across all partitions
config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52_428_800); // 50MB

// Concurrency (one thread per partition is optimal)
// Set on container:
factory.setConcurrency(8); // Set to number of partitions or fraction of it
```

### Tuning Strategy

```text
Slow consumer due to processing time:
  → Increase max.poll.interval.ms to match worst-case processing
  → Reduce max.poll.records to process fewer records per batch
  → Scale out: add consumers (up to partition count)

High lag:
  → Profile the processing code — look for DB bottlenecks, external calls
  → Use batch processing instead of one-by-one
  → Add consumer instances

Frequent rebalancing:
  → Check session.timeout.ms vs heartbeat.interval.ms ratio
  → Use CooperativeStickyAssignor
  → Use static group membership (group.instance.id) for stable deployments
```

### Static Group Membership (Kubernetes Friendly)

```java
// Assign a unique stable ID per pod instance
config.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "booking-processor-pod-0");
```

With static group membership, when a consumer restarts (pod restart), it rejoins without triggering a full rebalance if it comes back within `session.timeout.ms`.

---

## 9. Dead Letter Queue Pattern

```java
@Bean
public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<Object, Object> template) {
    return new DeadLetterPublishingRecoverer(template,
        (record, ex) -> new TopicPartition(record.topic() + "-dlq", record.partition()));
}

@Bean
public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
    return new DefaultErrorHandler(
        recoverer,
        new FixedBackOff(1000L, 3) // 3 retries with 1s delay
    );
}

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
```

---

## 10. Common Traps

| Trap | Root Cause | Fix |
|---|---|---|
| Auto-commit causes duplicate processing | Timer commits before processing | Use manual `MANUAL_IMMEDIATE` mode |
| Consumer keeps rebalancing | Session timeout too short | Tune heartbeat/session ratio, use CooperativeStickyAssignor |
| `max.poll.interval.ms` exceeded | Processing takes longer than limit | Increase interval or process fewer records per poll |
| Schema Registry connection refused | SR URL misconfigured | Verify SR URL, use HTTPS, test connectivity |
| Avro schema breaking change in production | Missing compatibility check | Set FULL compatibility, use CI schema compatibility check |
| Idempotent producer not actually deduplicating | Transactional ID changes per restart | Use stable transactional ID (not random) |
| `isolation.level=read_committed` not set | Consumer reads uncommitted messages | Always set for EOS consumers |
| Outbox relay creating duplicates | No idempotency on Kafka produce | Use idempotent producer; consumer deduplicates with `bookingId` |

---

## 11. Strong Interview Answers

### Exactly-Once

```text
True exactly-once in Kafka requires three things: idempotent producer (deduplicates producer retries),
transactional producer (atomic multi-partition writes), and read_committed isolation on the consumer
(skips messages from aborted transactions).

In practice, I often use the transactional outbox pattern: write to the DB and an outbox table in the
same DB transaction, then a relay process publishes from the outbox to Kafka. This separates the DB
atomicity guarantee from Kafka and avoids distributed transaction complexity.
```

### Schema Registry

```text
Schema Registry stores Avro schemas centrally. Producers include a schema ID in each message instead
of the full schema, and consumers fetch the schema by ID to deserialize. This saves space and enforces
contracts.

I configure FULL compatibility mode to allow both backward and forward-compatible changes — adding
optional fields with defaults is safe, removing required fields or changing types requires a major
version and migration strategy.
```

### Consumer Rebalancing

```text
Rebalancing happens when consumers join or leave a group, or when partitions change. The default eager
rebalancing revokes ALL partitions from ALL consumers during the rebalance, causing a full stop.

I use CooperativeStickyAssignor to get incremental rebalancing, where only the partitions that need to
move are revoked. I also implement a ConsumerRebalanceListener to commit pending offsets on revocation
and reinitialize state on assignment. For stable Kubernetes deployments, I use static group membership
so pod restarts don't trigger a rebalance.
```

---

## 12. Final Revision Checklist

```text
□ Idempotent producer: enable.idempotence=true, acks=all, retries=MAX
□ Transactional producer: unique transactional.id per instance, kafkaTemplate.executeInTransaction
□ EOS consumer: isolation.level=read_committed, manual offset commit
□ Outbox pattern: DB write + outbox insert in same transaction; relay publishes from outbox
□ Schema Registry: Avro schema ID in header, FULL compatibility, CI compatibility check
□ Rebalancing: CooperativeStickyAssignor, ConsumerRebalanceListener for commit-on-revoke
□ Static group membership: group.instance.id for Kubernetes pod stability
□ Offset commit: MANUAL_IMMEDIATE, commit after successful processing
□ Consumer lag: monitor kafka.consumer.records-lag-max, alert on growing trend
□ DLQ: DefaultErrorHandler + DeadLetterPublishingRecoverer with retry backoff
□ session.timeout.ms > 3 × heartbeat.interval.ms
□ max.poll.interval.ms > worst-case processing time
```
