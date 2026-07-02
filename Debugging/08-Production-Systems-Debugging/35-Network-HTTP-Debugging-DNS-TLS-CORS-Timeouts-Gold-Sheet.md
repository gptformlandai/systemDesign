# 35. Network And HTTP Debugging: DNS, TLS, CORS, Timeouts, Connections

## Goal

Debug failures below application code: DNS, TCP, TLS, HTTP, proxies, load balancers, CORS, caching, and connection pool behavior.

---

## Layered Mental Model

```text
name resolution -> route -> TCP connect -> TLS handshake -> HTTP request -> app response
```

Every layer has a different failure shape.

---

## Symptom To Layer

| Symptom | Likely Layer |
|---|---|
| `ENOTFOUND`, `NXDOMAIN` | DNS |
| `connection refused` | port closed / service not listening |
| `connection timeout` | firewall, route, security group, blackhole |
| `TLS handshake failed` | certificate, protocol, SNI, trust store |
| `HTTP 401/403` | auth / policy |
| `HTTP 404` | routing/path/version |
| `HTTP 429` | rate limiting |
| `HTTP 502/503/504` | proxy/LB/upstream failure |
| CORS error in browser | browser policy, missing headers |
| works with curl, fails in browser | CORS, cookies, mixed content, cache |

---

## First Commands

```bash
# DNS
dig api.example.com
nslookup api.example.com

# TCP
nc -vz api.example.com 443
lsof -i :8080

# HTTP/TLS
curl -v https://api.example.com/health
curl -vk https://api.example.com/health
openssl s_client -connect api.example.com:443 -servername api.example.com

# Route
traceroute api.example.com
```

Use `curl -v` often. It shows DNS, connect, TLS, request headers, and response headers in one place.

---

## Timeout Taxonomy

| Timeout | Meaning |
|---|---|
| DNS timeout | resolver did not answer |
| connect timeout | TCP connection not established |
| TLS timeout | handshake stalled |
| read timeout | server accepted but did not respond in time |
| idle timeout | connection was quiet too long |
| upstream timeout | proxy gave up waiting on backend |
| client timeout | caller gave up before callee finished |

Always identify which timeout fired.

---

## HTTP 502 / 503 / 504

| Code | Meaning |
|---|---|
| 502 Bad Gateway | proxy got invalid/error response from upstream |
| 503 Service Unavailable | no healthy upstream or overloaded service |
| 504 Gateway Timeout | upstream did not respond before proxy timeout |

Debug:

```text
client -> CDN -> load balancer -> ingress/proxy -> service -> pod/app
```

Find which hop generated the status code by checking response headers and proxy logs.

---

## TLS Debugging

```bash
openssl s_client -connect api.example.com:443 -servername api.example.com
```

Check:

- certificate subject and SANs
- expiry date
- issuer chain
- SNI hostname
- TLS protocol version
- trust store
- mTLS client certificate

Common trap: cert is valid for `api.example.com` but caller uses `internal-api.example.com`.

---

## CORS Debugging

CORS is enforced by browsers, not by curl.

Browser preflight:

```text
OPTIONS /orders
Origin: https://app.example.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: authorization,content-type
```

Server must answer:

```text
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Methods: POST
Access-Control-Allow-Headers: authorization,content-type
Access-Control-Allow-Credentials: true
```

Common bugs:

- wildcard origin with credentials
- missing allowed header
- auth middleware rejects OPTIONS
- CDN caches CORS response for wrong origin

---

## Browser Network Panel

Use Chrome DevTools Network tab for:

- request URL and method
- status code
- request/response headers
- cookies
- preflight requests
- redirects
- timing waterfall
- cache hits
- payload size
- blocked/mixed-content errors

If a request never reaches the backend, the browser/proxy/CDN layer is likely involved.

---

## Connection Pool Debugging

Symptoms:

```text
latency high
CPU normal
threads waiting
downstream healthy
pool active=max
queue pending grows
```

Check:

- pool max size
- acquire timeout
- idle timeout
- connection leak
- long-running queries/requests
- per-host connection limit
- keep-alive reuse

Connection pool exhaustion often looks like downstream slowness but is local saturation.

---

## Packet Tools

Use carefully:

```bash
sudo tcpdump -i any host api.example.com and port 443
```

Look for:

- SYN sent but no SYN-ACK: network/firewall/routing
- SYN -> RST: port closed/refused
- TLS ClientHello then alert: TLS/cert/protocol
- repeated retransmits: packet loss/path issue

Wireshark is better for deep packet inspection; `tcpdump` is better for quick capture.

---

## Practical Question

> A service call fails with timeout in production. How do you debug whether it is DNS, network, TLS, proxy, or app?

---

## Strong Answer

I would identify the exact timeout first: DNS, connect, TLS, read, proxy, or client timeout. From the same network location as the caller, I would run `dig`, `nc`, and `curl -v` to verify name resolution, TCP connectivity, TLS handshake, and HTTP response. If curl reaches the service but the app fails, I would inspect headers, auth, proxy route, and connection pool metrics.

If the browser fails but curl works, I would check CORS, cookies, mixed content, and cache behavior in the Network panel. If the proxy returns 502/503/504, I would identify which hop generated it and inspect upstream health and timeout settings.

---

## Interview Sound Bite

Network debugging is layer isolation. DNS answers "can I find it?", TCP answers "can I connect?", TLS answers "can I trust it?", HTTP answers "can the app serve it?", and CORS answers "will the browser allow it?"
