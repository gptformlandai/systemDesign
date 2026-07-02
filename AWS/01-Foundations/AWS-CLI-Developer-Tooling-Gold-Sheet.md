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

Outcome:
```text
Beginner: I can safely authenticate, identify my account, and run read-only commands.
Intermediate: I can inspect, filter, deploy, and debug resources across profiles/accounts.
Senior: I can automate repeatable operations with temporary credentials, guardrails, waits, retries, logs, and cleanup.
```

Golden CLI rule:
```text
Before every write command, know:
1. Which account?
2. Which region?
3. Which profile or role?
4. Which resource ARN/name?
5. What will be created, modified, deleted, or billed?
6. How will I verify it?
7. How will I roll it back or clean it up?
```

---

# Topic 1: Installation and Initial Setup

## 1. Install AWS CLI v2

AWS CLI v2 is the current major version and the right default for new learning and production workflows. Prefer the official AWS installer over random package-manager builds when you need predictable behavior.

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
# Confirm v2: aws-cli/2.x.x ...
```

Version sanity check:
```bash
aws --version
aws help
aws ec2 help
aws ec2 describe-instances help
```

Mental model:
```text
aws <service> <operation> [parameters] [global flags]

Examples:
aws sts get-caller-identity
aws ec2 describe-instances --region us-east-1 --profile dev
aws s3 cp ./app.zip s3://my-bucket/releases/app.zip
```

## 2. First Configuration

For learning in a personal sandbox, `aws configure` is simple. For company accounts, prefer IAM Identity Center (`aws configure sso`) so humans do not manage long-lived access keys.

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
Long-term keys in ~/.aws/credentials are only acceptable for tightly controlled learning/sandbox use.
```

## 2.1 Recommended Human Setup: IAM Identity Center / SSO

Most real companies do not want engineers creating IAM users and access keys. They want centralized human login, MFA, short-lived credentials, and account/permission-set assignments.

Configure an SSO-backed profile:
```bash
aws configure sso
# SSO session name: my-company
# SSO start URL: https://my-company.awsapps.com/start
# SSO region: us-east-1
# SSO registration scopes: sso:account:access
# Choose account
# Choose permission set / role
# Default client Region: us-east-1
# CLI default output format: json
# Profile name: dev-poweruser
```

Login and verify:
```bash
aws sso login --profile dev-poweruser
aws sts get-caller-identity --profile dev-poweruser
aws configure list --profile dev-poweruser
```

Logout:
```bash
aws sso logout --profile dev-poweruser
```

Typical `~/.aws/config` result:
```ini
[profile dev-poweruser]
sso_session = my-company
sso_account_id = 111122223333
sso_role_name = PowerUserAccess
region = us-east-1
output = json

[sso-session my-company]
sso_start_url = https://my-company.awsapps.com/start
sso_region = us-east-1
sso_registration_scopes = sso:account:access
```

Production instinct:
```text
Use SSO profiles for humans.
Use workload roles for apps.
Use OIDC role assumption for CI/CD.
Avoid IAM user access keys unless there is a documented exception.
```

## 2.2 First Five Commands Every Beginner Must Run

Run these before touching any real resource:

```bash
aws --version
aws configure list
aws configure list-profiles
aws sts get-caller-identity
aws ec2 describe-regions --query "Regions[*].RegionName" --output table
```

What you are checking:
```text
aws --version                 -> CLI installed and version is v2
aws configure list            -> where credentials/region/output come from
aws configure list-profiles   -> available local profiles
sts get-caller-identity       -> account, role/user ARN, and caller identity
describe-regions              -> API call works and region endpoint is reachable
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

Senior habit:
```text
Use service-side filters first, then --query.

Good:
  aws ec2 describe-instances --filters "Name=instance-state-name,Values=running" --query "..."

Risky at scale:
  aws ec2 describe-instances --query "[?State.Name=='running']..."

Why:
  filters reduce API payload and cost/latency; --query reshapes what remains.
