# Lab 04: Relevance Debugging

Goal: practice explaining search quality.

---

## Run

```bash
bash SCRIPTS/run-request.sh SCRIPTS/03-search-queries.sh
```

---

## Exercise

For these queries, write the expected top result and why:

| Query | Expected Reasoning |
|---|---|
| `mechanical keyboard` | title match should dominate |
| `running headphones` | title and description both matter |
| `running shoes` | apparel product should match |
| `mech key` | autocomplete field should help |

---

## Relevance Review

Ask:

- Which fields should be boosted?
- Which synonyms are needed?
- Is fuzziness safe here?
- Are filters changing score or only eligibility?
- What zero-result queries should be tracked?

---

## Completion Gate

- You can explain BM25 at a high level.
- You can explain boosts and filters.
- You can propose a golden query set.