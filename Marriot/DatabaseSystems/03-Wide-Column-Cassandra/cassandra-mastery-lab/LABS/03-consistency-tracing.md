# Lab 03: Consistency And Tracing

Goal: connect CQL reads to coordinator/replica behavior and consistency-level language.

---

## Run

```bash
bash SCRIPTS/run-cqlsh.sh SCRIPTS/04-tracing-consistency.cql
```

---

## What To Observe

The script enables tracing for a primary-key-shaped query.

In a single-node local lab, consistency behavior is limited. The learning value is still useful:

- You see that a coordinator executes the request.
- You see query steps rather than treating Cassandra as a black box.
- You practice explaining how this changes with RF=3 in production.

---

## Interview Drill

Question:

```text
With RF=3, why does LOCAL_QUORUM read plus LOCAL_QUORUM write give stronger local read-your-write behavior?
```

Answer shape:

```text
For RF=3, quorum is 2. If the write reaches 2 replicas and the read consults 2 replicas in the same DC, the read and write sets overlap on at least one replica. That makes stale reads less likely than ONE/ONE, at the cost of more latency and less availability during replica failures.
```

---

## Completion Gate

- You can define ONE, LOCAL_ONE, QUORUM, LOCAL_QUORUM, and ALL.
- You can explain timeout ambiguity.
- You can explain why local single-node tests do not prove multi-node production behavior.