```

## 4.1 High-Level vs Low-Level Commands

Some services expose both friendly high-level commands and raw API-shaped commands.

| Need | Use | Example |
|---|---|---|
| Daily S3 copy/sync/list | `aws s3` | `aws s3 sync ./dist s3://bucket/` |
| Exact S3 API features | `aws s3api` | `aws s3api put-object --bucket b --key k --body f` |
| CloudFormation stack lifecycle | `aws cloudformation` | `deploy`, `describe-stacks`, `wait` |
| ECS operational inspection | `aws ecs` | `describe-services`, `list-tasks` |
| CloudWatch Logs live tail | `aws logs` | `tail`, `filter-log-events` |

Rule of thumb:
```text
Use high-level commands for human workflows.
Use low-level API commands when you need exact flags, automation stability, or service behavior that maps directly to AWS APIs.
```

## 4.2 CLI Input JSON and Skeletons

When a command becomes unreadable, move input into JSON.

Generate a template:
```bash
aws lambda create-function --generate-cli-skeleton input > create-function.json
```

Run from file:
```bash
aws lambda create-function --cli-input-json file://create-function.json
```

Why this matters:
```text
Large IAM policies, ECS task definitions, Lambda environment maps, and CloudFormation parameters are easier to review in files than in one huge shell command.
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

# Confirm current caller account only
aws sts get-caller-identity --query "Account" --output text

# Confirm current caller ARN only
aws sts get-caller-identity --query "Arn" --output text
```

Account-safety habit:
```bash
aws sts get-caller-identity --profile prod
aws sts get-caller-identity --profile dev
```

Never rely on terminal prompt memory for production. Make the account identity explicit.

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

# Generate a presigned download URL
aws s3 presign s3://my-bucket/path/file.txt --expires-in 900

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket my-bucket \
  --versioning-configuration Status=Enabled

# List object versions
aws s3api list-object-versions \
  --bucket my-bucket \
  --prefix path/file.txt \
  --query "Versions[*].{Key:Key,VersionId:VersionId,LastModified:LastModified}"

# Block public access at bucket level
aws s3api put-public-access-block \
  --bucket my-bucket \
  --public-access-block-configuration \
  "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

S3 safety note:
```text
aws s3 sync --delete is powerful. It deletes destination objects not present in the source.
Run a read-only listing first, use a sandbox prefix, and keep versioning enabled for important buckets.
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

# Inspect network interfaces for an instance
aws ec2 describe-network-interfaces \
  --filters "Name=attachment.instance-id,Values=i-0123456789abcdef0" \
  --query "NetworkInterfaces[*].{ENI:NetworkInterfaceId,Subnet:SubnetId,PrivateIp:PrivateIpAddress,SGs:Groups[*].GroupId}" \
  --output table

# See what changed in a security group
aws ec2 describe-security-groups \
  --group-ids sg-0123456789abcdef0 \
  --query "SecurityGroups[*].IpPermissions"
```

EC2 write-command safety:
```text
Prefer --dry-run where supported.
Prefer tags on create.
Use waiters after state changes.
Capture instance IDs and ARNs in deployment output.
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

# Decode an encoded authorization failure message
aws sts decode-authorization-message \
  --encoded-message "<encoded-message-from-aws-error>"
```

IAM diagnosis sequence:
```text
1. Who am I? sts get-caller-identity
2. What action failed? exact service:Action from error or CloudTrail
3. What resource ARN failed?
4. Is there explicit deny in SCP, permission boundary, session policy, resource policy, or identity policy?
5. Does KMS/key policy also need to allow the action?
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

macOS note:
```text
The `date -d` syntax is GNU/Linux-specific. On macOS, use an absolute epoch timestamp, install GNU coreutils, or compute timestamps in your shell/runtime before passing them to the CLI.
```

## 10.1 CloudFormation Commands

```bash
# Validate a template
aws cloudformation validate-template \
  --template-body file://template.yaml

# Deploy a stack
aws cloudformation deploy \
  --stack-name my-stack \
  --template-file template.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides Env=dev ImageTag=v1

# Describe stack status
aws cloudformation describe-stacks \
  --stack-name my-stack \
  --query "Stacks[*].{Name:StackName,Status:StackStatus,Updated:LastUpdatedTime}" \
  --output table

