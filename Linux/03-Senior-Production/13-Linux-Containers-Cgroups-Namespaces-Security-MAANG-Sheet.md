# Linux Containers, cgroups, Namespaces, and Security - MAANG Sheet

> Track File #13 of 30 - Group 03: Senior Production
> For: Docker/Kubernetes/Linux interviews | Level: senior | Mode: container internals

## 1. Core Idea

Containers are Linux processes with isolation and resource controls.

```text
container = process + namespaces + cgroups + filesystem layers + security policies
```

They are not lightweight virtual machines in the strict sense; they share the host kernel.

---

## 2. Building Blocks

| Feature | Purpose |
|---|---|
| PID namespace | process ID isolation |
| network namespace | interfaces, routes, ports isolation |
| mount namespace | filesystem view isolation |
| user namespace | UID/GID mapping |
| cgroups | CPU, memory, I/O, process limits |
| capabilities | split root privileges into smaller rights |
| seccomp | restrict system calls |
| AppArmor/SELinux | mandatory access control policies |

---

## 3. Commands And Evidence

```bash
ps -ef
lsns
cat /proc/1/cgroup
cat /proc/self/status | grep Cap
mount | head
ip netns list
docker inspect container_name
docker stats
```

Kubernetes angle:

```text
pod resource limits -> container cgroups -> kernel enforcement -> throttling/OOM behavior
```

---

## 4. Production Failure Modes

- container OOM killed due to memory limit
- CPU throttling despite host appearing idle
- process runs as root inside container with too many capabilities
- bind mount hides expected files
- host path permissions differ from container user
- container DNS/network policy blocks access
- log file grows on host because container writes too much

---

## 5. Debug Path

```text
app symptom -> container logs -> process state -> resource limits -> mounts -> network namespace -> host kernel logs
```

Questions:

- Is the problem inside the app process or host-level resource enforcement?
- Did cgroups kill or throttle the process?
- Are mount and user permissions correct?
- Are capabilities or security policies denying behavior?

---

## 6. Interview Summary

```text
Linux containers are regular processes isolated by namespaces and limited by cgroups, with filesystem layers and security controls. For container incidents I check logs, process state, resource limits, cgroup OOM/throttling, mounts, network namespace, permissions, and host kernel messages.
```

---

## 7. Revision Notes

- One-line summary: Containers are isolated Linux processes sharing the host kernel.
- Three keywords: namespace, cgroup, capability.
- One trap: debugging only inside the container while the failure is caused by host cgroup limits.