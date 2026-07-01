# Lab 03: Facets And Aggregations

Goal: build product facets and log analytics aggregations.

---

## Run

```bash
bash SCRIPTS/run-request.sh SCRIPTS/04-aggregations.sh
```

---

## What To Observe

- terms aggregations by `brand` and `category`
- range aggregation by `price`
- date histogram over log timestamps
- terms aggregation by log level

---

## Explain Out Loud

```text
Why should facets use keyword/numeric fields instead of analyzed text fields?
```

---

## Completion Gate

- You can explain facets as aggregations over matching documents.
- You can name high-cardinality aggregation risk.
- You can explain why dashboards need guardrails.