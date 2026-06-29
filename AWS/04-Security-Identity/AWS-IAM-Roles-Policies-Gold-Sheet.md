# AWS Security: IAM Roles and Policies Gold Sheet

> Track: AWS Interview Track — Security and Identity
> Goal: understand IAM deeply enough to implement least-privilege access, explain policy evaluation logic, and design secure cross-account patterns.

---

## 0. How To Read This

Beginner focus:
- IAM users, groups, roles, policies
- Inline vs managed policies
- Console vs programmatic access

Intermediate focus:
- Policy evaluation logic
- Cross-account assume role
- IAM role for EC2 instance profile
- IRSA for EKS

Senior / MAANG focus:
- Permission boundaries
- Service control policies (SCPs)
- Resource-based vs identity-based policy interaction
- PassRole
- IAM policy conditions (IP, MFA, time, resource tags)
- Least-privilege principle at MAANG scale

---

# Topic 1: IAM Fundamentals

## 1. Core IAM Entities

| Entity | What It Is |
|---|---|
| User | person or service with long-term credentials (access key + password) |
| Group | collection of users, assign policies to the group |
| Role | assumable identity with temporary credentials, no long-term key |
| Policy | JSON document defining permissions (Allow/Deny, actions, resources) |
| Permission Boundary | max permissions a role or user can ever have |

Interview line:

```text
For applications on AWS (Lambda, EC2, ECS, EKS), always use IAM Roles, never IAM Users.
Roles provide temporary credentials via STS, no static access keys to rotate or leak.
```

## 2. Policy Types

| Type | Attached To | Purpose |
|---|---|---|
| Identity-based policy | user, group, role | grant permissions to the identity |
| Resource-based policy | resource (S3, SQS, KMS, Lambda) | grant permissions to identities to access that resource |
| SCP (Service Control Policy) | AWS Organizations OU or account | set maximum permissions for accounts (does not grant) |
| Permission boundary | user or role | set maximum permissions for that identity |
| Session policy | temporary session | further restrict assumed role session |

## 3. Policy Structure

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowS3ReadAccess",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ],
      "Condition": {
        "StringEquals": {
          "aws:RequestedRegion": "us-east-1"
        }
      }
    }
  ]
}
```

Key elements:
- `Effect`: Allow or Deny
- `Action`: AWS API actions (s3:GetObject, ec2:DescribeInstances, etc.)
- `Resource`: ARN(s) the action applies to
- `Condition`: optional condition context keys

## 4. Policy Evaluation Logic

This is the most-tested IAM concept:

```text
Step 1: Explicit DENY? -> DENY (always wins, regardless of any allow)
Step 2: Is there an SCP in the Organizations path? -> if SCP doesn't allow it, DENY
Step 3: Is there a Resource-based policy with explicit allow? -> may allow for cross-account
Step 4: Is there a Permission boundary? -> if boundary doesn't allow it, DENY
Step 5: Is there an Identity-based policy Allow? -> ALLOW
Step 6: Nothing else? -> DENY (implicit deny by default)
```

Summary of overrides:

```text
Explicit Deny beats everything.
Implicit Deny is the default (no policy = no access).
SCP restricts what accounts can do.
Permission Boundary restricts what a role can do.
```

Same-account cross-service:

```text
S3 bucket policy + IAM role:
  Only ONE needs to allow it for same-account access
  (either the resource policy or the identity policy)
```

Cross-account:

```text
For cross-account, BOTH must allow:
  Identity policy in Account A must allow the action
  Resource policy in Account B must allow Account A's role
```

---

# Topic 2: IAM Roles And Assume Role

## 1. What IAM Roles Are

A role is an identity with temporary credentials obtained via STS (Security Token Service).

Flow:

```text
Principal (user, service, another role)
-> calls sts:AssumeRole
-> STS returns: AccessKeyId, SecretAccessKey, SessionToken (valid 15 min - 12 hours)
-> caller uses temporary credentials for allowed API calls
```

## 2. Trust Policy

Every role has a trust policy that defines who can assume it:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Service": "ec2.amazonaws.com"
    },
    "Action": "sts:AssumeRole"
  }]
}
```

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "AWS": "arn:aws:iam::111122223333:role/DevOpsRole"
    },
    "Action": "sts:AssumeRole",
    "Condition": {
      "Bool": {
        "aws:MultiFactorAuthPresent": "true"
      }
    }
  }]
}
```

## 3. EC2 Instance Profile

An EC2 instance profile is a container for a single IAM role:

```text
EC2 instance -> associated with instance profile -> code running on instance
-> calls ec2 metadata API (169.254.169.254) -> gets temporary credentials
-> no need to put access keys on EC2

Never put access keys on EC2. Always use instance profiles.
```

## 4. IRSA — IAM Roles For Service Accounts (EKS)

IRSA gives each Kubernetes pod its own IAM role, not the entire EC2 node's role:

```text
EKS cluster -> OIDC identity provider
Service account annotated with IAM role ARN
Pod uses service account
Pod receives JWT token
AWS STS validates JWT via cluster's OIDC endpoint
Returns temporary credentials for the specific IAM role
```

Setup:

```bash
eksctl create iamserviceaccount \
  --name payment-service \
  --namespace prod \
  --cluster my-cluster \
  --attach-policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess \
  --approve
