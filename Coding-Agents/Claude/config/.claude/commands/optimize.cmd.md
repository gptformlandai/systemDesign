---
description: Performance and token efficiency analysis with prioritized improvements
---

Optimize the following:

$ARGUMENTS

Evaluate for:

1. **Algorithmic complexity**: current Big-O, better option if available
2. **Database/I/O patterns**: N+1 queries, missing indexes, reading more than needed
3. **Memory allocation**: unnecessary intermediate collections, objects in hot loops
4. **Concurrency**: sequential awaits that could run in parallel
5. **Caching**: pure function results that repeat unnecessarily

For each finding:
| Issue | Location | Impact | Fix | Effort |
|-------|----------|--------|-----|--------|

Show code fix for the top 2 findings only.
Do NOT suggest micro-optimizations without evidence of actual impact.
Do NOT suggest premature caching without profiling data.

After showing fixes: run any available benchmarks or tests to confirm improvement.
