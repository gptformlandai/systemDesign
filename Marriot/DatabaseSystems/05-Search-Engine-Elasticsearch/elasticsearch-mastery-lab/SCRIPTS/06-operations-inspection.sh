#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS "$base_url/_cluster/health?pretty"
curl -sS "$base_url/_cat/indices?v&s=index"
curl -sS "$base_url/_cat/shards?v&s=index,shard"
curl -sS "$base_url/_nodes/stats/jvm,fs,indices,thread_pool?pretty"