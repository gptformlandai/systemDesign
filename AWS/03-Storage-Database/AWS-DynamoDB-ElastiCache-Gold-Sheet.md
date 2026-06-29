# AWS NoSQL and Cache: DynamoDB and ElastiCache Gold Sheet

> Track: AWS Interview Track — Storage and Database
> Goal: design DynamoDB tables with the right access patterns, understand capacity modes, and choose the right caching strategy with ElastiCache.

---

## 0. How To Read This

Beginner focus:
- DynamoDB table, partition key, sort key
- On-demand vs provisioned capacity
- ElastiCache Redis vs Memcached

Intermediate focus:
- DynamoDB GSI, LSI, query vs scan
- DAX for DynamoDB caching
- DynamoDB Streams for event-driven patterns
- ElastiCache caching strategies

Senior / MAANG focus:
- DynamoDB single-table design
- Hot partition problem and key design
- DynamoDB Transactions (TransactWriteItems)
- WCU/RCU calculation and capacity planning
- ElastiCache for session store, leaderboard, rate limiting
- Redis data structures for production patterns
- ElastiCache Serverless

---

# Topic 1: Amazon DynamoDB

## 1. Intuition

DynamoDB is a fully managed NoSQL key-value and document database.

It scales horizontally, delivers single-digit millisecond latency, and requires no capacity planning with on-demand mode.

The mental model:

```text
Table = collection of items
Item = one row (map of attribute name → value)
Partition Key = hash key that routes item to a partition
Sort Key (optional) = range key for ordering within a partition
```

## 2. Capacity Modes

| Mode | How It Works | Use Case |
|---|---|---|
| On-Demand | pay per request; AWS scales automatically | unpredictable or spiky traffic |
| Provisioned | reserve RCUs and WCUs; pay for reserved capacity | predictable traffic, cost optimization |
| Provisioned + Auto Scaling | auto-adjust provisioned capacity based on target utilization | steady traffic with variable peaks |

WCU/RCU calculation:

```text
1 WCU = write 1 item up to 1 KB per second
1 RCU = read 1 item up to 4 KB per second (strongly consistent)
      = read 2 items up to 4 KB per second (eventually consistent, 0.5 RCU each)

Transactional reads/writes cost 2x (2 RCU/WCU)
```

Example:

```text
Write 100 items/second, average item size 2 KB:
  100 * ceil(2/1) = 200 WCUs required

Read 1000 items/second, average 3 KB, eventually consistent:
  1000 * ceil(3/4) * 0.5 = 500 RCUs required
```

## 3. Primary Key Design

Single key:

```text
Partition Key: userId
Each item is a unique user profile
Fast lookup by userId
```

Composite key:

```text
Partition Key: userId
Sort Key: timestamp#orderId
Enables: get all orders for a user, get orders in a time range
```

Hot partition problem:

```text
If the same partition key handles too much traffic, that partition becomes a hot spot.
DynamoDB partitions are limited to 3,000 RCU and 1,000 WCU per partition.

Bad key design: date as partition key for time-series events (all writes go to today's partition)
Good key design: distribute by userId, deviceId, orderId, or a random prefix
```

Write sharding for hot keys:

```text
Instead of partition key = "product-123":
  Use partition key = "product-123#" + random suffix (0-9)
  Write to a random shard
  Read all shards and merge
```

## 4. Secondary Indexes

| Index Type | Key | Scope | Limit |
|---|---|---|---|
| GSI (Global Secondary Index) | different partition key + optional sort key | entire table | 20 per table |
| LSI (Local Secondary Index) | same partition key, different sort key | within one partition | 5 per table, created at table creation |

GSI example:

```text
Table: Orders
Primary: orderId (PK)

GSI: by-customer
  PK: customerId
  SK: createdAt
  Projection: ALL

Enables: "Get all orders for customer C sorted by date"
```

GSI capacity:

```text
GSIs consume capacity independently. If a GSI has a hot partition key, it can throttle
even if the table itself is fine.

Throttle on GSI -> table writes to that item are also throttled.
Design GSI keys to spread load just like table keys.
```

## 5. Single-Table Design

Multiple entity types in one DynamoDB table using generic PK/SK with type prefixes:

```text
Table: EcommerceTable

# User
PK: USER#u-123
SK: PROFILE
Data: {name, email, createdAt}

# Order
PK: ORDER#o-456
SK: METADATA
Data: {status, total, createdAt}

# OrderItem
PK: ORDER#o-456
SK: ITEM#i-789
Data: {productId, quantity, price}

# UserOrder (relationship)
PK: USER#u-123
SK: ORDER#o-456
Data: {orderId, createdAt, status}
```

