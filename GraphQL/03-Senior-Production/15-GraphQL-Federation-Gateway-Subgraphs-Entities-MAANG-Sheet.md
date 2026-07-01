# GraphQL Federation, Gateway, Subgraphs, Entities - MAANG Sheet

> Track File #15 of 30 - Group 03: Senior Production
> For: distributed API ownership | Level: senior | Mode: federation

## 1. Core Idea

Federation composes multiple team-owned subgraphs into one supergraph exposed through a router/gateway.

```text
client operation -> router query plan -> subgraph calls -> entity resolution -> composed response
```

---

## 2. Federation Parts

| Part | Meaning |
|---|---|
| subgraph | team-owned GraphQL schema/service |
| supergraph | composed schema exposed to clients |
| router/gateway | plans and routes operations across subgraphs |
| entity | type addressable across subgraphs, usually by key |
| composition | validation that subgraph schemas combine safely |

---

## 3. Ownership Rules

- define clear type/field ownership
- avoid circular subgraph dependencies
- keep entity keys stable
- monitor router and subgraph latency separately
- run composition checks before deploy
- treat breaking changes as cross-team events

---

## 4. Failure Modes

- composition fails and blocks deploy
- entity resolver causes N+1 across subgraphs
- one subgraph outage degrades many operations
- ownership conflict over shared type fields
- gateway query plan is unexpectedly expensive

---

## 5. Interview Summary

```text
GraphQL federation enables distributed schema ownership through subgraphs, entities, composition, and a router. I manage it with ownership rules, composition checks, entity key stability, query-plan observability, and subgraph SLOs.
```

---

## 6. Revision Notes

- One-line summary: Federation is distributed schema ownership with router-mediated execution.
- Three keywords: subgraph, entity, composition.
- One trap: treating federation as a magic merge instead of an operational dependency graph.