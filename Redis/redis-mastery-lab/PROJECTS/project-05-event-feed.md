# Project 05: Real-Time Event Feed Using Streams

## Objective

Build a durable event bus where multiple services consume independently using Redis Streams consumer groups.

## Requirements

- Producer service: XADD to `events:{domain}` stream with MAXLEN cap
- Three consumer groups: notifications, analytics, audit
- Each group: XREADGROUP > with XACK on success
- Dead letter: XAUTOCLAIM entries idle > 60 seconds, move to `events:{domain}:dlq` after 3 retries
- Monitoring: XINFO GROUPS to track per-group lag and PEL count

## Key Redis Patterns Used

- Stream: `XADD`, `XREADGROUP`, `XACK`, `XAUTOCLAIM`, `XINFO GROUPS`, `XTRIM`
- Key patterns: `events:{domain}`, `events:{domain}:dlq`
- Consumer groups: notifications, analytics, audit

## Implementation Notes

Use `MAXLEN ~ 100000` in XADD to trim stream without blocking.

Each consumer group worker loop: XREADGROUP GROUP {group} {worker-id} COUNT 10 BLOCK 2000 STREAMS events:{domain} >

On successful processing: XACK immediately.

Dead-letter flow: XPENDING entry with delivery count > 3 -> XADD to DLQ -> XACK to remove from PEL.

## Test Scenarios

1. Publish 10 events. Verify XLEN is 10.
2. notifications group reads all 10. Verify PEL count = 10 before ACK.
3. ACK all 10. Verify PEL count = 0.
4. analytics group reads independently. Verify it also sees all 10.
5. Simulate worker crash: leave entries in PEL for 61 seconds. XAUTOCLAIM should reclaim them.
6. Verify stream trims when MAXLEN is exceeded.

## Interview Value

Demonstrates: Streams as durable event bus, consumer group independence, at-least-once delivery, PEL management, dead-letter handling.
