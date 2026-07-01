# Lab 08: Autocomplete And Geospatial Search

Goal: practice two user-facing search scenarios with strict latency and modeling guardrails.

---

## Run

```bash
bash SCRIPTS/reset-lab.sh
bash SCRIPTS/run-request.sh SCRIPTS/03-search-queries.sh
bash SCRIPTS/run-request.sh SCRIPTS/09-geo-place-search.sh
```

---

## What To Observe

- Product autocomplete uses a dedicated `title.autocomplete` field.
- Place search uses `geo_point`, `geo_distance`, and `_geo_distance` sort.
- Both scenarios include filters before ranking.

---

## Explain Out Loud

```text
Why are wildcard autocomplete and unlimited radius geo search risky?
```

Strong answer:

```text
Wildcard autocomplete can be expensive for every keystroke and produces noisy results. Unlimited geo radius expands work and can produce irrelevant or privacy-sensitive results. Both should use dedicated mappings, bounded queries, size limits, and latency SLOs.
```

---

## Completion Gate

- You can compare edge n-grams, search-as-you-type, and completion-style suggestions.
- You can explain `geo_point` and distance filtering.
- You can design radius, result-size, and privacy guardrails.