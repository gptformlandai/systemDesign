#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X POST "$base_url/_analyze?pretty" \
  -H 'Content-Type: application/json' \
  -d '{"analyzer":"standard","text":"Water-resistant running shoes"}'

curl -sS -X POST "$base_url/products-v1/_analyze?pretty" \
  -H 'Content-Type: application/json' \
  -d '{"analyzer":"autocomplete_analyzer","text":"Mechanical keyboard"}'