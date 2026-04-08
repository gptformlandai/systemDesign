# Nexturn — Round 2 Interview Prep (Wednesday, April 2, 2026)

---

## Interview Board Analysis

---

### Interviewer 1: Sushree Mohapatra — Cloud Architect, Nexturn

```
LinkedIn: https://www.linkedin.com/in/sushree-mohapatra-8ab1aa82/
Location: Odisha, India
Education: College of Engineering & Technology, Bhubaneswar
Role: Cloud Architect at NexTurn
About: "Technology Architect Java and Springboot, K8, devops"

CERTIFICATIONS:
  ✅ AWS Certified Solutions Architect – Associate (Nov 2020)
  ✅ AWS Academy Graduate – Cloud Architecting (Oct 2020)
  ✅ MuleSoft Certified Developer (Aug 2023)

ANALYSIS:
  - She IS the primary technical interviewer.
  - She builds the SAME stack as you: Java + Spring Boot + K8s + DevOps + AWS.
  - AWS SA Associate = she knows cloud architecture deeply.
  - MuleSoft cert = she understands integration/API patterns.
  - "Architect" title = she cares about WHY you chose something, not just HOW.
  - Expect: system design, architectural trade-offs, deep Java/Spring Boot,
    K8s operations, AWS services, CI/CD, and integration patterns.

SHE WILL GO DEEP. Expect implementation + architecture depth.
```

---

### Interviewer 2: Surendra Kumar — Senior Cloud Engineer-BI, Nexturn | PL-300

```
LinkedIn: https://www.linkedin.com/in/surendra-kumar-936320192/
Location: West Godavari, Andhra Pradesh, India
Education: Rajiv Gandhi University of Knowledge Technologies, RKValley (RAA)
Role: Senior Cloud Engineer - BI at NexTurn

ABOUT SECTION:
  "To play a challenging role and to become an integral part of a growth-oriented
  organization by devising optimal data driven solutions for complex problems to
  take informed decisions through leveraging the potential of the technology,
  commitment to honing skills and adapting latest technologies."

CERTIFICATIONS:
  ✅ Databricks Certified Data Engineer Associate (Dec 2025, expires Dec 2027)
  ✅ Databricks Fundamentals Accreditation (Jul 2025)
  ✅ Certified Data Science Professional — OdinSchool (Oct 2024)
  ✅ Microsoft Certified: Power BI Data Analyst Associate — PL-300 (Jun 2024)
  ✅ FinOps Introduction to FOCUS (Oct 2024)
  ✅ AWS QuickSight (Udemy, Jul 2024) — 2 certs
  ✅ Kaggle Pandas (Jul 2024)
  ✅ HackerRank SQL Intermediate (Jul 2024)
  ✅ HackerRank SQL Basic (Jul 2024)

SKILLS FROM CERTS:
  - Azure Databricks, Data Engineering
  - Machine Learning, Python
  - Extract Transform Load (ETL), Business Intelligence Tools
  - Data Analysis, Amazon QuickSight
  - FinOps, Financial Operations
  - Pandas, SQL

ANALYSIS:
  - He is the DATA / BI focused interviewer.
  - Won't grill you on Java internals — that's Sushree's domain.
  - He WILL test SQL skills (he has intermediate SQL cert from HackerRank).
  - He cares about: data pipelines, ETL, BI tools, analytics, data modeling.
  - He'll assess: how well your APIs and services SERVE data consumers.
  - Databricks + Power BI + Python + QuickSight = heavy data stack.
  - FinOps cert = he understands cloud cost optimization.

He tests: Can your code produce clean, queryable, efficient data?
```

---

## What Each Interviewer Will Likely Ask

