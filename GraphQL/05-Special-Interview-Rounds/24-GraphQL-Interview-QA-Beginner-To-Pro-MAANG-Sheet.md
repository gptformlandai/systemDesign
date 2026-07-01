# GraphQL Interview Q&A: Beginner To Pro - MAANG Sheet

> Track File #24 of 30 - Group 05: Special Interview Rounds
> For: GraphQL interviews | Level: beginner to senior | Mode: direct Q&A

## 1. What is GraphQL?

GraphQL is a typed API query language and execution model where clients request fields from a schema and servers resolve those fields through resolvers.

## 2. Schema vs query?

The schema defines what can be asked. The query defines what a client asks for in one operation.

## 3. What is a resolver?

A resolver is a function that returns the value for a schema field, often by calling a database, service, cache, or domain layer.

## 4. What is context?

Context is request-scoped state passed to resolvers, commonly including user identity, tenant, loaders, trace IDs, and service clients.

## 5. Query vs mutation vs subscription?

Queries read data, mutations change data, and subscriptions stream event-driven updates.

## 6. What is N+1?

N+1 occurs when nested resolvers make one backend call per parent item. DataLoader-style batching prevents this.

## 7. What is null bubbling?

If a non-null field returns null or throws, GraphQL nulls the nearest nullable parent and returns an error path.

## 8. How do you paginate GraphQL lists?

Use bounded list fields, stable sorting, max page sizes, and cursor-based connections for changing data.

## 9. How do you secure GraphQL?

Use authentication, field/object-level authorization, tenant-scoped data access, input validation, complexity/depth limits, persisted queries, rate limits, and safe error handling.

## 10. What is federation?

Federation composes multiple team-owned subgraphs into a supergraph served by a router/gateway.

## 11. How is GraphQL caching different from REST?

GraphQL often has one endpoint and variable response shapes, so caching needs operation identity, variables, auth scope, entity IDs, and freshness rules.

## 12. What makes a GraphQL answer senior-level?

It connects schema design, resolver execution, data-source fanout, auth, complexity controls, observability, caching, schema evolution, and client impact.