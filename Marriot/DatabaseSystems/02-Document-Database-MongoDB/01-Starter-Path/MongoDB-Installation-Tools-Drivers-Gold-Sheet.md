    # MongoDB Installation, Tools and Drivers - Gold Sheet

    > **Track File #3 of 28 - Group 01: Starter Path**
    > For: backend/database/system design interviews | Level: beginner practical setup | Mode: local development, Atlas, Compass, mongosh, drivers

    This sheet builds:
    - Community Server, Atlas, Compass, mongosh
- Docker/local setup
- Node.js, Python, and Spring Boot connection examples

Original master-map sections included here:
- 3. Installation and Tools

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 3. Installation and Tools

### Main Options

| Tool | Use |
|---|---|
| MongoDB Community Server | Local development or self-managed deployments |
| MongoDB Atlas | Managed cloud MongoDB with backups, monitoring, search, vector search |
| MongoDB Compass | GUI for browsing data, indexes, schema, and queries |
| mongosh | Official shell for commands, scripts, and admin tasks |
| Docker | Repeatable local development environment |
| Drivers | App connectivity for Node.js, Python, Java, Go, etc. |

### Local Install on macOS With Homebrew

```bash
brew tap mongodb/brew
brew install mongodb-community mongosh
brew services start mongodb-community
mongosh
```

Check status:

```bash
brew services list
```

### Docker Setup

```bash
docker run --name mongo-local \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=secret \
  -d mongo:7
```

Connect:

```bash
mongosh "mongodb://admin:secret@localhost:27017/admin"
```

Development `docker-compose.yml`:

```yaml
services:
  mongo:
    image: mongo:7
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: secret
    volumes:
      - mongo_data:/data/db
volumes:
  mongo_data:
```

### MongoDB Atlas

Atlas gives you:

- managed replica sets and sharded clusters
- backups and point-in-time restore depending on tier
- monitoring and alerts
- Atlas Search
- Atlas Vector Search
- network access controls
- database users and roles

Connection string example:

```text
mongodb+srv://app_user:<password>@cluster0.example.mongodb.net/appdb?retryWrites=true&w=majority
```

### MongoDB Compass

Use Compass to:

- inspect documents
- run filters and aggregations
- create indexes
- view schema sampling
- see explain plans visually
- manage validation rules

Connection string example:

```text
mongodb://admin:secret@localhost:27017/admin
```

### Authentication Basics

Authentication answers: who are you?

Authorization answers: what are you allowed to do?

Create an application user:

```javascript
use appdb

db.createUser({
  user: "app_user",
  pwd: passwordPrompt(),
  roles: [
    { role: "readWrite", db: "appdb" }
  ]
})
```

### Node.js Driver Example

Install:

```bash
npm install mongodb
```

Connect:

```javascript
import { MongoClient } from "mongodb";

const client = new MongoClient(process.env.MONGODB_URI, {
  maxPoolSize: 50,
  retryWrites: true
});

await client.connect();
const db = client.db("appdb");
const users = db.collection("users");

await users.insertOne({ email: "asha@example.com", createdAt: new Date() });
```

### Python PyMongo Example

Install:

```bash
pip install pymongo
```

Connect:

```python
from pymongo import MongoClient
from datetime import datetime, timezone

client = MongoClient("mongodb://localhost:27017")
db = client["appdb"]
users = db["users"]

users.insert_one({"email": "asha@example.com", "createdAt": datetime.now(timezone.utc)})
```

### Java / Spring Boot Example

Dependency concept:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

Configuration:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/appdb
```

Repository:

```java
public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmail(String email);
}
```

Document:

```java
@Document("users")
public class UserDocument {
    @Id
    private String id;
    private String email;
    private Instant createdAt;
}
```

---

---
