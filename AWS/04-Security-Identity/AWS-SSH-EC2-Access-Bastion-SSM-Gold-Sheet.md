# AWS SSH, EC2 Access, Bastion Hosts, and SSM Session Manager Gold Sheet

> Track: AWS Interview Track — Security and Identity
> Goal: understand every way to securely access EC2 instances — from traditional SSH key pairs through bastion hosts, EC2 Instance Connect, and SSM Session Manager — and choose the right pattern for each production scenario.

---

## 0. How To Read This

Beginner focus:
- SSH to EC2 with a PEM key pair
- Security group rules for SSH
- Basic key pair management

Intermediate focus:
- Bastion host (jump host) pattern
- SSH agent forwarding
- SSH config for AWS hosts

Senior / MAANG focus:
- SSM Session Manager — no SSH, no open ports, no key pairs
- EC2 Instance Connect — ephemeral keys, no standing access
- Port forwarding through SSM
- IAM-driven access control replacing security group rules
- Audit trail: who connected, when, what commands ran

---

# Topic 1: EC2 Key Pairs and SSH Fundamentals

## 1. How EC2 SSH Access Works

```text
You              EC2 Instance
  |                   |
  |-- SSH (port 22) -->|
  |   Key-based auth   |
  |                   |
  Your private key    Instance public key
  (stored locally)    (in ~/.ssh/authorized_keys on the instance)
```

AWS injects your public key into the instance at launch via cloud-init. You use the corresponding private key (`.pem` file) to authenticate.

## 2. Creating a Key Pair

**Via AWS Console:**
1. EC2 → Key Pairs → Create key pair
2. Name it, choose RSA or ED25519, choose `.pem` (Linux/Mac) or `.ppk` (PuTTY/Windows)
3. Download and save — AWS never stores the private key

**Via AWS CLI:**
```bash
# Create key pair and save private key
aws ec2 create-key-pair \
  --key-name my-prod-key \
  --key-type ed25519 \
  --query "KeyMaterial" \
  --output text > ~/.ssh/my-prod-key.pem

chmod 400 ~/.ssh/my-prod-key.pem
```

**Import your existing public key (if you already have an SSH key):**
```bash
# Generate a key pair locally
ssh-keygen -t ed25519 -C "me@company.com" -f ~/.ssh/aws-key

# Import the public key to AWS
aws ec2 import-key-pair \
  --key-name my-imported-key \
  --public-key-material fileb://~/.ssh/aws-key.pub
```

## 3. Connecting to EC2 via SSH

```bash
# Amazon Linux, RHEL, CentOS
ssh -i ~/.ssh/my-key.pem ec2-user@<PUBLIC_IP>

# Ubuntu
ssh -i ~/.ssh/my-key.pem ubuntu@<PUBLIC_IP>

# Debian
ssh -i ~/.ssh/my-key.pem admin@<PUBLIC_IP>

# SLES (SUSE)
ssh -i ~/.ssh/my-key.pem ec2-user@<PUBLIC_IP>

# Windows (after enabling OpenSSH)
ssh -i ~/.ssh/my-key.pem Administrator@<PUBLIC_IP>
```

**Common first-time errors:**

| Error | Cause | Fix |
|---|---|---|
| `Permission denied (publickey)` | Wrong user or wrong key | Check AMI default username; verify key pair |
| `UNPROTECTED PRIVATE KEY FILE` | Wrong file permissions | `chmod 400 ~/.ssh/key.pem` |
| `Connection timed out` | Security group blocks port 22 | Add inbound TCP 22 rule for your IP |
| `Host key verification failed` | Instance IP reused | Remove old host key: `ssh-keygen -R <IP>` |
| `Connection refused` | SSH daemon not running | Check instance status, use serial console |

## 4. Security Group Rules for SSH

**Minimum rule to allow SSH:**
```text
Type:    SSH
Protocol: TCP
Port:    22
Source:  YOUR_IP/32   (your specific IP, not 0.0.0.0/0)
```

**AWS CLI:**
```bash
MY_IP=$(curl -s https://checkip.amazonaws.com)

aws ec2 authorize-security-group-ingress \
  --group-id sg-0123456789abcdef0 \
  --protocol tcp \
  --port 22 \
  --cidr "$MY_IP/32"
```

