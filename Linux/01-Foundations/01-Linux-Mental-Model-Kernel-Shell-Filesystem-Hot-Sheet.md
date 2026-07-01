# Linux Mental Model: Kernel, Shell, Processes, and Filesystem - Hot Sheet

> Track File #1 of 30 - Group 01: Foundations
> For: beginners and interview prep | Level: beginner | Mode: mental model

## 1. Core Idea

Linux is an operating system made of a kernel plus user-space programs.

```text
user command -> shell -> program/process -> system call -> kernel -> hardware/resource
```

The shell is not Linux itself. The shell is the command interpreter you use to ask Linux to run programs, read files, start processes, connect to networks, and inspect system state.

---

## 2. Main Building Blocks

| Block | Meaning |
|---|---|
| kernel | controls CPU, memory, devices, filesystems, networking, process scheduling |
| user space | programs outside the kernel: shells, editors, services, package tools |
| shell | command interpreter such as bash or zsh |
| process | a running program with PID, memory, files, environment, permissions |
| filesystem | hierarchical namespace where files, devices, pseudo-files, and mounts appear |
| system call | controlled request from a program into the kernel |

---

## 3. Commands That Reveal The Model

```bash
uname -a        # kernel and OS info
id              # current user and groups
pwd             # current directory
ls -la          # directory contents and permissions
ps -ef          # running processes
df -h           # mounted filesystems and space
ip addr         # network interfaces
env             # environment variables
```

---

## 4. Production Significance

Most Linux incidents are one of these:

- process not running
- wrong user or permission
- config file missing or wrong
- disk full or inode exhaustion
- network path broken
- service failed under systemd
- resource saturation: CPU, memory, disk I/O, network

Strong debugging starts by identifying the layer.

---

## 5. Interview Summary

```text
Linux can be understood as a kernel managing hardware resources and user-space programs interacting with it through system calls. The shell lets us start programs and inspect state. In production, I debug by locating the failing layer: process, file, permission, disk, network, service manager, kernel resource, or application.
```

---

## 6. Revision Notes

- One-line summary: Linux commands are evidence about kernel-managed resources.
- Three keywords: kernel, process, filesystem.
- One trap: treating shell aliases or shell syntax as if they are kernel behavior.