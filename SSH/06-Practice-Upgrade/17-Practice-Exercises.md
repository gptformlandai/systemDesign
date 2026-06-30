# SSH — 10 Real-World Practice Exercises

> Each exercise is self-contained. Do them in order — each builds on the previous.
> You need: a Linux/macOS machine + ideally one remote server (free tier EC2, DigitalOcean, etc.)

---

## Exercise 1 — Generate and Inspect Your First Key Pair

**Goal:** Understand the physical files SSH uses.

**Steps:**
```bash
# 1. Generate a new Ed25519 key pair
ssh-keygen -t ed25519 -C "practice-key" -f ~/.ssh/practice_key

# 2. Inspect the private key
cat ~/.ssh/practice_key
# Observe: -----BEGIN OPENSSH PRIVATE KEY-----

# 3. Inspect the public key
cat ~/.ssh/practice_key.pub
# Format: [algorithm] [base64-key] [comment]

# 4. Get the fingerprint
ssh-keygen -l -f ~/.ssh/practice_key.pub
# Format: 256 SHA256:xxxx practice-key (ED25519)

# 5. Check permissions
ls -la ~/.ssh/practice_key*
# practice_key must be 600, practice_key.pub can be 644
```

**Verify you understand:**
- [ ] What is the difference between the two files?
- [ ] Why must the private key have `600` permissions?
- [ ] What does the comment in the public key do?

---

## Exercise 2 — SSH into a Remote Server with a Key

**Goal:** Perform a real key-based SSH login.

**Setup:** Use an AWS EC2 (free tier), DigitalOcean droplet, or any VPS. Or use `localhost` with a different user.

```bash
# Option A: AWS EC2
ssh -i ~/.ssh/my-aws-key.pem ubuntu@YOUR-EC2-IP

# Option B: Local test (create second user)
sudo adduser testuser
sudo mkdir -p /home/testuser/.ssh
sudo cp ~/.ssh/practice_key.pub /home/testuser/.ssh/authorized_keys
sudo chown -R testuser:testuser /home/testuser/.ssh
sudo chmod 700 /home/testuser/.ssh
sudo chmod 600 /home/testuser/.ssh/authorized_keys

ssh -i ~/.ssh/practice_key testuser@localhost
```

**Tasks once connected:**
```bash
whoami          # confirm you're the right user
hostname        # confirm you're on the right machine
uname -a        # check OS
df -h           # disk usage
exit
```

**Verify you understand:**
- [ ] What did the server check to allow you in?
- [ ] Where does the server look for your public key?

---

## Exercise 3 — Build a Complete ~/.ssh/config

**Goal:** Replace long ssh commands with single-word aliases.

**Steps:**
```bash
# 1. Create/edit config file
nano ~/.ssh/config  # or use your editor

# 2. Add at minimum these three entries:

# Your remote server
Host myserver
  HostName YOUR-SERVER-IP
  User ubuntu
  IdentityFile ~/.ssh/practice_key
  Port 22

# GitHub
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519

# Global defaults
Host *
  ServerAliveInterval 60
  ServerAliveCountMax 3
  AddKeysToAgent yes

# 3. Set permissions
chmod 600 ~/.ssh/config

# 4. Test
ssh myserver          # should connect without any flags
ssh -T git@github.com # should show GitHub auth success

# 5. Inspect effective config
ssh -G myserver       # see all resolved options for this alias
```

**Verify you understand:**
- [ ] What does `ServerAliveInterval` prevent?
- [ ] What does `Host *` mean?

---

## Exercise 4 — Deploy a Public Key Manually (No ssh-copy-id)

**Goal:** Understand exactly what `authorized_keys` is and how it works.

```bash
# Step 1: Print your public key
cat ~/.ssh/practice_key.pub

# Step 2: SSH into server with a working method (password or existing key)
ssh ubuntu@YOUR-SERVER

# Step 3: On the server — add the key manually
mkdir -p ~/.ssh
echo "PASTE-YOUR-PUBLIC-KEY-HERE" >> ~/.ssh/authorized_keys
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys

# Step 4: Exit
exit

# Step 5: Verify new key works
ssh -i ~/.ssh/practice_key ubuntu@YOUR-SERVER

# Step 6: View what's in authorized_keys
ssh ubuntu@YOUR-SERVER "cat ~/.ssh/authorized_keys"
```

**Verify you understand:**
- [ ] Can you have multiple keys in `authorized_keys`? How?
- [ ] What breaks if permissions on `authorized_keys` are wrong?

---

## Exercise 5 — SSH Agent Workflow

**Goal:** Load a key once, use it everywhere without re-typing passphrase.

