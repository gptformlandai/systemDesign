# 06 - Consistency Finance Strict Transactions Architecture

> Goal: design a strict financial transaction system where correctness, auditability, idempotency, and reconciliation matter more than always accepting writes.

---

## 1. Problem Statement

Design a finance system for:

- account lookup
- balance display
- internal transfer
- external payment/settlement
- ledger entries
- transaction history
- fraud/risk checks
- notifications
- reconciliation
- audit and compliance

Primary product promise:

```text
Money must not be duplicated, lost, silently overwritten, or reported as committed unless the system
can prove the commit.
```

Strict consistency posture:

```text
If correctness cannot be guaranteed, return pending, reject, or degrade non-critical features.
Do not fake success for money movement.
```

---

## 2. CAP Position

Consistency-first paths:

| Path | Consistency Choice | Why |
|---|---|---|
| ledger write | strict ACID | money movement invariant |
| debit/credit transfer | atomic transaction | prevent money disappearance |
| account ownership | strong authorization | prevent unauthorized transfer |
| limits/risk block | strong enough for enforcement | prevent abuse |
| transaction status | authoritative state | user trust/compliance |

Availability-tolerant paths:

| Path | Availability Strategy |
|---|---|
| marketing pages | CDN/cache |
| transaction history | can show cached with freshness label if safe |
| notifications | async after commit |
| analytics/reporting | delayed |
| external settlement | pending/reconciled states |

CAP answer:

```text
For finance, I choose consistency over availability for money movement. During a partition, if the
system cannot verify account state and commit a balanced ledger transaction, it should not accept
the transfer as successful.
```

---

## 3. High-Level Architecture

```text
Browser / Mobile App
  |
  v
CDN for static assets only
  |
  v
WAF + Bot Defense + TLS
  |
  v
API Gateway / BFF
  |
  v
Transfer Service
  |
  +--> AuthZ / Risk / Limits
  |
  +--> Ledger Service
          |
          +--> Ledger DB / Distributed SQL
          +--> Idempotency Table
          +--> Audit Table
          +--> Transactional Outbox
                    |
                    +--> Kafka/Event Bus
                            |
                            +--> Notifications
                            +--> Reporting
                            +--> Reconciliation
                            +--> Fraud Analytics
```

Core design:

```text
Strict transaction for ledger mutation. Async events only after durable commit.
```

---

## 4. Request Lifecycle - Internal Transfer

```text
1. User submits transfer
2. Client sends POST /transfers with auth, idempotency key, device context, traceparent
3. Edge/WAF checks request and blocks abuse
4. Gateway authenticates and enforces route/user limits
5. Transfer service validates schema and actor account ownership
6. Risk/limits check runs
7. Ledger service begins DB transaction
8. Idempotency key is inserted or existing result is returned
9. Source account debit entry is created
10. Destination account credit entry is created
11. Balance projection is updated or marked for committed projection
12. Audit row and outbox event are written in same transaction
13. DB commits
14. Response returns COMMITTED with transfer ID
15. Outbox relay publishes TransferCommitted
16. Notification/reporting/reconciliation consume asynchronously
```

If DB cannot commit:

```text
Return failed or pending based on whether outcome is known. Do not return committed.
```

---

## 5. Ledger Data Model

Ledger transaction:

```text
ledger_transaction
  id
  idempotency_key
  actor_id
  transaction_type
  status
  request_hash
  created_at
  committed_at
```

Ledger entry:

```text
ledger_entry
  id
  ledger_transaction_id
  account_id
  direction: DEBIT | CREDIT
  amount
  currency
  created_at
```

Balance projection:

```text
account_balance
  account_id
  available_balance
  pending_balance
  currency
  ledger_version
  updated_at
```

Audit:

```text
audit_log
  id
  actor_id
  action
  resource_type
  resource_id
  trace_id
  created_at
  metadata_redacted
```

Invariant:

```text
For every ledger_transaction:
  total_debits == total_credits for each currency/accounting scope
```

Wrong option:

```text
Only store account_balance and mutate it in place.
```

What fails:

```text
You cannot reconstruct history, audit money movement, reconcile external statements, or recover from
bad writes safely.
```

Better:

```text
Use immutable double-entry ledger rows and derive balances from committed ledger state.
```

---

## 6. Transaction Strategy

Inside one ledger database:

```text
Use ACID transaction, unique idempotency constraint, row locks/optimistic versioning, and invariant checks.
```

Example transaction:

