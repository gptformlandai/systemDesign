# Lab 02: Service Debugging

Goal: inspect a Linux service through systemd and logs.

---

## Run

```bash
bash SCRIPTS/05-systemd-log-triage.sh ssh
```

Replace `ssh` with a service on your host.

---

## Explain

- Is the service loaded?
- Is it active?
- What user runs it?
- What command starts it?
- What do recent logs show?
- What would you change safely?

---

## Completion Gate

- You can debug service state, unit config, logs, and restart policy.