# Linux Users, Groups, Permissions, and Sudo - Gold Sheet

> Track File #3 of 30 - Group 01: Foundations
> For: daily Linux and production debugging | Level: beginner to intermediate | Mode: access control

## 1. Core Idea

Every Linux process runs as a user and group set. File access is decided by ownership, permission bits, ACLs, mount options, and sometimes security modules like SELinux or AppArmor.

```text
process user + file owner/group/mode + policy = allow or deny
```

---

## 2. Permission Bits

```text
-rw-r--r--  owner group file
drwxr-xr-x  owner group directory
```

| Bit | File Meaning | Directory Meaning |
|---|---|---|
| r | read file | list names |
| w | write file | create/delete entries |
| x | execute file | traverse directory |

---

## 3. Daily Commands

```bash
id                         # current user/group identity
whoami                     # current username
ls -l file                 # owner, group, mode
stat file                  # detailed ownership/mode/time info
chmod 640 file             # change mode
chown app:app file         # change owner/group
groups user                # user's groups
sudo -l                    # allowed sudo commands
getfacl file               # ACLs if installed
```

---

## 4. Sudo Mental Model

`sudo` does not mean "be careless as root." It means run a command with elevated privileges according to sudoers policy.

Safe approach:

```text
least privilege -> exact command -> verify target -> run -> audit/log
```

---

## 5. Production Failure Modes

- service cannot read config owned by root
- app cannot write logs because directory ownership is wrong
- executable script lacks execute bit
- directory has file read permission but no traverse permission
- sudo works manually but service user lacks access
- SELinux/AppArmor denies access despite normal permissions looking correct

---

## 6. Interview Summary

```text
For permission denied issues, I check the user running the process, the file owner/group/mode, directory execute permissions, ACLs, mount options, and security modules. I avoid broad chmod 777 fixes because they hide the real problem and create security risk.
```

---

## 7. Revision Notes

- One-line summary: Permission debugging starts with the process identity and every directory in the path.
- Three keywords: user, group, mode.
- One trap: fixing with `chmod 777` instead of identifying the exact missing permission.