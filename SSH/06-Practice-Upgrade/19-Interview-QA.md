# SSH — Interview Q&A (Beginner → Advanced)

> Organized by difficulty. Each answer is crisp and interview-ready.
> Format: Question → Core Answer → What makes it stand out (bonus insight)

---

## TIER 1 — Beginner (Expect these in any interview)

---

**Q1. What is SSH and what problem does it solve?**

> SSH (Secure Shell) is an encrypted network protocol for securely accessing remote machines. It replaced Telnet and FTP, which sent credentials in plain text — sniffable on any network. SSH encrypts everything: login, commands, and data transfer.

**Bonus:** "SSH also solves integrity — every packet is hashed so tampering is detected."

---

**Q2. What is the difference between a public key and a private key?**

> They are a mathematically linked pair.
> - **Public key**: shareable — you put it on servers, GitHub, etc.
> - **Private key**: secret — never leaves your machine.
>
> What the private key signs, the public key can verify. What the public key encrypts, only the private key can decrypt.

**Bonus:** "Ed25519 is the modern recommended type — small, fast, and cryptographically stronger than RSA-2048."

---

**Q3. What is `authorized_keys`?**

> A file on the server (`~/.ssh/authorized_keys`) that lists public keys allowed to log in. When you connect, the server checks if your public key is in this file, then challenges you to prove you own the matching private key.

---

**Q4. What happens when you type `ssh user@host` for the first time?**

> 1. TCP connection to port 22
> 2. Server sends its host public key + fingerprint
> 3. You are asked: "Are you sure you want to connect?" → saves fingerprint to `~/.ssh/known_hosts`
> 4. Key exchange (ECDH) → both sides derive a shared session key
> 5. All traffic is now encrypted
> 6. Client authenticates (key or password)
> 7. Shell opens

---

**Q5. Why is SSH key-based auth more secure than passwords?**

> - No password is ever sent over the network
> - Immune to brute force (no guessable secret)
> - Keys can have passphrases for additional protection
> - Easily revocable: just delete the public key from `authorized_keys`

---

**Q6. What is a .pem file?**

> PEM (Privacy Enhanced Mail) is just an encoding format for keys — Base64-wrapped with a `-----BEGIN/END-----` header. Cloud providers (AWS, GCP) issue private keys in PEM format when you create instances. It IS your private key — the format is different, not the concept.

```bash
ssh -i ~/.ssh/my-key.pem ec2-user@ec2-ip
chmod 400 ~/.ssh/my-key.pem   # AWS requires read-only
```

---

**Q7. What are the correct file permissions for SSH keys?**

| File | Permission | Why |
|------|-----------|-----|
| `~/.ssh/` | `700` | Only owner accesses directory |
| `~/.ssh/id_ed25519` | `600` | Only owner reads private key |
| `~/.ssh/authorized_keys` | `600` | Only owner modifies it |
| `~/.ssh/config` | `600` | Only owner reads it |
| `~/.ssh/key.pem` | `400` | Read-only (AWS requirement) |

**Bonus:** "SSH refuses to use a private key with permissions wider than 600 — it's a hard security enforcement."

---

**Q8. What is `known_hosts` and why does it exist?**

> It stores fingerprints of servers you've connected to. On subsequent connections, SSH compares the server's fingerprint against this file. If it changed, SSH warns you — preventing Man-in-the-Middle attacks where a rogue server impersonates the real one.

---

## TIER 2 — Intermediate (Common in backend/DevOps roles)

---

**Q9. How does SSH key-based authentication actually work step by step?**

> 1. Client sends "I have this public key" to server
> 2. Server checks `authorized_keys` for that key
> 3. Server generates a random challenge, encrypts it with the public key
> 4. Client decrypts challenge with private key, signs it, sends back
> 5. Server verifies the signature with the public key
> 6. Match → authenticated. Private key never left the client.

---

**Q10. What is SSH agent forwarding and when would you use it?**

