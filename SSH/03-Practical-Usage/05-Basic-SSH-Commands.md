# S5 — Basic SSH Commands (Practical)

---

## Connect to Server

```bash
# Basic connection
ssh user@hostname
ssh user@192.168.1.10
ssh ubuntu@ec2-54-123-45-67.compute-1.amazonaws.com

# With specific key
ssh -i ~/.ssh/id_ed25519 user@host
ssh -i ~/.ssh/my-aws-key.pem ec2-user@host

# Custom port
ssh -p 2222 user@host

# Combine: key + port
ssh -i ~/.ssh/key.pem -p 2222 user@host

# Run a single command (no interactive shell)
ssh user@host "ls -la /var/log"
ssh user@host "df -h && uptime"

# Run multiple commands
ssh user@host << 'EOF'
  cd /app
  git pull
  systemctl restart myapp
EOF
```

---

## Essential Flags

| Flag | Meaning | Example |
|------|---------|---------|
| `-i` | Specify identity (private key) file | `ssh -i key.pem user@host` |
| `-p` | Custom port | `ssh -p 2222 user@host` |
| `-v` | Verbose (debug) | `ssh -v user@host` |
| `-vvv` | Very verbose (max debug) | `ssh -vvv user@host` |
| `-A` | Forward SSH agent | `ssh -A user@bastion` |
| `-N` | No remote command (tunnels) | `ssh -N -L 8080:...` |
| `-f` | Background after auth | `ssh -f -N -L 8080:...` |
| `-X` | Enable X11 forwarding (GUI apps) | `ssh -X user@host` |
| `-C` | Enable compression | `ssh -C user@host` |
| `-o` | One-off option | `ssh -o StrictHostKeyChecking=no user@host` |
| `-J` | Jump host (ProxyJump) | `ssh -J bastion user@internal` |
| `-L` | Local port forward | see S9 |
| `-R` | Remote port forward | see S9 |
| `-D` | Dynamic port forward | see S9 |

---

## Multi-Hop SSH (Jump Host)

```bash
# Jump through bastion to reach internal host
ssh -J bastion-user@bastion.example.com internal-user@10.0.0.5

# Multiple hops
ssh -J user@hop1,user@hop2 user@final-dest

# Or set in ~/.ssh/config (cleaner — see S7)
```

---

## Execute Remote Scripts

```bash
# Run local script on remote machine
ssh user@host 'bash -s' < local_script.sh

# Pass variables
ssh user@host "export APP_ENV=prod; ./deploy.sh"

# Sudo commands
ssh user@host "sudo systemctl restart nginx"
```

---

## Useful One-Liners

```bash
# Check if SSH port is open (no login)
ssh -o BatchMode=yes -o ConnectTimeout=5 user@host 2>&1

# Get remote hostname
ssh user@host hostname

# Check disk on remote
ssh user@host "df -h"

# Watch logs remotely
ssh user@host "tail -f /var/log/app.log"

# Create remote directory
ssh user@host "mkdir -p /opt/myapp/config"

# Test connection without side effects
ssh -T git@github.com   # GitHub identity check
```

---

## SSH with Key — Common Cloud Patterns

```bash
# AWS EC2
ssh -i ~/.ssh/my-key.pem ec2-user@ec2-ip       # Amazon Linux
ssh -i ~/.ssh/my-key.pem ubuntu@ec2-ip          # Ubuntu AMI
ssh -i ~/.ssh/my-key.pem centos@ec2-ip          # CentOS

# GCP Compute Engine (gcloud handles keys)
gcloud compute ssh instance-name --zone=us-central1-a

# DigitalOcean, Hetzner, Linode
ssh -i ~/.ssh/id_ed25519 root@droplet-ip

# Avoid "Permission denied": fix key perms
chmod 400 ~/.ssh/my-key.pem
```

---

## Quick Exit

```bash
~.      # Force close stuck SSH session (tilde + dot)
~?      # List all escape sequences
exit    # Normal exit
Ctrl+D  # EOF — same as exit
```
