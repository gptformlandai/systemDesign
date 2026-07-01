# Lab 03: N+1 And DataLoader Debugging

## Goal

See the difference between naive nested resolver fanout and batched loading.

## Run

```bash
../SCRIPTS/04-run-resolver-simulation.sh
```

## Observe

- naive resolver makes one seller call per product
- batched resolver groups seller IDs into one call
- duplicate seller IDs should not create duplicate backend calls

## Drill

Explain this flow:

```text
products -> product.seller for each product -> seller loader batches IDs -> seller results map back to products
```

## Interview Takeaway

```text
N+1 is proven with resolver path and data-source call counts. DataLoader-style batching should be request-scoped and auth-aware.
```