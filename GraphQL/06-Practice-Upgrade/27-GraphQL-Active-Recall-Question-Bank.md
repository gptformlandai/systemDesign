# GraphQL Active Recall Question Bank

> Track File #27 of 30 - Group 06: Practice Upgrade
> For: spaced repetition | Level: beginner to pro | Mode: question bank

## 1. Beginner Recall

1. What is GraphQL?
2. What is the difference between schema and operation?
3. What is SDL?
4. What are query, mutation, and subscription?
5. What is a resolver?
6. What is context?
7. What does nullability mean?
8. Why use variables instead of string interpolation?

## 2. Intermediate Recall

1. How does resolver execution walk a nested field tree?
2. What is null bubbling?
3. How do GraphQL errors differ from HTTP errors?
4. What is the N+1 problem?
5. How does DataLoader batching work?
6. How do you design cursor pagination?
7. Where should field-level authorization happen?
8. What does a normalized client cache need?

## 3. Senior Recall

1. How do you evolve a GraphQL schema safely?
2. What changes are breaking?
3. How do persisted queries help production GraphQL?
4. How do depth and complexity limits differ?
5. How do you observe slow GraphQL operations?
6. What can go wrong in federation?
7. How do you cache GraphQL safely?
8. How do you debug a GraphQL data leak?

## 4. Scenario Recall

1. A query is slow after adding a nested field. What evidence do you inspect?
2. A client breaks after a schema deploy. What is your rollback/evolution plan?
3. A user sees another tenant's data. What resolver/cache paths do you inspect?
4. Federation composition fails. What artifacts do you inspect?
5. GraphQL returns HTTP 200 with errors. What does that mean?
6. A mobile app shows stale data after mutation. What client cache behavior do you check?

## 5. Answer Pattern

```text
schema field -> operation -> resolver path -> data-source calls -> auth/cache scope -> evidence -> mitigation -> prevention
```