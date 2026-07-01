    # MongoDB Hands-On Exercises and Runnable Mini Labs - Gold Sheet

    > **Track File #26 of 28 - Group 06: Practice Upgrade**
    > For: backend/database/system design interviews | Level: hands-on beginner to pro | Mode: commands, labs, local practice, failure simulation

    This sheet builds:
    - Beginner, intermediate, advanced, and pro exercises
- Local labs for CRUD, explain, replica sets, change streams
- Practice tasks that convert reading into skill

Original master-map sections included here:
- 29. Hands-On Exercises

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 29. Hands-On Exercises

### Beginner

1. Create database and collection.

```javascript
use learning_mongo
db.createCollection("users")
```

2. Insert documents.

```javascript
db.users.insertMany([
  { email: "a@example.com", profile: { city: "Dallas" }, roles: ["USER"] },
  { email: "b@example.com", profile: { city: "Austin" }, roles: ["ADMIN"] }
])
```

3. Query nested fields.

```javascript
db.users.find({ "profile.city": "Dallas" })
```

4. Update arrays.

```javascript
db.users.updateOne({ email: "a@example.com" }, { $addToSet: { roles: "EDITOR" } })
```

5. Delete documents.

```javascript
db.users.deleteOne({ email: "b@example.com" })
```

### Intermediate

1. Build indexes for `users`, `orders`, and `products`.
2. Use `explain()` before and after indexes.
3. Build aggregation pipeline for sales by day.
4. Model product reviews with subset pattern.
5. Implement a transaction for inventory reservation plus order update.

### Advanced

1. Build replica set locally with Docker.
2. Simulate primary failover using `rs.stepDown()`.
3. Design shard key for orders, chat, and IoT events.
4. Optimize a slow query with explain output.
5. Build change stream listener that updates Redis cache.

### Pro

1. Design multi-tenant schema with tenant isolation tests.
2. Build RAG document store with metadata filters.
3. Build real-time dashboard with raw events and summaries.
4. Build event-driven pipeline with outbox and idempotent consumers.
5. Create system design docs for a sharded MongoDB order platform.

---

---
