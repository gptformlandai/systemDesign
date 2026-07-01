# 18. Scenario: `DEADLINE_EXCEEDED` Latency Debugging

## Incident

Clients report intermittent `DEADLINE_EXCEEDED` from `inventory.v1.InventoryService/GetAvailability` after a deployment.

---

## First Questions

1. What deadline did the client set?
2. Did the server handler start?
3. Is latency inside server logic, dependency calls, proxy, or connection setup?
4. Did the deployment change payload size, dependency behavior, route timeout, or retry policy?
5. Are all clients affected or one caller/region/version?

---

## Evidence Path

| Evidence | What It Tells You |
|---|---|
| client trace span | total call time, attempts, status |
| server trace span | whether handler executed and where time went |
| method latency metric | p50 vs p99 shift and status split |
| dependency spans | downstream database/cache/API delay |
| proxy access logs | route timeout or upstream reset |
| deployment diff | code/config change causing latency |
| request payload size | serialization or backend fan-out increase |

---

## Possible Causes

- client deadline reduced accidentally
- Envoy route timeout shorter than app deadline
- handler added sequential fan-out
- database query regressed
- retries amplified load
- one backend zone is slow
- response message grew too large
- server ignores cancellation and saturates workers

---

## Mitigation

Choose based on evidence:

- roll back bad deployment
- reduce fan-out or add batching/cache
- align route timeout with client deadline
- disable unsafe retry amplification
- drain slow zone/backend
- lower payload size or paginate
- add cancellation checks in handler loops

---

## Prevention

- method-level latency SLOs
- deadline budget review in code review
- trace coverage for client and server spans
- load test p95/p99 before rollout
- route timeout config tests
- alert on deadline-exceeded rate by method/client

---

## Interview Sound Bite

I treat `DEADLINE_EXCEEDED` as a time-budget failure, not automatically a server bug. I compare client and server spans, check route timeouts and retries, isolate whether the handler ran, inspect dependency latency, and mitigate the component consuming the budget.