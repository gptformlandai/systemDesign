# Linux Boot, Init, Kernel, and systemd - MAANG Sheet

> Track File #11 of 30 - Group 03: Senior Production
> For: senior Linux and SRE interviews | Level: senior | Mode: boot and init flow

## 1. Core Boot Flow

```text
firmware -> bootloader -> kernel -> initramfs -> root filesystem -> systemd -> targets/services
```

Boot problems are production-critical because the host may be unreachable, partially initialized, or running only in emergency mode.

---

## 2. Components

| Component | Role |
|---|---|
| firmware | initializes hardware and loads bootloader |
| bootloader | selects and loads kernel/initramfs |
| kernel | initializes core OS and hardware drivers |
| initramfs | temporary root used to mount real root filesystem |
| systemd | first user-space process on many distros, PID 1 |
| target | systemd boot state such as multi-user or graphical |

---

## 3. Commands

```bash
uname -a
systemctl get-default
systemctl list-units --failed
journalctl -b
journalctl -b -1
dmesg -T | tail -100
systemd-analyze
systemd-analyze blame
```

---

## 4. Production Boot Failures

- bad `/etc/fstab` blocks boot
- kernel update requires reboot but new kernel fails
- root filesystem missing or read-only
- service dependency chain delays startup
- disk UUID changed or mount missing
- initramfs lacks driver for storage/network boot

---

## 5. Debug Path

```text
console access -> boot logs -> failed units -> fstab/mounts -> kernel logs -> recent patch/change -> rollback or rescue
```

Safe checks:

```bash
journalctl -xb
systemctl --failed
findmnt --verify
cat /etc/fstab
```

---

## 6. Interview Summary

```text
I explain Linux boot as firmware loading a bootloader, bootloader loading kernel and initramfs, kernel mounting the root filesystem, and systemd starting targets and services. For boot failures I check console logs, journalctl -b, failed units, fstab, mounts, kernel messages, and the most recent kernel/package/config change.
```

---

## 7. Revision Notes

- One-line summary: Boot debugging follows the chain from bootloader to systemd services.
- Three keywords: kernel, initramfs, systemd.
- One trap: editing `/etc/fstab` without verifying it before reboot.