# Streaming Design Cheatsheet

## Design Questions

- Is streaming truly needed?
- What is the ordering guarantee?
- How does the client resume?
- Are duplicate events possible?
- What is the heartbeat behavior?
- What is the maximum stream lifetime?
- What happens when the client cancels?
- How are buffers bounded?
- What metrics reveal slow consumers?

## Metrics

- active streams
- messages sent per stream
- send latency
- cancellation count
- reconnect count
- buffer depth
- message size

## Common Failure

Slow consumers plus unbounded buffers create memory incidents.