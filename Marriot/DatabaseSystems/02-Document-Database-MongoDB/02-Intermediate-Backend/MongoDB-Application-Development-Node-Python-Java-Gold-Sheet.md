    # MongoDB Application Development with Node.js, Python and Java - Gold Sheet

    > **Track File #10 of 28 - Group 02: Intermediate Backend**
    > For: backend/database/system design interviews | Level: intermediate backend implementation | Mode: drivers, repositories, ODMs, app integration

    This sheet builds:
    - Node native driver and Mongoose
- PyMongo, Motor, FastAPI
- Spring Data MongoDB, repositories, MongoTemplate

Original master-map sections included here:
- 20. MongoDB With Application Development

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 20. MongoDB With Application Development

### Node.js Native Driver

```javascript
import { MongoClient } from "mongodb";

let client;

export async function getDb() {
  if (!client) {
    client = new MongoClient(process.env.MONGODB_URI, { maxPoolSize: 50 });
    await client.connect();
  }
  return client.db("appdb");
}
```

Repository pattern:

```javascript
export class UserRepository {
  constructor(db) {
    this.users = db.collection("users");
  }

  async findByEmail(tenantId, email) {
    return this.users.findOne({ tenantId, email: email.toLowerCase() });
  }

  async createUser(user) {
    const now = new Date();
    await this.users.insertOne({ ...user, email: user.email.toLowerCase(), createdAt: now, updatedAt: now });
  }
}
```

Error handling:

```javascript
try {
  await users.insertOne(user);
} catch (error) {
  if (error.code === 11000) {
    throw new Error("Duplicate user");
  }
  throw error;
}
```

### Mongoose

Mongoose adds schema definitions, validation, middleware, and model abstractions.

```javascript
import mongoose from "mongoose";

const userSchema = new mongoose.Schema({
  tenantId: { type: String, required: true, index: true },
  email: { type: String, required: true, lowercase: true },
  name: { type: String, required: true },
  createdAt: { type: Date, default: Date.now }
});

userSchema.index({ tenantId: 1, email: 1 }, { unique: true });

export const User = mongoose.model("User", userSchema);
```

Use Mongoose when you want ODM structure. Use native driver when you want thinner control.

### Python PyMongo

```python
from pymongo import MongoClient, ASCENDING

client = MongoClient(os.environ["MONGODB_URI"], maxPoolSize=50)
db = client.appdb
users = db.users

users.create_index([("tenantId", ASCENDING), ("email", ASCENDING)], unique=True)
```

### Motor Async Driver With FastAPI

```python
from motor.motor_asyncio import AsyncIOMotorClient
from fastapi import FastAPI

app = FastAPI()
client = AsyncIOMotorClient(os.environ["MONGODB_URI"])
db = client.appdb

@app.get("/users/{user_id}")
async def get_user(user_id: str):
    user = await db.users.find_one({"_id": user_id}, {"passwordHash": 0})
    return user
```

### Java / Spring Data MongoDB

Document:

```java
@Document("orders")
@CompoundIndex(name = "tenant_status_created", def = "{ 'tenantId': 1, 'status': 1, 'createdAt': -1 }")
public class OrderDocument {
    @Id
    private String id;
    private String tenantId;
    private String status;
    private Instant createdAt;
    private List<OrderItem> items;
}
```

Repository:

```java
public interface OrderRepository extends MongoRepository<OrderDocument, String> {
    List<OrderDocument> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status, Pageable pageable);
}
```

MongoTemplate for custom queries:

```java
Query query = Query.query(Criteria.where("tenantId").is(tenantId).and("status").is("PAID"))
    .with(Sort.by(Sort.Direction.DESC, "createdAt"))
    .limit(20);

List<OrderDocument> orders = mongoTemplate.find(query, OrderDocument.class);
```

### Testing With Testcontainers

Node/Python/Java can run real MongoDB in tests. Java example:

```java
@Testcontainers
class OrderRepositoryTest {
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
}
```

Testing rules:

- Unit test business logic with repository mocks.
- Integration test queries/indexes with real MongoDB.
- Verify unique indexes and transaction behavior.
- Seed realistic data shapes.

---

---
