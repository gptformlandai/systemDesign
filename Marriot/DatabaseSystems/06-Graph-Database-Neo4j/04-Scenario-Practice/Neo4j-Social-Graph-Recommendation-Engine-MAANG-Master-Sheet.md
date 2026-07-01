# Neo4j Social Graph and Recommendation Engine - MAANG Master Sheet

> Track File #17 of 30 - Group 04: Scenario Practice
> For: backend/system design interviews | Level: senior / MAANG | Mode: social graph, mutuals, recommendations

This sheet builds:
- Social graph modeling
- Friends-of-friends and recommendation traversals
- Scale and privacy tradeoffs

---

## 1. Requirements

- find friends
- find mutual friends
- suggest people to follow
- recommend products/content based on graph proximity
- enforce blocks/privacy
- support p99 latency targets

---

## 2. Model

```text
(:User)-[:FOLLOWS]->(:User)
(:User)-[:BLOCKED]->(:User)
(:User)-[:LIKED]->(:Content)
(:User)-[:BOUGHT]->(:Product)
```

Relationship properties can include `since`, `weight`, or `source`.

---

## 3. Friend Recommendation Query

```cypher
MATCH (me:User {userId: $userId})-[:FOLLOWS]->(:User)-[:FOLLOWS]->(candidate:User)
WHERE candidate <> me
  AND NOT (me)-[:FOLLOWS]->(candidate)
  AND NOT (me)-[:BLOCKED]-(candidate)
RETURN candidate.userId, count(*) AS mutuals
ORDER BY mutuals DESC
LIMIT 20;
```

---

## 4. Scale Concerns

- celebrity/supernode users
- privacy filters
- block relationships
- two-hop fan-out
- real-time vs precomputed recommendations
- regional/tenant partitioning

---

## 5. Strong Answer

```text
For social recommendations, I would model users and meaningful relationships like FOLLOWS, BLOCKED, LIKED, and BOUGHT. Online queries should start from an indexed user, bound traversal depth, apply privacy/block filters, and limit fan-out. For high-degree users or expensive recommendations, I would precompute scores or isolate algorithm workloads instead of doing arbitrary deep traversals on every request.
```

---

## 6. Revision Notes

- One-line summary: Social graph design is bounded traversal plus privacy and supernode control.
- Three keywords: mutuals, fan-out, privacy.
- One interview trap: ignoring celebrity nodes.
- Memory trick: recommendations are graph proximity with guardrails.