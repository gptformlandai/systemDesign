# Java Data Formats, Jackson, Protobuf, And Serialization Gold Sheet

Target: senior Java backend engineers who must move data across APIs, queues, files, caches, and services without creating security or compatibility problems.

---

## 1. Intuition

Data format choice is a contract choice.

```text
Java object
    -> serializer
bytes/text on the wire
    -> deserializer
object in another process or later version
```

Beginner line:

```text
Serialization is turning objects into a transferable shape. The hard part is not conversion;
the hard part is safety, compatibility, schema evolution, and clear ownership.
```

---

## 2. Definition

- Definition: A data format defines how application data is represented outside the JVM, such as JSON, Avro, Protobuf, XML, CSV, or Java native serialization.
- Category: API contracts, messaging, storage, and interoperability.
- Core idea: choose the format based on human readability, schema needs, performance, compatibility, and security.

---

## 3. Why It Exists

Java objects live inside one JVM. Real systems need data to cross boundaries:

- REST APIs.
- Kafka topics.
- SQS/SNS messages.
- Database JSON columns.
- Cache values.
- Files and reports.
- Partner integrations.

Without format discipline:

- API fields break clients.
- Deserialization opens security holes.
- Money loses precision.
- Dates shift across time zones.
- Unknown fields break deployments.
- Queue consumers fail after producer schema changes.

---

## 4. Reality

Common Java backend choices:

| Format | Common Use | Strength | Risk |
|---|---|---|---|
| JSON | REST APIs, logs, configs | readable, universal | weak schema unless enforced |
| Protobuf | internal RPC, high-throughput messaging | compact, schema-first | less readable by humans |
| Avro | Kafka/data platforms | schema evolution | schema registry complexity |
| XML | legacy/enterprise integrations | mature validation | verbose, XXE risks |
| CSV | exports/imports | simple tabular data | escaping and schema ambiguity |
| Java serialization | legacy Java-only object graphs | easy for old code | unsafe and brittle |

Senior line:

```text
I treat external data as a contract, not as a direct dump of internal entity objects.
```

---

## 5. How It Works

### JSON DTO Flow

1. Domain object is converted to a DTO.
2. JSON serializer maps fields to JSON.
3. API sends JSON over HTTP.
4. Client deserializes into its own model.
5. Unknown or missing fields are handled according to contract rules.

### Schema-First Messaging Flow

1. Team defines Protobuf or Avro schema.
2. Code is generated.
3. Producer writes message using schema.
4. Consumer reads message using compatible schema.
5. Schema registry or compatibility checks prevent breaking changes.

### Failure Path

1. Producer renames a field.
2. Consumer expects old field.
3. Deserialization succeeds with null/default or fails.
4. Business logic misbehaves.
5. Incident becomes a contract/versioning problem.

### Recovery Path

1. Add new field instead of renaming/removing.
2. Keep old field until all consumers migrate.
3. Make consumers tolerant of unknown fields.
4. Add contract tests and schema compatibility checks.

---

## 6. What Problem It Solves

- Primary problem solved: stable data exchange between systems, teams, and versions.
- Secondary benefits: observability, interoperability, storage, auditability.
- Systems impact: reduces deploy coupling and consumer breakage.

---

## 7. When To Rely On It

Use explicit data-format thinking when:

- Designing REST request/response DTOs.
- Publishing events.
- Storing serialized payloads.
- Building public APIs.
- Moving from monolith to services.
- Sharing contracts across teams.
- Handling money, timestamps, IDs, or PII.

Interviewer keywords:

- DTO
- schema evolution
- Jackson
- Protobuf
- Avro
- backward compatibility
- deserialization security
- unknown fields

---

## 8. When Not To Use A Heavy Format

Avoid schema-heavy tooling when:

- The data is local and temporary.
- Humans must manually edit the payload often.
- The service has one owner and simple REST needs.
- Format complexity is larger than the integration risk.

Better approach:

