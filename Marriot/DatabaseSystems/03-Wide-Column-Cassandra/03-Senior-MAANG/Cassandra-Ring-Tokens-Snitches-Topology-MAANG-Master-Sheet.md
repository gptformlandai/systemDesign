# Cassandra Ring, Tokens, Snitches, and Topology - MAANG Master Sheet

> Track File #11 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: cluster topology, replica placement, scaling

This sheet builds:
- Token ring and vnode mental model
- Snitches, racks, data centers, and replica placement
- Scaling and topology interview judgment

---

## 1. Token Ring Mental Model

Cassandra hashes partition keys into tokens. Nodes own token ranges. Replicas are placed based on token ownership and replication strategy.

```text
partition key -> hash token -> token range -> primary owning node -> additional replicas
```

The ring is logical. Modern clusters often use virtual nodes, so each physical node owns many smaller token ranges.

---

## 2. Virtual Nodes

Vnodes help distribute data and simplify balancing.

Benefits:

- smoother data distribution
- easier node additions/removals
- less manual token planning

Costs:

- repair and streaming can involve many ranges
- operational behavior depends on configuration and cluster size

---

## 3. Snitches

A snitch tells Cassandra about network topology: data centers and racks.

Why it matters:

- replica placement should spread across racks
- local-DC reads should prefer nearby replicas
- failure domains should not contain all replicas for a partition

Common production idea:

```text
Use topology-aware snitches and NetworkTopologyStrategy so replicas are placed across racks and data centers correctly.
```

---

## 4. Racks And Data Centers

| Concept | Purpose |
|---|---|
| Rack | Local failure domain inside a DC |
| Data center | Regional or logical isolation boundary |
| NetworkTopologyStrategy | Replication strategy that understands DC/rack placement |
| LOCAL_QUORUM | Quorum within local DC only |

Interview nuance:

```text
Do not use multi-DC replication without explaining client locality, cross-DC latency, repair, and failure behavior.
```

---

## 5. Adding Nodes

When adding nodes, Cassandra streams token ranges to the new node.

Consider:

- disk and network bandwidth
- compaction pressure
- repair schedule
- client traffic during bootstrap
- uneven load from hot partitions that new nodes cannot fix alone

Adding nodes helps distributed load, but it does not fix a single hot partition key.

---

## 6. Hot Partitions And Topology

If one partition key receives extreme traffic, all requests for that partition target the same replica set.

Adding more nodes may not help because the hot key still maps to a limited set of replicas.

Fixes:

- better partition key
- bucketing/sharding key component
- fan-out design
- cache or write aggregation layer
- product-level throttling

---

## 7. Strong Answer

Question:

> How does Cassandra decide where data lives?

Strong answer:

```text
Cassandra hashes the partition key into a token. Nodes own token ranges, often many ranges through vnodes. The replication strategy and snitch decide which additional replicas store that partition, ideally spreading copies across racks and data centers. This means partition-key design determines both query routing and load distribution, while topology settings determine failure-domain safety.
```

---

## 8. Revision Notes

- One-line summary: Partition keys map to token ranges; topology controls replica placement.
- Three keywords: token, vnode, snitch.
- One interview trap: thinking adding nodes solves hot-key design.
- Memory trick: topology spreads replicas; partition keys spread workload.