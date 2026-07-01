# Elasticsearch REST Cheatsheet

```http
GET _cluster/health
GET _cat/indices?v
GET _cat/shards?v
GET products-v1/_mapping
PUT products-v1
PUT products-v1/_doc/p1
POST products-v1/_search
POST _bulk
POST _analyze
POST _reindex
```

Local lab base URL:

```text
http://localhost:9200
```

Kibana:

```text
http://localhost:5601
```