# Watch stack events
aws cloudformation describe-stack-events \
  --stack-name my-stack \
  --query "StackEvents[0:10].[Timestamp,LogicalResourceId,ResourceStatus,ResourceStatusReason]" \
  --output table

# Detect drift
aws cloudformation detect-stack-drift --stack-name my-stack

# Wait for deployment
aws cloudformation wait stack-update-complete --stack-name my-stack
```

CloudFormation production rule:
```text
Never ignore stack events. The first failed resource usually explains the real permission, quota, dependency, or validation issue.
```

## 10.2 ECR and ECS Commands

```bash
# Login Docker to ECR
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Create repository
aws ecr create-repository \
  --repository-name myapp-backend \
  --image-scanning-configuration scanOnPush=true \
  --encryption-configuration encryptionType=AES256

# List ECS services
aws ecs list-services --cluster my-cluster

# Describe ECS service deployment state
aws ecs describe-services \
  --cluster my-cluster \
  --services my-service \
  --query "services[*].{Service:serviceName,Desired:desiredCount,Running:runningCount,Pending:pendingCount,Deployments:deployments[*].{Status:status,TaskDef:taskDefinition,Rollout:rolloutState}}" \
  --output json

# List stopped tasks to debug failed deployments
aws ecs list-tasks \
  --cluster my-cluster \
  --service-name my-service \
  --desired-status STOPPED

# Describe stopped task reason
aws ecs describe-tasks \
  --cluster my-cluster \
  --tasks <task-arn> \
  --query "tasks[*].{StoppedReason:stoppedReason,Containers:containers[*].reason}"

# Force a new deployment
aws ecs update-service \
  --cluster my-cluster \
  --service my-service \
  --force-new-deployment

# Wait until stable
aws ecs wait services-stable \
  --cluster my-cluster \
  --services my-service
```

ECS rollback instinct:
```text
Find the previous task definition revision, update the service to that revision, wait for services-stable, then verify ALB target health and CloudWatch error rate.
```

## 10.3 DynamoDB, SQS, SNS, and EventBridge Commands

```bash
# DynamoDB table summary
aws dynamodb describe-table \
  --table-name Orders \
  --query "Table.{Name:TableName,Status:TableStatus,Billing:BillingModeSummary.BillingMode,ItemCount:ItemCount}"

# Put an item
aws dynamodb put-item \
  --table-name Orders \
  --item '{"PK":{"S":"ORDER#1"},"SK":{"S":"META"},"status":{"S":"PAID"}}'

# SQS queue URL
aws sqs get-queue-url --queue-name orders-queue

# Send SQS message
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789012/orders-queue \
  --message-body '{"orderId":"o-123"}'

# Check approximate queue depth
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789012/orders-queue \
  --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible

# Publish SNS message
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:orders \
  --message '{"eventType":"OrderPaid","orderId":"o-123"}'

# Put EventBridge event
aws events put-events \
  --entries '[{"Source":"orders.api","DetailType":"OrderPaid","Detail":"{\"orderId\":\"o-123\"}","EventBusName":"default"}]'
```

## 10.4 Systems Manager Operator Commands

```bash
# Check managed instances
aws ssm describe-instance-information \
  --query "InstanceInformationList[*].{Instance:InstanceId,Platform:PlatformName,Ping:PingStatus,Agent:AgentVersion}" \
  --output table

# Start an SSM Session Manager shell
aws ssm start-session --target i-0123456789abcdef0

# Run a safe read-only command
aws ssm send-command \
  --document-name "AWS-RunShellScript" \
  --targets "Key=instanceids,Values=i-0123456789abcdef0" \
  --parameters 'commands=["uptime","df -h"]' \
  --comment "Read-only health check"

# Fetch command output
aws ssm get-command-invocation \
  --command-id "<command-id>" \
  --instance-id i-0123456789abcdef0

# Read a parameter
aws ssm get-parameter \
  --name "/myapp/dev/db-host" \
  --with-decryption
```

SSM production line:
```text
SSM Session Manager replaces broad SSH exposure. It gives private access, IAM authorization, audit logs, and no inbound port 22 requirement.
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

Override many config values with environment variables. This is useful for temporary shells, but dangerous if you forget what is exported.

