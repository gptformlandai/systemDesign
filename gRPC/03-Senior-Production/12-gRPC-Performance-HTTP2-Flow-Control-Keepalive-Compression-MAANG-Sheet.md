# 12. Performance: HTTP/2, Flow Control, Keepalive, Compression

## Goal

Tune gRPC performance by understanding where latency and resource cost come from.

```text
serialization + connection setup + HTTP/2 stream + flow control + handler + dependency + response size
```

---

## Performance Advantages

gRPC can be efficient because it uses:

- compact protobuf encoding
- generated serializers
- HTTP/2 multiplexing
- long-lived connections
- streaming
- binary framing

But performance is not automatic. Bad deadlines, huge messages, slow consumers, connection churn, and proxy misconfiguration can erase the benefits.

---

## Key Levers

| Lever | Impact |
|---|---|
| message size | affects serialization, memory, network, and max-frame/message limits |
| connection reuse | avoids repeated handshakes and warmup |
| HTTP/2 multiplexing | many streams share a connection |
| flow-control windows | protect receivers but can throttle senders |
| compression | saves bandwidth but costs CPU |
| keepalive | detects broken connections, can conflict with proxies |
| deadline budget | bounds resource use and tail latency |

---

## Large Message Warning

Large protobuf messages are common incident sources.

Prefer:

- pagination
- streaming chunks
- field masks
- summarized responses
- object storage for large blobs with references in gRPC

Avoid using gRPC as a bulk blob transfer channel unless the system is designed and tested for it.

---

## Keepalive

Keepalive pings detect dead connections, but aggressive settings can trigger proxy or server enforcement.

Check:

- client keepalive interval
- server minimum permitted ping interval
- load balancer idle timeout
- Envoy/mesh HTTP/2 settings
- mobile or unreliable network behavior

---

## Performance Metrics

Track:

- request count by method/status
- latency percentiles by method
- deadline exceeded rate
- message size distribution
- active streams
- connection/subchannel state
- flow-control stalls if available
- server handler time vs dependency time
- CPU spent in serialization/compression

---

## Interview Sound Bite

gRPC performance comes from protobuf and HTTP/2, but senior tuning looks at payload size, connection reuse, multiplexing, flow control, compression CPU cost, keepalive/proxy compatibility, deadlines, and method-level latency/status metrics.