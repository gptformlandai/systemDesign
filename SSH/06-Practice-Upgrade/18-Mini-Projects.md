# SSH — 5 Hands-On Mini Projects

> Real projects you can build in 30–90 minutes each.
> Each project has a clear problem, architecture, and step-by-step build.

---

## Project 1 — Personal Bastion + Internal Network Access

**Time:** 45 min | **Difficulty:** ⭐⭐ | **Skills:** Jump hosts, SSH config, agent forwarding

### Problem
You have two VMs: a public-facing bastion and an internal app server (no public IP). You want to SSH into the internal server seamlessly, and also access its web app from your laptop.

### Architecture
```
Your Laptop
    │
    ▼ (SSH port 22)
BASTION (public IP: 54.x.x.x)
    │
    ▼ (private network)
APP SERVER (private IP: 10.0.0.5, no public IP)
    │
    ├── Port 3000: Node.js API (not exposed)
    └── Port 5432: PostgreSQL (not exposed)
```

### Build Steps

```bash
# --- On your laptop: ~/.ssh/config ---
Host bastion
  HostName 54.x.x.x
  User ec2-user
  IdentityFile ~/.ssh/bastion.pem
  ForwardAgent yes

Host app-server
  HostName 10.0.0.5
  User ubuntu
  IdentityFile ~/.ssh/app.pem
  ProxyJump bastion

# --- Test direct jump ---
ssh app-server                  # one command through bastion — works!

# --- Tunnel the Node.js API to local port 4000 ---
ssh -fN -L 4000:localhost:3000 app-server
curl http://localhost:4000/health   # hits app-server's API through bastion

# --- Tunnel PostgreSQL to local port 5433 ---
ssh -fN -L 5433:localhost:5432 app-server
psql -h localhost -p 5433 -U postgres   # connects to remote DB

# --- Tunnel ALL services with one command (using config) ---
# Add to ~/.ssh/config under app-server:
#   LocalForward 4000 localhost:3000
#   LocalForward 5433 localhost:5432
ssh -fN app-server   # starts all tunnels
```

### Extend It
- Add a third `monitoring-server` behind bastion
- Set `ControlMaster auto` for fast re-connections
- Write a shell script `./tunnels-up.sh` and `./tunnels-down.sh`

---

## Project 2 — Automated Backup System with rsync + SSH

**Time:** 60 min | **Difficulty:** ⭐⭐ | **Skills:** rsync, cron, deploy keys, logging

### Problem
Automatically back up a directory on your server to your local machine (or another server) every hour, with logs and rotation.

### Architecture
```
PRODUCTION SERVER              YOUR MACHINE / BACKUP SERVER
/var/www/app/                 ~/backups/app/
/var/log/app/         ──────► YYYY-MM-DD_HH/
                    rsync+SSH  backup.log
```

### Build Steps

```bash
# Step 1: Create a dedicated backup SSH key (no passphrase for cron)
ssh-keygen -t ed25519 -C "backup-cron" -f ~/.ssh/backup_key -N ""
ssh-copy-id -i ~/.ssh/backup_key.pub ubuntu@PROD-SERVER

# Step 2: Test manual backup
mkdir -p ~/backups/app
rsync -avz --delete \
  -e "ssh -i ~/.ssh/backup_key" \
  ubuntu@PROD-SERVER:/var/www/app/ \
  ~/backups/app/

# Step 3: Write backup.sh
cat > ~/backup.sh << 'EOF'
#!/bin/bash
set -euo pipefail

SERVER="ubuntu@PROD-SERVER"
KEY="$HOME/.ssh/backup_key"
REMOTE_DIR="/var/www/app"
LOCAL_BASE="$HOME/backups/app"
TIMESTAMP=$(date +%Y-%m-%d_%H%M)
DEST="$LOCAL_BASE/$TIMESTAMP"
LOG="$LOCAL_BASE/backup.log"
MAX_BACKUPS=24  # keep last 24 hourly backups

echo "[$TIMESTAMP] Starting backup..." >> "$LOG"

mkdir -p "$DEST"

rsync -avz --delete \
  -e "ssh -i $KEY -o StrictHostKeyChecking=yes" \
  "$SERVER:$REMOTE_DIR/" \
  "$DEST/" >> "$LOG" 2>&1

# Keep only last MAX_BACKUPS
ls -dt "$LOCAL_BASE"/[0-9]* | tail -n +$((MAX_BACKUPS + 1)) | xargs rm -rf

echo "[$TIMESTAMP] Backup complete: $DEST" >> "$LOG"
EOF

chmod +x ~/backup.sh

# Step 4: Test it
~/backup.sh
cat ~/backups/app/backup.log

# Step 5: Schedule with cron (every hour)
crontab -e
# Add:  0 * * * * /Users/yourname/backup.sh
# Verify: crontab -l
```

