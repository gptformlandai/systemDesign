# Neo4j Fraud, Risk, and Identity Graph - Gold Sheet

> Track File #18 of 30 - Group 04: Scenario Practice
> For: backend/risk/system design interviews | Level: senior | Mode: fraud rings, shared signals, investigation paths

This sheet builds:
- Fraud graph modeling
- Shared-entity traversal patterns
- Risk scoring and investigation workflows

---

## 1. Requirements

- connect accounts by devices, emails, phones, cards, addresses, IPs
- detect fraud rings
- explain why an account is risky
- support investigator queries
- handle streaming updates

---

## 2. Model

```text
(:Account)-[:USES_DEVICE]->(:Device)
(:Account)-[:HAS_EMAIL]->(:Email)
(:Account)-[:PAID_WITH]->(:Card)
(:Account)-[:LOGGED_IN_FROM]->(:IpAddress)
(:Account)-[:FLAGGED_AS]->(:RiskEvent)
```

Relationship properties:

- first seen
- last seen
- count
- source
- confidence

---

## 3. Shared Signal Query

```cypher
MATCH (a:Account {accountId: $accountId})-->(signal)<--(other:Account)
WHERE other <> a
RETURN other.accountId, labels(signal) AS signalType, count(*) AS sharedSignals
ORDER BY sharedSignals DESC
LIMIT 20;
```

---

## 4. Risk Controls

- avoid treating common public signals as strong fraud proof
- weight signal types differently
- track provenance and timestamps
- avoid unlimited ring traversal
- review false positives

---

## 5. Strong Answer

```text
For fraud detection, I would model accounts, devices, cards, emails, addresses, IPs, and risk events as nodes, connected by typed relationships with timestamps and confidence. Queries start from a suspicious account and traverse bounded shared-signal patterns to find related accounts and explain risk. I would weight signals, track provenance, stream updates, and monitor false positives because a graph can reveal correlation but not automatically prove fraud.
```

---

## 6. Revision Notes

- One-line summary: Fraud graphs connect weak signals into explainable risk paths.
- Three keywords: shared signal, provenance, false positive.
- One interview trap: treating every shared IP as fraud.
- Memory trick: fraud graph is investigation, not magic certainty.