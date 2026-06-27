# Backend Build Tools — Mastery Roadmap — Gold Sheet

> Two sprint plans: 7-day rapid prep and 14-day deep mastery

---

## Pre-Start: Baseline Assessment

Before starting either sprint, run a baseline scoring session using the Scoring Rubrics file. Record your score for all 10 topics. This takes 20-30 minutes and tells you exactly where to invest time.

---

## 7-Day Rapid Preparation Sprint

> Goal: Reach Average ≥ 3.5 on all 10 topics. No topic below 2.5.
> Time budget: 90 minutes/day

---

### Day 1 — Java Build Foundation (90 min)

| Time | Activity |
|---|---|
| 0-20 min | Read: `Maven-Lifecycle-Dependencies-Snapshots-Gold-Sheet.md` — focus on lifecycle phases, dependency scopes, `<dependencyManagement>` |
| 20-40 min | Read: `Java-Artifacts-Repositories-Snapshots-Releases-Gold-Sheet.md` — SNAPSHOT vs RELEASE, publishing workflow |
| 40-60 min | Active Recall: Q1, Q2, Q3 from Active Recall Question Bank |
| 60-80 min | Write from memory: a parent POM with `<dependencyManagement>` importing Spring Boot BOM + a child module POM |
| 80-90 min | Review: fix any errors; note gaps |

**Checkpoints:** Can you name Maven lifecycle phases in order? Can you explain BOM imports?

---

### Day 2 — Spring Boot & JVM (90 min)

| Time | Activity |
|---|---|
| 0-25 min | Read: `Spring-Boot-Configuration-Profiles-Actuator-Gold-Sheet.md` — property priority, profiles, actuator endpoints |
| 25-50 min | Read: `JVM-Runtime-Tuning-Heap-GC-Threads-Diagnostics-Gold-Sheet.md` — container memory sizing, GC selection |
| 50-70 min | Active Recall: Q6, Q7, Q8, Q9, Q10 from Active Recall Question Bank |
| 70-80 min | Tricky Scenarios: SB-1, SB-2 |
| 80-90 min | Write from memory: production JVM flags set (`MaxRAMPercentage + MaxMetaspace + GC + ExitOnOOM`) |

**Checkpoints:** Can you explain liveness vs readiness? Can you diagnose the OOM-killed pod scenario?

---

### Day 3 — Testing (Java + Python) (90 min)

| Time | Activity |
|---|---|
| 0-25 min | Read: `Java-Testing-JUnit5-Mockito-Testcontainers-Slices-Gold-Sheet.md` — focus on test slices, context cache |
| 25-45 min | Read: `Python-Testing-Pytest-Coverage-Testcontainers-Gold-Sheet.md` — fixtures, async testing, patch rule |
| 45-65 min | Active Recall: Q8, Q18, Q19, Q20 from Active Recall Question Bank |
| 65-80 min | Tricky Scenarios: P-1 (patch at usage site) |
| 80-90 min | Write from memory: `@WebMvcTest` test with `MockMvc` + `@MockBean` |

**Checkpoints:** Can you explain why `@MockBean` slows builds? Can you state the patch-at-usage-site rule?

---

### Day 4 — Docker & Containers (90 min)

| Time | Activity |
|---|---|
| 0-30 min | Read: `Docker-BuildKit-Image-Scanning-Distroless-Gold-Sheet.md` — multi-stage, layer caching, secrets |
| 30-50 min | Active Recall: Q11, Q12, Q13, Q14 from Active Recall Question Bank |
| 50-70 min | Tricky Scenarios: D-1, D-2 |
| 70-80 min | Write from memory: production Spring Boot Dockerfile (multi-stage, non-root, BuildKit) |
| 80-90 min | Write from memory: Trivy scan command with CI fail-gate |

**Checkpoints:** Can you write a Dockerfile from memory? Can you explain why `COPY . .` breaks caching?

---

### Day 5 — Database Migrations + Quality (90 min)

