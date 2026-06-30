# S1 — What Is SSH (Core Intuition)

---

## One-Line Definition

> SSH (Secure Shell) is an **encrypted network protocol** that lets you securely log into and control remote machines over an untrusted network.

---

## The Real-World Analogy

Think of SSH like a **bank vault door + private key system**:

```
Without SSH (Telnet/FTP):
  You shout your password across a crowded room.
  Anyone nearby can hear it.

With SSH:
  You and the bank have a private sealed tube.
  Everything passes through it — encrypted, tamper-proof.
  No one outside can see or alter what's inside.
```

---

## Why SSH Exists — The Problem It Solves

| Protocol | Problem |
|----------|---------|
| **Telnet** | All data (including passwords) sent in **plain text** |
| **FTP** | Credentials sent unencrypted, no integrity checks |
| **HTTP** | No session security for shell-level access |
| **SSH** | ✅ Encrypts everything — login, commands, data |

- Pre-SSH (1990s): Sysadmins used Telnet. Passwords were sniffable on any network.
- SSH was created in **1995** by Tatu Ylönen after a password-sniffing attack at Helsinki University.

---

## What SSH Actually Does Internally

```
1. Opens a TCP connection to port 22 on the server
2. Server proves its identity (server host key)
3. Client and server negotiate an encryption algorithm
4. They create a shared session key (via key exchange)
5. All traffic from this point is encrypted
6. Client authenticates (password or key)
7. Shell or command runs inside the encrypted session
```

---

## Mental Model

```
SSH = Encrypted pipe + Identity verification + Remote shell

┌─────────────────────────────────────────────────────┐
│                     SSH Session                      │
│                                                      │
│  Your Terminal  ══════════════════  Remote Server   │
│                   (encrypted tunnel)                 │
│                                                      │
│  Everything you type → encrypted → sent → decrypted │
│  Everything you see  ← encrypted ← sent ← decrypted │
└─────────────────────────────────────────────────────┘
```

- **Port 22** is the default SSH port
- SSH is **stateful** — the encrypted session persists until you exit
- The protocol handles: **authentication + encryption + integrity** in one

---

## SSH vs Alternatives — When to Use What

| Scenario | Use |
|----------|-----|
| Remote shell into server | SSH |
| Transfer files securely | SCP / SFTP (both SSH-based) |
| Web API calls | HTTPS |
| Public website | HTTP/HTTPS |
| Internal service tunneling | SSH tunnel |

---

## Key Takeaway

> SSH is not just a login tool. It's a **secure transport layer** — you can tunnel any protocol through it, transfer files, forward ports, and automate deployments. Mastering SSH unlocks the entire DevOps toolkit.