**Interview line:**
```text
Never allow SSH from 0.0.0.0/0 (the entire internet).
In production: use bastion hosts, SSM Session Manager, or EC2 Instance Connect.
None of those require opening port 22 to the public internet.
```

---

# Topic 2: SSH Config File for AWS Hosts

## 5. ~/.ssh/config for AWS

Avoid typing the key path and username every time:

```
# ~/.ssh/config

# Dev environment
Host dev-api
  HostName 10.0.1.100
  User ec2-user
  IdentityFile ~/.ssh/dev-key.pem
  StrictHostKeyChecking accept-new

# Production via bastion
Host prod-app
  HostName 10.0.2.50         # private IP
  User ec2-user
  IdentityFile ~/.ssh/prod-key.pem
  ProxyJump bastion          # connects through bastion first

Host bastion
  HostName 54.172.10.20      # bastion public IP
  User ec2-user
  IdentityFile ~/.ssh/prod-key.pem
  StrictHostKeyChecking yes

# Wildcard for all AWS hosts in a region
Host *.compute-1.amazonaws.com
  User ec2-user
  IdentityFile ~/.ssh/my-key.pem
  ServerAliveInterval 60
  ServerAliveCountMax 3
```

Usage:
```bash
ssh dev-api            # replaces: ssh -i ~/.ssh/dev-key.pem ec2-user@10.0.1.100
ssh prod-app           # connects through bastion automatically
```

---

# Topic 3: Bastion Host (Jump Host) Pattern

## 6. What Is a Bastion Host?

A bastion host (jump server) is a publicly accessible EC2 instance that serves as the only SSH entry point into your private network.

```text
Internet
   |
   | SSH (port 22)
   ↓
Bastion Host          Public Subnet
   |
   | SSH (port 22)    Internal network only
   ↓
Private EC2 Instances  Private Subnet
```

**Why:**
- Private EC2 instances have no public IP
- Bastion is the only public-facing attack surface
- Security groups on private instances only allow SSH from the bastion's security group

## 7. Setting Up a Bastion Host

**Security Group for Bastion:**
```text
Inbound:
  TCP 22    from YOUR_OFFICE_CIDR (not 0.0.0.0/0)
Outbound:
  TCP 22    to private-instances-sg
```

**Security Group for Private Instances:**
```text
Inbound:
  TCP 22    from bastion-sg (not an IP — reference the bastion's security group ID)
Outbound:
  (restrict as needed)
```

## 8. Connecting Through a Bastion

**Method 1 — ProxyJump (modern, recommended):**
```bash
# Single command, transparent tunnel
ssh -J ec2-user@BASTION_IP ec2-user@PRIVATE_IP -i ~/.ssh/key.pem

# Or with different keys per hop
ssh -J ec2-user@BASTION_IP \
    -i ~/.ssh/prod-key.pem \
    ec2-user@10.0.2.50
```

**Method 2 — SSH Config ProxyJump:**
```
Host bastion
  HostName 54.172.10.20
  User ec2-user
  IdentityFile ~/.ssh/bastion-key.pem

Host prod-*
  User ec2-user
  IdentityFile ~/.ssh/prod-key.pem
  ProxyJump bastion
```

```bash
ssh prod-api     # connects: you → bastion → prod-api, one command
```

**Method 3 — SSH Agent Forwarding (legacy, less secure):**
```bash
# Add key to agent
ssh-add ~/.ssh/prod-key.pem

# Connect to bastion with agent forwarding
ssh -A ec2-user@BASTION_IP

# From bastion, connect to private instance (uses your forwarded key)
ssh ec2-user@10.0.2.50
```

Warning on agent forwarding:
```text
Agent forwarding exposes your key socket to the bastion host.
If the bastion is compromised, an attacker can use your forwarded key.
Prefer ProxyJump — it never exposes the key to the intermediate host.
```

## 9. SSH Port Forwarding Through Bastion

