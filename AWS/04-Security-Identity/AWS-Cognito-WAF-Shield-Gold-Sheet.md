# AWS Security: Cognito, WAF, and Shield Gold Sheet

> Track: AWS Interview Track — Security and Identity
> Goal: implement authentication and authorization with Cognito, protect applications with WAF, and understand DDoS protection layers.

---

## 0. How To Read This

Beginner focus:
- Cognito User Pools vs Identity Pools
- WAF basics (what it blocks)
- Shield Standard vs Advanced

Intermediate focus:
- Cognito JWT validation
- WAF web ACL rules, managed rule groups
- ALB + Cognito integration
- WAF rate limiting

Senior / MAANG focus:
- Cognito federation (Google, SAML, OIDC)
- Cognito custom authorizers vs Cognito integration
- WAF Bot Control and Fraud Control
- WAF rule evaluation order
- Shield Advanced cost and SRT access
- Cognito machine-to-machine (M2M) with client credentials
- Fine-grained IAM-based authorization with Cognito Identity Pools

---

# Topic 1: Amazon Cognito

## 1. Intuition

Cognito handles user authentication and identity federation.

It has two separate components that solve different problems:

```text
User Pool: authentication service (who are you?)
  - stores user directory
  - handles sign-up, sign-in, MFA, password reset
  - issues JWTs (ID token, access token, refresh token)

Identity Pool: AWS credential vending (what can you do?)
  - maps authenticated users to IAM roles
  - issues temporary AWS credentials
  - used when users need to directly call AWS APIs (S3, DynamoDB)
```

## 2. User Pool Deep Dive

### What User Pool Provides

- User directory (username/email + password storage)
- Sign-up and sign-in APIs
- MFA (TOTP, SMS)
- Email/phone verification
- Password policies
- Hosted UI (pre-built login pages)
- Lambda triggers for custom logic
- Social identity federation (Google, Apple, Facebook, Amazon)
- Enterprise federation via SAML 2.0 and OIDC

### JWT Tokens Issued By User Pool

| Token | Lifetime | Contains |
|---|---|---|
| ID Token | 1 hour | user identity claims (sub, email, custom attributes) |
| Access Token | 1 hour | scopes and groups |
| Refresh Token | 30 days (configurable) | used to get new ID + access tokens |

### JWT Validation (Critical For APIs)

Validate JWTs from Cognito:

```text
1. Verify token signature using Cognito's public JWKS (JSON Web Key Set)
   URL: https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json

2. Verify token has not expired (exp claim)

3. Verify iss claim matches your User Pool endpoint

4. Verify aud claim matches your app client ID

5. Verify token_use is "id" or "access" depending on what you expect
```

Do NOT skip JWT validation on the backend. Common vulnerability: accepting any token without signature verification.

### Lambda Triggers For Custom Logic

| Trigger | When It Fires | Use Case |
|---|---|---|
| Pre sign-up | before user is confirmed | custom validation, block domains |
| Post confirmation | after user confirmed | create user record in database |
| Pre token generation | before JWT issued | add custom claims, modify groups |
| Custom authentication | replace password with custom challenge | OTP, magic links |
| Pre authentication | before sign-in | check if user is allowed |
| Post authentication | after successful sign-in | log audit event |

## 3. Cognito Identity Pools

Identity Pools vend temporary AWS credentials (via STS) to authenticated users:

```text
User authenticates with:
  - Cognito User Pool (most common)
  - Google, Facebook (social login)
  - SAML identity provider
  - Custom developer authenticated identities

Identity Pool maps authenticated users to IAM roles:
  - Authenticated Role: what logged-in users can do
  - Unauthenticated Role: what guest users can do (optional)

Use case:
  User signs in -> gets Cognito JWT -> exchanges for AWS temporary credentials
  -> writes their own files to S3: s3://my-bucket/users/${cognito-identity.amazonaws.com:sub}/*
```

```json
{
  "Effect": "Allow",
  "Action": ["s3:PutObject", "s3:GetObject"],
  "Resource": [
    "arn:aws:s3:::my-bucket/users/${cognito-identity.amazonaws.com:sub}/*"
  ]
}
```

## 4. Cognito With ALB (No Custom Authorizer Needed)

ALB natively integrates with Cognito User Pools:

