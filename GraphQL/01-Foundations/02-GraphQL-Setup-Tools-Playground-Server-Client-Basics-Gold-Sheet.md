# GraphQL Setup, Tools, Playground, Server, Client Basics - Gold Sheet

> Track File #2 of 30 - Group 01: Foundations
> For: local setup and first workflow | Level: beginner | Mode: tooling

## 1. Core Idea

A GraphQL workflow needs a schema, a server/executor, a client/query tool, and visibility into validation/execution errors.

```text
schema -> server -> explorer/client -> operation -> response/errors
```

---

## 2. Common Tools

| Tool Type | Examples | Purpose |
|---|---|---|
| server libraries | Apollo Server, Yoga, Mercurius, GraphQL Java, Sangria | execute schemas |
| explorers | GraphiQL, Apollo Sandbox, Altair, Insomnia, Postman | inspect schema and run operations |
| clients | Apollo Client, Relay, urql, graphql-request, fetch | app integration |
| schema tools | GraphQL Code Generator, Rover, GraphQL Inspector | type generation and schema checks |

---

## 3. First Local Checks

```bash
node --version
npm --version
```

Useful GraphQL checks when a server exists:

```bash
curl -s http://localhost:4000/graphql
curl -s -X POST http://localhost:4000/graphql \
  -H 'content-type: application/json' \
  --data '{"query":"query { __typename }"}'
```

---

## 4. Introspection

Introspection lets tools discover schema shape. It is excellent for development and schema tooling.

Production policy should be intentional:

- enabled for trusted/internal tools
- restricted or disabled for public anonymous traffic when risk requires it
- replaced by schema registry or persisted operations for locked-down clients

---

## 5. Interview Summary

```text
For GraphQL setup, I need a schema executor, a query tool, a client integration path, and visibility into validation/execution errors. I treat introspection as a deliberate production policy, not an accident.
```

---

## 6. Revision Notes

- One-line summary: GraphQL setup is schema, executor, explorer, client, and diagnostics.
- Three keywords: schema, explorer, introspection.
- One trap: exposing introspection publicly without considering threat model and tooling alternatives.