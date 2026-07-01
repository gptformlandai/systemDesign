# Linux Mastery Track - Beginner To Pro Index

This folder is a complete Linux mastery track for developers, backend engineers, DevOps/SRE engineers, cloud engineers, and system design interviews.

It teaches Linux as a production operating system, not just a command list.

```text
terminal command -> kernel/user-space concept -> filesystem/process/network behavior -> production debugging -> interview answer
```

Use this track if:

- You want beginner-to-pro Linux confidence for daily engineering work.
- You want production troubleshooting skill for servers, containers, cloud VMs, SSH, logs, networking, permissions, storage, and services.
- You want MAANG-level interview answers that connect Linux commands to operating-system concepts and real incident response.
- You want hands-on drills and runbooks instead of reading-only notes.

---

## 1. Learning Style: Beginner To Pro Loop

Every topic should be learned with this loop:

```text
concept -> command -> output interpretation -> failure mode -> fix -> production scenario -> interview explanation
```

Linux mastery is not memorizing flags. It is knowing what the command proves, what it does not prove, and what to check next.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Foundations` | Linux mental model, filesystem, users, permissions, shell, environment |
| 2 | `02-Command-Line-Practical` | files, text processing, processes, packages, services, networking, storage |
| 3 | `03-Senior-Production` | boot, systemd, performance, containers, security, observability, automation, advanced diagnostics |
| 4 | `04-Scenario-Practice` | web server, CPU/memory/disk, networking, permissions, containers, incidents, cloud VM scenarios |
| 5 | `05-Special-Interview-Rounds` | Q&A, command cheat sheets, anti-patterns, debugging traps |
| 6 | `06-Practice-Upgrade` | active recall, drills, mini projects, production readiness checklist |
| Lab | `linux-mastery-lab` | scripts, labs, projects, cheatsheets, interview prep, and runbooks |

---

## 3. Foundations Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Foundations/01-Linux-Mental-Model-Kernel-Shell-Filesystem-Hot-Sheet.md](01-Foundations/01-Linux-Mental-Model-Kernel-Shell-Filesystem-Hot-Sheet.md) | Linux mental model, kernel vs user space, shell, processes, filesystem |
| 2 | [01-Foundations/02-Linux-Filesystem-FHS-Paths-Navigation-Gold-Sheet.md](01-Foundations/02-Linux-Filesystem-FHS-Paths-Navigation-Gold-Sheet.md) | FHS, absolute/relative paths, `/etc`, `/var`, `/proc`, `/sys`, `/home` |
| 3 | [01-Foundations/03-Linux-Users-Groups-Permissions-Sudo-Gold-Sheet.md](01-Foundations/03-Linux-Users-Groups-Permissions-Sudo-Gold-Sheet.md) | users, groups, file modes, ownership, sudo, least privilege |
| 4 | [01-Foundations/04-Linux-Shell-Bash-Zsh-Environment-Gold-Sheet.md](01-Foundations/04-Linux-Shell-Bash-Zsh-Environment-Gold-Sheet.md) | shell expansion, pipes, redirects, variables, PATH, environment |

Foundation target:

- You can explain what Linux is and how shell commands interact with the operating system.
- You can navigate the filesystem and explain important directories.
- You can reason about permissions, users, groups, and sudo safely.

---

## 4. Command-Line Practical Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Command-Line-Practical/05-Linux-File-Text-Processing-Find-Grep-Awk-Sed-Gold-Sheet.md](02-Command-Line-Practical/05-Linux-File-Text-Processing-Find-Grep-Awk-Sed-Gold-Sheet.md) | file inspection, `find`, `grep`, `awk`, `sed`, `sort`, `uniq`, `xargs` |
| 6 | [02-Command-Line-Practical/06-Linux-Processes-Jobs-Signals-Gold-Sheet.md](02-Command-Line-Practical/06-Linux-Processes-Jobs-Signals-Gold-Sheet.md) | processes, jobs, signals, `ps`, `top`, `kill`, `nice`, `nohup` |
| 7 | [02-Command-Line-Practical/07-Linux-Packages-Repositories-Updates-Gold-Sheet.md](02-Command-Line-Practical/07-Linux-Packages-Repositories-Updates-Gold-Sheet.md) | apt/yum/dnf concepts, repos, updates, package ownership |
| 8 | [02-Command-Line-Practical/08-Linux-Systemd-Services-Logs-Journalctl-Gold-Sheet.md](02-Command-Line-Practical/08-Linux-Systemd-Services-Logs-Journalctl-Gold-Sheet.md) | `systemctl`, units, service lifecycle, `journalctl` |
| 9 | [02-Command-Line-Practical/09-Linux-Networking-DNS-Ports-Curl-SSH-Gold-Sheet.md](02-Command-Line-Practical/09-Linux-Networking-DNS-Ports-Curl-SSH-Gold-Sheet.md) | IP, DNS, ports, sockets, curl, ss, ping, traceroute, SSH checks |
| 10 | [02-Command-Line-Practical/10-Linux-Storage-Disks-Mounts-LVM-Filesystems-Gold-Sheet.md](02-Command-Line-Practical/10-Linux-Storage-Disks-Mounts-LVM-Filesystems-Gold-Sheet.md) | disks, partitions, mounts, filesystems, inode/disk usage, LVM basics |

Practical target:

- You can inspect files, processes, services, network state, packages, and storage with confidence.
- You can interpret command output and decide the next check.

---

## 5. Senior Production Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-Production/11-Linux-Boot-Init-Kernel-Systemd-MAANG-Sheet.md](03-Senior-Production/11-Linux-Boot-Init-Kernel-Systemd-MAANG-Sheet.md) | boot flow, kernel, initramfs, systemd targets, startup failures |
| 12 | [03-Senior-Production/12-Linux-Performance-CPU-Memory-IO-Troubleshooting-MAANG-Sheet.md](03-Senior-Production/12-Linux-Performance-CPU-Memory-IO-Troubleshooting-MAANG-Sheet.md) | CPU, memory, disk I/O, load average, OOM, saturation analysis |
| 13 | [03-Senior-Production/13-Linux-Containers-Cgroups-Namespaces-Security-MAANG-Sheet.md](03-Senior-Production/13-Linux-Containers-Cgroups-Namespaces-Security-MAANG-Sheet.md) | cgroups, namespaces, containers, resource limits, isolation |
| 14 | [03-Senior-Production/14-Linux-Security-Hardening-SELinux-AppArmor-Audit-MAANG-Sheet.md](03-Senior-Production/14-Linux-Security-Hardening-SELinux-AppArmor-Audit-MAANG-Sheet.md) | hardening, SELinux/AppArmor, audit logs, SSH security, firewall basics |
| 15 | [03-Senior-Production/15-Linux-Observability-Logs-Metrics-Tracing-Runbooks-Gold-Sheet.md](03-Senior-Production/15-Linux-Observability-Logs-Metrics-Tracing-Runbooks-Gold-Sheet.md) | logs, metrics, tracing mindset, on-call runbooks, incident evidence |
| 16 | [03-Senior-Production/16-Linux-Automation-Shell-Scripting-Cron-Ansible-Gold-Sheet.md](03-Senior-Production/16-Linux-Automation-Shell-Scripting-Cron-Ansible-Gold-Sheet.md) | shell scripting, cron/systemd timers, idempotency, automation safety |
| Gap Fill | [03-Senior-Production/17-Linux-Advanced-Diagnostics-Strace-Perf-Tcpdump-Ulimit-Sysctl-Gap-Fill-MAANG-Sheet.md](03-Senior-Production/17-Linux-Advanced-Diagnostics-Strace-Perf-Tcpdump-Ulimit-Sysctl-Gap-Fill-MAANG-Sheet.md) | `strace`, `lsof`, `tcpdump`, `perf`, `ulimit`, `sysctl`, core dumps, safe evidence gathering |

Senior target:

- You can troubleshoot production Linux under pressure.
- You can connect command output to kernel, process, storage, network, and security behavior.
- You can use advanced diagnostics safely when basic commands are inconclusive.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/17-Linux-Web-Server-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/17-Linux-Web-Server-Debugging-Scenario-MAANG-Sheet.md) | Nginx/Apache/app service debugging, ports, logs, firewall, upstreams |
| 18 | [04-Scenario-Practice/18-Linux-High-CPU-Memory-Disk-Scenario-Gold-Sheet.md](04-Scenario-Practice/18-Linux-High-CPU-Memory-Disk-Scenario-Gold-Sheet.md) | resource saturation incident triage |
| 19 | [04-Scenario-Practice/19-Linux-Network-Troubleshooting-Scenario-Gold-Sheet.md](04-Scenario-Practice/19-Linux-Network-Troubleshooting-Scenario-Gold-Sheet.md) | DNS, routing, ports, firewall, TLS, connectivity failures |
| 20 | [04-Scenario-Practice/20-Linux-Permission-Access-Sudo-Scenario-Gold-Sheet.md](04-Scenario-Practice/20-Linux-Permission-Access-Sudo-Scenario-Gold-Sheet.md) | permission denied, ownership, groups, ACLs, sudoers safety |
| 21 | [04-Scenario-Practice/21-Linux-Container-Host-Troubleshooting-Scenario-MAANG-Sheet.md](04-Scenario-Practice/21-Linux-Container-Host-Troubleshooting-Scenario-MAANG-Sheet.md) | host vs container debugging, cgroups, namespaces, logs, mounts |
| 22 | [04-Scenario-Practice/22-Linux-Production-Incident-OnCall-Scenario-MAANG-Sheet.md](04-Scenario-Practice/22-Linux-Production-Incident-OnCall-Scenario-MAANG-Sheet.md) | incident response, evidence gathering, mitigation, RCA |
| 23 | [04-Scenario-Practice/23-Linux-Cloud-VM-Bastion-Patching-Scenario-Gold-Sheet.md](04-Scenario-Practice/23-Linux-Cloud-VM-Bastion-Patching-Scenario-Gold-Sheet.md) | cloud VM operations, bastion access, patching, reboot planning |

Scenario target:

- You can diagnose realistic Linux production problems with a repeatable path.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/24-Linux-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md](05-Special-Interview-Rounds/24-Linux-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md) | Linux Q&A from beginner to senior/MAANG |
| 25 | [05-Special-Interview-Rounds/25-Linux-Commands-Cheat-Sheet-And-Decision-Map.md](05-Special-Interview-Rounds/25-Linux-Commands-Cheat-Sheet-And-Decision-Map.md) | command map by debugging goal |
| 26 | [05-Special-Interview-Rounds/26-Linux-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md](05-Special-Interview-Rounds/26-Linux-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md) | unsafe commands, wrong assumptions, production debugging traps |

Special-round target:

- You can answer Linux interviews and avoid dangerous production mistakes.

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 27 | [06-Practice-Upgrade/27-Linux-Active-Recall-Question-Bank.md](06-Practice-Upgrade/27-Linux-Active-Recall-Question-Bank.md) | recall prompts across beginner to pro topics |
| 28 | [06-Practice-Upgrade/28-Linux-Hands-On-Exercises-And-Command-Drills.md](06-Practice-Upgrade/28-Linux-Hands-On-Exercises-And-Command-Drills.md) | practical command drills |
| 29 | [06-Practice-Upgrade/29-Linux-Mini-Projects-Portfolio.md](06-Practice-Upgrade/29-Linux-Mini-Projects-Portfolio.md) | portfolio-ready Linux projects |
| 30 | [06-Practice-Upgrade/30-Linux-Pro-Gap-Fill-Production-Readiness-Checklist.md](06-Practice-Upgrade/30-Linux-Pro-Gap-Fill-Production-Readiness-Checklist.md) | senior readiness checklist and scoring rubric |

Practice target:

- You can use Linux daily, debug incidents, explain concepts, and build confidence through hands-on labs.

---

## 9. Linux Mastery Lab

Use the lab when you want practice instead of reading-only notes:

- [linux-mastery-lab/README.md](linux-mastery-lab/README.md)
- [linux-mastery-lab/LEARNING_PATH.md](linux-mastery-lab/LEARNING_PATH.md)

Lab target:

- You can run safe diagnostic scripts.
- You can practice scenario labs and runbooks.
- You can build mini projects that prove Linux production skill.

---

## 10. Interview Answer Pattern

For Linux debugging and interview answers, use this shape:

```text
1. Symptom:
   What exactly is failing and who is affected?

