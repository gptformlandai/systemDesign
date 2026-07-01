# Runbook: Mapping Explosion

## Symptoms

- thousands of fields in one index
- cluster-state pressure
- high heap
- mapping limit errors
- slow index creation/update behavior

## Confirm

1. Inspect mapping field count.
2. Identify dynamic field sources.
3. Check log/event payload changes.
4. Check cluster-state and heap signals.

## Mitigate

- block or sanitize noisy source
- stop dynamic indexing for arbitrary fields
- use `flattened` for controlled arbitrary metadata
- isolate bad source into quarantine index

## Durable Fix

- explicit mappings
- dynamic templates
- schema registry or ingest validation
- source payload contracts

## Interview Summary

```text
Mapping explosion is usually a data-contract failure. The fix is controlled mappings and source validation, not only larger nodes.
```