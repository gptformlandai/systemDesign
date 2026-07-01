# Simple Store GraphQL Example

This example is dependency-free. It provides a sample schema, operation documents, and a Node.js resolver simulation for N+1 behavior.

## Files

| File | Purpose |
|---|---|
| [schema.graphql](schema.graphql) | product/cart/order schema excerpt |
| [operations.graphql](operations.graphql) | query and mutation examples |
| [resolver-simulation.js](resolver-simulation.js) | naive vs batched resolver behavior |

## Run Inventory Scripts

From the workspace root:

```bash
GraphQL/graphql-mastery-lab/SCRIPTS/01-schema-inventory.sh
GraphQL/graphql-mastery-lab/SCRIPTS/02-operation-inventory.sh
GraphQL/graphql-mastery-lab/SCRIPTS/03-rough-operation-cost-check.sh
```

## Run Resolver Simulation

```bash
GraphQL/graphql-mastery-lab/SCRIPTS/04-run-resolver-simulation.sh
```

## What This Demonstrates

- schema fields and nullability
- named operations and variables
- cursor pagination shape
- mutation payloads
- naive resolver fanout
- batching with request-scoped loading