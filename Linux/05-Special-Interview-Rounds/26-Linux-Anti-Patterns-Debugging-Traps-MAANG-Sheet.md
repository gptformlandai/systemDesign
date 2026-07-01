# Linux Anti-Patterns and Debugging Traps - MAANG Sheet

> Track File #26 of 30 - Group 05: Special Interview Rounds
> For: production maturity | Level: senior | Mode: traps and safer alternatives

## 1. Dangerous Anti-Patterns

| Anti-Pattern | Why It Is Bad | Safer Approach |
|---|---|---|
| `chmod 777` | hides real permission issue and weakens security | identify exact user/group/mode needed |
| `kill -9` first | loses graceful cleanup and evidence | try SIGTERM, inspect logs, then escalate |
| deleting logs blindly | may remove incident evidence | rotate/archive after confirming impact |
| patching all hosts at once | fleet-wide outage risk | canary, staged rollout, rollback plan |
| editing config without backup | no easy rollback | copy, validate, reload safely |
| assuming ping proves HTTP | ICMP differs from TCP/application | use curl/nc/ss and app logs |
| ignoring SELinux/AppArmor | policy denial misdiagnosed | read denial logs and fix policy/labels |
| running scripts as root by default | broad blast radius | least privilege and dry-run |

---

## 2. Debugging Traps

- averages hide per-host p99 problems
- service enabled does not mean service running
- disk bytes free does not mean inodes free
- file deleted does not mean disk freed if process holds it open
- container healthy status can hide app-level degradation
- manual shell PATH differs from cron/systemd PATH
- localhost binding prevents external access

---

## 3. Recovery Phrase For Interviews

```text
I would avoid making a broad change before proving the failing layer. I would gather evidence, apply the narrowest safe mitigation, validate recovery, and then implement prevention.
```

---

## 4. Interview Summary

```text
Senior Linux work is careful because commands can change production state quickly. I avoid broad permission changes, blind kills, unplanned patching, destructive scripts, and assumption-driven debugging. I prefer evidence, least privilege, staged change, rollback, and clear validation.
```

---

## 5. Revision Notes

- One-line summary: Production Linux maturity is knowing what not to run.
- Three keywords: evidence, narrow, reversible.
- One trap: using a powerful command because it is familiar, not because it is safe.