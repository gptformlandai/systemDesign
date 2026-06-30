# S4 — Authentication Methods

---

## Overview: 4 Ways to Authenticate

```
1. Password        → type username + password
2. Key-based       → private key proves identity (recommended)
3. SSH Agent       → agent holds key in memory, no typing passphrase
4. Certificate     → CA-signed certificate (enterprise scale)
```

---

## Comparison Table

| Method | Security | Scalability | Use Case | Pros | Cons |
|--------|----------|-------------|----------|------|------|
| **Password** | Low | Poor | Quick/temp access | Simple setup | Brute-forceable, unsafe on shared networks |
| **Key-based** | High | Good | Daily use, servers, CI/CD | No passwords sent, fast | Key management needed |
| **SSH Agent** | High | Great | Dev workflow, multi-hop | No passphrase typing each time | Agent must be running |
| **Certificate** | Very High | Excellent | Enterprise, 100s of servers | Central CA, expiring certs | Complex to set up |

---

## 1. Password Authentication

```bash
ssh user@host
# → prompted: "user@host's password: ****"
```

- Server checks against system's `/etc/shadow` (Linux user passwords)
- Password sent encrypted over SSH tunnel (safe from sniffing)
- **Risk:** Vulnerable to brute force, credential stuffing
- **Disable it in production** → set `PasswordAuthentication no` in `/etc/ssh/sshd_config`

---

## 2. Key-Based Authentication (Recommended)

```bash
# Basic
ssh -i ~/.ssh/id_ed25519 user@host

# If default key (~/.ssh/id_ed25519 or id_rsa) is used:
ssh user@host   # SSH auto-tries default keys
```

**Flow:**
```
1. Server checks if your public key is in authorized_keys
2. Server sends random challenge encrypted with your public key
3. Your client decrypts with private key, sends proof
4. Server verifies → access granted
```

**Setup:**
```bash
# Generate key
ssh-keygen -t ed25519 -C "my-laptop"

# Deploy public key to server
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@host
```

---

## 3. SSH Agent Authentication

- **Problem:** Key has a passphrase → type it every single time = painful
- **Solution:** `ssh-agent` holds the decrypted key in memory

```bash
# Start agent (usually auto-started on macOS/Linux desktop)
eval "$(ssh-agent -s)"

# Add key to agent (type passphrase ONCE)
ssh-add ~/.ssh/id_ed25519

# List loaded keys
ssh-add -l

# Connect — no passphrase prompt
ssh user@host
```

**Agent Forwarding** — use your local key on a remote server:
```bash
ssh -A user@bastion   # -A = forward agent
# Now from bastion, ssh to internal server uses YOUR local keys
```

> ⚠️ Use `-A` only to trusted servers — agent forwarding can be abused.

---

## 4. Certificate-Based Authentication (Enterprise)

- A **Certificate Authority (CA)** signs user/host public keys
- Server trusts the CA → trusts any cert signed by it
- Certs can have **expiry dates**, user/host restrictions

```
Traditional Key-based:                Certificate-based:
  Add pub key to each server            CA signs your key once
  100 servers = 100 manual updates      All servers trust the CA
  No expiry                             Cert expires in 24h → secure
```

**Basic setup:**
```bash
# CA signs user's public key (done by admin)
ssh-keygen -s ca_key -I "user-id" -n "ubuntu,ec2-user" -V +8h user.pub

# User gets user-cert.pub, connects with it
ssh -i user -i user-cert.pub user@host

# Server trusts CA (one-time config in sshd_config)
# TrustedUserCAKeys /etc/ssh/ca.pub
```

---

## Decision Guide: Which Method to Use?

```
Personal machine → daily work?
  ✅ Key-based + SSH Agent

AWS/Cloud EC2?
  ✅ Key-based with .pem file

CI/CD (GitHub Actions, Jenkins)?
  ✅ Key-based (deploy key or machine user key, no passphrase)

100+ servers, rotating access, short-lived access?
  ✅ Certificate-based (Vault SSH, AWS Systems Manager)

Quick test on throwaway server?
  ⚠️ Password (then disable it)

Never use in production:
  ❌ Password-only auth on internet-facing servers
```

---

## Server-Side Config (sshd_config)

```bash
# /etc/ssh/sshd_config — key settings

PasswordAuthentication no          # Disable password login
PubkeyAuthentication yes           # Enable key-based
AuthorizedKeysFile .ssh/authorized_keys  # Where to look for keys
PermitRootLogin no                 # Never allow root SSH login
MaxAuthTries 3                     # Limit brute force attempts
AllowUsers ubuntu deploy           # Whitelist specific users

# Restart after change:
sudo systemctl restart sshd
```
