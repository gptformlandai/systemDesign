# Linux Mastery Lab Learning Path

Use this path after or alongside the main track.

---

## Stage 1: Basic Evidence

Read:

- `../01-Foundations/01-Linux-Mental-Model-Kernel-Shell-Filesystem-Hot-Sheet.md`
- `../01-Foundations/02-Linux-Filesystem-FHS-Paths-Navigation-Gold-Sheet.md`
- `../01-Foundations/03-Linux-Users-Groups-Permissions-Sudo-Gold-Sheet.md`
- `../01-Foundations/04-Linux-Shell-Bash-Zsh-Environment-Gold-Sheet.md`

Run:

```bash
bash SCRIPTS/01-system-snapshot.sh
```

Lab:

- [LABS/01-system-snapshot.md](LABS/01-system-snapshot.md)
- [CHEATSHEETS/COMMAND_MAP.md](CHEATSHEETS/COMMAND_MAP.md)

---

## Stage 2: Practical Debugging

Read:

- `../02-Command-Line-Practical/05-Linux-File-Text-Processing-Find-Grep-Awk-Sed-Gold-Sheet.md`
- `../02-Command-Line-Practical/06-Linux-Processes-Jobs-Signals-Gold-Sheet.md`
- `../02-Command-Line-Practical/08-Linux-Systemd-Services-Logs-Journalctl-Gold-Sheet.md`
- `../02-Command-Line-Practical/09-Linux-Networking-DNS-Ports-Curl-SSH-Gold-Sheet.md`
- `../02-Command-Line-Practical/10-Linux-Storage-Disks-Mounts-LVM-Filesystems-Gold-Sheet.md`

Labs:

- [LABS/02-service-debugging.md](LABS/02-service-debugging.md)
- [LABS/03-resource-saturation.md](LABS/03-resource-saturation.md)
- [LABS/04-network-debugging.md](LABS/04-network-debugging.md)
- [LABS/05-permission-debugging.md](LABS/05-permission-debugging.md)

---

## Stage 3: Senior Production

Read:

- `../03-Senior-Production/11-Linux-Boot-Init-Kernel-Systemd-MAANG-Sheet.md`
- `../03-Senior-Production/12-Linux-Performance-CPU-Memory-IO-Troubleshooting-MAANG-Sheet.md`
- `../03-Senior-Production/13-Linux-Containers-Cgroups-Namespaces-Security-MAANG-Sheet.md`
- `../03-Senior-Production/14-Linux-Security-Hardening-SELinux-AppArmor-Audit-MAANG-Sheet.md`
- `../03-Senior-Production/15-Linux-Observability-Logs-Metrics-Tracing-Runbooks-Gold-Sheet.md`
- `../03-Senior-Production/16-Linux-Automation-Shell-Scripting-Cron-Ansible-Gold-Sheet.md`
- `../03-Senior-Production/17-Linux-Advanced-Diagnostics-Strace-Perf-Tcpdump-Ulimit-Sysctl-Gap-Fill-MAANG-Sheet.md`

Labs:

- [LABS/06-container-host-debugging.md](LABS/06-container-host-debugging.md)
- [LABS/07-advanced-diagnostics.md](LABS/07-advanced-diagnostics.md)
- [RUNBOOKS/SERVICE_DOWN.md](RUNBOOKS/SERVICE_DOWN.md)
- [RUNBOOKS/HIGH_CPU_MEMORY.md](RUNBOOKS/HIGH_CPU_MEMORY.md)
- [RUNBOOKS/DISK_FULL.md](RUNBOOKS/DISK_FULL.md)
- [RUNBOOKS/FILE_DESCRIPTOR_LIMITS.md](RUNBOOKS/FILE_DESCRIPTOR_LIMITS.md)

---

## Stage 4: Portfolio And Interview

Projects:

- [PROJECTS/01-linux-health-snapshot-script.md](PROJECTS/01-linux-health-snapshot-script.md)
- [PROJECTS/02-service-debugging-playbook.md](PROJECTS/02-service-debugging-playbook.md)
- [PROJECTS/03-log-triage-pipeline.md](PROJECTS/03-log-triage-pipeline.md)
- [PROJECTS/04-network-diagnosis-playbook.md](PROJECTS/04-network-diagnosis-playbook.md)
- [PROJECTS/05-secure-linux-baseline.md](PROJECTS/05-secure-linux-baseline.md)

Interview prep:

- [INTERVIEW_PREP/QUESTIONS.md](INTERVIEW_PREP/QUESTIONS.md)
- [INTERVIEW_PREP/ANSWER_PATTERNS.md](INTERVIEW_PREP/ANSWER_PATTERNS.md)

Final gate:

- Explain what every command proves.
- Explain what every command does not prove.
- Name safe mitigation and prevention for each scenario.