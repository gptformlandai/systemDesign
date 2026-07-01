# Linux Container Host Troubleshooting Scenario - MAANG Sheet

> Track File #21 of 30 - Group 04: Scenario Practice
> For: containers/Kubernetes interviews | Level: senior | Mode: host vs container boundary

## 1. Scenario

```text
A containerized app is restarting and sometimes gets OOMKilled.
```

The bug may be in the application, container config, cgroup limit, host resource pressure, or mounted files.

---

## 2. Triage Flow

```text
container status -> logs -> exit reason -> cgroup limits -> host kernel logs -> mounts/network -> app evidence
```

Commands:

```bash
docker ps -a
docker logs container_name --tail 100
docker inspect container_name
docker stats
dmesg -T | grep -i -E 'oom|killed process'
cat /proc/1/cgroup
df -h
ss -ltnp
```

---

## 3. Common Causes

- memory limit too low
- app memory leak
- CPU throttling from strict limit
- missing environment variable
- bind mount path missing or wrong ownership
- container cannot resolve DNS
- health check too aggressive
- log growth fills host disk

---

## 4. Mitigation

- raise limit or reduce memory usage
- rollback app build
- fix mount path or ownership
- adjust health check thresholds
- route traffic away from unhealthy instance
- clean logs safely and fix rotation

---

## 5. Interview Summary

```text
For container incidents, I distinguish app failure from host enforcement. I check container status, logs, exit code, cgroup limits, OOM/throttling evidence, mounts, environment, network, host disk, and kernel logs. Containers share the host kernel, so host state matters.
```

---

## 6. Revision Notes

- One-line summary: Container debugging crosses the app, cgroup, namespace, and host boundary.
- Three keywords: cgroup, OOM, mount.
- One trap: only checking application logs and missing kernel OOM evidence.