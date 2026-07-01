# Linux Mini Projects Portfolio

> Track File #29 of 30 - Group 06: Practice Upgrade
> For: hands-on proof and interview stories | Level: beginner to pro | Mode: projects

Each project should include commands used, expected output, failure modes, and a short incident-style write-up.

---

## 1. Linux Health Snapshot Script

Build a script that prints:

- OS/kernel
- uptime/load
- disk/inode usage
- memory usage
- top processes
- listening ports
- failed services

Discuss:

- what each signal means
- what thresholds deserve alerts
- how to avoid exposing secrets

---

## 2. Service Debugging Lab

Create a simple service or use an existing local service.

Practice:

- unit file inspection
- log inspection
- restart/reload behavior
- failed-start debugging
- environment and working directory issues

---

## 3. Log Triage Pipeline

Use shell tools to:

- search logs
- extract timestamps/status codes/errors
- count and sort patterns
- identify top noisy endpoints or errors
- generate a short summary

---

## 4. Network Diagnosis Playbook

Write a playbook for:

- DNS failure
- TCP port blocked
- service not listening
- TLS failure
- application 500

Include exact commands and expected evidence.

---

## 5. Secure Linux Baseline

Document a baseline with:

- least-privilege users
- sudo review
- SSH hardening
- firewall/port review
- patching plan
- audit/log review
- backup/restore check

---

## Portfolio Scoring

| Area | What To Prove |
|---|---|
| command fluency | commands are purposeful and safe |
| interpretation | output is explained clearly |
| production thinking | failure modes and mitigations are named |
| security | least privilege and audit are considered |
| automation | scripts are repeatable and non-destructive |
| interview value | project can be explained in 5-10 minutes |