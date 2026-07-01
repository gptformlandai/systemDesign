# Greeter grpcurl Commands

These commands assume a local server at `localhost:50051`.

## Reflection Mode

```bash
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext localhost:50051 describe greeter.v1.GreeterService
grpcurl -plaintext \
  -H 'x-request-id: local-001' \
  -d '{"name":"Aravind","request_id":"local-001"}' \
  localhost:50051 greeter.v1.GreeterService/SayHello
```

## Proto File Mode

```bash
grpcurl -plaintext \
  -proto EXAMPLES/greeter/greeter.proto \
  -d '{"name":"Aravind","request_id":"local-001"}' \
  localhost:50051 greeter.v1.GreeterService/SayHello
```

## Streaming Mode

```bash
grpcurl -plaintext \
  -proto EXAMPLES/greeter/greeter.proto \
  -d '{"name":"Aravind","resume_token":""}' \
  localhost:50051 greeter.v1.GreeterService/WatchGreetings
```

## Failure Practice

| Change | Expected Learning |
|---|---|
| wrong method name | `UNIMPLEMENTED` or method lookup failure |
| wrong port | connectivity failure |
| TLS server with `-plaintext` | handshake/config failure |
| empty name | `INVALID_ARGUMENT` if handler validates correctly |