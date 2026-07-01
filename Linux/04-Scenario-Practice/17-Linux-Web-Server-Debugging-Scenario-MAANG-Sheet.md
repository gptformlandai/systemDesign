# Linux Web Server Debugging Scenario - MAANG Sheet

> Track File #17 of 30 - Group 04: Scenario Practice
> For: backend/SRE interviews | Level: senior | Mode: web service incident

## 1. Scenario

```text
Users report that the website is down. The Linux host is reachable over SSH.
```

Goal: prove where the failure is: DNS, network, firewall, web server, upstream app, disk, permissions, or config.

---

## 2. Triage Flow

```text
impact -> DNS/IP -> port listener -> service state -> logs -> config -> upstream -> resource saturation
```

Commands:

```bash
curl -v http://localhost
ss -ltnp | grep ':80\|:443'
systemctl status nginx
journalctl -u nginx --since "30 minutes ago"
nginx -t
df -h
free -h
```

---

## 3. Common Causes

- service stopped or restart loop
- port not listening
- firewall/security group blocks traffic
- bad web server config
- certificate expired
- upstream app is down
- disk full prevents logs/cache/temp files
- permission denied on web root or certificate key

---

## 4. Mitigation

- rollback bad config
- restart or reload service after validation
- route to healthy upstream
- free disk or rotate logs
- fix ownership/permission narrowly
- restore certificate or key path

---

## 5. Interview Summary

```text
For a Linux web server outage, I check client impact, DNS, local curl, port listeners, firewall/security group, systemd service state, web server config test, logs, upstream health, disk, memory, and permissions. I mitigate with rollback, reload, routing change, or resource cleanup, then add monitoring and a runbook.
```

---

## 6. Revision Notes

- One-line summary: Web server debugging follows network, listener, service, logs, config, upstream, resources.
- Three keywords: port, service, upstream.
- One trap: restarting repeatedly before checking the first failure log.