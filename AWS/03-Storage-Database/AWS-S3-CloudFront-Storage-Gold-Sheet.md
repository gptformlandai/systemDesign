# AWS Storage: S3 and CloudFront Gold Sheet

> Track: AWS Interview Track — Storage and Database
> Goal: design correct S3 storage architectures, control access securely, optimize cost with lifecycle, and deliver content through CloudFront with the right behaviors.

---

## 0. How To Read This

Beginner focus:
- Buckets, objects, keys
- Public vs private access
- Storage classes basics

Intermediate focus:
- Versioning, lifecycle policies
- Pre-signed URLs, presigned POST
- S3 event notifications
- Storage class comparison and cost

Senior / MAANG focus:
- S3 access patterns and data lake design
- OAC, signed URLs/cookies for private content
- S3 Replication (CRR/SRR) and consistency model
- S3 performance patterns (multipart, prefix design, byte-range)
- CloudFront behaviors, cache policies, Lambda@Edge
- S3 cost traps (per-request pricing, data transfer, intelligent tiering overhead)

---

# Topic 1: Amazon S3

## 1. Intuition

S3 is durable, scalable object storage.

Objects live in buckets. Each object has a key (path-like string), value (bytes), metadata, and optional tags. There is no file system. There are no folders. Slashes in keys simulate folder structure.

Durability: 11 nines (99.999999999%). Availability: 99.99%.

## 2. Storage Classes

| Class | Access Pattern | Min Duration | Cost Strategy |
|---|---|---|---|
| S3 Standard | frequent access | none | default for active data |
| S3 Standard-IA | infrequent (monthly) | 30 days | 40% cheaper storage, per-retrieval fee |
| S3 One Zone-IA | infrequent, one AZ | 30 days | 20% cheaper than Standard-IA, lower resilience |
| S3 Glacier Instant Retrieval | archive, ms access | 90 days | quarterly access, instant retrieval |
| S3 Glacier Flexible Retrieval | archive, minutes-hours | 90 days | bulk retrieval cheapest (5-12 hrs) |
| S3 Glacier Deep Archive | compliance archive | 180 days | cheapest storage, 12-48hr retrieval |
| S3 Intelligent-Tiering | unknown or changing access | none | auto-moves between tiers, small monitoring fee per object |

Cost trap:

```text
Standard-IA and Glacier classes charge a per-GB retrieval fee.
Frequent small reads of IA objects can cost MORE than Standard.
Use Intelligent-Tiering for unpredictable access patterns.
Use Standard for objects < 128 KB (Intelligent-Tiering monitoring overhead exceeds savings).
```

## 3. Versioning

Enable versioning to protect against accidental deletion and overwrite:

```text
PUT object: creates a new version
DELETE object: creates a delete marker (object hidden, not removed)
DELETE specific version: permanently removes that version

MFA Delete: require MFA to permanently delete versions (extra protection)
```

Versioning + lifecycle rules:

```text
Expire noncurrent versions after 30 days
Delete delete markers with no noncurrent versions
```

## 4. Lifecycle Rules

Automate cost optimization:

```json
{
  "Rules": [{
    "Status": "Enabled",
    "Transitions": [
      {"Days": 30, "StorageClass": "STANDARD_IA"},
      {"Days": 90, "StorageClass": "GLACIER_IR"},
      {"Days": 365, "StorageClass": "DEEP_ARCHIVE"}
    ],
    "NoncurrentVersionExpiration": {"NoncurrentDays": 30},
    "Expiration": {"ExpiredObjectDeleteMarker": true}
  }]
}
```

Common patterns:
- 30 days → IA for logs and backups
- 90 days → Glacier for compliance archive
- Expire noncurrent versions for versioned buckets

## 5. S3 Security

### Bucket Policy vs IAM Policy

| Policy Type | Scope | Allows |
|---|---|---|
| Bucket Policy | resource-based, attached to bucket | cross-account access, public access, IP restrictions |
| IAM Policy | identity-based, attached to user/role | service access from within AWS account |

Both must allow the operation for cross-account access.

### Block Public Access

Always enabled for production buckets. Four settings:
- Block new public ACLs
- Remove public ACL effects
- Block public bucket policies
- Remove public bucket policy effects

### S3 ACLs

