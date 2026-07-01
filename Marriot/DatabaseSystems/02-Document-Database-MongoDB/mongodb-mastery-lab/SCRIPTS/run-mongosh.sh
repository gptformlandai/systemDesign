#!/usr/bin/env bash
set -euo pipefail

URI="${MONGODB_URI:-mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true}"

if [[ $# -eq 0 ]]; then
  mongosh "$URI"
else
  mongosh "$URI" "$@"
fi
