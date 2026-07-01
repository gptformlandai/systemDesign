# Linux systemd, Services, Logs, and journalctl - Gold Sheet

> Track File #8 of 30 - Group 02: Command-Line Practical
> For: server operations | Level: intermediate | Mode: services and logs

## 1. Core Idea

On many modern Linux distributions, systemd manages services, dependencies, startup, shutdown, restarts, timers, targets, and logs.

```text
unit file -> systemd -> process lifecycle -> journal logs -> service state
```

---

## 2. Daily Commands

```bash
systemctl status nginx
systemctl start nginx
systemctl stop nginx
systemctl restart nginx
systemctl reload nginx
systemctl enable nginx
systemctl disable nginx
systemctl list-units --type=service
journalctl -u nginx --since "1 hour ago"
journalctl -xe
```

---

## 3. Unit File Basics

Common locations:

```text
/etc/systemd/system/       local/admin unit files
/usr/lib/systemd/system/   packaged unit files on some distros
/lib/systemd/system/       packaged unit files on Debian-style systems
```

Important fields:

| Field | Meaning |
|---|---|
| `ExecStart` | command to start service |
| `User` | user running the process |
| `WorkingDirectory` | working directory |
| `Environment` | environment variables |
| `Restart` | restart policy |
| `After` / `Requires` | ordering and dependency hints |

---

## 4. Production Debug Flow

```text
systemctl status -> journalctl logs -> unit file -> process -> port -> config -> dependency
```

Commands:

```bash
systemctl cat my-service
systemctl show my-service --property=User,ExecStart,Restart
journalctl -u my-service -n 100 --no-pager
ss -ltnp
```

---

## 5. Failure Modes

- service starts manually but fails under systemd because PATH or working directory differs
- wrong service user lacks file permission
- restart loop hides the first error
- unit file changed but `systemctl daemon-reload` was not run
- service enabled state confused with running state

---

## 6. Interview Summary

```text
For service issues, I check systemctl status, journalctl logs, unit configuration, service user, working directory, environment, restart policy, process state, listening port, and dependencies. systemd is both a service manager and a strong source of operational evidence.
```

---

## 7. Revision Notes

- One-line summary: systemd explains why a service is or is not running.
- Three keywords: unit, journal, lifecycle.
- One trap: changing a unit file without running `systemctl daemon-reload`.