# Reliability & Fault Tolerance - Mentorship Track

> Goal: build strong intuition and interview-ready depth for keeping systems available, resilient, and safe under failure.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `1.8 Reliability & Fault Tolerance`.
- We will follow the same learning style used in the communication-model and asynchronous-system notes.
- We will add topics one by one in a repeatable architect-level structure.
- We will include code samples, mini programs, and interview-style answers.

---

## Roadmap for This Sheet

1. Replication
2. Redundancy
3. Graceful degradation
4. Retries with exponential backoff
5. Circuit breakers
6. Bulkheads
7. Cell-based architecture
8. Blast radius containment

---

# Topic 1: Replication

> Track: 1.8 Reliability & Fault Tolerance
> Scope: data copies, leader-follower patterns, durability, read scaling, replica lag

---

## 1. Intuition

Think of replication like keeping multiple copies of an important notebook.

If one notebook is damaged or unavailable, the information still exists somewhere else. That protects you from loss. If several people want to read at the same time, multiple copies also reduce contention.

In distributed systems, replication means storing the same data on multiple nodes so the system can survive failures and often serve more reads.

Short memory trick:
- partitioning spreads data out
- replication copies data for safety and availability

---

## 2. Definition

- Definition: Replication is the practice of maintaining multiple copies of the same data across nodes, machines, zones, or regions.
- Category: Reliability, durability, and availability mechanism
- Core idea: If one copy fails or becomes unreachable, another copy can still serve data or help recover state.

Common forms:
- leader-follower replication
- multi-leader replication
- quorum-based replication
- synchronous replication
- asynchronous replication

---

## 3. Why It Exists

Single copies fail.

Disks fail, hosts crash, zones become unavailable, networks partition, and deployments go wrong. If important data exists in only one place, the system has a single failure away from outage or loss.

Replication exists because systems need:
- higher durability
- higher availability
- safer failover
- read scaling in many designs
- disaster recovery options

It is one of the core building blocks behind reliable storage systems.

---

## 4. Reality

### Replication is common in:

- primary-replica relational databases
- distributed key-value stores
- Kafka partition replicas
- search clusters
- object storage systems
- multi-AZ and multi-region databases

### Real-world architecture truth

Replication does not come for free.

It improves reliability, but it introduces:
- write coordination cost
- replica lag
- failover complexity
- stale reads
- split-brain risks in some architectures

Another important truth:
- replication is not the same as backup

Replication copies current state, including bad writes and accidental deletes. Backups help recover older good state. Replication helps survive current failures.

---

## 5. How It Works

The basic idea is simple:

1. A client writes data.
2. The system stores that data on more than one node.
3. Reads may come from one or many replicas depending on the design.
4. If one replica fails, another can continue serving.

### Leader-follower flow

1. Client sends write to the leader.
2. Leader commits the write locally.
3. Leader ships the change log to followers.
4. Followers apply the same change.
5. Reads may go to leader or followers.

### Synchronous replication

- The write is not considered successful until one or more replicas also confirm it.
- Better consistency and lower data-loss window
- Higher write latency

### Asynchronous replication

- The leader acknowledges the write before replicas fully catch up.
- Lower write latency
- Higher risk of lag and data loss if the leader fails before followers catch up

### Quorum intuition

Some systems require enough replicas to acknowledge a write or participate in reads so the system can preserve stronger guarantees without every replica responding every time.

---

## 6. What Problem It Solves

- Primary problem solved: protects data and availability against node or location failure
- Secondary benefits: read scaling, faster failover, regional resilience, maintenance flexibility
- Systems impact: directly affects durability guarantees, read/write latency, and recovery behavior

Replication is the main answer when the question is:
- what happens if this machine dies right now?

---

## 7. When to Rely on It

Use replication when:
- the data is important enough that one copy is unacceptable
- the service must survive host or zone failure
- reads are heavy and can be distributed
- failover requirements are meaningful
- recovery point objectives matter

Especially important for:
- user accounts
- orders
- payments
- metadata systems
- logs and event infrastructure
- production databases

---

## 8. When Not to Use It

Do not assume replication is automatically the right answer when:
- the system is tiny and failure cost is genuinely low
- operational simplicity matters more than high availability
- the team is not ready to reason about lag, consistency, and failover

Also avoid these bad assumptions:
- replicated means no backups needed
- followers are always up to date
- failover is trivial once replicas exist

Better framing:
- replication protects current availability
- backups protect historical recovery

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Replication | Higher durability, better availability, read scaling, safer failover | More write complexity, lag, stale reads, operational coordination overhead |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Write latency vs data safety:
  synchronous replication improves safety but slows writes.
