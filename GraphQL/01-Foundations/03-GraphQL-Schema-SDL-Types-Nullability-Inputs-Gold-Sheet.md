# GraphQL Schema SDL, Types, Nullability, Inputs - Gold Sheet

> Track File #3 of 30 - Group 01: Foundations
> For: schema fluency | Level: beginner | Mode: type system

## 1. Core Idea

The schema is the GraphQL API contract. SDL describes the graph of types, fields, arguments, inputs, and relationships.

```graphql
type Product {
  id: ID!
  name: String!
  priceCents: Int!
  category: Category
}

enum Category {
  BOOK
  ELECTRONICS
}

input ProductFilterInput {
  category: Category
  search: String
}

type Query {
  products(filter: ProductFilterInput): [Product!]!
}
```

---

## 2. Type Building Blocks

| SDL Feature | Meaning |
|---|---|
| scalar | primitive value such as `String`, `Int`, `Boolean`, `ID` |
| object | selectable fields returned by operations |
| input | argument object for operations |
| enum | controlled set of values |
| interface | common fields implemented by object types |
| union | one of several object types |
| list | ordered collection |
| non-null `!` | value must not be null at that position |

---

## 3. Nullability Matters

`String` means nullable string.

`String!` means non-null string.

`[Product!]!` means the list is non-null and every item is non-null.

Nullability is a product contract. Do not mark fields non-null unless the system can truly guarantee them.

---

## 4. Interview Summary

```text
GraphQL SDL is the API contract. I design types, inputs, enums, interfaces/unions, lists, and nullability carefully because they define client expectations and how errors propagate.
```

---

## 5. Revision Notes

- One-line summary: SDL is the typed shape of what clients can ask for.
- Three keywords: type, input, nullability.
- One trap: overusing non-null fields and causing large null-bubbling failures.