**Forward a database port to your laptop:**
```bash
# Access RDS on private subnet from local port 5433
ssh -L 5433:my-rds.cluster-xyz.us-east-1.rds.amazonaws.com:5432 \
    -N -f \
    ec2-user@BASTION_IP \
    -i ~/.ssh/bastion-key.pem

# Now connect to localhost:5433 from your database client
psql -h localhost -p 5433 -U admin -d mydb
```

**Forward through bastion to a private service:**
```bash
# Access an internal API on port 8080
ssh -L 8080:10.0.2.100:8080 \
    -J ec2-user@BASTION_IP \
    ec2-user@10.0.2.100 \
    -N -f
# Access at localhost:8080
```

Flags:
```text
-L  local port forwarding (your_port:remote_host:remote_port)
-N  don't execute a remote command (tunnel only)
-f  background after authentication
-J  ProxyJump host
```

---

# Topic 4: EC2 Instance Connect

## 10. What Is EC2 Instance Connect?

EC2 Instance Connect pushes a **one-time, short-lived SSH public key** (valid for 60 seconds) to the instance's authorized_keys. You authenticate with a standard SSH session, but you never manage persistent keys.

```text
You
 |
 |-- 1. Push temp public key (60-sec TTL) --> AWS API (ec2-instance-connect)
 |-- 2. SSH to instance using matching private key --> EC2 Instance
```

**Requirements:**
- EC2 Instance Connect agent installed on the instance (pre-installed on Amazon Linux 2, Amazon Linux 2023, Ubuntu 20.04+)
- IAM permission: `ec2-instance-connect:SendSSHPublicKey`
- Security group still needs TCP 22 open (to your IP or the EC2 Instance Connect service IPs)

## 11. Using EC2 Instance Connect

**Via AWS Console:**
- EC2 → Instances → Connect → EC2 Instance Connect → Connect
- Browser-based terminal, no key file needed

**Via AWS CLI:**
```bash
# Push a temporary public key (TTL: 60 seconds)
aws ec2-instance-connect send-ssh-public-key \
  --instance-id i-0123456789abcdef0 \
  --availability-zone us-east-1a \
  --instance-os-user ec2-user \
  --ssh-public-key file://~/.ssh/temp-key.pub

# SSH within 60 seconds
ssh -i ~/.ssh/temp-key ec2-user@<PUBLIC_IP>
```

**Automated with the EC2 Instance Connect CLI (`mssh`):**
```bash
pip install ec2instanceconnectcli
mssh ec2-user@i-0123456789abcdef0 --region us-east-1
# Handles key generation, push, and SSH in one command
```

**EC2 Instance Connect Endpoint (no public IP required):**
```bash
# Access instances in private subnets without a bastion
# Create the endpoint in your VPC first (one per VPC)
aws ec2 create-instance-connect-endpoint \
  --subnet-id subnet-0123456789abcdef0 \
  --security-group-ids sg-0123456789abcdef0

# Connect using the endpoint
aws ec2-instance-connect ssh \
  --instance-id i-0123456789abcdef0 \
  --os-user ec2-user
```

**Advantages over traditional SSH key pairs:**
```text
No persistent keys to manage, rotate, or revoke
Access controlled entirely by IAM policy
Every connection attempt is logged in CloudTrail
Works through AWS Console with no client tooling
EC2 Instance Connect Endpoint eliminates bastion hosts entirely for many use cases
```

---

# Topic 5: SSM Session Manager — No SSH Required

## 12. What Is SSM Session Manager?

SSM Session Manager opens an encrypted shell session to EC2 instances using AWS Systems Manager. There is **no SSH daemon required**, **no port 22 open**, and **no key pairs**.

```text
You (browser or CLI)
      |
      | HTTPS (port 443 outbound only)
      ↓
AWS Systems Manager
      |
      | SSM Agent (pre-installed, polls SSM endpoint)
      ↓
EC2 Instance (private, no public IP needed)
```

**What replaces what:**
```text
Traditional SSH   → SSM Session Manager
Bastion host      → SSM Session Manager + PrivateLink (optional)
Port 22 open      → Port 443 outbound only (from instance)
SSH key pair      → IAM policy (ssm:StartSession)
SSH audit logs    → CloudWatch Logs + S3 session logs
```

