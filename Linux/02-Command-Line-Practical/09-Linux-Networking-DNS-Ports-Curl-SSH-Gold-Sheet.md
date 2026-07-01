# Linux Networking, DNS, Ports, curl, and SSH - Gold Sheet

> Track File #9 of 30 - Group 02: Command-Line Practical
> For: backend, cloud, and production debugging | Level: intermediate | Mode: network diagnosis

## 1. Core Idea

Linux networking debugging separates name resolution, routing, connection, TLS/application response, and firewall/security policy.

```text
DNS -> route -> local firewall -> remote firewall -> port listener -> protocol response
```

---

## 2. Command Map

| Question | Command |
|---|---|
| what IPs do I have? | `ip addr` |
| where will packets go? | `ip route` |
| does DNS resolve? | `dig`, `nslookup`, `getent hosts` |
| is port listening locally? | `ss -ltnp` |
| can I connect to TCP port? | `nc -vz host port` |
| what HTTP response do I get? | `curl -v` |
| can I reach host? | `ping` if ICMP allowed |
| where does path fail? | `traceroute` or `tracepath` |
| SSH debug | `ssh -v user@host` |

---

## 3. Daily Commands

```bash
ip addr
ip route
ss -ltnp
curl -v http://localhost:8080/health
curl -vk https://example.com
getent hosts example.com
dig example.com
ssh -v user@host
```

---

## 4. Production Debug Flow

```text
URL fails -> DNS -> IP route -> local listener -> firewall/security group -> TLS -> app response -> logs
```

Example:

```bash
getent hosts api.example.com
curl -v https://api.example.com/health
ss -ltnp | grep ':443'
journalctl -u nginx --since "30 minutes ago"
```

---

## 5. Failure Modes

- DNS resolves to old IP
- service listens on localhost but traffic arrives on public interface
- port open on host but blocked by cloud security group
- firewall permits TCP but app returns 500
- TLS certificate expired or hostname mismatch
- SSH key, user, or file permission issue

---

## 6. Interview Summary

```text
For Linux network issues, I split the problem into DNS resolution, routing, local listener, firewall/security policy, TLS, protocol response, and application logs. I use ip, ss, curl, dig/getent, nc, traceroute, and ssh -v to prove each layer.
```

---

## 7. Revision Notes

- One-line summary: Network debugging is layer-by-layer proof, not guessing.
- Three keywords: DNS, route, port.
- One trap: assuming ping failure means service failure, since ICMP may be blocked.