- Read scale vs consistency:
  follower reads reduce leader load but may be stale.
- Simplicity vs resilience:
  one node is simpler, but replicas reduce outage risk.
- Local performance vs regional protection:
  cross-region replication improves disaster tolerance but adds latency and complexity.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Treating replication as backup | Bad updates also replicate | Keep independent backup and restore strategy |
| Reading from lagging replicas for critical flows | Can return stale state | Use leader reads or consistency-aware read paths |
| Ignoring failover testing | Replica promotion can fail operationally | Practice failover drills |
| Assuming async replication has zero data-loss risk | Leader can fail before followers catch up | Match replication mode to RPO needs |
| Replicating everywhere by default | More copies increase cost and coordination | Place replicas where reliability or locality justifies them |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Replication factor:
  often 3 for many distributed systems because it balances fault tolerance and cost
- Replica lag:
  should be monitored in milliseconds or seconds depending on the workload
- Recovery point objective:
  how much recent data loss is acceptable after failure
- Recovery time objective:
  how long service can be unavailable during recovery or failover
- Write acknowledgment policy:
  affects latency and durability semantics

Interview shorthand:
- replication factor, lag, failover, stale reads, RPO

---

## 12. Failure Modes

### Replica lag

Problem:
- Followers fall behind the leader during traffic spikes or network slowdown.

User impact:
- stale reads, bad failover targets, inconsistent user experience

Mitigation:
- monitor lag
- route critical reads to leader
- improve replication throughput

### Leader failure before followers catch up

Problem:
- The leader acknowledges a write and dies before async replicas fully apply it.

User impact:
- recent acknowledged writes may be lost after failover

Mitigation:
- synchronous or quorum writes where needed
- align replication mode with business durability requirements

### Split-brain or dual-writer confusion

Problem:
- Two nodes both behave like leaders due to coordination failure.

User impact:
- conflicting writes, data divergence, painful reconciliation

Mitigation:
- strong leader election
- fencing tokens
- careful failover controls

### Replica promotion failure

Problem:
- Replicas exist, but promotion scripts, DNS cutover, or application configs fail during incident response.

User impact:
- longer outage despite having redundant copies

Mitigation:
- run failover drills
- automate promotion
- keep operational playbooks current

---

## 13. Scenario

- Product / system: E-commerce orders database
- Requirement:
  the system must survive a single database node failure without losing customer orders
- Good design:
  primary-replica setup across availability zones, with controlled failover and clear read-routing rules
- Why replication fits:
  order data is valuable and the system cannot depend on one machine
- What would go wrong without it:
  a single host failure could take the write path down and potentially risk data loss

---

## 14. Code Sample

### Write path pseudocode

```java
public Order createOrder(CreateOrderRequest request) {
    Order order = primaryDb.insert(request);
    replicationMonitor.ensureHealthy();
    return order;
}
```

### Read routing sketch

```java
public Order getOrder(String orderId, boolean requiresFreshRead) {
    if (requiresFreshRead) {
        return primaryDb.find(orderId);
    }
    return readReplicaPool.find(orderId);
}
```

### Replica-apply sketch

```java
public void applyLogEntry(LogEntry entry) {
    stateMachine.apply(entry);
    replicationState.updateLastApplied(entry.sequenceNumber());
}
```

Key idea:
- write ownership and read routing must be explicit
- replication helps only if the application understands freshness and failover behavior

---

## 15. Mini Program / Simulation

This mini program shows a leader write and follower lag.

