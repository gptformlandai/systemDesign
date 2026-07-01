# Linux File and Text Processing: find, grep, awk, sed - Gold Sheet

> Track File #5 of 30 - Group 02: Command-Line Practical
> For: daily debugging and log analysis | Level: intermediate | Mode: text tools

## 1. Core Idea

Linux text tools let you inspect large systems quickly.

```text
files/logs -> filter -> transform -> count/sort -> evidence
```

---

## 2. Tool Map

| Goal | Tool |
|---|---|
| locate files | `find`, `locate` |
| search text | `grep`, `rg` if installed |
| select columns | `awk`, `cut` |
| replace/transform stream | `sed` |
| sort/count | `sort`, `uniq`, `wc` |
| pass file lists safely | `xargs`, `find -exec` |
| inspect start/end | `head`, `tail` |

---

## 3. Daily Commands

```bash
find /var/log -type f -name '*.log'
grep -R "ERROR" /var/log/app
tail -f /var/log/syslog
awk '{print $1, $5}' access.log
sed 's/old/new/g' file.txt
sort access.log | uniq -c | sort -nr | head
find . -type f -name '*.tmp' -print0 | xargs -0 ls -lh
```

---

## 4. Production Patterns

Find biggest files:

```bash
du -ah /var/log | sort -h | tail -20
```

Count HTTP status codes:

```bash
awk '{print $9}' access.log | sort | uniq -c | sort -nr
```

Search recent service errors:

```bash
journalctl -u my-service --since "1 hour ago" | grep -i error
```

---

## 5. Failure Modes

- searching too broad and hammering disks
- breaking filenames with spaces by unsafe `xargs`
- editing in-place with `sed -i` before backing up
- missing compressed logs
- using regex that matches too much

---

## 6. Interview Summary

```text
For log and file analysis I combine find, grep, awk, sed, sort, uniq, wc, head, and tail. The goal is to turn noisy text into evidence: what failed, how often, when it started, which host/user/path is affected, and whether the pattern is growing.
```

---

## 7. Revision Notes

- One-line summary: Text tools turn logs into evidence.
- Three keywords: filter, transform, count.
- One trap: piping unsafe file names into commands without null delimiters.