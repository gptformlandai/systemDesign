# Lab 06: Container Host Debugging

Goal: reason about container problems from the Linux host perspective.

---

## Drill

If Docker is available, inspect a container:

```bash
docker ps -a
docker stats --no-stream
docker inspect CONTAINER
docker logs CONTAINER --tail 100
```

Host checks:

```bash
dmesg -T | grep -i -E 'oom|killed process'
df -h
ss -ltnp
```

---

## Explain

- Did the app exit or did the kernel/cgroup kill it?
- Are CPU/memory limits involved?
- Are mounts and permissions correct?
- Is host disk pressure involved?

---

## Completion Gate

- You can explain why containers are Linux processes with host-kernel constraints.