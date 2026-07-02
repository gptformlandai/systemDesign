# AWS CLI and Developer Tooling Gold Sheet

> Track: AWS Interview Track — Foundations
> Goal: use the AWS CLI fluently from first install through production scripting and automation; understand CloudShell, profiles, MFA, pagination, and query filtering as a senior engineer would.

---

## 0. How To Read This

Beginner focus:
- Install, configure, and run your first aws command
- Understand credentials and regions
- Read output in table and JSON formats

Intermediate focus:
- Named profiles and environment variables
- --query with JMESPath for output filtering
- Scripting loops and automation patterns

Senior / MAANG focus:
- MFA, role assumption, and temporary credentials
- Pagination and waiters in production scripts
- CloudShell, SSO (IAM Identity Center), and CI/CD credential injection
- Security: no long-term keys in scripts or containers

---

# Topic 1: Installation and Initial Setup

## 1. Install AWS CLI v2

AWS CLI v2 is the current supported version. v1 is deprecated.

**macOS:**
```bash
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /
aws --version
# aws-cli/2.x.x Python/3.x.x Darwin/...
```

**Linux (x86_64):**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
aws --version
```

**Windows:**
```powershell
# Download and run the MSI installer from:
# https://awscli.amazonaws.com/AWSCLIV2.msi
# Or via winget:
winget install -e --id Amazon.AWSCLI
```

**Verify:**
```bash
aws --version
which aws
# Confirm v2 — v2 has built-in auto-complete and binary downloads
```

## 2. First Configuration

```bash
aws configure
# AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
# AWS Secret Access Key [None]: wJalrXUtn...
# Default region name [None]: us-east-1
# Default output format [None]: json
```

This writes to two files:
```
~/.aws/credentials     # access key + secret key
~/.aws/config          # region, output format, and profile settings
```

Credential file:
```ini
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

Config file:
```ini
[default]
region = us-east-1
output = json
```

**Interview line:**
```text
Never store long-term IAM User access keys in production systems.
Use IAM roles for EC2/ECS/Lambda, OIDC for CI/CD, and IAM Identity Center (SSO) for human access.
Long-term keys in ~/.aws/credentials are only for local development.
```

---

# Topic 2: Output Formats and Query Filtering

## 3. Output Formats

```bash
# JSON (default) — machine-readable, good for scripting
aws ec2 describe-instances --output json

# Table — human-readable, good for quick inspection
aws ec2 describe-instances --output table

# Text — tab-separated, good for shell parsing with awk/cut
aws ec2 describe-instances --output text

# YAML — readable structured output
aws ec2 describe-instances --output yaml
```

Set default output globally:
```bash
aws configure set output table
```

Or override per command:
```bash
aws s3 ls --output table
```

## 4. JMESPath Filtering with --query

`--query` uses JMESPath syntax to filter and reshape JSON output before it leaves the CLI.

**List all instance IDs:**
```bash
aws ec2 describe-instances \
  --query "Reservations[*].Instances[*].InstanceId" \
  --output text
```

**Get instance ID + state:**
```bash
aws ec2 describe-instances \
  --query "Reservations[*].Instances[*].{ID:InstanceId,State:State.Name}" \
  --output table
```

**Filter running instances only:**
```bash
aws ec2 describe-instances \
  --filters "Name=instance-state-name,Values=running" \
  --query "Reservations[*].Instances[*].{ID:InstanceId,Type:InstanceType,IP:PublicIpAddress}" \
  --output table
```

**Get specific S3 bucket names:**
```bash
aws s3api list-buckets \
  --query "Buckets[?starts_with(Name,'prod')].Name" \
  --output text
```

**Extract a single value (no array):**
```bash
aws sts get-caller-identity --query "Account" --output text
# 123456789012
```

JMESPath cheat sheet:
```
Reservations[*]              all items in array
.Instances[0]                first item
.{Key:Field}                 project to new key name
[?State.Name=='running']     filter expression
[?contains(Name,'prod')]     string filter
sort_by(@, &LaunchTime)      sort
```

---

# Topic 3: Core Service Commands — Beginner Reference

## 5. Identity and Account

```bash
# Who am I?
aws sts get-caller-identity

# What region/profile am I using?
aws configure list

# List all configured profiles
aws configure list-profiles
```

## 6. S3 Commands

