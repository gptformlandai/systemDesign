# 07. Transactions and Consistency

## Single-Document Atomicity

MongoDB guarantees atomic updates inside one document. This is why good aggregate design matters.

Example:

```javascript
db.carts.updateOne(
  { _id: 'cart-u1' },
  { $push: { items: { sku: 'SKU-1', quantity: 1 } }, $inc: { itemCount: 1 } }
)
```

Both changes occur atomically for the cart document.

## Multi-Document Transactions

Use transactions when one business invariant crosses documents.

Examples:

- money transfer
- inventory reservation plus order state
- payment record plus order transition
- unique workflow state across collections

## When Transactions Are Not Needed

- data fits one aggregate document
- eventual consistency is acceptable
- read model can catch up asynchronously
- schema can be redesigned to make the operation local

## Read Concern

Read concern controls what a read is allowed to observe.

| Read Concern | Meaning |
|---|---|
| `local` | local node data |
| `majority` | data acknowledged by majority |
| `snapshot` | transaction-consistent snapshot |
| `linearizable` | strongest primary read, higher latency |

## Write Concern

Write concern controls write acknowledgement.

| Write Concern | Meaning |
|---|---|
| `w: 1` | primary acknowledged |
| `w: 'majority'` | majority acknowledged |

Use majority for critical state transitions.

## Read Preference

Read preference chooses where reads go: primary, secondary, secondaryPreferred, nearest, etc. Secondary reads can be stale.

## Transaction Costs

- more latency
- conflict/retry handling
- more resource usage
- timeout concerns
- harder failure handling

## Senior Interview Answer

MongoDB supports transactions, but the first design question is whether the invariant can fit in one document. If yes, single-document atomicity is cheaper and simpler. If not, use sessions and transactions with explicit read/write concern and retry handling.
