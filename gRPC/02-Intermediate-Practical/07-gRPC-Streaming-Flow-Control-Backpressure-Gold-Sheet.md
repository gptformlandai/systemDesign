# 07. Streaming, Flow Control, Backpressure

## Goal

Operate gRPC streams without turning them into hidden memory, latency, or reliability incidents.

```text
stream messages -> HTTP/2 flow control -> receiver speed -> backpressure/cancellation -> bounded resources
```

---

## Streaming Mental Model

Each stream is a sequence of protobuf messages over an HTTP/2 stream. Messages are not free: they use memory, flow-control windows, CPU, serialization cost, and handler time.

---

## Flow Control

HTTP/2 flow control prevents senders from overwhelming receivers. It works at connection and stream levels.

Symptoms of flow-control pressure:

- sender writes slow down
- stream latency grows
- buffers increase
- memory rises
- downstream consumers lag
- cancellation is delayed

---

## Backpressure Strategy

| Strategy | Use When |
|---|---|
| bounded queues | producer can pause or drop safely |
| chunking | messages are large |
| pagination instead of streaming | clients need finite scans |
| resume tokens | long streams can reconnect |
| cancellation checks | callers may stop watching |
| rate limits | producers can overwhelm consumers |

---

## Server Streaming Design

```proto
rpc WatchShipment(WatchShipmentRequest) returns (stream ShipmentEvent);

message WatchShipmentRequest {
  string shipment_id = 1;
  string resume_token = 2;
}
```

Good stream APIs document:

- ordering guarantee
- resume behavior
- duplicate behavior
- heartbeat behavior
- max stream lifetime
- cancellation behavior
- event schema compatibility

---

## Bidi Streaming Design

Bidi streams need a protocol, not just messages.

Define:

- initial handshake
- allowed message order
- error states
- heartbeat/ping policy
- reconnect rules
- idempotency/deduplication
- close semantics

---

## Common Incidents

| Symptom | Likely Cause |
|---|---|
| stream stalls | receiver not reading, flow-control window exhausted |
| memory grows | unbounded buffering or slow consumer |
| clients reconnect constantly | keepalive/proxy timeout mismatch |
| duplicate events | reconnect without resume/dedupe design |
| cancellation ignored | server loop does not check context/cancellation |

---

## Interview Sound Bite

gRPC streaming is powerful but operationally expensive. I design streams with bounded buffers, cancellation, explicit protocol states, resume/dedupe behavior, flow-control awareness, and metrics for active streams, send latency, message size, and cancellation.