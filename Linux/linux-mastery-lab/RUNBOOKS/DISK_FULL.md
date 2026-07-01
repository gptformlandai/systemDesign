# Runbook: Disk Full

## Symptoms

- writes fail
- service cannot start
- logs stop writing
- database/application errors mention no space left

## Confirm

```bash
df -h
df -ih
du -ah /var | sort -h | tail -20
lsof +L1
findmnt
```

## Mitigate

- rotate/compress logs
- clean safe cache/temp directories
- restart process holding deleted files if safe
- expand volume/filesystem
- route traffic away if writes are failing

## Prevent

- disk and inode alerts
- log rotation
- retention policy
- separate data and root volumes
- capacity forecast