```text
ALB Listener Rule:
  Authenticate with Cognito User Pool
  -> If not authenticated: redirect to Cognito hosted UI
  -> If authenticated: forward JWT headers to backend
  -> Backend reads headers without implementing OAuth flow

Headers added by ALB:
  X-Amzn-Oidc-Data: JWT ID token
  X-Amzn-Oidc-Identity: user's sub
  X-Amzn-Oidc-Accesstoken: access token
```

Use ALB + Cognito instead of building OAuth in your application when:
- SSO with enterprise SAML is needed
- you want to offload auth from the app
- using ECS or EC2 behind ALB

## 5. API Gateway With Cognito Authorizer

API Gateway can authorize requests using Cognito User Pool tokens:

```text
API Gateway Cognito Authorizer:
  Client sends: Authorization: Bearer {cognitoIdToken}
  API Gateway validates token against User Pool
  If valid: calls backend Lambda
  If invalid: 401 Unauthorized (no Lambda invocation)
```

vs. Lambda Authorizer:

```text
Lambda Authorizer: custom logic, any token format, can call external auth
Cognito Authorizer: simpler, native, no Lambda needed for JWT validation
```

---

# Topic 2: AWS WAF

## 1. What WAF Does

WAF (Web Application Firewall) inspects HTTP/HTTPS requests and blocks malicious traffic before it reaches your application.

WAF can be attached to:
- CloudFront distributions
- ALB
- API Gateway
- AppSync
- Cognito User Pool

## 2. WAF Web ACL

A Web ACL (Access Control List) contains ordered rules:

```text
Rules are evaluated in priority order (lower number = first)
Each rule has a Statement and an Action:
  Action: Allow, Block, Count, CAPTCHA, Challenge

If no rule matches: default action (Allow or Block)
```

Rule types:
- Managed Rule Groups (AWS or third-party, e.g., AWSManagedRulesCommonRuleSet)
- Rate-based rules (requests per 5 minutes per IP)
- IP set rules (block/allow specific IPs)
- Geo match rules (block countries)
- Regex match rules
- SQL injection match
- XSS match

## 3. AWS Managed Rule Groups

Ready-to-use rule sets maintained by AWS:

| Rule Group | Protects Against |
|---|---|
| AWSManagedRulesCommonRuleSet | OWASP Top 10 (SQLi, XSS, etc.) |
| AWSManagedRulesKnownBadInputsRuleSet | known exploit patterns |
| AWSManagedRulesBotControlRuleSet | automated bots |
| AWSManagedRulesAmazonIpReputationList | known malicious IP addresses |
| AWSManagedRulesAnonymousIpList | VPNs, Tor, proxies |
| AWSManagedRulesSQLiRuleSet | SQL injection patterns |

Start with `AWSManagedRulesCommonRuleSet` in Count mode, review CloudWatch metrics, then switch to Block.

## 4. Rate-Based Rules

Limit requests per IP per 5-minute window:

```json
{
  "Name": "RateLimitPerIP",
  "Priority": 1,
  "Action": {"Block": {}},
  "Statement": {
    "RateBasedStatement": {
      "Limit": 1000,
      "AggregateKeyType": "IP"
    }
  }
}
```

