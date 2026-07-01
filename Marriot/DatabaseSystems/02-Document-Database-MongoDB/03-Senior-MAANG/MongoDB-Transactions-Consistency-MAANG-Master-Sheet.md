    # MongoDB Transactions and Consistency - MAANG Master Sheet

    > **Track File #11 of 28 - Group 03: Senior MAANG**
    > For: backend/database/system design interviews | Level: senior backend | Mode: ACID, concerns, sessions, transaction tradeoffs

    This sheet builds:
    - Single-document atomicity
- Multi-document transactions and sessions
- Read concern, write concern, read preference, retryability

Original master-map sections included here:
- 10. Transactions and Consistency

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 10. Transactions and Consistency

### Single-Document Atomicity

MongoDB guarantees atomic updates within a single document. This is the core reason embedding is powerful.

```javascript
db.carts.updateOne(
  { _id: "cart1" },
  { $inc: { itemCount: 1 }, $push: { items: { sku: "SKU-1", quantity: 1 } } }
)
```

Both changes happen atomically for that document.

### Multi-Document Transactions

MongoDB supports ACID transactions across multiple documents, collections, databases, and shards depending on deployment/version constraints. Use them when invariants truly cross document boundaries.

ACID:

| Property | Meaning |
|---|---|
| Atomicity | All operations commit or none do |
| Consistency | Invariants preserved if application logic is correct |
| Isolation | Concurrent transactions do not see partial changes |
| Durability | Committed writes survive according to write concern |

### Sessions

Transactions run inside sessions.

### Read Concern

Read concern controls what data is visible to reads.

| Read Concern | Meaning |
|---|---|
| `local` | Latest local data, may roll back in failover |
| `majority` | Data acknowledged by majority |
| `snapshot` | Consistent snapshot for transactions |
| `linearizable` | Strongest for primary reads, higher latency |

### Write Concern

Write concern controls acknowledgment durability.

| Write Concern | Meaning |
|---|---|
| `{ w: 1 }` | Primary acknowledged |
| `{ w: "majority" }` | Majority acknowledged |
| `{ w: "majority", j: true }` | Majority plus journal acknowledgment behavior depending config |

Use majority for important writes.

### Read Preference

Read preference controls where reads go.

| Preference | Use |
|---|---|
| `primary` | Strongest default consistency |
| `primaryPreferred` | Primary unless unavailable |
| `secondary` | Scale reads, accept staleness |
| `secondaryPreferred` | Secondary unless unavailable |
| `nearest` | Lowest network latency, consistency tradeoff |

### Retryable Writes

Retryable writes let drivers retry certain idempotent write operations after transient network errors. Design application commands with idempotency anyway.

### Causal Consistency

Causal consistency lets a session read its own writes across replica set members when configured correctly.

### When Transactions Are Needed

- money transfer between accounts
- order creation plus payment state when both must commit together
- inventory reservation plus order status
- multi-document workflow with strict invariants
- unique constraint plus side-effect document creation

### When Transactions Are Not Needed

- all data is in one aggregate document
- eventual consistency is acceptable
- asynchronous projection can catch up
- denormalized summary can be repaired
- schema redesign can make the operation single-document atomic

### Node.js Transaction Example

```javascript
const session = client.startSession();

try {
  await session.withTransaction(async () => {
    const accounts = client.db("bank").collection("accounts");

    await accounts.updateOne(
      { _id: "A", balanceCents: { $gte: 5000 } },
      { $inc: { balanceCents: -5000 } },
      { session }
    );

    await accounts.updateOne(
      { _id: "B" },
      { $inc: { balanceCents: 5000 } },
      { session }
    );
  }, {
    readConcern: { level: "snapshot" },
    writeConcern: { w: "majority" }
  });
} finally {
  await session.endSession();
}
```

### Python Transaction Example

```python
with client.start_session() as session:
    with session.start_transaction(read_concern=ReadConcern("snapshot"), write_concern=WriteConcern("majority")):
        accounts.update_one(
            {"_id": "A", "balanceCents": {"$gte": 5000}},
            {"$inc": {"balanceCents": -5000}},
            session=session,
        )
        accounts.update_one(
            {"_id": "B"},
            {"$inc": {"balanceCents": 5000}},
            session=session,
        )
```

### Java / Spring Transaction Example

```java
@Service
public class TransferService {
    private final MongoTemplate mongoTemplate;

    @Transactional
    public void transfer(String fromId, String toId, long cents) {
        Query debitQuery = Query.query(Criteria.where("_id").is(fromId).and("balanceCents").gte(cents));
        Update debit = new Update().inc("balanceCents", -cents);
        mongoTemplate.updateFirst(debitQuery, debit, Account.class);

        Query creditQuery = Query.query(Criteria.where("_id").is(toId));
        Update credit = new Update().inc("balanceCents", cents);
        mongoTemplate.updateFirst(creditQuery, credit, Account.class);
    }
}
```

Requires replica set and transaction manager configuration.

### Transaction Costs

- longer locks and resource usage
- more oplog overhead
- higher latency
- conflicts under high concurrency
- lifetime limits and timeout concerns
- complexity in retry behavior

### Transaction Anti-Patterns

| Anti-Pattern | Better Approach |
|---|---|
| Transactions for every request | Use single-document aggregates |
| Long-running user workflows in one transaction | Use state machine and saga |
| Huge batch updates in transaction | Use idempotent batches |
| Cross-service distributed transactions | Use outbox/saga/eventual consistency |
| Ignoring retry logic | Handle transient transaction errors |

---

---