- JSON for simple public REST.
- Protobuf for internal high-throughput, schema-first contracts.
- Avro with registry for Kafka/data-platform workflows.
- Plain domain objects inside the JVM only.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Enables service boundaries | Adds compatibility responsibility |
| Supports versioned APIs | Mapping code can feel repetitive |
| Makes contracts testable | Schema tooling adds build complexity |
| Can improve performance | Bad configuration creates security risks |
| Separates domain from transport | Duplicate DTO/domain models need maintenance |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- JSON is readable but larger and less strict.
- Protobuf is compact and schema-first but less inspectable.
- Avro is strong for data pipelines but usually needs registry discipline.
- DTO mapping is extra code but protects domain models.
- Polymorphic deserialization is convenient but can be dangerous if configured loosely.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Exposing JPA entities as API JSON | Leaks persistence shape and lazy fields | Use DTOs |
| Using `double` for money | Precision loss | Use `BigDecimal` or integer cents |
| Sending `LocalDateTime` for instant events | No timezone/offset | Use `Instant` or offset-aware fields |
| Enabling broad polymorphic typing | Deserialization risk | Use explicit allowlisted subtypes |
| Removing fields immediately | Breaks consumers | Deprecate, add new fields, migrate |
| Trusting Java serialization | Unsafe for untrusted data | Prefer JSON/Protobuf/Avro |

---

## 11. Key Numbers

| Item | Guidance |
|---|---|
| Public API compatibility | Preserve for as long as documented |
| JSON unknown fields | Usually ignore for forward compatibility |
| Money | Prefer exact decimal or integer minor units |
| Timestamps | Prefer ISO-8601 with zone/offset or epoch millis by contract |
| Protobuf field numbers | Never reuse removed field numbers |
| Event payloads | Version explicitly or use schema registry |
| Large payloads | Watch memory, streaming, compression, and timeouts |

---

## 12. Failure Modes

| Failure | User Observes | Cause | Mitigation |
|---|---|---|---|
| Missing field | Null/default behavior | Producer changed contract | compatibility checks |
| Date shifted | Wrong time shown | timezone mismatch | use `Instant`/offset and contract tests |
| Money mismatch | rounding errors | `double`/float | exact numeric representation |
| Deserialization exploit | security incident | unsafe polymorphic/native serialization | allowlist and avoid unsafe deserialization |
| Message poison pill | consumer retries forever | incompatible payload | dead-letter queue and schema checks |
| Payload too large | latency/OOM | unbounded JSON read | streaming, limits, pagination |

---

## 13. Scenario

- Product / system: Booking events published to Kafka.
- Why this concept fits: producers and consumers deploy independently and need schema compatibility.
- What would go wrong without it: renaming `bookingId` to `reservationId` breaks downstream billing and analytics consumers.

---

## 14. Code Sample

Jackson DTO example:

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingResponse(
    @JsonProperty("booking_id")
    String bookingId,

    @JsonProperty("room_id")
    String roomId,

    @JsonProperty("amount")
    BigDecimal amount,

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant createdAt
) {}
```

Why this is better than exposing an entity:

- Transport field names are explicit.
- Unknown fields can be ignored for forward compatibility.
- Money uses `BigDecimal`.
- Timestamp is an instant, not ambiguous local time.

---

## 15. Mini Program / Simulation

Versioning thought exercise:

```java
// v1 JSON
// {"booking_id":"B1","room_id":"R1","amount":"120.00"}

// v2 additive JSON
// {"booking_id":"B1","room_id":"R1","amount":"120.00","currency":"USD"}

record BookingV1(String bookingId, String roomId, String amount) {}
record BookingV2(String bookingId, String roomId, String amount, String currency) {}
```

Debrief:

1. Why is adding `currency` safer than renaming `amount`?
2. What default should a v1 consumer assume?
3. How would contract tests catch a breaking rename?
4. How would Protobuf handle field numbers differently from JSON names?

---

## 16. Practical Question

> You are designing an event payload for booking-created events consumed by five teams. Would you use JSON, Avro, or Protobuf, and how would you prevent breaking consumers?

---

## 17. Strong Answer

I would first clarify whether the event is internal, high-volume, and schema-governed. For Kafka-style data platform events, Avro with a schema registry is a strong fit because compatibility can be enforced. For internal RPC or compact service-to-service messages, Protobuf is strong. For simple REST/public readability, JSON is fine but needs contract tests and versioning discipline. I would make changes additive, avoid renaming/removing fields without a migration window, define timestamp and money formats explicitly, and add consumer-driven contract or schema compatibility checks in CI. I would not publish internal JPA entities as event contracts.

---

## 18. Revision Notes

- One-line summary: data formats are system contracts; choose and evolve them deliberately.
- Three keywords: DTO, schema, compatibility.
- One interview trap: serialization is not just conversion; it is security and versioning.
- One memory trick: JSON reads easy, Protobuf runs lean, Avro evolves with schema, Java serialization stays locked away.
