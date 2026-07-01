# Leaderboard Patterns With Sorted Sets

## Basic Leaderboard Operations

```bash
# Initialize leaderboard.
DEL leaderboard:game:weekly

# Add/update players with scores.
ZADD leaderboard:game:weekly 1500 player:alice
ZADD leaderboard:game:weekly 2200 player:bob
ZADD leaderboard:game:weekly 980  player:carol
ZADD leaderboard:game:weekly 1800 player:dave

# Top 10 players (descending, with scores).
ZREVRANGE leaderboard:game:weekly 0 9 WITHSCORES

# Player rank (0-based from top).
ZREVRANK leaderboard:game:weekly player:alice
# Result: 2 (third place)

# Player score.
ZSCORE leaderboard:game:weekly player:alice
# Result: "1500"

# Players in score range 1000-2000.
ZRANGEBYSCORE leaderboard:game:weekly 1000 2000 WITHSCORES

# Total players.
ZCARD leaderboard:game:weekly
```

---

## Incrementing Scores

```bash
# Add points to player (upsert: creates if not exists).
ZINCRBY leaderboard:game:weekly 300 player:carol

# Atomic check: only update if score would be higher (ZADD GT).
ZADD leaderboard:game:weekly GT 2500 player:bob
```

---

## Multi-Period Leaderboards

```bash
# All-time and weekly use separate keys.
ZADD leaderboard:game:alltime 1500 player:alice
ZADD leaderboard:game:weekly:2026-W27 1500 player:alice

# Weekly key with automatic TTL (expires after 14 days).
EXPIRE leaderboard:game:weekly:2026-W27 1209600

# Get player's global rank and weekly rank.
ZREVRANK leaderboard:game:alltime player:alice
ZREVRANK leaderboard:game:weekly:2026-W27 player:alice
```

---

## Nearby Rank Window

```bash
# Get player alice's rank.
rank = ZREVRANK leaderboard:game:weekly player:alice

# Get 5 players above and below.
start = MAX(0, rank - 5)
end = rank + 5
ZREVRANGE leaderboard:game:weekly start end WITHSCORES
```

---

## Pagination

```bash
# Page 1: positions 0-9.
ZREVRANGE leaderboard:game:weekly 0 9 WITHSCORES

# Page 2: positions 10-19.
ZREVRANGE leaderboard:game:weekly 10 19 WITHSCORES
```

---

## Leaderboard Key Naming

```text
leaderboard:{game}:alltime
leaderboard:{game}:weekly:{year}-W{week}
leaderboard:{game}:daily:{yyyy-mm-dd}
leaderboard:{region}:{game}:{period}
```
