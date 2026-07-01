# Cache-Aside Pattern: Key Naming Examples

## Key Naming Conventions

```text
# Pattern: {entity_type}:{id}
user:1001
product:catalog:5001
session:abc123xyz
config:global:feature-flags

# Pattern: {prefix}:{entity}:{id}:{attribute}
cache:user:1001:profile
cache:product:5001:details
cache:search:hash-of-query

# Pattern: {domain}:{sub-domain}:{id}
orders:status:5001
payments:pending:user:1001
inventory:item:5001:count
```

---

## Cache-Aside Command Sequence

```bash
# Read: check cache first.
GET cache:product:5001

# On miss: set from source with TTL.
SET cache:product:5001 '{"name":"widget","price":1999}' EX 300

# Write: update source, then invalidate cache (not update).
DEL cache:product:5001
# Application DB write happens separately.

# Write-through alternative: update both.
SET cache:product:5001 '{"name":"widget-v2","price":2199}' EX 300
# Application DB write happens separately.
```

---

## TTL Strategy Examples

```bash
# User session: 30-minute idle timeout.
SET session:abc123 '{"user_id":1001}' EX 1800

# Product catalog: 5-minute TTL (tolerate slight staleness).
SET cache:product:5001 '{"name":"widget"}' EX 300

# Config/feature flags: 60-second TTL.
SET cache:config:feature-flags '{"dark_mode":true}' EX 60

# Rate limit window key: match window.
SET rate:user:1001:requests:202607011000 0 EX 60

# Idempotency key: match request processing window.
SET idempotency:order:req-uuid-abc123 "processed" EX 86400
```

---

## Namespace Isolation With Prefixes

```bash
# Good: prefixed by service and entity type.
SET payments:cache:invoice:5001 '...' EX 300
SET auth:session:abc123 '...' EX 1800
SET catalog:product:5001 '...' EX 300

# Bad: no prefix (collisions across services).
SET product:5001 '...'   # which service owns this?
SET session:abc123 '...' # which app?
```
