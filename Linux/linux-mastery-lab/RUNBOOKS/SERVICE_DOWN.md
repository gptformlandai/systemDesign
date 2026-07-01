# Runbook: Service Down

## Symptoms

- health check fails
- port not listening
- service restart loop
- users see 5xx or timeout

## Confirm

```bash
systemctl status SERVICE
journalctl -u SERVICE -n 100 --no-pager
systemctl cat SERVICE
ss -ltnp
```

## Mitigate

- rollback bad config
- reload/restart after validation
- shift traffic away from bad host
- fix missing environment, path, user, or permission

## Prevent

- config validation in deployment
- service health checks
- restart-loop alerts
- runbook with known healthy output