# Social Feed Distribution Architecture: Push, Pull & Hybrid Simulation

This project is a high-performance, reactive Spring Boot (WebFlux) and Apache Kafka demonstration of the three primary social feed distribution models: **Push (Fanout-on-Write)**, **Pull (Fanout-on-Read)**, and **Hybrid**.

It simulates real-world scale and follow-skew characteristics (e.g., normal users vs. celebrities) to measure database writes, read latencies, and storage footprints.

---

## 🏗️ Architectural Overview

The project models the read and write paths of major social networks (Instagram, Facebook, Twitter/X) in five cooperating layers:

```text
POST PATH (Write & Fanout)
Client -> POST /api/posts -> DB Write (Post Store) 
                          -> Emit PostCreatedEvent to Kafka (or Mock Event Bus)
                          -> Kafka Listener (Fanout Worker) 
                             -> Query Follower Graph
                             -> Insert references into feed_inbox of followers

GET PATH (Read Timeline)
Client -> GET /api/feed/{userId}?strategy=PUSH|PULL|HYBRID
       -> PUSH:   Read feed_inbox table directly -> Hydrate post details
       -> PULL:   Read follow graph -> Query followed timelines -> Merge-sort in memory
       -> HYBRID: Read feed_inbox (normal users) + Pull celebrity timelines -> Merge-sort
```

### The Three Feed Models Defined

| Distribution Model | Write Path Behavior | Read Path Behavior | Production Advantage | Production Bottleneck |
|:---|:---|:---|:---|:---|
| **Push** (Fanout-on-Write) | Post is immediately copied (written) into the inbox of all followers via Kafka. | Bounded $O(1)$ lookup from `feed_inbox` table. | Extremely fast reads (under 15ms). | High write amplification for creators with millions of followers. |
| **Pull** (Fanout-on-Read) | Post is written only to the author's personal timeline. No fanout. | Queries followed graph, scans timelines of all followed users, and merge-sorts. | Cheap, fast writes. | Heavy database read pressure and high network latency at read-time. |
| **Hybrid** | Push is used for normal users; Pull is dynamically executed for celebrity posts. | Merges pushed normal posts from `feed_inbox` with pulled celebrity timeline posts. | Balanced write overhead and consistent read latency. | Higher query complexity on the read path. |

---

## 🚀 Getting Started

### 📋 Prerequisites
* **Java**: JDK 17 or JDK 21 (Java 21 is running on your environment)
* **Docker**: Docker Desktop (optional, only required for Real Kafka mode)
* **Maven**: Built-in wrapper script `mvnw.cmd` (or Maven downloaded at `C:\Users\aravi\.gemini\antigravity\apache-maven-3.9.6\bin\mvn.cmd`)

---

## 🛠️ Running the Application

You can execute this simulation in two modes: **Zero-Dependency Mock Mode** (starts instantly without Docker) or **Real Broker Mode** (uses Docker Kafka containers).

### Option A: Zero-Dependency Mock Mode (Recommended for quick testing)
This mode bypasses Docker. It uses an in-memory queue that mimics Kafka’s asynchronous delivery transit time (adds a simulated 5ms delay) using a virtual thread pool.

1. Open your terminal in the project directory:
   ```powershell
   cd C:\Users\aravi\Desktop\SystemDesign\july-2026\feed-system-simulation
   ```
2. Run the application:
   ```powershell
   C:\Users\aravi\.gemini\antigravity\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
   ```
3. The server will start on port **`8080`**.

---

### Option B: Real Apache Kafka Mode
This mode connects Spring Boot to a real Dockerized Apache Kafka broker running in KRaft mode.

1. Start the Kafka container:
   ```powershell
   docker compose up -d
   ```
2. Start the Spring Boot application with the `kafka` profile active:
   ```powershell
   C:\Users\aravi\.gemini\antigravity\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=kafka
   ```
3. Spring WebFlux will connect to Kafka on `localhost:9092` and register the `post-events` topic automatically.

---

## 📊 How to Run Benchmarks & Tests

The API provides a fully automated testing suite that seeds a skewed social graph, executes post writes, performs read queries, and compares results.

