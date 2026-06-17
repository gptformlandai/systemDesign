🏆 The Definitive MAANG System Design Master Canon

📚 Organized Study Library
    • [0. Core Foundations](00-Core-Foundations/README.md)
    • [1. High-Level Design (HLD)](01-High-Level-Design-HLD/README.md)
    • [2. Low-Level Design (LLD)](02-Low-Level-Design-LLD/README.md)
    • [3. End-to-End System Design Practice](03-End-to-End-System-Design-Practice/README.md)
    • [4. Architect-Level Thinking](04-Architect-Level-Thinking/README.md)
    • [5. Interview Execution Framework](05-Interview-Execution-Framework/README.md)
    • [6. Elite MAANG Appendices](06-Elite-MAANG-Appendices/README.md)
    • [7. Staff / Principal-Level Additions](07-Staff-Principal-Level-Additions/README.md)
    • [PDF exports](PDFs/README.md)
    • [Utility scripts](scripts/README.md)

0️⃣ Core Foundations (Non-Negotiable)
    • [Core foundations notes](00-Core-Foundations/README.md)
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
    • [HLD mentorship tracks](01-High-Level-Design-HLD/README.md)
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
    • OAuth / JWT 
    • Rate limiting
    • DDoS protection
    • Encryption in transit & at rest
    • Secrets management