Access patterns satisfied:
- Get user: PK=USER#u-123, SK=PROFILE
- Get order: PK=ORDER#o-456, SK=METADATA
- Get order items: PK=ORDER#o-456, SK begins_with ITEM#
- Get user orders: PK=USER#u-123, SK begins_with ORDER#

Interview line:

```text
Single-table design is the DynamoDB best practice for applications with many related
entity types and well-known access patterns. The key is to identify ALL access patterns
before designing the schema, since DynamoDB does not allow ad-hoc queries.
```

## 6. DynamoDB Transactions

TransactWriteItems: atomic write across up to 100 items in up to 100 tables:

```java
client.transactWriteItems(TransactWriteItemsRequest.builder()
    .transactItems(
        TransactWriteItem.builder()
            .update(Update.builder()
                .tableName("Inventory")
                .key(Map.of("productId", AttributeValue.fromS("p-123")))
                .updateExpression("SET quantity = quantity - :q")
                .conditionExpression("quantity >= :q")
                .expressionAttributeValues(Map.of(":q", AttributeValue.fromN("1")))
                .build())
            .build(),
        TransactWriteItem.builder()
            .put(Put.builder()
                .tableName("Orders")
                .item(orderItem)
                .build())
            .build()
    ).build()
);
```

Use for:
- reserve inventory AND create order atomically
- transfer balance between accounts
- multi-item consistency requirements

Cost: transactions cost 2x WCUs/RCUs.

## 7. DynamoDB Streams

DynamoDB Streams emit change events for every item modification:

```text
Event types: INSERT, MODIFY, REMOVE
Stream view: KEYS_ONLY | NEW_IMAGE | OLD_IMAGE | NEW_AND_OLD_IMAGES

Use cases:
- Trigger Lambda on item change (event-driven architecture)
- Replicate changes to Elasticsearch/OpenSearch for search
- Audit trail
- Update materialized views in another table
- CDC (change data capture) to downstream systems
```

## 8. DAX — DynamoDB Accelerator

In-memory cache for DynamoDB:

```text
Write-through: write to DAX -> DAX writes to DynamoDB
Read: DAX hits -> returns cached item (microseconds)
Read: DAX miss -> reads from DynamoDB, caches result

Use DAX when:
- read-heavy workload with repetitive reads of same items
- cannot afford DynamoDB read latency for specific operations

Do NOT use DAX when:
- write-heavy workload (writes bypass cache, not accelerated)
- strongly consistent reads required (DAX is eventually consistent)
- Lambda functions (DAX is VPC-based, adds Lambda in VPC overhead)
```

## 9. Common DynamoDB Mistakes

| Mistake | Better Approach |
|---|---|
| Use date as partition key for time-series | use userId or deviceId + sort by time |
| Use Scan instead of Query | always Query when you have the partition key |
| Forget to project attributes in GSI | project only needed attributes, not ALL |
| Large item (400 KB limit) | store large payloads in S3, reference from DynamoDB |
| Hot partition from sequential IDs | prefix with hash or random segment |
| No TTL for ephemeral data | set TTL attribute to auto-expire temporary items |
| Transactions with slow processing | transactions hold internal locks briefly; keep transaction simple |

---

# Topic 2: Amazon ElastiCache

## 1. Redis vs Memcached

| Feature | Redis | Memcached |
|---|---|---|
| Data structures | strings, hashes, lists, sets, sorted sets, streams, bitmaps | strings only |
| Persistence | yes (RDB snapshots, AOF) | no |
| Replication | yes (primary + replicas) | no |
| Clustering | yes (cluster mode) | yes (multi-node simple) |
| Lua scripting | yes | no |
| Transactions | yes (MULTI/EXEC) | no |
| Use case | sessions, leaderboards, rate limiting, pub/sub, geospatial | simple caching only |

Choose Redis unless your team specifically needs Memcached for simplicity. Redis is the default for production.

## 2. ElastiCache Caching Strategies

| Strategy | Write | Read | Risk | Use Case |
|---|---|---|---|---|
| Cache-Aside (Lazy) | app writes to DB only | app reads cache; miss → DB, populate cache | stale data between miss and DB | general purpose |
| Write-Through | app writes to cache + DB | app reads from cache | write overhead, cold cache on launch | user profiles, small tables |
| Write-Behind | app writes to cache; cache async to DB | reads from cache | complexity, potential data loss | write-heavy with eventual persistence |
| Read-Through | cache fetches from DB on miss | reads from cache | cold start | similar to cache-aside but cache fetches |

Cache-aside is the most common production pattern:

```python
def get_user(user_id):
    cached = redis.get(f"user:{user_id}")
    if cached:
        return json.loads(cached)
    
    user = db.query("SELECT * FROM users WHERE id = ?", user_id)
    redis.setex(f"user:{user_id}", 300, json.dumps(user))  # TTL 5 min
    return user
```

## 3. Redis Data Structures For Production Patterns

Sorted Sets (leaderboard):

```python
# Add player score
redis.zadd("leaderboard:daily", {"player:123": 9500})

# Get top 10 players
redis.zrevrange("leaderboard:daily", 0, 9, withscores=True)

# Get player rank
redis.zrevrank("leaderboard:daily", "player:123")
```

Sets (session tokens, dedupe):

```python
# Track unique visitors today
redis.sadd(f"visitors:{today}", user_id)
redis.expire(f"visitors:{today}", 86400)
count = redis.scard(f"visitors:{today}")
```

Rate limiting with fixed window:

```python
def allow_request(user_id, limit=100, window=60):
    key = f"rate:{user_id}:{int(time.time() / window)}"
    count = redis.incr(key)
    if count == 1:
        redis.expire(key, window)
    return count <= limit
```

Sliding window rate limiting with sorted sets:

```python
def allow_request_sliding(user_id, limit=100, window=60):
    now = time.time()
    key = f"rate_sliding:{user_id}"
    pipe = redis.pipeline()
    pipe.zremrangebyscore(key, 0, now - window)
    pipe.zadd(key, {str(now): now})
    pipe.zcard(key)
    pipe.expire(key, window)
    results = pipe.execute()
    return results[2] <= limit
```

Pub/Sub (notifications):

```python
# Publisher
redis.publish("order-updates", json.dumps({"orderId": "o-123", "status": "shipped"}))

# Subscriber
pubsub = redis.pubsub()
pubsub.subscribe("order-updates")
for message in pubsub.listen():
    if message["type"] == "message":
        handle(json.loads(message["data"]))
```

## 4. ElastiCache Cluster Modes

| Mode | Replication | Sharding | Use Case |
|---|---|---|---|
| Single node | none | none | dev/test |
| Cluster mode disabled | 1 primary + up to 5 replicas | no | read-heavy, single shard |
| Cluster mode enabled | shards across nodes | yes (up to 500 shards) | write-heavy, horizontal scale |

Multi-AZ with automatic failover:
- enabled for production
- replica promoted automatically if primary fails
- ~60 seconds failover time

## 5. Session Store Pattern

Store web session in Redis instead of server memory:

```text
Benefits:
- stateless servers (any server handles any request)
- auto-expiry with Redis TTL
- survives server restart (Redis data persists if AOF enabled)
- shared across all app instances

Spring Boot integration:
  spring.session.store-type=redis
  spring.data.redis.host=${ELASTICACHE_ENDPOINT}
```

## 6. ElastiCache Serverless

Fully managed ElastiCache without node management:

```text
Scale from minimum to maximum automatically
Pay per ECU (ElastiCache Compute Unit) and GB-hour
No node type selection, no cluster sizing

Use when:
- variable traffic, do not want to size clusters
- want operational simplicity over cost control
- dev/test or new applications

Consider provisioned when:
- predictable load, cost-sensitive, large volume
```

## 7. Common Mistakes

| Mistake | Better Approach |
|---|---|
| No TTL on cached items | always set TTL to prevent stale data accumulation |
| Cache S3/Lambda responses without cache-control | set appropriate TTL per data type |
| Write directly to cache without write-through or invalidation | use read-through or invalidate on update |
| Single-node ElastiCache in production | Multi-AZ with automatic failover |
| Cache large objects (>10 MB) | cache serialized references, not huge objects |
| DynamoDB Scan in a loop | paginate Scans, but better: redesign key schema to use Query |
| No retry logic on DynamoDB | use AWS SDK exponential backoff, handle ProvisionedThroughputExceededException |

## 8. Revision Notes

- DynamoDB: partition key determines which physical partition; hot key = throttle for that partition
- On-demand mode for spiky; provisioned + auto-scaling for predictable
- Single-table design: all entities in one table, type prefix in PK/SK, access patterns drive design
- GSI: query by non-primary keys; consume separate capacity; design to spread load
- Transactions: atomic across items, 2x capacity cost
- DynamoDB Streams: event-driven, trigger Lambda, CDC, replication
- Redis vs Memcached: Redis always unless only need simple string cache
- Cache-aside: most common pattern; always set TTL; invalidate on write

## 9. Official Source Notes

- DynamoDB: <https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html>
- DynamoDB best practices: <https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html>
- Single-table design: <https://www.alexdebrie.com/posts/dynamodb-single-table/>
- ElastiCache: <https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/WhatIs.html>
- ElastiCache Serverless: <https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/serverless.html>
