#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS "$base_url/_cluster/health?pretty"
curl -sS "$base_url/_cat/nodes?v"
curl -sS "$base_url/_cat/indices?v"