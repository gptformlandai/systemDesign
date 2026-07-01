# Linux Storage, Disks, Mounts, LVM, and Filesystems - Gold Sheet

> Track File #10 of 30 - Group 02: Command-Line Practical
> For: server and incident debugging | Level: intermediate | Mode: storage fundamentals

## 1. Core Idea

Linux storage appears as block devices, partitions, logical volumes, filesystems, and mount points.

```text
disk -> partition/LVM -> filesystem -> mount point -> files/directories
```

---

## 2. Command Map

| Question | Command |
|---|---|
| what disks exist? | `lsblk` |
| what filesystems are mounted? | `findmnt`, `mount` |
| how much disk is free? | `df -h` |
| what directories are large? | `du -sh *` |
| are inodes exhausted? | `df -ih` |
| what filesystem type? | `blkid`, `lsblk -f` |
| what files are open? | `lsof` |

---

## 3. Important Concepts

| Concept | Meaning |
|---|---|
| block device | disk-like device such as `/dev/sda`, `/dev/nvme0n1` |
| partition | subdivision of a disk |
| filesystem | format that stores files/directories, such as ext4 or xfs |
| mount point | directory where filesystem is attached |
| inode | metadata object for files; can run out before bytes run out |
| LVM | logical volume management layer for flexible volumes |
| fstab | boot-time mount configuration in `/etc/fstab` |

---

## 4. Production Debug Flow

```text
write failed -> df -h -> df -ih -> du large dirs -> lsof deleted files -> logs -> rotate/cleanup/expand
```

Commands:

```bash
df -h
df -ih
du -ah /var | sort -h | tail -20
findmnt
lsblk -f
lsof +L1
```

---

## 5. Failure Modes

- disk bytes full
- inode exhaustion
- deleted file still held open by running process
- mount missing after reboot due to bad `/etc/fstab`
- read-only filesystem after storage error
- application writes to root filesystem instead of mounted data disk

---

## 6. Interview Summary

```text
For Linux storage issues, I check block devices, mounts, disk space, inodes, large directories, open deleted files, filesystem state, and fstab. Disk-full incidents are often not just byte usage; inodes, log rotation, missing mounts, and deleted-but-open files matter too.
```

---

## 7. Revision Notes

- One-line summary: Storage debugging follows device, filesystem, mount, usage, and open-file evidence.
- Three keywords: mount, inode, filesystem.
- One trap: deleting a large log file but not restarting the process that still holds it open.