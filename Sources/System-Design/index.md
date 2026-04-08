🏆 The Definitive MAANG System Design Master Canon
0️⃣ Core Foundations (Non-Negotiable)
Computer Science Fundamentals
    • Time & space complexity (incl. amortized analysis)
    • Memory hierarchy (CPU cache → RAM → Disk/SSD)
    • CPU-bound vs I/O-bound workloads
    • Cache locality & data access patterns
Operating Systems
    • Processes vs threads
    • Context switching
    • Locks, mutexes, semaphores
    • Deadlocks & starvation
    • Virtual memory basics
Networking
    • TCP vs UDP
    • HTTP/1.1 vs HTTP/2 vs HTTP/3
    • DNS resolution
    • TLS handshake (conceptual)
    • Latency vs bandwidth
    • Connection pooling
1️⃣ High-Level Design (HLD)
1.1 Requirements & Estimation
    • Functional vs non-functional requirements   
    • Latency targets
    • Throughput targets
    • Availability (e.g., 99.9 vs 99.99)
    • Back-of-the-envelope calculations: (QPS, Storage estimation, Memory feasibility, Bandwidth estimation)
1.2 Data Management & Storage
    • Relational Databases: B-Trees (why optimized for disk), Indexing trade-offs, Write-Ahead Logging (WAL), ACID properties, Isolation levels, Read replicas, Master-slave vs multi-writer
    • NoSQL Databases: Key-value stores, Document databases, Wide-column stores, When NOT to use NoSQL
    • Data Modeling: Normalization vs denormalization, Read-heavy vs write-heavy systems, Hot partitions, Secondary indexes
1.3 Consistency & Distributed Coordination
    • CAP theorem (deep intuition)
    • PACELC theorem
    • Strong vs eventual consistency
    • Quorum (R/W/N)
    • Leader election
    • Distributed locks
    • Saga pattern
    • 2-Phase Commit (2PC)
1.4 Scaling & Partitioning
    • Vertical vs horizontal scaling
    • Stateless vs stateful services
    • Sharding strategies: (Hash-based, Range-based, Geo-based)
    • Rebalancing & resharding
    • Hot key problem
1.5 Communication Models & Real-Time Systems ⭐
    • Request–Response: REST APIs, gRPC, GraphQL, Idempotency, Pagination, Versioning
    • Client-Pull Models: Polling (fixed interval), Long polling
    • Server-Push Models: WebSockets (full-duplex), Server-Sent Events (SSE)
    • Choosing Between Them: Chat → WebSockets, Live feed → SSE, Periodic dashboard → Polling, Internal service calls → gRPC, Public APIs → REST
1.6 Caching
    • Cache layers (CDN, service, DB)
    • LRU, LFU eviction
    • Write-through vs write-back
    • TTL strategies
    • Cache invalidation
    • Cache stampede
    • Hot keys
1.7 Asynchronous & Event-Driven Systems
    • Message queues vs event streams
    • At-most-once / at-least-once / exactly-once
    • Idempotent consumers
    • Dead-letter queues
    • Backpressure handling
    • Event-driven architecture
1.8 Reliability & Fault Tolerance
    • Replication
    • Redundancy
    • Graceful degradation
    • Retries with exponential backoff
    • Circuit breakers
    • Bulkheads
    • Cell-based architecture
    • Blast radius containment
1.9 Observability & Operations
    • Metrics, logs, distributed tracing
    • Liveness vs readiness probes
    • SLIs, SLOs, SLAs
    • Canary deployments
    • Blue-green deployments
1.10 Security
    • Authentication vs authorization
    • OAuth / JWT (conceptual)
    • Rate limiting
    • DDoS protection
    • Encryption in transit & at rest
    • Secrets management
2️⃣ Low-Level Design (LLD)
2.1 Object-Oriented Design
    • SOLID principles (applied)
    • Encapsulation, abstraction
    • Composition vs inheritance
    • Immutability
    • Cohesion & coupling