```
┌─────────────────────────────────────────────────────────────────────────┐
│ SUSHREE (Cloud Architect)               SURENDRA (BI + Data Engineer)  │
│                                                                         │
│ ★ Java + Spring Boot deep dive          ★ SQL: joins, CTEs, window fn  │
│ ★ Microservices architecture            ★ Data modeling + schema design │
│ ★ Kubernetes: architecture to ops       ★ ETL / data pipeline concepts │
│ ★ AWS services: EKS, SQS, S3, VPC      ★ API design for BI consumers  │
│ ★ System design (end-to-end)            ★ Query optimization + indexing│
│ ★ CI/CD pipelines + DevOps             ★ Cloud data services (S3, RDS) │
│ ★ Docker + Helm                         ★ Python basics (if you know)  │
│ ★ Integration patterns (MuleSoft exp)   ★ FinOps / cost awareness      │
│ ★ Security: IAM, RBAC, secrets          ★ Aggregation + reporting APIs │
│ ★ Trade-off reasoning ("why X not Y?")  ★ Batch vs stream processing  │
│                                                                         │
│ Tests: "Can you DESIGN and BUILD it?"   Tests: "Can you feed DATA?"   │
│ Depth: Architecture + implementation    Depth: Data + SQL + analytics │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## HOT TOPICS — Priority Prep List

### 🔴 CRITICAL (will definitely come up — prep first)

```
1. JAVA + SPRING BOOT (Sushree)
   ─────────────────────────────
   ✦ Spring Boot auto-configuration — how does @SpringBootApplication work?
   ✦ Bean lifecycle: @PostConstruct → init → ready → @PreDestroy
   ✦ Dependency injection: constructor vs field vs setter (why constructor wins)
   ✦ Profiles: how you manage dev/staging/prod configs
   ✦ Exception handling: @ControllerAdvice, @ExceptionHandler
   ✦ Spring Security: filter chain, JWT authentication flow
   ✦ Transactions: @Transactional, propagation levels, isolation levels
   ✦ Java 17+ features: records, sealed classes, pattern matching, text blocks
   ✦ Collections: HashMap internals, ConcurrentHashMap, TreeMap vs HashMap
   ✦ Concurrency: ExecutorService, CompletableFuture, virtual threads (Java 21)
   ✦ JVM: garbage collection basics, heap vs stack, memory model
   ✦ Spring Data JPA: repositories, custom queries, pagination
   ✦ Spring Boot Actuator: health, metrics, Prometheus integration
   ✦ @Async, @Scheduled, @Cacheable — know the annotations
   ✦ Lazy loading, N+1 problem in JPA, fetch types

2. KUBERNETES (Sushree listed K8 explicitly in her About)
   ──────────────────────────────────────────────────────
   ✦ Architecture: control plane + worker node components
   ✦ Pod lifecycle, probes (liveness/readiness/startup)
   ✦ Deployments vs StatefulSets — when to use which
   ✦ Services: ClusterIP vs NodePort vs LoadBalancer vs Headless
   ✦ Ingress: path-based routing, TLS termination
   ✦ HPA: how autoscaling works, metrics-based scaling
   ✦ ConfigMaps + Secrets: how to manage config in K8s
   ✦ RBAC: Roles, ClusterRoles, ServiceAccounts
   ✦ Network Policies: Pod-to-Pod isolation
   ✦ Troubleshooting: CrashLoopBackOff, Pending, ImagePullBackOff, OOMKilled
   ✦ Java on K8s: JVM memory sizing, graceful shutdown, startup probes
   ✦ Rolling updates, blue-green, canary in K8s
   ✦ Helm basics: charts, values, upgrade, rollback
   → K8s-Complete-Refresher.md covers ALL of this ✅