| Time | Activity |
|---|---|
| 0-25 min | Read: `Database-Migrations-Flyway-Liquibase-Gold-Sheet.md` — naming, expand-contract, CI integration |
| 25-45 min | Read: `Sonar-JaCoCo-Test-Reports-Quality-Gates-Gold-Sheet.md` — JaCoCo phases, coverage ordering, quality gate |
| 45-65 min | Active Recall: Q15, Q16, Q17 from Active Recall Question Bank |
| 65-80 min | Tricky Scenarios: F-1 (rolling deploy rename) |
| 80-90 min | Case Study: Study Case 2 (Flyway fails on deploy) + Case 4 (Sonar 0% coverage) |

**Checkpoints:** Can you write the expand-contract migration steps from memory? Can you explain the Sonar phase order?

---

### Day 6 — Node.js + Gradle (90 min)

| Time | Activity |
|---|---|
| 0-20 min | Read: `NodeJS-Package-Managers-npm-yarn-pnpm-Gold-Sheet.md` — npm ci, scopes, NODE_ENV |
| 20-40 min | Read: `Gradle-Task-DAG-Caching-Optimization-Gold-Sheet.md` — task DAG, build cache, incremental |
| 40-60 min | Active Recall: Q4, Q5, Q21, Q22 from Active Recall Question Bank |
| 60-75 min | Tricky Scenarios: N-1 (runtime lib in devDeps), J-2 (Gradle task no inputs) |
| 75-90 min | Case Study: Study Case 3 (CI slow build) |

**Checkpoints:** Can you explain why `npm ci` is used in CI? Can you explain why a Gradle task never caches?

---

### Day 7 — Mock Interview + Gaps (90 min)

| Time | Activity |
|---|---|
| 0-60 min | Run Round 1 Mock Interview (all 8 questions, timed 2 min each, self-grade) |
| 60-75 min | Score with Rubrics file; identify any topic below 3.0 |
| 75-90 min | Re-read lowest-scoring topic's gold sheet; do active recall for that topic only |

**Final checkpoint:** Are you ≥ 3.5 average? Any topic below 2.5 still? If yes → start 14-day plan.

---

## 14-Day Deep Mastery Sprint

> Goal: Average ≥ 4.0. All topics ≥ 3.5. Confident with production incident simulation.
> Time budget: 90-120 minutes/day

---

### Week 1: Foundation + Depth (Days 1-7)

Follow the 7-Day Sprint plan above to establish baseline comprehension across all topics.

**Week 1 exit criteria:**
- Scored all topics at ≥ 3.0 after Day 7
- Can write a production Dockerfile and Spring Boot config from memory
- Can explain 5 of the tricky scenarios without notes

---

### Week 2: Production Depth + Interview Simulation

---

**Day 8 — Production Case Studies (90 min)**

| Time | Activity |
|---|---|
| 0-45 min | Read all 5 case studies in `BackendBuildTools-Production-Debugging-Case-Studies-Gold-Sheet.md` |
| 45-75 min | Cover answers and narrate each case study aloud as if presenting to a panel |
| 75-90 min | Add your own scenario: think of a build failure you've personally experienced and map it to the diagnostic framework |

---

**Day 9 — Deep Dive: Gradle & Build Optimization (90 min)**

| Time | Activity |
|---|---|
| 0-30 min | Read Gradle gold sheet focusing on: version catalogs (`libs.versions.toml`), configuration cache, build scans |
| 30-60 min | Mock deep-dive: Round 2, Q2 (Gradle build cache) — answer fully timed |
| 60-90 min | Research: look up one Gradle version catalog real-world example; write your own `libs.versions.toml` template |

---

**Day 10 — Deep Dive: JVM + Observability (90 min)**