```bash
# Step 1: Create a key WITH a passphrase
ssh-keygen -t ed25519 -C "agent-test" -f ~/.ssh/agent_test_key
# Enter a passphrase you'll remember

# Step 2: Confirm passphrase is needed every time (without agent)
ssh -i ~/.ssh/agent_test_key ubuntu@YOUR-SERVER  # prompts passphrase
ssh -i ~/.ssh/agent_test_key ubuntu@YOUR-SERVER  # prompts AGAIN

# Step 3: Start agent and load key
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/agent_test_key   # enter passphrase ONCE

# Step 4: Confirm key is loaded
ssh-add -l

# Step 5: Connect — no passphrase prompt
ssh ubuntu@YOUR-SERVER   # no prompt! agent signs automatically
ssh ubuntu@YOUR-SERVER   # still no prompt
scp ~/.ssh/config ubuntu@YOUR-SERVER:/tmp/  # no prompt here either

# Step 6: Remove from agent, confirm passphrase returns
ssh-add -d ~/.ssh/agent_test_key
ssh -i ~/.ssh/agent_test_key ubuntu@YOUR-SERVER  # passphrase required again
```

**Verify you understand:**
- [ ] Where is the decrypted key stored? (answer: in memory, not disk)
- [ ] What happens to agent keys when you reboot?

---

## Exercise 6 — File Transfer: SCP vs rsync Comparison

**Goal:** Know when to use each tool and feel the difference.

```bash
# Prepare test data
mkdir -p ~/ssh-test/{small,large}
echo "hello ssh" > ~/ssh-test/small/file.txt
dd if=/dev/urandom bs=1M count=10 of=~/ssh-test/large/bigfile.bin  # 10MB file
echo "extra file" > ~/ssh-test/large/extra.txt

# --- SCP ---
# Upload single file
time scp ~/ssh-test/small/file.txt ubuntu@YOUR-SERVER:/tmp/

# Upload directory
time scp -r ~/ssh-test/ ubuntu@YOUR-SERVER:/tmp/scp-test/

# Download file
scp ubuntu@YOUR-SERVER:/tmp/file.txt ~/ssh-test/downloaded.txt

# --- rsync ---
# First sync (full)
time rsync -avz ~/ssh-test/ ubuntu@YOUR-SERVER:/tmp/rsync-test/

# Second sync (no changes — observe speed difference)
time rsync -avz ~/ssh-test/ ubuntu@YOUR-SERVER:/tmp/rsync-test/

# Delete the extra file locally, then mirror
rm ~/ssh-test/large/extra.txt
rsync -avzn --delete ~/ssh-test/ ubuntu@YOUR-SERVER:/tmp/rsync-test/  # dry run first
rsync -avz --delete ~/ssh-test/ ubuntu@YOUR-SERVER:/tmp/rsync-test/   # real run
```

**Verify you understand:**
- [ ] Why was the second rsync faster?
- [ ] What does `--delete` do and why is it dangerous without `-n` first?

---

## Exercise 7 — Local Port Forwarding (Access Remote DB Locally)

**Goal:** Access a service on a remote server's localhost as if it's on your machine.

**Scenario:** Nginx runs on the remote server's port 80. Port 80 is NOT open in the firewall — only port 22 is.

```bash
# Step 1: Start a simple server on the remote machine
ssh ubuntu@YOUR-SERVER "python3 -m http.server 8888 &"

# Step 2: Confirm it's NOT accessible from outside
curl http://YOUR-SERVER:8888   # should fail (no firewall rule)

# Step 3: Create SSH tunnel (pull port 8888 to your local 9999)
ssh -fN -L 9999:localhost:8888 ubuntu@YOUR-SERVER

# Step 4: Access remote service locally!
curl http://localhost:9999
# You should see directory listing served by the remote server

# Step 5: See tunnel in process list
ps aux | grep "ssh -fN"

# Step 6: Kill the tunnel
pkill -f "ssh -fN -L 9999"
```

**Variation — Database tunnel:**
```bash
# If you have PostgreSQL on remote server (port 5432 not exposed)
ssh -fN -L 5432:localhost:5432 ubuntu@YOUR-SERVER
psql -h localhost -U postgres   # connects through tunnel
```

**Verify you understand:**
- [ ] Draw on paper: where does the traffic go step by step?
- [ ] What does `-fN` do and why is it useful for tunnels?

---

## Exercise 8 — GitHub SSH Setup + Multi-Account

**Goal:** Set up GitHub SSH (and optionally a second account).

