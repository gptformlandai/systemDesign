# Kafka Schema Registry and Event Design Gold Sheet

> Goal: design Kafka events that remain readable, evolvable, and safe across many producers and consumers.

---

## 0. How To Read This

Beginner focus:

- event
- schema
- producer
- consumer
- backward compatibility

Intermediate focus:

- Avro
- Protobuf
- JSON Schema
- Schema Registry
- subject naming
- schema evolution

Senior focus:

- event contract ownership
- compatibility strategy
- event versioning
- data governance
- replay safety
- schema-breaking incident recovery

---

# Topic 1: Kafka Schema Registry and Event Design

---

## 1. Intuition

A Kafka topic is not just a pipe. It is a public contract.

If a producer changes an event shape without warning, every consumer becomes a potential production incident.

Schema Registry is like a passport office for events:

- producers register what an event should look like
- consumers use the registered schema to read it
- compatibility rules prevent unsafe changes

Beginner explanation:

Schemas protect Kafka consumers from surprise payload changes. A good event schema lets producers evolve safely while old and new consumers continue to work.

---

## 2. Definition

- Definition: Schema Registry is a centralized service that stores versioned schemas for Kafka messages and checks compatibility as schemas evolve.
- Category: Event contract management
- Core idea: treat event payloads as contracts, not random JSON strings.

---

## 3. Why It Exists

Without schemas:

```json
{
  "orderId": "O-1",
  "amount": 120.50
}
```

One day a producer changes it:

```json
{
  "id": "O-1",
  "totalAmount": "120.50"
}
```

Now consumers may break because:

- field names changed
- data types changed
- required fields disappeared
- meanings changed silently
- replaying old events becomes dangerous

Schema Registry exists to stop these changes from silently reaching production.

---

## 4. Reality

Schema and event design matter in:

- microservice event-driven systems
- CDC pipelines
- data lake ingestion
- payments and ledger events
- order lifecycle workflows
- analytics pipelines
- ML feature pipelines
- regulated audit systems

Senior engineers are expected to ask:

- Who owns the event?
- Who consumes it?
- Can old consumers read new data?
- Can new consumers read old data during replay?
- What is the compatibility policy?
- How do we deprecate fields?

---

## 5. How It Works

### Part A: Schema Registry Flow

Producer flow:

```text
producer has object
serializer checks/registers schema
schema registry returns schema id
producer writes schema id + encoded payload to Kafka
```

Consumer flow:

```text
consumer reads message
deserializer extracts schema id
deserializer fetches schema from registry
payload is decoded into object
```

Important:

- Kafka stores bytes.
- Schema Registry stores schemas.
- The message usually carries a schema ID so consumers know how to decode.

### Part B: Schema Formats

| Format | Strength | Common Use |
|---|---|---|
| Avro | compact binary, strong schema evolution support | Kafka event pipelines |
| Protobuf | compact, language-friendly contracts | service/event contracts |
| JSON Schema | human-readable, flexible | teams already using JSON heavily |

Do not choose only by syntax preference. Choose by:

- compatibility behavior
- tooling
- language support
- team maturity
- schema governance needs

### Part C: Compatibility Types

| Type | Meaning | Interview Use |
|---|---|---|
| Backward | new consumers can read old data | common default for replay safety |
| Forward | old consumers can read new data | useful during rolling producer deploys |
| Full | backward and forward with latest version | safer evolution |
| Transitive | compatibility checked across all previous versions | stronger long-term safety |

Simple memory:

```text
Backward = new reader reads old data
Forward = old reader reads new data
Full = both directions
Transitive = compare beyond only the last version
```

### Part D: Safe Schema Evolution

Generally safe:

- adding optional fields with defaults
- adding nullable fields
- adding enum values only when consumers handle unknown values
- deprecating fields but not removing immediately

Usually unsafe:

- renaming fields
- changing field type
- removing required fields
- changing semantic meaning of a field
- changing key structure without migration
- making nullable field required

### Part E: Event Envelope

A strong event often has an envelope:

```json
{
  "eventId": "evt-123",
  "eventType": "OrderPlaced",
  "eventVersion": 3,
  "aggregateType": "Order",
  "aggregateId": "order-91",
  "occurredAt": "2026-06-17T10:15:30Z",
  "producer": "order-service",
  "traceId": "trace-abc",
  "payload": {
    "orderId": "order-91",
    "customerId": "cust-7",
    "amount": 250.00,
    "currency": "USD"
  }
}
```

