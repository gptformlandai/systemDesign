# Linux Commands Cheat Sheet and Decision Map

> Track File #25 of 30 - Group 05: Special Interview Rounds
> For: daily recall | Level: beginner to pro | Mode: command map

## 1. System Identity

```bash
uname -a
hostnamectl
uptime
id
whoami
```

## 2. Files And Text

```bash
pwd
ls -lah
find . -type f -name '*.log'
grep -R "ERROR" .
awk '{print $1}' file
sed 's/old/new/g' file
tail -f app.log
```

## 3. Processes

```bash
ps -ef
top
pgrep -af app
kill -TERM PID
lsof -p PID
```

## 4. Services And Logs

```bash
systemctl status service
systemctl restart service
systemctl cat service
journalctl -u service --since "1 hour ago"
journalctl -p err -b
```

## 5. Network

```bash
ip addr
ip route
ss -ltnp
getent hosts example.com
curl -v http://host:port/health
nc -vz host port
ssh -v user@host
```

## 6. Storage

```bash
df -h
df -ih
du -sh *
lsblk -f
findmnt
lsof +L1
```

## 7. Security And Access

```bash
ls -l file
stat file
namei -l /path/to/file
sudo -l
groups user
getenforce
aa-status
```

## 8. Decision Map

| Symptom | Start With |
|---|---|
| service down | `systemctl status`, `journalctl -u` |
| port unreachable | `ss`, `curl -v`, firewall/security group |
| high CPU | `top`, `ps`, `pidstat` |
| memory issue | `free`, `vmstat`, OOM logs |
| disk full | `df`, `du`, `lsof +L1` |
| permission denied | `id`, `namei -l`, `stat`, policy logs |
| command works manually but not cron/systemd | PATH, environment, working directory, user |