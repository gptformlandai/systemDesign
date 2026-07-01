# Runbook: Permission Denied

## Symptoms

- service cannot read config
- app cannot write logs
- script cannot execute
- user cannot sudo expected command

## Confirm

```bash
id
systemctl show SERVICE --property=User,Group,WorkingDirectory
namei -l /path/to/file
stat /path/to/file
sudo -l
```

## Mitigate

- fix owner/group narrowly
- add directory traverse permission
- add service user to correct group
- fix sudoers rule through safe editor
- check SELinux/AppArmor denial before disabling policy

## Prevent

- deployment preserves ownership
- least-privilege service users
- permission checks in CI/CD
- audit sudo access regularly