## 13. Prerequisites for Session Manager

**1. SSM Agent running on the instance:**
```bash
# Check on the instance
sudo systemctl status amazon-ssm-agent

# Install on Amazon Linux 2023 (usually pre-installed)
sudo yum install -y amazon-ssm-agent
sudo systemctl enable amazon-ssm-agent
sudo systemctl start amazon-ssm-agent
```

**2. IAM Instance Profile with SSM permissions:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:UpdateInstanceInformation",
        "ssmmessages:CreateControlChannel",
        "ssmmessages:CreateDataChannel",
        "ssmmessages:OpenControlChannel",
        "ssmmessages:OpenDataChannel"
      ],
      "Resource": "*"
    }
  ]
}
```

Or attach the AWS managed policy: `AmazonSSMManagedInstanceCore`

**3. IAM policy for the user/role starting sessions:**
```json
{
  "Effect": "Allow",
  "Action": [
    "ssm:StartSession",
    "ssm:DescribeSessions",
    "ssm:GetConnectionStatus",
    "ssm:DescribeInstanceProperties"
  ],
  "Resource": [
    "arn:aws:ec2:*:*:instance/i-*",
    "arn:aws:ssm:*:*:document/AWS-StartSSHSession",
    "arn:aws:ssm:*:*:document/AWS-StartInteractiveCommand"
  ]
}
```

## 14. Starting an SSM Session

**Via AWS Console:**
- EC2 → Instances → Connect → Session Manager → Connect
- Full browser-based terminal

**Via AWS CLI:**
```bash
# Install Session Manager plugin for the CLI first
# https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html

# macOS:
curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/mac/session-manager-plugin.pkg" \
  -o "session-manager-plugin.pkg"
sudo installer -pkg session-manager-plugin.pkg -target /

# Start a session
aws ssm start-session --target i-0123456789abcdef0

# Connect to a specific shell
aws ssm start-session \
  --target i-0123456789abcdef0 \
  --document-name AWS-StartInteractiveCommand \
  --parameters command="bash -l"
```

## 15. Port Forwarding via SSM (No Bastion Needed)

Forward a remote port to your local machine through SSM — no open security group ports.

**Forward RDS port locally:**
```bash
aws ssm start-session \
  --target i-0123456789abcdef0 \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{
    "host": ["my-rds.cluster-xyz.us-east-1.rds.amazonaws.com"],
    "portNumber": ["5432"],
    "localPortNumber": ["5433"]
  }'

# Now connect to localhost:5433
psql -h localhost -p 5433 -U admin -d mydb
```

**Forward an application port:**
```bash
aws ssm start-session \
  --target i-0123456789abcdef0 \
  --document-name AWS-StartPortForwardingSession \
  --parameters portNumber="8080",localPortNumber="8080"
# Access localhost:8080
```

## 16. SSH over SSM (ProxyCommand)

Use your existing SSH tooling with SSM as the transport — no port 22 required:

**~/.ssh/config:**
```
Host i-* mi-*
  ProxyCommand sh -c \
    "aws ssm start-session \
     --target %h \
     --document-name AWS-StartSSHSession \
     --parameters 'portNumber=%p'"
  User ec2-user
  IdentityFile ~/.ssh/ssm-temp-key.pem
  StrictHostKeyChecking accept-new
```

Then:
```bash
ssh i-0123456789abcdef0   # SSM carries the SSH session — no port 22 needed
scp -i ~/.ssh/ssm-temp-key.pem file.txt i-0123456789abcdef0:/tmp/
```

## 17. Session Manager Logging and Audit

Enable logging for compliance and audit:

**Via SSM Preferences (in Console) or CLI:**
```bash
aws ssm update-document \
  --name "SSM-SessionManagerRunShell" \
  --content '{
    "schemaVersion": "1.0",
    "description": "Default session document",
    "sessionType": "Standard_Stream",
    "inputs": {
      "s3BucketName": "my-audit-bucket",
      "s3KeyPrefix": "ssm-sessions/",
      "cloudWatchLogGroupName": "/ssm/sessions",
      "cloudWatchEncryptionEnabled": true,
      "s3EncryptionEnabled": true
    }
  }' \
  --document-version "\$LATEST"
