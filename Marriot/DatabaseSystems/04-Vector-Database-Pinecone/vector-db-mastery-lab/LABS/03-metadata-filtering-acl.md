# Lab 03: Metadata Filtering And ACL

Goal: practice tenant and ACL filters during vector retrieval.

---

## Run

```bash
bash SCRIPTS/04-metadata-filter-search.sh
```

---

## What To Observe

- tenant `t1` results are allowed
- tenant `t2` private catalog result is excluded
- ACL group controls which records are candidates

---

## Explain Out Loud

```text
Why should ACL filtering happen before reranking?
```

Strong answer:

```text
Unauthorized candidates can leak through logs, reranker calls, traces, metrics, or prompt context. Authorization belongs in the retrieval filter before candidates leave the search layer.
```

---

## Completion Gate

- You can explain tenant filters.
- You can explain ACL filters.
- You can name post-retrieval filtering risks.