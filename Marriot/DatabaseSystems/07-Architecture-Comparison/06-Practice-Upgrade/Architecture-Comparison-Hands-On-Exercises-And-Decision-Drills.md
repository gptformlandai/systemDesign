# Architecture Comparison Hands-On Exercises and Decision Drills

> Track File #27 of 30 - Group 06: Practice Upgrade
> For: practical drills | Level: beginner to pro | Mode: workload-to-datastore decisions

Use these with the `architecture-comparison-lab` folder.

---

## Drill 1: Source Of Truth Or Derived Store

Classify each as source or derived:

- order payment status
- product search index
- RAG vector index
- Redis cart cache
- analytics dashboard table
- image blob in object storage

---

## Drill 2: Pick The Datastore

For each workload, choose primary and derived stores:

- payment ledger
- product search
- social feed
- chat messages
- fraud ring traversal
- semantic document search
- log search
- metrics dashboard

---

## Drill 3: Name The Failure Mode

For each choice, name one failure:

- SQL read replica
- MongoDB large document
- Cassandra partition
- Elasticsearch index
- Vector DB embedding upgrade
- Neo4j supernode
- Redis cache
- warehouse pipeline

---

## Drill 4: Interview Answer In 90 Seconds

Prompt:

```text
Design datastore choices for an ecommerce marketplace.
```

Answer must include:

- source of truth
- search index
- cache
- recommendations/vector
- analytics
- consistency and freshness risks

---

## Completion Gate

You finish these drills only when you can explain:

- why each datastore is chosen
- what each datastore should not own
- how derived stores sync
- what fails under production stress
- which alternative you rejected and why