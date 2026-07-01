# Lab 05: Permission Debugging

Goal: diagnose access problems without broad permission changes.

---

## Run

```bash
bash SCRIPTS/04-disk-permission-triage.sh .
```

---

## Explain

- Which user are you?
- Who owns the path?
- Which directory permissions matter?
- Is the file readable, writable, or executable?
- What narrow fix would be safe?

---

## Completion Gate

- You can explain why `chmod 777` is not a professional fix.