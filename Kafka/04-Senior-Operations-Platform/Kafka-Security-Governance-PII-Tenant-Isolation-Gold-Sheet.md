# Kafka Security Governance PII Tenant Isolation Gold Sheet

> Track: Kafka Interview Track - Senior / MAANG Governance Layer

Target: senior backend, platform, and MAANG interviews where Kafka data must be secured, governed, audited, and safely shared across teams.

This sheet fills the security/governance layer: event classification, PII handling, ACL policy design, tenant isolation, schema governance, retention governance, auditability, encryption, secrets, and safe replay.

---

## 0. How To Read This

Use this after the operations/security and schema registry sheets.

Security mental model:

```text
producer identity -> topic permission -> event contract -> data classification -> retention -> consumer identity -> audit/replay controls
```

Strong answer shape:

```text
I do not treat Kafka topics as anonymous pipes. Every topic needs an owner, schema, data
classification, retention policy, ACLs, and a replay/audit story.
```

---

# Topic 1: Kafka Security And Governance

## 1. Intuition

Kafka is a shared memory layer for the company.

If you put sensitive data into Kafka:

- many consumers may read it
- replay can expose old data again
- retention keeps it longer than expected
- logs and DLQs can duplicate it
- schema evolution can leak new fields

Kafka security is about who can publish, who can read, what data is allowed, how long it lives, and how access is audited.

---

## 2. Definition

- Definition: Kafka governance is the set of policies and controls that keep Kafka topics secure, compliant, discoverable, evolvable, and safe for multi-team use.
- Category: event platform security and data governance.
- Core idea: topic data is durable and replayable, so access and data classification must be explicit.

---

## 3. Why It Exists

Without governance:

- wildcard ACLs spread sensitive data
- teams publish PII into broad topics
- DLQs retain bad records with secrets
- schema changes expose fields unexpectedly
- replay jobs duplicate external side effects
- retention violates deletion policy
- audit cannot answer who accessed what
- shared clusters become noisy and unsafe

---

## 4. Kafka Security Layers

| Layer | Control |
|---|---|
| Network | private networking, firewall/security groups |
| Encryption | TLS for traffic, platform encryption at rest |
| Authentication | mTLS, SASL/SCRAM, OAuth, Kerberos, provider IAM |
| Authorization | ACLs/RBAC for topics, groups, transactional IDs, cluster ops |
| Contract | schema registry and compatibility rules |
| Data governance | PII classification, masking/tokenization, retention |
| Operations | audit logs, alerts, owner review, replay approval |

Interview line:

```text
TLS protects traffic, authentication proves identity, ACLs authorize operations, and governance
controls what data is allowed to exist and be replayed.
```

---

## 5. Topic Ownership

Every production topic should have:

- owner team
- business domain
- event purpose
- schema subject
- compatibility mode
- retention policy
- compaction policy if applicable
- PII classification
- allowed producers
- allowed consumers
- expected throughput
- replay policy
- on-call/runbook

Topic metadata example:

```yaml
topic: payments.payment-authorized.v1
owner: payments-platform
classification: confidential
retention: 14d
schema: payments.payment-authorized-value
compatibility: backward
producers:
  - payments-service
consumers:
  - ledger-service
  - fraud-service
replay_requires_approval: true
```

---

## 6. Data Classification For Events

| Class | Examples | Kafka Controls |
|---|---|---|
| public | product catalog event | normal ACLs, standard retention |
| internal | inventory changed | team-scoped ACLs |
| confidential | customer email, address | minimize fields, strict ACLs, audit |
| restricted | payment token, government ID | avoid if possible, tokenize/encrypt, short retention |
| regulated | health/financial regulated data | legal/compliance review, strict audit/retention |

Rule:

```text
Do not put raw sensitive data in Kafka just because consumers might need it later.
```

---

## 7. PII Minimization

Bad event:

```json
{
  "eventType": "BookingCreated",
  "bookingId": "b-123",
  "customerEmail": "alice@example.com",
  "customerPhone": "+1-555-1234",
  "customerAddress": "...",
  "cardNumber": "4111111111111111"
}
```

Better event:

```json
{
  "eventType": "BookingCreated",
  "bookingId": "b-123",
  "customerId": "c-456",
  "hotelId": "h-789",
  "checkIn": "2026-07-10",
  "checkOut": "2026-07-12"
}
```

Strong answer:

```text
I publish stable identifiers and business facts. Consumers that need PII should call an
authorized service or use a governed data product, not scrape it from broad Kafka topics.
```

---

## 8. Tokenization And Field Encryption

Use when sensitive data must move through Kafka.

Options:

- tokenization: replace raw value with token resolvable by authorized system
- field-level encryption: encrypt selected fields before publishing
- envelope encryption: key management outside Kafka
- redaction: remove fields before publishing to broad topics

Trade-offs:

| Control | Benefit | Cost |
|---|---|---|
| tokenization | limits raw data exposure | lookup dependency |
| field encryption | protects sensitive fields from broad readers | key management and consumer complexity |
| redaction | simplest safe default | consumers may need alternate lookup path |

Trap:

```text
Encryption does not solve over-broad ACLs, retention, DLQ leakage, or replay governance.
```

---

## 9. ACL Policy Design

ACLs should be least privilege.

Producer usually needs:

- Write on topic
- Describe on topic
- transactional ID access if using transactions

Consumer usually needs:

- Read on topic
- Describe on topic
- Read on consumer group

Avoid:

- wildcard topic access
- shared service principals across many apps
- broad cluster admin privileges for business services
- using one consumer group ID across unrelated apps

Interview line:

```text
A service principal should only access the topics and groups it owns. Admin operations should
be separated from business runtime credentials.
```

---

## 10. Tenant Isolation

Kafka tenant isolation can mean:

- topic-per-tenant
- tenant field inside event
- tenant-specific ACLs
- separate cluster for high-risk tenants
- quota limits per tenant/client

Shared-topic tenant event example:

```json
{
  "tenantId": "t-123",
  "eventType": "OrderCreated",
  "orderId": "o-456"
}
```

Risks:

- consumers forget tenant filter
- broad ACL lets tenant service read other tenants
- compacted topics leak latest state across tenants
- replay jobs mix tenant boundaries
- schema evolution adds sensitive tenant fields

Strong answer:

```text
Tenant isolation must be enforced through ACLs, topic boundaries, event design, consumer
logic, quotas, and audit. A tenant_id field alone is not isolation.
```

---

## 11. Schema Governance

Schema registry governance should define:

- subject naming strategy
- compatibility mode
- owner approval for breaking changes
- required metadata fields
- banned fields or PII review
- semantic compatibility review
- consumer notification process

Schema rules:

- additive optional fields are usually safest
- removing/renaming fields can break consumers
- changing meaning without changing type is still a breaking semantic change
- defaults matter for backward compatibility
- event envelope should include event id, type, timestamp, version, producer, correlation id when appropriate

---

## 12. DLQ Governance

DLQs often contain the most sensitive and broken payloads.

Controls:

- strict ACLs on DLQ topics
- shorter retention than main topics when possible
- redaction for known sensitive fields
- include error metadata without secrets
- owner alerts and review SLA
- replay approval process
- poison record quarantine by topic/partition/offset

Trap:

```text
A DLQ is not a trash can. It is a production data store with sensitive failure payloads.
```

---

## 13. Replay Governance

Replay risks:

- duplicate payments/emails/refunds
- reprocessing old PII
- overwhelming downstream systems
- violating retention/deletion expectations
- reintroducing old schema assumptions

Replay checklist:

1. Identify topic, partitions, offset/time range.
2. Confirm consumer idempotency.
3. Confirm downstream capacity.
4. Confirm schema compatibility for old events.
5. Use separate replay group or controlled repair job.
6. Rate limit replay.
7. Audit who initiated replay and why.
8. Monitor output side effects.