### Step 1: Trigger the Automated Simulation
Run a POST request to the simulation endpoint. This seeds **50 normal users**, **2 celebrities**, and generates **100 random posts** (with a 20% celebrity probability distribution):

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/simulate?users=50&celebrities=2&posts=100" | ConvertTo-Json -Depth 5
```

### Step 2: Analyze the Performance Output
The simulation runs all three models sequentially and returns a JSON report:

```json
{
  "PULL_Strategy": {
    "strategy": "PULL",
    "totalPostsWritten": 100,
    "postCreateDbWrites": 100,
    "postCreateDbReads": 100,
    "totalFeedReadsTriggered": 250,
    "feedReadDbReads": 500,
    "avgFeedReadLatencyMs": "429.51"
  },
  "PUSH_Strategy": {
    "strategy": "PUSH",
    "totalPostsWritten": 100,
    "postCreateDbWrites": 1238,
    "postCreateDbReads": 200,
    "totalFeedReadsTriggered": 250,
    "feedReadDbReads": 5940,
    "avgFeedReadLatencyMs": "1245.70"
  },
  "HYBRID_Strategy": {
    "strategy": "HYBRID",
    "totalPostsWritten": 100,
    "postCreateDbWrites": 525,
    "postCreateDbReads": 186,
    "totalFeedReadsTriggered": 250,
    "feedReadDbReads": 3125,
    "avgFeedReadLatencyMs": "910.07"
  }
}
```

* **Write DB Writes (Write Amplification)**: Note how **PUSH** requires 1,238 database writes for 100 posts (due to fanout), while **HYBRID** cuts this by half to 525 because celebrity posts are pulled, not pushed.
* **Feed Read Latency**: In our in-memory H2 SQL database, PULL runs fast because joins are in-memory. However, PUSH and HYBRID execute multiple sequential `findById` fetches to hydrate post contents. In production, these individual fetches are replaced by ultra-fast Redis cache reads.

---

## 🗄️ Database Inspection

The project uses an in-memory SQL database (H2) via R2DBC. 

### Why R2DBC + H2?
Since social feeds are highly concurrent, traditional blocking JDBC drivers block threads on every DB query. We use **R2DBC (Reactive Relational Database Connectivity)** to execute completely non-blocking reactive streams, meaning Netty threads are never idle.

### Tables & Schema
The database compiles the following schema on startup:
* **`users`**: Stores user metadata and `is_celebrity` status flags.
* **`posts`**: Stores canonical post details (the source of truth).
* **`follows`**: Stores follow-graph edges (forward and reverse index lookups).
* **`feed_inbox`**: Stores viewer-specific timeline feed lists containing only lightweight references `(viewer_id, post_id, author_id)`.

### 🔌 Connecting from your IDE (IntelliJ IDEA / DBeaver / DataGrip)
To allow external clients to inspect the database in real-time, the application starts a local **H2 TCP Server** on port **`9090`** during startup.

While the Spring Boot application is running, configure your IDE's Database tool as follows:
1. **Driver**: H2
2. **Connection Type**: `Remote` (TCP Server)
3. **JDBC URL**: `jdbc:h2:tcp://localhost:9090/mem:feed_sim_db`
4. **User**: `sa`
5. **Password**: *(leave blank)*
6. Click **Test Connection** and connect! You can now run `SELECT` queries on `users`, `posts`, `follows`, and `feed_inbox` dynamically.

### Monitoring Database Operations
To check DB read/write operation counts without external client setups, hit the metrics endpoint at any time during testing:
```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/simulate/metrics" | ConvertTo-Json -Depth 5
```

---

## 🛠️ Step-by-Step Manual Endpoint Verification

If you prefer testing individual actions manually:

### 1. Create a User
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/users" -ContentType "application/json" -Body '{"username": "elonmusk", "celebrity": true}'
```

### 2. Follow a User
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/follow" -ContentType "application/json" -Body '{"followerId": 2, "followedId": 1}'
```

### 3. Write a Post (Will trigger asynchronous fanout via Kafka/Mock Bus)
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/posts" -ContentType "application/json" -Body '{"authorId": 1, "content": "Hello SpaceX!"}'
```

### 4. Fetch the Feed Dynamically
Toggle strategies dynamically via query parameter to inspect the differences:
```powershell
# Get Feed via PUSH Strategy
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/feed/2?strategy=PUSH"

# Get Feed via PULL Strategy
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/feed/2?strategy=PULL"

# Get Feed via HYBRID Strategy
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/feed/2?strategy=HYBRID"
```
