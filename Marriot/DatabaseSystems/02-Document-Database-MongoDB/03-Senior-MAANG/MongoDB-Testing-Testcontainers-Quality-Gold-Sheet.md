    # MongoDB Testing, Testcontainers and Quality Gates - Gold Sheet

    > **Track File #19 of 28 - Group 03: Senior MAANG**
    > For: backend/database/system design interviews | Level: senior backend quality | Mode: unit, integration, performance, schema and index tests

    This sheet builds:
    - Test pyramid for MongoDB apps
- Real MongoDB integration tests
- Index, schema, transaction, and performance verification

Original master-map sections included here:
- 27. Testing MongoDB Applications

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 27. Testing MongoDB Applications

### Test Pyramid

| Test Type | Purpose |
|---|---|
| Unit tests | Business logic with mocked repository |
| Integration tests | Real queries, indexes, transactions |
| Contract tests | Service API and event contracts |
| Performance tests | Query latency at realistic volume |
| Migration tests | Old and new schema compatibility |

### Unit Test With Mocked Repository

Use for domain logic:

```javascript
const repo = { findByEmail: jest.fn(), createUser: jest.fn() };
```

Do not mock MongoDB when testing query correctness.

### Integration Tests With Real MongoDB

Use Docker/Testcontainers.

Node idea:

```javascript
beforeAll(async () => {
  client = new MongoClient(uri);
  await client.connect();
  await db.collection("users").createIndex({ tenantId: 1, email: 1 }, { unique: true });
});
```

Python idea:

```python
def test_unique_email(users):
    users.create_index([("tenantId", 1), ("email", 1)], unique=True)
    users.insert_one({"tenantId": "t1", "email": "a@example.com"})
    with pytest.raises(DuplicateKeyError):
        users.insert_one({"tenantId": "t1", "email": "a@example.com"})
```

Java/Spring idea:

```java
@DataMongoTest
class UserRepositoryTest {
    @Autowired UserRepository repository;

    @Test
    void findsByEmail() {
        repository.save(new UserDocument("t1", "a@example.com"));
        assertThat(repository.findByTenantIdAndEmail("t1", "a@example.com")).isPresent();
    }
}
```

### Seed Data

Seed realistic shapes:

- nested fields
- missing optional fields
- large enough collections to test index behavior
- multiple tenants
- skewed tenant sizes
- old schema versions

### Cleanup Strategies

- drop database between test classes
- use unique test database name
- delete by `testRunId`
- transactions can help in some cases, but MongoDB transaction test setup requires replica set

### Index Verification Tests

Test critical indexes exist:

```javascript
const indexes = await db.collection("orders").indexes();
expect(indexes).toEqual(expect.arrayContaining([
  expect.objectContaining({ key: { tenantId: 1, status: 1, createdAt: -1 } })
]));
```

### Schema Validation Tests

Try invalid documents and expect failure.

### Performance Tests

- Load realistic data volume.
- Run hot queries.
- Assert explain plan avoids `COLLSCAN`.
- Track p95/p99 latency.
- Test index build time separately.

---

---
