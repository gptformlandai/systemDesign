# Runbook: Network Failure

## Symptoms

- connection timeout
- connection refused
- DNS resolution failure
- TLS error
- HTTP 5xx or 4xx from dependency

## Confirm

```bash
getent hosts HOST
ip route
nc -vz HOST PORT
curl -v URL
ss -ltnp
journalctl -u SERVICE --since "30 minutes ago"
```

## Mitigate

- fix DNS or endpoint
- open specific firewall/security group rule
- restart listener after config validation
- fix TLS certificate/SNI/proxy settings
- route to healthy dependency

## Prevent

- synthetic checks
- dependency health alerts
- DNS/change review
- firewall rule ownership