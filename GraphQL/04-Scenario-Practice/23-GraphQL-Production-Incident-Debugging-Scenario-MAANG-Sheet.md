# GraphQL Production Incident Debugging Scenario - MAANG Sheet

> Track File #23 of 30 - Group 04: Scenario Practice
> For: on-call and SRE interviews | Level: senior | Mode: incident response

## 1. Scenario

```text
GraphQL error rate and latency increase after a client release.
```

Goal: identify affected operations and restore service safely.

---

## 2. Incident Flow

```text
scope -> operation names/hashes -> error paths -> resolver traces -> data-source calls -> recent schema/client change -> mitigation
```

Evidence:

- operation name/hash
- variables and client version
- validation vs execution error split
- resolver span latency
- data-source call count and errors
- query complexity/depth
- cache hit/miss behavior

---

## 3. Mitigations

- rollback client persisted operation
- disable or gate expensive field
- restore previous schema/supergraph
- tighten depth/complexity limits
- add/load DataLoader batching
- rate-limit abusive operation
- degrade optional field instead of failing whole response

---

## 4. RCA Questions

- Which operation changed?
- Which field path caused latency/errors?
- Did nullability amplify the failure?
- Did auth/cache/loader scope change?
- Was there a schema or resolver deploy?
- Which guardrail was missing?

---

## 5. Interview Summary

```text
For GraphQL incidents, I group by operation name/hash, separate validation from execution errors, inspect error paths and resolver traces, measure data-source fanout, correlate client/schema changes, mitigate safely, and add guardrails such as persisted queries, complexity limits, and tests.
```

---

## 6. Revision Notes

- One-line summary: GraphQL incidents are best scoped by operation and resolver path.
- Three keywords: operation hash, error path, resolver trace.
- One trap: treating all failures as `/graphql` endpoint failures without operation-level evidence.