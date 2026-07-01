# Runbook: Streaming Stall

## Symptoms

- stream stops delivering events
- memory grows
- clients reconnect repeatedly
- cancellation is delayed

## Checks

1. Active stream count.
2. Send latency per stream.
3. Buffer depth and memory profile.
4. Client read rate.
5. Flow-control signals if available.
6. Proxy idle timeout.
7. Cancellation logs.
8. Message size distribution.

## Mitigations

- cap buffers
- shed slow consumers
- reduce message size
- add max stream lifetime
- fix cancellation checks
- add resume tokens

## Prevention

Slow-consumer tests, bounded queues, reconnect protocol, and streaming SLOs.