> Agent forwarding (`-A` flag) allows a remote server to use your local SSH agent to authenticate to a third server — without copying your private key to the intermediate server.
>
> **Use case:** You SSH into a bastion host and need to SSH from there to an internal server. With `-A`, your local key handles the auth transparently.
>
> **Risk:** A compromised intermediate server can use your agent to authenticate anywhere. Only forward to trusted servers.

```bash
ssh -A ubuntu@bastion
# From bastion: ssh ubuntu@internal  ← uses YOUR local key
```

---

**Q11. Explain SSH local port forwarding with a real example.**

> Local port forwarding (`-L`) creates a tunnel where traffic sent to a local port is forwarded through the SSH server to a remote destination.
>
> **Example:** Production DB on port 5432 is not publicly exposed. Only SSH port 22 is open.

```bash
ssh -fN -L 5432:localhost:5432 ubuntu@prod-server
psql -h localhost -U myapp mydb   # connects to remote postgres through tunnel
```

> **Traffic path:** `psql` → `localhost:5432` → SSH tunnel → `prod-server:5432`

---

**Q12. What is the difference between local, remote, and dynamic port forwarding?**

| Type | Flag | Direction | Use Case |
|------|------|-----------|----------|
| Local | `-L` | Pull remote → local | Access remote DB/service locally |
| Remote | `-R` | Push local → remote | Expose local app to internet via VPS |
| Dynamic | `-D` | SOCKS proxy | Route all traffic through SSH server |

---

**Q13. What is an SSH bastion host / jump server?**

> A bastion is a single public-facing server that acts as the entry point to a private network. All other servers have no public IPs — only the bastion does.
>
> **Benefits:** All access is audited, firewall rules are simplified (only port 22 to bastion), attack surface is minimized.

```bash
ssh -J ec2-user@bastion ubuntu@10.0.0.5
# Or in config: ProxyJump bastion
```

---

**Q14. What is `ssh-agent` and why is it useful?**

> `ssh-agent` is a daemon that holds decrypted private keys in memory. You enter the passphrase once when adding the key (`ssh-add`). Subsequent SSH connections ask the agent to sign challenges — no passphrase re-entry.
>
> **Keys are in memory only** — not written to disk, lost on reboot.

```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519   # one-time passphrase
ssh user@server1            # no prompt
ssh user@server2            # no prompt
```

---

**Q15. How would you give a CI/CD system SSH access to deploy?**

> Use a **deploy key**: an SSH key pair with no passphrase, whose public key is added to the server's `authorized_keys` (or GitHub repo's deploy keys for repo-only access).
>
> The private key is stored as a CI/CD secret. Never reuse personal keys for automation.

```yaml
# GitHub Actions
- name: Setup SSH
  run: |
    echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/deploy_key
    chmod 600 ~/.ssh/deploy_key
    ssh-keyscan -H ${{ secrets.SERVER }} >> ~/.ssh/known_hosts
```

---

## TIER 3 — Advanced (Senior / DevOps / Platform roles)

---

**Q16. How does the SSH key exchange work cryptographically?**

> SSH uses **Elliptic Curve Diffie-Hellman (ECDH)** (specifically `curve25519`):
>
> 1. Client and server each generate a temporary key pair
> 2. They exchange public values
> 3. Each computes the same shared secret from: `their private value + other's public value`
> 4. The shared secret is used to derive the symmetric session key
>
> **Why DH?** Neither side sends the session key over the wire — it's independently computed. Even recording the handshake doesn't help an attacker — the session key cannot be derived from captured traffic alone (forward secrecy).

---

**Q17. What is SSH certificate-based authentication and why is it better than regular keys at scale?**

> Instead of adding each user's public key to every server's `authorized_keys`, a **Certificate Authority (CA)** signs user public keys. Servers trust the CA — they automatically trust any cert it signs.
>
> **Advantages:**
> - Centralized control — revoke CA cert → all access revoked instantly
> - Expiring certs (e.g., 8 hours) → no persistent access
> - No per-server key distribution
>
> **Tools:** HashiCorp Vault SSH Secrets Engine, AWS Systems Manager, Teleport

