# Linux Interview Answer Patterns

## Debugging Pattern

```text
I start by scoping impact, then gather evidence from the relevant Linux layer: process, service, logs, files, permissions, disk, memory, CPU, network, kernel messages, or security policy. I mitigate with the narrowest safe action, validate recovery, and add prevention.
```

## Command Explanation Pattern

```text
This command proves <state>. It does not prove <missing layer>. If the output is unhealthy, my next check is <next command> because <reason>.
```

## Production Maturity Pattern

```text
I avoid broad fixes like chmod 777, kill -9 first, or patching every host at once. I prefer least privilege, evidence, staged rollout, rollback, logs, alerts, and runbooks.
```