```

Interview line:

```text
Without IRSA, all pods on a node share the EC2 instance profile permissions.
A compromised pod could access any resource the node role allows.
IRSA gives pod-level IAM isolation with the principle of least privilege.
```

## 5. PassRole

PassRole is a special permission that controls who can assign an IAM role to a service:

```text
Problem: if I can pass any role to Lambda, I can create a Lambda with an admin role
and execute it to do admin actions — privilege escalation.

iam:PassRole permission must specify which roles the user can pass:
```

```json
{
  "Effect": "Allow",
  "Action": "iam:PassRole",
  "Resource": "arn:aws:iam::123456789012:role/AllowedLambdaExecutionRole"
}
```

Always scope `iam:PassRole` to specific role ARNs, never `*`.

## 6. Permission Boundaries

A permission boundary is a policy that limits the maximum permissions of a role:

```text
The effective permissions = intersection of:
  identity policy AND permission boundary

If the role has admin policy but the boundary allows only S3:
  Effective: S3 access only

Use cases:
- Give developers ability to create roles but limit what they can grant
  (developer cannot create an admin role because boundary restricts it)
- Guardrail for automation that creates roles (e.g., CDK, Terraform)
```

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:*", "dynamodb:*"],
      "Resource": "*"
    },
    {
      "Effect": "Deny",
      "Action": ["iam:*", "organizations:*"],
      "Resource": "*"
    }
  ]
}
```

## 7. Service Control Policies (SCPs)

SCPs are applied at AWS Organizations level:

```text
Applied to: OU (Organizational Unit) or individual account
Effect: sets maximum permissions (does not grant — only restricts)
Purpose: prevent specific actions regardless of IAM policies in the account
```

Common SCP examples:

Deny leaving the organization:

```json
{
  "Effect": "Deny",
  "Action": "organizations:LeaveOrganization",
  "Resource": "*"
}
```

Deny non-us-east-1 region:

```json
{
  "Effect": "Deny",
  "Action": "*",
  "Resource": "*",
  "Condition": {
    "StringNotEquals": {
      "aws:RequestedRegion": "us-east-1"
    }
  }
}
```

Require MFA for sensitive actions:

```json
{
  "Effect": "Deny",
  "Action": ["iam:DeleteRole", "ec2:TerminateInstances"],
  "Resource": "*",
  "Condition": {
    "BoolIfExists": {
      "aws:MultiFactorAuthPresent": "false"
    }
  }
}
```

## 8. Policy Conditions

| Condition Key | Meaning |
|---|---|
| `aws:RequestedRegion` | restrict actions to specific regions |
| `aws:MultiFactorAuthPresent` | require MFA |
| `aws:SourceIp` | restrict by caller IP CIDR |
| `aws:SourceVpc` | restrict to VPC traffic only |
| `aws:PrincipalOrgID` | restrict to identities in your AWS Organization |
| `s3:prefix` | restrict S3 access to specific prefix |
| `ec2:ResourceTag` | restrict to instances with specific tag |

Tag-based access control:

```json
{
  "Effect": "Allow",
  "Action": "ec2:StopInstances",
  "Resource": "*",
  "Condition": {
    "StringEquals": {
      "ec2:ResourceTag/Owner": "${aws:username}"
    }
  }
}
```

## 9. Common Mistakes

| Mistake | Better Approach |
|---|---|
| IAM User with access keys for EC2 apps | EC2 instance profile (IAM Role) |
| Admin role for all Lambda functions | separate least-privilege role per function |
| Wildcard resource in policies (`*`) | scope to specific resource ARNs |
| No permission boundary on developer-created roles | permission boundaries as org guardrail |
| Forget PassRole scope, allow `*` | scope PassRole to specific role ARNs |
| SCP thought to grant permissions | SCPs only restrict, never grant |
| Shared IAM user for multiple services | separate role per service, separate trust policy |

## 10. Interview Scenario

**Scenario**: "A Lambda function needs to read from an S3 bucket in another account. How do you set this up?"

Strong answer:

```text
Two things must both allow the access:

1. Lambda execution role (identity-based policy in Account A):
{
  "Effect": "Allow",
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::cross-account-bucket/*"
}

2. S3 bucket policy in Account B (resource-based policy):
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::AccountA:role/lambda-execution-role"
  },
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::cross-account-bucket/*"
}

If SCP in Account B restricts S3 access to same-account only, this will still fail.
Always check SCPs in Organizations when cross-account access is unexpectedly denied.
```

## 11. Revision Notes

- IAM evaluation: explicit deny > SCP > resource policy > permission boundary > identity policy
- Roles: temporary credentials via STS; always use for services, never static keys
- Trust policy: defines who can assume the role
- IRSA: pod-level IAM in EKS, not node-level
- PassRole: must be scoped to specific role ARNs to prevent privilege escalation
- Permission boundary: caps max permissions; effective = intersection with identity policy
- SCPs: max permissions at org level; do not grant, only restrict
- Cross-account: both identity policy AND resource policy must allow

## 12. Official Source Notes

- IAM: <https://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html>
- Policy evaluation: <https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic.html>
- SCPs: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html>
- IRSA: <https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html>
- Permission boundaries: <https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_boundaries.html>
