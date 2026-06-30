# S6 — File Transfer via SSH

---

## Three Tools — Quick Comparison

| Tool | Protocol | Best For | Speed | Resume | Bandwidth |
|------|----------|----------|-------|--------|-----------|
| **SCP** | SSH | Simple copies, scripting | Fast | ❌ No | No throttle |
| **SFTP** | SSH (FTP-like) | Interactive browsing | Medium | Partial | No throttle |
| **rsync** | SSH | Large syncs, backups, delta sync | Fastest | ✅ Yes | Throttleable |

**Rule of thumb:**
- Quick copy → `scp`
- Interactive browse + upload → `sftp`
- Syncing folders, backups, deployments → `rsync`

---

## SCP — Secure Copy Protocol

```bash
# Upload (local → remote)
scp file.txt user@host:/path/to/destination/
scp -i key.pem file.txt ubuntu@ec2-ip:/home/ubuntu/

# Download (remote → local)
scp user@host:/remote/file.txt ./local/
scp -i key.pem ubuntu@ec2-ip:/var/log/app.log ~/Downloads/

# Copy directory (recursive)
scp -r ./local-dir/ user@host:/remote/dir/

# Custom port
scp -P 2222 file.txt user@host:/path/

# With key + custom port
scp -i key.pem -P 2222 file.txt user@host:/path/

# Copy between two remote servers (via local machine)
scp user@host1:/file.txt user@host2:/dest/
```

**SCP Syntax Pattern:**
```
scp [flags] SOURCE DESTINATION
  local path: /path/to/file  or  ./file
  remote path: user@host:/path/to/file
```

---

## SFTP — Interactive SSH FTP

```bash
# Start SFTP session
sftp user@host
sftp -i key.pem ubuntu@ec2-ip

# SFTP interactive commands
sftp> ls               # list remote files
sftp> lls              # list LOCAL files
sftp> pwd              # remote current dir
sftp> lpwd             # local current dir
sftp> cd /var/log      # change remote dir
sftp> lcd ~/Downloads  # change local dir
sftp> get app.log      # download file
sftp> put deploy.sh    # upload file
sftp> get -r /remote/dir   # download directory
sftp> put -r ./local/dir/  # upload directory
sftp> mkdir /remote/new-dir
sftp> rm /remote/file.txt
sftp> exit
```

**Use SFTP when:** You need to browse the remote filesystem interactively before deciding what to transfer.

---

## rsync — Efficient Sync Tool

```bash
# Basic sync (local → remote)
rsync -avz ./local-dir/ user@host:/remote/dir/

# Key breakdown:
#   -a = archive mode (preserves perms, timestamps, symlinks, etc.)
#   -v = verbose
#   -z = compress in transit
#   --progress = show progress bar
#   --delete = delete files on remote not in local (mirror)
#   -n = dry run (shows what WOULD happen, safe to test)

# Download (remote → local)
rsync -avz user@host:/remote/dir/ ./local/

# With custom key
rsync -avz -e "ssh -i ~/.ssh/key.pem" ./app/ ubuntu@ec2-ip:/var/www/app/

# With custom port
rsync -avz -e "ssh -p 2222" ./dir/ user@host:/path/

# Exclude files
rsync -avz --exclude='node_modules/' --exclude='.git/' ./app/ user@host:/app/

# Throttle bandwidth (useful on slow links)
rsync -avz --bwlimit=1000 ./dir/ user@host:/path/   # 1000 KB/s max

# Resume interrupted transfer
rsync -avz --partial --progress ./bigfile.tar user@host:/backup/

# Dry run (preview changes)
rsync -avzn ./dir/ user@host:/path/
```

---

## Deployment Pattern (rsync)

```bash
# Common deploy script pattern
rsync -avz \
  --exclude='node_modules/' \
  --exclude='.env' \
  --exclude='.git/' \
  --delete \
  -e "ssh -i ~/.ssh/deploy_key" \
  ./dist/ \
  deploy@prod-server:/var/www/myapp/

# Then restart service
ssh -i ~/.ssh/deploy_key deploy@prod-server "sudo systemctl restart myapp"
```

---

## ASCII: Transfer Directions

```
LOCAL MACHINE                   REMOTE SERVER
     │                               │
     │── scp file.txt user@host:/──► │  UPLOAD
     │◄─ scp user@host:/file.txt ./ ─│  DOWNLOAD
     │                               │
     │── rsync -avz ./dir/ user@...─►│  SYNC LOCAL→REMOTE
     │◄─ rsync -avz user@.../dir/ ./─│  SYNC REMOTE→LOCAL
     │                               │
     │══ sftp user@host ═════════════│  INTERACTIVE SESSION
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Forgot trailing `/` in rsync | `rsync ./dir/` copies dir contents; `./dir` copies dir itself |
| SCP fails silently | Add `-v` for verbose output |
| Wrong user for AWS | ec2-user (Amazon Linux), ubuntu (Ubuntu), centos (CentOS) |
| Permission denied on dest | Check remote directory permissions with `ls -la` |
