# Runbook: Hot Node

## Symptoms

- one node has extremely high relationship degree
- traversals touching that node are slow
- writes contend around one entity
- query plans look acceptable until they expand from the hot node

## Confirm

1. Identify high-degree labels and relationship types.
2. Check whether hot nodes are natural, such as public IPs or celebrity users.
3. Inspect queries that expand through those nodes.
4. Check lock waits and transaction retries for write hot spots.

## Mitigate

- cap traversals through the node
- filter by relationship property such as time/source
- exclude low-value common signals
- temporarily isolate noisy paths

## Durable Fix

- split relationship types
- add bucket/intermediate nodes
- precompute summaries
- partition large tenants/domains
- redesign the model around query semantics

## Interview Summary

```text
A hot node is a graph-shape skew problem. The fix is usually relationship/model design, not only more hardware.
```