```python
leader = []
follower = []


def write(value: str) -> None:
    leader.append(value)
    print("Leader accepted:", value)


def replicate_one() -> None:
    if len(follower) < len(leader):
        follower.append(leader[len(follower)])


def main() -> None:
    write("order-1")
    write("order-2")

    print("Follower before catch-up:", follower)
    replicate_one()
    print("Follower after one step:", follower)
    print("Leader state:", leader)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- the leader can move ahead of followers
- replicas increase resilience, but freshness is not automatic
- lag matters operationally

---

## 16. Practical Question

> You are designing a user-account database that must survive zone failure and support heavy read traffic. How would you use replication, and what consistency risks would you explicitly call out?

---

## 17. Strong Answer

I would replicate the database across availability zones so the service can survive a single-zone or single-node failure. For a common design, I would use one primary for writes and one or more replicas for failover and read scaling.

I would keep the write path on the primary and allow carefully chosen read traffic on replicas, but I would explicitly call out replica lag. That means read-after-write-sensitive flows such as password changes, checkout confirmation, or profile updates may still need primary reads or stronger consistency controls.

If the business has strict durability requirements, I would consider synchronous or quorum-based acknowledgment for writes, understanding that this increases write latency. I would also make clear that replication is not backup. I would still keep independent backups and regularly test failover and restore procedures.

---

## 18. Revision Notes

- One-line summary: Replication keeps multiple copies of data so systems can survive failures and often scale reads, but it introduces lag and coordination trade-offs.
- Three keywords: replicas, lag, failover
- One interview trap: saying replication eliminates the need for backups
- One memory trick: partitioning spreads, replication copies

---

# Topic 2: Redundancy

> Track: 1.8 Reliability & Fault Tolerance
> Scope: duplicate components, spare capacity, active-active, active-passive, eliminating single points of failure

---

## 1. Intuition

Think of redundancy like carrying a spare tire.

You hope you never need it, but the reason it exists is that failure is normal enough to plan for. The spare does not make the car faster. It makes the journey more survivable.

In systems, redundancy means having extra components, extra paths, or extra capacity so one failure does not stop the service.

Short memory trick:
- replication is redundant data
- redundancy is the broader idea of duplicate capacity and components

---

## 2. Definition

- Definition: Redundancy is the deliberate duplication of critical system components, capacity, or paths so the service can continue operating when part of the system fails.
- Category: Availability and fault-tolerance design principle
- Core idea: Remove single points of failure by ensuring that one broken component does not mean total outage.

Redundancy can exist in:
- servers
- network paths
- load balancers
- zones
- regions
- data stores
- power and infrastructure layers

---

## 3. Why It Exists

Every critical component eventually fails or becomes unreachable.

If the system depends on exactly one instance of something important, that thing becomes a single point of failure. Redundancy exists to avoid that trap.

It is used because systems need:
- higher uptime
- maintenance without downtime
- capacity headroom during incidents
- safer failover during deployments and outages

Redundancy is one of the simplest and strongest ways to buy reliability, but only if the duplicates are truly independent.

---

## 4. Reality

### Redundancy shows up in:

- multiple stateless app instances behind a load balancer
- databases with failover nodes
- multiple availability zones
- duplicate network links
- CDN points of presence
- standby control planes or coordinators

### Real-world architecture truth

Redundancy is often necessary but not sufficient.

You can have extra servers and still go down because:
- all instances depend on one database
- one shared cache fails
- one DNS or config system fails
- one bad deploy hits every copy at once

Another important truth:
- redundancy without independence can create false confidence

If all replicas share the same bug, same zone, same credential store, or same operator mistake, they may all fail together.

---

## 5. How It Works

At a high level:

1. Identify critical components.
2. Find single points of failure.
3. Add extra independent capacity or instances.
4. Detect failure quickly.
5. Route traffic to healthy capacity.

### Common redundancy models

#### Active-active

- Multiple components serve traffic at the same time.
- Better utilization and often faster failover
- Harder coordination in some systems

#### Active-passive

- One component actively serves traffic, another waits as standby.
- Simpler for some stateful systems
- Standby capacity may be underused

#### N+1

- The system needs N units to handle normal traffic and keeps one extra so a failure does not immediately overload the rest.

---

## 6. What Problem It Solves

- Primary problem solved: prevents single component failure from taking down the entire system
- Secondary benefits: maintenance flexibility, safer deployments, capacity headroom, faster incident recovery
- Systems impact: directly affects availability and how gracefully systems handle ordinary infrastructure failure

Redundancy is the answer to:
- what if this server, zone, or network path disappears right now?

---

## 7. When to Rely on It

Use redundancy when:
- the system has uptime requirements that matter
- traffic should continue during host or zone failure
- deployments must avoid total downtime
- critical dependencies cannot be single-instance
- maintenance windows are expensive or unacceptable

Especially important for:
- production APIs
- payment systems
- login systems
- control planes
- edge gateways
- databases and caches

---

## 8. When Not to Use It

Be careful about over-investing in redundancy when:
- the service is internal, low-risk, and cheap to restart
- the operational cost outweighs outage impact
- the team is not ready to operate the added complexity

Also avoid this misunderstanding:
- more copies does not automatically mean higher reliability

If the copies are badly coordinated or not independently failure-isolated, you may only multiply cost and confusion.

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Redundancy | Removes many single points of failure, improves uptime, enables safer maintenance | Increases cost, capacity planning, health-check complexity, and coordination overhead |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Cost vs uptime:
  extra capacity costs money but reduces outage risk.
- Utilization vs readiness:
  active-passive wastes capacity but is simpler; active-active uses capacity better but coordinates more.
- Simplicity vs resilience:
  single-instance designs are easy until they fail.
- Independence vs convenience:
  true redundancy often requires separate zones, networks, or failure domains.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Duplicating app servers but not the dependency chain | The real single point of failure remains | Map all critical dependencies end to end |
| Putting all redundant instances in one zone | Shared failure domain defeats the goal | Spread across zones or regions |
| Never testing failover | Standby paths may be broken in practice | Run failover drills and game days |
| Running at 100 percent utilization before failure | One node loss overloads the rest immediately | Keep spare capacity |
| Assuming identical copies mean independent failure | Shared config, bugs, or credentials can fail everywhere | Design for failure-domain separation |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- N+1 capacity:
  enough spare capacity to survive one unit failing
- Headroom:
  systems often keep extra margin so failover does not instantly overload survivors
- Failover detection time:
  driven by health checks and routing update speed
- Recovery time objective:
  how long degraded or unavailable service is acceptable
- Availability target:
  helps determine whether zone-level or region-level redundancy is justified

Interview shorthand:
- single point of failure, N+1, health checks, failover, independence

---

## 12. Failure Modes

### Common-mode failure

Problem:
- The redundant components share the same bug, config error, or dependency failure.

User impact:
- multiple copies fail together, so redundancy provides less protection than expected

Mitigation:
- isolate failure domains
- control rollout blast radius
- audit shared dependencies

### Insufficient spare capacity

Problem:
- One node fails and the remaining nodes cannot handle the load.

User impact:
- cascading overload after failover

Mitigation:
- capacity headroom
- load tests that include failure scenarios

### Broken standby path

Problem:
- The passive node or failover route has not been exercised recently.

User impact:
- delayed recovery during a real incident

Mitigation:
- regular drills
- health validation
- automation

### Hidden single point of failure

Problem:
- The visible service tier is redundant, but a shared database, config store, or certificate system is not.

User impact:
- full outage despite apparent redundancy

Mitigation:
- dependency mapping
- end-to-end failure review

---

## 13. Scenario

- Product / system: Payment API
- Requirement:
  the API should continue serving requests if one application server or one availability zone fails
- Good design:
  multiple stateless app instances across zones behind a load balancer, with redundant database and network paths
- Why redundancy fits:
  payment traffic cannot depend on one machine or one zone
- What would go wrong without it:
  normal infrastructure failures would become customer-facing outages

---

## 14. Code Sample

### Healthy-instance selection sketch

```java
public Instance selectInstance(List<Instance> instances) {
    return instances.stream()
        .filter(Instance::isHealthy)
        .findAny()
        .orElseThrow(() -> new NoHealthyInstanceException("No healthy capacity available"));
}
```

### Active-passive failover sketch

```java
public Endpoint currentEndpoint() {
    if (primaryEndpoint.isHealthy()) {
        return primaryEndpoint;
    }
    return standbyEndpoint;
}
```

Key idea:
- redundancy only helps when routing and health decisions actually use the spare capacity correctly

---

## 15. Mini Program / Simulation

This mini program shows traffic shifting away from a failed node.

```python
instances = [
    {"name": "app-a", "healthy": True},
    {"name": "app-b", "healthy": False},
    {"name": "app-c", "healthy": True},
]


