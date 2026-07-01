# Runbook: gRPC-Web Or Gateway Issue

## Symptoms

- browser client cannot call gRPC service
- CORS or preflight failures
- gateway returns HTTP error instead of gRPC status
- streaming behavior differs from native gRPC

## Checks

1. Is the client using native gRPC, gRPC-Web, or JSON transcoding?
2. Does the gateway support the method and content type?
3. Are CORS rules correct?
4. Are auth headers/metadata translated safely?
5. Are deadlines/timeouts aligned?
6. Are status codes mapped correctly?
7. Is streaming supported in the selected gateway mode?

## Mitigations

- fix gateway route or transcoding config
- adjust CORS/auth header policy
- align timeouts
- document unsupported streaming behavior

## Prevention

Gateway contract tests, browser smoke tests, and explicit API mode documentation.