```

What gets logged:
```text
CloudTrail:
  - ssm:StartSession (who, when, from which IP)
  - ssm:TerminateSession

CloudWatch Logs / S3:
  - Full session transcript (every command typed and output)
  - Session duration, target instance, user identity

This replaces bastion host audit logs entirely.
```

---

# Topic 6: Access Pattern Comparison and Decision Guide

## 18. Choosing the Right Access Method

| Method | Port 22 Open? | Key Pair Required? | IAM Controls? | Audit Log? | Best For |
|---|---|---|---|---|---|
| Direct SSH | Yes (to internet) | Yes | No | No | Local dev, lab |
| SSH via Bastion | Yes (bastion only) | Yes | Partial | Partial | Legacy enterprise |
| EC2 Instance Connect | Yes (to EC2IC IPs or your IP) | No | Yes (IAM) | CloudTrail | Developer access, ephemeral |
| EC2 Instance Connect Endpoint | No | No | Yes (IAM) | CloudTrail | Private subnet access without bastion |
| SSM Session Manager | No | No | Yes (IAM) | CloudTrail + session logs | Production, compliance, regulated |

**The MAANG-level answer for "how do you access EC2 in production?":**
```text
SSM Session Manager — no open ports, no key pairs, access controlled by IAM,
every session logged in CloudTrail and optionally S3/CloudWatch Logs.

For developer convenience in non-prod: EC2 Instance Connect Endpoint.
Bastion hosts are legacy — they require key management, port 22 exposure,
and manual audit log setup. Avoid in new architectures.
```

## 19. Security Hardening for EC2 SSH Access

If you must use SSH:

```bash
# On the instance — harden sshd config
sudo vi /etc/ssh/sshd_config
```

Key settings:
```
PermitRootLogin no          # never allow root login
PasswordAuthentication no   # key-based only, no passwords
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
X11Forwarding no
MaxAuthTries 3
LoginGraceTime 30
AllowUsers ec2-user ubuntu  # whitelist specific users
```

```bash
sudo systemctl restart sshd
```

**Key pair rotation:**
```bash
# On instance: add new public key
echo "ssh-ed25519 AAAAC3Nza... new-key" >> ~/.ssh/authorized_keys

# Test with new key
ssh -i ~/.ssh/new-key.pem ec2-user@<IP>

# Remove old key
sed -i '/old-key-fingerprint/d' ~/.ssh/authorized_keys
```

## 20. Production Checklist

```text
□ No port 22 open to 0.0.0.0/0 in any security group
□ IAM role attached to all EC2 instances (no instance without a role)
□ SSM Session Manager configured as primary access method
□ SSM session logs going to CloudWatch Logs and/or S3
□ MFA required for SSM session start (IAM condition: aws:MultiFactorAuthPresent)
□ EC2 Instance Connect Endpoint deployed for developer access to private subnets
□ No hardcoded SSH keys in automation scripts or AMIs
□ Key pairs rotated on schedule; old keys deleted from authorized_keys
□ VPC endpoint for SSM (com.amazonaws.region.ssm, ssmmessages, ec2messages)
    deployed so instances don't need internet access to reach SSM
□ CloudTrail enabled — all management API calls including ssm:StartSession logged
```

---

# Topic 7: VPC Endpoints for SSM (Air-Gapped Private Subnets)

## 21. SSM Without Internet Access

Instances in fully private subnets (no NAT Gateway, no internet route) need VPC endpoints to reach SSM:

```bash
# Create the three required VPC endpoints
for svc in ssm ssmmessages ec2messages; do
  aws ec2 create-vpc-endpoint \
    --vpc-id vpc-0123456789abcdef0 \
    --vpc-endpoint-type Interface \
    --service-name "com.amazonaws.us-east-1.$svc" \
    --subnet-ids subnet-0123456789abcdef0 \
    --security-group-ids sg-0123456789abcdef0 \
    --private-dns-enabled
