# GraphQL N+1 Latency Debugging Scenario - MAANG Sheet

> Track File #18 of 30 - Group 04: Scenario Practice
> For: performance incident interviews | Level: senior | Mode: latency debugging

## 1. Scenario

```text
A product page GraphQL operation became slow after adding seller and inventory fields.
```

Goal: prove whether nested resolvers are causing fanout.

---

## 2. Debug Flow

```text
operation name/hash -> resolver trace -> data-source call count -> batch/cache behavior -> slow field -> mitigation
```

Evidence:

- resolver spans by path
- data-source query count
- batch size metrics
- operation variables
- page size/depth
- recent schema or client operation change

---

## 3. Likely Causes

- missing DataLoader for seller/inventory
- loader created globally or per resolver call incorrectly
- list page size too high
- nested field fetches unnecessary backend data
- client added expensive fragment to common page

---

## 4. Mitigation

- add request-scoped batching
- cap page size
- optimize backend batch endpoint/query
- split expensive field behind explicit user action
- use persisted query telemetry to find callers

---

## 5. Interview Summary

```text
For GraphQL N+1 latency, I inspect operation identity, resolver traces, data-source call counts, batch size, page size, and recent operation/schema changes. Then I fix with request-scoped DataLoader, bounded lists, backend batch APIs, and complexity controls.
```

---

## 6. Revision Notes

- One-line summary: N+1 is proven by resolver path and data-source call count.
- Three keywords: trace, DataLoader, fanout.
- One trap: scaling servers before fixing resolver fanout.