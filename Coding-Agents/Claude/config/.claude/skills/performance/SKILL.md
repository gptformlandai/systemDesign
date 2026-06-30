# Performance Analysis Skill

## When to Invoke
Apply this skill automatically when the task involves:
- Analyzing slow code, slow endpoints, or high memory usage
- Finding database query bottlenecks or N+1 problems
- Reviewing async concurrency patterns for blocking operations
- Profiling results analysis
- Memory leak investigation
- Keywords: "slow", "performance", "optimize", "bottleneck", "N+1", "memory leak", "latency"

## Core Principle: Measure First, Change Second

The #1 performance anti-pattern is optimizing code that isn't a bottleneck.
Before any optimization recommendation, I require evidence:
  - Profiler output (cProfile, py-spy, flame graph)
  - Timing data (response times, percentile metrics)
  - Observable symptom (8s response, OOM kill, 100% CPU)
  - OR: obvious algorithmic complexity issue in the code

If no evidence is provided: ask for it before proceeding.
Exception: clear N+1 query patterns or obvious O(n²) where O(n) is possible.

## Analysis Framework

### 1 — Algorithmic Complexity
Primary question: Is the algorithm fundamentally wrong for the data size?

Patterns to identify:
  - Nested loops over collections: O(n²) or worse
  - Linear search in a hot path that should use a hash map: O(n) vs O(1)
  - Sorting inside a loop: O(n² log n) that should be sorted once: O(n log n)
  - Recursive algorithms that should be iterative with memoization

Evidence required: collection size, call frequency, observed latency

### 2 — Database and I/O Patterns
Most common production performance problem. Check every DB access pattern.

N+1 Query Pattern (most common ORM issue):
  ```python
  # N+1: 1 query for orders + N queries for each order's items
  orders = await session.execute(select(Order))
  for order in orders.scalars():
      print(order.items)  # lazy load = 1 query per order
  
  # Fix: selectinload eliminates the N queries
  orders = await session.execute(
      select(Order).options(selectinload(Order.items))
  )
  ```

Missing Index Check:
  - WHERE clause columns without an index
  - ORDER BY columns without an index
  - JOIN columns without an index
  Identify from query patterns in the code, suggest index names

Over-fetching:
  - SELECT * when only 2 columns are needed
  - Loading entire model for a count or existence check
  - Loading related objects that aren't used

### 3 — Memory Allocation
  - Large intermediate collections: `[x for x in large_list]` → use generators for streaming
  - String concatenation in loops: O(n²) memory → use join() or string builder
  - Unbounded caches: dict/list that grows without eviction policy
  - Object creation in tight loops: allocate outside the loop

### 4 — Async and Concurrency
  - Sequential awaits for INDEPENDENT operations (biggest async performance mistake):
    ```python
    # Wrong: sequential (total = A_time + B_time)
    result_a = await fetch_a()
    result_b = await fetch_b()
    
    # Correct: concurrent (total = max(A_time, B_time))
    result_a, result_b = await asyncio.gather(fetch_a(), fetch_b())
    ```
  - Sync blocking calls inside async functions (time.sleep, requests.get, open() in tight loop)
  - Missing connection pool limits causing resource exhaustion

### 5 — Caching Candidates
Only recommend caching when: data is expensive to compute AND stable enough to cache safely.
  - Pure functions (same input → same output) called in hot paths
  - Database queries for reference data (config, categories, permissions)
  - External API calls for data that changes infrequently

## Output Format

```
Evidence received: [what was provided]
Performance target: [what acceptable looks like]

Priority findings:
| Rank | Issue | Location | Impact | Fix | Effort | Expected Gain |
|------|-------|----------|--------|-----|--------|---------------|
| 1 | N+1 query | order_service.py:67 | HIGH | selectinload(Order.items) | 5 min | ~100x fewer queries |
| 2 | Sequential awaits | payment_api.py:34 | MEDIUM | asyncio.gather() | 10 min | ~50% latency reduction |

Fix for Rank 1: [complete code change as unified diff]
Fix for Rank 2: [complete code change as unified diff]

Verification plan:
- Before: run [benchmark command] → record P50/P95 latency
- Apply fix 1: re-run → compare
- Apply fix 2: re-run → compare
```

## What I NEVER Do
- Suggest caching without evidence the operation is slow
- Recommend micro-optimizations (single function call overhead) unless in a 10M+ iteration loop
- Suggest changing database engines or message queues as a first response
- Recommend adding async where code is already fast enough
- Claim performance improvement without a verification plan
