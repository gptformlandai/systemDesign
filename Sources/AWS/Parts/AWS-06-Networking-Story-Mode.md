# AWS Networking Through Story Mode: How Your App Actually Talks

> You have a Spring Boot backend, a React frontend, and a PostgreSQL database running on your laptop. Everything just works. Then you move to AWS and suddenly nothing can talk to anything. This guide explains AWS networking the way it actually matters — how your components find each other, how users reach your app, and what every piece of the network does in plain language.

---

# Table of Contents

1. [How It Works on Your Laptop (The Baseline)](#1-how-it-works-on-your-laptop-the-baseline)
2. [What Changes When You Move to AWS](#2-what-changes-when-you-move-to-aws)
3. [VPC — Your Private Building in the Cloud](#3-vpc--your-private-building-in-the-cloud)
4. [Subnets — Rooms Inside Your Building](#4-subnets--rooms-inside-your-building)
5. [How Your Components Talk to Each Other](#5-how-your-components-talk-to-each-other)
6. [How Users Reach Your App From the Internet](#6-how-users-reach-your-app-from-the-internet)
7. [How Your Backend Reaches the Outside World](#7-how-your-backend-reaches-the-outside-world)
8. [DNS — How Names Become Addresses](#8-dns--how-names-become-addresses)
9. [Security — Who Can Talk to Whom](#9-security--who-can-talk-to-whom)
10. [Putting It All Together — Full Traffic Flow](#10-putting-it-all-together--full-traffic-flow)
11. [Real-Life Analogy: The Complete Picture](#11-real-life-analogy-the-complete-picture)
12. [Common Mistakes and Debugging Tips](#12-common-mistakes-and-debugging-tips)
13. [Interview-Ready Answers](#13-interview-ready-answers)
14. [Quick Revision Sheet](#14-quick-revision-sheet)

---

# 1. How It Works on Your Laptop (The Baseline)

Before AWS, your app works like this:

```text
React frontend  (localhost:3000)
      |
      | HTTP calls to localhost:8080/api/*
      ↓
Spring Boot backend  (localhost:8080)
      |
      | JDBC connection to localhost:5432
      ↓
PostgreSQL  (localhost:5432)
```

Everything is on one machine. Everything uses `localhost`. No network complexity.

Why does this "just work"?

- all processes share the same machine
- `localhost` means "this computer"
- port numbers separate the processes (3000, 8080, 5432)
- no firewalls between them
- no DNS needed
- no routing needed

That is the baseline. Now let's see what changes.

---

# 2. What Changes When You Move to AWS

On AWS, your components run on **separate machines** inside a **private network**.

```text
React        → runs on S3 + CloudFront (or a container)
Spring Boot  → runs on EC2 / ECS / EKS (a different machine)
PostgreSQL   → runs on RDS (yet another machine)
```

Now `localhost` is gone. Each component has its own IP address. They are no longer neighbors. They need:

- a network to live in (VPC)
- addresses to find each other (private IPs, DNS names)
- doors to allow traffic through (security groups)
- a way for users from the internet to reach the frontend and API
- a way for the backend to call external APIs if needed

Let's build this up, one piece at a time.

---

# 3. VPC — Your Private Building in the Cloud

## 3.1 Real-Life Analogy

Think of a VPC as a **private office building**.

- the building has walls — nothing outside can see in
- inside, there are floors and rooms
- you control who enters, who leaves, and which rooms connect to which
- you decide the address system for the rooms (IP ranges)

On AWS, a VPC is your **isolated network**. When you create one, you say:

"I want a private network with this range of IP addresses."

## 3.2 CIDR — The Address System

When you create a VPC, you choose a CIDR block like `10.0.0.0/16`.

Real-life version:

```text
Think of it like apartment numbering.

10.0.0.0/16 means:
  "I own all addresses from 10.0.0.0 to 10.0.255.255"
  That is 65,536 addresses.

Why does this matter?
  Every EC2 instance, every ECS task, every RDS instance, every ALB
  gets an IP address from this range.

If you run out of IPs, you cannot launch more resources.
```

Common choices:

```text
10.0.0.0/16     → huge, 65K addresses, good for production
10.0.0.0/20     → moderate, 4K addresses, good for smaller environments
172.16.0.0/16   → also common
```

You do not need to memorize CIDR math. Just know:

- smaller number after `/` = more addresses
- plan ahead so you do not run out
- different VPCs should not overlap if you ever want to connect them

## 3.3 One VPC per Environment is Normal

Typical pattern:

```text
VPC: myapp-dev     (10.1.0.0/16)
VPC: myapp-staging (10.2.0.0/16)
VPC: myapp-prod    (10.3.0.0/16)
```

Each environment is fully isolated at the network level.

---

# 4. Subnets — Rooms Inside Your Building

## 4.1 What Are Subnets

A subnet is a **slice** of your VPC's IP range, placed in a **specific Availability Zone**.

Real-life version:

```text
VPC = the building
Subnet = a room on a specific floor

Each room (subnet) has:
  - a portion of the building's addresses
  - a location (Availability Zone)
  - rules about who can enter and leave (route tables)
```

## 4.2 Public vs Private Subnets

This is the single most important networking concept.

### Public Subnet

A subnet is public if its route table has a route to the **Internet Gateway**.

Real-life: a room with a door to the street. Anyone can walk in if the door is open.

Used for:

- load balancers (ALB/NLB)
- NAT Gateways
- bastions if unavoidable

### Private Subnet

A subnet is private if its route table has **no route to the Internet Gateway**.

Real-life: an internal room with no street-facing door. The only way in is through another room.

Used for:

- your Spring Boot backend (EC2, ECS tasks, EKS pods)
- your database (RDS, ElastiCache)
- anything that should not be directly reachable from the internet

### Why This Matters for Your App

```text
Your React frontend              → served from S3/CloudFront (not in VPC at all)
Your Spring Boot backend          → private subnet (no internet exposure)
Your PostgreSQL database (RDS)    → private subnet (definitely no internet exposure)
Your ALB (load balancer)          → public subnet (must receive internet traffic)
```

The ALB sits in the public subnet like a **reception desk**. It faces the outside world. Users talk to the ALB. The ALB then forwards requests to your backend in the private subnet.

Your backend never directly faces the internet.

## 4.3 Multi-AZ Layout

For resilience, you spread subnets across multiple Availability Zones.

```text
                       VPC: 10.0.0.0/16
     ┌──────────────────────┬──────────────────────┐
     │     AZ: us-east-1a   │     AZ: us-east-1b   │
     │                      │                       │
     │  Public Subnet       │  Public Subnet        │
     │  10.0.1.0/24         │  10.0.2.0/24          │
     │  (ALB, NAT GW)       │  (ALB, NAT GW)        │
     │                      │                       │
     │  Private Subnet      │  Private Subnet       │
     │  10.0.10.0/24        │  10.0.20.0/24         │
     │  (Spring Boot)       │  (Spring Boot)        │
     │                      │                       │
     │  Private Subnet      │  Private Subnet       │
     │  10.0.100.0/24       │  10.0.200.0/24        │
     │  (RDS, ElastiCache)  │  (RDS, ElastiCache)   │
     └──────────────────────┴──────────────────────┘
```

Why two AZs?

- if one data center has a problem, the other keeps running
- ALB automatically routes to the healthy AZ
- RDS can failover to the standby in the other AZ

This is the minimum production layout.

---

# 5. How Your Components Talk to Each Other

This is the question: "On my laptop, Spring Boot calls `localhost:5432` for the database. What happens on AWS?"

## 5.1 Backend → Database

On AWS, your Spring Boot backend calls the database using a **DNS name**, not `localhost`.

```text
Local:
  spring.datasource.url=jdbc:postgresql://localhost:5432/myapp

AWS:
  spring.datasource.url=jdbc:postgresql://myapp-db.abc123.us-east-1.rds.amazonaws.com:5432/myapp
```

That long hostname is the **RDS endpoint**. AWS creates it automatically when you create the database.

How does it work underneath?

```text
1. Spring Boot says: "I need to connect to myapp-db.abc123.us-east-1.rds.amazonaws.com"
2. DNS resolves that name to a private IP like 10.0.100.42
3. The network routes the request within the VPC
4. Security group on RDS checks: "Is this caller allowed on port 5432?"
5. If yes, connection is established
```

All of this happens inside the VPC. The database is never exposed to the internet.

## 5.2 Backend → Cache (Redis/ElastiCache)

Same pattern:

```text
Local:
  spring.redis.host=localhost

AWS:
  spring.redis.host=myapp-cache.abc123.cache.amazonaws.com
```

ElastiCache creates a DNS endpoint. Your backend resolves it to a private IP inside the VPC. Traffic never leaves the private network.

## 5.3 Frontend → Backend

This depends on how you host the frontend.

### If React is on S3 + CloudFront (most common pattern)

The React app runs in **the user's browser**, not on AWS compute.

```text
1. User opens https://app.yourcompany.com
2. CloudFront serves the React HTML/JS/CSS from S3
3. React app loads in the user's browser
4. React makes API calls to https://api.yourcompany.com/api/orders
5. That request goes over the internet to your ALB
6. ALB forwards it to your Spring Boot backend in the private subnet
7. Backend responds. ALB sends response back to the browser.
```

Important realization:

**The frontend does not talk to the backend inside AWS.**
**The user's browser talks to the backend through the internet and ALB.**

```text
  User's Browser (React running here)
        |
        | HTTPS request to api.yourcompany.com
        ↓
  Internet
        |
        ↓
  ALB (public subnet)
        |
        | HTTP forward to backend
        ↓
  Spring Boot (private subnet)
        |
        ↓
  RDS (private subnet)
```

### If React is in a container (ECS/EKS alongside backend)

Then the frontend container serves static files. Users still hit it through the ALB. Internally, the frontend container does not call the backend — the browser does.

The key insight: **React is client-side. The API call always originates from the user's browser, not from the frontend server.**

## 5.4 Backend → Backend (Microservices)

If you have multiple backend services:

```text
Service A needs to call Service B
```

Options on AWS:

| Method | How it works | When to use |
|---|---|---|
| Through ALB (internal) | Internal ALB routes to Service B | Simple, works on EC2/ECS/EKS |
| AWS Cloud Map | ECS registers services with DNS names like `service-b.myapp.local` | ECS service-to-service |
| Kubernetes DNS | Service B is reachable at `service-b.myapp.svc.cluster.local` | EKS |
| Direct IP/hostname | Hardcoded or config-driven | Small scale, not recommended |

For your first app with one backend service, this does not apply yet. But it matters when you grow.

## 5.5 Backend → External APIs

Your Spring Boot might call third-party APIs (payment gateway, email provider, etc.).

```text
Spring Boot (private subnet)
      |
      | "I need to call https://api.stripe.com"
      |
      | But I am in a private subnet — I have no internet route!
      ↓
  NAT Gateway (public subnet)
      |
      | NAT translates private IP to public IP
      ↓
  Internet → api.stripe.com
      |
      | Response comes back through NAT
      ↓
  Spring Boot receives the response
```

This is covered in detail in the next section.

---

# 6. How Users Reach Your App From the Internet

This is north-south traffic — from outside AWS into your app.

## 6.1 The Full Chain

```text
User types: https://app.yourcompany.com

Step 1: DNS Resolution
   Browser asks: "What is the IP of app.yourcompany.com?"
   Route 53 answers: "It's a CloudFront distribution at d123xxx.cloudfront.net"

Step 2: CloudFront
   The request hits a CloudFront edge location near the user.
   CloudFront serves the React static files (HTML, JS, CSS) from S3.

Step 3: Browser loads React
   React app is now running in the user's browser.

Step 4: React makes API call
   Browser sends: GET https://api.yourcompany.com/api/orders
   DNS resolves api.yourcompany.com to the ALB's IP address.

Step 5: ALB receives the request
   The ALB lives in a public subnet.
   It has an Internet Gateway route, so internet traffic can reach it.
   ALB terminates TLS (HTTPS → HTTP internally).

Step 6: ALB forwards to backend
   ALB knows which targets are healthy (via health checks).
   It forwards to a Spring Boot instance/task/pod in a private subnet.

Step 7: Backend processes
   Spring Boot queries RDS, maybe Redis.
   Builds response.

Step 8: Response travels back
   Backend → ALB → Internet → User's browser.
```

## 6.2 Internet Gateway — The Front Door

The Internet Gateway (IGW) is attached to your VPC. It allows resources in **public subnets** to communicate with the internet.

Real-life analogy:

```text
IGW = the main entrance of your building.

If your room (subnet) has a hallway to the main entrance  → public subnet
If your room has NO hallway to the main entrance          → private subnet
```

Important:

- the IGW itself does not cost money
- it does not limit bandwidth significantly
- you need exactly one per VPC
- it is the reason your ALB can receive internet traffic

## 6.3 ALB — The Smart Receptionist

The Application Load Balancer sits in public subnets and is the entry point for HTTP/HTTPS traffic.

What it does:

```text
1. Receives requests from the internet
2. Terminates TLS (handles HTTPS certificates)
3. Reads the HTTP request (path, headers, host)
4. Routes to the right backend based on rules:
     /api/*     → Spring Boot service
     /admin/*   → Admin service
     /health    → Health check service
5. Only sends traffic to healthy targets
6. Distributes load across multiple instances/tasks/pods
```

Real-life analogy:

```text
ALB = a reception desk in the lobby

"You want to see Engineering? Floor 3, room 302."
"You want Finance? Floor 5, room 510."
"Room 302 is being renovated? I'll send you to room 303 instead."
```

## 6.4 Why the Backend Is NOT in a Public Subnet

You might think: "Why not just put Spring Boot in a public subnet and skip the ALB?"

Reasons:

```text
1. Direct exposure → any attacker can probe your app directly
2. No load balancing → one instance = single point of failure
3. No health checks → broken instance still receives traffic
4. No TLS termination → each instance manages certificates
5. No routing rules → cannot split traffic by path
6. No connection draining during deploys

The ALB is not optional overhead. It is a critical safety and operations layer.
```

---

# 7. How Your Backend Reaches the Outside World

Your backend sits in a private subnet. It cannot reach the internet by default. But sometimes it needs to:

- call external APIs (Stripe, Twilio, email providers)
- download packages during setup
- pull container images
- reach AWS services that are outside the VPC

## 7.1 NAT Gateway — The Outbound-Only Door

NAT = Network Address Translation.

Real-life analogy:

```text
Imagine your office building has a security rule:
  "People outside cannot walk in uninvited."
  "But people inside CAN walk out, do their errands, and come back."

The NAT Gateway is that one-way exit door.

  Outbound: allowed (your backend can call Stripe)
  Inbound:  blocked (Stripe cannot randomly connect to your backend)
```

How it works:

```text
Spring Boot (private subnet, IP: 10.0.10.15)
      |
      | "I need to call https://api.stripe.com"
      ↓
NAT Gateway (public subnet, has a public IP: 54.23.xx.xx)
      |
      | Translates: 10.0.10.15 → 54.23.xx.xx
      | Sends request to Stripe
      ↓
Internet → api.stripe.com
      |
      | Stripe responds to 54.23.xx.xx
      ↓
NAT Gateway
      |
      | Translates back: 54.23.xx.xx → 10.0.10.15
      ↓
Spring Boot receives the response
```

The outside world only sees `54.23.xx.xx` (the NAT Gateway's IP). It never sees `10.0.10.15`. Your backend stays hidden.

### NAT Gateway Costs

This is important because NAT Gateway is a common surprise cost:

```text
NAT Gateway:
  $0.045/hour     = ~$32/month just to exist
  $0.045/GB       = extra for every GB of data

If your backend sends 100 GB/month through NAT:
  $32 + ($0.045 × 100) = $36.50/month  PER NAT GATEWAY

You usually want one per AZ for resilience, so double it.
```

## 7.2 VPC Endpoints — The Shortcut That Skips NAT

When your backend needs to call **AWS services** (S3, Secrets Manager, DynamoDB, SQS), it does not need to go through the NAT Gateway and out to the internet.

VPC Endpoints create a **private tunnel** directly to the AWS service.

Real-life analogy:

```text
Without VPC Endpoint:
  You (private room) → exit building through NAT → walk across the city → enter AWS S3 building

With VPC Endpoint:
  You (private room) → walk through an internal corridor → arrive at S3 (never left the building)
```

```text
Without endpoint:
  Backend → NAT Gateway → Internet → S3 public endpoint
  (costs NAT data processing fees, slower, exposed to internet)

With endpoint:
  Backend → VPC Endpoint → S3
  (free for gateway endpoints, private, faster)
```

Two types:

```text
Gateway Endpoints:
  Available for: S3, DynamoDB
  Cost: FREE
  How: add an entry to the route table

Interface Endpoints (powered by PrivateLink):
  Available for: Secrets Manager, SQS, ECR, CloudWatch, and many more
  Cost: ~$7/month per endpoint + data charges
  How: creates a private IP (ENI) in your subnet
```

### When to Use VPC Endpoints

```text
Always use Gateway Endpoints for S3 and DynamoDB.
  → They are free. No reason not to.

Use Interface Endpoints when:
  → your backend calls Secrets Manager, SQS, ECR frequently
  → you want to avoid NAT Gateway data charges for those calls
  → you want to keep traffic fully private

Skip Interface Endpoints when:
  → the service is called rarely
  → the $7/month per endpoint exceeds the NAT savings
```

## 7.3 Summary: Backend Outbound Traffic Decision

```text
Backend needs to reach:

  AWS service (S3, DynamoDB)?        → Gateway VPC Endpoint (free)
  AWS service (Secrets Manager, SQS) → Interface VPC Endpoint (if frequent)
  External API (Stripe, Twilio)?     → NAT Gateway (only option)
  Nothing external?                  → No NAT Gateway needed, save money
```

---

# 8. DNS — How Names Become Addresses

On your laptop, you type `localhost`. On AWS, you type `myapp-db.abc123.us-east-1.rds.amazonaws.com`. DNS is the system that turns names into IP addresses.

## 8.1 Real-Life Analogy

```text
DNS is like your phone's contact list.

You do not memorize phone numbers.
You tap "Mom" and the phone knows to dial +1-555-123-4567.

DNS works the same way:
  You type "api.yourcompany.com"
  DNS translates it to "52.23.178.42" (the ALB's IP)
  Your browser connects to that IP
```

## 8.2 Route 53 — AWS DNS Service

Route 53 is where you manage DNS records for your domain.

Typical records for your app:

```text
app.yourcompany.com    → CloudFront distribution   (React frontend)
api.yourcompany.com    → ALB                       (Spring Boot backend)
```

How you set this up:

```text
1. Buy or transfer your domain to Route 53 (or keep it elsewhere and point nameservers)
2. Create a Hosted Zone for yourcompany.com
3. Add records:

   Type: A (Alias)
   Name: app.yourcompany.com
   Target: CloudFront distribution ID
   
   Type: A (Alias)
   Name: api.yourcompany.com
   Target: ALB DNS name
```

Now when a user types `app.yourcompany.com`, DNS knows to send them to CloudFront. When React calls `api.yourcompany.com`, DNS knows to send traffic to the ALB.

## 8.3 Private DNS Inside the VPC

Inside your VPC, AWS provides **private DNS** automatically.

When you create an RDS instance, AWS gives it a DNS name like:

```text
myapp-db.abc123.us-east-1.rds.amazonaws.com
```

This name resolves to a **private IP** (like `10.0.100.42`) that only works inside the VPC.

Your backend uses this hostname in its config. It never needs to know the raw IP.

Why hostnames instead of IPs?

```text
If RDS fails over to another AZ:
  Old IP: 10.0.100.42  (AZ-a, now dead)
  New IP: 10.0.200.18  (AZ-b, now active)

If you used the IP directly, your app breaks.
If you used the hostname, DNS updates automatically to point to the new IP.
Your app reconnects seamlessly.
```

This is why you always use DNS names, not raw IPs, for databases and services.

## 8.4 How DNS Resolution Flows for Your App

```text
User's browser → "What is api.yourcompany.com?"
      |
      ↓
Route 53 → "It's an alias for myapp-alb-123.us-east-1.elb.amazonaws.com"
      |
      ↓
DNS resolves ALB name → "52.23.178.42"  (public IP)
      |
      ↓
Browser connects to 52.23.178.42 (the ALB)


Spring Boot → "What is myapp-db.abc123.us-east-1.rds.amazonaws.com?"
      |
      ↓
VPC internal DNS → "10.0.100.42"  (private IP, only works inside VPC)
      |
      ↓
Spring Boot connects to 10.0.100.42 (the RDS instance)
```

Public DNS for external traffic. Private DNS for internal traffic. Same mechanism, different worlds.

---

# 9. Security — Who Can Talk to Whom

Networking is not just about connectivity. It is about **controlled** connectivity.

## 9.1 Security Groups — The Bouncers at Each Door

A Security Group is a **firewall attached to a specific resource**.

Real-life analogy:

```text
Security Group = a bouncer at the door of each room

The bouncer has a list:
  "Allow anyone from the lobby (ALB) on port 8080"
  "Block everyone else"

Every EC2 instance, ECS task, RDS instance, and ALB has its own bouncer.
```

### Security Groups for Your App

```text
ALB Security Group:
  Inbound:
    - Allow TCP 443 (HTTPS) from 0.0.0.0/0  (anyone on the internet)
  Outbound:
    - Allow TCP 8080 to Backend Security Group

Backend Security Group (EC2/ECS/EKS):
  Inbound:
    - Allow TCP 8080 from ALB Security Group  (only the ALB can reach the backend)
  Outbound:
    - Allow TCP 5432 to Database Security Group
    - Allow TCP 6379 to Redis Security Group
    - Allow TCP 443 to 0.0.0.0/0             (for calling external APIs via NAT)

Database Security Group (RDS):
  Inbound:
    - Allow TCP 5432 from Backend Security Group  (only backend can reach the DB)
  Outbound:
    - None needed (DB does not initiate outbound connections)

Redis Security Group (ElastiCache):
  Inbound:
    - Allow TCP 6379 from Backend Security Group
  Outbound:
    - None needed
```

Notice the pattern:

```text
Internet  → ALB (only port 443)
ALB       → Backend (only port 8080)
Backend   → Database (only port 5432)
Backend   → Redis (only port 6379)

Nobody skips a layer. The database is unreachable from the internet.
```

### Why Security Groups Reference Other Security Groups

Instead of writing:

```text
"Allow port 5432 from IP 10.0.10.15"
```

You write:

```text
"Allow port 5432 from sg-backend-security-group"
```

Why?

- IPs change when instances scale or replace
- security group references work regardless of how many instances exist or what IPs they have
- this is the correct pattern in production

## 9.2 Network ACLs — The Building-Level Rules

NACLs are subnet-level rules. They apply to all traffic entering or leaving a subnet.

Real-life analogy:

```text
Security Group = bouncer at each room door
NACL           = security gate at the entrance of each floor

NACLs are coarser. Most teams rely on Security Groups for fine control
and only use NACLs for rare subnet-level deny rules.
```

For your app, Security Groups do 95% of the work. NACLs are there if you need to block a specific IP range at the subnet level.

## 9.3 TLS / HTTPS — Encryption in Transit

```text
User → ALB:      HTTPS (encrypted with TLS certificate)
ALB → Backend:   HTTP usually (within private network, TLS optional)
Backend → RDS:   TLS recommended (can be enforced in RDS settings)
```

The TLS certificate for your domain lives in **AWS Certificate Manager (ACM)**. It is free. ALB uses it automatically.

---

# 10. Putting It All Together — Full Traffic Flow

Here is every networking component working together for one API request:

```text
User types: https://app.yourcompany.com
      │
      ▼
[Route 53]  resolves app.yourcompany.com → CloudFront
      │
      ▼
[CloudFront]  serves React static files from S3
      │
      ▼
React app loads in browser. User clicks "View Orders."
      │
      ▼
Browser sends: GET https://api.yourcompany.com/api/orders
      │
      ▼
[Route 53]  resolves api.yourcompany.com → ALB public IP
      │
      ▼
[Internet Gateway]  allows the request into the VPC
      │
      ▼
[ALB]  (public subnet, SG allows port 443 from internet)
  │  terminates TLS
  │  checks path: /api/orders → forward to backend target group
  │  picks a healthy backend instance
      │
      ▼
[Spring Boot]  (private subnet, SG allows port 8080 from ALB only)
  │  receives request
  │  needs to query database
      │
      ▼
[RDS PostgreSQL]  (private subnet, SG allows port 5432 from backend only)
  │  executes query
  │  returns result
      │
      ▼
[Spring Boot]  builds JSON response
      │
      ▼
[ALB]  sends response back through Internet Gateway
      │
      ▼
[Browser]  receives JSON, React renders the orders page
```

And if the backend needs to call Stripe during order processing:

```text
[Spring Boot]  (private subnet)
      │
      │  "I need to call https://api.stripe.com/v1/charges"
      ▼
[NAT Gateway]  (public subnet)
      │
      │  translates private IP → public IP
      ▼
[Internet]  → api.stripe.com
      │
      │  response comes back
      ▼
[NAT Gateway]  translates back
      │
      ▼
[Spring Boot]  receives Stripe response
```

---

# 11. Real-Life Analogy: The Complete Picture

Here is the entire AWS networking model as an office building:

```text
┌─────────────────────────────────────────────────────────────────┐
│                    YOUR OFFICE BUILDING (VPC)                    │
│                                                                 │
│  MAIN ENTRANCE (Internet Gateway)                               │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  LOBBY — Public Subnet                                  │    │
│  │                                                         │    │
│  │  Reception Desk (ALB)                                   │    │
│  │    "Hello, who are you here to see?"                    │    │
│  │    "Engineering API? Let me check if they're available" │    │
│  │    "Yes, they're healthy. Follow me."                   │    │
│  │                                                         │    │
│  │  Side Exit (NAT Gateway)                                │    │
│  │    "Staff can go out for errands."                      │    │
│  │    "Outsiders cannot enter through this door."          │    │
│  └─────────────────┬───────────────────────────────────────┘    │
│                    │                                            │
│                    ▼                                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  OFFICE FLOOR — Private Subnet (App Tier)               │    │
│  │                                                         │    │
│  │  Room 301: Spring Boot Instance A                       │    │
│  │  Room 302: Spring Boot Instance B                       │    │
│  │                                                         │    │
│  │  Door bouncer (Security Group):                         │    │
│  │    "Only people from the Reception Desk allowed in"     │    │
│  │    "Only staff here can visit the Archive Floor"        │    │
│  └─────────────────┬───────────────────────────────────────┘    │
│                    │                                            │
│                    ▼                                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  ARCHIVE FLOOR — Private Subnet (Data Tier)             │    │
│  │                                                         │    │
│  │  Vault A: PostgreSQL (RDS)                              │    │
│  │  Vault B: Redis (ElastiCache)                           │    │
│  │                                                         │    │
│  │  Door bouncer (Security Group):                         │    │
│  │    "Only people from the Office Floor allowed in"       │    │
│  │    "Absolutely nobody from outside"                     │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  INTERNAL CORRIDOR (VPC Endpoint to S3):                        │
│    "Need files from the S3 warehouse next door?                 │
│     Use this private corridor. No need to go outside."          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

SEPARATE BUILDING (S3 + CloudFront):
  "The marketing materials (React frontend) are stored here.
   Anyone can pick up a copy from any CloudFront branch office
   around the world."

PHONE DIRECTORY (Route 53 / DNS):
  "Looking for the Engineering building? Here's the address."
  "Looking for the database? Here's the INTERNAL extension number."
```

---

# 12. Common Mistakes and Debugging Tips

## 12.1 "My backend cannot connect to RDS"

Check in this order:

```text
1. Is the RDS in the same VPC as the backend?
2. Is the backend's security group allowed outbound to port 5432?
3. Is the RDS security group allowing inbound from the backend's security group?
4. Is the backend using the correct RDS endpoint hostname?
5. Are the database credentials correct?
6. Are they in the same (or peered) subnets?
```

90% of the time it is a security group misconfiguration.

## 12.2 "My backend cannot call external APIs"

```text
1. Is the backend in a private subnet? (It should be)
2. Is there a NAT Gateway in a public subnet?
3. Does the private subnet's route table have:
     0.0.0.0/0 → NAT Gateway?
4. Does the backend's security group allow outbound on port 443?
```

If any of these is missing, outbound traffic will silently fail.

## 12.3 "Users cannot reach my API"

```text
1. Does Route 53 have an A record pointing to the ALB?
2. Is the ALB in a public subnet with an Internet Gateway route?
3. Does the ALB security group allow inbound port 443 from 0.0.0.0/0?
4. Does the ALB have a valid TLS certificate from ACM?
5. Is the target group healthy? (Check ALB target group health in console)
6. Does the backend security group allow inbound from the ALB security group?
```

## 12.4 "My ECS task cannot pull images from ECR"

```text
The task is in a private subnet and has no way to reach ECR.

Fix options:
  a) Add a NAT Gateway (costs money)
  b) Add a VPC Interface Endpoint for ECR (com.amazonaws.region.ecr.dkr 
     and com.amazonaws.region.ecr.api) + S3 Gateway Endpoint (for image layers)
```

This catches many teams on their first ECS deployment.

## 12.5 Quick Debug Mental Model

```text
If something cannot connect to something else:

  Step 1: Are they in the same VPC? (or peered/PrivateLinked?)
  Step 2: Can the network route get from A to B? (route tables)
  Step 3: Do security groups allow the traffic? (inbound on target, outbound on source)
  Step 4: Is DNS resolving correctly? (nslookup / dig)
  Step 5: Is the application actually listening on the expected port?
```

---

# 13. Interview-Ready Answers

## 13.1 "How does a user's request reach your Spring Boot backend on AWS?"

```text
"The user's browser resolves the API domain via Route 53, which points to 
an ALB in a public subnet. The ALB terminates TLS, evaluates routing rules, 
and forwards the request to a healthy Spring Boot instance in a private 
subnet. The backend processes the request, queries RDS in another private 
subnet, and sends the response back through the ALB."
```

## 13.2 "Why private subnets for the backend?"

```text
"Private subnets have no route to the Internet Gateway, so they cannot be 
directly reached from the internet. This reduces attack surface. The ALB 
in the public subnet acts as the controlled entry point. Backend instances 
only accept traffic from the ALB's security group."
```

## 13.3 "How does your backend in a private subnet call external APIs?"

```text
"Through a NAT Gateway in a public subnet. The private subnet's route table 
sends 0.0.0.0/0 traffic to the NAT Gateway, which translates the private IP 
to a public IP for outbound requests. Inbound internet traffic is still 
blocked. For AWS service calls like S3 or Secrets Manager, I use VPC 
endpoints to keep traffic private and avoid NAT costs."
```

## 13.4 "Explain security groups in your architecture"

```text
"Each layer has its own security group. The ALB allows inbound HTTPS from 
the internet. The backend allows inbound on port 8080 only from the ALB's 
security group. The database allows inbound on port 5432 only from the 
backend's security group. This creates a chain where each layer only 
accepts traffic from the layer directly above it. No layer is directly 
exposed beyond its intended purpose."
```

## 13.5 "What is the difference between Internet Gateway and NAT Gateway?"

```text
"Internet Gateway allows two-way internet traffic for public subnets — 
both inbound and outbound. It is used by ALBs and anything that needs 
to be publicly reachable.

NAT Gateway allows one-way outbound internet access for private subnets. 
Resources behind it can reach the internet, but the internet cannot 
initiate connections to them. I use it when the backend needs to call 
external APIs like payment gateways."
```

---

# 14. Quick Revision Sheet

## Every Component in One Line

```text
VPC            = your private network on AWS
Subnet         = a slice of the VPC in one AZ, public or private
Internet GW    = the front door for public internet access (two-way)
NAT Gateway    = the back door for private subnet outbound-only internet
Route Table    = the map that says "traffic to X goes through Y"
Security Group = per-resource firewall (bouncer at each door)
NACL           = per-subnet firewall (security gate per floor)
ALB            = HTTPS load balancer and smart router (receptionist)
Route 53       = DNS service (phone directory)
CloudFront     = CDN (branch offices serving copies worldwide)
VPC Endpoint   = private shortcut to AWS services, skips internet
ACM            = free TLS certificates for your domains
```

## The Standard 3-Tier Network Layout

```text
Public Subnet:   ALB, NAT Gateway
Private Subnet:  Backend compute (EC2, ECS, EKS)
Private Subnet:  Database (RDS, ElastiCache)
```

## Traffic Flow Cheat Sheet

```text
User → App:        Route 53 → CloudFront → S3
User → API:        Route 53 → ALB (public) → Backend (private)
Backend → DB:      Private subnet → Private subnet (via security group)
Backend → Redis:   Private subnet → Private subnet (via security group)
Backend → S3:      VPC Gateway Endpoint (free, private)
Backend → Stripe:  NAT Gateway → Internet
Backend → Secrets: VPC Interface Endpoint (private) or NAT Gateway
```

## Gold Standard Sentence

```text
"On AWS, I place the ALB in public subnets as the only internet-facing 
entry point. The backend runs in private subnets, reachable only through the 
ALB. The database sits in private subnets, reachable only from the backend. 
Outbound internet access uses a NAT Gateway, and AWS service calls use VPC 
endpoints to stay private and save costs. Security groups enforce least-privilege 
at every layer, and Route 53 handles DNS for both external users and internal 
service discovery."
```
