# S15 — SSH Quick Cheat Sheet

> **Bookmark this page.** Everything you need at a glance.

---

## Connect

```bash
ssh user@host                                   # basic
ssh -i ~/.ssh/key.pem user@host                # with key file
ssh -p 2222 user@host                          # custom port
ssh -i key.pem -p 2222 user@host              # key + port
ssh user@host "ls -la /var/log"               # run command
ssh -J bastion-user@bastion user@internal-ip  # jump through bastion
ssh -A user@host                               # forward agent
ssh -v user@host                               # debug verbose
```

---

## Key Management

```bash
# Generate
ssh-keygen -t ed25519 -C "comment"            # create Ed25519 key
ssh-keygen -t rsa -b 4096 -C "comment"        # create RSA 4096

# Deploy
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@host

# View fingerprint
ssh-keygen -l -f ~/.ssh/id_ed25519.pub

# Extract public key from private
ssh-keygen -y -f ~/.ssh/id_ed25519

# Extract public key from PEM
ssh-keygen -y -f ~/.ssh/key.pem

# Add passphrase to existing key
ssh-keygen -p -f ~/.ssh/id_ed25519

# Fix permissions
chmod 700 ~/.ssh && chmod 600 ~/.ssh/id_ed25519 && chmod 644 ~/.ssh/id_ed25519.pub
```

---

## SSH Agent

```bash
eval "$(ssh-agent -s)"                   # start agent
ssh-add ~/.ssh/id_ed25519                # add key (enter passphrase once)
ssh-add --apple-use-keychain ~/.ssh/id_ed25519  # macOS: save to Keychain
ssh-add -t 3600 ~/.ssh/id_ed25519        # add with 1-hour expiry
ssh-add -l                               # list loaded keys (short)
ssh-add -L                               # list loaded keys (full pub key)
ssh-add -d ~/.ssh/id_ed25519             # remove specific key
ssh-add -D                               # remove ALL keys
```

---

## File Transfer

```bash
# SCP — simple copy
scp file.txt user@host:/path/                  # upload
scp user@host:/file.txt ./local/               # download
scp -r ./dir/ user@host:/path/                 # upload directory
scp -P 2222 file.txt user@host:/path/          # custom port

# rsync — efficient sync
rsync -avz ./local/ user@host:/remote/         # upload
rsync -avz user@host:/remote/ ./local/         # download
rsync -avz --delete ./local/ user@host:/remote/  # mirror (deletes extra)
rsync -avz --exclude='node_modules/' ./app/ user@host:/app/
rsync -avz -n ./local/ user@host:/remote/      # dry run
rsync -avz -e "ssh -i key.pem" ./dir/ user@host:/path/  # with key

# SFTP interactive
sftp user@host
sftp> get file.txt
sftp> put file.txt
sftp> ls / lls / cd / lcd / exit
```

---

## Port Forwarding (Tunneling)

```bash
# Local: access remote service locally
ssh -L 8080:localhost:3000 user@host           # localhost:8080 → remote:3000
ssh -L 5432:localhost:5432 ubuntu@db-server    # local psql → remote postgres
ssh -fN -L 8080:localhost:3000 user@host       # background (no shell)

# Remote: expose local service to remote
ssh -R 9090:localhost:3000 user@host           # remote:9090 → local:3000
ssh -fN -R 8080:localhost:8080 user@public     # background

# Dynamic SOCKS proxy
ssh -D 1080 user@host                          # SOCKS5 proxy on local:1080
ssh -fN -D 1080 user@host                      # background
```

---

## SSH Config (~/.ssh/config)

```
Host alias
  HostName real-host-or-ip
  User username
  IdentityFile ~/.ssh/keyfile
  Port 22
  ForwardAgent yes
  ProxyJump bastion-alias
  ServerAliveInterval 60
  ServerAliveCountMax 3
  AddKeysToAgent yes

Host *
  AddKeysToAgent yes
  IdentityFile ~/.ssh/id_ed25519
  ServerAliveInterval 60
```

```bash
chmod 600 ~/.ssh/config           # required permission
ssh -G alias                       # show effective config for alias
```

---

## known_hosts Management

```bash
ssh-keygen -R hostname             # remove stale host fingerprint
ssh-keyscan hostname               # get server's public key
ssh-keyscan -H hostname >> ~/.ssh/known_hosts  # add to known_hosts
```

---

## Debugging

```bash
ssh -v user@host                   # verbose
ssh -vvv user@host                 # max verbose
ssh-add -l                         # list agent keys
ssh-keygen -l -f ~/.ssh/id_ed25519.pub  # key fingerprint
ssh -o IdentitiesOnly=yes -i key user@host  # use ONLY this key
```

---

## GitHub / Git SSH

```bash
ssh -T git@github.com              # test GitHub auth
git clone git@github.com:user/repo.git
git remote set-url origin git@github.com:user/repo.git  # switch from HTTPS

# Multiple accounts in config
ssh -T git@github-personal
ssh -T git@github-work
git clone git@github-work:company/repo.git
```

---

## Server Hardening (sshd_config)

```
PasswordAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
MaxAuthTries 3
AllowUsers ubuntu deploy
ClientAliveInterval 300
ClientAliveCountMax 2
```
```bash
sudo sshd -t              # validate config
sudo systemctl restart sshd
```

---

## File Paths Reference

| Path | Purpose |
|------|---------|
| `~/.ssh/` | SSH directory (chmod 700) |
| `~/.ssh/id_ed25519` | Private key (chmod 600) |
| `~/.ssh/id_ed25519.pub` | Public key (chmod 644) |
| `~/.ssh/authorized_keys` | Keys allowed to login HERE (chmod 600) |
| `~/.ssh/known_hosts` | Trusted server fingerprints |
| `~/.ssh/config` | Client config (chmod 600) |
| `~/.ssh/key.pem` | AWS/cloud PEM key (chmod 400) |
| `/etc/ssh/sshd_config` | Server daemon config |
| `/etc/ssh/ssh_host_*` | Server host keys |
| `/var/log/auth.log` | SSH auth logs (Ubuntu) |
| `/var/log/secure` | SSH auth logs (RHEL/CentOS) |

---

## Common Default Usernames (Cloud)

| Platform | Default User |
|----------|-------------|
| AWS EC2 (Amazon Linux) | `ec2-user` |
| AWS EC2 (Ubuntu) | `ubuntu` |
| AWS EC2 (CentOS) | `centos` |
| AWS EC2 (Debian) | `admin` |
| GCP Compute | your-gcloud-username |
| DigitalOcean | `root` (or user you created) |
| Hetzner | `root` |
| Azure | depends on image |

---

## Escape Sequences (Stuck Session)

```
~.    Force-close a stuck SSH session
~?    List all escape sequences
~^Z   Suspend SSH (send to background)
```

---

## Mental Model Summary

```
PUBLIC KEY  = Lock    → put it everywhere (servers, GitHub)
PRIVATE KEY = Key     → keep it secret, only on your machine

ssh-agent   = Keyring → holds decrypted key in memory
authorized_keys = Front door lock list
known_hosts = List of trusted servers
~/.ssh/config = Address book with shortcuts

Tunneling = SSH as a secure pipe for ANY protocol
  -L = pull remote service to local port
  -R = push local service to remote port
  -D = use SSH server as SOCKS proxy
```
