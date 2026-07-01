# Lab 03: Resource Saturation

Goal: identify CPU, memory, disk, inode, or kernel pressure.

---

## Run

```bash
bash SCRIPTS/02-process-resource-triage.sh
bash SCRIPTS/04-disk-permission-triage.sh .
```

---

## Explain

- Is CPU saturated?
- Is memory low?
- Is OOM evidence present?
- Are disk bytes or inodes low?
- Which process or directory is the top suspect?

---

## Completion Gate

- You can distinguish CPU, memory, disk, inode, and I/O symptoms.