```bash
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI...
export AWS_SESSION_TOKEN=AQoXnyc4LLI...     # needed with temp credentials
export AWS_DEFAULT_REGION=us-west-2
export AWS_REGION=us-west-2
export AWS_DEFAULT_OUTPUT=table
export AWS_PROFILE=prod
```

Practical resolution order for most CLI work:
```
1. Explicit command flags where applicable: --profile, --region, --output
2. Environment variables: AWS_PROFILE, AWS_REGION/AWS_DEFAULT_REGION, access keys/session token
3. Named profile config: ~/.aws/config
4. Shared credentials: ~/.aws/credentials
5. Web identity / SSO / credential_process configured by profile
6. Container credentials: ECS task role
7. Instance metadata credentials: EC2 instance profile
8. Lambda execution role credentials in the Lambda runtime
```

Avoid this mistake:
```bash
export AWS_PROFILE=prod
aws s3 ls
# Later...
aws ec2 terminate-instances --instance-ids i-xxx
```

Better:
```bash
aws sts get-caller-identity --profile prod
aws ec2 describe-instances --profile prod --region us-east-1
```

## 13. MFA and Role Assumption

**Assume role with MFA:**
```bash
CREDS=$(aws sts assume-role \
  --role-arn "arn:aws:iam::123456789012:role/ProdAdmin" \
  --role-session-name "MFASession" \
  --serial-number "arn:aws:iam::111111111111:mfa/myuser" \
  --token-code 123456)

# Extract and export the temporary credentials
export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r '.Credentials.SessionToken')
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

Senior preference:
```text
For humans: IAM Identity Center is cleaner than hand-exporting STS credentials.
For break-glass: MFA-backed assume-role profiles are acceptable when documented.
For services: use workload roles; never export permanent keys into app containers.
```

## 13.1 Credential Debugging Flow

When the CLI uses the wrong identity:

```bash
aws configure list
aws configure list-profiles
aws sts get-caller-identity
aws sts get-caller-identity --profile dev
aws sts get-caller-identity --profile prod
```

Check the shell:
```bash
env | grep '^AWS_'
```

Common hidden causes:
```text
AWS_PROFILE still exported from yesterday.
AWS_ACCESS_KEY_ID overrides your intended SSO profile.
The default region is different from the resource's region.
SSO token expired.
The role chain points to the wrong source_profile.
You are inside an EC2/ECS environment and receiving role credentials from metadata.
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

## 16.1 Throttling, Retries, and Timeouts

AWS APIs enforce service quotas and request-rate limits. Production scripts must expect throttling.

Common symptoms:
```text
ThrottlingException
TooManyRequestsException
RequestLimitExceeded
Rate exceeded
ProvisionedThroughputExceededException
SlowDown
```

Practical response:
```text
Use SDK retries for application code.
For CLI scripts, reduce parallelism, batch requests, add backoff, and use waiters.
Check Service Quotas before assuming the design can scale.
Use CloudWatch metrics to confirm throttling rather than guessing.
```

CLI retry config:
```bash
aws configure set retry_mode standard
aws configure set max_attempts 5
```

Per-command environment override:
```bash
AWS_RETRY_MODE=adaptive AWS_MAX_ATTEMPTS=8 aws dynamodb describe-table --table-name Orders
```

Service Quotas examples:
```bash
# List service quota services
aws service-quotas list-services --output table

# List Lambda quotas
aws service-quotas list-service-quotas \
  --service-code lambda \
  --query "Quotas[*].{Name:QuotaName,Value:Value,Adjustable:Adjustable}" \
  --output table
```

Interview line:
```text
Senior AWS automation treats throttling as normal backpressure, not a surprise. I use retries with backoff, waiters, quotas, and CloudWatch evidence.
```

---

# Topic 6: Scripting and Automation Patterns

## 17. Shell Scripting with AWS CLI

Baseline script guardrails:
```bash
set -euo pipefail

: "${AWS_PROFILE:?Set AWS_PROFILE explicitly}"
: "${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION explicitly}"

aws sts get-caller-identity
```