### Extend It
- Add email/Slack alert on failure
- Sync to S3 after rsync using `aws s3 sync`
- Add checksum verification: `rsync --checksum`
- Encrypt backups: pipe through `gpg` before storing

---

## Project 3 — SSH Tunnel Manager (Local Dev Tool)

**Time:** 45 min | **Difficulty:** ⭐⭐ | **Skills:** tunneling, shell scripting, process management

### Problem
You frequently need multiple SSH tunnels to access staging/prod databases and services. Managing them manually is tedious. Build a CLI tunnel manager.

### Architecture
```
tunnel.sh up          → starts all defined tunnels
tunnel.sh down        → kills all tunnels
tunnel.sh status      → shows which tunnels are running
tunnel.sh add NAME    → add new tunnel interactively
```

### Build Steps

```bash
cat > ~/bin/tunnel.sh << 'SCRIPT'
#!/bin/bash
set -euo pipefail

# ─── TUNNEL DEFINITIONS ───────────────────────────────
# Format: "name:local_port:remote_host:remote_port:ssh_host"
TUNNELS=(
  "prod-db:5432:localhost:5432:ubuntu@prod-server"
  "staging-db:5433:localhost:5432:ubuntu@staging-server"
  "prod-redis:6379:localhost:6379:ubuntu@prod-server"
  "grafana:3000:grafana.internal:3000:ubuntu@bastion"
)
PID_DIR="$HOME/.ssh/tunnels"
# ──────────────────────────────────────────────────────

mkdir -p "$PID_DIR"

cmd_up() {
  for tunnel in "${TUNNELS[@]}"; do
    IFS=: read -r name lport rhost rport shost <<< "$tunnel"
    pidfile="$PID_DIR/$name.pid"
    if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
      echo "  ✓ $name already running (pid $(cat "$pidfile"))"
      continue
    fi
    ssh -fN -o ExitOnForwardFailure=yes \
        -L "${lport}:${rhost}:${rport}" "$shost"
    # Find the PID of the background ssh just launched
    sleep 0.3
    pgrep -n -f "ssh -fN.*${lport}:${rhost}:${rport}" > "$pidfile" 2>/dev/null || true
    echo "  ↑ $name  localhost:$lport → $rhost:$rport via $shost"
  done
}

cmd_down() {
  for tunnel in "${TUNNELS[@]}"; do
    IFS=: read -r name _ <<< "$tunnel"
    pidfile="$PID_DIR/$name.pid"
    if [[ -f "$pidfile" ]]; then
      pid=$(cat "$pidfile")
      kill "$pid" 2>/dev/null && echo "  ↓ $name stopped" || echo "  - $name not running"
      rm -f "$pidfile"
    fi
  done
}

cmd_status() {
  echo "SSH Tunnels:"
  for tunnel in "${TUNNELS[@]}"; do
    IFS=: read -r name lport rhost rport shost <<< "$tunnel"
    pidfile="$PID_DIR/$name.pid"
    if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
      echo "  [UP]   $name  localhost:$lport → $rhost:$rport"
    else
      echo "  [DOWN] $name  localhost:$lport → $rhost:$rport"
    fi
  done
}

case "${1:-help}" in
  up)     cmd_up ;;
  down)   cmd_down ;;
  status) cmd_status ;;
  *)      echo "Usage: tunnel.sh [up|down|status]" ;;
esac
SCRIPT

chmod +x ~/bin/tunnel.sh

# Usage
tunnel.sh up
tunnel.sh status
tunnel.sh down
```

### Extend It
- Add health check: after `up`, `nc -z localhost $port && echo OK`
- Auto-reconnect with `autossh` for production use
- macOS: create a Launch Agent plist to start tunnels at login

---

## Project 4 — GitHub Actions SSH Deploy Pipeline

**Time:** 60 min | **Difficulty:** ⭐⭐⭐ | **Skills:** CI/CD, deploy keys, GitHub Actions, rsync

### Problem
Every push to `main` should automatically deploy your app to a production server — using only SSH, no external deploy tools.

### Architecture
```
Developer
    │ git push origin main
    ▼
GitHub Actions Runner
    │ SSH with deploy key
    ▼ rsync + ssh
Production Server
    └── /var/www/myapp/ (files updated)
    └── systemctl restart myapp
```

### Build Steps

