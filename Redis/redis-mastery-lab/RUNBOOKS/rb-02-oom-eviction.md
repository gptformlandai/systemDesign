# Runbook 02: OOM And High Eviction

## When To Use

- Application returns OOM errors
- `evicted_keys` is growing
- `used_memory` is at or near `maxmemory`

## Steps

1. Confirm OOM condition.

```bash
redis-cli INFO memory | grep -E "used_memory_human|maxmemory_human|maxmemory_policy"
redis-cli INFO stats | grep evicted_keys
```

2. Check eviction policy.

```bash
redis-cli CONFIG GET maxmemory-policy
# If noeviction: change it immediately for a cache.
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

3. Find largest keys.

```bash
redis-cli --bigkeys
```

4. Scan for keys without TTL.

```bash
# Sample 1000 keys and count no-TTL.
CURSOR=0
NO_TTL=0
for i in $(seq 1 10); do
  RESULT=$(redis-cli SCAN "$CURSOR" COUNT 100)
  CURSOR=$(echo "$RESULT" | awk 'NR==1')
  while read KEY; do
    [ -z "$KEY" ] && continue
    TTL=$(redis-cli TTL "$KEY")
    [ "$TTL" = "-1" ] && NO_TTL=$((NO_TTL+1))
  done <<< "$(echo "$RESULT" | awk 'NR>1')"
  [ "$CURSOR" = "0" ] && break
done
echo "Keys without TTL in sample: $NO_TTL"
```

5. Check fragmentation.

```bash
redis-cli INFO memory | grep mem_fragmentation_ratio
# If > 1.5: enable active defrag.
redis-cli CONFIG SET activedefrag yes
```

6. Increase maxmemory if legitimate growth.

```bash
redis-cli CONFIG SET maxmemory 8gb
redis-cli CONFIG REWRITE
```

## Resolution Criteria

- evicted_keys stops growing
- used_memory below 80% of maxmemory
- All cache keys have TTL