def route_request() -> None:
    for instance in instances:
        if instance["healthy"]:
            print("Routed to:", instance["name"])
            return
    print("No healthy instance available")


def main() -> None:
    route_request()
    instances[0]["healthy"] = False
    route_request()


if __name__ == "__main__":
    main()
```

What this demonstrates:
- spare capacity matters only if health-aware routing exists
- redundancy is about surviving ordinary failure without full outage

---

## 16. Practical Question

> You are designing a login service that must stay available during server and zone failures. What redundancy strategy would you apply, and what hidden single points of failure would you check for?

---

## 17. Strong Answer

I would start by identifying every critical dependency in the login flow, not just the application servers. Then I would place multiple stateless service instances behind a load balancer across at least two availability zones. I would make sure the database, cache, networking, and secrets path are also not single-instance dependencies.

I would keep enough spare capacity so that losing one node or one zone does not immediately overload the surviving fleet. Depending on the system, I would choose active-active for the stateless service layer and a replication-based failover design for stateful dependencies.

I would also call out that redundancy must be tested. A standby path that has never been exercised is not a reliable design. So I would include health checks, failover drills, and dependency reviews for common-mode failures such as bad config pushes or shared credential issues.

---

## 18. Revision Notes

- One-line summary: Redundancy removes single points of failure by adding independent spare components and capacity.
- Three keywords: spare, failover, N+1
- One interview trap: thinking redundant app servers are enough while shared dependencies remain single points of failure
- One memory trick: the spare tire is useful only if it is real, reachable, and inflated

---

# Topic 3: Graceful Degradation

> Track: 1.8 Reliability & Fault Tolerance
> Scope: preserving core functionality, fallback behavior, optional dependencies, degraded modes, prioritization

---

## 1. Intuition

Think of graceful degradation like an airplane losing in-flight entertainment but still flying safely to the destination.

The ideal experience is reduced, but the core mission still works.

In systems, graceful degradation means that when dependencies fail or capacity gets tight, the product keeps its most important features alive while reducing or disabling less critical ones.

Short memory trick:
- total failure says "everything stops"
- graceful degradation says "the essentials still work"

---

## 2. Definition

- Definition: Graceful degradation is the design approach of preserving core service behavior during partial failure by reducing optional features, lowering quality, or switching to fallback behavior.
- Category: Reliability and user-experience resilience pattern
- Core idea: Not every dependency is equally critical, so not every failure should cause a total outage.

Examples of degradation:
- serve cached or stale data
- disable recommendations
- skip personalization
- lower media quality
- queue non-critical work for later

---

## 3. Why It Exists

Real systems depend on many components.

If every dependency is treated as mandatory, then one weak service can pull down the whole product. Graceful degradation exists because many user journeys have a smaller true critical path than the full rich experience.

It is used because systems need:
- resilience under partial failure
- better user experience during incidents
- overload survival
- protection from optional dependency outages

It reflects a mature design question:
- what absolutely must work, and what can temporarily become worse?

---

## 4. Reality

### Graceful degradation is common in:

- e-commerce sites that hide recommendations while checkout still works
- streaming apps that lower video quality under network stress
- search systems that fall back from personalized ranking to basic ranking
- dashboards that show cached data when a live analytics service is slow
- social feeds that load core posts before secondary enrichments

### Real-world architecture truth

Graceful degradation only works if product and engineering agree on priority.

The system needs explicit answers to:
- what is the core path?
- what is optional?
- what stale data is acceptable?
- what can be hidden or delayed safely?

Another important truth:
- graceful degradation is not a license to ignore root causes

It reduces user harm during failure, but it does not replace fixing the underlying dependency or capacity issue.

---

## 5. How It Works

The design usually follows this flow:

1. Identify critical user journeys.
2. Separate mandatory dependencies from optional ones.
3. Set timeouts so slow optional services do not block the core path.
4. Define fallback behavior for missing or slow dependencies.
5. Trigger degraded mode during incidents or overload.

### Typical fallback patterns

- serve stale cache
- return partial response
- disable secondary widgets
- queue background work for later
- reduce quality or freshness
- use a simpler algorithm

### Important design principle

Graceful degradation requires intentional product decisions.

If the fallback is undefined, the system often degrades chaotically instead of gracefully.

---

## 6. What Problem It Solves

- Primary problem solved: prevents partial failure from becoming total product failure
- Secondary benefits: better user experience during incidents, lower blast radius from optional dependency outages, improved overload handling
- Systems impact: shapes how the product behaves under stress, not just whether the service is technically up

Graceful degradation is often the difference between:
- "users can still complete the main task"
- and
- "the entire experience is broken"

---

## 7. When to Rely on It

Use graceful degradation when:
- the system has rich features with a smaller essential core
- optional dependencies can fail independently
- user-facing latency matters
- partial functionality is better than none
- overload events are realistic

Strong fits:
- e-commerce
- search
- media
- social products
- dashboards
- recommendation-driven products

---

## 8. When Not to Use It

Do not degrade blindly when:
- the failing component is safety-critical or correctness-critical
- returning stale or partial data would be misleading or dangerous
- the business contract requires strict completeness before success

Examples where caution matters:
- payment authorization
- medical data
- legal compliance flows
- security decisions

In those cases, a fast clear failure can be better than a degraded but incorrect result.

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Graceful degradation | Preserves core experience, reduces outage impact, handles optional dependency failure well | Adds product complexity, fallback logic, and risk of inconsistent UX if poorly designed |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Richness vs survivability:
  richer experiences often rely on more dependencies and create more degradation choices.
- Accuracy vs availability:
  stale or simplified responses may keep the product usable.
- Simplicity vs resilience:
  fallback logic improves survivability but adds branches to test and operate.
- User delight vs task completion:
  degrade what is nice to have before what is necessary.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Treating every dependency as critical | Optional-service failure becomes full outage | Define hard and soft dependencies explicitly |
| No timeout on secondary services | Slow optional calls block the core path | Use strict deadlines and fallback behavior |
| Serving dangerously stale or misleading data | Product may appear available but behave incorrectly | Set freshness and safety boundaries |
| Never testing degraded mode | Fallback logic can rot | Exercise degraded paths regularly |
| Hiding errors from operators | Degraded mode may mask real incidents | Alert on degraded behavior while protecting users |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Timeout budgets:
  optional dependencies often get shorter deadlines than the core path
- Staleness budget:
  how old fallback data may be before it becomes unacceptable
- Degraded-mode trigger:
  latency, error rate, dependency health, or overload thresholds
- Recovery threshold:
  when the system should restore full functionality
- Core-path success rate:
  the main metric that degradation should protect

Interview shorthand:
- critical path, optional dependency, fallback, stale data, partial response

---

## 12. Failure Modes

### Optional dependency blocks the core path

Problem:
- A recommendations or analytics service is slow, and the main request waits on it.

User impact:
- core user flow becomes slow or unavailable

Mitigation:
- strict timeouts
- fallback response
- asynchronous enrichment

### Degraded mode returns misleading data

Problem:
- Stale or partial data is shown without clear safety limits.

User impact:
- confusion, wrong decisions, possible business harm

Mitigation:
- freshness boundaries
- domain-specific safety rules
- clear product policy

### Degraded path overload

Problem:
- When the primary path fails, everyone falls back to one weaker dependency or cache path.

User impact:
- fallback path also collapses

Mitigation:
- capacity-plan degraded mode
- rate limit
- keep fallbacks lightweight

### Sticky degradation

Problem:
- The system enters degraded mode but does not recover cleanly after the incident ends.

User impact:
- product stays unnecessarily worse for too long

Mitigation:
- recovery thresholds
- feature toggles
- observability for mode transitions

---

## 13. Scenario

- Product / system: Online marketplace home page
- Requirement:
  users should still browse products and place orders even if personalization and recommendations are unhealthy
- Good design:
  serve core catalog content from cache or base ranking, skip recommendations, and protect checkout as the highest-priority path
- Why graceful degradation fits:
  the personalized experience is valuable, but not more valuable than core shopping
- What would go wrong without it:
  one optional service outage could make the whole site feel down

---

## 14. Code Sample

### Optional dependency fallback sketch

```java
public HomePageResponse buildHomePage(User user) {
    ProductFeed feed = catalogService.getCoreFeed();

    try {
        Recommendations recommendations = recommendationClient.get(user.id(), 80);
        return HomePageResponse.full(feed, recommendations);
    } catch (TimeoutException ex) {
        return HomePageResponse.degraded(feed);
    }
}
```

### Feature-gated degradation example

```java
public boolean shouldShowRecommendations(SystemHealth health) {
    return health.recommendationServiceHealthy() && !health.isTrafficEmergency();
}
```

Key idea:
- the user gets the essential experience even when the secondary dependency is unhealthy

---

## 15. Mini Program / Simulation

This mini program shows a core response surviving when an optional dependency fails.

```python
def get_catalog() -> list[str]:
    return ["item-1", "item-2", "item-3"]


