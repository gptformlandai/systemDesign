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

---

## 22. Cloud-Native Kafka Authentication

Modern Kafka deployments run on managed platforms. Authentication differs by provider.

### AWS MSK IAM Authentication

AWS MSK supports IAM-based authentication using `SASL_IAM` mechanism.

How it works:

```text
Kafka client uses AWS credentials provider
-> signs Kafka connection request with IAM credentials
-> MSK validates against IAM policy
-> topic-level permissions are IAM resource policies, not Kafka ACLs
```

Producer config (MSK IAM):

```properties
security.protocol=SASL_SSL
sasl.mechanism=AWS_MSK_IAM
sasl.jaas.config=software.amazon.msk.auth.iam.IAMLoginModule required;
sasl.client.callback.handler.class=software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

Key difference from Apache Kafka ACLs:

- MSK IAM uses IAM policies to control topic access, not `kafka-acls.sh`
- IAM roles can be attached to ECS tasks, Lambda, EC2 instances, or EC2 instance profiles
- service-to-topic permissions are expressed as IAM resource policies

Interview trap:

```text
On MSK IAM, do not add native Kafka ACLs unless you also set an Authorizer. With IAM, the IAM
policy IS the authorization. Mixing both requires understanding which authorizer wins.
```

---

### Confluent Cloud Authentication

Confluent Cloud uses API keys, service accounts, and OAuth tokens.

Options:

| Mechanism | Best For |
|---|---|
| API Key + Secret | simple service-to-cluster auth |
| Service Account + RBAC | multi-team platform governance |
| OAuth/OIDC (Confluent Cloud) | SSO/enterprise identity integration |

SASL/PLAIN config for Confluent Cloud API key:

```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="<api-key>" \
  password="<api-secret>";
```

Confluent RBAC roles:

| Role | Scope |
|---|---|
| DeveloperRead | read topic/group |
| DeveloperWrite | produce to topic |
| ResourceOwner | full topic management |
| ClusterAdmin | cluster management |

Strong answer:

```text
On Confluent Cloud, I prefer service accounts with RBAC role bindings over shared API keys.
Each service gets a dedicated service account with the minimum required roles.
```

---

### Azure Event Hubs (Kafka Protocol)

Azure Event Hubs supports the Kafka protocol with SAS token or Azure AD/managed identity authentication.

SAS token config:

```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="$ConnectionString" \
  password="<Event Hubs connection string>";
bootstrap.servers=<namespace>.servicebus.windows.net:9093
```

Managed identity (preferred for production):

- assign Managed Identity to compute resource
- bind Azure EventHubs Data Owner/Sender/Receiver role
- use `SASL_OAUTHBEARER` with Azure AD token provider

Key caveats:

- consumer groups in Event Hubs map to Event Hubs consumer groups, not Kafka groups exactly
- partition count cannot be changed after creation
- retention is time-based only; no log compaction support

---

### SASL/OAUTHBEARER

`SASL_OAUTHBEARER` is the standard mechanism for OAuth/JWT token-based Kafka auth.

Flow:

```text
client requests token from identity provider (OAuth2/OIDC)
-> token included in Kafka SASL handshake
-> broker validates token (signature, expiry, claims)
-> Kafka maps token claim to principal for ACL checks
```

Config skeleton:

```properties
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
sasl.login.callback.handler.class=<provider-specific-class>
sasl.oauthbearer.token.endpoint.url=https://idp.example.com/oauth2/token
```

Token refresh:

- `sasl.login.refresh.min.period.seconds` controls token refresh frequency
- tokens must be refreshed before expiry to avoid sudden auth failure
- alert on refresh errors

---

### mTLS Certificate Rotation Runbook

mTLS uses client certificates for authentication. Rotating certificates without downtime requires careful ordering.

Rotation strategy:

```text
Step 1: Add new CA cert to broker truststore (brokers trust both old and new CA)
Step 2: Distribute new CA truststore to all brokers and rolling restart
Step 3: Issue new client certificates signed by new CA
Step 4: Roll out new client certs to services (one by one, verify each)
Step 5: Remove old CA cert from broker truststore
Step 6: Final broker rolling restart to clean up old trust anchor
```

Critical rule:

```text
Brokers must trust the new CA BEFORE any service sends a new certificate.
The old CA must remain trusted UNTIL all services are using the new certificate.
```

Monitor during rotation:

- authentication failure rate per principal
- SSL handshake error rate
- certificate expiry metrics (`ssl.valid.to` JMX metric)

Alert on:

- certificate expiry within 30 days
- sudden auth failure spike after certificate rollout

---

## 23. GDPR Right-to-Erasure And Kafka

GDPR Article 17 gives individuals the right to request deletion of their personal data.

### The Core Challenge

Kafka logs are immutable and append-only. You cannot surgically delete one event.

The tension:

```text
GDPR: delete user data on request
Kafka: immutable, durable, replayable log
```

This is one of the most common senior governance interview questions.

---

### Option 1: Avoid PII In Topics

Best long-term approach:

```text
Publish identifiers and business facts, not raw PII.
Store PII in a governed data store with deletion support.
Consumers fetch PII on demand, not from Kafka replay.
```

Interview line:

```text
The best GDPR strategy for Kafka is to not put PII in topics at all. Publish customer IDs, not
names, emails, or addresses. If a consumer needs PII to process the event, it fetches it
from the authorized source of truth, not from a Kafka replay.
```

---

### Option 2: Crypto-Shredding

Crypto-shredding means encrypting PII fields with a per-user encryption key, then deleting the key when the user requests erasure.

Flow:

```text
publish event:
  PII field is encrypted with user-specific key stored in KMS
  Kafka holds encrypted ciphertext only
  
