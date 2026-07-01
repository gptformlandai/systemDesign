# Linux Active Recall Question Bank

> Track File #27 of 30 - Group 06: Practice Upgrade
> For: retrieval practice | Level: beginner to pro | Mode: active recall

Answer without notes first. Then check the relevant sheet.

---

## 1. Beginner Recall

1. What is Linux?
2. Kernel vs shell?
3. What is a process?
4. What is the root directory `/`?
5. Absolute path vs relative path?
6. What are `/etc`, `/var`, `/proc`, `/sys`, and `/home`?
7. What do `r`, `w`, and `x` mean for files?
8. What do `r`, `w`, and `x` mean for directories?
9. What is sudo?
10. What is PATH?

---

## 2. Practical Recall

1. How do you find a file by name?
2. How do you search logs for errors?
3. How do you find the biggest directories?
4. How do you list listening ports?
5. How do you check a systemd service?
6. How do you read service logs?
7. How do you check disk and inode usage?
8. How do you check DNS resolution?
9. How do you check current user and groups?
10. How do you prove which package owns a file?

---

## 3. Senior Recall

1. Explain the Linux boot flow.
2. Explain systemd unit lifecycle.
3. Explain load average and why it is not only CPU.
4. Explain OOM killer behavior.
5. Explain cgroups and namespaces.
6. Explain why containers share the host kernel.
7. Explain SELinux/AppArmor vs file permissions.
8. Explain cron vs systemd timers.
9. Explain how to debug a service that works manually but fails under systemd.
10. Explain how to write a safe production shell script.

---

## 4. Scenario Recall

1. Web server is down but SSH works. What do you check?
2. Host load is high. How do you find the bottleneck?
3. API cannot call another service. How do you debug Linux networking?
4. Service has permission denied. What is the safe debug path?
5. Container is OOMKilled. How do you prove why?
6. Disk is full but deleting files did not free space. What happened?
7. Patch caused boot failure. What do you check?
8. Cron job fails but manual command works. Why?
9. Service restart loop hides the original error. What do you do?
10. You need to patch a VM fleet. What is the rollout plan?

---

## 5. Scorecard

| Score | Meaning |
|---:|---|
| 0 | cannot answer without notes |
| 1 | can define only |
| 2 | can give command |
| 3 | can interpret output and name next check |
| 4 | can explain failure mode, mitigation, and prevention |

Target:

```text
Pro-ready = mostly 3s and 4s across filesystem, permissions, shell, processes, services, networking, storage, performance, containers, security, and incidents.
```