# gRPC Debugging Prompts

## Prompt 1

Clients see `UNAVAILABLE` after a deploy. Walk through evidence collection.

## Prompt 2

`DEADLINE_EXCEEDED` spikes for one method. Prove where time is spent.

## Prompt 3

A streaming API causes memory growth. Explain your backpressure investigation.

## Prompt 4

Old clients get wrong values with `OK`. Explain why this can happen.

## Prompt 5

After cert rotation, one zone fails mTLS. What do you check?

## Answer Shape

```text
symptom -> method -> caller/server scope -> status/latency evidence -> trace/log/metric/proxy evidence -> recent change -> mitigation -> prevention
```