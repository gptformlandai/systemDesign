# GraphQL Schema Query Cheat Sheet And Decision Map

> Track File #25 of 30 - Group 05: Special Interview Rounds
> For: fast recall | Level: beginner to pro | Mode: schema/query decision map

## 1. SDL Basics

```graphql
type Query {
  product(id: ID!): Product
  products(first: Int!, after: String): ProductConnection!
}

type Product {
  id: ID!
  name: String!
  priceCents: Int!
}
```

## 2. Operation Basics

```graphql
query ProductPage($id: ID!) {
  product(id: $id) {
    id
    name
  }
}
```

## 3. Resolver Decision Map

| Need | Pattern |
|---|---|
| request identity | context |
| backend access | data source/service layer |
| nested lookup | DataLoader |
| list field | pagination and max size |
| sensitive field | field-level auth |
| slow operation | resolver tracing and data-source counts |
| schema change | additive field and deprecation |
| public operation | persisted query and complexity limit |

## 4. Debug Decision Map

| Symptom | Start With |
|---|---|
| validation error | schema and operation document |
| partial data | error path and nullability |
| high latency | operation hash, resolver trace, data-source calls |
| data leak | resolver path, context, authz, loader/cache scope |
| stale client UI | normalized cache identity and fetch policy |
| breaking client | schema diff and operation usage |
| federation failure | composition result and query plan |