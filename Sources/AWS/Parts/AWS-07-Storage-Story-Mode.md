# AWS Storage Through Story Mode: Where Your App's Data Actually Lives

> You have a React frontend, a Spring Boot backend, a database, maybe some uploaded files, maybe some cache. On your laptop, all of this feels simple. On AWS, you suddenly have many storage choices: S3, EBS, EFS, RDS, Aurora, DynamoDB, ElastiCache. This guide explains them in a practical, story-mode way so you can understand what goes where without getting lost.

---

# Table of Contents

1. [How Storage Feels on Your Laptop](#1-how-storage-feels-on-your-laptop)
2. [What Changes When You Move to AWS](#2-what-changes-when-you-move-to-aws)
3. [The Big Mental Model: Three Kinds of Storage](#3-the-big-mental-model-three-kinds-of-storage)
4. [Story Mode: Your App Starts Simple](#4-story-mode-your-app-starts-simple)
5. [Storage for the Frontend](#5-storage-for-the-frontend)
6. [Storage for the Backend Server Itself](#6-storage-for-the-backend-server-itself)
7. [Storage for Application Data](#7-storage-for-application-data)
8. [Storage for User Uploaded Files](#8-storage-for-user-uploaded-files)
9. [Storage for Shared Files Across Servers](#9-storage-for-shared-files-across-servers)
10. [Storage for Cache and Fast Reads](#10-storage-for-cache-and-fast-reads)
11. [Putting It All Together for a Real App](#11-putting-it-all-together-for-a-real-app)
12. [Backups, Durability, and Recovery](#12-backups-durability-and-recovery)
13. [Common Mistakes and Debugging Tips](#13-common-mistakes-and-debugging-tips)
14. [Interview-Ready Answers](#14-interview-ready-answers)
15. [Quick Revision Sheet](#15-quick-revision-sheet)

---

# 1. How Storage Feels on Your Laptop

On your laptop, your app usually looks like this:

```text
frontend/
backend/
uploads/
local-postgres-data/
```

And conceptually:

```text
React frontend
  -> browser loads built files from local dev server

Spring Boot backend
  -> reads config from local files or env vars
  -> stores data in local PostgreSQL
  -> maybe writes uploads to a local folder like ./uploads
  -> maybe caches some things in memory
```

Everything sits on one machine, so storage feels invisible.

Examples:

- your React build files are just files on disk
- your Spring Boot JAR is just a file on disk
- PostgreSQL stores its data on your local disk
- uploaded PDFs may go into a local folder
- cache may live inside JVM memory

This works locally because:

- there is only one machine
- there is no scaling yet
- if the machine dies, it is just your dev environment

Production is different.

---

# 2. What Changes When You Move to AWS

On AWS, you stop asking:

```text
"Where is my file on disk?"
```

and start asking:

```text
"What kind of data is this?"
"Who needs to access it?"
"Does it need SQL?"
"Is it a file, a disk, or an object?"
"Does it need to survive server replacement?"
"Will one server use it, or many servers?"
```

That is the key shift.

AWS storage is not one big thing. Different data belongs in different places.

For your app, you usually split storage like this:

```text
Frontend static files        -> S3
Backend boot/root disk       -> EBS
Database records             -> RDS / Aurora / DynamoDB
User uploads                 -> S3
Shared filesystem            -> EFS (only if needed)
Cache / sessions / hot data  -> ElastiCache
```

That is the big picture.

---

# 3. The Big Mental Model: Three Kinds of Storage

This is the most important section.

## 3.1 Object Storage

Think: "store a whole file or blob by name."

Examples:

- image
- PDF
- video
- React build output
- logs archive
- backup file

AWS service:

- **S3**

Real-life analogy:

```text
S3 is like a giant warehouse.

You do not say:
  "Give me sector 4, block 19, byte 200"

You say:
  "Give me the box named invoices/2026/march/report.pdf"
```

## 3.2 Block Storage

Think: "a disk attached to a machine."

Examples:

- EC2 root volume
- database files on a VM
- application filesystem on one server

AWS service:

- **EBS**

Real-life analogy:

```text
EBS is like a hard drive attached to one computer.
It behaves like a disk.
The machine formats it, mounts it, reads and writes blocks.
```

## 3.3 File Storage

Think: "a shared filesystem multiple machines can mount."

Examples:

- shared document folder
- legacy app needing common shared files
- multiple app servers reading the same files

AWS service:

- **EFS**

Real-life analogy:

```text
EFS is like a shared company drive.
Many servers can mount it and see the same files.
```

## 3.4 Database Storage

Think: "structured application records."

Examples:

- users
- orders
- payments
- inventory
- shopping cart items

AWS services:

- **RDS**
- **Aurora**
- **DynamoDB**

This is not file storage. This is application data storage.

## 3.5 Cache Storage

Think: "temporary fast-access data."

Examples:

- frequently read product data
- session data
- OTP/rate-limit counters
- expensive query results

AWS service:

- **ElastiCache**

Real-life analogy:

```text
Cache is like the receptionist's desk drawer.
Important things that are needed often stay close at hand.
If the drawer is cleared, the office still works, but some things get slower.
```

---

# 4. Story Mode: Your App Starts Simple

Let us follow a realistic app.

You have:

- React frontend
- Spring Boot backend
- PostgreSQL database
- users upload profile pictures and invoices
- dashboard reads same reports often
- maybe several app servers later

At the beginning, you do something like this locally:

```text
Frontend files               -> frontend/dist on laptop
Backend JAR                  -> target/app.jar on laptop
Database                     -> local PostgreSQL data directory
User uploads                 -> ./uploads folder
Cache                        -> JVM memory or none
```

That is okay for local development.

Then production starts and problems appear:

- what if the EC2 instance dies?
- what if you scale from 1 backend instance to 3?
- what if uploads are stored on one server and another server cannot see them?
- what if users need to download files globally?
- what if DB backups are needed?

Now AWS storage choices matter.

---

# 5. Storage for the Frontend

## 5.1 What the Frontend Actually Is

For a normal React SPA, the frontend build output is just static files:

- `index.html`
- JS bundles
- CSS
- images

This is not database data.
This is not shared filesystem data.
This is static asset data.

## 5.2 Best AWS Home for Frontend Files

Use:

- **S3** to store the built files
- **CloudFront** to serve them globally

Why?

- cheap
- durable
- scalable
- perfect for static files

Real-life flow:

```text
1. You run npm run build
2. React produces static files
3. CI/CD uploads them to S3 bucket: myapp-frontend-prod
4. CloudFront serves them to users worldwide
```

This is much better than storing React files on an EC2 disk.

## 5.3 Why Not Store React Files on EC2 EBS?

You can, but it is usually the wrong default.

Why not?

- EC2 disk is tied to that server
- if the instance is replaced, you redeploy again
- serving static files from S3 + CloudFront is cheaper and cleaner
- no need to waste backend compute on static assets

So for frontend static files:

```text
Right answer   -> S3
Wrong default  -> EC2 disk
```

---

# 6. Storage for the Backend Server Itself

This section is about the backend machine, not the backend data.

## 6.1 If You Run on EC2

An EC2 instance needs a disk.

That disk is usually:

- **EBS**

Typical uses of EBS on EC2:

- OS boot volume
- storing application binaries temporarily
- logs before shipping them elsewhere
- local temp files

Real-life example:

```text
EC2 instance running Spring Boot
  -> root volume on EBS
  -> app.jar may sit on EBS
  -> Linux OS sits on EBS
```

## 6.2 Important Rule About EBS

Do not confuse:

```text
"disk of the server"
```

with:

```text
"long-term application data"
```

Your EC2 disk is not where you should keep business-critical uploaded files or your primary database unless you intentionally chose that architecture.

Why?

- servers get replaced
- Auto Scaling may terminate instances
- local files on one instance are not visible to other instances

Use EBS for the machine.
Use better storage choices for shared or durable app data.

## 6.3 If You Run on ECS or EKS

Then you usually care less about a disk attached to a server.

Why?

- containers are treated as replaceable
- local container storage is temporary
- if a container restarts, local written files may disappear

So the rule becomes even stronger:

```text
Never store important application data inside container local storage.
```

Use S3, RDS, EFS, or ElastiCache instead depending on the data type.

---

# 7. Storage for Application Data

This is your real business data.

Examples:

- users
- orders
- invoices
- product catalog
- payments
- subscriptions

This is not a file problem. This is a database problem.

## 7.1 Default Choice for a Typical Spring Boot App

If your app has tables, joins, transactions, and relational data:

Use:

- **RDS PostgreSQL** or **RDS MySQL**

Real-life example:

```text
users table
orders table
order_items table
payments table
```

That belongs in RDS.

Why?

- SQL queries
- ACID transactions
- joins
- indexes
- backups
- Multi-AZ availability options

## 7.2 RDS in Story Mode

Local version:

```text
Spring Boot -> localhost:5432
```

AWS version:

```text
Spring Boot -> myapp-db.abc123.us-east-1.rds.amazonaws.com:5432
```

Same idea, different host.

Spring Boot still connects with JDBC.
But now AWS manages the database infrastructure better than a self-managed VM in many cases.

## 7.3 When Aurora Enters the Story

Aurora is still relational, still SQL, still for app data.

You choose **Aurora** instead of normal RDS when:

- you want stronger managed scalability
- you want higher performance characteristics
- you want faster failover and AWS-optimized DB internals
- your workload is growing and cost/benefit makes sense

Simple rule:

```text
RDS PostgreSQL/MySQL -> normal default for many apps
Aurora               -> stronger managed relational option when scale/HA needs grow
```

## 7.4 When DynamoDB Enters the Story

Sometimes your app data is not best modeled relationally.

Examples:

- shopping cart by user ID
- session state
- key-value profile data
- huge request-driven workloads with simple access patterns

Then **DynamoDB** can be the right answer.

Real-life example:

```text
Get cart by user_id
Update cart by user_id
Expire cart after 7 days
```

That is a classic DynamoDB use case.

But if you need:

- joins
- complex reporting queries
- normalized relational structure

then RDS/Aurora is usually better.

## 7.5 Practical Guidance for Your App

If you have a normal Spring Boot business application:

```text
Users / orders / payments / subscriptions  -> RDS or Aurora
Session store / carts / high-scale key-value -> DynamoDB or Redis depending on use case
```

Start simple:

- choose RDS first for relational business data
- choose DynamoDB only when access patterns clearly justify it

---

# 8. Storage for User Uploaded Files

This is one of the most common real-life questions.

Users upload:

- profile images
- resumes
- invoices
- videos
- PDFs

Where do these go?

## 8.1 Wrong First Instinct

Many people think:

```text
"I'll save uploads in /uploads on the EC2 server."
```

This breaks quickly.

Why?

- if you have 3 backend servers, uploads saved on server A are not on B or C
- if the instance dies, files may be lost
- scaling becomes messy
- backups become awkward

## 8.2 Correct Default

Store uploads in:

- **S3**

Store metadata in:

- **RDS** or another database

Real-life pattern:

```text
S3 stores the actual file:
  s3://myapp-documents-prod/invoices/2026/03/invoice-123.pdf

RDS stores metadata:
  file_id
  user_id
  s3_key
  uploaded_at
  content_type
  size_bytes
```

That is the correct separation.

## 8.3 Best Upload Flow in Real Life

### Option A: Backend receives file and uploads to S3

```text
Browser -> Spring Boot -> S3
```

Simple, but backend handles the file bytes.

### Option B: Pre-signed URL (better default)

```text
1. Browser asks Spring Boot: "I want to upload a PDF"
2. Spring Boot creates a pre-signed S3 upload URL
3. Browser uploads directly to S3
4. Browser or backend stores metadata in RDS
```

Why this is better:

- backend does not carry large file upload traffic
- simpler scaling
- cheaper and cleaner architecture

## 8.4 Download Flow

For downloads:

- private download via pre-signed URL
- or serve through CloudFront if broad/global distribution matters

Example:

```text
Private invoice download:
  backend checks authorization
  backend returns pre-signed S3 URL valid for 5 minutes

Public image download:
  CloudFront serves S3 object globally
```

---

# 9. Storage for Shared Files Across Servers

Sometimes teams ask:

```text
"What if multiple EC2 instances need to read the same files?"
```

That is where **EFS** comes in.

## 9.1 What EFS Is Good For

Use EFS when:

- multiple servers need the same filesystem
- app expects POSIX-style files and folders
- legacy app cannot easily move to S3 object model
- containers need shared RWX filesystem behavior

Real-life examples:

- CMS with shared media directory
- legacy Java app writing generated reports to a shared mounted folder
- ML jobs reading the same dataset as files

## 9.2 Why EFS Is Not the Default

Many teams ask about EFS too early.

Why not default to it?

- more expensive than S3 for many use cases
- slower than local disk for some patterns
- unnecessary if S3 object storage solves the problem better

Simple rule:

```text
Need shared files with filesystem semantics? -> EFS
Need file/object storage, upload/download, assets? -> S3
```

## 9.3 Example: When EFS Is Actually Right

Imagine:

- 4 ECS tasks generate PDF reports
- another service reads them from the same shared folder
- the app is hardcoded for filesystem paths like `/reports/monthly/report.pdf`

Then EFS may be practical.

But if you control the design, S3 is often cleaner.

---

# 10. Storage for Cache and Fast Reads

Not all storage is about durability. Some storage is about speed.

## 10.1 What Cache Is For

Suppose your dashboard hits the same expensive query repeatedly:

```text
SELECT * FROM sales_summary WHERE region = 'US' AND month = '2026-03'
```

If 10,000 users ask for the same thing, hitting RDS every time is wasteful.

Use:

- **ElastiCache (Redis)**

## 10.2 Story Mode Example

```text
User opens dashboard
  -> backend checks Redis for dashboard:US:2026-03
  -> if present, return instantly
  -> if not present, query RDS, build response, store in Redis with TTL, return
```

That is cache-aside.

## 10.3 What Belongs in Cache

Good candidates:

- frequently read product details
- session data
- OTP / token validation data
- rate limit counters
- expensive query results

Bad candidates:

- your only copy of important business data
- things needing perfect permanence

## 10.4 Key Rule

If Redis is lost, the app should become slower, not incorrect forever.

That is the mindset.

Cache is a performance layer, not your system of record.

---

# 11. Putting It All Together for a Real App

Let us map a realistic app end to end.

## 11.1 Example App

You have:

- React frontend
- Spring Boot backend
- PostgreSQL database
- users upload profile images and invoices
- dashboard reads hot summary data
- maybe reports are generated nightly

## 11.2 Best Storage Mapping

```text
React build output             -> S3
Global frontend delivery       -> CloudFront

Spring Boot instance root disk -> EBS (if EC2)
Container local storage        -> temporary only

Users / Orders / Payments      -> RDS PostgreSQL
Heavy relational scale upgrade -> Aurora PostgreSQL/MySQL

Profile images / invoices      -> S3
File metadata                  -> RDS

Shared legacy report folder    -> EFS (only if truly needed)

Cached dashboards / sessions   -> ElastiCache Redis
Key-value ultra-scale pattern  -> DynamoDB when justified
```

## 11.3 Real-Life Flow: User Uploads Invoice

```text
1. User logs into React app
2. User clicks "Upload invoice"
3. React asks backend for upload permission
4. Spring Boot generates pre-signed S3 URL
5. Browser uploads PDF directly to S3
6. Spring Boot stores invoice metadata in RDS:
     user_id, s3_key, upload_time, status
7. Later, user requests invoice list
8. Backend queries RDS for metadata
9. If user downloads one, backend returns pre-signed S3 download URL
```

That is a very common real production pattern.

## 11.4 Real-Life Flow: Dashboard Load

```text
1. User opens analytics dashboard
2. Backend checks Redis
3. If cached -> return fast
4. If not cached -> query RDS, build response, cache it for 5 minutes
5. Future requests hit Redis first
```

## 11.5 Real-Life Flow: EC2 Instance Dies

What survives?

```text
EBS root volume on terminated instance      -> depends on termination settings
Files only on that instance                 -> risky / maybe gone
RDS data                                    -> safe, still there
S3 uploaded files                           -> safe, still there
Redis cache                                 -> may be rebuildable
EFS shared files                            -> still there
```

This is why application data should not live only on one server's local disk.

---

# 12. Backups, Durability, and Recovery

Storage is not just "where data lives." It is also:

- how safely it lives
- how recoverable it is
- how much data loss you can tolerate

## 12.1 S3 Durability

S3 is highly durable and excellent for stored objects.

Use features like:

- versioning
- lifecycle rules
- server-side encryption
- replication only if needed

Real-life example:

```text
Enable versioning on document bucket.
If someone overwrites invoice.pdf accidentally,
you can recover the older version.
```

## 12.2 EBS Backups

EBS uses snapshots.

Good for:

- EC2 disk backups
- AMI creation
- restore scenarios

But remember:

EBS snapshotting a server disk is not the same as designing proper application data storage.

## 12.3 RDS Backups

RDS gives:

- automated backups
- snapshots
- point-in-time recovery
- Multi-AZ for availability

Important distinction:

```text
Backup answers: "Can I recover old data?"
Multi-AZ answers: "Can I survive infrastructure failure quickly?"
```

They solve different problems.

## 12.4 Cache Recovery

For Redis cache, your design should tolerate cache loss.

If cache disappears:

- backend should repopulate from DB
- performance degrades temporarily
- business truth remains in primary data store

---

# 13. Common Mistakes and Debugging Tips

## 13.1 "We stored uploads on the EC2 server"

This breaks when:

- you scale horizontally
- the instance is replaced
- one server has files another does not

Fix:

- move files to S3
- keep metadata in DB

## 13.2 "We put business data in Redis"

This is dangerous unless you truly designed for it.

Fix:

- keep source of truth in RDS/Aurora/DynamoDB
- treat Redis as performance layer

## 13.3 "We used EFS when S3 would do"

Teams often overuse shared filesystems.

Ask:

```text
Do I actually need mounted filesystem semantics?
Or do I just need durable file/object storage?
```

If it is upload/download/object retrieval, S3 is usually better.

## 13.4 "We used DynamoDB without clear access patterns"

This causes pain.

Fix:

- define required queries first
- design partition key around access pattern
- if data is relational and query patterns are broad, use RDS instead

## 13.5 Quick Debug Mental Model

When confused, ask in this order:

```text
1. Is this file storage, block storage, or database data?
2. Is it shared by many machines or tied to one machine?
3. Does it need SQL transactions?
4. Is it the source of truth or just cache?
5. Does it need to survive server replacement?
```

Usually the correct AWS service becomes obvious after that.

---

# 14. Interview-Ready Answers

## 14.1 "Where would you store frontend assets for a React app?"

```text
"For a React SPA, I would build the static files and store them in S3,
then serve them through CloudFront. S3 is durable and cheap for static
assets, and CloudFront gives low-latency global delivery. I would not
default to serving React files from EC2 disks unless there is a strong
reason to keep everything on compute."
```

## 14.2 "Where would you store user uploaded files?"

```text
"I would store the actual files in S3 and keep metadata in a database
like RDS. For uploads, I prefer pre-signed URLs so the browser can upload
directly to S3 without routing large file traffic through the backend."
```

## 14.3 "When would you use EBS vs EFS vs S3?"

```text
"I use EBS when one machine needs a disk, like an EC2 root volume.
I use EFS when multiple machines need a shared filesystem with mounted
file semantics. I use S3 for object storage like assets, documents,
backups, and uploads. For most application files, S3 is the default.
EFS is only when shared filesystem semantics are genuinely required."
```

## 14.4 "When RDS vs DynamoDB?"

```text
"I choose RDS when the data is relational and I need transactions,
joins, and flexible SQL queries. I choose DynamoDB when access patterns
are known up front and the workload fits key-value or document access
at large scale with low-latency reads and writes. For a typical Spring
Boot business app with users, orders, and payments, I would usually start
with RDS."
```

## 14.5 "How do you think about cache?"

```text
"I treat cache as a speed layer, not as the source of truth. I use
ElastiCache Redis for hot reads, sessions, or counters, but the durable
business truth remains in RDS, Aurora, or DynamoDB. If the cache is lost,
the app should slow down temporarily, not lose correctness permanently."
```

---

# 15. Quick Revision Sheet

## One-Line Mapping

```text
S3          = object storage for files, assets, backups, uploads
EBS         = disk attached to one EC2 instance
EFS         = shared filesystem for multiple servers
RDS         = managed relational database
Aurora      = AWS-optimized relational database
DynamoDB    = managed key-value/document database
ElastiCache = in-memory cache, often Redis
```

## What Goes Where for a Normal App

```text
React static build              -> S3 + CloudFront
Spring Boot server disk         -> EBS (if using EC2)
Users / orders / payments       -> RDS
Uploaded files                  -> S3
File metadata                   -> RDS
Shared mounted filesystem       -> EFS only if needed
Hot reads / sessions / counters -> ElastiCache
High-scale key-value pattern    -> DynamoDB when justified
```

## Gold Standard Sentence

```text
"For a typical React plus Spring Boot application, I store frontend static
assets in S3, business data in RDS, uploaded files in S3 with metadata in
the database, and hot read data in ElastiCache. I use EBS only for the
server's disk and EFS only when multiple servers genuinely need a shared
filesystem. I choose DynamoDB only when the workload is naturally key-value
or document-driven and the access patterns are well understood."
```