deletion request:
  delete the user's encryption key from KMS
  Kafka records still exist but PII is permanently unreadable
```

Properties:

- Kafka data is not modified (immutable log preserved)
- PII becomes unreadable after key deletion
- compliant for most interpretations of GDPR Article 17
- applies to all replicas, backups, and tiered storage automatically

Trade-offs:

| Pro | Con |
|---|---|
| No log rewrite needed | per-user key management complexity |
| Works with immutable Kafka | higher encryption/decryption cost |
| Applies to replicas automatically | key rotation adds overhead |
| Audit trail preserved | old events remain as ciphertext |

Tiered storage note:

```text
Crypto-shredding also covers tiered storage because the encrypted payload is the
same bytes in remote storage. Key deletion renders all copies unreadable.
```

---

### Option 3: Tombstone Records (Compacted Topics)

For compacted topics, a tombstone record with `null` value and the user's key signals deletion.

Example:

```text
topic: customer-profile-state (compacted)
key: customer-id-456
value: null  <- tombstone
```

After compaction:

- all records with key `customer-id-456` are removed from the log
- tombstone itself is eventually removed after `delete.retention.ms`

Limitations:

- only works for compacted topics with delete.cleanup.policy
- does not help delete the key from offset-based (non-compacted) topics with time-based retention
- tiered storage complicates tombstone propagation if cold segments are already in remote storage

---

### Option 4: Short Retention + Source-of-Truth Model

If PII must be in Kafka:

- set short retention (hours, not days)
- treat Kafka as a transient buffer, not the system of record
- store PII in a governed database with deletion support
- design consumers to be stateless or persist only anonymized derived state

When retention expires, PII is gone naturally.

---

### GDPR Kafka Checklist

Before putting any user data in a topic:

1. Is this PII or regulated data?
2. Can it be replaced by an identifier?
3. Is short retention appropriate?
4. If PII must be included, is crypto-shredding implemented?
5. Does tiered storage behavior affect retention?
6. Is DLQ retention shorter than main topic?
7. Is there an audit log of deletion requests and key destruction?
8. Are backups and archives governed by the same policy?
9. Can consumers handle null/missing PII gracefully after deletion?

---

## 24. Event Lineage And Provenance

Regulated industries and audit-heavy platforms often ask: "Where did this event come from, and what happened to it?"

### Event Envelope For Lineage

A lineage-aware event carries:

```json
{
  "eventId": "evt-uuid-123",
  "eventType": "PaymentAuthorized",
  "eventVersion": 2,
  "producer": "payment-service",
  "producedAt": "2026-06-28T10:15:30Z",
  "traceId": "trace-abc-456",
  "spanId": "span-def-789",
  "correlationId": "request-ghi-012",
  "causationId": "evt-uuid-100",
  "schemaId": 42
}
```

Field purposes:

| Field | Use |
|---|---|
| `eventId` | dedupe and traceability per event |
| `producer` | who published this event |
| `producedAt` | business time vs ingestion time |
| `traceId` / `spanId` | distributed tracing correlation |
| `correlationId` | link to originating API request |
| `causationId` | link to parent event that caused this event |
| `schemaId` | which schema version encoded this payload |

### Causation vs Correlation

- `correlationId`: links all events originating from one external request
- `causationId`: links one event to the specific event that caused it

Example:

```text
HTTP request -> OrderCreated (correlationId=R1, causationId=null)
OrderCreated -> PaymentRequested (correlationId=R1, causationId=OrderCreated.eventId)
PaymentRequested -> PaymentAuthorized (correlationId=R1, causationId=PaymentRequested.eventId)
```

Interview line:

```text
correlationId tells you which user request caused this chain. causationId tells you which specific
event in the chain caused this specific event. Together they reconstruct causal history.
```

### Lineage Monitoring

Audit systems can use event headers and topic to:

- trace which events a user's action produced
- identify which events contributed to a downstream output
- confirm compliance events were published and consumed
- reconstruct processing history after an incident
