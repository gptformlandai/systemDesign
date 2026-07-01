# Lab 05: Evaluation

Goal: practice a tiny golden-set retrieval check.

---

## Run

```bash
bash SCRIPTS/06-evaluation.sh
```

---

## What To Observe

- golden query is `password reset help`
- expected relevant doc is `password-policy`
- if the expected doc appears in top 2, recall@2 for this tiny set is 1.0

---

## Extend

Add two more golden queries:

- comfortable shoes for standing all day -> `shoe-arch-support`
- login incident MFA errors -> `login-runbook`

---

## Completion Gate

- You can define recall@K.
- You can explain why golden sets matter.
- You can name at least three RAG-specific metrics.