# S7 — SSH Config File (~/.ssh/config)

---

## Why SSH Config Is a Game-Changer

Without config:
```bash
ssh -i ~/.ssh/my-aws-key.pem -p 2222 -A ec2-user@ec2-54-123-45-67.compute-1.amazonaws.com
```

With config:
```bash
ssh myserver
```

> One alias → replaces all flags. The `~/.ssh/config` file is the developer's SSH power tool.

---

## Basic Structure

```
Host <alias>
  HostName <actual-hostname-or-IP>
  User <username>
  IdentityFile <path-to-private-key>
  Port <port>
  [other options...]
```

---

## Practical Config Examples

```
# ~/.ssh/config

# ----- AWS EC2 -----
Host myec2
  HostName ec2-54-123-45-67.compute-1.amazonaws.com
  User ec2-user
  IdentityFile ~/.ssh/my-aws-key.pem
  Port 22

# ----- DigitalOcean Droplet -----
Host droplet
  HostName 178.62.10.20
  User root
  IdentityFile ~/.ssh/id_ed25519

# ----- GitHub -----
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_key

# ----- Custom Port Server -----
Host devbox
  HostName 192.168.1.100
  User ubuntu
  Port 2222
  IdentityFile ~/.ssh/id_ed25519

# ----- Jump Host / Bastion -----
Host internal
  HostName 10.0.0.5
  User ubuntu
  ProxyJump bastion
  IdentityFile ~/.ssh/id_ed25519

Host bastion
  HostName bastion.example.com
  User ec2-user
  IdentityFile ~/.ssh/bastion-key.pem
  ForwardAgent yes

# ----- Wildcard Pattern -----
Host *.example.com
  User ubuntu
  IdentityFile ~/.ssh/company-key

# ----- Default fallback for all hosts -----
Host *
  ServerAliveInterval 60
  ServerAliveCountMax 3
  AddKeysToAgent yes
  IdentityFile ~/.ssh/id_ed25519
```

**Usage after config:**
```bash
ssh myec2      # connects to EC2 with key + user pre-configured
ssh droplet    # connects to DigitalOcean
ssh devbox     # connects on port 2222
ssh internal   # jumps through bastion automatically
```

---

## All Useful Config Options

| Option | Purpose | Example |
|--------|---------|---------|
| `HostName` | Real hostname or IP | `HostName 192.168.1.10` |
| `User` | Remote username | `User ubuntu` |
| `Port` | SSH port | `Port 2222` |
| `IdentityFile` | Private key path | `IdentityFile ~/.ssh/key.pem` |
| `ProxyJump` | Jump through host | `ProxyJump bastion` |
| `ForwardAgent` | Forward SSH agent | `ForwardAgent yes` |
| `ServerAliveInterval` | Keep-alive ping seconds | `ServerAliveInterval 60` |
| `ServerAliveCountMax` | Max keep-alive attempts | `ServerAliveCountMax 3` |
| `AddKeysToAgent` | Auto-add key to agent | `AddKeysToAgent yes` |
| `StrictHostKeyChecking` | known_hosts checking | `StrictHostKeyChecking no` ⚠️ |
| `LogLevel` | Logging verbosity | `LogLevel VERBOSE` |
| `Compression` | Enable compression | `Compression yes` |
| `ConnectTimeout` | Connection timeout | `ConnectTimeout 10` |
| `ControlMaster` | SSH multiplexing | `ControlMaster auto` |
| `ControlPath` | Socket for multiplexing | `ControlPath ~/.ssh/cm-%r@%h:%p` |
| `ControlPersist` | Keep master alive | `ControlPersist 10m` |

---

## SSH Multiplexing (Speed Boost)

Reuse an existing SSH connection for subsequent connections:

```
# ~/.ssh/config
Host myserver
  HostName host
  User ubuntu
  ControlMaster auto
  ControlPath ~/.ssh/cm-%r@%h:%p
  ControlPersist 10m
```

```bash
# First connection: full handshake
ssh myserver       # normal speed

# Subsequent connections: instant (reuse tunnel)
ssh myserver       # near-instant
scp file myserver:/path/   # uses same tunnel
```

---

## Multiple GitHub Accounts (Pro Pattern)

```
# ~/.ssh/config

Host github-personal
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_personal_key

Host github-work
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_work_key
```

```bash
# Clone with specific identity
git clone git@github-personal:username/repo.git
git clone git@github-work:company/repo.git
```

---

## File Permissions

```bash
chmod 600 ~/.ssh/config   # SSH refuses to read if too permissive
chmod 700 ~/.ssh/          # Directory must be owner-only
```

---

## Test Config Without Connecting

```bash
ssh -G myserver   # Print all effective config options for 'myserver'
ssh -nT myserver  # Dry run (no PTY, no command)
```
