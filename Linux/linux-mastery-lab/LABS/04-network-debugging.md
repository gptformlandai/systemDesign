# Lab 04: Network Debugging

Goal: separate DNS, route, listener, firewall, TLS, and application behavior.

---

## Run

```bash
bash SCRIPTS/03-network-service-triage.sh example.com https://example.com
```

---

## Explain

- Did DNS resolve?
- Is there a route?
- What local ports are listening?
- Did HTTP/TLS respond?
- What layer would you check next if it failed?

---

## Completion Gate

- You can avoid mixing up network, TLS, and application errors.