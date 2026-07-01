# Project 01: Social Recommendation Engine

Goal: build a Neo4j-backed recommendation graph.

---

## Requirements

- store users, follows, blocks, likes, purchases
- recommend friends or products
- enforce block/privacy filters
- keep online traversal bounded

---

## Graph Model

```text
(:User)-[:FOLLOWS]->(:User)
(:User)-[:BLOCKED]->(:User)
(:User)-[:BOUGHT]->(:Product)
(:User)-[:LIKED]->(:Content)
```

---

## Interview Talking Points

- bounded two-hop traversals
- celebrity node risk
- precomputed scores for high scale
- privacy filters before ranking