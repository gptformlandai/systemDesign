# Lab 04: Fraud And Identity Graph

Goal: find related accounts through shared signals.

---

## Run

```bash
bash SCRIPTS/run-cypher.sh SCRIPTS/05-fraud-identity.cypher
```

---

## What To Observe

- accounts can share devices, emails, or cards
- suspended accounts can be near active accounts through shared signals
- relationship properties carry first-seen/count evidence

---

## Explain Out Loud

```text
Why does a shared signal create suspicion but not proof?
```

---

## Completion Gate

- You can explain shared-signal traversal.
- You can explain false positives.
- You can describe signal weighting and provenance.