def get_recommendations() -> list[str]:
    raise TimeoutError("recommendation service slow")


def main() -> None:
    catalog = get_catalog()

    try:
        recommendations = get_recommendations()
        print({"catalog": catalog, "recommendations": recommendations})
    except TimeoutError:
        print({"catalog": catalog, "recommendations": [], "mode": "degraded"})


if __name__ == "__main__":
    main()
```

What this demonstrates:
- the system can still serve the essential response
- optional dependency failure does not have to become total failure

---

## 16. Practical Question

> You are designing a search page with spelling suggestions, personalization, ads, analytics, and the main search results. How would you decide what degrades first when dependencies are slow or failing?

---

## 17. Strong Answer

I would start by identifying the true critical path for the user. In a search page, the core requirement is returning reasonably good search results quickly. Features like personalization, suggestions, ads, or secondary analytics are important, but they should not block the main result path.

So I would give the main search service the highest priority and make secondary dependencies time-bounded. If those dependencies fail or exceed latency budgets, I would fall back to simpler ranking, cached suggestions, or no suggestions at all. I would also make sure degraded mode is observable so operators know the system is protecting users rather than silently masking a problem.

The key idea is to degrade the experience in a deliberate order: preserve correctness and core utility first, then sacrifice non-essential enrichments before allowing the whole page to fail.

---

## 18. Revision Notes

- One-line summary: Graceful degradation keeps the core user journey working by reducing optional functionality during failure or overload.
- Three keywords: fallback, optional, core-path
- One interview trap: treating every dependency as mandatory and causing avoidable full outages
- One memory trick: lose the extras before you lose the mission

---

# Topic 4: Retries with Exponential Backoff

> Track: 1.8 Reliability & Fault Tolerance
> Scope: transient failures, retry policy, jitter, retry storms, idempotency

---

## 1. Intuition

Think of calling someone whose phone is temporarily busy.

If you redial instantly over and over, you make the situation worse. If you wait a little longer after each failed attempt, you give the other side time to recover and reduce pressure on the system.

That is exponential backoff:
- first retry after a short wait
- next retry after a longer wait
- then longer again, usually up to a cap

Short memory trick:
- retry, but calm down each time

---

## 2. Definition

- Definition: Retries with exponential backoff are a failure-handling strategy in which a client retries transient failures with increasing delays between attempts.
- Category: Reliability and overload-protection mechanism
- Core idea: transient failures often recover shortly, but immediate repeated retries can amplify outage and overload.

Important additions:
- retries should usually be bounded
- retries often need jitter
- retries are safest when the operation is idempotent

---

## 3. Why It Exists

Many failures are temporary.

Examples:
- a brief network hiccup
- a dependency restart
- a short overload window
- a transient timeout
- a `429` or `503` response

Without retries, a lot of recoverable operations would fail unnecessarily.

But without backoff, retries become dangerous because clients can:
- hammer the dependency
- synchronize into waves
- amplify latency
- turn partial failure into full collapse

This topic exists because retries are both useful and dangerous.

---

## 4. Reality

### Retries with exponential backoff are common in:

- HTTP clients
- queue consumers
- service-to-service RPC calls
- database connection attempts
- cloud SDKs
- third-party API integrations

### Real-world architecture truth

Retries improve reliability only when they are selective.

They are a bad fit for:
- validation errors
- permanent business-rule failures
- unsafe non-idempotent operations without protection

Another important truth:
- retries are part of a policy, not a magic fix

A solid retry policy usually includes:
- retryable status classification
- deadlines
- max attempts
- exponential backoff
- jitter
- observability

---

## 5. How It Works

The standard flow is:

1. Attempt the operation.
2. If it fails with a retryable error, wait for a delay.
3. Increase the delay, usually by multiplying by 2.
4. Try again until success or attempt limit is reached.

### Example delay pattern

- attempt 1 fails
- wait 100 ms
- attempt 2 fails
- wait 200 ms
- attempt 3 fails
- wait 400 ms
- attempt 4 succeeds

### Why jitter matters

If thousands of clients all retry on the same schedule, they can create synchronized spikes.

Jitter adds randomness so retries spread out instead of arriving in a herd.

### Important design rule

Retry only when the error is plausibly transient.

Do not retry:
- bad input
- authorization failure
- deterministic business rejection

---

## 6. What Problem It Solves

- Primary problem solved: recovers from transient failures without instantly hammering the dependency
- Secondary benefits: better success rate, smoother recovery during short incidents, less synchronized retry pressure
- Systems impact: strongly affects incident behavior, latency, and downstream stability

Retries with exponential backoff help systems recover from short-lived problems while avoiding self-inflicted overload.

---

## 7. When to Rely on It

Use retries with exponential backoff when:
- the failure is likely transient
- the dependency explicitly signals retryable overload or temporary unavailability
- the operation is idempotent or protected by an idempotency key
- end-to-end latency budget can tolerate extra delay

Strong fits:
- background jobs
- webhook delivery
- message processing
- service-to-service calls with temporary failures
- rate-limited APIs

---

## 8. When Not to Use It

Avoid retries when:
- the error is permanent
- the operation is unsafe to repeat
- the caller is already past its deadline
- the dependency is deeply unhealthy and retries would only amplify pressure

Examples:
- malformed request payload
- invalid credentials
- insufficient funds
- non-idempotent charge submission without safeguards

In those cases, fail fast or use a different recovery mechanism.

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Retries with exponential backoff | Recovers many transient failures, smooths retry pressure, easy to apply broadly | Can increase latency, duplicate side effects, or trigger retry storms if misused |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Reliability vs latency:
  retries improve success probability but consume time.
- Simplicity vs correctness:
  naive retries are easy, safe retries need idempotency and classification.
- Recovery vs overload:
  retries can help a recovering system or crush it if too aggressive.
- Completeness vs deadlines:
  more attempts may exceed user or workflow latency budgets.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Retrying every failure | Permanent failures do not improve with repetition | Retry only classified transient errors |
| Using fixed delay without jitter | Clients synchronize and create waves of pressure | Add exponential backoff with jitter |
| Retrying non-idempotent work unsafely | Can duplicate side effects | Use idempotency keys or avoid retry |
| Unlimited retries | Work piles up forever and latency explodes | Set max attempts and retry budgets |
| Ignoring total deadline | Each layer may retry until the user times out | Use end-to-end time budgets |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Initial backoff:
  often small, such as tens or hundreds of milliseconds for online calls
- Backoff multiplier:
  commonly 2x
- Max backoff:
  cap delays so retries do not grow forever
- Max attempts:
  usually a small bounded number
- Jitter:
  strongly recommended to avoid herd behavior
- End-to-end deadline:
  total retry time should fit within the workflow's latency budget

Interview shorthand:
- retryable errors, capped exponential backoff, jitter, idempotency, deadlines

---

## 12. Failure Modes

### Retry storm

Problem:
- Many clients all retry an unhealthy dependency at once.

User impact:
- outage becomes worse and lasts longer

Mitigation:
- backoff
- jitter
- retry budgets
- circuit breakers

### Duplicate side effects

Problem:
- A request succeeds remotely, but the client times out and retries.

User impact:
- duplicate emails, duplicate orders, duplicate charges

Mitigation:
- idempotency keys
- idempotent consumers
- operation design

### Latency blow-up

Problem:
- Multiple retries make a single request much slower than the user can tolerate.

User impact:
- slow responses, timeout chains, poor UX

Mitigation:
- deadline-aware retries
- bounded attempts
- separate sync and async retry policies

### Retry on permanent failure

Problem:
- The client keeps retrying errors that will never succeed.

User impact:
- wasted capacity and delayed failure handling

Mitigation:
- classify retryable vs non-retryable errors correctly

---

## 13. Scenario

- Product / system: Checkout service calling a third-party payment gateway
- Requirement:
  temporary gateway timeouts should not immediately fail customer checkout
- Good design:
  retry transient gateway errors with capped exponential backoff and idempotency keys
- Why it fits:
  payment gateways do have short-lived timeouts and overload windows
- What would go wrong without discipline:
  aggressive retries could multiply pressure or duplicate charges

---

## 14. Code Sample

### Retry with backoff sketch

```java
public PaymentResult charge(PaymentRequest request) {
    long delayMs = 100;

    for (int attempt = 1; attempt <= 4; attempt++) {
        try {
            return paymentClient.charge(request.withIdempotencyKey());
        } catch (RetryableException ex) {
            if (attempt == 4) {
                throw ex;
            }

            sleepWithJitter(delayMs);
            delayMs = Math.min(delayMs * 2, 2000);
        }
    }

    throw new IllegalStateException("unreachable");
}
```

### Retryability classification sketch

```java
public boolean shouldRetry(Response response) {
    return response.statusCode() == 429 || response.statusCode() == 503;
}
```

Key idea:
- retries should be bounded, jittered, and safe to repeat

---

## 15. Mini Program / Simulation

This mini program shows a transient failure recovering after exponential backoff.

```python
attempts = 0


