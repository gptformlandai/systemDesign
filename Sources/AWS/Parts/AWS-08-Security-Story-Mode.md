# AWS Security Through Story Mode: Who Can Do What and How Nothing Leaks

> On your laptop, security barely exists. Your Spring Boot talks to your PostgreSQL with `postgres/postgres`. Your React calls `localhost:8080` with no authentication. There are no firewalls, no roles, no encrypted secrets. Then you move to AWS and suddenly you need IAM roles, security groups, encrypted databases, secret managers, TLS certificates, and user authentication. This guide explains AWS security the way your app actually needs it — layer by layer, starting from what you already know.

---

# Table of Contents

1. [How Security Feels on Your Laptop](#1-how-security-feels-on-your-laptop)
2. [What Changes When You Move to AWS](#2-what-changes-when-you-move-to-aws)
3. [The Big Picture: Four Security Questions](#3-the-big-picture-four-security-questions)
4. [Story Mode: Your App Grows Up](#4-story-mode-your-app-grows-up)
5. [Who Can Call AWS Services — IAM](#5-who-can-call-aws-services--iam)
6. [Who Can Reach Your App — Network Security](#6-who-can-reach-your-app--network-security)
7. [Where Secrets Live — Secrets Manager and Parameter Store](#7-where-secrets-live--secrets-manager-and-parameter-store)
8. [How Data Is Protected — Encryption](#8-how-data-is-protected--encryption)
9. [Who Your Users Are — Authentication](#9-who-your-users-are--authentication)
10. [What Your Users Can Do — Authorization](#10-what-your-users-can-do--authorization)
11. [Putting It All Together for a Real App](#11-putting-it-all-together-for-a-real-app)
12. [Common Mistakes and How to Avoid Them](#12-common-mistakes-and-how-to-avoid-them)
13. [Interview-Ready Answers](#13-interview-ready-answers)
14. [Quick Revision Sheet](#14-quick-revision-sheet)

---

# 1. How Security Feels on Your Laptop

On your laptop, your app probably works like this:

```text
React frontend
  -> calls localhost:8080 with no auth token (or a hardcoded dev token)

Spring Boot backend
  -> connects to PostgreSQL with username: postgres, password: postgres
  -> JWT secret is "my-dev-secret" in application.yml
  -> AWS credentials might be in ~/.aws/credentials from aws configure
  -> no TLS anywhere, everything is HTTP

PostgreSQL
  -> listens on localhost:5432
  -> accepts connections from anyone on the machine
  -> no encryption
```

Why does this "work"?

- there is only one user: you
- there is no internet exposure
- there is no attacker
- convenience is more important than security in local dev

That is fine for development. But on AWS, every one of these shortcuts becomes a vulnerability.

---

# 2. What Changes When You Move to AWS

On AWS, your app is exposed to real threats:

```text
Local:   nobody can reach your laptop from the outside
AWS:     your ALB is on the public internet, anyone can send requests

Local:   postgres/postgres is fine
AWS:     default credentials on a reachable database = immediate breach

Local:   secrets in application.yml are fine
AWS:     secrets in code or environment files can leak through logs, repos, or image layers

Local:   no encryption needed
AWS:     data at rest and in transit must be encrypted for compliance and protection

Local:   you are the only user
AWS:     your app has real users who need authentication and authorization
```

So the question becomes:

```text
How do I keep everything locked down while still letting
the right people and the right services do their jobs?
```

---

# 3. The Big Picture: Four Security Questions

Every security decision on AWS maps to one of four questions:

```text
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│   1. WHO CAN CALL AWS SERVICES?                                    │
│      "Can my Spring Boot app read from S3?"                        │
│      "Can my ECS task pull secrets?"                                │
│      Answer: IAM roles and policies                                │
│                                                                    │
│   2. WHO CAN REACH MY APP OVER THE NETWORK?                        │
│      "Can the internet hit my database?"                           │
│      "Can only the ALB talk to my backend?"                        │
│      Answer: Security groups, subnets, NACLs                      │
│                                                                    │
│   3. HOW ARE SECRETS AND DATA PROTECTED?                           │
│      "Where is my database password stored?"                       │
│      "Is my data encrypted on disk and in transit?"                │
│      Answer: Secrets Manager, KMS, TLS, encryption settings       │
│                                                                    │
│   4. WHO ARE MY APPLICATION USERS AND WHAT CAN THEY DO?            │
│      "How do users log in?"                                        │
│      "Can this user access admin features?"                        │
│      Answer: Cognito/JWT/OAuth + application-level authorization   │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

Think of it as four layers:

```text
Layer 1: IAM            → controls what AWS resources your code can touch
Layer 2: Network        → controls what traffic can flow where
Layer 3: Secrets + Encryption → protects data at rest and in transit
Layer 4: App Auth       → controls who your end users are and what they can do
```

Each layer is independent. You need all four.

---

# 4. Story Mode: Your App Grows Up

## Phase 1: "We just deployed, it works"

The team puts Spring Boot on EC2, PostgreSQL on RDS.

They hardcode the DB password in `application.yml`.
They use the default security group that allows all traffic.
There is no HTTPS.

This works. It is also completely insecure.

## Phase 2: "Security review said we have problems"

Someone audits the setup and finds:

```text
Problem 1: DB password is in the source code repo
Problem 2: RDS is reachable from the internet
Problem 3: No TLS — API traffic is unencrypted
Problem 4: EC2 instance has admin-level AWS permissions
Problem 5: No user authentication on the API
```

Now the team starts fixing things.

## Phase 3: "We locked it down properly"

After fixing:

```text
✓ DB password in Secrets Manager, injected at runtime
✓ RDS in private subnet, security group allows only backend
✓ ALB terminates HTTPS with ACM certificate
✓ EC2/ECS uses scoped IAM role with least privilege
✓ API validates JWT tokens from Cognito or custom auth
✓ S3 buckets are private with block public access
✓ EBS and RDS volumes encrypted with KMS
```

That is the journey. Let us walk through each layer.

---

# 5. Who Can Call AWS Services — IAM

This is the most fundamental security concept on AWS.

## 5.1 The Problem

Your Spring Boot app needs to:

- read secrets from Secrets Manager
- upload files to S3
- send messages to SQS
- pull images from ECR

How does AWS know your app is allowed to do these things?

## 5.2 Real-Life Analogy

```text
IAM is like a company badge system.

Every employee (user) and every robot (service/application) gets a badge.
The badge says what doors they can open and what rooms they can enter.

Without a badge, you cannot do anything.
With a badge scoped to "Floor 3 only," you cannot reach Floor 5.
```

## 5.3 The Key Concept: Roles, Not Passwords

On your laptop, you probably ran `aws configure` and pasted an access key.

That is a **static credential**. Like writing a password on a sticky note.

On AWS, the correct approach is:

```text
Local dev:     AWS access keys in ~/.aws/credentials (acceptable for dev)
EC2 in prod:   IAM Instance Role (no keys stored anywhere)
ECS in prod:   IAM Task Role (no keys stored anywhere)
EKS in prod:   IRSA / Pod Identity (no keys stored anywhere)
Lambda:        Execution Role (no keys stored anywhere)
```

The application never sees or stores AWS credentials. AWS injects temporary credentials automatically through the role.

## 5.4 How It Actually Works for Your Spring Boot App

### On EC2

```text
1. You create an IAM role: myapp-backend-role
2. You attach policies to it:
     - s3:GetObject on the uploads bucket
     - secretsmanager:GetSecretValue on the DB password secret
     - sqs:SendMessage on the orders queue
3. You attach this role to the EC2 instance
4. Spring Boot uses the AWS SDK
5. The SDK automatically finds the role credentials via instance metadata
6. No access keys anywhere in your code or config
```

### On ECS

```text
1. You create an IAM Task Role: myapp-backend-task-role
2. Same policy attachments as above
3. You put the role ARN in the ECS task definition
4. Each running task gets its own temporary credentials
5. Different services can have different task roles → least privilege
```

### On EKS

```text
1. You create an IAM role: myapp-backend-role
2. You create a Kubernetes ServiceAccount annotated with the role ARN (IRSA)
3. Pods using that ServiceAccount automatically get temporary AWS credentials
4. Pod A (needs S3) gets a different role than Pod B (needs DynamoDB)
```

## 5.5 What an IAM Policy Looks Like

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::myapp-uploads-prod/*"
    },
    {
      "Effect": "Allow",
      "Action": "secretsmanager:GetSecretValue",
      "Resource": "arn:aws:secretsmanager:us-east-1:123456789012:secret:myapp/db-password-*"
    }
  ]
}
```

Notice:

- specific actions, not `"Action": "*"`
- specific resources, not `"Resource": "*"`
- this is least privilege

## 5.6 The Two IAM Roles in ECS (Important Distinction)

This confuses many people:

```text
Execution Role:
  WHO: ECS agent (not your app)
  WHAT: pull container image from ECR, push logs to CloudWatch, read secrets
  WHEN: before and during task startup

Task Role:
  WHO: your application code running inside the container
  WHAT: call S3, SQS, DynamoDB, whatever your app needs
  WHEN: while the app is running
```

Real-life analogy:

```text
Execution Role = the delivery person who brings supplies to your office
Task Role      = you, the employee, doing your actual job

The delivery person needs access to the loading dock.
You need access to the office floor.
They are different permissions for different purposes.
```

## 5.7 Common Mistake: Over-Permissive Roles

```text
BAD:
{
  "Effect": "Allow",
  "Action": "*",
  "Resource": "*"
}

This gives your app permission to do ANYTHING in your AWS account.
Delete databases, terminate instances, create new users.

GOOD:
Scope every permission to the exact action and resource needed.
```

---

# 6. Who Can Reach My App — Network Security

This was covered in detail in the networking guide. Here is the security-focused summary.

## 6.1 The Layered Model

```text
Internet
    ↓
ALB (public subnet, SG: allow 443 from internet)
    ↓
Spring Boot (private subnet, SG: allow 8080 from ALB only)
    ↓
RDS (private subnet, SG: allow 5432 from backend only)
```

Each layer can only be reached by the layer above it.

## 6.2 Security Groups as the Primary Guard

```text
ALB Security Group:
  Inbound:  TCP 443 from 0.0.0.0/0
  
Backend Security Group:
  Inbound:  TCP 8080 from ALB Security Group only

Database Security Group:
  Inbound:  TCP 5432 from Backend Security Group only

Redis Security Group:
  Inbound:  TCP 6379 from Backend Security Group only
```

Rule: always reference security groups, not IPs. IPs change. Security groups don't.

## 6.3 Private Subnets as the Foundation

Your backend and database should never be in a public subnet.

```text
Public subnet  = has a route to the Internet Gateway = reachable from internet
Private subnet = no route to IGW = unreachable from internet directly
```

If your database is in a public subnet with a public IP, it is one misconfigured security group away from exposure.

## 6.4 No SSH in Production

Old pattern: SSH into servers to debug.

Better pattern:

```text
Use AWS Systems Manager (SSM) Session Manager.
  - no SSH port open
  - no key pair management
  - audit trail of who connected
  - works through IAM, not network rules
```

---

# 7. Where Secrets Live — Secrets Manager and Parameter Store

## 7.1 The Problem

Your Spring Boot app needs secrets:

- database password
- JWT signing key
- API keys for Stripe, Twilio, SendGrid
- OAuth client secrets

On your laptop:

```yaml
# application.yml — this is fine locally, dangerous in production
spring:
  datasource:
    password: postgres
app:
  jwt-secret: my-dev-secret
  stripe-key: sk_test_xxxxxxxxxxxx
```

If this file ends up in a Docker image, a Git repo, or a log, those secrets are compromised.

## 7.2 Real-Life Analogy

```text
Secrets Manager is like a locked safe in the office.

You do not tape the office door code to the front door.
You store it in a safe. Only people with the right badge (IAM role)
can open the safe and read the code.
```

## 7.3 How It Works for Your App

### Step 1: Store the secret in Secrets Manager

```text
Secret name: myapp/prod/db-password
Secret value: s3cureP@ssw0rd!2026
```

### Step 2: Give your app permission to read it

IAM policy on the task/instance role:

```json
{
  "Effect": "Allow",
  "Action": "secretsmanager:GetSecretValue",
  "Resource": "arn:aws:secretsmanager:us-east-1:123456789012:secret:myapp/prod/*"
}
```

### Step 3: Your app reads the secret at startup

Option A — Spring Boot reads it via AWS SDK at startup:

```java
// In a @Configuration class or custom EnvironmentPostProcessor
SecretsManagerClient client = SecretsManagerClient.create();
GetSecretValueResponse response = client.getSecretValue(
    GetSecretValueRequest.builder()
        .secretId("myapp/prod/db-password")
        .build()
);
String dbPassword = response.secretString();
```

Option B — ECS injects it as an environment variable (simpler):

```json
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:myapp/prod/db-password"
    }
  ]
}
```

The container sees `DB_PASSWORD` as an environment variable. Spring Boot picks it up via `${DB_PASSWORD}`. No code changes needed.

Option C — Kubernetes External Secrets (for EKS):

An operator syncs Secrets Manager values into Kubernetes Secrets, which are mounted as environment variables or volumes.

## 7.4 Secrets Manager vs Parameter Store

```text
Use Secrets Manager when:
  → secrets need automatic rotation (e.g., RDS passwords)
  → compliance requires managed secret lifecycle
  → you want built-in rotation for supported services

Use Parameter Store when:
  → storing non-sensitive config (feature flags, endpoints)
  → budget matters (free tier is generous)
  → simple key-value config for your application
  → secrets that do not need automatic rotation

Both encrypt with KMS. Both integrate with IAM.
```

## 7.5 What Should NEVER Be in Your Code or Docker Image

```text
✗ Database passwords
✗ JWT signing keys
✗ API keys (Stripe, Twilio, etc.)
✗ OAuth client secrets
✗ AWS access keys
✗ TLS private keys

All of these belong in Secrets Manager or Parameter Store,
retrieved at runtime via IAM role permissions.
```

---

# 8. How Data Is Protected — Encryption

## 8.1 Two Types of Encryption

```text
Encryption at rest:
  Data is encrypted when stored on disk.
  Even if someone steals the physical disk, they cannot read the data.

Encryption in transit:
  Data is encrypted while moving over the network.
  Even if someone intercepts the traffic, they cannot read it.
```

You need both.

## 8.2 Real-Life Analogy

```text
Encryption at rest = locking your documents in a filing cabinet
Encryption in transit = putting your letter in a sealed envelope before mailing it

Without at-rest encryption:
  someone who breaks into the office reads your files

Without in-transit encryption:
  someone who intercepts the mail reads your letters
```

## 8.3 Encryption at Rest for Your App

### S3

```text
Default: S3 encrypts all new objects automatically (SSE-S3).
Better: use SSE-KMS with a customer managed key if you need audit and key control.
```

### RDS

```text
Enable encryption when creating the instance.
Cannot be enabled after creation on an unencrypted instance.
So always enable it from the start.
```

### EBS

```text
Enable encryption on volumes.
Use default AWS-managed key or a customer managed KMS key.
```

### ElastiCache

```text
Redis supports at-rest encryption. Enable it.
```

### DynamoDB

```text
Encrypted by default with an AWS-owned key.
Can use customer managed KMS key for more control.
```

Rule: enable encryption at rest everywhere. There is almost no reason not to.

## 8.4 Encryption in Transit for Your App

```text
User → ALB:
  HTTPS (TLS certificate from ACM, free)
  
ALB → Backend:
  HTTP is common (private network)
  HTTPS is better if compliance requires end-to-end encryption

Backend → RDS:
  TLS can be enforced in RDS parameter group
  Use sslmode=require in JDBC connection string

Backend → ElastiCache:
  Redis supports in-transit encryption. Enable it.

Backend → S3:
  HTTPS by default via AWS SDK
```

## 8.5 KMS — The Key Manager

KMS manages the encryption keys used by all the services above.

You usually do not interact with KMS directly. But you should know:

```text
AWS-managed key:
  AWS creates and manages it for you per service.
  You cannot control rotation or access beyond defaults.
  Good enough for many workloads.

Customer-managed key:
  You create the key in KMS.
  You control the key policy (who can encrypt/decrypt).
  You control rotation schedule.
  Better for regulated or compliance-heavy workloads.
```

For a normal app:

- AWS-managed keys are fine to start
- customer-managed keys when compliance or audit requirements demand it

## 8.6 Envelope Encryption — How It Actually Works

This sounds complex but is simple:

```text
1. AWS generates a short-lived data key
2. Your data is encrypted with the data key (fast, local)
3. The data key itself is encrypted with the KMS master key
4. Both the encrypted data and encrypted data key are stored

To decrypt:
1. Send the encrypted data key to KMS
2. KMS decrypts it using the master key (KMS never sees your data)
3. Use the decrypted data key to decrypt your data locally
```

Why?

- encrypting large data directly with KMS would be slow and expensive
- envelope encryption keeps large data local while keeping key security centralized

---

# 9. Who Your Users Are — Authentication

Everything above secures your infrastructure. This section secures your application layer.

## 9.1 The Problem

Your app has real users:

- customers
- admins
- internal staff
- maybe API consumers

You need to know:

- is this user who they claim to be? (authentication)
- is this user allowed to do what they are requesting? (authorization)

## 9.2 Real-Life Analogy

```text
Authentication = checking someone's ID at the door
  "Are you really John Smith? Show me your driver's license."

Authorization = checking if they have permission
  "Okay, you are John Smith. But are you allowed in the VIP room?"
```

## 9.3 How Authentication Works for Your App

### The Common Pattern: JWT

```text
1. User sends username + password to login endpoint
2. Backend validates credentials against user store
3. Backend creates a JWT token signed with a secret key
4. Browser stores the token
5. Every subsequent request includes the token in the Authorization header
6. Backend verifies the token signature and extracts user identity
```

### Where the User Store Lives

Option A — Your own database:

```text
You store users in RDS.
You handle password hashing (bcrypt), login, token creation.
Full control, more code to maintain.
```

Option B — AWS Cognito:

```text
Cognito handles:
  - user registration
  - email verification
  - password policy
  - login
  - JWT token issuance
  - MFA
  - social login (Google, Facebook, etc.)

Your Spring Boot backend just validates the JWT token that Cognito issues.
You do not store passwords. You do not handle email verification flows.
```

Option C — External identity provider:

```text
Auth0, Okta, Firebase Auth, or corporate SSO.
Your backend validates their tokens.
```

## 9.4 How Spring Boot Validates JWT

Regardless of who issues the token, Spring Boot validates it:

```java
// Spring Security with JWT validation
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())    // validates JWT signature
            );
        return http.build();
    }
}
```

```yaml
# application.yml — point to token issuer
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXXX
```

Spring Boot:

1. receives request with `Authorization: Bearer <token>`
2. fetches Cognito's public key (JWKS)
3. verifies the token signature and expiration
4. extracts user identity and roles from token claims
5. applies authorization rules

No password handling in your backend. The identity provider does that.

## 9.5 Cognito in Story Mode

```text
Before Cognito:
  You build: registration page, email verification, password reset,
  MFA, token issuance, token refresh, social login integration.
  That is weeks of work and ongoing maintenance.

With Cognito:
  You configure a User Pool.
  Users sign up and log in through Cognito.
  Cognito issues JWT tokens.
  Your backend validates tokens.
  You focus on business logic.
```

When Cognito is NOT the right fit:

- you need very custom authentication flows
- pricing does not work at your scale
- you already have a corporate identity provider (use that instead)
- you want zero vendor lock-in on identity

## 9.6 IAM vs Cognito — They Solve Different Problems

```text
IAM:
  Controls what AWS resources (S3, RDS, SQS) your services can access.
  Used by: your backend code, CI/CD pipelines, AWS services.
  Not for: end users logging into your web app.

Cognito:
  Controls who your application users are and gives them tokens.
  Used by: end users of your web/mobile app.
  Not for: controlling which S3 bucket your backend can read.
```

This is the single most common confusion in AWS security interviews.

---

# 10. What Your Users Can Do — Authorization

Authentication tells you WHO the user is.
Authorization tells you WHAT they can do.

## 10.1 Where Authorization Happens

```text
Layer 1: API Gateway or ALB
  Can block requests before they reach your backend.
  Example: only allow requests with valid JWT.

Layer 2: Spring Boot Security
  Checks roles, scopes, and permissions in the token.
  Example: /api/admin/* requires ADMIN role.

Layer 3: Business Logic
  Application-level rules.
  Example: user can only edit their own orders, not other users' orders.
```

## 10.2 Example Authorization Flow

```text
1. User sends: GET /api/orders/123
   Header: Authorization: Bearer <jwt-token>

2. Spring Security extracts token.
   Token contains: { "sub": "user-42", "roles": ["USER"] }

3. Security filter: is this user authenticated? Yes.

4. Controller receives request.
   Business logic: does user-42 own order 123?
   Query: SELECT * FROM orders WHERE id = 123 AND user_id = 'user-42'

5. If yes → return order.
   If no → return 403 Forbidden.
```

This is defense in depth:

- Spring Security handles identity verification
- business logic handles data-level access control

## 10.3 Role-Based vs Attribute-Based

```text
Role-Based (RBAC):
  "ADMIN can do everything. USER can only read their own data."
  Simple. Works for most apps.

Attribute-Based (ABAC):
  "User can access resource if user.department == resource.department
   AND user.clearance >= resource.sensitivityLevel"
  More flexible. More complex. Used in enterprise/compliance scenarios.
```

For most Spring Boot apps, RBAC is enough.

---

# 11. Putting It All Together for a Real App

Let us map the complete security posture for your React + Spring Boot + PostgreSQL app on AWS.

## 11.1 The Full Security Stack

```text
┌──────────────────────────────────────────────────────────────┐
│                    YOUR APPLICATION                           │
│                                                              │
│  LAYER 4: APPLICATION AUTH                                    │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Cognito issues JWT tokens to users                     │  │
│  │ Spring Security validates tokens on every request      │  │
│  │ Business logic enforces data-level permissions         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  LAYER 3: SECRETS + ENCRYPTION                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ DB password in Secrets Manager (auto-rotated)          │  │
│  │ JWT secret in Secrets Manager                          │  │
│  │ API keys in Secrets Manager                            │  │
│  │ RDS encrypted at rest (KMS)                            │  │
│  │ S3 encrypted at rest (SSE-S3 or SSE-KMS)              │  │
│  │ All traffic TLS-encrypted (ACM cert on ALB)            │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  LAYER 2: NETWORK SECURITY                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ ALB in public subnet (only HTTPS from internet)        │  │
│  │ Backend in private subnet (only from ALB)              │  │
│  │ RDS in private subnet (only from backend)              │  │
│  │ Security groups enforce every connection               │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  LAYER 1: IAM                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Backend has scoped IAM role (S3, Secrets, SQS only)    │  │
│  │ No static AWS credentials anywhere                     │  │
│  │ Execution role for image pull + logging                │  │
│  │ Task/instance role for application AWS calls           │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 11.2 Request Flow With All Security Layers Active

```text
1. User opens React app (served from CloudFront + S3)

2. User logs in
   → React sends credentials to Cognito
   → Cognito validates, returns JWT access token + refresh token
   → React stores tokens in memory (not localStorage for sensitive apps)

3. User requests their orders
   → React sends: GET /api/orders
   → Header: Authorization: Bearer <jwt>

4. Request hits ALB
   → ALB is in public subnet
   → ALB terminates TLS (ACM certificate)
   → ALB forwards to healthy backend in private subnet

5. Spring Boot receives request
   → Spring Security validates JWT (checks signature, expiry, issuer)
   → Extracts user identity from token claims
   → Controller calls service layer

6. Service layer queries RDS
   → Backend connects to RDS using credentials from Secrets Manager
   → Connection uses TLS (sslmode=require)
   → Security group allows backend → RDS on port 5432

7. Service layer reads profile image URL
   → Backend generates pre-signed S3 URL for the user's image
   → This uses the IAM task role's S3 permissions

8. Response returns
   → JSON with order data + signed image URL
   → Backend → ALB → Internet → Browser
   → All encrypted in transit
```

Every layer participated. Nothing was bypassed.

---

# 12. Common Mistakes and How to Avoid Them

## 12.1 "We put the DB password in the Docker image"

```text
Problem:
  Anyone who pulls the image can extract the password.
  ECR images, CI/CD logs, or leaked layers expose it.

Fix:
  Store in Secrets Manager.
  Inject via ECS secrets or read at app startup via SDK.
  The Docker image should contain zero secrets.
```

## 12.2 "We gave the backend AdministratorAccess"

```text
Problem:
  If the app is compromised, the attacker owns your entire AWS account.

Fix:
  Scope IAM role to exact actions and resources needed.
  s3:GetObject on one bucket ≠ s3:* on *.
```

## 12.3 "We put RDS in a public subnet"

```text
Problem:
  One security group mistake = database exposed to the internet.

Fix:
  RDS in private subnet. Always.
  Security group allows only backend security group on port 5432.
```

## 12.4 "We hardcoded AWS access keys in the app"

```text
Problem:
  Keys in code get committed to Git.
  Keys in environment files get logged.
  Rotation becomes manual and error-prone.

Fix:
  Use IAM roles. The AWS SDK finds credentials automatically.
  Zero access keys in application code, config, or Docker images.
```

## 12.5 "We disabled HTTPS because it was hard to set up"

```text
Problem:
  All traffic including login credentials transmitted in cleartext.

Fix:
  ACM provides free TLS certificates.
  ALB handles TLS termination with one configuration.
  There is no valid reason to skip HTTPS in production.
```

## 12.6 "We store JWT tokens in localStorage"

```text
Problem:
  XSS attacks can steal tokens from localStorage.

Fix:
  For sensitive apps, use httpOnly cookies or keep tokens in memory.
  Short-lived access tokens + refresh tokens reduce exposure.
```

## 12.7 "We log everything including secrets"

```text
Problem:
  Database passwords, tokens, and API keys appear in CloudWatch logs.

Fix:
  Never log secret values.
  Mask or redact sensitive fields in log output.
  Review what your framework logs at DEBUG level before enabling it in prod.
```

---

# 13. Interview-Ready Answers

## 13.1 "How do you handle secrets in your application?"

```text
"I store all secrets — database credentials, API keys, JWT signing keys — 
in AWS Secrets Manager. The application reads them at startup using the 
AWS SDK, authorized by its IAM task role. No secrets are hardcoded, stored 
in environment files, or baked into Docker images. For ECS, I use the 
secrets field in the task definition so ECS injects them as environment 
variables from Secrets Manager directly."
```

## 13.2 "How do you implement least privilege?"

```text
"Every service gets its own IAM role scoped to exactly the actions and 
resources it needs. The Spring Boot backend might have 
s3:GetObject/PutObject on the uploads bucket and 
secretsmanager:GetSecretValue on its own secrets. No wildcard actions, 
no wildcard resources. On EKS, I use IRSA so each pod gets its own 
IAM identity. On ECS, each service gets its own task role."
```

## 13.3 "How does your app authenticate users?"

```text
"I use Cognito User Pools for user registration and login. Cognito issues 
JWT tokens. The React frontend includes the token in every API request. 
Spring Security validates the JWT signature using Cognito's JWKS endpoint, 
checks expiration, and extracts user identity. Authorization is handled 
at the controller level with role-based access and at the service level 
with data ownership checks."
```

## 13.4 "How is data encrypted?"

```text
"Encryption at rest is enabled on RDS, S3, and EBS using KMS. In transit, 
the ALB terminates HTTPS using a free ACM certificate. Backend-to-RDS 
connections enforce TLS. S3 access via the SDK uses HTTPS by default. 
For regulated workloads, I use customer-managed KMS keys for audit trail 
and key rotation control."
```

## 13.5 "What is the difference between IAM and Cognito?"

```text
"IAM controls which AWS services and resources your infrastructure and 
applications can access. Cognito controls who your end users are and 
issues them tokens. IAM is for machine-to-service authorization. 
Cognito is for human-to-application authentication. They serve 
completely different layers."
```

## 13.6 "Walk me through the security of a single API request"

```text
"The request arrives at the ALB over HTTPS, so it is encrypted in transit.
The ALB is in a public subnet; the backend and database are in private 
subnets. The ALB forwards to the backend, which validates the JWT token 
issued by Cognito. The backend queries RDS over a TLS connection, using 
credentials from Secrets Manager that it has permission to access via 
its IAM task role. The response returns through the same encrypted path.
Network access is enforced by security groups at every layer, and IAM 
ensures the backend can only touch the specific AWS resources it needs."
```

---

# 14. Quick Revision Sheet

## The Four Security Layers

```text
IAM              = what can my code touch in AWS?
Network          = what traffic can flow where?
Secrets + Crypto = how are credentials and data protected?
App Auth         = who are my users and what can they do?
```

## One-Line Mapping

```text
IAM Role            = badge that lets your app call AWS services
Security Group      = bouncer at each resource's door
Secrets Manager     = locked safe for passwords and API keys
Parameter Store     = config shelf for non-sensitive settings
KMS                 = key manager for encryption
ACM                 = free TLS certificates
Cognito             = managed user login and JWT tokens
JWT                 = signed token proving who the user is
Spring Security     = validates tokens and enforces access rules
```

## What Goes Where

```text
DB password          -> Secrets Manager
JWT signing key      -> Secrets Manager
Stripe API key       -> Secrets Manager
Feature flags        -> Parameter Store
API base URLs        -> Parameter Store or ConfigMap
User identities      -> Cognito or your own DB
Access control       -> Spring Security + business logic
AWS permissions      -> IAM roles (never static keys)
TLS certificates     -> ACM (free, auto-renewed)
Encryption keys      -> KMS
```

## Gold Standard Sentence

```text
"I secure my AWS application in four layers: IAM roles for least-privilege 
AWS access with no static credentials, private subnets and security groups 
for network isolation, Secrets Manager for credentials and KMS for 
encryption at rest and in transit, and Cognito or JWT-based authentication 
with Spring Security for user identity and authorization. Each layer is 
independent — compromising one does not automatically compromise the others."
```
