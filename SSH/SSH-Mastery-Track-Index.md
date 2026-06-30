# SSH Mastery Track — Index

> **Goal:** Production-ready SSH mastery in 1–2 hours.
> **Audience:** Developers using SSH daily — servers, Git, CI/CD, cloud.

---

## Folder Structure

```
SSH/
├── SSH-Mastery-Track-Index.md          ← You are here
├── 01-Foundations/
│   ├── 01-What-Is-SSH.md               S1 — Core intuition, analogy, mental model
│   ├── 02-How-SSH-Works.md             S2 — Client/server, encryption, handshake
│   └── 03-SSH-Keys.md                  S3 — Key pairs, PEM files, ~/.ssh layout
├── 02-Authentication/
│   └── 04-Authentication-Methods.md    S4 — Password vs Key vs Agent vs Cert
├── 03-Practical-Usage/
│   ├── 05-Basic-SSH-Commands.md        S5 — Essential daily commands
│   ├── 06-File-Transfer.md             S6 — SCP, SFTP, rsync
│   ├── 07-SSH-Config.md                S7 — ~/.ssh/config power usage
│   └── 08-SSH-Agent.md                 S8 — ssh-agent, ssh-add, passphrase mgmt
├── 04-Advanced/
│   ├── 09-Advanced-SSH-Tunneling.md    S9 — Local/Remote/Dynamic port forwarding
│   ├── 10-Real-World-Use-Cases.md      S10 — Servers, CI/CD, bastion, APIs
│   └── 11-Git-SSH.md                   S11 — Git over SSH, GitHub setup
├── 05-Security-Debugging/
│   ├── 12-Security-Best-Practices.md   S12 — Hardening, key hygiene, server config
│   └── 13-Debugging-SSH.md             S13 — Errors, -v flag, permission fixes
└── 06-Practice-Upgrade/
    ├── 14-Beginner-To-Pro-Roadmap.md   S14 — 5-stage mastery roadmap
    ├── 15-Quick-Cheat-Sheet.md         S15 — Commands, paths, flags at a glance
    ├── 16-One-Page-Cheat-Sheet.md      S16 — Print-and-pin single-page reference
    ├── 17-Practice-Exercises.md        S17 — 10 real-world hands-on exercises
    ├── 18-Mini-Projects.md             S18 — 5 build-it-yourself projects
    └── 19-Interview-QA.md              S19 — 22 Q&A from beginner → advanced
```

---

## Learning Path (Recommended Order)

| Stage | Files | Time |
|-------|-------|------|
| **Stage 1 — Foundations** | S1 → S2 → S3 | 20 min |
| **Stage 2 — Auth + Practice** | S4 → S5 → S6 | 15 min |
| **Stage 3 — Config + Agent** | S7 → S8 | 10 min |
| **Stage 4 — Advanced** | S9 → S10 → S11 | 20 min |
| **Stage 5 — Security + Debug** | S12 → S13 | 10 min |
| **Stage 6 — Roadmap + Cheat Sheet** | S14 → S15 → S16 | 5 min |
| **Stage 7 — Practice + Interview Prep** | S17 → S18 → S19 | ongoing |

---

## Quick Reference: Must-Know Commands

```bash
# Connect
ssh user@host
ssh -i ~/.ssh/key.pem ubuntu@ec2-ip

# File Transfer
scp file.txt user@host:/path/
rsync -avz ./local/ user@host:/remote/

# Tunneling
ssh -L 8080:localhost:3000 user@host   # local forward
ssh -R 9090:localhost:8080 user@host   # remote forward

# Agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# Debug
ssh -v user@host
ssh-add -l
```

---

## Key Files on Your Machine

| File | Purpose |
|------|---------|
| `~/.ssh/id_ed25519` | Private key (NEVER share) |
| `~/.ssh/id_ed25519.pub` | Public key (safe to share) |
| `~/.ssh/authorized_keys` | Keys allowed to log into this machine |
| `~/.ssh/known_hosts` | Verified server fingerprints |
| `~/.ssh/config` | SSH shortcut configuration |
| `~/.ssh/key.pem` | AWS/cloud private key (PEM format) |

---

## Core Mental Model

```
YOU (client)          INTERNET          SERVER
    │                                     │
    │  ──── TCP:22 ──────────────────►   │
    │  ◄─── server sends public cert ─── │
    │  ──── client verifies fingerprint ► │
    │  ──── key exchange (ECDH) ────────► │
    │  ══════ encrypted tunnel ══════════ │
    │  ──── authenticate (key/password) ► │
    │  ◄─────── shell / command ───────── │
```

---

*Updated: 2026-06-30*