3. SQL (Surendra is SQL + BI certified)
   ─────────────────────────────────────
   ✦ Joins: INNER, LEFT, RIGHT, FULL OUTER, CROSS, SELF join
   ✦ Window functions: ROW_NUMBER(), RANK(), DENSE_RANK(), LAG(), LEAD()
   ✦ Window functions: SUM() OVER, AVG() OVER with PARTITION BY and ORDER BY
   ✦ CTEs: WITH clause, recursive CTEs
   ✦ Subqueries: correlated vs non-correlated
   ✦ Indexing: B-tree, composite indexes, covering indexes, when NOT to index
   ✦ Query optimization: EXPLAIN/ANALYZE, full table scans, index selection
   ✦ Aggregations: GROUP BY, HAVING, ROLLUP, CUBE
   ✦ Transactions: ACID properties, isolation levels
       (Read Uncommitted, Read Committed, Repeatable Read, Serializable)
   ✦ Normalization: 1NF, 2NF, 3NF, BCNF — when to denormalize
   ✦ Star schema vs snowflake (Surendra's BI background)
   ✦ N+1 query problem + solutions
   ✦ Pagination: OFFSET vs cursor-based
   ✦ UPSERT / MERGE / ON CONFLICT
   ✦ Stored procedures vs application-level logic
```

### 🟡 HIGH (very likely — prep second)

```
4. AWS SERVICES (Sushree is SA certified)
   ──────────────────────────────────────
   ✦ Compute: EC2 vs ECS vs EKS vs Lambda — when to use which
   ✦ Storage: S3 (classes, lifecycle, pre-signed URLs), EBS, EFS
   ✦ Database: RDS vs DynamoDB vs Aurora — SQL vs NoSQL trade-offs
   ✦ Messaging: SQS vs SNS vs EventBridge — async communication patterns
   ✦ Networking: VPC, subnets (public/private), Security Groups, NACLs
   ✦ Load Balancing: ALB (L7) vs NLB (L4) — when to use which
   ✦ IAM: roles, policies, least privilege principle, assume role
   ✦ CloudWatch: logs, metrics, alarms
   ✦ Route 53: DNS routing policies (simple, weighted, latency, failover)
   ✦ Secrets Manager vs Parameter Store
   ✦ ElastiCache: Redis vs Memcached
   ✦ Well-Architected Framework: 6 pillars (know at least names)

5. MICROSERVICES ARCHITECTURE (both interviewers)
   ──────────────────────────────────────────────
   ✦ Monolith vs microservices — trade-offs, when to migrate
   ✦ Service communication: REST vs gRPC vs async messaging
   ✦ Circuit breaker pattern (Resilience4j in Spring Boot)
   ✦ Saga pattern: choreography vs orchestration
   ✦ API Gateway pattern: routing, rate limiting, auth
   ✦ Service discovery: Eureka (Spring) vs K8s DNS (K8s native)
   ✦ Event-driven architecture: SQS/Kafka/SNS for decoupling
   ✦ Distributed tracing: correlation IDs, OpenTelemetry, Jaeger
   ✦ 12-Factor App principles (config, logs, backing services, etc.)
   ✦ CQRS + Event Sourcing concepts
   ✦ Idempotency: why it matters, how to implement
   ✦ Distributed transactions: 2PC vs Saga (your SD notes cover this ✅)
   → SD-Topics-Batch-1.md covers Saga + 2PC patterns ✅

6. CI/CD + DEVOPS (Sushree listed devops)
   ─────────────────────────────────────
   ✦ Pipeline stages: build → test → SAST/DAST → push image → deploy
   ✦ Docker: Dockerfile best practices, multi-stage builds, layer caching
   ✦ .dockerignore: what to exclude
   ✦ Container image optimization: Alpine, JRE not JDK, non-root user
   ✦ Helm: chart structure, values.yaml, upgrade/rollback
   ✦ GitOps: ArgoCD concept (Git = source of truth → auto-sync to cluster)
   ✦ Blue-green, canary, rolling deployments — how to implement in K8s
   ✦ SonarQube: static analysis, quality gates
   ✦ Tools awareness: Jenkins, GitHub Actions, GitLab CI
   ✦ Infrastructure as Code: Terraform basics (what it does)
```

### 🟢 GOOD TO KNOW (differentiators — prep last)

```
7. MULESOFT / INTEGRATION PATTERNS (Sushree is MuleSoft certified)
   ──────────────────────────────────────────────────────────────
   ✦ What MuleSoft is: integration platform, API-led connectivity
   ✦ API-led connectivity layers:
       System APIs    → connect to backend systems (DB, SAP, legacy)
       Process APIs   → orchestrate business logic across system APIs
       Experience APIs → tailored for specific consumers (web, mobile)
   ✦ ESB (Enterprise Service Bus) vs microservices — why ESB fell out of favor
   ✦ Integration patterns:
       - Request-Reply
       - Fire-and-Forget
       - Pub-Sub
       - Content-Based Routing
       - Message Transformation
       - Idempotent Receiver
   ✦ API design: OpenAPI/Swagger, contract-first development
   ✦ Even if you don't know MuleSoft, knowing these patterns scores big points.

8. DATA ENGINEERING BASICS (Surendra is Databricks certified)
   ──────────────────────────────────────────────────────────
   ✦ ETL vs ELT:
       ETL = Extract → Transform → Load (traditional, transform before storage)
       ELT = Extract → Load → Transform (modern, load raw then transform in DW)
   ✦ Batch processing vs stream processing
       Batch: Spark, Databricks, scheduled jobs
       Stream: Kafka Streams, Flink, Kinesis
   ✦ Change Data Capture (CDC): track DB changes → stream to data lake
   ✦ Data Lake vs Data Warehouse vs Lakehouse
   ✦ Medallion architecture: Bronze (raw) → Silver (cleaned) → Gold (business)
   ✦ How APIs feed data lakes/warehouses (events, CDC, batch exports)
   ✦ Basic awareness of Spark/Databricks (what they solve, not implementation)
   ✦ FinOps: cloud cost optimization concepts (Surendra has FinOps cert)

9. SYSTEM DESIGN (Sushree — Architect role, end-to-end thinking)
   ──────────────────────────────────────────────────────────────
   ✦ Be ready to design one of these on a whiteboard:
       - URL shortener
       - Notification system (email/SMS/push)
       - E-commerce order processing system
       - Real-time dashboard/analytics pipeline
       - File upload and processing service
   ✦ Framework: Requirements → High-Level Design → Components →
     Data Model → API Design → Scaling → Trade-offs
   ✦ Always discuss: caching, load balancing, DB choice, async processing
   ✦ Show you think about: failure modes, monitoring, cost
   → SD-Topics-Batch-1.md patterns help here ✅
```

---

## 2-Day Prep Plan

```
MONDAY (March 30 — Today):
  ┌─────────────────────────────────────────────────────┐
  │ Morning (2 hrs):   Java + Spring Boot deep review   │
  │ Afternoon (2 hrs): SQL — window functions, CTEs,    │
  │                    indexing, query optimization      │
  │ Evening (1.5 hrs): Review K8s-Complete-Refresher.md │
  │                    Focus: probes, scaling, troublesh │
  └─────────────────────────────────────────────────────┘

TUESDAY (March 31):
  ┌─────────────────────────────────────────────────────┐
  │ Morning (2 hrs):   AWS services — EKS, SQS, S3,    │
  │                    RDS, Lambda, VPC, IAM             │
  │ Afternoon (1.5 hrs): Microservices patterns + CI/CD │
  │                      pipeline walkthrough            │
  │ Evening (1 hr):    Mock system design — design one  │
  │                    end-to-end system out loud        │
  │ Night (1 hr):      Final review of all notes        │
  └─────────────────────────────────────────────────────┘

WEDNESDAY (April 2 — Interview Day):
  ┌─────────────────────────────────────────────────────┐
  │ Morning:  Quick review of cheat sheets ONLY.        │
  │           Don't learn new things.                    │
  │           Relax. Confidence > cramming.              │
  └─────────────────────────────────────────────────────┘
```

---

## Existing Notes That Cover These Topics

```
✅ K8s-Complete-Refresher.md    → Kubernetes architecture to production ops
✅ K8s-Interview-30.md          → 30 K8s interview questions with answers
✅ NodeJS-Complete-Refresher.md → Node.js full stack (for React/Node questions)
✅ SD-Topics-Batch-1.md         → 2PC, Saga, Stateless/Stateful, Rebalancing, Hot Key

📝 NEEDED: Java + Spring Boot refresher MD
📝 NEEDED: SQL Advanced Concepts refresher MD
📝 NEEDED: AWS Services refresher MD
```

---

## Interview Tips for These Specific Interviewers

```
FOR SUSHREE (Architect):
  ✦ Answer with "What → Why → How" structure
  ✦ Always mention trade-offs: "I chose X over Y because..."
  ✦ She'll ask follow-ups — go deep confidently, say "I'd need to verify" if unsure
  ✦ Show architectural thinking: "In production, I'd also add monitoring/alerting"
  ✦ Name specific tools/libraries: "Resilience4j for circuit breaking", not just "a library"

FOR SURENDRA (Data/BI):
  ✦ Write SQL queries confidently — he may give you a problem to solve
  ✦ Explain your data model choices: "I denormalized here for query performance"
  ✦ Show awareness of downstream consumers: "This API supports pagination
    and filtering so BI tools can pull efficiently"
  ✦ If he asks about Python/data tools, be honest about your level
  ✦ Cost awareness is a plus (he has FinOps cert): "I'd use S3 Intelligent-Tiering
    to optimize storage costs"
```