2️⃣ Low-Level Design (LLD)
    • [LLD mentorship tracks](02-Low-Level-Design-LLD/README.md)
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
    • [Problem bank by category](03-End-to-End-System-Design-Practice/README.md)

    Core Infrastructure
    • [URL shortener](03-End-to-End-System-Design-Practice/01-Core-Infrastructure/URL-Shortener-End-to-End-System-Design.md)
    • [API gateway](03-End-to-End-System-Design-Practice/01-Core-Infrastructure/API-Gateway-End-to-End-System-Design.md)
    • [Notification system](03-End-to-End-System-Design-Practice/01-Core-Infrastructure/Notification-System-End-to-End-System-Design.md)

    Caching Systems
    • [LRU cache](03-End-to-End-System-Design-Practice/02-Caching-Systems/LRU-Cache-End-to-End-System-Design.md)
    • [LFU cache](03-End-to-End-System-Design-Practice/02-Caching-Systems/LFU-Cache-End-to-End-System-Design.md)
    • [CDN cache](03-End-to-End-System-Design-Practice/02-Caching-Systems/CDN-Cache-End-to-End-System-Design.md)
    • [Distributed cache](03-End-to-End-System-Design-Practice/02-Caching-Systems/Distributed-Cache-End-to-End-System-Design.md)

    Feeds / Social Systems
    • [News feed](03-End-to-End-System-Design-Practice/03-Feeds-Social-Systems/News-Feed-End-to-End-System-Design.md)
    • [Instagram / Facebook feed](03-End-to-End-System-Design-Practice/03-Feeds-Social-Systems/Instagram-Facebook-Feed-End-to-End-System-Design.md)
    • [Twitter (X) feed](03-End-to-End-System-Design-Practice/03-Feeds-Social-Systems/Twitter-X-Feed-End-to-End-System-Design.md)
    • [TikTok video feed and recommendations](03-End-to-End-System-Design-Practice/03-Feeds-Social-Systems/TikTok-Video-Feed-Recommendations-End-to-End-System-Design.md)
    • [LinkedIn feed](03-End-to-End-System-Design-Practice/03-Feeds-Social-Systems/LinkedIn-Feed-End-to-End-System-Design.md)
    • [Reddit / discussion forum](03-End-to-End-System-Design-Practice/03-Feeds-Social-Systems/Reddit-Discussion-Forum-End-to-End-System-Design.md)

    Media Streaming Systems
    • [YouTube](03-End-to-End-System-Design-Practice/04-Media-Streaming-Systems/YouTube-End-to-End-System-Design.md)
    • [Netflix](03-End-to-End-System-Design-Practice/04-Media-Streaming-Systems/Netflix-End-to-End-System-Design.md)
    • [Spotify](03-End-to-End-System-Design-Practice/04-Media-Streaming-Systems/Spotify-End-to-End-System-Design.md)

    Messaging / Realtime Systems
    • [Chat system (WebSockets at scale)](03-End-to-End-System-Design-Practice/05-Messaging-Realtime-Systems/Chat-System-End-to-End-System-Design.md)
    • [WhatsApp](03-End-to-End-System-Design-Practice/05-Messaging-Realtime-Systems/WhatsApp-End-to-End-System-Design.md)
    • [Messenger](03-End-to-End-System-Design-Practice/05-Messaging-Realtime-Systems/Messenger-End-to-End-System-Design.md)
    • [Slack](03-End-to-End-System-Design-Practice/05-Messaging-Realtime-Systems/Slack-End-to-End-System-Design.md)
    • [Discord](03-End-to-End-System-Design-Practice/05-Messaging-Realtime-Systems/Discord-End-to-End-System-Design.md)

    Transaction / Booking Systems
    • [Uber / Ola](03-End-to-End-System-Design-Practice/06-Transaction-Booking-Systems/Uber-Ola-End-to-End-System-Design.md)
    • [BookMyShow](03-End-to-End-System-Design-Practice/06-Transaction-Booking-Systems/BookMyShow-End-to-End-System-Design.md)
    • [Airline booking system](03-End-to-End-System-Design-Practice/06-Transaction-Booking-Systems/Airline-Booking-System-End-to-End-System-Design.md)
    • [Food delivery (Swiggy / Zomato)](03-End-to-End-System-Design-Practice/06-Transaction-Booking-Systems/Food-Delivery-Swiggy-Zomato-End-to-End-System-Design.md)

    Storage / Database Systems
    • [Dropbox / Google Drive](03-End-to-End-System-Design-Practice/07-Storage-Database-Systems/Dropbox-Google-Drive-End-to-End-System-Design.md)
    • [File storage system](03-End-to-End-System-Design-Practice/07-Storage-Database-Systems/File-Storage-System-End-to-End-System-Design.md)
    • [Key-value store (Redis-like)](03-End-to-End-System-Design-Practice/07-Storage-Database-Systems/Key-Value-Store-Redis-Like-End-to-End-System-Design.md)
    • [Log storage system](03-End-to-End-System-Design-Practice/07-Storage-Database-Systems/Log-Storage-System-End-to-End-System-Design.md)

    Concurrency / Machine Coding Classics
    • [Rate limiter](03-End-to-End-System-Design-Practice/08-Concurrency-Machine-Coding-Classics/Rate-Limiter-End-to-End-System-Design.md)
    • [Producer-Consumer](03-End-to-End-System-Design-Practice/08-Concurrency-Machine-Coding-Classics/Producer-Consumer-End-to-End-System-Design.md)
    • [Thread pool](03-End-to-End-System-Design-Practice/08-Concurrency-Machine-Coding-Classics/Thread-Pool-End-to-End-System-Design.md)
    • [Blocking queue](03-End-to-End-System-Design-Practice/08-Concurrency-Machine-Coding-Classics/Blocking-Queue-End-to-End-System-Design.md)
    • [Logger system](03-End-to-End-System-Design-Practice/08-Concurrency-Machine-Coding-Classics/Logger-System-End-to-End-System-Design.md)

    E-Commerce / Product Systems
    • [Amazon e-commerce system](03-End-to-End-System-Design-Practice/09-Ecommerce-Product-Systems/Amazon-E-Commerce-System-End-to-End-System-Design.md)
    • [Shopping cart](03-End-to-End-System-Design-Practice/09-Ecommerce-Product-Systems/Shopping-Cart-End-to-End-System-Design.md)
    • [Payment system](03-End-to-End-System-Design-Practice/09-Ecommerce-Product-Systems/Payment-System-End-to-End-System-Design.md)
    • [Coupon / discount engine](03-End-to-End-System-Design-Practice/09-Ecommerce-Product-Systems/Coupon-Discount-Engine-End-to-End-System-Design.md)
    • [Payment workflow](03-End-to-End-System-Design-Practice/09-Ecommerce-Product-Systems/Payment-Workflow-End-to-End-System-Design.md)

    Graph / Search / Recommendation Systems
    • [Search autocomplete](03-End-to-End-System-Design-Practice/10-Graph-Search-Recommendation-Systems/Search-Autocomplete-End-to-End-System-Design.md)
    • [Google Search](03-End-to-End-System-Design-Practice/10-Graph-Search-Recommendation-Systems/Google-Search-End-to-End-System-Design.md)
    • [Search engine (ElasticSearch-like)](03-End-to-End-System-Design-Practice/10-Graph-Search-Recommendation-Systems/Search-Engine-ElasticSearch-Like-End-to-End-System-Design.md)
    • [Recommendation system](03-End-to-End-System-Design-Practice/10-Graph-Search-Recommendation-Systems/Recommendation-System-End-to-End-System-Design.md)
    • For each: APIs, Data model, Caching, Scaling, Failures, Trade-offs
