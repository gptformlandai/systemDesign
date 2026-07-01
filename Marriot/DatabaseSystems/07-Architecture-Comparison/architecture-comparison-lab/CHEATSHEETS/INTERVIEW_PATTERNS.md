# Interview Patterns Cheatsheet

## Universal Answer

```text
I would choose based on access pattern and correctness. <System> is strong for <fit>, but weak for <risk>. I would use <source> as source of truth and <derived stores> for specialized reads, synchronized with <CDC/events>. I would monitor <SLO/freshness> and keep <backup/rebuild> paths.
```

## Rejection Pattern

```text
I would not use <system> as the source of truth here because <correctness/query/ops reason>. It can still be useful as a derived store for <specialized access pattern>.
```