Automation principles:
```text
Make account and region explicit.
Use tags so cleanup and ownership are possible.
Use waiters after async changes.
Make scripts idempotent where possible.
Capture resource IDs in output.
Use --query for simple JSON extraction; use jq only when transformations are too complex.
Avoid unbounded loops against AWS APIs.
```

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

**Pattern 5 — Safe resource tagging on create:**
```bash
aws ec2 run-instances \
  --image-id ami-0123456789abcdef0 \
  --instance-type t3.micro \
  --subnet-id subnet-0123456789abcdef0 \
  --security-group-ids sg-0123456789abcdef0 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Project,Value=aws-cli-lab},{Key=Owner,Value=aravind},{Key=TTL,Value=2026-07-03}]' \
  --dry-run
```

Remove `--dry-run` only after account, region, subnet, security group, and tags are correct.

## 17.1 Cleanup and Teardown Playbook

Every hands-on AWS lab should end with cleanup. CLI mastery includes knowing what costs continue after the demo.

Common cost survivors:
```text
EC2 instances and EBS volumes
NAT Gateways
Elastic IPs
Load balancers
RDS/Aurora clusters
EKS clusters and node groups
CloudWatch log retention
S3 objects and noncurrent versions
OpenSearch, SageMaker, Bedrock provisioned throughput, and endpoints
```

Find resources by tag:
```bash
aws resourcegroupstaggingapi get-resources \
  --tag-filters Key=Project,Values=aws-cli-lab \
  --query "ResourceTagMappingList[*].ResourceARN" \
  --output text
```

Set CloudWatch Logs retention:
```bash
aws logs put-retention-policy \
  --log-group-name /aws/lambda/my-function \
  --retention-in-days 14
```

Empty an S3 lab bucket before deleting:
```bash
aws s3 rm s3://my-lab-bucket --recursive
aws s3api delete-bucket --bucket my-lab-bucket
```

Terraform-backed environments:
```bash
terraform plan -destroy
terraform destroy
```

Cleanup interview line:
```text
I tag lab resources, keep teardown steps next to creation steps, set log retention, and remove expensive stateful services first.
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

CLI inside CI/CD should be boring:
```text
1. Authenticate by OIDC role assumption.
2. Print caller identity.
3. Build artifact.
4. Deploy through IaC or controlled service command.
5. Wait for stability.
6. Verify health/alarms.
7. Roll back automatically or stop for approval.
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

CloudShell safety:
```text
CloudShell is authenticated as your console identity. It is convenient, but it still has real permissions and can create real costs.
Always run sts get-caller-identity before write commands.
```

## 20. IAM Identity Center (SSO) Integration

For organizations using IAM Identity Center (formerly AWS SSO):

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

Reusable session setup:
```bash
aws configure sso-session
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

SSO troubleshooting:
```text
ExpiredToken or UnauthorizedException:
  run aws sso login --profile <profile>

Wrong account:
  inspect ~/.aws/config sso_account_id and sso_role_name

Browser cannot open:
  use the printed device/browser URL or run from CloudShell

Multiple profiles share one SSO session:
  define one [sso-session] and point many [profile ...] blocks to it
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
| `--cli-auto-prompt` | interactive prompt for required options |
| `--cli-binary-format raw-in-base64-out` | send raw JSON payloads to services such as Lambda |

## 22.1 Binary Payloads and Lambda Invoke

AWS CLI v2 changed binary payload handling. For direct JSON payloads to Lambda, include:

```bash
aws lambda invoke \
  --function-name my-function \
  --cli-binary-format raw-in-base64-out \
  --payload '{"hello":"world"}' \
  response.json
```

Without that flag, payload encoding errors can look confusing.

## 22.2 Auto Prompt for Learning

```bash
aws ec2 run-instances --cli-auto-prompt
```

Use auto prompt to discover required parameters, then convert the final command into a reviewed script or IaC. Do not use interactive prompts as the only production deployment process.

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

