# S12 — Security Best Practices

---

## 1. Private Key Hygiene

```
RULES (non-negotiable):
  ❌ NEVER share your private key with anyone
  ❌ NEVER commit private keys to Git (ever)
  ❌ NEVER send private keys over email/Slack/chat
  ✅ Private key stays on your machine only
  ✅ Share the PUBLIC key freely
  ✅ Use one key per machine (not one key for everything)
```

**Git protection — prevent accidental commits:**
```bash
# .gitignore
*.pem
id_rsa
id_ed25519
!*.pub    # allow public keys if needed
```

**Scan for leaked keys (pre-commit check):**
```bash
# Install gitleaks
brew install gitleaks
gitleaks detect --source . -v
```

---

## 2. Use Passphrases on Private Keys

```bash
# Generate key with passphrase
ssh-keygen -t ed25519 -C "my-laptop" -f ~/.ssh/id_ed25519
# Enter passphrase: [strong passphrase]

# Add passphrase to existing key
ssh-keygen -p -f ~/.ssh/id_ed25519

# With macOS Keychain — type passphrase once ever
ssh-add --apple-use-keychain ~/.ssh/id_ed25519
```

**Why passphrase matters:**
- If your laptop is stolen or key file is leaked → attacker still can't use it
- Passphrase encrypts the private key file at rest
- Use SSH Agent so you don't type it every time (see S8)

---

## 3. File Permissions (Critical)

```bash
# Correct permissions — SSH enforces these strictly
chmod 700 ~/.ssh                       # only owner can access directory
chmod 600 ~/.ssh/id_ed25519            # private key: owner read/write only
chmod 600 ~/.ssh/authorized_keys       # only owner can modify
chmod 600 ~/.ssh/config                # only owner can read config
chmod 644 ~/.ssh/id_ed25519.pub        # public key: readable by all (fine)
chmod 400 ~/.ssh/key.pem               # AWS PEM: read-only

# Fix all at once
chmod 700 ~/.ssh && chmod 600 ~/.ssh/* && chmod 644 ~/.ssh/*.pub
```

**What happens if permissions are wrong:**
```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@ WARNING: UNPROTECTED PRIVATE KEY FILE! @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
Permissions 0644 for '~/.ssh/id_ed25519' are too open.
→ SSH refuses to use the key
```

---

## 4. Server Hardening (sshd_config)

```bash
# /etc/ssh/sshd_config — production server settings

# DISABLE password authentication
PasswordAuthentication no
ChallengeResponseAuthentication no

# DISABLE root login
PermitRootLogin no

# Only allow key-based auth
PubkeyAuthentication yes

# Limit which users can SSH
AllowUsers ubuntu deploy jenkins
# OR by group:
AllowGroups ssh-users

# Restrict to specific IP/subnet (if your IP is static)
# In /etc/hosts.allow:
# sshd: 10.0.0.0/8

# Limit auth attempts
MaxAuthTries 3
MaxSessions 10
LoginGraceTime 30

# Disconnect idle sessions after 10 minutes
ClientAliveInterval 300
ClientAliveCountMax 2

# Use modern algorithms only
KexAlgorithms curve25519-sha256,ecdh-sha2-nistp256
Ciphers aes256-gcm@openssh.com,chacha20-poly1305@openssh.com
MACs hmac-sha2-256-etm@openssh.com

# Apply changes
sudo systemctl restart sshd
# Verify config before restarting
sudo sshd -t
```

---

## 5. Change Default SSH Port

```bash
# /etc/ssh/sshd_config
Port 2222   # Change from 22

# Update firewall
sudo ufw allow 2222/tcp
sudo ufw deny 22/tcp

# Connect using custom port
ssh -p 2222 user@host
```

> ⚠️ Security through obscurity — reduces noise from automated bots, but is NOT a primary security control. Always combine with proper auth.

---

## 6. Firewall: Only Allow SSH from Known IPs

```bash
# UFW (Ubuntu)
sudo ufw default deny incoming
sudo ufw allow from 10.0.0.0/8 to any port 22    # internal only
sudo ufw allow from YOUR_OFFICE_IP to any port 22

# AWS Security Groups
# Inbound: SSH (22) — Source: Your IP /32 only
# NEVER: 0.0.0.0/0 on port 22 (open to world)
```

---

## 7. Key Rotation and Lifecycle

```bash
# Rotate keys periodically (every 6-12 months or after team member leaves)

# Step 1: Generate new key
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_new -C "laptop-2026"

# Step 2: Add new public key to server (while old key still works)
ssh-copy-id -i ~/.ssh/id_ed25519_new.pub user@server

# Step 3: Test new key works
ssh -i ~/.ssh/id_ed25519_new user@server

# Step 4: Remove old key from server's authorized_keys
ssh user@server "sed -i '/old-key-fingerprint/d' ~/.ssh/authorized_keys"

# Step 5: Replace local key
mv ~/.ssh/id_ed25519_new ~/.ssh/id_ed25519
mv ~/.ssh/id_ed25519_new.pub ~/.ssh/id_ed25519.pub
```

---

## 8. Disable Unused Features

```bash
# /etc/ssh/sshd_config

X11Forwarding no              # Disable GUI forwarding if not needed
AllowAgentForwarding no       # Disable unless explicitly needed
AllowTcpForwarding no         # Disable tunneling unless needed
PermitTunnel no               # No TUN/TAP device access
PrintMotd no                  # Don't leak server info in banner
Banner none                   # No version banner
```

---

## 9. Monitor SSH Access

```bash
# View auth logs
sudo tail -f /var/log/auth.log         # Debian/Ubuntu
sudo tail -f /var/log/secure           # RHEL/CentOS

# Check failed login attempts
sudo grep "Failed password" /var/log/auth.log | wc -l
sudo grep "Invalid user" /var/log/auth.log | tail -20

# See who is currently logged in
who
w
last -20   # Recent logins

# Install fail2ban to auto-ban brute force attackers
sudo apt install fail2ban
# → bans IPs after 5 failed auth attempts
```

---

## 10. Least Privilege Principle

```bash
# Create a dedicated deploy user (not root, not your personal user)
sudo adduser deploy
sudo usermod -aG docker deploy        # only groups it needs

# Restrict deploy user's commands (sudoers)
echo "deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart myapp" | sudo tee /etc/sudoers.d/deploy

# Restrict SFTP user to specific directory (chroot jail)
# /etc/ssh/sshd_config:
Match User sftp-user
  ChrootDirectory /var/www/uploads
  ForceCommand internal-sftp
  AllowTcpForwarding no
  X11Forwarding no
```

---

## Security Checklist

| Item | Status |
|------|--------|
| ✅ Password auth disabled | `PasswordAuthentication no` |
| ✅ Root login disabled | `PermitRootLogin no` |
| ✅ Private key has passphrase | `ssh-keygen -p -f key` |
| ✅ Correct file permissions | `chmod 600 ~/.ssh/*` |
| ✅ SSH port restricted by firewall | Security group / UFW |
| ✅ fail2ban installed | Auto-ban brute force |
| ✅ Keys rotated periodically | Every 6-12 months |
| ✅ Unused features disabled | X11, agent fwd, tunneling |
| ✅ Auth logs monitored | `/var/log/auth.log` |
| ✅ No private keys in Git | `.gitignore` + gitleaks |
