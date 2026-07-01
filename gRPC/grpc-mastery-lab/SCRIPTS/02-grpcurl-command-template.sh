#!/usr/bin/env bash
set -euo pipefail

host="${1:-localhost:50051}"
service="${2:-greeter.v1.GreeterService}"
method="${3:-SayHello}"

cat <<TEMPLATE
# List services when reflection is enabled.
grpcurl -plaintext ${host} list

# Describe the target service.
grpcurl -plaintext ${host} describe ${service}

# Call ${service}/${method} with JSON payload.
grpcurl -plaintext \\
  -H 'x-request-id: local-test-1' \\
  -d '{"name":"Aravind","request_id":"local-test-1"}' \\
  ${host} ${service}/${method}

# If reflection is disabled, add -proto path/to/file.proto.
TEMPLATE