Why each field exists:

- `eventId`: dedupe and traceability
- `eventType`: routing and debugging
- `eventVersion`: explicit evolution
- `aggregateId`: partition key candidate
- `occurredAt`: business time
- `producer`: ownership
- `traceId`: distributed tracing
- `payload`: domain data

### Part F: Event Naming

Use past-tense facts:

Good:

- `OrderPlaced`
- `PaymentAuthorized`
- `InventoryReserved`
- `ShipmentDispatched`

Avoid:

- `CreateOrder`
- `AuthorizePayment`
- `UpdateInventory`

Reason:

- commands request action
- events record facts that already happened

### Part G: Key Design and Schema Design Together

Kafka key is also part of the contract.

Example:

```text
topic: order-events
key: orderId
value: OrderPlaced / OrderCancelled / OrderShipped
```

This gives:

- all events for one order in the same partition
- per-order ordering
- easier replay

Bad keys:

- `status`
- `country`
- `eventType`
- boolean flags

These create hot partitions or weak ordering semantics.

---

## 6. What Problem It Solves

- Primary problem solved: safe event evolution across many independent producers and consumers
- Secondary benefits: type safety, data governance, replay safety, debugging, documentation
- Systems impact: reduces cross-team breakage and makes Kafka usable as a long-lived platform

---

## 7. When To Rely On It

Use Schema Registry when:

- multiple services consume a topic
- messages are business-critical
- events live for a long time
- replay matters
- producers and consumers deploy independently
- data goes to analytics or ML systems
- schema ownership matters

Use simpler JSON without registry only when:

- team is tiny
- consumers are tightly controlled
- payloads are temporary
- breakage risk is low

---

## 8. When Not To Overcomplicate

Avoid excessive schema governance when:

- the event is internal and short-lived
- one producer and one consumer are deployed together
- a database queue is enough
- schema registry operation cost is not justified

Still document:

- event purpose
- owner
- key
- retention
- compatibility expectations

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Prevents breaking payload changes | Adds tooling and operational dependency |
| Enables safe schema evolution | Requires compatibility discipline |
| Improves type safety | Bad schema design can still break semantics |
| Helps replay old events | Requires subject/version management |
| Strong fit for multi-team platforms | Developers must understand evolution rules |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Strict compatibility:
  Safer consumers, but slower schema evolution.
- Flexible JSON:
  Faster early development, but higher long-term breakage risk.
- Rich envelope:
  Better tracing and dedupe, but more payload overhead.
- One topic per event type:
  Clear contracts, but more topics.
- Shared topic for related events:
  Easier ordering by aggregate, but schema union/versioning is harder.

### Common Mistakes

- Mistake: "It is JSON, so it is self-describing."
  Why it is wrong: JSON does not enforce required fields, types, or compatibility by itself.
  Better approach: use schema validation for important events.

- Mistake: "Version number alone solves compatibility."
  Why it is wrong: consumers still need a migration and decoding strategy.
  Better approach: use compatibility checks plus deprecation windows.

- Mistake: "Remove a field after no one seems to use it."
  Why it is wrong: replay, hidden consumers, and analytics jobs may still depend on it.
  Better approach: mark deprecated, monitor, communicate, then remove only with policy.

- Mistake: "Use eventType as Kafka key."
  Why it is wrong: creates hot partitions and weak business ordering.
  Better approach: key by aggregate ID such as `orderId`, `paymentId`, or `customerId`.

---

## 11. Key Numbers

Interview-ready guidance:

- Compatibility policy for critical event topics: `BACKWARD_TRANSITIVE` or `FULL_TRANSITIVE` when long replay matters
- Dedupe/event ID retention: at least as long as the replay or retry window
- Schema deprecation window: weeks to months in large organizations
- One event owner: always define one producer/domain owner
- Required event metadata: event ID, type, version, aggregate ID, timestamp, producer

These are governance choices, not universal constants.

---

## 12. Failure Modes

### Producer Deploys Breaking Schema

What fails:

- required field removed or type changed

What user observes:

- consumers fail deserialization
- lag grows
- DLQ fills

Mitigation:

- enforce compatibility in CI/CD
- block incompatible schema registration
- canary producer rollout

### Schema Registry Unavailable

What fails:

- producer/consumer cannot register or fetch schema

What user observes:

- startup failures
- serialization/deserialization errors

Mitigation:

- cache schemas in clients
- run registry highly available
- avoid registering new schemas during incident

### Semantic Break Without Schema Break

What fails:

- field still exists but meaning changes

Example:

```text
amount changes from cents to dollars without field rename
```

What user observes:

- no deserialization error
- business logic becomes wrong

Mitigation:

- document semantics
- use explicit field names
- add new field instead of changing meaning
- contract review for critical events

### Replay Breaks New Consumer

What fails:

- old events do not match assumptions of new code

What user observes:

- reprocessing fails halfway through history

Mitigation:

- backward compatibility
- default values
- replay tests
- transitive compatibility for long-lived topics

---

## 13. Scenario

- Product / system: Order event platform
- Why this concept fits: payment, inventory, fulfillment, analytics, and customer support all consume order events
- What would go wrong without it: one producer field rename could break several teams at once

---

## 14. Code Sample

Example Avro-style schema evolution idea:

```json
{
  "type": "record",
  "name": "OrderPlaced",
  "namespace": "com.example.orders",
  "fields": [
    { "name": "eventId", "type": "string" },
    { "name": "orderId", "type": "string" },
    { "name": "customerId", "type": "string" },
    { "name": "amount", "type": "double" },
    { "name": "currency", "type": "string", "default": "USD" },
    { "name": "couponCode", "type": ["null", "string"], "default": null }
  ]
}
```

Why this is safer:

- new field has default or is nullable
- older data can still be read
- event has a stable ID for dedupe

---

## 15. Mini Program / Simulation

This tiny simulation catches a dangerous schema change.

```python
def is_backward_safe(old_fields, new_fields):
    for field, old_rule in old_fields.items():
        if old_rule["required"] and field not in new_fields:
            return False, f"removed required field: {field}"
        if field in new_fields and old_rule["type"] != new_fields[field]["type"]:
            return False, f"changed type for field: {field}"
    return True, "safe enough for this simplified check"


def main():
    old_schema = {
        "orderId": {"type": "string", "required": True},
        "amount": {"type": "double", "required": True},
    }
    new_schema = {
        "orderId": {"type": "string", "required": True},
        "totalAmount": {"type": "double", "required": True},
    }

    print(is_backward_safe(old_schema, new_schema))


if __name__ == "__main__":
    main()
```

Real schema registries do much more than this, but the interview idea is simple: block breaking changes before they hit consumers.

---

## 16. Practical Question

> You own an `order-events` Kafka topic used by eight teams. How would you allow schema evolution without breaking consumers?

---

## 17. Strong Answer

I would treat the event as a published contract. The topic would use Schema Registry with a compatibility policy such as backward transitive or full transitive depending on replay requirements.

Events would have a stable envelope: `eventId`, `eventType`, `eventVersion`, `aggregateId`, `occurredAt`, `producer`, and `traceId`. The Kafka key would be `orderId` so all events for one order stay ordered within a partition.

For evolution, I would add optional fields with defaults instead of renaming or removing required fields. Semantic changes would use new fields or new event types. In CI/CD, producer builds would validate schema compatibility before deployment. I would maintain ownership, docs, deprecation policy, and replay tests.

If a breaking change is unavoidable, I would create a new topic or event version with a migration plan instead of silently changing the existing contract.

---

## 18. Revision Notes

- One-line summary: Kafka events are contracts; Schema Registry makes those contracts versioned and compatibility-checked.
- Three keywords: compatibility, event envelope, schema evolution
- One interview trap: JSON does not mean safe.
- One memory trick: new fields need defaults; old fields need patience.

---

## 19. Official Source Notes

- Confluent Schema Registry docs: <https://docs.confluent.io/platform/current/schema-registry/index.html>
- Confluent schema evolution docs: <https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html>
- Apache Kafka design docs: <https://kafka.apache.org/43/design/design/>

---

## 20. Subject Naming Strategies

Schema Registry organizes schemas by subject name. The naming strategy controls how schema versions and topics relate.

