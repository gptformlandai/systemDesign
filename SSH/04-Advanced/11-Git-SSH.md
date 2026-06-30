# S11 — Git + SSH

---

## Why SSH for Git?

| | HTTPS | SSH |
|-|-------|-----|
| Auth | Username + token/password | SSH key (no secrets per clone) |
| Token expiry | Tokens expire, need renewal | Keys don't expire (unless rotated) |
| CI/CD | Token must be stored as secret | Deploy key stored as secret (more granular) |
| 2FA complications | Yes — needs tokens, not passwords | No — key handles it |
| Speed | Slightly slower auth | Faster repeated auths |
| Setup | Zero setup | One-time key setup |
| Recommendation | Quick/personal use | ✅ **Teams, CI/CD, daily dev** |

---

## One-Time GitHub SSH Setup

```bash
# Step 1: Generate a key (if you don't have one)
ssh-keygen -t ed25519 -C "your@email.com"
# → saves to ~/.ssh/id_ed25519 and ~/.ssh/id_ed25519.pub

# Step 2: Copy public key
cat ~/.ssh/id_ed25519.pub
# → copy the output

# Step 3: Add to GitHub
# GitHub → Settings → SSH and GPG keys → New SSH key
# Paste the public key content → Save

# Step 4: Test
ssh -T git@github.com
# "Hi username! You've successfully authenticated, but GitHub does not provide shell access."
```

---

## SSH Config for GitHub

```
# ~/.ssh/config
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519
  AddKeysToAgent yes
```

macOS addition (Keychain):
```
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519
  AddKeysToAgent yes
  UseKeychain yes
```

---

## Clone and Remote Operations

```bash
# Clone using SSH
git clone git@github.com:username/repo.git

# SSH URL format:
# git@github.com:OWNER/REPO.git

# Switch existing repo from HTTPS to SSH
git remote -v                    # see current remote
git remote set-url origin git@github.com:username/repo.git
git remote -v                    # verify

# Push/pull work seamlessly — no passwords
git push origin main
git pull origin main
```

---

## Deploy Keys — Repo-Level SSH Keys

> Deploy keys are SSH keys with access to **one specific repository only** — ideal for CI/CD.

```bash
# Generate deploy key (no passphrase — for automation)
ssh-keygen -t ed25519 -C "ci-deploy-key" -f ~/.ssh/deploy_key -N ""

# Add public key to GitHub repo:
# Repo → Settings → Deploy Keys → Add key
# Paste content of ~/.ssh/deploy_key.pub
# Check "Allow write access" if deploying (pushing)

# Use deploy key to clone
GIT_SSH_COMMAND="ssh -i ~/.ssh/deploy_key" git clone git@github.com:org/repo.git

# Or via config:
Host github-deploy
  HostName github.com
  User git
  IdentityFile ~/.ssh/deploy_key
```

---

## GitHub Actions: SSH Deploy Pattern

```yaml
# .github/workflows/deploy.yml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup SSH key
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.DEPLOY_SSH_KEY }}" > ~/.ssh/deploy_key
          chmod 600 ~/.ssh/deploy_key
          # Add server to known_hosts (avoids interactive prompt)
          ssh-keyscan -H ${{ secrets.SERVER_HOST }} >> ~/.ssh/known_hosts

      - name: Build
        run: npm ci && npm run build

      - name: Deploy via rsync
        run: |
          rsync -avz --delete \
            -e "ssh -i ~/.ssh/deploy_key" \
            ./dist/ \
            deploy@${{ secrets.SERVER_HOST }}:/var/www/app/

      - name: Restart service
        run: |
          ssh -i ~/.ssh/deploy_key deploy@${{ secrets.SERVER_HOST }} \
            "sudo systemctl restart myapp && sudo systemctl status myapp"
```

---

## Multiple GitHub Accounts on One Machine

```
# ~/.ssh/config
Host github-personal
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_personal

Host github-work
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_work
```

```bash
# Clone for personal account
git clone git@github-personal:personal-username/repo.git

# Clone for work account
git clone git@github-work:company-org/repo.git

# Test each identity
ssh -T git@github-personal
ssh -T git@github-work
```

---

## GitLab, Bitbucket, Self-Hosted

```bash
# GitLab
ssh -T git@gitlab.com
git clone git@gitlab.com:group/project.git

# Bitbucket
ssh -T git@bitbucket.org
git clone git@bitbucket.org:team/repo.git

# Self-hosted Git (custom port)
git clone ssh://git@mycompany.com:7999/project/repo.git

# In ~/.ssh/config for custom port
Host mycompany.com
  Port 7999
  User git
  IdentityFile ~/.ssh/company-key
```

---

## Git SSH Troubleshooting

```bash
# Permission denied?
ssh -vT git@github.com   # verbose debug

# Wrong key loaded?
ssh-add -l               # see what's in agent
ssh-add ~/.ssh/github_key  # add the right one

# Key not recognized by GitHub?
# → Check: GitHub Settings > SSH Keys — is it listed?

# Test with explicit key
GIT_SSH_COMMAND="ssh -i ~/.ssh/id_ed25519 -v" git pull

# Known hosts issue after key rotation
ssh-keygen -R github.com
ssh -T git@github.com   # re-adds fingerprint
```