LocalStack boundary:
```text
LocalStack is excellent for quick feedback and SDK/CLI learning.
It is not a full proof of IAM, networking, quotas, regional behavior, managed service internals, or production scaling.
Always validate critical infrastructure against a real AWS sandbox account.
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

Use `--debug` carefully:
```text
Debug output can expose request metadata and sometimes sensitive paths or headers.
Do not paste raw debug logs into tickets or chat without reviewing/redacting them.
```

## 25. Common Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `Unable to locate credentials` | No credentials configured | Run `aws configure` or set env vars |
| `InvalidClientTokenId` | Wrong or expired access key ID | Re-run `aws configure`, check key is active |
| `AuthFailure` | Wrong secret key or signature mismatch | Re-enter secret key; check system clock |
| `ExpiredToken` | Session token expired | Refresh STS credentials or SSO login |
| `AccessDenied` | Missing IAM/SCP/resource/KMS permission | Check CloudTrail, IAM policy, resource policy, permission boundary, SCP, and key policy |
| `NoSuchBucket` | Bucket doesn't exist or wrong region | Check bucket name and `--region` |
| `SignatureDoesNotMatch` | System clock skew | Sync system time (`ntpdate`) |
| `An error occurred (RequestExpired)` | Clock drift >15 min | Fix system clock |
| `Could not connect to the endpoint URL` | Network/VPN issue or wrong region | Check connectivity, `--endpoint-url` |
| `UnrecognizedClientException` | Bad credentials or wrong session token | Refresh credentials; clear stale env vars |
| `ValidationException` | Missing/invalid parameter | Use service help or `--generate-cli-skeleton` |
| `ThrottlingException` | API rate/quota pressure | Backoff, reduce concurrency, request quota increase if justified |
| `ResourceNotFoundException` | Wrong name, region, or account | Verify ARN, region, and caller identity |

## 25.1 Debug Playbooks

Wrong account or role:
```bash
aws configure list
env | grep '^AWS_'
aws sts get-caller-identity
```

Wrong region:
```bash
aws configure get region
aws ec2 describe-regions --query "Regions[*].RegionName" --output table
```

Access denied:
```bash
aws sts get-caller-identity
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=<FailedApiName>
```

ECS deployment failing:
```bash
aws ecs describe-services --cluster my-cluster --services my-service
aws ecs list-tasks --cluster my-cluster --service-name my-service --desired-status STOPPED
aws logs tail /ecs/my-service --follow
```

CloudFormation failed:
```bash
aws cloudformation describe-stack-events \
  --stack-name my-stack \
  --query "StackEvents[0:20].[Timestamp,LogicalResourceId,ResourceStatus,ResourceStatusReason]" \
  --output table
```

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

Access Analyzer direction:
```text
Use IAM Access Analyzer for external access findings and policy validation.
Use simulate-principal-policy for "would this principal be allowed?" checks.
Use CloudTrail for "what actually happened?" evidence.
```

---

# Topic 9: Hands-On CLI Labs

## 27. Lab 1: Read-Only Account Orientation

Goal: prove you can safely orient yourself before changing anything.

Commands:
```bash
aws --version
aws configure list
aws configure list-profiles
aws sts get-caller-identity
aws ec2 describe-regions --query "Regions[*].RegionName" --output table
aws s3 ls
```

Expected learning:
```text
You know the caller, account, region source, profile source, and whether basic API calls work.
```

## 28. Lab 2: S3 Safe Upload, Versioning, Presigned URL, Cleanup

Create and verify:
```bash
BUCKET="aws-cli-lab-123456789012"

aws s3 mb "s3://$BUCKET" --region us-east-1
aws s3api put-bucket-versioning \
  --bucket "$BUCKET" \
  --versioning-configuration Status=Enabled
aws s3 cp ./sample.txt "s3://$BUCKET/sample.txt"
aws s3api head-object --bucket "$BUCKET" --key sample.txt
aws s3 presign "s3://$BUCKET/sample.txt" --expires-in 300
```

Cleanup:
```bash
aws s3 rm "s3://$BUCKET" --recursive
aws s3api delete-bucket --bucket "$BUCKET"
```

Expected learning:
```text
High-level S3 commands handle daily movement. s3api exposes exact bucket/object controls.
```

## 29. Lab 3: CloudFormation Deploy and Inspect

Goal: practice stack deployment with validation, waiters, and event inspection.

```bash
aws cloudformation validate-template --template-body file://template.yaml
aws cloudformation deploy \
  --stack-name aws-cli-lab \
  --template-file template.yaml \
  --capabilities CAPABILITY_NAMED_IAM
