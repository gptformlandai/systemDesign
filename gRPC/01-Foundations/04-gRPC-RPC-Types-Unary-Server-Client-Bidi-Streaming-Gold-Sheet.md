# 04. gRPC RPC Types: Unary, Server Streaming, Client Streaming, Bidi Streaming

## Goal

Know which RPC shape fits which communication pattern.

```text
request/response need -> RPC type -> deadline/flow-control/error model
```

---

## Four RPC Types

| Type | Proto Shape | Use Case |
|---|---|---|
| unary | `rpc Get(A) returns (B);` | normal request/response |
| server streaming | `rpc List(A) returns (stream B);` | one request, many responses |
| client streaming | `rpc Upload(stream A) returns (B);` | many request chunks, one response |
| bidirectional streaming | `rpc Chat(stream A) returns (stream B);` | both sides stream independently |

---

## Unary

```proto
rpc GetOrder(GetOrderRequest) returns (GetOrderResponse);
```

Best for direct reads, writes, commands, and small bounded responses.

Production checks:

- deadline is set by caller
- method is idempotent only when designed that way
- errors use canonical status codes
- retries are configured only for safe cases

---

## Server Streaming

```proto
rpc WatchOrder(WatchOrderRequest) returns (stream OrderEvent);
```

Best for event feeds, progress updates, large result sets, or watch-style APIs.

Production checks:

- client can cancel
- server handles slow consumers
- max stream duration is bounded
- flow-control metrics are visible
- reconnect/resume semantics are documented

---

## Client Streaming

```proto
rpc UploadReceipt(stream ReceiptChunk) returns (UploadReceiptResponse);
```

Best for uploads, batch ingestion, or chunked client data.

Production checks:

- chunk size has limits
- server validates cumulative size
- partial failure behavior is clear
- timeout covers the whole upload
- client handles final status after sending all chunks

---

## Bidirectional Streaming

```proto
rpc SyncInventory(stream InventoryClientEvent) returns (stream InventoryServerEvent);
```

Best for collaborative sessions, device streams, control channels, and continuous synchronization.

Production checks:

- protocol states are documented
- both sides can cancel safely
- heartbeats or keepalives are intentional
- backpressure strategy is explicit
- reconnect and dedupe are designed

---

## Decision Map

| Need | Choose |
|---|---|
| simple command or query | unary |
| one request produces many bounded items | server streaming |
| upload many chunks to one result | client streaming |
| ongoing conversation/control channel | bidirectional streaming |
| browser public API | consider REST, GraphQL, or gRPC-Web gateway |

---

## Interview Sound Bite

Unary is the default. Streaming is for data that is naturally incremental or long-lived, but it requires stronger handling for flow control, cancellation, deadlines, reconnection, and partial failure.