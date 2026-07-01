#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X DELETE "$base_url/places-v1" >/dev/null || true

curl -sS -X PUT "$base_url/places-v1" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "place_id": { "type": "keyword" },
      "tenant_id": { "type": "keyword" },
      "name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "category": { "type": "keyword" },
      "location": { "type": "geo_point" },
      "rating": { "type": "double" },
      "open_now": { "type": "boolean" }
    }
  }
}
JSON

curl -sS -X POST "$base_url/_bulk?refresh=true" \
  -H 'Content-Type: application/x-ndjson' \
  --data-binary @- <<'NDJSON'
{ "index": { "_index": "places-v1", "_id": "pl1" } }
{ "place_id": "pl1", "tenant_id": "t1", "name": "River North Coffee", "category": "coffee", "location": { "lat": 40.7301, "lon": -73.9352 }, "rating": 4.6, "open_now": true }
{ "index": { "_index": "places-v1", "_id": "pl2" } }
{ "place_id": "pl2", "tenant_id": "t1", "name": "Downtown Running Store", "category": "retail", "location": { "lat": 40.7412, "lon": -73.9896 }, "rating": 4.4, "open_now": true }
{ "index": { "_index": "places-v1", "_id": "pl3" } }
{ "place_id": "pl3", "tenant_id": "t1", "name": "Night Owl Coffee", "category": "coffee", "location": { "lat": 40.7527, "lon": -73.9772 }, "rating": 4.2, "open_now": false }
NDJSON

curl -sS -X GET "$base_url/places-v1/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "query": {
    "bool": {
      "must": [{ "match": { "name": "coffee" } }],
      "filter": [
        { "term": { "tenant_id": "t1" } },
        { "term": { "open_now": true } },
        { "geo_distance": { "distance": "5km", "location": { "lat": 40.7306, "lon": -73.9352 } } }
      ]
    }
  },
  "sort": [
    { "_geo_distance": { "location": { "lat": 40.7306, "lon": -73.9352 }, "order": "asc", "unit": "km" } },
    { "rating": "desc" }
  ],
  "_source": ["place_id", "name", "category", "rating", "open_now"]
}
JSON