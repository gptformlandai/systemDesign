# Lab 10: Observability And SLO Drill

## Task

Design observability for `orders.v1.OrderService/CreateOrder`.

Include:

- request count by method/status
- latency percentiles
- deadline exceeded rate
- trace spans for client, server, and dependencies
- safe logs with request id
- SLO statement
- alert for error budget burn

## SLO Template

```text
99.9% of CreateOrder RPCs return a non-INTERNAL status within 500 ms over 30 days.
```

## Done When

You can explain what dashboard proves whether the RPC is healthy.