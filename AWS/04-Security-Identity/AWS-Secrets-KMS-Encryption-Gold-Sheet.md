# AWS Security: Secrets Management and Encryption Gold Sheet

> Track: AWS Interview Track — Security and Identity
> Goal: design correct secrets management, understand envelope encryption, and apply encryption at rest across AWS services.

---

## 0. How To Read This

Beginner focus:
- KMS basic concepts (CMK, encrypt/decrypt)
- Secrets Manager vs environment variables
- Encryption at rest for common services

Intermediate focus:
- KMS key policies vs IAM policies
- Envelope encryption mechanism
- Secrets Manager rotation
- SSM Parameter Store vs Secrets Manager

Senior / MAANG focus:
- KMS key hierarchy (CMK wraps DEK)
- Key policies, grants, ViaService
- Cross-account KMS
- Secrets Manager rotation with Lambda
- Audit trail via CloudTrail KMS events
- KMS key rotation and key material expiry

---

# Topic 1: AWS KMS — Key Management Service

## 1. Intuition

KMS manages cryptographic keys for encryption and decryption.

Your data is never stored unencrypted. AWS services use KMS keys to encrypt data at rest.

You control who can use a key via key policies.

## 2. KMS Key Types

| Type | Who Manages | Rotation | Cost |
|---|---|---|---|
| AWS-Managed Key | AWS | automatic yearly | free |
| Customer-Managed Key (CMK) | you | manual or automatic yearly | $1/month per key |
| AWS-Owned Key | AWS (internal) | AWS-controlled | free, not visible |

Interview answer:

```text
Use customer-managed keys when:
- you need to control access via key policy
- you need a CloudTrail audit trail of every encryption/decryption
- you need to cross-account share the key
- you need to disable/delete the key immediately if needed

AWS-managed keys are fine for most services when you don't need that control.
```

## 3. Envelope Encryption

KMS never encrypts large data directly. It uses envelope encryption:

```text
1. KMS generates a Data Encryption Key (DEK) for you
2. KMS gives you:
   - plaintext DEK (use to encrypt your data)
   - encrypted DEK (store alongside your data)
3. You encrypt your data with the plaintext DEK
4. Discard the plaintext DEK from memory
5. Store: encrypted data + encrypted DEK

To decrypt:
1. Send encrypted DEK to KMS -> KMS decrypts it with your CMK -> returns plaintext DEK
2. Decrypt your data with plaintext DEK
3. Discard plaintext DEK from memory
```

Benefits:
- CMK never leaves KMS
- DEK is AES-256, fast for large data
- Encrypted DEK stored with data (no key management overhead)
- CMK compromise: rotate CMK, re-encrypt DEKs (data re-encryption not always needed)

## 4. KMS Key Policy

Every KMS key has a key policy (resource-based policy):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Enable IAM user permissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:root"
      },
      "Action": "kms:*",
      "Resource": "*"
    },
    {
      "Sid": "Allow Lambda to use key",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:role/payment-service-role"
      },
      "Action": [
        "kms:Decrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": "*"
    }
  ]
}
```

KMS key policy vs IAM policy:

```text
Unlike S3, KMS key access requires EITHER key policy OR both key policy AND IAM policy:
- If key policy grants root account access: IAM policies in that account control access
- If key policy doesn't grant root: only principals explicitly listed in key policy can access

To prevent "locked out of key": always include root account as key admin.
```

## 5. KMS Grants

Grants delegate specific KMS permissions to principals for a limited time:

```text
Use grants when:
- you need to allow a service to use a key for a specific operation
- cross-account temporary access
- AWS services like AWS Backup, RDS, EBS use grants behind the scenes

Create grant:
aws kms create-grant \
  --key-id arn:aws:kms:us-east-1:...:key/key-id \
  --grantee-principal arn:aws:iam::...:role/my-role \
  --operations Decrypt GenerateDataKey

Retire grant:
aws kms retire-grant --key-id ... --grant-id ...
```

## 6. KMS ViaService Condition

Restrict CMK usage only through specific AWS services:

```json
{
  "Effect": "Deny",
  "Principal": "*",
  "Action": ["kms:Decrypt", "kms:GenerateDataKey"],
  "Resource": "*",
  "Condition": {
    "StringNotEquals": {
      "kms:ViaService": ["s3.us-east-1.amazonaws.com", "rds.us-east-1.amazonaws.com"]
    }
  }
}
```

This prevents anyone from calling KMS directly — only S3 and RDS can use this key.

## 7. Encryption At Rest For AWS Services

| Service | How To Enable | Key Option |
|---|---|---|
| S3 | bucket default encryption | SSE-S3, SSE-KMS, DSSE-KMS |
| EBS | enable at volume creation | AWS-managed or CMK |
| RDS | enable at DB creation | AWS-managed or CMK |
| DynamoDB | always encrypted (AWS-managed) | CMK optional via table settings |
| Secrets Manager | encrypted by default | CMK optional |
| SSM Parameter Store | SecureString tier uses KMS | AWS-managed or CMK |
| Lambda env vars | encrypted at rest | CMK optional |
| CloudWatch Logs | optional | CMK optional |
| SQS | SSE option | CMK optional |

EBS encryption is set at volume creation — cannot be enabled post-creation without snapshot copy.
RDS encryption is set at DB creation — cannot be enabled post-creation without snapshot restore.

## 8. KMS Audit Trail

Every KMS API call appears in CloudTrail:

```text
Event: kms:Decrypt
  Principal: arn:aws:iam::...:role/payment-service-role
  KeyId: arn:aws:kms:...:key/my-key
  Timestamp: 2025-01-15T10:30:00Z
  EncryptionContext: {"service": "payment", "environment": "prod"}
