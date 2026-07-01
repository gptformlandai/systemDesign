# Lab 08: Permission Graph And Access Control

Goal: practice inherited permissions, deny precedence, tenant filters, and explainable access paths.

---

## Run

```bash
bash SCRIPTS/reset-lab.sh
bash SCRIPTS/run-cypher.sh SCRIPTS/08-permission-graph.cypher
```

---

## What To Observe

- access starts from indexed user and resource anchors
- group inheritance is bounded to three hops
- deny is modeled explicitly
- allowed access returns an explanation path

---

## Explain Out Loud

```text
Why should tenant filters and deny rules be evaluated inside the graph query instead of after the query?
```

Strong answer:

```text
Post-processing can leak paths, counts, metadata, or intermediate resources. Permission correctness requires tenant and deny logic inside the candidate traversal before an allow decision is returned.
```

---

## Completion Gate

- You can explain inherited group access.
- You can explain deny precedence.
- You can name cache invalidation and stale-permission risks.