```bash
# List buckets
aws s3 ls

# List contents of a bucket
aws s3 ls s3://my-bucket/
aws s3 ls s3://my-bucket/prefix/ --recursive

# Copy file to S3
aws s3 cp ./file.txt s3://my-bucket/path/file.txt

# Download file from S3
aws s3 cp s3://my-bucket/path/file.txt ./local-file.txt

# Sync directory to S3 (like rsync)
aws s3 sync ./dist/ s3://my-bucket/dist/ --delete

# Remove object
aws s3 rm s3://my-bucket/path/file.txt

# Move object
aws s3 mv s3://my-bucket/old-key s3://my-bucket/new-key

# Make bucket
aws s3 mb s3://my-new-bucket --region us-east-1

# Get object metadata without downloading
aws s3api head-object --bucket my-bucket --key path/file.txt
```

## 7. EC2 Commands

```bash
# List instances
aws ec2 describe-instances \
  --query "Reservations[*].Instances[*].{ID:InstanceId,State:State.Name,Type:InstanceType,IP:PublicIpAddress}" \
  --output table

# Start / stop / terminate
aws ec2 start-instances --instance-ids i-0123456789abcdef0
aws ec2 stop-instances --instance-ids i-0123456789abcdef0
aws ec2 terminate-instances --instance-ids i-0123456789abcdef0

# Reboot
aws ec2 reboot-instances --instance-ids i-0123456789abcdef0

# Get instance console output
aws ec2 get-console-output --instance-id i-0123456789abcdef0 --output text

# Describe security groups
aws ec2 describe-security-groups \
  --group-names my-sg \
  --query "SecurityGroups[*].{Name:GroupName,ID:GroupId}"

# Create key pair
aws ec2 create-key-pair --key-name my-key --query "KeyMaterial" --output text > my-key.pem
chmod 400 my-key.pem

# List key pairs
aws ec2 describe-key-pairs --query "KeyPairs[*].KeyName" --output table

# Describe AMIs (your own)
aws ec2 describe-images --owners self --output table

# Get latest Amazon Linux 2023 AMI (us-east-1)
aws ec2 describe-images \
  --owners amazon \
  --filters "Name=name,Values=al2023-ami-*-x86_64" \
            "Name=state,Values=available" \
  --query "sort_by(Images, &CreationDate)[-1].ImageId" \
  --output text
```

## 8. IAM Commands

```bash
# List users
aws iam list-users --query "Users[*].UserName" --output table

# List roles
aws iam list-roles --query "Roles[*].{Name:RoleName,ARN:Arn}" --output table

# Get current user/role details
aws sts get-caller-identity

# List attached policies on a role
aws iam list-attached-role-policies --role-name my-role

# Get role's inline policies
aws iam list-role-policies --role-name my-role

# Assume a role (get temp credentials)
aws sts assume-role \
  --role-arn "arn:aws:iam::123456789012:role/MyRole" \
  --role-session-name "MySession"
```

## 9. Lambda Commands

```bash
# List functions
aws lambda list-functions \
  --query "Functions[*].{Name:FunctionName,Runtime:Runtime,Memory:MemorySize}" \
  --output table

# Invoke a function (synchronous)
aws lambda invoke \
  --function-name my-function \
  --payload '{"key": "value"}' \
  --cli-binary-format raw-in-base64-out \
  response.json
cat response.json

# Get function configuration
aws lambda get-function-configuration --function-name my-function

# Update function code (from zip)
aws lambda update-function-code \
  --function-name my-function \
  --zip-file fileb://function.zip

# Update environment variable
aws lambda update-function-configuration \
  --function-name my-function \
  --environment "Variables={ENV=prod,DB_HOST=mydb.cluster.example}"

# Get function logs (last 10 min)
aws logs filter-log-events \
  --log-group-name "/aws/lambda/my-function" \
  --start-time $(date -d '-10 minutes' +%s000) \
  --output text
```

## 10. CloudWatch Logs

```bash
# List log groups
aws logs describe-log-groups \
  --query "logGroups[*].logGroupName" \
  --output text

# Tail logs (CloudWatch Logs Insights query)
aws logs tail /aws/lambda/my-function --follow

# Filter log events
aws logs filter-log-events \
  --log-group-name "/aws/lambda/my-function" \
  --filter-pattern "ERROR" \
  --start-time 1700000000000

# Run Logs Insights query
aws logs start-query \
  --log-group-name "/aws/lambda/my-function" \
  --start-time $(date -d '-1 hour' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, @message | filter @message like /ERROR/ | limit 20'
# Returns queryId — then:
aws logs get-query-results --query-id <queryId>
```

