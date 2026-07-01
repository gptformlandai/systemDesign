# GraphQL Mastery Track - Beginner To Pro Index

This folder is a complete GraphQL mastery track for backend engineers, frontend/full-stack engineers, platform engineers, and system design interviews.

It teaches GraphQL as a production API architecture, not just a query syntax.

```text
product use case -> schema contract -> operation -> resolver execution -> data source access -> performance/security/observability -> production answer
```

Use this track if:

- You want beginner-to-pro GraphQL confidence for API design, backend implementation, frontend integration, and interviews.
- You want to understand schemas, queries, mutations, subscriptions, resolvers, validation, errors, pagination, caching, and federation deeply.
- You want MAANG-level answers connecting GraphQL to API gateways, schema governance, N+1 performance, authorization, observability, and distributed ownership.
- You want hands-on labs, runbooks, portfolio projects, and scenario drills instead of reading-only notes.

---

## 1. Learning Style: Beginner To Pro Loop

Every topic should be learned with this loop:

```text
concept -> SDL/query -> execution behavior -> resolver/data access -> failure mode -> fix -> production scenario -> interview explanation
```

GraphQL mastery is not memorizing `{ user { id } }`. It is understanding how schema, operation, resolver, data source, cache, auth, and observability interact.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Foundations` | GraphQL mental model, schema, operations, type system, resolver basics |
| 2 | `02-Intermediate-Practical` | resolvers, errors, pagination, DataLoader, auth, clients |
| 3 | `03-Senior-Production` | schema governance, performance, security, observability, federation, caching |
| 4 | `04-Scenario-Practice` | API design, N+1, auth leaks, schema evolution, federation, real-time, incidents |
| 5 | `05-Special-Interview-Rounds` | Q&A, cheat sheets, anti-patterns, debugging traps |
| 6 | `06-Practice-Upgrade` | active recall, drills, mini projects, production readiness checklist |
| Lab | `graphql-mastery-lab` | runnable examples, scripts, labs, projects, cheatsheets, interview prep, runbooks |

---

## 3. Foundations Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Foundations/01-GraphQL-Mental-Model-Schema-Operations-Resolvers-Hot-Sheet.md](01-Foundations/01-GraphQL-Mental-Model-Schema-Operations-Resolvers-Hot-Sheet.md) | GraphQL mental model, graph-shaped API, schema, operation, resolver |
| 2 | [01-Foundations/02-GraphQL-Setup-Tools-Playground-Server-Client-Basics-Gold-Sheet.md](01-Foundations/02-GraphQL-Setup-Tools-Playground-Server-Client-Basics-Gold-Sheet.md) | local tooling, server/client basics, introspection, GraphiQL/Apollo Sandbox |
| 3 | [01-Foundations/03-GraphQL-Schema-SDL-Types-Nullability-Inputs-Gold-Sheet.md](01-Foundations/03-GraphQL-Schema-SDL-Types-Nullability-Inputs-Gold-Sheet.md) | SDL, objects, scalars, lists, non-null, enums, input types |
| 4 | [01-Foundations/04-GraphQL-Queries-Mutations-Subscriptions-Fragments-Variables-Gold-Sheet.md](01-Foundations/04-GraphQL-Queries-Mutations-Subscriptions-Fragments-Variables-Gold-Sheet.md) | queries, mutations, subscriptions, variables, fragments, directives |

Foundation target:

- You can explain schema vs operation vs resolver vs data source.
- You can read and write basic SDL and operations.
- You can explain why GraphQL is a contract and execution model, not just JSON over HTTP.

---

## 4. Intermediate Practical Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Practical/05-GraphQL-Resolvers-Execution-Context-DataSources-Gold-Sheet.md](02-Intermediate-Practical/05-GraphQL-Resolvers-Execution-Context-DataSources-Gold-Sheet.md) | resolver execution, parent/args/context/info, data sources |
| 6 | [02-Intermediate-Practical/06-GraphQL-Validation-Errors-Null-Bubbling-Response-Shape-Gold-Sheet.md](02-Intermediate-Practical/06-GraphQL-Validation-Errors-Null-Bubbling-Response-Shape-Gold-Sheet.md) | validation, execution errors, null bubbling, partial responses |
| 7 | [02-Intermediate-Practical/07-GraphQL-Pagination-Filtering-Sorting-Connections-Gold-Sheet.md](02-Intermediate-Practical/07-GraphQL-Pagination-Filtering-Sorting-Connections-Gold-Sheet.md) | offset/cursor pagination, Relay connection pattern, filtering/sorting |
| 8 | [02-Intermediate-Practical/08-GraphQL-DataLoader-NPlusOne-Batching-Caching-Gold-Sheet.md](02-Intermediate-Practical/08-GraphQL-DataLoader-NPlusOne-Batching-Caching-Gold-Sheet.md) | N+1, batching, per-request caching, resolver performance |
| 9 | [02-Intermediate-Practical/09-GraphQL-Auth-Authorization-Context-Field-Level-Gold-Sheet.md](02-Intermediate-Practical/09-GraphQL-Auth-Authorization-Context-Field-Level-Gold-Sheet.md) | authentication, authorization, context, field-level access control |
| 10 | [02-Intermediate-Practical/10-GraphQL-Client-Integration-Apollo-Relay-Normalized-Cache-Gold-Sheet.md](02-Intermediate-Practical/10-GraphQL-Client-Integration-Apollo-Relay-Normalized-Cache-Gold-Sheet.md) | clients, generated types, normalized cache, optimistic UI basics |

Practical target:

- You can implement resolvers, return predictable errors, paginate safely, prevent N+1, enforce auth, and integrate clients.

---

## 5. Senior Production Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-Production/11-GraphQL-Schema-Design-Evolution-Governance-MAANG-Sheet.md](03-Senior-Production/11-GraphQL-Schema-Design-Evolution-Governance-MAANG-Sheet.md) | schema design, naming, deprecation, compatibility, governance |
| 12 | [03-Senior-Production/12-GraphQL-Performance-Complexity-Depth-Persisted-Queries-MAANG-Sheet.md](03-Senior-Production/12-GraphQL-Performance-Complexity-Depth-Persisted-Queries-MAANG-Sheet.md) | N+1, depth/complexity limits, APQ, cost controls |
| 13 | [03-Senior-Production/13-GraphQL-Security-Auth-Introspection-Rate-Limits-Injection-MAANG-Sheet.md](03-Senior-Production/13-GraphQL-Security-Auth-Introspection-Rate-Limits-Injection-MAANG-Sheet.md) | authz, introspection policy, rate limiting, input validation, injection safety |
| 14 | [03-Senior-Production/14-GraphQL-Observability-Tracing-Metrics-Errors-SLOs-Gold-Sheet.md](03-Senior-Production/14-GraphQL-Observability-Tracing-Metrics-Errors-SLOs-Gold-Sheet.md) | resolver tracing, metrics, error classification, SLOs |
| 15 | [03-Senior-Production/15-GraphQL-Federation-Gateway-Subgraphs-Entities-MAANG-Sheet.md](03-Senior-Production/15-GraphQL-Federation-Gateway-Subgraphs-Entities-MAANG-Sheet.md) | federation, gateway/router, subgraphs, entities, composition |
| 16 | [03-Senior-Production/16-GraphQL-Caching-CDN-Response-Entity-Persisted-Query-MAANG-Sheet.md](03-Senior-Production/16-GraphQL-Caching-CDN-Response-Entity-Persisted-Query-MAANG-Sheet.md) | HTTP caching limits, entity cache, persisted query cache, CDN strategy |
| Gap fill | [03-Senior-Production/31-GraphQL-Pro-Gap-Fill-Transport-Scalars-Directives-Mutations-Testing-MAANG-Sheet.md](03-Senior-Production/31-GraphQL-Pro-Gap-Fill-Transport-Scalars-Directives-Mutations-Testing-MAANG-Sheet.md) | HTTP transport/status, custom scalars/directives, polymorphism, mutation idempotency, contract testing, incremental delivery |

Senior target:

- You can explain GraphQL in production: schema governance, performance controls, security, observability, federation, and caching tradeoffs.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/17-GraphQL-Design-Ecommerce-API-Scenario-Gold-Sheet.md](04-Scenario-Practice/17-GraphQL-Design-Ecommerce-API-Scenario-Gold-Sheet.md) | design an e-commerce/product API with GraphQL |
| 18 | [04-Scenario-Practice/18-GraphQL-NPlusOne-Latency-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/18-GraphQL-NPlusOne-Latency-Debugging-Scenario-MAANG-Sheet.md) | debug resolver fanout and latency |
| 19 | [04-Scenario-Practice/19-GraphQL-Authorization-Data-Leak-Scenario-MAANG-Sheet.md](04-Scenario-Practice/19-GraphQL-Authorization-Data-Leak-Scenario-MAANG-Sheet.md) | field-level and object-level auth incident |
| 20 | [04-Scenario-Practice/20-GraphQL-Schema-Evolution-Breaking-Change-Scenario-Gold-Sheet.md](04-Scenario-Practice/20-GraphQL-Schema-Evolution-Breaking-Change-Scenario-Gold-Sheet.md) | backward-compatible schema evolution |
| 21 | [04-Scenario-Practice/21-GraphQL-Federation-Composition-Gateway-Incident-Scenario-MAANG-Sheet.md](04-Scenario-Practice/21-GraphQL-Federation-Composition-Gateway-Incident-Scenario-MAANG-Sheet.md) | gateway/subgraph composition incident |
| 22 | [04-Scenario-Practice/22-GraphQL-Subscriptions-Realtime-Scenario-Gold-Sheet.md](04-Scenario-Practice/22-GraphQL-Subscriptions-Realtime-Scenario-Gold-Sheet.md) | subscriptions, events, WebSocket/SSE tradeoffs |
| 23 | [04-Scenario-Practice/23-GraphQL-Production-Incident-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/23-GraphQL-Production-Incident-Debugging-Scenario-MAANG-Sheet.md) | production incident response and mitigation |

Scenario target:

- You can diagnose realistic GraphQL problems with a repeatable evidence path.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/24-GraphQL-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md](05-Special-Interview-Rounds/24-GraphQL-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md) | GraphQL Q&A from beginner to MAANG |
| 25 | [05-Special-Interview-Rounds/25-GraphQL-Schema-Query-Cheat-Sheet-And-Decision-Map.md](05-Special-Interview-Rounds/25-GraphQL-Schema-Query-Cheat-Sheet-And-Decision-Map.md) | schema/query/resolver decision map |
| 26 | [05-Special-Interview-Rounds/26-GraphQL-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md](05-Special-Interview-Rounds/26-GraphQL-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md) | unsafe schema and resolver practices |

Special-round target:

- You can answer GraphQL interviews and avoid common production mistakes.

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 27 | [06-Practice-Upgrade/27-GraphQL-Active-Recall-Question-Bank.md](06-Practice-Upgrade/27-GraphQL-Active-Recall-Question-Bank.md) | recall prompts across beginner to pro topics |
| 28 | [06-Practice-Upgrade/28-GraphQL-Hands-On-Exercises-And-Query-Drills.md](06-Practice-Upgrade/28-GraphQL-Hands-On-Exercises-And-Query-Drills.md) | query/schema/resolver drills |
| 29 | [06-Practice-Upgrade/29-GraphQL-Mini-Projects-Portfolio.md](06-Practice-Upgrade/29-GraphQL-Mini-Projects-Portfolio.md) | portfolio-ready GraphQL projects |
| 30 | [06-Practice-Upgrade/30-GraphQL-Pro-Gap-Fill-Production-Readiness-Checklist.md](06-Practice-Upgrade/30-GraphQL-Pro-Gap-Fill-Production-Readiness-Checklist.md) | senior readiness checklist and scoring rubric |

Practice target:

- You can design, implement, debug, secure, observe, and evolve production GraphQL APIs.

---

## 9. GraphQL Mastery Lab

Use the lab when you want practice instead of reading-only notes:

- [graphql-mastery-lab/README.md](graphql-mastery-lab/README.md)
- [graphql-mastery-lab/LEARNING_PATH.md](graphql-mastery-lab/LEARNING_PATH.md)

Lab target:

- You can run schema and query examples.
- You can practice resolver execution and N+1 debugging.
- You can debug auth, schema evolution, federation-style ownership, and production-style failures.

---

## 10. Interview Answer Pattern

For GraphQL debugging and interview answers, use this shape:

```text
1. Symptom:
   What exactly is failing: schema, validation, resolver, data source, auth, performance, cache, federation, or client behavior?