4️⃣ Architect-Level Thinking (The Real Differentiator)
    • [Architect-level thinking track](04-Architect-Level-Thinking/README.md)
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
    • [Interview execution guide](05-Interview-Execution-Framework/README.md)
    • HLD Flow: Clarify requirements, Define assumptions, Estimate scale, Define APIs, Design data model, High-level architecture, Bottlenecks, Failure handling, Trade-offs
    • LLD Flow: Clarify use cases, Identify entities, Design classes, Define relationships, Handle concurrency, Ensure extensibility
🌟 6️⃣️⃣ Elite MAANG Appendices
    • [Elite MAANG appendices track](06-Elite-MAANG-Appendices/README.md)
    • 6.1 Advanced Distributed Data & Engines: LSM-Trees vs B-Trees, SSTables & Memtables, Compaction strategies, Online schema changes at scale
    • 6.2 Time & Ordering in Distributed Systems: Clock drift, Lamport clocks, Vector clocks, TrueTime
    • 6.3 Advanced Networking & Infrastructure: TCP BBR vs Cubic, Anycast & BGP, Edge computing, Service mesh concepts, Sidecar pattern, Control plane vs data plane
    • 6.4 Probabilistic & Specialized Data Structures: Bloom filters, HyperLogLog, Quad-trees, Geohashing
    • 6.5 High-Scale Data Engineering: Batch vs stream processing, Change Data Capture (CDC), Backpressure & load shedding
🆕 7️⃣️⃣ Staff / Principal-Level Additions
    • [Staff / principal-level additions track](07-Staff-Principal-Level-Additions/README.md)
    • 7.1 Edge, Gateway & Traffic Management: API gateways, Global load balancing, Request shaping, Throttling, WAF concepts, Edge authentication, Multi-layer rate limiting
    • 7.2 Multi-Region & Disaster Recovery: Active-active vs active-passive, RPO & RTO, Cross-region replication lag, Geo-fencing & data residency, Region failover strategies, Split-brain avoidance
    • 7.3 Data Lifecycle & Storage Economics: Hot / warm / cold storage tiers, Tiered storage policies, Archival pipelines, Data retention strategies, GDPR-style deletions, Cost-aware storage design
    • 7.4 Deployment, Migration & Evolution: Infrastructure as Code mindset, Immutable infrastructure, Feature flags, Dark launches, Schema versioning, Online migrations, Roll-forward vs rollback
    • 7.5 Rate Limiting Algorithms (Deep Dive): Token bucket, Leaky bucket, Fixed window, Sliding window log, Sliding window counter, Distributed rate limiting
    • 7.6 Advanced Security & Compliance: PII segregation, Encryption key rotation, Audit logging, RBAC vs ABAC, Least privilege enforcement, Zero-trust principles
    • 7.7 Advanced Scaling & Migration Patterns: Shadow traffic, Dual writes, Read/write splitting, Strangler Fig pattern, Monolith → microservices evolution, Contract testing
    • 7.8 Decision Narration & Staff-Level Communication: Assumption declaration, Multiple-option framing, Explicit trade-off comparison, Justified decision making, Rejected-alternative explanation, Business impact alignment