---

# Topic 4: Named Profiles and Multi-Account Management

## 11. Named Profiles

Use named profiles to manage multiple AWS accounts and roles.

**Create a profile:**
```bash
aws configure --profile prod
# Enter prod account's access key, secret, region, output
```

**Use a profile:**
```bash
aws s3 ls --profile prod
aws ec2 describe-instances --profile staging
```

**Set profile as environment variable (session-scoped):**
```bash
export AWS_PROFILE=prod
aws s3 ls    # uses prod profile
unset AWS_PROFILE
```

Config file with multiple profiles:
```ini
[default]
region = us-east-1
output = json

[profile dev]
region = us-west-2
output = table

[profile prod]
region = us-east-1
output = json

[profile prod-admin]
role_arn = arn:aws:iam::PROD_ACCOUNT:role/AdminRole
source_profile = prod
role_session_name = my-prod-session
mfa_serial = arn:aws:iam::DEV_ACCOUNT:mfa/myuser
```

## 12. Environment Variables

Override any config value with environment variables (higher priority than config files):

```bash
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI...
export AWS_SESSION_TOKEN=AQoXnyc4LLI...     # needed with temp credentials
export AWS_DEFAULT_REGION=us-west-2
export AWS_DEFAULT_OUTPUT=table
export AWS_PROFILE=prod
```

Priority order (highest to lowest):
```
1. Command line options (--region, --profile)
2. Environment variables (AWS_DEFAULT_REGION, AWS_PROFILE)
3. Config file (~/.aws/config)
4. Credentials file (~/.aws/credentials)
5. EC2/ECS/Lambda instance metadata (for IAM roles)
```

## 13. MFA and Role Assumption

**Assume role with MFA:**
```bash
aws sts assume-role \
  --role-arn "arn:aws:iam::123456789012:role/ProdAdmin" \
  --role-session-name "MFASession" \
  --serial-number "arn:aws:iam::111111111111:mfa/myuser" \
  --token-code 123456

# Extract and export the temporary credentials
CREDS=$(aws sts assume-role ...)
export AWS_ACCESS_KEY_ID=$(echo $CREDS | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDS | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDS | jq -r '.Credentials.SessionToken')
```

**Auto-refresh via ~/.aws/config:**
```ini
[profile prod-mfa]
role_arn = arn:aws:iam::123456789012:role/ProdAdmin
source_profile = default
mfa_serial = arn:aws:iam::111111111111:mfa/myuser
duration_seconds = 3600
```

Then:
```bash
aws ec2 describe-instances --profile prod-mfa
# CLI prompts you for the MFA token automatically
```

---

# Topic 5: Pagination, Waiters, and Dry Run

## 14. Handling Pagination

AWS APIs paginate large results. The CLI handles this automatically with `--no-paginate` or manual tokens.

**Auto-paginate (CLI fetches all pages automatically):**
```bash
# CLI fetches all pages by default — but can be slow and hit memory for large datasets
aws ec2 describe-instances --output json

# Turn off auto-pagination and get one page at a time:
aws ec2 describe-instances --no-paginate --max-results 50
```

**Manual pagination:**
```bash
# First page
aws ec2 describe-instances --max-results 5 > page1.json

# Next page using NextToken
NEXT=$(cat page1.json | jq -r '.NextToken')
aws ec2 describe-instances --max-results 5 --next-token "$NEXT" > page2.json
```

**S3 pagination:**
```bash
# s3api uses continuation tokens
aws s3api list-objects-v2 \
  --bucket my-bucket \
  --max-keys 100 \
  --query "{Contents:Contents[*].Key,Token:NextContinuationToken}"
```

## 15. Waiters

Waiters poll until a resource reaches the desired state — essential in scripts.

