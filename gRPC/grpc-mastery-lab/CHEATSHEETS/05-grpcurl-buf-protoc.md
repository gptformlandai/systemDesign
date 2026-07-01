# grpcurl, Buf, protoc Cheatsheet

## grpcurl

```bash
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext localhost:50051 describe package.v1.Service
grpcurl -plaintext -d '{}' localhost:50051 package.v1.Service/Method
```

## With Metadata

```bash
grpcurl -plaintext \
  -H 'authorization: Bearer token' \
  -H 'x-request-id: local-1' \
  -d '{}' \
  localhost:50051 package.v1.Service/Method
```

## Buf

```bash
buf lint
buf breaking --against '.git#branch=main'
buf generate
```

## protoc

```bash
protoc --proto_path=. --go_out=. --go-grpc_out=. proto/service.proto
```