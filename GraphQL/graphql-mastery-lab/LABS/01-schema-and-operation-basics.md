# Lab 01: Schema And Operation Basics

## Goal

Understand how SDL and operation documents define the GraphQL contract.

## Steps

```bash
../SCRIPTS/01-schema-inventory.sh
../SCRIPTS/02-operation-inventory.sh
```

## Observe

- object types
- input types
- deprecated fields
- named operations
- fragments and variables

## Questions

1. Which fields are nullable?
2. Which list fields are paginated?
3. Which operations are named?
4. Which operation could become expensive?

## Interview Takeaway

```text
I start GraphQL analysis by reading schema shape, operation documents, variables, nullability, and list boundaries.
```