```bash
# Wait for instance to be running (polls every 15 sec, max 40 attempts)
aws ec2 wait instance-running --instance-ids i-0123456789abcdef0
echo "Instance is running"

# Wait for instance to stop
aws ec2 wait instance-stopped --instance-ids i-0123456789abcdef0

# Wait for S3 bucket to exist
aws s3api wait bucket-exists --bucket my-new-bucket

# Wait for CloudFormation stack creation to complete
aws cloudformation wait stack-create-complete \
  --stack-name my-stack

# Wait for ECS service to be stable
aws ecs wait services-stable \
  --cluster my-cluster \
  --services my-service
```

Built-in waiters exist for: EC2, S3, CloudFormation, ECS, RDS, ElastiCache, DynamoDB, and more.

## 16. Dry Run

Use `--dry-run` to validate IAM permissions without making real API calls:

```bash
# Check if you have permission to start an instance (no actual start)
aws ec2 start-instances \
  --instance-ids i-0123456789abcdef0 \
  --dry-run

# Returns: An error occurred (DryRunOperation) ... Request would have succeeded
# or:      An error occurred (UnauthorizedOperation) ... You are not authorized
```

Available on most EC2 write operations: `run-instances`, `stop-instances`, `terminate-instances`, `copy-image`, `create-snapshot`.

---

# Topic 6: Scripting and Automation Patterns

## 17. Shell Scripting with AWS CLI

**Pattern 1 — Get and act on instance IDs:**
```bash
#!/bin/bash
# Stop all running instances tagged with Environment=dev
INSTANCE_IDS=$(aws ec2 describe-instances \
  --filters "Name=tag:Environment,Values=dev" \
             "Name=instance-state-name,Values=running" \
  --query "Reservations[*].Instances[*].InstanceId" \
  --output text)

if [ -z "$INSTANCE_IDS" ]; then
  echo "No running dev instances found"
  exit 0
fi

echo "Stopping: $INSTANCE_IDS"
aws ec2 stop-instances --instance-ids $INSTANCE_IDS
aws ec2 wait instance-stopped --instance-ids $INSTANCE_IDS
echo "All dev instances stopped"
```

**Pattern 2 — Deploy Lambda function:**
```bash
#!/bin/bash
set -euo pipefail

FUNCTION_NAME="my-api"
ZIP_FILE="function.zip"

echo "Packaging..."
zip -r $ZIP_FILE . -x "*.git*" "node_modules/.cache/*"

echo "Updating Lambda code..."
aws lambda update-function-code \
  --function-name "$FUNCTION_NAME" \
  --zip-file "fileb://$ZIP_FILE" \
  --output text \
  --query "FunctionArn"

echo "Waiting for update to complete..."
aws lambda wait function-updated \
  --function-name "$FUNCTION_NAME"

echo "Publishing new version..."
VERSION=$(aws lambda publish-version \
  --function-name "$FUNCTION_NAME" \
  --query "Version" \
  --output text)

echo "Deployed version: $VERSION"
```

**Pattern 3 — Loop over S3 objects:**
```bash
#!/bin/bash
BUCKET="my-data-bucket"
PREFIX="reports/"

# Process each object
aws s3 ls "s3://$BUCKET/$PREFIX" --recursive \
  | awk '{print $4}' \
  | while read -r key; do
      echo "Processing: $key"
      aws s3 cp "s3://$BUCKET/$key" "./local/$(basename $key)"
    done
```

**Pattern 4 — Parameterized deploy with jq:**
```bash
#!/bin/bash
# Update ECS service desired count
CLUSTER=$1
SERVICE=$2
COUNT=$3

aws ecs update-service \
  --cluster "$CLUSTER" \
  --service "$SERVICE" \
  --desired-count "$COUNT" \
  --output json | jq '.service | {name: .serviceName, desiredCount: .desiredCount}'

aws ecs wait services-stable \
  --cluster "$CLUSTER" \
  --services "$SERVICE"
echo "Service $SERVICE is stable at $COUNT tasks"
```

## 18. CI/CD Credential Best Practices

**GitHub Actions (OIDC — no long-term keys):**
```yaml
permissions:
  id-token: write
  contents: read

steps:
  - name: Configure AWS credentials
    uses: aws-actions/configure-aws-credentials@v4
    with:
      role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
      aws-region: us-east-1

  - name: Deploy
    run: |
      aws s3 sync ./dist/ s3://my-bucket/
      aws cloudfront create-invalidation --distribution-id ABCDEF --paths "/*"
```

