# Redis Mastery Lab: Learning Path

## How To Use This Lab

Follow this sequence. Each item builds on the previous. Mark items complete as you go.

---

## Phase 1: Foundation (Days 1-3)

- [ ] Read: Sheet 01 — Mental Model and Data Structure Server
- [ ] Read: Sheet 02 — Setup, CLI, Config, AUTH
- [ ] Lab: lab-01-strings-ttl-basics.md
- [ ] Drill: All Drill 1 and Drill 2 items from Sheet 28
- [ ] Read: Sheet 03 — Strings, Numbers, TTLs
- [ ] Read: Sheet 04 — Collection Types
- [ ] Lab: lab-02-collection-types.md
- [ ] Drill: Drills 3, 4, 5 from Sheet 28

---

## Phase 2: Intermediate (Days 4-7)

- [ ] Read: Sheet 05 — Redis As Cache, Eviction, Patterns
- [ ] Lab: lab-03-cache-patterns.md
- [ ] Read: Sheet 06 — Pub/Sub
- [ ] Read: Sheet 07 — Streams
- [ ] Lab: lab-04-pubsub-streams.md
- [ ] Drill: Drill 7 and Drill 11 from Sheet 28
- [ ] Read: Sheet 08 — Transactions, MULTI/EXEC, WATCH
- [ ] Read: Sheet 09 — Lua Scripting
- [ ] Lab: lab-05-transactions-lua.md
- [ ] Drill: Drills 9 and 10 from Sheet 28
- [ ] Read: Sheet 10 — Persistence
- [ ] Lab: lab-06-persistence-config.md

---

## Phase 3: Senior Production (Days 8-12)

- [ ] Read: Sheet 11 — Replication
- [ ] Lab: lab-07-replication-setup.md
- [ ] Read: Sheet 12 — Sentinel
- [ ] Read: Sheet 13 — Cluster
- [ ] Read: Sheet 14 — Security, ACL, TLS
- [ ] Lab: lab-08-security-acl.md
- [ ] Read: Sheet 15 — Observability
- [ ] Lab: lab-09-observability.md
- [ ] Run: SCRIPTS/01 through SCRIPTS/04
- [ ] Read: Sheet 16 — Advanced Patterns
- [ ] Lab: lab-10-advanced-patterns.md
- [ ] Drill: Drills 6, 7, 8 from Sheet 28

---

## Phase 4: Scenario And Interview (Days 13-17)

- [ ] Read: Sheets 17-23 (all scenario sheets)
- [ ] Read: Sheet 24 — Interview Q&A
- [ ] Practice: Answer all Round 1-4 questions from Sheet 27 without looking
- [ ] Read: Sheet 25 — Commands and Decision Cheatsheet
- [ ] Read: Sheet 26 — Anti-Patterns and Debugging Traps
- [ ] Review: INTERVIEW_PREP/ directory — all 5 IP files

---

## Phase 5: Projects And Mastery (Days 18-25)

- [ ] Build: project-01-rate-limiter.md
- [ ] Build: project-02-leaderboard.md
- [ ] Build: project-03-session-store.md
- [ ] Build: project-04-job-queue.md
- [ ] Build: project-05-event-feed.md
- [ ] Review: Sheet 30 — Production Readiness Checklist
- [ ] Run: SCRIPTS/06-incident-evidence-template.sh on local Redis
- [ ] Simulate: Sentinel failover using RUNBOOKS/rb-03-sentinel-failover.md

---

## Readiness Gate

You are interview-ready when you can:

- Name the right data structure for any cache, queue, leaderboard, stream, or rate-limit problem without hesitation
- Diagnose a Redis memory issue using INFO output alone
- Explain Sentinel failover timeline from SDOWN to client reconnect
- Explain why Redlock has known failure modes and name the fencing token pattern
- Implement a sliding-window rate limiter Lua script from memory
- Debug a production latency spike using SLOWLOG and commandstats
