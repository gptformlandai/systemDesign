# Architecture Comparison PostgreSQL vs MongoDB Document Tradeoffs - Gold Sheet

> Track File #6 of 30 - Group 02: Intermediate Backend
> For: backend/system design interviews | Level: intermediate | Mode: relational vs document

## 1. Use PostgreSQL When

- relational integrity matters
- joins are natural and frequent
- transactions span multiple related entities
- constraints protect correctness
- reporting/ad hoc queries matter
- schema is stable enough to model clearly

## 2. Use MongoDB When

- access pattern reads/writes document aggregates
- flexible schema is useful
- nested JSON maps naturally to the domain
- related data is usually read together
- horizontal scaling around document keys is useful

---

## 3. Tradeoff Table

| Dimension | PostgreSQL | MongoDB |
|---|---|---|
| model | relational tables | JSON-like documents |
| joins | first-class | possible but not core design center |
| constraints | strong | fewer relational constraints |
| schema | explicit | flexible |
| transactions | mature ACID | supported, but model often avoids broad transactions |
| best fit | orders, payments, workflow state | profiles, catalogs, content aggregates |

---

## 4. Scenario Answer

For user profiles with nested preferences and settings, MongoDB can fit well.

For orders, payments, inventory reservations, and ledger-like workflows, PostgreSQL is usually safer.

---

## 5. Interview Summary

```text
PostgreSQL is stronger when relationships, constraints, joins, and transaction correctness dominate. MongoDB is stronger when the domain is naturally aggregate/document-shaped and the application usually reads the full document together. If the document grows unbounded or needs many cross-document joins, I would reconsider the model.
```

---

## 6. Revision Notes

- One-line summary: PostgreSQL protects relationships; MongoDB optimizes document aggregates.
- Three keywords: joins, aggregate, transaction.
- One trap: putting unbounded arrays into one MongoDB document.