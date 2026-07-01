# Redis Mastery Lab

A hands-on lab environment for the Redis Mastery track.

## Structure

```
redis-mastery-lab/
├── README.md                    (this file)
├── LEARNING_PATH.md             (guided learning path with lab sequence)
├── EXAMPLES/
│   ├── cache-aside/             (cache-aside key pattern examples)
│   ├── rate-limiter/            (Lua rate limiter script examples)
│   └── leaderboard/             (sorted set leaderboard examples)
├── SCRIPTS/
│   ├── 01-redis-info-snapshot.sh
│   ├── 02-memory-usage-scan.sh
│   ├── 03-slowlog-report.sh
│   ├── 04-keyspace-inventory.sh
│   ├── 05-rate-limiter-template.sh
│   └── 06-incident-evidence-template.sh
├── LABS/
│   ├── lab-01-strings-ttl-basics.md
│   ├── lab-02-collection-types.md
│   ├── lab-03-cache-patterns.md
│   ├── lab-04-pubsub-streams.md
│   ├── lab-05-transactions-lua.md
│   ├── lab-06-persistence-config.md
│   ├── lab-07-replication-setup.md
│   ├── lab-08-security-acl.md
│   ├── lab-09-observability.md
│   └── lab-10-advanced-patterns.md
├── PROJECTS/
│   ├── project-01-rate-limiter.md
│   ├── project-02-leaderboard.md
│   ├── project-03-session-store.md
│   ├── project-04-job-queue.md
│   └── project-05-event-feed.md
├── CHEATSHEETS/
│   ├── cs-01-data-structure-commands.md
│   ├── cs-02-expiry-memory-commands.md
│   ├── cs-03-streams-commands.md
│   ├── cs-04-admin-observability-commands.md
│   ├── cs-05-cluster-sentinel-commands.md
│   └── cs-06-security-acl-commands.md
├── INTERVIEW_PREP/
│   ├── ip-01-beginner-questions.md
│   ├── ip-02-intermediate-questions.md
│   ├── ip-03-senior-questions.md
│   ├── ip-04-system-design-questions.md
│   └── ip-05-anti-patterns-traps.md
└── RUNBOOKS/
    ├── rb-01-health-check.md
    ├── rb-02-oom-eviction.md
    ├── rb-03-sentinel-failover.md
    ├── rb-04-replica-resync.md
    ├── rb-05-slowlog-investigation.md
    ├── rb-06-connection-exhaustion.md
    ├── rb-07-cluster-rebalance.md
    └── rb-08-acl-key-rotation.md
```

## Prerequisites

- Redis 7.x installed locally or Docker: `docker run -p 6379:6379 redis:7-alpine`
- redis-cli available on PATH
- bash or zsh shell for scripts

## Quick Start

```bash
# Verify Redis is running.
redis-cli PING
# Expected: PONG

# Run first lab.
cat LABS/lab-01-strings-ttl-basics.md

# Run health check script.
./SCRIPTS/01-redis-info-snapshot.sh
```
