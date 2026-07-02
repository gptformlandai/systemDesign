# Microservices Privacy, Compliance, And Data Lifecycle Gold Sheet

> Track: Microservices Interview Track - Senior Path  
> Goal: design microservices that handle PII, retention, erasure, residency, audit, and compliance without vague hand-waving.

---

## 1. Intuition

In a monolith, data is hard to govern. In microservices, data is harder to find.

The privacy question is:

```text
Where does this data live, who can access it, how long do we keep it, and how do we delete it?
```

Microservices make that harder because data spreads across service databases, caches, logs,
events, search indexes, read models, analytics stores, backups, and third parties.

---

## 2. Definition

- Definition: privacy and compliance data lifecycle is the set of controls for classifying,
  minimizing, protecting, retaining, deleting, auditing, and proving correct handling of data.
- Category: security, architecture governance, regulated systems.
- Core idea: every service that stores or emits sensitive data must own its lifecycle.

---

## 3. Data Classification

Classify data before designing flows.

| Data | Classification | Design Impact |
|---|---|---|
| booking ID | internal identifier | safe but still access-controlled |
| email | PII | minimize, mask in logs |
| phone | PII | protect, retention policy |
| payment token | sensitive/payment | avoid raw card storage |
| full card number | PCI scope | avoid storing unless required |
| passport ID | highly sensitive PII | strict access and retention |
| loyalty history | personal/business | access and deletion rules |
| audit log | compliance evidence | durable, tamper-resistant |

Rule:

```text
Do not put data into an event, log, cache, or read model unless a consumer truly needs it.
```

---

## 4. Data Inventory

Every service should document:

- data owner
- data fields
- classification
- storage location
- retention policy
- encryption method
- access roles
- audit requirements
- downstream consumers
- event payloads
- cache/search copies
- deletion behavior

Service catalog should include data classification.

---

## 5. Data Minimization

Bad event:

```json
{
  "bookingId": "B123",
  "email": "guest@example.com",
  "phone": "555-0100",
  "fullAddress": "..."
}
```

Better event:

```json
{
  "bookingId": "B123",
  "userId": "U123",
  "status": "CONFIRMED"
}
```

If Notification Service needs email, it can call a profile service with authorization or
consume a carefully scoped contact event.

---

## 6. Right To Erasure

Erasure in microservices is a workflow, not a single SQL delete.

Flow:

```text
1. Receive deletion request.
2. Verify requester identity and legal eligibility.
3. Create deletion case ID.
4. Identify owning services.
5. Services delete, anonymize, or retain with legal basis.
6. Delete caches/search/read models.
7. Mark event-stream strategy.
8. Record audit evidence.
9. Notify requester when complete.
```

Important:

- some records may be retained for legal/audit reasons
- delete from derived stores too
- event streams may require tombstone/anonymization strategy
- backups usually follow retention expiration process
- audit logs should prove action without leaking data

---

## 7. Events And Deletion

Event streams are tricky because events are historical records.

Strategies:

| Strategy | Use When | Trade-Off |
|---|---|---|
| avoid PII in events | default | best prevention |
| reference by ID | consumers can fetch if authorized | extra lookup |
| encrypt field with key | crypto-shredding possible | key lifecycle complexity |
| tombstone/anonymization event | read models can remove data | consumers must handle |
| short retention | event not long-lived | replay window reduced |

Strong line:

```text
The easiest PII to delete from events is PII we never put there.
```

---

## 8. Read Models, Search, And Caches

Deletion must propagate to:

- service database
- cache
- search index
- materialized read model
- analytics table
- ML feature store if used
- logs if policy allows masking/deletion
- downstream partners where contract requires

Design:

```text
UserDeleted/UserAnonymized event -> projection workers remove or mask derived copies
```

Need metrics:

- deletion request age
- pending deletion tasks
- failed deletion tasks
- derived store lag
- missed consumer count

---

## 9. Data Residency

Data residency asks:

```text
Which geographic region is this data allowed to live in?
```

Microservices concerns:

- primary database region
- replica region
- queue/broker region
- logs/traces region
- backups
- cross-region failover
- support access
- third-party processors

Interview line:

```text
A multi-region design must respect data residency. Failover cannot silently move regulated
data into a forbidden region.
```

---

## 10. PCI Boundary

Payment design goal:

```text
keep raw card data out of your services if possible
```

Use:

- hosted payment page or payment provider tokenization
- payment token instead of card number
- strict audit for payment actions
- encryption in transit and at rest
- least privilege
- separate payment service boundary
- no payment data in logs/events

Bad:

```text
Booking Service logs full payment payload for debugging.
```

Better:

```text
Payment Service stores provider token, masked last4 if allowed, audit record, and no raw card.
```

---

## 11. Audit Evidence

Audit logs should record:

- actor
- subject
- action
- resource
- timestamp
- request/correlation ID
- decision
- reason code
- service identity
- before/after where appropriate and safe

Audit logs should be:

- durable
- tamper-resistant
- searchable
- access-controlled
- retained by policy
- free of secrets/raw tokens

---

## 12. Access Control

Controls:

- least privilege service identities
- scoped user permissions
- tenant isolation
- field-level authorization for sensitive fields
- break-glass access with audit
- support tooling access restrictions
- data export approval

Common failure:

```text
Search index includes tenant ID but query forgets tenant filter.
```

Mitigation:

- tenant-aware indexes
- mandatory tenant filter
- tests for cross-tenant leakage
- audit and alert unusual access

---

## 13. Privacy Incident Response

Scenario:

```text
Tenant A sees Tenant B booking details in search results.
```

Response:

1. Stop exposure: disable endpoint or bad index.
2. Scope affected tenants/users.
3. Preserve evidence.
4. Fix authorization/index logic.
5. Invalidate bad caches/search docs.
6. Rebuild derived store safely.
7. Notify per policy.
8. Add tests and monitoring.
9. Record RCA and prevention.

---

## 14. Interview Question

> A user requests deletion of their account in a microservice hotel platform. How do you design this?

Strong answer:

```text
I first classify the user's data and identify owning services and derived copies. I create a
deletion workflow with a case ID. Each service deletes, anonymizes, or retains data according
to legal basis. Derived stores like caches, search indexes, read models, analytics, and
partner exports must be handled too. I avoid PII in events where possible, use tombstone or
anonymization events for projections, track deletion task age/failures, and keep audit
evidence without leaking sensitive data.
```

---

## 15. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| delete only primary DB row | derived stores still leak data | lifecycle workflow |
| put PII in every event | hard to delete/control | minimize event payloads |
| logs contain tokens/PII | privacy/security leak | masking and log policy |
| ignore backups | incomplete lifecycle | retention-based backup strategy |
| one tenant filter in app code | easy to miss | mandatory tenant isolation design |
| no deletion metrics | silent backlog | deletion SLO/dashboard |
| no audit evidence | cannot prove compliance | durable audit records |

---

## 16. Strong Closing Answer

```text
In microservices, privacy is a distributed data lifecycle problem. I classify data, minimize
what flows through APIs/events/logs, assign service ownership, define retention and deletion,
protect derived stores, respect residency, and keep audit evidence. The best design prevents
sensitive data from spreading unnecessarily in the first place.
```