```sql
BEGIN;

INSERT INTO idempotency_keys(key, request_hash, status)
VALUES (:key, :request_hash, 'IN_PROGRESS');

SELECT * FROM accounts
WHERE account_id IN (:from_account, :to_account)
FOR UPDATE;

-- validate balance, limits, currency, account status

INSERT INTO ledger_transaction(id, idempotency_key, status)
VALUES (:tx_id, :key, 'COMMITTED');

INSERT INTO ledger_entry(transaction_id, account_id, direction, amount, currency)
VALUES
  (:tx_id, :from_account, 'DEBIT', :amount, :currency),
  (:tx_id, :to_account, 'CREDIT', :amount, :currency);

UPDATE account_balance
SET available_balance = available_balance - :amount,
    ledger_version = ledger_version + 1
WHERE account_id = :from_account;

UPDATE account_balance
SET available_balance = available_balance + :amount,
    ledger_version = ledger_version + 1
WHERE account_id = :to_account;

INSERT INTO outbox_events(id, aggregate_id, event_type, payload)
VALUES (:event_id, :tx_id, 'TransferCommitted', :payload);

COMMIT;
```

Wrong option:

```text
Debit account in one transaction and credit destination in a later async consumer.
```

What fails:

```text
Money can disappear if the consumer fails, or balances can be inconsistent during the gap.
```

Better:

```text
Commit balanced ledger entries atomically. Use async only for external side effects after commit.
```

---

## 7. 2PC, Saga, Or Local Transaction?

| Pattern | Use In Finance | Explanation |
|---|---|---|
| local ACID transaction | yes for internal ledger | best for core debit/credit invariant |
| distributed SQL transaction | yes if global/account distribution needs it | accepts coordination cost |
| 2PC | limited internal use | only if participants support it and availability trade-off accepted |
| saga | external settlement/workflow | not for atomic ledger invariant |
| outbox | yes | emit events after commit |
| reconciliation | yes | detect external/internal mismatch |

Wrong option:

```text
Use saga for the internal ledger because microservices should not share a database.
```

What fails:

```text
Core money invariant becomes eventually consistent. Compensation is not equivalent to never being wrong.
```

Better:

```text
Keep ledger mutation inside one transactional boundary. Service purity is less important than money correctness.
```

Where saga fits:

```text
Ledger committed -> external bank transfer initiated -> bank pending -> bank settled -> reconciliation.
```

The internal ledger can record pending/settlement states, but the balanced internal accounting remains durable.

---

## 8. Database Choice

Good choices:

| Choice | Fit |
|---|---|
| PostgreSQL/MySQL with strong operational discipline | single-region/regional ledger |
| Aurora/RDS-style managed relational | managed OLTP with HA |
| Spanner/CockroachDB/YugabyteDB | distributed SQL and strict transactions |
| FoundationDB | transactional ordered key-value foundation |

Risky choices for core ledger:

| Choice | Why Risky |
|---|---|
| eventually consistent wide-column with LWW | conflict/lost update risk |
| Elasticsearch | derived index, not ledger source |
| Redis primary store | durability/eviction risk unless specialized design |
| document store without strict transaction discipline | cross-account invariant complexity |
| warehouse/lake | analytics, not OLTP |

Wrong option:

```text
Use Cassandra with eventual reads/writes and last-write-wins for balances.
```

What fails:

```text
Concurrent transfers can conflict, and last-write-wins can erase money movements.
```

Better:

```text
Use a database with strict transaction semantics for the ledger. Use Cassandra/warehouse/search only
for derived history/reporting if appropriate.
```

---

## 9. Sharding Finance Data

Shard key options:

| Key | Fit | Risk |
|---|---|---|
| accountId | account-scoped transactions | cross-account transfer across shards |
| customerId | customer view | business accounts/hot users |
| ledgerTransactionId | lookup/write distribution | account history needs index |
| region/account home | data residency/locality | cross-region transfers |

Cross-shard transfer options:

| Option | Fit | Trade-off |
|---|---|---|
| keep accounts on same shard when possible | internal transfers | rebalancing complexity |
| distributed transaction | strict correctness | latency/coordination |
| clearing account model | external-like settlement | more accounting complexity |
| account home region | clear ownership | cross-region pending/settlement |

Wrong option:

```text
Randomly shard ledger entries only by transaction ID and ignore account access.
```

What fails:

```text
Balance calculation, account history, limits, and account locking become expensive scatter-gather operations.
```

Better:

```text
Shard around account ownership/access patterns and design cross-shard transfers explicitly.
```

---

## 10. Idempotency And Retry Behavior

Idempotency table:

```text
idempotency_key
actor_id
operation_type
request_hash
status
resource_id
response_code
response_body_hash
created_at
expires_at
```

Rules:

- same key + same request returns original result
- same key + different request returns conflict
- unique constraint prevents concurrent duplicate commits
- unknown outcomes are reconciled, not blindly retried

