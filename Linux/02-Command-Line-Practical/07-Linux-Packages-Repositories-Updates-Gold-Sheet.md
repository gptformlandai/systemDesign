# Linux Packages, Repositories, and Updates - Gold Sheet

> Track File #7 of 30 - Group 02: Command-Line Practical
> For: server administration | Level: intermediate | Mode: package management

## 1. Core Idea

Package managers install, upgrade, verify, and remove software from trusted repositories.

```text
repository metadata -> package resolver -> install files -> service/config changes
```

---

## 2. Package Families

| Family | Common Distros | Tools |
|---|---|---|
| Debian/Ubuntu | Ubuntu, Debian | `apt`, `dpkg` |
| RHEL/Fedora | RHEL, CentOS, Fedora, Amazon Linux | `dnf`, `yum`, `rpm` |
| SUSE | SLES, openSUSE | `zypper`, `rpm` |
| Arch | Arch Linux | `pacman` |

---

## 3. Daily Commands

Debian/Ubuntu:

```bash
apt update
apt list --upgradable
apt install nginx
apt remove nginx
dpkg -L nginx
dpkg -S /usr/sbin/nginx
```

RHEL-style:

```bash
dnf check-update
dnf install nginx
dnf remove nginx
rpm -ql nginx
rpm -qf /usr/sbin/nginx
```

---

## 4. Production Update Rules

- know maintenance window and rollback plan
- snapshot or backup important hosts before risky upgrades
- review package changelog for breaking changes
- restart only affected services when possible
- test patching on non-production first
- track kernel updates that require reboot

---

## 5. Failure Modes

- repo misconfigured or unavailable
- dependency conflict
- package upgrade changes config format
- service not restarted after library update
- kernel updated but host not rebooted
- manual binary install conflicts with package manager

---

## 6. Interview Summary

```text
Linux package managers install software from repositories, resolve dependencies, and track which package owns which files. In production I patch with a maintenance plan, test first, watch service restarts and kernel reboot requirements, and avoid unmanaged manual installs unless there is a strong reason.
```

---

## 7. Revision Notes

- One-line summary: Package managers give repeatable, auditable software installation.
- Three keywords: repo, dependency, patch.
- One trap: upgrading production without checking service restart and rollback impact.