```bash
# Part A: Main account
# 1. Generate key
ssh-keygen -t ed25519 -C "github-main" -f ~/.ssh/github_main

# 2. Copy public key
cat ~/.ssh/github_main.pub  # copy this

# 3. Add to GitHub: Settings → SSH keys → New SSH key

# 4. Add to ~/.ssh/config
cat >> ~/.ssh/config << 'EOF'

Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_main
  AddKeysToAgent yes
EOF

# 5. Test
ssh -T git@github.com
# Expected: "Hi username! You've successfully authenticated"

# 6. Clone a repo
git clone git@github.com:YOUR-USERNAME/some-repo.git

# Part B: Second account (e.g., work)
ssh-keygen -t ed25519 -C "github-work" -f ~/.ssh/github_work

cat >> ~/.ssh/config << 'EOF'

Host github-work
  HostName github.com
  User git
  IdentityFile ~/.ssh/github_work
  AddKeysToAgent yes
EOF

# Test work account (after adding key to work GitHub account)
ssh -T git@github-work

# Clone from work account
git clone git@github-work:WORK-ORG/repo.git
```

**Verify you understand:**
- [ ] Why does the SSH URL start with `git@` not `https://`?
- [ ] What does the `Host github-work` alias in config achieve?

---

## Exercise 9 — Harden a Server's SSH Config

**Goal:** Apply production security settings to `sshd_config`.

**⚠️ Do this on a test server only — have console access as backup.**

```bash
# Step 1: SSH into server
ssh ubuntu@YOUR-SERVER

# Step 2: Back up current config
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak

# Step 3: Apply hardening
sudo nano /etc/ssh/sshd_config
# Change/add these lines:
#   PasswordAuthentication no
#   PermitRootLogin no
#   MaxAuthTries 3
#   ClientAliveInterval 300
#   ClientAliveCountMax 2
#   AllowUsers ubuntu

# Step 4: Validate config (BEFORE restarting!)
sudo sshd -t
# Must show no errors

# Step 5: Restart sshd
sudo systemctl restart sshd

# Step 6: In a NEW terminal (keep old session open as backup)
ssh ubuntu@YOUR-SERVER   # confirm you can still log in

# Step 7: Confirm password auth is rejected
ssh -o PreferredAuthentications=password ubuntu@YOUR-SERVER
# Expected: Permission denied (publickey)
```

**Verify you understand:**
- [ ] Why do you validate with `sshd -t` before restarting?
- [ ] Why keep the original session open during testing?

---

## Exercise 10 — Build a Full SSH Deploy Pipeline

**Goal:** Deploy a local app to a remote server using only SSH tools.

**Setup:** Create a simple Node.js app (or any static files) locally.

```bash
# Step 1: Create local "app"
mkdir -p ~/deploy-practice/dist
echo "<h1>Deployed via SSH!</h1>" > ~/deploy-practice/dist/index.html
echo "v1.0" > ~/deploy-practice/dist/version.txt

# Step 2: Create deploy key (no passphrase — for automation)
ssh-keygen -t ed25519 -C "deploy-key" -f ~/.ssh/deploy_key -N ""

# Step 3: Install deploy key on server
ssh-copy-id -i ~/.ssh/deploy_key.pub ubuntu@YOUR-SERVER

# Step 4: Prepare server
ssh -i ~/.ssh/deploy_key ubuntu@YOUR-SERVER << 'EOF'
  sudo apt-get install -y nginx
  sudo mkdir -p /var/www/myapp
  sudo chown ubuntu:ubuntu /var/www/myapp
EOF

# Step 5: Write deploy.sh
cat > ~/deploy-practice/deploy.sh << 'DEPLOY'
#!/bin/bash
set -e

SERVER="ubuntu@YOUR-SERVER"
KEY="$HOME/.ssh/deploy_key"
DEST="/var/www/myapp"

echo "==> Syncing files..."
rsync -avz --delete \
  -e "ssh -i $KEY" \
  ./dist/ \
  $SERVER:$DEST/

echo "==> Reloading nginx..."
ssh -i $KEY $SERVER "sudo nginx -s reload || sudo systemctl start nginx"

echo "==> Deploy complete!"
ssh -i $KEY $SERVER "cat $DEST/version.txt"
DEPLOY

chmod +x ~/deploy-practice/deploy.sh

# Step 6: Run deployment
cd ~/deploy-practice && ./deploy.sh

# Step 7: Update and redeploy
echo "v1.1" > ~/deploy-practice/dist/version.txt
echo "<h1>v1.1 - Updated!</h1>" > ~/deploy-practice/dist/index.html
./deploy.sh

# Step 8: Verify
curl http://YOUR-SERVER/  # or ssh in and check files
```

**Verify you understand:**
- [ ] Why no passphrase on the deploy key?
- [ ] What does `set -e` do in the deploy script?
- [ ] How would you extend this for a real CI/CD pipeline?
