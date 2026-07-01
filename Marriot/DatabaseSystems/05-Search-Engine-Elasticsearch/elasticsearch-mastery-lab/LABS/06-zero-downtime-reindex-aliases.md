# Lab 06: Zero-Downtime Reindex And Aliases

Goal: practice the safest Elasticsearch mapping migration pattern.

---

## Run

```bash
bash SCRIPTS/reset-lab.sh
bash SCRIPTS/run-request.sh SCRIPTS/07-alias-reindex-migration.sh
```

---

## What To Observe

- `products-v2` is created with a new `search_version` field.
- `_reindex` copies documents from `products-v1` to `products-v2`.
- `products-read` and `products-write` aliases switch to `products-v2`.
- Applications keep using aliases instead of physical index names.

---

## Explain Out Loud

```text
Why is alias switching safer than changing application code to point directly at a new index?
```

Strong answer:

```text
Aliases decouple the application from physical index versions. We can build, backfill, validate, switch, and roll back with less application churn.
```

---

## Completion Gate

- You can explain versioned indices.
- You can explain `_reindex` and alias switching.
- You can name rollback and validation checks before deleting the old index.