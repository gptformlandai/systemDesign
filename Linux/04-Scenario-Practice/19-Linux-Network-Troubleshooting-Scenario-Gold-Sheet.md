# Linux Network Troubleshooting Scenario - Gold Sheet

> Track File #19 of 30 - Group 04: Scenario Practice
> For: backend/cloud interviews | Level: intermediate to senior | Mode: connectivity debugging

## 1. Scenario

```text
Service A on a Linux host cannot call Service B.
```

Debug by proving each layer.

---

## 2. Triage Flow

```text
DNS -> route -> local firewall -> remote firewall -> port listener -> TLS -> application response
```

Commands:

```bash
getent hosts service-b.example.com
ip route
curl -v https://service-b.example.com/health
nc -vz service-b.example.com 443
ss -ltnp
traceroute service-b.example.com
journalctl -u app --since "30 minutes ago"
```

---

## 3. Common Causes

- DNS returns wrong/stale IP
- route table points traffic incorrectly
- local firewall blocks egress
- cloud security group/NACL blocks traffic
- Service B not listening on expected port
- TLS certificate or SNI issue
- proxy environment variables misconfigured
- app-level 401/403/500 looks like network failure

---

## 4. Mitigation

- fix DNS record or cache issue
- open specific firewall/security group rule
- restart listener after config validation
- correct proxy/TLS settings
- route traffic to healthy endpoint
- add synthetic health check

---

## 5. Interview Summary

```text
I debug Linux network failures by separating DNS, routing, firewall/security policy, port listener, TLS, and application response. curl -v, getent/dig, ip route, ss, nc, traceroute, and logs help prove each layer.
```

---

## 6. Revision Notes

- One-line summary: Network debugging is layered proof.
- Three keywords: DNS, route, listener.
- One trap: calling every HTTP 500 a network problem.