```bash
# Step 1: Generate deploy key (no passphrase)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ./deploy_key -N ""

# Step 2: Add public key to production server
ssh-copy-id -i ./deploy_key.pub ubuntu@PROD-SERVER

# Step 3: Add private key to GitHub Secrets
# GitHub repo → Settings → Secrets and variables → Actions
# Name: SSH_PRIVATE_KEY   Value: (contents of deploy_key)
# Name: SERVER_HOST        Value: your-prod-ip
# Name: SERVER_USER        Value: ubuntu

# Step 4: Create GitHub Actions workflow
mkdir -p .github/workflows
cat > .github/workflows/deploy.yml << 'EOF'
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build
        run: |
          npm ci
          npm run build

      - name: Setup SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/deploy_key
          chmod 600 ~/.ssh/deploy_key
          # Trust server fingerprint (safe: fetched at deploy time)
          ssh-keyscan -H ${{ secrets.SERVER_HOST }} >> ~/.ssh/known_hosts

      - name: Deploy files
        run: |
          rsync -avz --delete \
            --exclude='.git/' \
            --exclude='node_modules/' \
            -e "ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=yes" \
            ./dist/ \
            ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }}:/var/www/myapp/

      - name: Restart application
        run: |
          ssh -i ~/.ssh/deploy_key \
            -o StrictHostKeyChecking=yes \
            ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }} \
            "sudo systemctl restart myapp && systemctl is-active myapp"

      - name: Verify deployment
        run: |
          ssh -i ~/.ssh/deploy_key \
            ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }} \
            "cat /var/www/myapp/version.txt"
EOF

# Step 5: Push and watch it deploy
git add .github/
git commit -m "Add SSH deploy pipeline"
git push origin main
```

### Extend It
- Add `on: pull_request` to deploy to staging
- Send Slack notification on success/failure
- Rollback step: keep previous `dist/` as `dist.bak/`
- Add health check endpoint: `curl https://prod.example.com/health`

---

## Project 5 — Reverse SSH Remote Access System

**Time:** 60 min | **Difficulty:** ⭐⭐⭐ | **Skills:** reverse tunneling, autossh, systemd, NAT traversal

### Problem
Your home lab / work laptop is behind NAT (no public IP). You want to SSH into it from anywhere in the world using a cheap VPS as a relay.

### Architecture
```
YOUR LAPTOP (behind NAT, 192.168.x.x)
    │
    │  autossh reverse tunnel (always-on)
    ▼
PUBLIC VPS (54.x.x.x, port 2222 open)
    ▲
    │  ssh -p 2222 localhost
FROM ANYWHERE
```

### Build Steps

```bash
# --- On your LAPTOP (the machine behind NAT) ---

# Step 1: Generate relay key
ssh-keygen -t ed25519 -C "home-relay" -f ~/.ssh/relay_key -N ""

# Step 2: Install public key on VPS
ssh-copy-id -i ~/.ssh/relay_key.pub ubuntu@VPS-IP

# Step 3: Configure VPS to allow remote port binding
# On VPS: /etc/ssh/sshd_config → GatewayPorts yes
# sudo systemctl restart sshd

# Step 4: Test manual reverse tunnel
# From YOUR laptop:
ssh -fN -R 2222:localhost:22 -i ~/.ssh/relay_key ubuntu@VPS-IP

# From ANYWHERE:
ssh -p 2222 ubuntu@VPS-IP    # → lands on YOUR laptop!

# Step 5: Make it permanent with autossh
brew install autossh       # macOS
# sudo apt install autossh # Linux

autossh -M 20000 \
  -fN \
  -R 2222:localhost:22 \
  -i ~/.ssh/relay_key \
  ubuntu@VPS-IP

# Step 6: Create systemd service (Linux) for auto-start on boot
# /etc/systemd/system/ssh-relay.service
cat > /tmp/ssh-relay.service << 'EOF'
[Unit]
Description=Persistent SSH Reverse Tunnel
After=network.target

[Service]
User=youruser
ExecStart=/usr/bin/autossh -M 20000 -N \
  -o "ServerAliveInterval=30" \
  -o "ServerAliveCountMax=3" \
  -o "ExitOnForwardFailure=yes" \
  -i /home/youruser/.ssh/relay_key \
  -R 2222:localhost:22 \
  ubuntu@VPS-IP
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo mv /tmp/ssh-relay.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ssh-relay
sudo systemctl start ssh-relay

# macOS: use launchd plist instead (or just use autossh in .zshrc)

# Step 7: Verify from VPS
ssh ubuntu@VPS-IP
# On VPS:
netstat -tlnp | grep 2222    # should show LISTEN
ssh -p 2222 ubuntu@localhost # should reach your laptop
```

### Extend It
- Add port 8080 reverse tunnel → expose your local dev server
- Use VPS nginx as SSL terminator → `https://myhome.example.com`
- Add `AllowUsers` on VPS to restrict who can use port 2222
- Use `ServerAliveInterval` + `ExitOnForwardFailure` for reliability
