# Cassandra Installation, Tools, and CQLSH - Gold Sheet

> Track File #3 of 25 - Group 01: Starter Path
> For: backend/database/system design interviews | Level: beginner | Mode: local setup, tools, first commands

This sheet builds:
- Local practice setup
- Basic cqlsh and nodetool workflow
- First operational vocabulary

---

## 1. Practical Setup Options

| Option | Use When | Notes |
|---|---|---|
| Docker single node | Learning CQL and table modeling | Fastest local start |
| Docker multi-node | Practicing replication and consistency | More realistic but heavier |
| DataStax Astra / managed Cassandra | Cloud practice without local ops | Feature set may differ from Apache Cassandra |
| Local package install | Deep node inspection | More maintenance on macOS |

For interviews, local Docker is enough to learn CQL, primary keys, TTL, tracing, and simple consistency behavior.

---

## 2. Docker Quick Start

```bash
docker run --name cassandra-lab -p 9042:9042 -d cassandra:5
docker logs -f cassandra-lab
docker exec -it cassandra-lab cqlsh
```

The node can take time to accept CQL connections. Wait until logs show it is listening for clients.

---

## 3. First CQL Commands

```sql
DESCRIBE KEYSPACES;

CREATE KEYSPACE IF NOT EXISTS app
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE app;

CREATE TABLE IF NOT EXISTS users_by_id (
  user_id text PRIMARY KEY,
  email text,
  name text,
  created_at timestamp
);

INSERT INTO users_by_id (user_id, email, name, created_at)
VALUES ('u1', 'asha@example.com', 'Asha', toTimestamp(now()));

SELECT * FROM users_by_id WHERE user_id = 'u1';
```

---

## 4. Keyspace Replication

For local single-node learning:

```sql
CREATE KEYSPACE app
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
```

For production-style multi-DC thinking:

```sql
CREATE KEYSPACE app
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'dc1': 3,
  'dc2': 3
};
```

Interview rule:

```text
SimpleStrategy is for local learning. NetworkTopologyStrategy is the production default because it understands data centers and racks.
```

---

## 5. cqlsh Commands

| Command | Purpose |
|---|---|
| `DESCRIBE KEYSPACES;` | List keyspaces |
| `DESCRIBE TABLES;` | List tables in current keyspace |
| `DESCRIBE TABLE table_name;` | Show table schema |
| `CONSISTENCY;` | Show current consistency level |
| `CONSISTENCY QUORUM;` | Set session consistency level |
| `TRACING ON;` | Show request tracing for queries |
| `PAGING ON;` | Enable paged output |
| `EXPAND ON;` | Show rows vertically |

---

## 6. nodetool Commands

Inside the container:

```bash
nodetool status
nodetool info
nodetool ring
nodetool describecluster
nodetool tpstats
nodetool compactionstats
nodetool tablestats app.users_by_id
```

Production caution:

```text
nodetool repair, cleanup, drain, decommission, and assassinate are operational commands. Do not run them casually in real clusters.
```

---

## 7. Strong Answer

Question:

> How would you start learning Cassandra hands-on?

Strong answer:

```text
I would start with a single-node Docker container and cqlsh to learn keyspaces, tables, primary keys, and CQL query restrictions. Then I would move to a multi-node setup to observe replication, consistency levels, nodetool status, tracing, and failure behavior. I would not treat single-node results as proof of production behavior because Cassandra's real behavior appears under replication, compaction, repair, and failure.
```

---

## 8. Revision Notes

- One-line summary: Use Docker and cqlsh for learning; use nodetool to inspect cluster health.
- Three keywords: cqlsh, nodetool, NetworkTopologyStrategy.
- One interview trap: using SimpleStrategy as if it were production-grade.
- Memory trick: cqlsh teaches syntax; nodetool teaches operations.