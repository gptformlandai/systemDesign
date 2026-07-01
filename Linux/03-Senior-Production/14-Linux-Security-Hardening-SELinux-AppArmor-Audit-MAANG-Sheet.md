# Linux Security, Hardening, SELinux, AppArmor, and Audit - MAANG Sheet

> Track File #14 of 30 - Group 03: Senior Production
> For: production security interviews | Level: senior | Mode: hardening and policy

## 1. Core Idea

Linux security is layered.

```text
identity + permissions + sudo + service isolation + firewall + MAC policy + audit + patching
```

Strong Linux security reduces blast radius and improves traceability.

---

## 2. Security Layers

| Layer | Examples |
|---|---|
| identity | users, groups, service accounts |
| discretionary permissions | owner/group/mode, ACLs |
| privilege elevation | sudoers, least privilege |
| service isolation | systemd `User`, `NoNewPrivileges`, `PrivateTmp` |
| network filtering | firewalld, ufw, iptables/nftables, cloud security groups |
| mandatory access control | SELinux, AppArmor |
| audit | auditd, auth logs, sudo logs, journal |
| patching | package updates, kernel updates, vulnerability management |

---

## 3. Commands

```bash
id
sudo -l
ls -l /etc/sudoers /etc/sudoers.d
ss -ltnp
systemctl status firewalld
ufw status
getenforce        # SELinux if installed
sestatus          # SELinux if installed
aa-status         # AppArmor if installed
journalctl _COMM=sudo
last
lastb             # if available and permitted
```

---

## 4. Production Hardening Checklist

- disable password SSH where appropriate; use keys/certs
- restrict sudo to necessary commands/groups
- run services as non-root users
- keep packages and kernel patched
- close unused ports
- configure firewall and cloud security groups
- enable audit logging for sensitive systems
- back up and protect secrets/config files
- use SELinux/AppArmor policies where supported

---

## 5. Failure Modes

- overly broad sudo rules
- service running as root unnecessarily
- firewall open to the world
- secrets readable by all users
- SELinux/AppArmor denial misunderstood as normal permission issue
- patching delayed until a known vulnerability is exploited

---

## 6. Interview Summary

```text
I harden Linux by applying least privilege across users, files, sudo, services, network ports, MAC policies, audit logs, and patching. For access issues I check normal permissions first, then ACLs, sudoers, firewall, SELinux/AppArmor, and audit logs.
```

---

## 7. Revision Notes

- One-line summary: Linux security is layered least privilege plus auditability.
- Three keywords: sudo, firewall, MAC.
- One trap: disabling SELinux/AppArmor permanently instead of reading the denial and fixing policy or labeling.