---

## 14. Retention And Deletion Governance

Kafka retention has compliance impact.

Ask:

- How long should this data live?
- Is it delete retention, compaction, or compact+delete?
- Does the topic contain PII?
- Are DLQs shorter retention?
- Does tiered storage extend retention unexpectedly?
- Are backups/archive copies governed too?
- How do deletion requests interact with immutable events?

Strong answer:

```text
If events contain PII, retention is a governance decision, not only a storage setting.
```

---

## 15. Audit And Observability

Audit:

- topic creation/config changes
- ACL changes
- schema changes
- principal access failures
- replay jobs
- offset resets
- DLQ reprocessing
- production AdminClient actions

Monitor:

- auth failures
- denied ACLs
- certificate expiry
- quota violations
- schema compatibility failures
- DLQ growth
- unexpected new consumers on sensitive topics
- high-volume replay reads

---

## 16. Secure Event Design Checklist

Before approving a topic:

1. Is the topic owner clear?
2. Is the event purpose clear?
3. Does it include only necessary fields?
4. Is PII classified and minimized?
5. Are schema compatibility rules set?
6. Are allowed producers/consumers listed?
7. Are ACLs least privilege?
8. Is retention justified?
9. Is DLQ retention/access controlled?
10. Is replay safe and audited?
11. Are quotas needed?
12. Is the topic discoverable with metadata?

---

## 17. Security Incident Runbook

Scenario:

```text
A service principal unexpectedly reads from a sensitive topic.
```

Runbook:

1. Identify principal, topic, group, time window, and client host.
2. Revoke or narrow ACL if access is not approved.
3. Check whether data was consumed, exported, logged, or replayed.
4. Review recent ACL/schema/topic changes.
5. Notify owner/security/compliance based on classification.
6. Rotate credentials if compromise is suspected.
7. Add alert or policy guardrail to prevent recurrence.

---

## 18. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Publish raw PII broadly | publish IDs and authorize lookup |
| Use wildcard ACLs | least-privilege service principals |
| Ignore DLQ sensitivity | secure DLQ access and retention |
| Treat schema compatibility as only type compatibility | review semantic compatibility |
| Replay without idempotency check | approval, rate limits, dedupe, audit |
| Use tenant_id field as only isolation | combine topic/ACL/consumer/audit controls |
| Keep infinite retention by default | align retention with business/compliance need |
| Give business apps AdminClient cluster privileges | separate platform automation credentials |

---

## 19. Scenario

Prompt:

```text
A payments team wants to publish payment events to Kafka for fraud, ledger, analytics, and support tools. How do you govern it?
```

Strong answer:

```text
I would classify the data first and avoid raw card or sensitive PII in the event. The event
should carry payment id, customer id, status, amount, currency, event id, timestamp, and
correlation id, with schema registry compatibility rules. Producers and consumers get
least-privilege ACLs, with separate principals for fraud, ledger, and analytics. Retention is
business-approved, DLQ access is restricted, and replay requires approval because duplicate
payments or ledger entries are dangerous. I would audit schema changes, ACL changes, replay
jobs, and unexpected consumers.
```

---

## 20. Revision Notes

- Kafka topics need owner, schema, classification, ACLs, retention, and replay policy.
- Publish IDs and business facts, not broad raw PII.
- ACLs protect topic/group operations; governance controls what data should exist.
- DLQs and replay jobs are high-risk data paths.
- Tenant isolation needs topic/ACL/design/audit controls, not just a tenant field.
- Schema compatibility must include semantic compatibility.
- Retention is a security/compliance decision when data is sensitive.

---

## 21. Official Source Notes

- Apache Kafka security docs: https://kafka.apache.org/43/security/
- Apache Kafka authorization and ACLs: https://kafka.apache.org/43/security/authorization-and-acls/
- Confluent Schema Registry docs: https://docs.confluent.io/platform/current/schema-registry/index.html
