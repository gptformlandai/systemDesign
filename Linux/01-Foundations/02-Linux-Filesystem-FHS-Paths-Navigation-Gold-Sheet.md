# Linux Filesystem, FHS, Paths, and Navigation - Gold Sheet

> Track File #2 of 30 - Group 01: Foundations
> For: beginners and daily Linux work | Level: beginner | Mode: filesystem navigation

## 1. Core Idea

Linux presents almost everything through a single filesystem tree starting at `/`.

```text
/ -> directories -> files, devices, mounts, sockets, pseudo-files
```

---

## 2. Important Directories

| Path | Purpose |
|---|---|
| `/` | root of the filesystem tree |
| `/home` | user home directories |
| `/root` | root user's home directory |
| `/etc` | system and service configuration |
| `/var` | variable data: logs, caches, spools |
| `/var/log` | many system/application logs |
| `/usr` | user-space programs and libraries |
| `/bin`, `/sbin` | essential binaries, often symlinked into `/usr` on modern systems |
| `/tmp` | temporary files, often cleaned automatically |
| `/proc` | process and kernel pseudo-files |
| `/sys` | device and kernel subsystem pseudo-files |
| `/dev` | device files |
| `/mnt`, `/media` | mount points |

---

## 3. Daily Commands

```bash
pwd                 # current directory
ls -lah             # list with hidden files and sizes
cd /var/log         # change directory
realpath file       # absolute resolved path
tree -L 2 .         # directory tree if installed
du -sh *            # directory sizes
df -h               # filesystem free space
mount               # mounted filesystems
find . -name '*.log' # find by name
```

---

## 4. Path Rules

| Concept | Example | Meaning |
|---|---|---|
| absolute path | `/etc/hosts` | starts at root |
| relative path | `logs/app.log` | starts from current directory |
| current directory | `.` | this directory |
| parent directory | `..` | one level up |
| home expansion | `~/.ssh` | current user's home |
| hidden file | `.bashrc` | leading dot hides from normal `ls` |

---

## 5. Production Failure Modes

- app points to the wrong config path
- file exists in container but not host, or host but not container
- mount missing after reboot
- log directory full
- inode exhaustion despite free disk space
- script uses relative path and fails under cron/systemd

---

## 6. Interview Summary

```text
Linux uses a single rooted filesystem hierarchy. I inspect paths with pwd, ls, realpath, find, df, du, and mount. In production I pay attention to config under /etc, logs under /var/log, pseudo-files under /proc and /sys, and mounted storage because missing mounts or wrong paths commonly break services.
```

---

## 7. Revision Notes

- One-line summary: Linux path debugging starts by proving where the file actually is.
- Three keywords: root, mount, path.
- One trap: using relative paths inside services or cron jobs without setting working directory.