Use cases:
- Prevent login brute force: rate-limit /auth/* to 20 requests/5min per IP
- Prevent scraping: rate-limit /api/products/* to 500 requests/5min
- DDoS mitigation at application layer

## 5. WAF Logging And Metrics

Enable logging to:
- CloudWatch Logs (WAF logs)
- S3 (WAF access logs)
- Kinesis Firehose (streaming)

CloudWatch metrics per rule:
- `AllowedRequests`
- `BlockedRequests`
- `CountedRequests`

Use Count mode first:
```text
New rule -> set action to Count -> review metrics for false positives
After confirming -> switch to Block
```

---

# Topic 3: AWS Shield

## 1. Shield Standard vs Advanced

| Feature | Shield Standard | Shield Advanced |
|---|---|---|
| Cost | free, always on | $3,000/month per org + data transfer |
| Protection | L3/L4 DDoS (network/transport) | L3/L4 + L7 (application) |
| Automatic detection | yes | yes, more sophisticated |
| WAF integration | manual | automatic WAF rules during attack |
| DDoS Response Team (SRT) | no | yes (24/7 support during attacks) |
| Cost protection | no | yes (credits for scaling during DDoS) |
| Global Threat Dashboard | no | yes |

Shield Advanced is justified for:
- public-facing applications that are DDoS targets
- financial services, gaming, media, government
- when $3,000/month is justified by potential DDoS downtime cost

## 2. Shield Advanced DDoS Protection

Shield Advanced monitors:
- EIP (Elastic IPs)
- ALB
- CloudFront
- Route 53
- Global Accelerator

When attack detected:
- Shield Advanced automatically creates WAF rules to mitigate L7 attacks
- Shield Response Team (SRT) can engage within 15 minutes
- Cost protection prevents AWS bill from spiking due to DDoS-triggered scaling

## 3. Architecture For DDoS Resilience

Layered defense:

```text
Internet
-> Route 53 (global Anycast routing, Shield Standard protects DNS)
-> CloudFront (absorbs volumetric attacks at edge, Shield Standard)
-> WAF (blocks L7 attacks: SQLi, XSS, bad bots, rate limiting)
-> ALB (distributes traffic, Shield Standard)
-> ECS/EC2 in private subnet (application layer)
```

Use CloudFront in front of everything — CloudFront's global capacity absorbs volumetric attacks before they reach your origin.

## 4. Common Mistakes

| Mistake | Better Approach |
|---|---|
| No JWT validation on backend | always verify Cognito JWT signature, expiry, iss, aud |
| Accept ID token for API authorization | use access token for API authorization (contains scopes) |
| WAF in Block mode immediately | start in Count mode, review false positives |
| WAF attached to ALB only (not CloudFront) | attach WAF to CloudFront too for edge protection |
| Expose S3 presigned URLs without expiry | set short expiry (300-3600s) on presigned URLs |
| Cognito Identity Pool unauthenticated role with broad permissions | unauthenticated role should have minimal or no permissions |
| No rate-based rule for login endpoints | always rate-limit authentication endpoints |

## 5. Interview Scenarios

**Scenario**: "How do you implement auth for a React SPA calling an API Gateway?"

Strong answer:

```text
Cognito User Pool handles authentication.
The React SPA uses Amplify or AWS SDK to sign in with Cognito hosted UI or custom UI.
On sign-in, the app receives ID token, access token, and refresh token.
All API calls include: Authorization: Bearer {access_token}

API Gateway has a Cognito authorizer configured with the User Pool.
API Gateway validates the JWT automatically — no Lambda authorizer needed.
Backend Lambda receives the request only if the token is valid.

On the backend, I validate specific claims (groups, scopes) for fine-grained authorization.
Refresh token is used client-side to get new tokens before expiry.
```

**Scenario**: "How do you protect an API from a DDoS attack?"

Strong answer:

```text
Defense in depth:
1. CloudFront in front: absorbs volumetric attacks at edge
2. Shield Advanced: auto-detects and mitigates L3/L4 attacks, SRT for L7
3. WAF attached to CloudFront and ALB:
   - AWS Managed Common Rule Set blocks OWASP attacks
   - Rate-based rule: 1000 req/5min per IP globally, lower for auth endpoints
   - IP reputation list blocks known malicious sources
   - Bot Control to manage automated traffic
4. API Gateway throttling: stage-level and method-level rate limits
5. ALB in private VPC: not directly exposed to internet

If attack happens:
- CloudWatch WAF metrics show BlockedRequests spike
- SRT engages if Shield Advanced
- Manually add IP set block rules for attack source ranges
- Increase rate limit strictness temporarily
```

## 6. Revision Notes

- User Pool: authentication, issues JWTs
- Identity Pool: exchanges tokens for temporary AWS credentials
- Always validate JWT: signature, expiry, iss, aud
- ALB native Cognito integration: no code needed for OAuth redirect
- API Gateway Cognito authorizer: simpler than Lambda authorizer for JWT validation
- WAF: always start rules in Count mode before Block
- Shield Standard: free, always on, L3/L4 DDoS only
- Shield Advanced: $3,000/month, L7 protection, SRT access, cost protection
- CloudFront + WAF at edge: first line of L7 defense

## 7. Official Source Notes

- Cognito: <https://docs.aws.amazon.com/cognito/latest/developerguide/what-is-amazon-cognito.html>
- Cognito JWT: <https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html>
- WAF: <https://docs.aws.amazon.com/waf/latest/developerguide/what-is-aws-waf.html>
- Shield: <https://docs.aws.amazon.com/waf/latest/developerguide/shield-chapter.html>
- ALB Cognito auth: <https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html>
