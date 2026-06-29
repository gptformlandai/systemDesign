---
name: Performance Review
description: Find performance bottlenecks and suggest concrete optimizations
---

Perform a performance review of:

${selection}

Context: ${input:Describe the performance problem if known (e.g., slow API response, high memory usage, N+1 queries)}

Evaluate for:

1. **Algorithmic complexity**
   - What is the time complexity of the main operation?
   - Is there a simpler algorithm for this use case?

2. **Database / I/O patterns**
   - N+1 queries (ORM lazy loading producing one query per item)
   - Missing indexes on filtered/sorted columns
   - Reading more data than needed (SELECT * vs SELECT specific columns)
   - Synchronous I/O blocking async context

3. **Memory allocation**
   - Creating large intermediate collections unnecessarily
   - Strings or objects created in hot loops
   - Missing lazy evaluation where a generator would suffice

4. **Caching opportunities**
   - Pure function results that could be memoized
   - Database queries repeated on every request with stable data

5. **Concurrency**
   - Sequential operations that could run in parallel
   - CPU-bound work blocking I/O-bound event loop

For each finding:
| Issue | Location | Impact | Fix | Effort |
|-------|----------|--------|-----|--------|
| N+1 query | line 45 | HIGH | Use selectinload | LOW |

Output: prioritized table + code snippet for the top 2 fixes.
Do NOT include premature micro-optimizations — focus on meaningful bottlenecks.
