# Linux Interview Q&A: Beginner To Pro - MAANG Sheet

> Track File #24 of 30 - Group 05: Special Interview Rounds
> For: Linux interviews | Level: beginner to senior | Mode: direct Q&A

## 1. What is Linux?

Linux is an operating-system kernel commonly used with GNU/user-space tools and distributions. It manages CPU, memory, storage, devices, networking, filesystems, and processes.

## 2. Kernel vs shell?

The kernel manages hardware and resources. The shell is a user-space program that interprets commands and starts processes.

## 3. What is a process?

A running program with PID, parent PID, user/group identity, memory, open files, environment, and resource usage.

## 4. How do permissions work?

File access depends on process identity, file owner/group/mode, directory permissions, ACLs, mount options, and possibly SELinux/AppArmor.

## 5. How do you debug permission denied?

Check process user, `namei -l`, `ls -l`, `stat`, `id`, ACLs, sudoers, mount options, and SELinux/AppArmor denials.

## 6. How do you check a service?

Use `systemctl status`, `journalctl -u`, unit file inspection, process checks, port checks, config validation, and dependency checks.

## 7. How do you debug high CPU?

Use `top`, `ps`, `pidstat`, logs, recent deploys, thread dumps/profiling where relevant, and confirm whether CPU is user, system, or iowait related.

## 8. How do you debug memory pressure?

Use `free`, `vmstat`, process RSS, `/proc/meminfo`, OOM logs from `dmesg`, and application memory evidence.

## 9. How do you debug disk full?

Use `df -h`, `df -ih`, `du`, `lsof +L1`, log rotation checks, mounts, and cleanup or expansion plan.

## 10. How do you debug network connectivity?

Check DNS, routing, listener, firewall/security group, TLS, application response, and logs using `getent`, `ip route`, `ss`, `nc`, `curl -v`, and `journalctl`.

## 11. What is systemd?

systemd is a service and init manager that starts units, tracks lifecycle, manages dependencies, logs to journal, and controls boot targets.

## 12. What are cgroups and namespaces?

Namespaces isolate views of system resources. cgroups limit and account for resources like CPU and memory. Together they are core Linux container primitives.

## 13. How do you answer a Linux production incident question?

State symptom, scope impact, gather command evidence, identify OS layer, mitigate safely, validate recovery, and propose prevention.

## 14. What makes a senior Linux answer strong?

It explains not just the command, but what the output proves, the next check, the failure mode, the safe mitigation, and prevention.