# Elasticsearch Installation, Tools, REST, and Kibana - Gold Sheet

> Track File #3 of 27 - Group 01: Starter Path
> For: backend/search/system design interviews | Level: beginner | Mode: local setup, tools, first commands

This sheet builds:
- Local practice setup
- REST API and Kibana Dev Tools workflow
- First operational vocabulary

---

## 1. Practical Setup Options

| Option | Use When | Notes |
|---|---|---|
| Docker single node | Learning APIs, mappings, queries, aggregations | Fastest local start |
| Docker Compose with Kibana | Learning Dev Tools and dashboards | More complete local experience |
| Elastic Cloud | Managed production-like practice | Feature/version/license behavior matters |
| OpenSearch local | Similar concepts, different product/ecosystem | Mention differences carefully in interviews |

For interviews, local Docker plus curl/Kibana Dev Tools is enough to learn index design, query DSL, aggregations, analyzers, and incident drills.

---

## 2. Docker Quick Start

```bash
docker run --name elasticsearch-lab \
  -p 9200:9200 \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -e ES_JAVA_OPTS="-Xms1g -Xmx1g" \
  -d docker.elastic.co/elasticsearch/elasticsearch:8.14.3

curl http://localhost:9200
```

Local security is disabled here only for learning simplicity. Production must use auth, TLS, roles, and secrets.

---

## 3. First REST Commands

```bash
curl http://localhost:9200/_cluster/health?pretty
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_nodes/stats?pretty
```

Create an index:

```bash
curl -X PUT http://localhost:9200/products
```

Index a document:

```bash
curl -X POST http://localhost:9200/products/_doc/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical keyboard","category":"electronics","price":129.99}'
```

Search:

```bash
curl 'http://localhost:9200/products/_search?q=keyboard&pretty'
```

---

## 4. Kibana Dev Tools

Kibana Dev Tools lets you write API requests without curl noise.

Example:

```http
GET _cluster/health

GET products/_search
{
  "query": {
    "match": {
      "name": "keyboard"
    }
  }
}
```

---

## 5. Useful APIs

| API | Purpose |
|---|---|
| `_cluster/health` | cluster status |
| `_cat/indices` | index list and sizes |
| `_cat/shards` | shard placement |
| `_nodes/stats` | node metrics |
| `_mapping` | field mappings |
| `_settings` | index settings |
| `_analyze` | analyzer/token output |
| `_search` | query execution |
| `_bulk` | bulk ingest |
| `_reindex` | copy/rebuild documents |
| `_snapshot` | backup/restore |

---

## 6. Strong Answer

Question:

> How would you start learning Elasticsearch hands-on?

Strong answer:

```text
I would run a local single-node Elasticsearch container with security disabled only for lab practice, then use curl or Kibana Dev Tools to create indexes, define mappings, index documents, run searches, inspect analyzers, and test aggregations. After the basics, I would practice aliases, bulk indexing, refresh behavior, slow-query debugging, and snapshot/restore concepts because production Elasticsearch is as much operations as syntax.
```

---

## 7. Revision Notes

- One-line summary: Learn Elasticsearch through REST APIs, mappings, searches, and operational inspection.
- Three keywords: REST, Dev Tools, `_analyze`.
- One interview trap: disabling security in production because local tutorials do.
- Memory trick: curl teaches API shape; Kibana teaches exploratory speed.