# S10 — Real-World Use Cases

---

## Use Case 1: Login to Remote Server

```bash
# Direct login
ssh ubuntu@api.mycompany.com

# With .pem key (AWS)
ssh -i ~/.ssh/prod-key.pem ec2-user@ec2-35-172-10-20.compute-1.amazonaws.com

# After config setup (SSH/07-SSH-Config.md):
ssh prod-api
```

**Common remote tasks:**
```bash
ssh user@host "sudo systemctl restart myapp"
ssh user@host "tail -100f /var/log/myapp/app.log"
ssh user@host "journalctl -u myapp --since '5 minutes ago'"
ssh user@host "docker ps && docker logs myapp --tail 50"
```

---

## Use Case 2: GitHub SSH Usage

```bash
# Test GitHub SSH auth
ssh -T git@github.com
# "Hi username! You've successfully authenticated"

# Clone over SSH (no password prompts ever)
git clone git@github.com:user/repo.git

# Change existing HTTPS remote to SSH
git remote set-url origin git@github.com:user/repo.git

# Multiple GitHub accounts (see S7 SSH Config)
git clone git@github-work:company/private-repo.git
```

**Why SSH over HTTPS for Git:**
- No token expiry headaches
- No credential manager needed
- Seamless in CI/CD with deploy keys

---

## Use Case 3: File Transfer (Deployment)

```bash
# Deploy static build to server
rsync -avz --delete ./dist/ ubuntu@webserver:/var/www/html/

# Backup logs from production
scp -r ubuntu@prod:/var/log/app/ ./prod-logs/$(date +%Y%m%d)/

# Upload config files
scp -i key.pem ./nginx.conf ubuntu@server:/tmp/
ssh ubuntu@server "sudo mv /tmp/nginx.conf /etc/nginx/ && sudo nginx -t && sudo nginx -s reload"
```

---

## Use Case 4: CI/CD Servers (GitHub Actions / Jenkins)

**Pattern: Deploy key — a read-only SSH key for a specific repo**

```bash
# Generate deploy key (no passphrase for automation)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy_key -N ""

# Add public key to GitHub repo:
# Settings → Deploy Keys → Add Key

# Add private key to GitHub Secrets:
# Settings → Secrets → SSH_DEPLOY_KEY = (private key content)
```

**GitHub Actions example:**
```yaml
# .github/workflows/deploy.yml
- name: Setup SSH
  run: |
    mkdir -p ~/.ssh
    echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/deploy_key
    chmod 600 ~/.ssh/deploy_key
    ssh-keyscan -H ${{ secrets.SERVER_HOST }} >> ~/.ssh/known_hosts

- name: Deploy
  run: |
    rsync -avz -e "ssh -i ~/.ssh/deploy_key" \
      ./dist/ deploy@${{ secrets.SERVER_HOST }}:/var/www/app/
    ssh -i ~/.ssh/deploy_key deploy@${{ secrets.SERVER_HOST }} \
      "sudo systemctl restart myapp"
```

---

## Use Case 5: Securing Internal APIs (SSH Tunneling)

**Problem:** Database/Redis/internal API not exposed publicly, but you need access from local.

```bash
# Access production DB locally for debugging
ssh -fN -L 5432:localhost:5432 ubuntu@prod-server
psql -h localhost -p 5432 -U myapp mydb

# Access Redis
ssh -fN -L 6379:localhost:6379 ubuntu@prod-server
redis-cli -h localhost

# Access internal Kubernetes dashboard
ssh -fN -L 8001:localhost:8001 ubuntu@k8s-master
kubectl proxy &   # or forward from k8s
open http://localhost:8001/api/v1/namespaces/kube-system/services/kubernetes-dashboard/proxy/

# Access Grafana on internal network
ssh -fN -L 3000:grafana.internal:3000 ubuntu@bastion
open http://localhost:3000
```

---

## Use Case 6: Jump Servers / Bastion Hosts

**What is a Bastion Host?**
```
INTERNET
    │
    ▼
┌──────────┐     Private Subnet
│ BASTION  │──────────────────► App Server (10.0.0.5)
│ (public) │──────────────────► DB Server  (10.0.0.6)
└──────────┘──────────────────► Cache      (10.0.0.7)
    ▲
    │
 Only SSH (port 22) is open to internet
 Everything else is private
```

- Bastion = single point of entry to private network
- All access is audited through one server
- Internal servers have NO public IP

**Connecting through bastion:**
```bash
# One-off jump
ssh -J ec2-user@bastion.example.com ubuntu@10.0.0.5

# Config-driven (cleaner)
# ~/.ssh/config
Host bastion
  HostName bastion.example.com
  User ec2-user
  IdentityFile ~/.ssh/bastion-key.pem

Host app-server
  HostName 10.0.0.5
  User ubuntu
  ProxyJump bastion
  IdentityFile ~/.ssh/app-key.pem

# Usage:
ssh app-server    # auto-jumps through bastion
```

---

## Use Case 7: SSH in Docker / Kubernetes

```bash
# SSH into a running Docker container
docker exec -it my-container /bin/bash   # (preferred — not SSH)
# OR if container runs sshd:
ssh -p 2222 user@localhost   # if port is mapped

# SSH into Kubernetes pod
kubectl exec -it pod-name -- /bin/bash   # (preferred)
# For actual SSH (if pod has sshd):
kubectl port-forward pod/my-pod 2222:22
ssh -p 2222 user@localhost
```

---

## Use Case 8: Reverse SSH (Access Machine Behind NAT)

**Problem:** Your laptop is behind a corporate NAT. You want to SSH into it from outside.

```bash
# FROM your laptop — create reverse tunnel to a public server
ssh -fN -R 2222:localhost:22 user@public-server

# FROM anywhere — SSH into your laptop through public server
ssh -p 2222 user@public-server
# → now you're on your laptop!
```

**Keep it alive with autossh:**
```bash
autossh -M 20001 -fN -R 2222:localhost:22 user@public-server
```
