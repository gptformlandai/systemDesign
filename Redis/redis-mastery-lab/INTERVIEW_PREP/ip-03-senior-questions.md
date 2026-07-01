# Interview Prep 03: Senior Questions

## Questions And Answers

**Q: How does Redis replication work?**
Replicas connect to the primary and receive an initial RDB snapshot. After that, the primary streams write commands asynchronously. PSYNC enables partial resync from the replication backlog on reconnect, avoiding a full RDB resync.

**Q: What is the WAIT command?**
`WAIT n timeout-ms` blocks until n replicas have acknowledged the most recent writes or timeout expires. Returns the count of confirming replicas. It is best-effort durability, not a hard guarantee.

**Q: What is Redis Sentinel?**
An orchestration layer that monitors Redis primary and replicas. When quorum Sentinels agree the primary is down, a leader promotes the best replica. Clients use Sentinel-aware libraries to discover the new primary after failover.

**Q: What is the minimum Sentinel count and why?**
Three Sentinels for quorum 2. This tolerates one Sentinel failure while still reaching agreement.

**Q: How does Redis Cluster route requests?**
Keys are assigned to 16384 hash slots via `CRC16(key) mod 16384`. Each primary owns a slot range. Wrong-node requests receive a MOVED redirect. Cluster-aware clients update their slot map and retry.

**Q: What is a MOVED redirect vs an ASK redirect?**
MOVED: permanent. The slot has been assigned to another node. Client updates slot map and retries. ASK: temporary. Slot is being migrated. Client sends ASKING then retries to target node without updating slot map.

**Q: How do you prevent data loss in Sentinel?**
Configure `min-replicas-to-write 1` and `min-replicas-max-lag 10` on the primary. This refuses writes if no replica is in sync, preventing writes to an isolated primary that will be deposed.

**Q: What is replication backlog and when does full resync occur?**
The replication backlog is a circular buffer of recent write commands. If a replica reconnects and its last offset is within the backlog, partial resync sends only the missing commands. If outside the backlog, full resync occurs via BGSAVE + RDB transfer.

**Q: What are hash tags and why are they needed?**
Hash tags `{...}` cause only the bracketed part to be used for slot assignment. Without them, multi-key commands across different slots fail with CROSSSLOT errors. Hash tags co-locate related keys in the same slot.

**Q: What does `CONFIG REWRITE` do?**
Persists the current runtime configuration back to the redis.conf file. Without it, CONFIG SET changes are lost on restart.