2.2 High-Signal Design Patterns
    • Creational: Factory, Builder, Singleton (and why dangerous)
    • Structural: Adapter, Decorator, Facade, Proxy
    • Behavioral: Strategy, Observer, State, Command
2.3 UML & Modeling
    • Class diagrams, Sequence diagrams, State diagrams, Entity-relationship modeling
2.4 Concurrency & Thread Safety
    • Thread safety principles
    • Optimistic vs pessimistic locking
    • Atomic operations (CAS intuition)
    • Thread pools
    • Producer–consumer pattern
2.5 Performance Engineering
    • Profiling, Memory leaks, GC basics
    • Object lifecycle, Lazy loading, Batching
3️⃣ End-to-End System Design Practice
    • URL shortener
    • News feed
    • Chat system (WebSockets at scale)
    • Notification system
    • Payment workflow
    • File storage system
    • For each: APIs, Data model, Caching, Scaling, Failures, Trade-offs
4️⃣ Architect-Level Thinking (The Real Differentiator)
    • Latency vs consistency
    • Cost vs reliability
    • Simplicity vs extensibility
    • Strong vs eventual consistency
    • Build vs buy decisions
    • Cost awareness
    • Over-engineering detection
    • Failure-first design
    • Rollbacks & recovery
5️⃣ Interview Execution Framework
    • HLD Flow: Clarify requirements, Define assumptions, Estimate scale, Define APIs, Design data model, High-level architecture, Bottlenecks, Failure handling, Trade-offs
    • LLD Flow: Clarify use cases, Identify entities, Design classes, Define relationships, Handle concurrency, Ensure extensibility
🌟 6️⃣️⃣ Elite MAANG Appendices
    • 6.1 Advanced Distributed Data & Engines: LSM-Trees vs B-Trees, SSTables & Memtables, Compaction strategies, Online schema changes at scale
    • 6.2 Time & Ordering in Distributed Systems: Clock drift, Lamport clocks, Vector clocks, TrueTime
    • 6.3 Advanced Networking & Infrastructure: TCP BBR vs Cubic, Anycast & BGP, Edge computing, Service mesh concepts, Sidecar pattern, Control plane vs data plane
    • 6.4 Probabilistic & Specialized Data Structures: Bloom filters, HyperLogLog, Quad-trees, Geohashing
    • 6.5 High-Scale Data Engineering: Batch vs stream processing, Change Data Capture (CDC), Backpressure & load shedding
🆕 7️⃣️⃣ Staff / Principal-Level Additions
    • 7.1 Edge, Gateway & Traffic Management: API gateways, Global load balancing, Request shaping, Throttling, WAF concepts, Edge authentication, Multi-layer rate limiting
    • 7.2 Multi-Region & Disaster Recovery: Active-active vs active-passive, RPO & RTO, Cross-region replication lag, Geo-fencing & data residency, Region failover strategies, Split-brain avoidance
    • 7.3 Data Lifecycle & Storage Economics: Hot / warm / cold storage tiers, Tiered storage policies, Archival pipelines, Data retention strategies, GDPR-style deletions, Cost-aware storage design
    • 7.4 Deployment, Migration & Evolution: Infrastructure as Code mindset, Immutable infrastructure, Feature flags, Dark launches, Schema versioning, Online migrations, Roll-forward vs rollback
    • 7.5 Rate Limiting Algorithms (Deep Dive): Token bucket, Leaky bucket, Fixed window, Sliding window log, Sliding window counter, Distributed rate limiting
    • 7.6 Advanced Security & Compliance: PII segregation, Encryption key rotation, Audit logging, RBAC vs ABAC, Least privilege enforcement, Zero-trust principles
    • 7.7 Advanced Scaling & Migration Patterns: Shadow traffic, Dual writes, Read/write splitting, Strangler Fig pattern, Monolith → microservices evolution, Contract testing
    • 7.8 Decision Narration & Staff-Level Communication: Assumption declaration, Multiple-option framing, Explicit trade-off comparison, Justified decision making, Rejected-alternative explanation, Business impact alignment