2. Scope:
   One host, one process, one user, one network path, one service, or all traffic?

3. Evidence:
   Which command proves the state?

4. Layer:
   Filesystem, process, CPU, memory, disk, network, service manager, security, or application?

5. Cause:
   What changed or saturated?

6. Mitigation:
   What safe action restores service?

7. Prevention:
   What monitoring, automation, limit, or runbook prevents recurrence?
```

---

## 11. Recommended Study Orders

### 2-Week Practical Path

1. Foundation files 1-4.
2. Command-line practical files 5-10.
3. Scenario files 17-23.
4. Cheat sheet, exercises, and interview Q&A.

### 4-Week Pro Path

1. Week 1: mental model, filesystem, shell, permissions, text tools.
2. Week 2: processes, services, packages, networking, storage.
3. Week 3: boot, performance, containers, security, observability, automation, advanced diagnostics.
4. Week 4: production scenarios, runbooks, mini projects, interview practice.

### Production On-Call Path

1. Learn the command map.
2. Practice high CPU/memory/disk/network/service incidents.
3. Use runbooks to gather evidence and mitigate safely.
4. Write RCA notes from every scenario.

---

## 12. Readiness Gate

You are Linux interview-ready when you can do all of this without notes:

- Explain kernel vs user space, shell, filesystem, process, signal, service, and network basics.
- Navigate and inspect files safely.
- Diagnose permissions, sudo, ownership, and environment issues.
- Use `grep`, `awk`, `sed`, `find`, `xargs`, pipes, and redirects with confidence.
- Diagnose CPU, memory, disk, network, service, and log issues.
- Explain systemd units, journal logs, package management, boot flow, and storage mounts.
- Explain containers through cgroups, namespaces, mounts, networking, and logs.
- Explain Linux security hardening, SSH hygiene, firewall basics, audit logs, and least privilege.
- Use advanced diagnostics like `strace`, `lsof`, `tcpdump`, `perf`, `ulimit`, `sysctl`, and `coredumpctl` safely and with clear scope.
- Handle production incidents with evidence, mitigation, and prevention.