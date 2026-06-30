# S13 — Debugging SSH

---

## Golden Rule: Always Start with -v

```bash
ssh -v user@host       # verbose — shows each step
ssh -vv user@host      # more verbose
ssh -vvv user@host     # maximum debug output

# Read the output looking for:
# "Offering public key" → which keys are being tried
# "Authentications that can continue" → what server allows
# "debug1: Connection established" → TCP OK
# "Permission denied" → auth failed
```

---

## Error 1: Permission denied (publickey)

**Symptoms:**
```
user@host: Permission denied (publickey).
```

**Diagnosis flow:**
```bash
# 1. Is the key loaded?
ssh-add -l
# "The agent has no identities" → key not loaded
ssh-add ~/.ssh/id_ed25519

# 2. Does server have your public key?
ssh -v user@host 2>&1 | grep "Offering\|Accepted\|denied"

# 3. Wrong username?
ssh ubuntu@host       # Ubuntu AMI uses "ubuntu"
ssh ec2-user@host     # Amazon Linux uses "ec2-user"
ssh centos@host       # CentOS uses "centos"
ssh admin@host        # Some use "admin"

# 4. Wrong key?
ssh -i ~/.ssh/correct-key.pem user@host

# 5. Check server's authorized_keys
ssh user@host "cat ~/.ssh/authorized_keys"
# Is YOUR public key in there?
cat ~/.ssh/id_ed25519.pub   # compare
```

---

## Error 2: WARNING: UNPROTECTED PRIVATE KEY FILE

**Symptoms:**
```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@ WARNING: UNPROTECTED PRIVATE KEY FILE! @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
Permissions 0644 for '~/.ssh/id_ed25519' are too open.
```

**Fix:**
```bash
chmod 600 ~/.ssh/id_ed25519
chmod 400 ~/.ssh/key.pem      # for PEM files
chmod 700 ~/.ssh/              # fix directory too
```

---

## Error 3: Host Key Verification Failed

**Symptoms:**
```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@ WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED! @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
```

**Causes:** Server was rebuilt, IP reassigned, or (rarely) MITM attack.

**Fix (when you know the server changed legitimately):**
```bash
# Remove old fingerprint
ssh-keygen -R hostname
ssh-keygen -R 192.168.1.10

# Or manually edit ~/.ssh/known_hosts and delete the stale line
# Then reconnect and accept new fingerprint
ssh user@host
```

**Bypass for ephemeral hosts (CI/CD, disposable VMs):**
```bash
ssh -o StrictHostKeyChecking=no user@host   # ⚠️ only for trusted throwaway hosts
```

---

## Error 4: Connection Refused

**Symptoms:**
```
ssh: connect to host x.x.x.x port 22: Connection refused
```

**Causes & Fixes:**
```bash
# Is server running?
ping host   # basic connectivity
nc -zv host 22   # is port 22 open?
nmap -p 22 host  # port scan

# Is sshd running on server? (if you have other access)
sudo systemctl status sshd
sudo systemctl start sshd

# Wrong port?
ssh -p 2222 user@host   # maybe server uses non-standard port

# Firewall blocking?
# Check: AWS Security Group, GCP Firewall rules, UFW, iptables
sudo ufw status
sudo iptables -L INPUT | grep ssh
```

---

## Error 5: Connection Timeout

**Symptoms:**
```
ssh: connect to host x.x.x.x port 22: Operation timed out
```

**Causes & Fixes:**
```bash
# Firewall blocking at network level (silent drop)
nc -zv host 22        # timeout = firewall, refused = sshd not running

# Check routing
traceroute host       # where does it fail?

# Try with timeout
ssh -o ConnectTimeout=10 user@host

# On AWS: check Security Group inbound rules
# On GCP: check VPC Firewall rules
# On-prem: check router/switch ACLs
```

---

## Error 6: SSH Hangs / Freezes Mid-Session

**Fix:**
```bash
# Escape sequence to kill stuck session
~.     # press tilde THEN period — force disconnect

# Prevent hangs with keep-alive settings
# ~/.ssh/config
Host *
  ServerAliveInterval 30    # send keepalive every 30s
  ServerAliveCountMax 3     # disconnect after 3 missed keepalives
  TCPKeepAlive yes
```

---

## Error 7: Too Many Authentication Failures

**Symptoms:**
```
Received disconnect from x.x.x.x: Too many authentication failures
```

**Cause:** ssh-agent has too many keys loaded; server rejects after MaxAuthTries.

**Fix:**
```bash
# Connect with ONLY the specific key (ignore agent)
ssh -o IdentitiesOnly=yes -i ~/.ssh/correct-key user@host

# Or add to config:
Host myserver
  IdentitiesOnly yes
  IdentityFile ~/.ssh/server-specific-key
```

---

## Diagnostic Commands Quick Reference

```bash
# Test connection step by step
ssh -v user@host 2>&1 | head -50

# List agent keys
ssh-add -l

# Show fingerprint of a public key
ssh-keygen -l -f ~/.ssh/id_ed25519.pub

# Show fingerprint of known_hosts entry
ssh-keygen -l -f ~/.ssh/known_hosts

# Test if public key matches private key
ssh-keygen -y -f ~/.ssh/id_ed25519   # should output pub key
diff <(ssh-keygen -y -f ~/.ssh/id_ed25519) ~/.ssh/id_ed25519.pub  # should be empty

# Test specific key for GitHub
ssh -i ~/.ssh/github_key -T git@github.com

# Check server's host keys (fingerprints)
ssh-keyscan host
ssh-keyscan -t ed25519 host

# Check sshd config syntax (on server)
sudo sshd -t

# View auth logs (on server)
sudo tail -50 /var/log/auth.log | grep ssh
```

---

## Debugging Checklist

```
Connection refused?
  → Is sshd running? Is port open? Is IP reachable?

Permission denied (publickey)?
  → Right key? Key in authorized_keys? Right user? Key perms?

Host key changed warning?
  → Server rebuilt? → ssh-keygen -R host, reconnect

Too many auth failures?
  → Use IdentitiesOnly yes + specific IdentityFile

Hangs mid-session?
  → Use ~. to escape. Add ServerAliveInterval to config.

Slow login?
  → DNS reverse lookup delay: add UseDNS no to sshd_config

Can't forward agent?
  → AllowAgentForwarding yes in sshd_config
  → ForwardAgent yes in client config
```