### TopicNameStrategy (Default)

Subject name = `<topic>-key` or `<topic>-value`

```text
topic: order-events
key subject:   order-events-key
value subject: order-events-value
```

Rules:

- one schema per topic key and one per topic value
- all producers to a topic must use the same schema
- different event types in one topic cannot have different schemas

Best for:

- one event type per topic
- simple, common setup

Limitation:

```text
If you mix OrderPlaced, OrderCancelled, OrderShipped in one topic, they all share one subject.
You must use a union type (e.g. Avro union) to handle all event types under one schema.
```

---

### RecordNameStrategy

Subject name = fully qualified record class name

```text
com.example.orders.OrderPlaced
com.example.orders.OrderCancelled
```

Rules:

- each record type gets its own schema version history
- schemas are shared across all topics that produce that record type

Best for:

- same event type published to multiple topics
- schema versioning independent of topic name
- organizations where event schemas are reused across pipelines

Limitation:

```text
Schema evolution is governed per record type, not per topic. A topic may receive different
schema types and must handle dynamic dispatch.
```

---

### TopicRecordNameStrategy

Subject name = `<topic>-<fully-qualified-record-name>`

```text
topic: order-events
subjects:
  order-events-com.example.orders.OrderPlaced
  order-events-com.example.orders.OrderCancelled
  order-events-com.example.orders.OrderShipped
```

Rules:

- each event type in each topic has its own schema version history
- different topics can produce the same event type with independent schema evolution

Best for:

- topics that carry multiple event types (domain event bus pattern)
- fine-grained schema governance per topic-type combination

Trade-off comparison:

| Strategy | Subject Granularity | Best For |
|---|---|---|
| TopicNameStrategy | one per topic | simple, one event type per topic |
| RecordNameStrategy | one per record type globally | shared event types across topics |
| TopicRecordNameStrategy | one per topic + record type | multi-event topics with strong governance |

Interview answer:

```text
For simple pipelines with one event type per topic, TopicNameStrategy is easiest. For domain
event buses with multiple event types in one topic, TopicRecordNameStrategy gives cleaner
subject governance. RecordNameStrategy is useful when the same event schema is produced
to multiple topics and version history should be shared.
```

---

## 21. Schema Registry HA Deployment And CI/CD Gate

### Schema Registry HA Deployment

Schema Registry supports multi-instance deployment with leader election backed by Kafka.

Architecture:

```text
Multiple Schema Registry instances join one cluster
-> one instance is elected leader
-> write requests (schema registration) go to leader
-> read requests (schema fetch) can be served by any instance
-> schemas are stored in a Kafka compacted topic: _schemas
-> all instances sync from _schemas topic
```

Key config:

```properties
# All instances must use same Kafka cluster
kafkastore.bootstrap.servers=broker1:9092,broker2:9092
kafkastore.topic=_schemas

# Inter-instance communication (for forwarding writes to leader)
listeners=http://0.0.0.0:8081
host.name=schema-registry-host-1
```

HA properties:

- `_schemas` topic is compacted, so Schema Registry state is recovered after restart
- if Schema Registry is unavailable, producers/consumers can still work by using cached schemas
- schema registration fails during outage (writes) but reads from cache continue

Interview trap:

```text
Schema Registry availability is critical for new schema registration, not for reading already-
registered schemas. Producers with cached schema IDs can tolerate short registry outages. But
new schema versions cannot be registered until the registry recovers.
```

DR consideration:

```text
In cross-region Kafka DR, ensure Schema Registry is also available in the failover region.
Consumers trying to deserialize events in the DR cluster need to reach a registry that has
the same schema IDs. Options: replicate _schemas topic, deploy a second registry, or use
Confluent Schema Linking.
```

---

### CI/CD Schema Compatibility Gate

Prevent breaking schema changes from reaching production by integrating compatibility checks into the CI pipeline.

Confluent Schema Registry CLI gate:

```bash
# Check if new schema is compatible before registering
curl -s -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"OrderPlaced\",...}"}' \
  "http://schema-registry:8081/compatibility/subjects/order-events-value/versions/latest"

# Response:
# {"is_compatible": true}  <- green
# {"is_compatible": false} <- fail the build
```

GitHub Actions gate example:

