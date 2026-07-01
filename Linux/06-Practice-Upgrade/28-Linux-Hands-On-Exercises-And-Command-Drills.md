# Linux Hands-On Exercises and Command Drills

> Track File #28 of 30 - Group 06: Practice Upgrade
> For: practical fluency | Level: beginner to pro | Mode: command drills

Use safe commands first. Avoid destructive changes unless you are in a disposable VM/container.

---

## Drill 1: System Snapshot

Collect:

```bash
uname -a
uptime
id
pwd
df -h
free -h
ss -ltnp
```

Explain what each command proves.

---

## Drill 2: File Search And Log Analysis

Tasks:

- find all `.log` files under a directory
- search for `ERROR`
- count top error messages
- show the last 50 lines of a log
- identify largest log files

---

## Drill 3: Permission Debugging

Given a path, run:

```bash
namei -l /path/to/file
stat /path/to/file
id
```

Explain which user needs which permission.

---

## Drill 4: Service Debugging

Pick any local service and inspect:

```bash
systemctl status service
systemctl cat service
journalctl -u service -n 50 --no-pager
```

Explain service user, command, logs, and restart policy.

---

## Drill 5: Network Debugging

Test:

```bash
getent hosts example.com
curl -v https://example.com
ip route
ss -ltnp
```

Separate DNS, route, TCP, TLS, and HTTP behavior.

---

## Drill 6: Disk And Inode Debugging

Run:

```bash
df -h
df -ih
du -sh *
findmnt
```

Explain filesystem, mount point, byte usage, and inode usage.

---

## Completion Gate

You complete these drills only when you can explain:

- what the command proves
- what it does not prove
- what the next check is
- what safe mitigation might follow