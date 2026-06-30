# SSH — One-Page Cheat Sheet

> Print this. Tape it to your monitor. Reference it daily.

---

```
╔══════════════════════════════════════════════════════════════════════════════════╗
║                         SSH MASTER CHEAT SHEET                                  ║
╠══════════════════════╦═══════════════════════════════════════════════════════════╣
║ CONNECT              ║ ssh user@host                                            ║
║                      ║ ssh -i key.pem user@host         (with key file)         ║
║                      ║ ssh -p 2222 user@host             (custom port)          ║
║                      ║ ssh -J bastion user@internal      (jump host)            ║
║                      ║ ssh -A user@host                  (forward agent)        ║
║                      ║ ssh user@host "cmd"               (run command)          ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ KEY MANAGEMENT       ║ ssh-keygen -t ed25519 -C "me"     (generate key)        ║
║                      ║ ssh-copy-id -i key.pub user@host  (deploy pubkey)       ║
║                      ║ ssh-keygen -l -f key.pub          (show fingerprint)    ║
║                      ║ ssh-keygen -y -f key.pem          (extract pubkey)      ║
║                      ║ chmod 600 ~/.ssh/id_ed25519        (fix perms)           ║
║                      ║ chmod 700 ~/.ssh                   (fix dir perms)       ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ SSH AGENT            ║ eval "$(ssh-agent -s)"             (start agent)        ║
║                      ║ ssh-add ~/.ssh/id_ed25519           (load key once)      ║
║                      ║ ssh-add --apple-use-keychain key    (macOS Keychain)     ║
║                      ║ ssh-add -l                          (list loaded keys)   ║
║                      ║ ssh-add -D                          (clear all keys)     ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ FILE TRANSFER        ║ scp file.txt user@host:/path/       (upload)            ║
║                      ║ scp user@host:/file.txt ./          (download)          ║
║                      ║ scp -r ./dir/ user@host:/path/      (upload dir)        ║
║                      ║ rsync -avz ./dir/ user@host:/path/  (sync)              ║
║                      ║ rsync -avz --delete ./dir/ user@host:/path/ (mirror)    ║
║                      ║ rsync -avzn ...                     (dry run first!)    ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ TUNNELING            ║ ssh -L 8080:localhost:3000 user@host  (local fwd)       ║
║                      ║ ssh -R 9090:localhost:3000 user@host  (remote fwd)      ║
║                      ║ ssh -D 1080 user@host                  (SOCKS proxy)    ║
║                      ║ ssh -fN -L 5432:localhost:5432 user@host  (background)  ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ DEBUG                ║ ssh -v user@host                    (verbose)           ║
║                      ║ ssh -vvv user@host                  (max verbose)       ║
║                      ║ ssh-keygen -R host                  (remove known_host) ║
║                      ║ ssh -o IdentitiesOnly=yes -i k u@h  (force key)        ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ GIT + SSH            ║ ssh -T git@github.com               (test GitHub auth)  ║
║                      ║ git clone git@github.com:user/repo  (clone via SSH)    ║
║                      ║ git remote set-url origin git@github.com:u/r           ║
╠══════════════════════╬═══════════════════════════════════════════════════════════╣
║ ESCAPE SEQUENCES     ║ ~.   force close stuck session                          ║
║                      ║ ~?   list all escape sequences                          ║
╚══════════════════════╩═══════════════════════════════════════════════════════════╝
```

---

## ~/.ssh/config Template

```
Host alias
  HostName ip-or-hostname
  User ubuntu
  IdentityFile ~/.ssh/key.pem
  Port 22
  ForwardAgent yes
  ProxyJump bastion-alias
  ServerAliveInterval 60

Host *
  AddKeysToAgent yes
  IdentityFile ~/.ssh/id_ed25519
  ServerAliveInterval 60
  ServerAliveCountMax 3
```

---

## Key File Locations

| File | What it is | Permission |
|------|-----------|------------|
| `~/.ssh/id_ed25519` | Your private key | `600` |
| `~/.ssh/id_ed25519.pub` | Your public key | `644` |
| `~/.ssh/authorized_keys` | Who can log in HERE | `600` |
| `~/.ssh/known_hosts` | Trusted servers | auto |
| `~/.ssh/config` | SSH shortcuts | `600` |
| `~/.ssh/key.pem` | AWS cloud key | `400` |
| `/etc/ssh/sshd_config` | Server config | root only |

---

## Auth Method Decision

```
Daily dev work?          → Key-based + SSH Agent
AWS EC2?                 → ssh -i key.pem  (or config alias)
CI/CD pipeline?          → Deploy key (no passphrase)
100+ servers, enterprise → Certificate-based (Vault SSH)
Quick throwaway test?    → Password (then disable it!)
```

---

## Cloud Default Users

| Platform | User |
|----------|------|
| AWS EC2 Amazon Linux | `ec2-user` |
| AWS EC2 Ubuntu | `ubuntu` |
| AWS EC2 CentOS | `centos` |
| DigitalOcean | `root` |

---

## Tunnel Quick-Pick

```
Can't reach remote DB?        → ssh -L local:remote (pull to you)
Want remote to see your app?  → ssh -R remote:local (push to them)
Need a proxy/VPN?             → ssh -D 1080 (SOCKS5)
```

---

## Top 5 Errors & Fixes

| Error | Fix |
|-------|-----|
| `Permission denied (publickey)` | Check key loaded: `ssh-add -l` / correct user |
| `UNPROTECTED PRIVATE KEY FILE` | `chmod 600 ~/.ssh/id_ed25519` |
| `REMOTE HOST ID HAS CHANGED` | `ssh-keygen -R hostname` then reconnect |
| `Connection refused` | Is `sshd` running? Is port open? |
| `Too many auth failures` | Add `IdentitiesOnly yes` in config |
