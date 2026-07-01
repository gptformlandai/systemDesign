# GraphQL Federation Composition Gateway Incident Scenario - MAANG Sheet

> Track File #21 of 30 - Group 04: Scenario Practice
> For: distributed GraphQL operations | Level: senior | Mode: federation incident

## 1. Scenario

```text
A subgraph deploy causes composition failure or gateway errors for product detail operations.
```

Goal: restore the supergraph safely and identify subgraph ownership issue.

---

## 2. Debug Flow

```text
composition result -> changed subgraph schema -> affected entity/type/field -> query plan -> subgraph health -> rollback/fix
```

Evidence:

- composition error
- schema diff
- router/gateway logs
- operation query plan
- subgraph latency/errors
- affected clients/operations

---

## 3. Likely Causes

- subgraph removed/changed shared field
- entity key changed
- ownership conflict in composed type
- subgraph deploy succeeded but router composition rejected it
- gateway query plan now calls slow/failing subgraph

---

## 4. Mitigation

- stop rollout or rollback subgraph
- publish previous known-good supergraph
- patch schema incompatibility
- route around degraded subgraph if platform supports it
- add composition check to deployment gate

---

## 5. Interview Summary

```text
For federation incidents, I inspect composition errors, changed subgraph schema, entity keys, ownership, router query plans, and subgraph health. I restore a known-good supergraph and add composition checks before deploy.
```

---

## 6. Revision Notes

- One-line summary: Federation incidents are schema composition plus runtime routing problems.
- Three keywords: composition, entity, query plan.
- One trap: debugging only the gateway while the subgraph schema change caused the incident.