Largely obsolete. Use bucket policies and IAM instead. AWS recommends disabling S3 ACLs.

### Encryption

| Type | How | Use Case |
|---|---|---|
| SSE-S3 | AWS-managed key, per-object | default for most |
| SSE-KMS | your CMK or AWS-managed KMS key | audit trail, key rotation control |
| SSE-C | customer-provided key | you manage keys completely |
| Client-side | encrypted before upload | most control, most complexity |

Force encryption via bucket policy:

```json
{
  "Condition": {
    "StringNotEquals": {
      "s3:x-amz-server-side-encryption": "aws:kms"
    }
  },
  "Effect": "Deny"
}
```

## 6. Pre-Signed URLs And Presigned POST

Pre-signed URL: time-limited direct access to a specific object.

```python
import boto3

s3 = boto3.client('s3')

# Generate download URL (valid 1 hour)
url = s3.generate_presigned_url(
    'get_object',
    Params={'Bucket': 'my-bucket', 'Key': 'uploads/document.pdf'},
    ExpiresIn=3600
)

# Generate upload URL (valid 5 minutes)
url = s3.generate_presigned_url(
    'put_object',
    Params={'Bucket': 'my-bucket', 'Key': 'uploads/user-123/photo.jpg'},
    ExpiresIn=300
)
```

Pre-signed POST: form-based upload with server-side policy constraints:

```text
Use presigned POST when:
- client uploads directly to S3 from browser (avoids routing through backend)
- you need to enforce file size limit, content type at upload time
- you want server to control upload policy but not proxy the upload bytes
```

## 7. S3 Event Notifications

Trigger processing when objects are created/deleted:

```text
S3 -> SQS / SNS / Lambda / EventBridge

Use cases:
- Image uploaded -> Lambda for thumbnail generation
- CSV uploaded -> Lambda for ETL to RDS
- Log file uploaded -> Lambda for parsing and CloudWatch metrics
```

## 8. S3 Performance Patterns

Multipart upload:
- for objects > 100 MB (recommended), > 5 GB (required)
- parallel part uploads for speed
- automatically retry failed parts

Byte-range fetch:
- download specific byte ranges in parallel
- useful for large file partial reads (video seeking, partial CSV processing)

Prefix design:
- S3 partitions by key prefix automatically
- no need to randomize prefixes for performance (previous limit removed)
- organize by logical hierarchy: `uploads/{userId}/{date}/{filename}`

S3 Transfer Acceleration:
- routes uploads through CloudFront edge locations
- 50-500% faster for long-distance uploads
- extra cost per GB

## 9. S3 Replication

| Type | Direction | Use Case |
|---|---|---|
| CRR (Cross-Region Replication) | source → destination in different region | DR, data sovereignty, latency |
| SRR (Same-Region Replication) | source → destination in same region | test/staging copy, cross-account copy |

Requirements:
- versioning must be enabled on both source and destination
- replication applies to new objects (does not backfill existing objects)
- delete markers not replicated by default (configure explicitly)

## 10. S3 Consistency Model

Since 2020, S3 offers strong read-after-write consistency:

```text
PUT then immediate GET: always returns the new object
DELETE then immediate GET: always returns 404
LIST after PUT: reflects the new object immediately
```

No longer need workarounds for eventual consistency.

---

# Topic 2: CloudFront (Deep Dive)

## 1. Behaviors And Cache Policies

Each behavior matches a URL path pattern and specifies:
- origin (S3, ALB, custom)
- cache policy (what to cache, TTL)
- origin request policy (what headers/cookies to forward to origin)
- viewer protocol policy (HTTPS only, redirect, or allow HTTP)
- allowed HTTP methods

Cache policy controls:
- minimum, maximum, default TTL
- cache key: which headers/cookies/query strings create separate cache entries

Origin request policy controls:
- which headers to forward to origin (host, authorization, custom headers)
- which cookies to forward
- which query strings to forward

## 2. Origin Access Control (OAC)

The current best practice for S3 origins:

```text
OAC Configuration:
  Signing behavior: Always (sign all requests)
  Origin type: S3

Bucket policy grant:
{
  "Principal": {
    "Service": "cloudfront.amazonaws.com"
  },
  "Effect": "Allow",
  "Action": "s3:GetObject",
  "Condition": {
    "StringEquals": {
      "AWS:SourceArn": "arn:aws:cloudfront::ACCOUNT:distribution/DISTRIBUTION_ID"
    }
  }
}
```