| Time | Activity |
|---|---|
| 0-30 min | Re-read JVM tuning file focusing on: GC deep dive (G1GC vs ZGC), thread pool sizing, jstack/jmap commands |
| 30-50 min | Active Recall: Q10 (ZGC vs G1GC) — answer timed 5 min |
| 50-70 min | Write from memory: Prometheus/Actuator config + K8s probe yaml + custom `Timer` metric for payments service |
| 70-90 min | Mock: Round 2, Q5 (Actuator + Prometheus) — answer timed 5 min |

---

**Day 11 — Deep Dive: Supply Chain Security (90 min)**

| Time | Activity |
|---|---|
| 0-30 min | Read: `Dependency-Management-Reproducibility-Supply-Chain-Gold-Sheet.md` — SBOM, dependency confusion, Renovate |
| 30-50 min | Active Recall: Q23, Q24 (dependency confusion, SBOM) |
| 50-70 min | Mock: Round 2, Q3 (dependency confusion) — answer timed 5 min |
| 70-90 min | Write from memory: `.npmrc` scoped registry config + Docker Trivy scan command |

---

**Day 12 — Full Round 2 Mock (90 min)**

| Time | Activity |
|---|---|
| 0-45 min | Run Round 2 Mock Interview (Q1-Q5, timed 5 min each, all answers stated aloud) |
| 45-60 min | Score each answer with Rubrics file |
| 60-90 min | For any question scored ≤ 3: re-read relevant gold sheet, narrate answer again |

---

**Day 13 — Incident Simulation (90 min)**

| Time | Activity |
|---|---|
| 0-30 min | Run Round 3, Incident 1 (Friday Deploy Broke DB) — narrate full triage aloud, 15 min |
| 30-60 min | Run Round 3, Incident 2 (Slow Build Broke Sprint) — narrate full triage aloud, 15 min |
| 60-90 min | Invent a new incident: pick a tool from the track, design a production failure scenario, narrate root cause + fix |

---

**Day 14 — Final Assessment (90 min)**

| Time | Activity |
|---|---|
| 0-50 min | Run Round 1 Mock Interview cold (no notes, timed). Self-score all 8 questions |
| 50-70 min | Score with Rubrics; compare to Day 7 baseline. Calculate improvement |
| 70-80 min | Record any remaining gaps for targeted post-sprint study |
| 80-90 min | Calibrate readiness: Average ≥ 4.0 and no topic below 3.5 → Interview ready |

---

## Topic Priority by Frequency in Interviews

Based on backend engineering interviews at MAANG, fintech, and healthtech companies:

| Priority | Topic | Why high frequency |
|---|---|---|
| P1 (Most common) | Spring Boot Config | Every Spring shop asks about profiles, externalized config |
| P1 | Docker / Multi-stage | Universal; asked in virtually every backend round |
| P1 | JVM Memory (OOM) | Classic interview trap; easy to demonstrate depth |
| P2 | Maven/Gradle basics | Expected knowledge; asked as warm-up |
| P2 | Java Testing (slices) | Quality-focused companies ask how you test efficiently |
| P2 | Database Migrations | Any service with persistence; expand-contract is known pattern |
| P3 | Node.js builds | Only when job includes Node services |
| P3 | Python testing | Only when job includes Python services |
| P3 | Sonar/JaCoCo | More commonly asked at DevOps-forward companies |
| P3 | Supply Chain | Security-focused roles; increasing frequency post-log4shell |

---

## Daily Habit During Sprint

**Morning (5 min):** Read one set of Revision Notes from any gold sheet.
**During study:** Say answers aloud — hearing yourself articulate is more effective than re-reading.
**Evening (5 min):** Write the three keywords from the topics covered today.

---

## Revision Notes

- One-line summary: 7-day rapid (1.5h/day) targets 3.5+ average; 14-day deep (same pace) targets 4.0+ with incident fluency.
- Priority topics: Spring Boot Config, Docker multi-stage, JVM OOM — P1 for virtually all backend interviews.
- One interview trap: Knowledge without structure is not enough — practice structuring answers as definition → example → trade-off every time.