```yaml
- name: Check schema compatibility
  run: |
    RESULT=$(curl -s -X POST \
      -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      --data @schema/order-placed-v2.json \
      "$SCHEMA_REGISTRY_URL/compatibility/subjects/$SUBJECT/versions/latest")
    
    COMPATIBLE=$(echo $RESULT | jq -r '.is_compatible')
    if [ "$COMPATIBLE" != "true" ]; then
      echo "Schema is NOT compatible: $RESULT"
      exit 1
    fi
    echo "Schema is compatible"
```

Workflow placement:

```text
Code PR opened
-> lint / build / test
-> schema compatibility check (fails early on breaking change)
-> canary deploy
-> full deploy
```

Benefits:

- breaking schema changes are caught at PR time, not production time
- developers get immediate feedback before merge
- compliance: schema governance is auditable and automated

Interview answer:

```text
I add schema compatibility checks as a CI gate using the Schema Registry compatibility API.
If the schema is not backward compatible, the build fails before any artifact is deployed.
This prevents silent schema breaks from reaching consumers across teams.
```

---

## 22. CloudEvents Specification

CloudEvents is a CNCF specification for describing event data in a common format across clouds, platforms, and protocols.

### Why It Matters

Kafka does not define what an event envelope should look like. Teams invent their own. CloudEvents provides a standard, interoperable envelope.

CloudEvents is increasingly used at companies adopting multi-cloud or event mesh architectures, and it appears in CNCF-adjacent interviews.

### Required CloudEvents Attributes

| Attribute | Type | Description |
|---|---|---|
| `id` | string | unique ID per event; used for dedupe |
| `source` | URI | identifies the context that created the event (e.g. `/orders/service`) |
| `specversion` | string | CloudEvents version (`"1.0"`) |
| `type` | string | event type in reverse-DNS style (`com.example.orders.created`) |

### Optional But Common Attributes

| Attribute | Type | Description |
|---|---|---|
| `subject` | string | identifies the subject of the event (e.g. `order-id-123`) |
| `time` | timestamp | when the event occurred (RFC 3339) |
| `datacontenttype` | string | content type of `data` (e.g. `application/json`) |
| `dataschema` | URI | URI of schema for the `data` attribute |
| `data` | any | the actual event payload |

### CloudEvents JSON Example

```json
{
  "specversion": "1.0",
  "id": "evt-f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "source": "/payments/payment-service",
  "type": "com.example.payments.authorized",
  "subject": "payment-id-91",
  "time": "2026-06-28T10:15:30Z",
  "datacontenttype": "application/json",
  "dataschema": "https://schema.example.com/payment-authorized.json",
  "traceId": "trace-abc-456",
  "data": {
    "paymentId": "payment-id-91",
    "customerId": "cust-7",
    "amount": 250.00,
    "currency": "USD",
    "status": "authorized"
  }
}
```

### Mapping To Kafka

CloudEvents can be carried as Kafka messages in two ways:

Structured mode (whole event as JSON value):

```text
Kafka value = entire CloudEvents JSON document
```

Binary mode (attributes in headers, data in value):

```text
Kafka headers:
  ce_specversion = 1.0
  ce_id = evt-f47ac10b-...
  ce_type = com.example.payments.authorized
  ce_source = /payments/payment-service

Kafka value = raw data bytes (JSON, Avro, Protobuf)
```

Binary mode is preferred for performance-sensitive pipelines because:

- data can be Avro/Protobuf-encoded without wrapping
- Schema Registry integrates with data, not the envelope headers
- consumers can route on headers without deserializing data

### When To Use CloudEvents

Use CloudEvents when:

- building multi-cloud or event mesh architectures
- integrating with cloud-native services (Azure Event Grid, GCP Eventarc, Knative) that already speak CloudEvents
- standardizing event envelopes across many teams or domains
- building a platform that routes events across Kafka and other brokers

Skip CloudEvents when:

- team is Kafka-only with no cloud-native integration requirements
- overhead of standard envelope is not justified
- existing internal envelope standards already serve the same purpose

Interview line:

```text
CloudEvents is not Kafka-specific, but it provides a standard envelope that lets events move
cleanly between Kafka, HTTP, event grids, and other transports. For multi-cloud or event mesh
designs, it reduces integration friction.
```