```bash
# CA signs user key
ssh-keygen -s ca_key -I "user@corp" -n "ubuntu" -V +8h user.pub

# Server config (trust all certs from this CA)
# TrustedUserCAKeys /etc/ssh/ca.pub
```

---

**Q18. What is SSH multiplexing and when would you enable it?**

> SSH multiplexing (`ControlMaster`) reuses an existing TCP connection for multiple SSH sessions. Subsequent connections skip the full handshake — near-instant.
>
> **Use case:** Scripts that SSH repeatedly (deployments, monitoring), or dev workflows with many short commands.

```
# ~/.ssh/config
Host myserver
  ControlMaster auto
  ControlPath ~/.ssh/cm-%r@%h:%p
  ControlPersist 10m
```

> First connection: full handshake. All subsequent connections reuse the socket — 10x faster.

---

**Q19. You see "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED". What do you do?**

> **Don't ignore it blindly.** This means the server's fingerprint changed. Steps:
>
> 1. **Investigate first:** Did the server get rebuilt? IP reassigned? Expected rotation?
> 2. **Verify out-of-band:** Contact the server admin or check instance console
> 3. **If legitimate:** `ssh-keygen -R hostname` to remove old fingerprint, reconnect
> 4. **If suspicious:** Do NOT connect — escalate as potential MITM incident

---

**Q20. How would you lock down an SSH server for production?**

Key `sshd_config` settings:
```
PasswordAuthentication no       # keys only
PermitRootLogin no              # no root SSH
MaxAuthTries 3                  # limit brute force
AllowUsers ubuntu deploy        # whitelist only needed users
ClientAliveInterval 300         # disconnect idle sessions
Port 2222                       # obscurity (reduces bot noise)
```

Additional layers:
- Firewall: restrict port 22/2222 to known IPs only
- `fail2ban`: auto-ban IPs after failed attempts
- Monitor: `sudo grep "Accepted\|Failed" /var/log/auth.log`
- Rotate keys every 6–12 months

---

**Q21. How does SSH prevent replay attacks?**

> Each SSH session uses:
> - **Fresh session keys** derived from ECDH — different every session
> - **Sequence numbers** on every packet — replaying old packets fails
> - **MACs (HMAC)** — packet integrity verified; modified packets rejected
> - **Nonces** in the handshake — prevent reusing captured handshakes

---

**Q22. How would you implement zero-trust SSH access for a 500-server fleet?**

> **Traditional:** SSH keys distributed manually — hard to revoke, no expiry, sprawl.
>
> **Zero-trust approach using Vault SSH:**
>
> 1. No static keys on servers — servers only trust Vault CA
> 2. Developer authenticates to Vault (SSO/MFA)
> 3. Vault issues short-lived (1–8 hour) signed SSH certificate
> 4. Developer SSHes with cert — auto-expires, no revocation needed
> 5. Every access is logged in Vault audit log
>
> **Result:** Compromise of a cert is time-limited. No key rotation needed. Central audit trail.

---

## Rapid-Fire Q&A

| Question | Answer |
|----------|--------|
| Default SSH port? | 22 |
| SSH daemon process name? | `sshd` |
| Best key type today? | Ed25519 |
| Where does `ssh-add` store keys? | In-memory (RAM), in `ssh-agent` |
| How to run command without interactive shell? | `ssh user@host "command"` |
| How to copy SSH key to server in one command? | `ssh-copy-id -i key.pub user@host` |
| How to debug SSH connection? | `ssh -v user@host` |
| How to remove a server from known_hosts? | `ssh-keygen -R hostname` |
| How to force close a stuck SSH session? | `~.` (tilde + dot) |
| What flag disables host key checking? | `-o StrictHostKeyChecking=no` ⚠️ |
| Git over SSH URL format? | `git@github.com:user/repo.git` |
| How to test GitHub SSH auth? | `ssh -T git@github.com` |
| What is a deploy key? | Repo-specific SSH key for CI/CD with no passphrase |
| Difference between SCP and rsync? | rsync does delta sync (only changed bytes), SCP copies full files |
| How to background a tunnel? | `ssh -fN -L local:remote:port user@host` |
