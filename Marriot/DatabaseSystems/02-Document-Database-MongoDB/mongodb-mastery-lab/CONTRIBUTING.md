# Contributing

This repository is a learning lab. Keep contributions practical, runnable, and beginner-friendly without losing production depth.

## Contribution Guidelines

- Prefer small focused examples over giant files.
- Include commands that can run against the local Docker MongoDB setup.
- Add indexes for any new hot query examples.
- Include `explain()` guidance for performance-sensitive examples.
- Keep secrets out of committed files.
- Use realistic schemas and tenant-aware examples.

## Content Template

For new learning docs, use this shape:

1. Mental model.
2. Why it exists.
3. Example schema/query/code.
4. Tradeoffs.
5. Production notes.
6. Interview answer.
7. Hands-on exercise.

## Validation

Before submitting changes, run:

```bash
docker compose config
find . -name '*.md' -print
```

Then run at least one mongosh lab:

```bash
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/01-crud.js
```