done
```

Endpoints required:
```text
com.amazonaws.<region>.ssm          — SSM service
com.amazonaws.<region>.ssmmessages  — session messaging
com.amazonaws.<region>.ec2messages  — EC2 messages for SSM
(optional) com.amazonaws.<region>.s3  — if logging to S3
```

---

# Topic 8: Interview-Ready Summary

## 22. Top Interview Questions

**Q: How do you access EC2 instances in a private subnet without a bastion host?**
```text
Two options:

1. SSM Session Manager — install SSM Agent, attach IAM role with
   AmazonSSMManagedInstanceCore, and use 'aws ssm start-session'.
   No open ports, no key pairs needed.

2. EC2 Instance Connect Endpoint — create one endpoint per VPC, then use
   'aws ec2-instance-connect ssh'. Ephemeral keys, IAM-controlled.

Both options require VPC endpoints to SSM/EC2IC if there is no internet access.
```

**Q: What is the difference between EC2 Instance Connect and SSM Session Manager?**
```text
EC2 Instance Connect:
  - Pushes a 60-second ephemeral SSH public key to the instance
  - Still uses the SSH protocol (port 22 must be reachable from EC2 Instance Connect IPs)
  - EC2 Instance Connect Endpoint removes the port 22 requirement
  - Good for ad-hoc developer access

SSM Session Manager:
  - No SSH at all — SSM Agent opens a control channel over HTTPS (port 443)
  - No port 22, no key pairs, no security group inbound rules needed
  - Full session transcript logging built in
  - Preferred for production and compliance-sensitive environments
```

**Q: What happens if someone loses an EC2 SSH key pair?**
```text
Options to regain access:

1. Stop the instance, detach the root EBS volume, attach to another instance,
   edit ~/.ssh/authorized_keys to add a new public key, reattach, restart.

2. Use EC2 Instance Connect or SSM Session Manager if the instance already
   has those agents/roles — these bypass SSH key entirely.

3. Use EC2 Systems Manager Run Command to write a new authorized_keys entry.

4. From an AMI: the original key is irrelevant — build a new instance from AMI.

Prevention: never rely on a single key pair. Use SSM Session Manager so
key loss is irrelevant in the first place.
```

**Q: How do you audit who SSH'd into which instance?**
```text
With SSM Session Manager:
  - CloudTrail logs ssm:StartSession with user identity, IP, timestamp, target instance
  - Session transcript goes to CloudWatch Logs and/or S3
  - Session duration and termination also logged

With traditional SSH:
  - /var/log/auth.log (Ubuntu) or /var/log/secure (Amazon Linux) on the instance
  - But these are instance-local, can be deleted, and require log shipping to be reliable
  - CloudTrail does NOT log SSH sessions — only SSM API calls

SSM Session Manager gives you complete, tamper-resistant audit logging.
```

## 23. Quick Reference Cheat Sheet

```bash
# SSH with key pair
ssh -i ~/.ssh/key.pem ec2-user@<PUBLIC_IP>

# SSH through bastion (ProxyJump)
ssh -J ec2-user@BASTION_IP ec2-user@PRIVATE_IP -i ~/.ssh/key.pem

# EC2 Instance Connect (CLI)
aws ec2-instance-connect send-ssh-public-key \
  --instance-id i-xxx --availability-zone us-east-1a \
  --instance-os-user ec2-user --ssh-public-key file://key.pub

# EC2 Instance Connect (one-liner)
mssh ec2-user@i-0123456789abcdef0 --region us-east-1

# SSM Session Manager
aws ssm start-session --target i-0123456789abcdef0

# SSM port forwarding (RDS example)
aws ssm start-session \
  --target i-0123456789abcdef0 \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters host="rds.host.example",portNumber="5432",localPortNumber="5433"

# SSH over SSM (with ProxyCommand in ~/.ssh/config)
ssh i-0123456789abcdef0

# List active SSM sessions
aws ssm describe-sessions --state Active

# Terminate an SSM session
aws ssm terminate-session --session-id <session-id>

# Check SSM agent status on instance
aws ssm describe-instance-information \
  --filters Key=InstanceIds,Values=i-0123456789abcdef0 \
  --output table
```

---

*Last updated: 2026-07 | Part of AWS Interview Track — Security and Identity*