2. Contract:
   Which schema field/type/operation is involved?

3. Execution:
   Which resolver path executes, and what data sources does it call?

4. Evidence:
   Which trace, metric, log, query plan, or operation proves the state?

5. Cause:
   What changed or mismatched?

6. Mitigation:
   What safe action restores service?

7. Prevention:
   What schema rule, resolver pattern, auth policy, complexity limit, cache, test, or runbook prevents recurrence?
```

---

## 11. Recommended Study Orders

### 2-Week Practical Path

1. Foundation files 1-4.
2. Practical files 5-10.
3. Scenario files 17-23.
4. Cheat sheet, exercises, and interview Q&A.

### 4-Week Pro Path

1. Week 1: GraphQL mental model, SDL, operations, resolvers.
2. Week 2: validation/errors, pagination, DataLoader, auth, clients.
3. Week 3: schema governance, performance, security, observability, federation, caching.
4. Week 4: production scenarios, runbooks, mini projects, interview practice.

### Production Operator Path

1. Learn schema/query/resolver debugging workflow.
2. Practice N+1, auth, schema evolution, and error incidents.
3. Add complexity, persisted query, and observability controls.
4. Write RCA notes from each scenario.

---

## 12. Readiness Gate

You are GraphQL interview-ready when you can do all of this without notes:

- Explain schema, operation, resolver, context, data source, validation, execution, and response shape.
- Design clean SDL with nullability, lists, inputs, enums, interfaces/unions, and deprecations.
- Write queries, mutations, subscriptions, variables, fragments, and directives.
- Implement resolver patterns and prevent N+1 with batching/caching.
- Explain partial errors, null bubbling, and client-safe error contracts.
- Design pagination, filtering, sorting, and connection models.
- Enforce authentication and field/object-level authorization.
- Explain GraphQL security: introspection policy, complexity/depth limits, persisted queries, rate limits, and input validation.
- Explain observability with resolver traces, operation metrics, error budgets, and slow-field evidence.
- Explain GraphQL-over-HTTP status behavior, custom scalars/directives, mutation idempotency, contract testing/codegen, and incremental delivery tradeoffs.
- Explain federation, schema governance, caching tradeoffs, and production incident response.