# S14 — Beginner → Pro Roadmap

---

## 5-Stage Mastery Path

```
STAGE 1 ──► STAGE 2 ──► STAGE 3 ──► STAGE 4 ──► STAGE 5
Foundations  Practical   Power User  Advanced    Expert
[20 min]     [20 min]    [15 min]    [30 min]    [ongoing]
```

---

## Stage 1 — Foundations (Day 1)

**Goal:** Understand what SSH is and connect to a server.

**Know:**
- [ ] What SSH is and why it exists (vs Telnet/FTP)
- [ ] How the handshake works (conceptually)
- [ ] Difference between public and private key
- [ ] Where keys live (`~/.ssh/`)

**Can do:**
```bash
ssh user@host                          # basic connect
ssh -i key.pem user@host               # with specific key
ssh-keygen -t ed25519                  # generate a key pair
ssh-copy-id -i key.pub user@host       # deploy public key
```

**Milestone:** SSH into an AWS EC2 instance for the first time ✅

---

## Stage 2 — Practical Daily Use (Day 2–3)

**Goal:** Use SSH for Git, file transfers, and scripting.

**Know:**
- [ ] Why key-based auth is better than password
- [ ] What `authorized_keys` is and where it lives
- [ ] SCP vs rsync (when to use which)
- [ ] How `git@github.com` works with SSH

**Can do:**
```bash
git clone git@github.com:user/repo.git  # SSH Git clone
scp file.txt user@host:/path/           # file upload
rsync -avz ./dir/ user@host:/remote/    # directory sync
ssh user@host "command"                 # remote command
```

**Milestone:** Replace all HTTPS Git remotes with SSH. Set up GitHub SSH key. ✅

---

## Stage 3 — Power User Setup (Week 1)

**Goal:** Stop typing flags, use config and agent.

**Know:**
- [ ] How `~/.ssh/config` works
- [ ] What `ssh-agent` does and why it exists
- [ ] How to use macOS Keychain integration
- [ ] Multiple SSH identities for different hosts

**Can do:**
```bash
# Config-driven connect
ssh prod              # one word, no flags

# Agent workflow
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
ssh-add -l

# Multiple GitHub accounts
git clone git@github-work:company/repo.git
```

**Milestone:** `~/.ssh/config` has 5+ host aliases. Zero passphrase prompts in daily work. ✅

---

## Stage 4 — Advanced Techniques (Week 2)

**Goal:** Tunnel, jump hosts, automate deployments.

**Know:**
- [ ] Local / Remote / Dynamic port forwarding
- [ ] Bastion host pattern (why, how)
- [ ] Agent forwarding (and its risks)
- [ ] CI/CD deploy keys pattern

**Can do:**
```bash
# Tunnel to access remote database
ssh -fN -L 5432:localhost:5432 ubuntu@prod-server
psql -h localhost ...

# Jump through bastion
ssh -J bastion ubuntu@10.0.0.5

# Expose local service via reverse tunnel
ssh -fN -R 8080:localhost:3000 user@public-server

# CI/CD deploy
rsync -avz -e "ssh -i deploy_key" ./dist/ deploy@server:/var/www/
```

**Milestone:** Write a complete deploy script using rsync + SSH. Set up bastion config. ✅

---

## Stage 5 — Expert Level (Month 1+)

**Goal:** Production hardening, automation, enterprise patterns.

**Know:**
- [ ] SSH server hardening (sshd_config best practices)
- [ ] SSH certificate-based auth (CA-signed certs)
- [ ] SSH multiplexing (ControlMaster for speed)
- [ ] Multi-hop SSH patterns
- [ ] Monitoring and auditing SSH access
- [ ] `autossh` for persistent tunnels
- [ ] Vault SSH / short-lived certificate workflows

**Can do:**
```bash
# Multi-hop SSH
ssh -J user@hop1,user@hop2 user@final

# SSH multiplexing (fast repeated connections)
# ControlMaster auto + ControlPersist 10m in config

# Generate CA-signed user cert (Vault pattern)
vault write ssh-client-signer/sign/my-role \
  public_key=@~/.ssh/id_ed25519.pub

# Audit who logged in
last -50
sudo grep sshd /var/log/auth.log | grep "Accepted"
```

**Milestone:** Zero-trust SSH setup with short-lived certs. Automated key rotation. ✅

---

## Skills Matrix

| Skill | Stage | Priority |
|-------|-------|----------|
| Basic SSH connect | 1 | 🔴 Essential |
| Key generation | 1 | 🔴 Essential |
| GitHub SSH setup | 2 | 🔴 Essential |
| SCP / rsync | 2 | 🔴 Essential |
| `~/.ssh/config` | 3 | 🟠 High |
| SSH Agent | 3 | 🟠 High |
| Local port forwarding | 4 | 🟠 High |
| Bastion host pattern | 4 | 🟠 High |
| CI/CD deploy keys | 4 | 🟠 High |
| Remote port forwarding | 4 | 🟡 Medium |
| Dynamic forwarding (SOCKS) | 4 | 🟡 Medium |
| SSH multiplexing | 5 | 🟡 Medium |
| Certificate auth | 5 | 🟢 Advanced |
| Vault SSH integration | 5 | 🟢 Advanced |

---

## Daily Practice Plan

| Day | Activity |
|-----|----------|
| Day 1 | Read S1-S3, generate key, connect to a server |
| Day 2 | Set up GitHub SSH, do first SSH Git clone |
| Day 3 | Practice SCP and rsync, transfer real files |
| Day 4 | Write `~/.ssh/config` with 3+ aliases |
| Day 5 | Set up SSH agent, test macOS Keychain |
| Day 6 | Create a local port-forward tunnel to a DB |
| Day 7 | Write a deploy script using rsync + SSH |
| Week 2 | Harden a server, set up bastion, create deploy key |
| Month 1 | SSH certs, autossh tunnels, monitoring |
