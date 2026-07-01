# Linux Safety Rules Cheatsheet

1. Prove the failing layer before changing state.
2. Prefer read-only diagnostic commands first.
3. Avoid `chmod 777`.
4. Avoid `kill -9` before collecting evidence.
5. Back up config before editing.
6. Validate config before reload/restart.
7. Use canaries for fleet changes.
8. Avoid destructive scripts without dry-run.
9. Record commands during incidents.
10. Validate recovery after mitigation.

Strong production phrase:

```text
I would gather evidence first, apply the narrowest safe mitigation, validate recovery, and then add prevention.
```