Retry behavior:

| Failure | Behavior |
|---|---|
| client timeout after commit | retry returns same committed transfer |
| DB deadlock before commit | retry transaction with same idempotency key |
| provider timeout external rail | mark pending/unknown and reconcile |
| validation error | do not retry |
| rate limit | retry after delay |

Wrong option:

```text
Let mobile app generate a new transfer ID on every retry.
```

What fails:

```text
Duplicate transfers can be committed.
```

Better:

```text
One logical transfer action gets one idempotency key reused across retries.
```

---

## 11. External Settlement

Internal ledger and external settlement are different.

Internal ledger:

- strict
- immediate transaction boundary
- source of truth for internal accounting

External settlement:

- asynchronous
- provider/bank dependent
- may be delayed, rejected, reversed
- requires reconciliation

External workflow:

```text
1. Internal ledger records transfer intent/pending
2. External rail request is sent with provider idempotency key
3. Provider returns accepted/pending/rejected/timeout
4. Webhooks update settlement state
5. Reconciliation confirms final state
6. Correction entries are appended if needed
```

Wrong option:

```text
Assume provider success because HTTP request was sent.
```

What fails:

```text
Network success does not equal financial settlement. Provider may reject or timeout later.
```

Better:

```text
Model provider state explicitly and reconcile.
```

---

## 12. Availability Strategy Without Violating Consistency

Allowed degradations:

- show cached transaction history with freshness label
- delay notifications
- pause external settlement initiation
- return pending
- reject new transfer temporarily
- allow read-only mode
- fail over to standby if data consistency is known

Not allowed:

- bypass ledger DB and write to cache
- accept transfer without idempotency
- ignore balance/limit checks
- double-submit provider requests with new keys
- mutate audit/ledger history destructively

Wrong option:

```text
During ledger DB outage, store transfers in Redis and commit later while telling users success.
```

What fails:

```text
Redis may lose data, ordering and balance checks are not guaranteed, and users see false committed state.
```

Better:

```text
Return service unavailable or pending only if durable acceptance is recorded. Preserve correctness over write availability.
```

---

## 13. Observability View

Trace for transfer:

```text
POST /transfers
  gateway.auth
  gateway.rate_limit
  transfer.validate
  risk.check
  ledger.idempotency_insert
  ledger.lock_accounts
  ledger.write_entries
  ledger.update_balance_projection
  audit.write
  outbox.write
```

Datadog dashboard:

- transfer API p95/p99
- ledger commit latency
- DB lock wait/deadlock count
- idempotency replay/conflict count
- unbalanced transaction attempts blocked
- reconciliation mismatch count
- pending transfer age
- outbox lag
- external rail latency/error
- audit log write failures

Critical monitor:

```text
unbalanced_ledger_committed > 0
```

Expected value:

```text
0 always
```

---

## 14. Chosen Final Architecture

Final choice:

```text
Use a strict transactional ledger as the source of truth, with immutable double-entry entries,
idempotency keys, account-level concurrency control, audit logs, and transactional outbox. Use async
events for notifications, reporting, fraud analytics, and external settlement workflows. If strict
commit cannot be guaranteed, return pending or reject rather than violating financial correctness.
```

Why this is right:

- protects money invariant
- supports audit/reconciliation
- handles retries safely
- separates internal truth from external settlement
- keeps async side effects from corrupting ledger
- gives operators evidence during incidents

Why tempting alternatives are wrong:

| Alternative | Why Rejected |
|---|---|
| eventual balance updates | double spend/wrong balance |
| saga for debit/credit invariant | compensation is not atomic correctness |
| cache as source of balance | stale/lost money state |
| no ledger, just mutable balance | no audit/reconciliation |
| blind retry on timeout | duplicate transfer/charge risk |

---

## 15. Strong Interview Answer

```text
For finance, I would design around a strict ledger, not around mutable balances. A transfer request
uses an idempotency key and enters one transactional boundary that writes balanced debit and credit
ledger entries, updates a balance projection if needed, writes audit records, and records an outbox
event. Notifications, reporting, and external settlement are async after commit. I would not use
eventual consistency for the internal money movement invariant, and I would not use saga as a
replacement for atomic debit/credit. If the ledger cannot commit safely, the API returns pending or
rejects rather than claiming success. Reconciliation catches drift with external systems.
```

---

## 16. Revision Notes

- One-line summary: Finance chooses correctness over write availability for money movement.
- Three keywords: ledger, idempotency, reconciliation.
- One interview trap: using a saga to replace an atomic ledger transaction.
- Memory trick: never move money without a balanced receipt.

