# Runbook: File Descriptor Limits

## Symptoms

- `Too many open files`
- service accepts fewer connections than expected
- logs mention `EMFILE` or `ENFILE`
- process cannot open sockets, logs, or config files

## Confirm

```bash
ulimit -n
systemctl show SERVICE --property=LimitNOFILE,LimitNPROC,TasksMax
cat /proc/PID/limits
ls /proc/PID/fd | wc -l
lsof -p PID | wc -l
sysctl fs.file-max
```

## Mitigate

- restart leaking process only after collecting evidence
- reduce traffic or connection churn
- raise service-specific `LimitNOFILE` if justified
- fix application connection/file leaks
- validate system-wide file limit capacity

## Prevent

- alert on open file descriptor usage
- load test connection limits
- configure systemd service limits explicitly
- review connection pooling and file handle cleanup
- document rollback for limit changes

## Interview Summary

```text
For too many open files, I compare shell limits, systemd service limits, process limits, current open descriptors, and system-wide file-max. The fix is not only raising limits; I also check for leaks and connection churn.
```