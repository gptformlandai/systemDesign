# Interview Prep 01: Beginner Questions

## Questions And Answers

**Q: What is Redis?**
Redis is an in-memory data structure server. It stores data in RAM for fast access and supports strings, lists, hashes, sets, sorted sets, streams, HyperLogLog, and geospatial data.

**Q: Why is Redis fast?**
- Data lives in RAM
- Single-threaded event loop eliminates lock contention
- Simple RESP protocol
- O(1) time complexity for most operations

**Q: What is the difference between KEYS and SCAN?**
KEYS is O(N) and blocks Redis for the entire operation. SCAN uses a cursor to iterate in batches without blocking. Use SCAN in production.

**Q: What does TTL return?**
- Positive number: seconds until expiry
- -1: key exists with no TTL
- -2: key does not exist

**Q: How do you delete a key?**
`DEL key` — synchronous. `UNLINK key` — asynchronous background deletion.

**Q: What is PERSIST?**
PERSIST removes the TTL from a key, making it persist indefinitely.

**Q: What is the difference between SET with EX vs PX?**
EX sets TTL in seconds. PX sets TTL in milliseconds.

**Q: What is the difference between LPUSH and RPUSH?**
LPUSH adds to the head (left). RPUSH adds to the tail (right). For a FIFO queue: RPUSH to enqueue, LPOP to dequeue.

**Q: How do you check if a field exists in a hash?**
`HEXISTS key field` returns 1 if present, 0 if not.

**Q: What is SISMEMBER?**
Checks if a value is a member of a set. Returns 1 or 0.

**Q: What is ZADD?**
Adds a member to a sorted set with a score. `ZADD key score member`. If member exists, updates the score.

**Q: What is INFO?**
Displays Redis server statistics in sections: memory, clients, stats, replication, keyspace, persistence, and more.
