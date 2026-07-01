# Project 02: Fraud Ring Detection

Goal: model fraud rings through shared devices, emails, cards, IPs, and risk events.

---

## Requirements

- connect accounts to shared signals
- find active accounts near suspended accounts
- explain risk paths
- support streaming updates and false-positive review

---

## Graph Model

```text
(:Account)-[:USES_DEVICE]->(:Device)
(:Account)-[:HAS_EMAIL]->(:Email)
(:Account)-[:PAID_WITH]->(:Card)
(:Account)-[:FLAGGED_AS]->(:RiskEvent)
```

---

## Interview Talking Points

- shared signal weighting
- provenance and timestamps
- false positives
- supernodes from common IPs/devices