```

Encryption context:
- key-value pairs passed at encrypt time
- MUST match at decrypt time (prevents unauthorized decryption)
- appears in CloudTrail for audit

---

# Topic 2: AWS Secrets Manager

## 1. Secrets Manager vs SSM Parameter Store

| Feature | Secrets Manager | SSM Parameter Store |
|---|---|---|
| Auto rotation | yes (built-in Lambda rotation) | no (manual) |
| Encryption | always encrypted | SecureString tier (KMS) |
| Cost | $0.40/secret/month + API costs | free (Standard tier), $0.05/param/month (Advanced) |
| Max value size | 64 KB | 4 KB (Standard), 8 KB (Advanced) |
| Versioning | yes, with version staging labels | yes (version numbers) |
| Cross-account | yes (resource policy) | no native cross-account |
| Use case | DB passwords, API keys (need rotation) | config params, feature flags |

Interview answer:

```text
Secrets Manager for anything that needs automatic rotation (DB passwords, OAuth client secrets).
SSM Parameter Store for configuration values, feature flags, non-rotating parameters.
The cost difference ($0.40/month/secret) is worth it for rotated secrets.
```

## 2. Secrets Manager Rotation

How rotation works:

```text
1. Rotation Lambda creates a new version of the secret (AWSPENDING label)
2. Lambda updates the credential at the target service (e.g., DB user password reset)
3. Lambda tests the new credential
4. Lambda moves AWSCURRENT label to new version
5. Lambda deletes AWSPREVIOUS label (after grace period)
```

Built-in rotation templates provided for:
- Amazon RDS (MySQL, PostgreSQL, Aurora)
- Amazon Redshift
- Amazon DocumentDB
- MongoDB

Custom rotation Lambda for other services.

Rotation schedule:

```json
{
  "RotationRules": {
    "AutomaticallyAfterDays": 30
  }
}
```

## 3. Accessing Secrets In Applications

SDK (Java example — cache the secret):

```java
private static String cachedSecret;
private static long cacheExpiry;

public static String getSecret() {
    long now = System.currentTimeMillis();
    if (cachedSecret != null && now < cacheExpiry) {
        return cachedSecret;
    }
    
    GetSecretValueResponse response = secretsManagerClient.getSecretValue(
        GetSecretValueRequest.builder()
            .secretId("prod/payment-service/db-password")
            .build()
    );
    
    cachedSecret = response.secretString();
    cacheExpiry = now + 300_000; // cache for 5 minutes
    return cachedSecret;
}
```

ECS task definition injection:

```json
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:...:secret:prod/db-password"
    }
  ]
}
```

Lambda via environment:

```text
Reference in Lambda env var:
  DB_PASSWORD = {{resolve:secretsmanager:prod/db-password:SecretString:password}}

Resolved at function creation, not at invocation — consider rotation timing.
Better: fetch from SDK at runtime for rotation support.
```

## 4. Cross-Account Secret Access

Secrets Manager resource policy:

```json
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::OtherAccountId:role/consumer-role"
  },
  "Action": [
    "secretsmanager:GetSecretValue",
    "secretsmanager:DescribeSecret"
  ],
  "Resource": "*"
}
```

Also required: KMS key policy must allow the other account's role to Decrypt.

---

# Topic 3: Certificate Manager (ACM)

## 1. ACM Overview

ACM issues and renews SSL/TLS certificates for use with:
- ALB, NLB, API Gateway
- CloudFront
- Elastic Beanstalk

Free for AWS-managed services. Auto-renews 60 days before expiry.

Cannot export private key from ACM (export allowed only for ACM Private CA).

## 2. ACM In Different Regions

```text
CloudFront certificates: must be in us-east-1 (global service requirement)
ALB/NLB certificates: must be in same region as the load balancer

If you forget to create ACM cert in us-east-1 for CloudFront, it won't appear in dropdown.
```

## 3. Private CA

ACM Private CA for internal certificates:

```text
Use for:
- mTLS between internal microservices
- Internal APIs behind NLB
- Device certificates for IoT
```

## 4. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Hardcode DB password in app config | store in Secrets Manager, fetch at runtime |
| Use environment variables for secrets | environment vars visible in Lambda console; use Secrets Manager |
| No caching of fetched secrets | cache with TTL (5-10 min), not every invocation |
| Encrypt sensitive data directly with CMK | use envelope encryption (DEK for data, CMK for DEK) |
| KMS key with no root account in key policy | always include root account to prevent key lockout |
| EBS not encrypted at creation | enable default EBS encryption account-wide |
| No encryption context | always pass encryption context for auditability |
| ACM cert in wrong region for CloudFront | CloudFront requires ACM cert in us-east-1 |

## 5. Revision Notes

- KMS: CMK wraps DEK (envelope encryption); CMK never leaves KMS
- Customer-managed key: $1/month, audit trail, fine-grained key policy
- Key policy: must include root account; IAM policies work if root is in key policy
- Encryption at rest: enable at creation for RDS/EBS (cannot enable post-creation without snapshot)
- Secrets Manager: $0.40/month/secret, auto-rotation, 64 KB, preferred for rotating secrets
- SSM Parameter Store: free (Standard), for config and non-rotating parameters
- Always cache secrets (5-10 min TTL) to avoid per-invocation API calls
- ACM: free for AWS-managed services, CloudFront requires cert in us-east-1

## 6. Official Source Notes

- KMS: <https://docs.aws.amazon.com/kms/latest/developerguide/overview.html>
- Envelope encryption: <https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#enveloping>
- Secrets Manager: <https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html>
- Secrets rotation: <https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets.html>
- ACM: <https://docs.aws.amazon.com/acm/latest/userguide/acm-overview.html>