**Container-based (ECS/EKS) — use IAM Roles, not env vars:**
```text
ECS Task Role    → task-level permissions (recommended for tasks)
EC2 Instance Profile → instance-level permissions
IRSA (EKS)       → pod-level role via service account annotation
```

**Never do this in production:**
```bash
# WRONG — keys in environment of a container
docker run -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... myapp

# WRONG — keys hardcoded in scripts
AWS_ACCESS_KEY_ID=AKIA... aws s3 ls
```

---

# Topic 7: Advanced CLI Features

## 19. AWS CloudShell

CloudShell is a browser-based CLI environment built into the AWS Console — zero setup required.

```text
Access: AWS Console → top nav → CloudShell icon (>_)

Included tools:
  - AWS CLI v2 (pre-authenticated as your console user)
  - Python 3, Node.js, jq, git, vim, pip, npm
  - 1 GB persistent home directory storage per region

Use cases:
  - Quick ad-hoc commands without local CLI setup
  - Running scripts from a trusted environment
  - Operations from restricted networks
  - Demos and onboarding

Limits:
  - No root access
  - 20 min inactivity timeout
  - 1 GB storage
  - Not for long-running batch jobs
```

## 20. IAM Identity Center (SSO) Integration

For organizations using AWS SSO (IAM Identity Center):

```bash
# Configure SSO profile
aws configure sso
# SSO start URL: https://my-org.awsapps.com/start
# SSO region: us-east-1
# Choose account and role interactively

# Login (browser-based)
aws sso login --profile my-sso-profile

# Use the profile
aws s3 ls --profile my-sso-profile

# Logout
aws sso logout --profile my-sso-profile
```

Config produced (`~/.aws/config`):
```ini
[profile dev-readonly]
sso_session = my-org
sso_account_id = 123456789012
sso_role_name = ReadOnlyAccess
region = us-east-1
output = table

[sso-session my-org]
sso_start_url = https://my-org.awsapps.com/start
sso_region = us-east-1
sso_registration_scopes = sso:account:access
```

## 21. AWS CLI Auto-Completion

```bash
# bash
complete -C '/usr/local/bin/aws_completer' aws
echo "complete -C '/usr/local/bin/aws_completer' aws" >> ~/.bashrc

# zsh (add to ~/.zshrc)
autoload bashcompinit && bashcompinit
complete -C '/usr/local/bin/aws_completer' aws

# Fish shell
~/.config/fish/completions/aws.fish
```

Usage:
```bash
aws ec2 describe-<TAB>      # shows all describe-* subcommands
aws s3 cp --<TAB>           # shows all flags for cp
```

## 22. Useful Global Flags

| Flag | Purpose |
|---|---|
| `--region us-west-2` | override region for this command |
| `--profile myprofile` | use named profile |
| `--output json\|table\|text\|yaml` | override output format |
| `--query "..."` | JMESPath filter on output |
| `--no-verify-ssl` | skip TLS verification (use only in lab) |
| `--cli-input-json file://input.json` | pass large input from file |
| `--generate-cli-skeleton` | output empty input JSON template |
| `--debug` | verbose debug output (shows HTTP calls) |
| `--no-paginate` | disable auto-pagination |
| `--dry-run` | validate permissions without action (EC2) |
| `--endpoint-url http://localhost:4566` | use LocalStack or custom endpoint |

## 23. LocalStack for Local Development

LocalStack runs a local mock of AWS services in Docker:

```bash
# Start LocalStack
docker run --rm -it \
  -p 4566:4566 \
  -p 4510-4559:4510-4559 \
  localstack/localstack

# Point CLI at LocalStack
aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket
aws --endpoint-url=http://localhost:4566 s3 ls

# Use a profile for LocalStack
# ~/.aws/config:
[profile localstack]
region = us-east-1
output = json
endpoint_url = http://localhost:4566

# ~/.aws/credentials:
[localstack]
aws_access_key_id = test
aws_secret_access_key = test

aws s3 ls --profile localstack
```

---

# Topic 8: Debugging and Troubleshooting

## 24. Debug Mode

```bash
# Show full HTTP request/response
aws s3 ls --debug 2>&1 | head -100

# Check endpoint being called
aws ec2 describe-instances --debug 2>&1 | grep endpoint

# Check which credentials are loaded
aws configure list

# Check who you're authenticated as
aws sts get-caller-identity
```

