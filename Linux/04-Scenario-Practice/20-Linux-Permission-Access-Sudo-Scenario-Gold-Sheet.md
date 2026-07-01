# Linux Permission, Access, and Sudo Scenario - Gold Sheet

> Track File #20 of 30 - Group 04: Scenario Practice
> For: production access debugging | Level: intermediate | Mode: permissions

## 1. Scenario

```text
A service fails with permission denied after deployment.
```

The fix should be narrow and explainable.

---

## 2. Triage Commands

```bash
systemctl status my-service
systemctl show my-service --property=User,Group,WorkingDirectory
journalctl -u my-service -n 100 --no-pager
namei -l /path/to/file
ls -ld /path /path/to /path/to/file
stat /path/to/file
id appuser
sudo -l -U appuser
```

---

## 3. What To Check

- service user and group
- directory execute permission for every path segment
- file read/write/execute permission
- ownership after deployment
- ACLs
- mount options such as `noexec` or read-only
- SELinux/AppArmor denial
- sudoers rule if elevation is expected

---

## 4. Mitigation

- change ownership to correct service user/group
- add least-privilege group access
- fix directory traverse bit
- adjust service unit user or working directory
- fix SELinux label/policy if relevant
- avoid `chmod 777`

---

## 5. Interview Summary

```text
For permission denied, I check the process identity, file owner/group/mode, directory traverse permissions, ACLs, mount options, sudoers, and SELinux/AppArmor. I avoid broad permission changes and fix the minimum required access.
```

---

## 6. Revision Notes

- One-line summary: Permission fixes should be narrow, not magical.
- Three keywords: identity, mode, policy.
- One trap: fixing the file permission but forgetting parent directory execute permission.