aws cloudformation wait stack-create-complete --stack-name aws-cli-lab
aws cloudformation describe-stack-events --stack-name aws-cli-lab --output table
```

Cleanup:
```bash
aws cloudformation delete-stack --stack-name aws-cli-lab
aws cloudformation wait stack-delete-complete --stack-name aws-cli-lab
```

## 30. Lab 4: Debug an AccessDenied

Flow:
```text
1. Copy exact failed API action and resource ARN from error or app log.
2. Run sts get-caller-identity with the same profile/environment.
3. Search CloudTrail for the failed event.
4. Check identity policy, resource policy, permission boundary, SCP, session policy, and KMS key policy.
5. Use simulate-principal-policy for a focused action/resource check.
```

CLI commands:
```bash
aws sts get-caller-identity
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=GetObject
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:role/MyRole \
  --action-names s3:GetObject \
  --resource-arns arn:aws:s3:::my-bucket/path/file.txt
```

## 31. Lab 5: ECS Deployment Triage

Goal: diagnose "deployment stuck" without guessing.

```bash
aws ecs describe-services \
  --cluster my-cluster \
  --services my-service \
  --query "services[*].{Running:runningCount,Desired:desiredCount,Events:events[0:5].message}"

aws ecs list-tasks \
  --cluster my-cluster \
  --service-name my-service \
  --desired-status STOPPED

aws ecs describe-tasks \
  --cluster my-cluster \
  --tasks <task-arn> \
  --query "tasks[*].{StoppedReason:stoppedReason,Containers:containers[*].reason}"

aws logs tail /ecs/my-service --since 30m
```

Expected learning:
```text
ECS failures usually show up in service events, stopped task reasons, container exit reasons, target group health, or CloudWatch logs.
```

---

# Topic 10: Interview-Ready Summary

## 32. Top Interview Questions

**Q: How do you authenticate AWS CLI in a CI/CD pipeline?**
```text
Use OIDC with your CI provider (GitHub Actions, GitLab CI, CircleCI).
The CI job assumes an IAM role via a trust policy that validates the OIDC token.
No long-term access keys — credentials are short-lived and scoped per workflow.
```

**Q: What's the priority order for AWS CLI credential resolution?**
```text
For day-to-day CLI usage:
1. Explicit command flags: --profile, --region, --output
2. Environment variables
3. Profile configuration in ~/.aws/config
4. Shared credentials in ~/.aws/credentials
5. SSO/web identity/credential_process configured by profile
6. ECS/EC2/Lambda role credentials from runtime metadata
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

**Q: What makes an AWS CLI script production-grade?**
```text
It pins account/profile/region, prints caller identity, uses temporary credentials,
handles pagination/waiters/retries, tags created resources, avoids broad deletes,
checks exit codes, logs enough context, and has a cleanup/rollback path.
```

**Q: How do you debug AccessDenied from the CLI?**
```text
First prove the caller with STS, then find the failed API in CloudTrail.
Check identity policy, resource policy, permission boundary, session policy, SCP,
and KMS key policy. Then validate with simulate-principal-policy or Access Analyzer.
```

## 33. Quick Reference Cheat Sheet

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

## 34. Revision Notes

```text
One-line summary:
  AWS CLI is the API surface for operators; it is safe only when account, region, identity, permissions, and rollback are explicit.

Three keywords:
  SSO, --query, waiters.

One interview trap:
  Saying "I put AWS keys in GitHub secrets" instead of using OIDC role assumption.

One memory trick:
  W-R-I-T-E before any write command: Who, Region, Identity, Target, Exit plan.
```

## 35. Official Source Notes

- AWS CLI User Guide: <https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html>
- AWS CLI IAM Identity Center configuration: <https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html>
- AWS CLI command reference: <https://docs.aws.amazon.com/cli/latest/reference/>
- AWS SDKs and Tools settings reference: <https://docs.aws.amazon.com/sdkref/latest/guide/settings-reference.html>

---

*Last updated: 2026-07 | AWS CLI v2 | Part of AWS Interview Track — Foundations*