This ensures only the specific CloudFront distribution can read from the bucket.

## 3. Signed URLs And Signed Cookies

| Method | Granularity | Use Case |
|---|---|---|
| Signed URL | per object per URL | individual file download, one-time links |
| Signed Cookie | per session across multiple objects | video playlist, multi-file access for authenticated user |

Both require:
- CloudFront key group attached to behavior
- key pair for signing
- trusted key groups configured in distribution

## 4. Lambda@Edge Trigger Points

```text
Viewer Request:  (before cache lookup)
  -> URL normalization, auth header injection, A/B test cookie setting

Origin Request:  (cache miss, before calling origin)
  -> add custom headers to origin request, origin failover logic

Origin Response: (origin response received, before caching)
  -> add security headers, modify origin response before caching

Viewer Response: (after cache, before returning to user)
  -> add security headers, set cookies, modify final response
```

Lambda@Edge constraints:
- Max timeout: 5 seconds (Viewer), 30 seconds (Origin)
- No VPC access
- No Lambda layers
- Deployed to us-east-1, replicated globally

CloudFront Functions (lighter alternative):
- sub-millisecond execution
- JavaScript only
- Viewer Request and Viewer Response events only
- no network calls
- good for URL rewrites, simple redirects, header manipulation

## 5. Security Headers Via CloudFront

Add security headers at CloudFront level (Origin Response event or Response Headers Policy):

```text
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
```

Use CloudFront Response Headers Policy (no Lambda needed) for standard security headers.

## 6. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Public S3 bucket for website content | S3 private + CloudFront + OAC |
| Same cache behavior for /api/* and /static/* | separate behaviors with different TTLs |
| Caching personalized content (user-specific responses) | vary on authorization/cookies, or set TTL=0 |
| No HTTPS on distribution | enable redirect HTTP to HTTPS |
| No WAF on public APIs | attach WAF web ACL to CloudFront |
| No lifecycle rules on log/backup buckets | set lifecycle to IA → Glacier → expire |
| Not enabling versioning on critical buckets | enable versioning before first object write |

## 7. Interview Scenarios

**Scenario**: "How do you allow users to upload profile photos directly to S3 from a browser?"

Strong answer:

```text
I generate a presigned URL for PUT from the backend (after auth verification).
The frontend uses that URL to PUT the file directly to S3 (no file bytes through backend).
The key is scoped to the user: uploads/{userId}/profile.jpg
After upload, S3 event notification triggers a Lambda to resize/validate the image.
The resized image is served through CloudFront with OAC for bucket protection.
The original bucket blocks all public access. Only CloudFront + the resize Lambda can read it.
```

**Scenario**: "Explain how you would serve private video content to paying subscribers."

Strong answer:

```text
Videos are stored in a private S3 bucket. CloudFront sits in front with OAC.
The behavior for /videos/* requires signed cookies.
When a subscriber authenticates, the API server validates their subscription,
generates a CloudFront signed cookie (valid for the session duration), and returns it.
The browser sends this cookie with every CloudFront request to /videos/*.
CloudFront validates the cookie; unsigned requests return 403.
Lambda@Edge at Origin Request can also verify session tokens for additional auth.
```

## 8. Revision Notes

- S3 storage classes: Standard → IA (retrieval fee) → Glacier variants (archive)
- Intelligent-Tiering: auto-moves, monitoring fee per object, skip for objects < 128 KB
- Always block public access; use bucket policy + IAM for controlled access
- OAC: S3 bucket only accessible through CloudFront distribution
- Presigned URL: time-limited, user-specific, no credentials exposed in browser
- S3 events: trigger Lambda/SQS/SNS/EventBridge for object events
- CloudFront behaviors: separate cache policies for static vs dynamic paths
- Lambda@Edge: complex edge logic; CloudFront Functions: fast URL rewrites

## 9. Official Source Notes

- S3: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html>
- S3 storage classes: <https://aws.amazon.com/s3/storage-classes/>
- S3 presigned URLs: <https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html>
- CloudFront: <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Introduction.html>
- CloudFront OAC: <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html>