def flaky_call() -> str:
    global attempts
    attempts += 1
    if attempts < 4:
        raise TimeoutError("temporary failure")
    return "success"


def main() -> None:
    delay = 1

    for attempt in range(1, 6):
        try:
            print("Attempt", attempt, "->", flaky_call())
            return
        except TimeoutError:
            print("Attempt", attempt, "failed; waiting", delay, "units")
            delay *= 2


if __name__ == "__main__":
    main()
```

What this demonstrates:
- transient failures may recover
- waiting longer between retries reduces pressure
- retries need bounds and pacing

---

## 16. Practical Question

> You are integrating with a third-party email provider that sometimes returns `429 Too Many Requests` and brief timeouts. How would you design retries so delivery improves without causing a retry storm?

---

## 17. Strong Answer

I would retry only clearly transient failures such as timeouts, `429`, or temporary `5xx` responses. The retries would be bounded, use exponential backoff, and include jitter so clients do not all retry in lockstep.

I would also make sure the operation is safe to retry. For an external provider, that usually means using an idempotency key or deduplication identifier if supported. If the end-to-end request is user-facing, I would keep a strict total deadline so retries do not quietly turn a fast failure into a very slow failure. For background delivery, I can be a bit more patient, but I would still cap attempts and eventually move persistent failures to a dead-letter or operator-review path.

The main design principle is that retries should help the dependency recover, not become the reason it stays overloaded.

---

## 18. Revision Notes

- One-line summary: Retries with exponential backoff improve success for transient failures by spacing attempts farther apart each time.
- Three keywords: transient, jitter, idempotency
- One interview trap: retrying every failure, including permanent or unsafe ones
- One memory trick: retry, but breathe longer each time
