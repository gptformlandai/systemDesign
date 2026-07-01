# Runbook: Patching And Reboot

## Symptoms

- kernel vulnerability needs patch
- packages outdated
- reboot required
- host failed after patch

## Confirm

```bash
uname -a
uptime
apt list --upgradable 2>/dev/null || true
dnf check-update || true
systemctl --failed
last reboot
```

## Mitigate

- patch staging first
- snapshot or backup critical hosts
- canary a small subset
- reboot gradually
- validate services after each wave
- rollback or replace bad host if needed

## Prevent

- patch calendar
- automated compliance report
- reboot runbook
- canary process
- service startup validation