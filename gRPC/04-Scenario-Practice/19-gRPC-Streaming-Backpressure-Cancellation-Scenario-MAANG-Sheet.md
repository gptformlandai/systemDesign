# 19. Scenario: Streaming Backpressure And Cancellation

## Incident

A server-streaming method `events.v1.EventService/WatchEvents` causes rising memory and delayed cancellation during peak traffic.

---

## Hypothesis

The server is producing events faster than clients can read, buffering without bounds, or ignoring cancellation after clients disconnect.

---

## Evidence To Collect

| Evidence | Why It Matters |
|---|---|
| active stream count | shows concurrency pressure |
| per-stream send latency | reveals slow consumers |
| server memory profile | identifies buffered events/messages |
| cancellation logs | proves whether handlers exit |
| flow-control metrics | shows sender throttling when available |
| client reconnect rate | suggests proxy timeouts or unstable streams |
| message size distribution | large events can exhaust buffers |

---

## Safe Mitigations

- cap per-client buffer size
- stop sending when cancellation is observed
- add max stream duration and reconnect guidance
- reduce message size or split payloads
- add server-side rate limiting
- shed slow consumers with clear status
- add resume tokens so reconnects do not lose progress

---

## Better API Contract

```proto
message WatchEventsRequest {
  string tenant_id = 1;
  string resume_token = 2;
  int32 max_events_per_second = 3;
}

message EventEnvelope {
  string event_id = 1;
  string resume_token = 2;
  Event event = 3;
}
```

The contract makes resume behavior and rate expectations explicit.

---

## Prevention

- stream load tests with slow consumers
- bounded queue review
- cancellation tests
- reconnect/resume tests
- active-stream and send-latency alerts
- proxy idle timeout documentation

---

## Interview Sound Bite

For streaming incidents, I check whether the server is bounded and cancellation-aware. I look for slow consumers, growing buffers, flow-control stalls, reconnect loops, message-size growth, and missing resume semantics.