## 25. Common Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `Unable to locate credentials` | No credentials configured | Run `aws configure` or set env vars |
| `InvalidClientTokenId` | Wrong or expired access key ID | Re-run `aws configure`, check key is active |
| `AuthFailure` | Wrong secret key or signature mismatch | Re-enter secret key; check system clock |
| `ExpiredToken` | Session token expired | Refresh STS credentials or SSO login |
| `AccessDenied` | Missing IAM permission | Check IAM policy; use `--dry-run` to test |
| `NoSuchBucket` | Bucket doesn't exist or wrong region | Check bucket name and `--region` |
| `SignatureDoesNotMatch` | System clock skew | Sync system time (`ntpdate`) |
| `An error occurred (RequestExpired)` | Clock drift >15 min | Fix system clock |
| `Could not connect to the endpoint URL` | Network/VPN issue or wrong region | Check connectivity, `--endpoint-url` |

## 26. Check Effective Permissions

```bash
# What can I do?
aws iam simulate-principal-policy \
  --policy-source-arn "arn:aws:iam::123456789012:role/MyRole" \
  --action-names "s3:GetObject" \
  --resource-arns "arn:aws:s3:::my-bucket/*"

# Access Advisor — what was last accessed
aws iam generate-service-last-accessed-details \
  --arn "arn:aws:iam::123456789012:role/MyRole"
# Then:
aws iam get-service-last-accessed-details --job-id <job-id>
```

---

# Topic 9: Interview-Ready Summary

## 27. Top Interview Questions

**Q: How do you authenticate AWS CLI in a CI/CD pipeline?**
```text
Use OIDC with your CI provider (GitHub Actions, GitLab CI, CircleCI).
The CI job assumes an IAM role via a trust policy that validates the OIDC token.
No long-term access keys — credentials are short-lived and scoped per workflow.
```

**Q: What's the priority order for AWS CLI credential resolution?**
```text
1. Command line: --profile
2. Env vars: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN
3. ~/.aws/credentials file
4. ~/.aws/config file (credential_process, sso settings)
5. Container credential provider (ECS task role metadata)
6. EC2 instance metadata service (IMDS) for IAM role
```

**Q: How do you handle pagination in scripts?**
```text
For simple scripts: rely on AWS CLI auto-pagination (default behavior fetches all pages).
For large datasets: use --no-paginate and loop with NextToken/ContinuationToken.
For production scripts: check for truncated responses and handle them explicitly.
```

**Q: What is the difference between aws s3 and aws s3api?**
```text
aws s3 — high-level commands (cp, ls, sync, mv, rm, mb). Good for daily use.
aws s3api — raw API calls (put-object, get-object, list-objects-v2, head-bucket).
Use s3api when you need precise control: server-side encryption parameters,
custom metadata, specific ACLs, multipart uploads, pre-signed URLs.
```

**Q: How do you test IAM permissions without making real changes?**
```text
Use --dry-run flag on supported EC2 commands.
Use aws iam simulate-principal-policy for any action/resource pair.
Use access analyzer to detect over-permissioned roles.
```

## 28. Quick Reference Cheat Sheet

```bash
# Identity
aws sts get-caller-identity

# S3
aws s3 ls s3://bucket/
aws s3 cp file.txt s3://bucket/
aws s3 sync ./dist/ s3://bucket/dist/ --delete

# EC2
aws ec2 describe-instances --output table
aws ec2 start-instances --instance-ids i-xxx
aws ec2 wait instance-running --instance-ids i-xxx

# Lambda
aws lambda list-functions --output table
aws lambda invoke --function-name fn --payload '{}' out.json

# Logs
aws logs tail /aws/lambda/fn --follow

# CloudFormation
aws cloudformation describe-stacks --output table
aws cloudformation wait stack-create-complete --stack-name my-stack

# Filter output
aws ec2 describe-instances \
  --filters "Name=instance-state-name,Values=running" \
  --query "Reservations[*].Instances[*].{ID:InstanceId,IP:PublicIpAddress}" \
  --output table

# Profiles
aws s3 ls --profile prod
export AWS_PROFILE=prod

# Debug
aws s3 ls --debug 2>&1 | head -50
aws configure list
```

---

*Last updated: 2026-07 | AWS CLI v2 | Part of AWS Interview Track — Foundations*
