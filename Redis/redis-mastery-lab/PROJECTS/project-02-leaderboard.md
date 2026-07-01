# Project 02: Leaderboard Service

## Objective

Build a multi-period leaderboard with real-time score updates, ranking, and nearby-rank windows.

## Requirements

- Add or update player score (ZINCRBY)
- Get top-N players with ranks and scores
- Get player's current rank and score
- Nearby-rank window: 5 above, 5 below a given player
- Support all-time, weekly, and daily leaderboards
- Weekly and daily keys auto-expire via TTL

## Key Redis Patterns Used

- Sorted set: `ZADD`, `ZINCRBY`, `ZREVRANGE`, `ZREVRANK`, `ZSCORE`, `ZCARD`
- TTL: weekly keys expire after 14 days, daily after 2 days
- Key pattern: `leaderboard:{game}:{period}` where period = alltime | weekly:YYYY-Www | daily:YYYY-MM-DD

## Implementation Notes

When updating score, use `ZINCRBY` not `ZADD` to add delta to existing score.

For nearby rank: `ZREVRANK` to get rank, then `ZREVRANGE (rank-5) to (rank+5)`.

Weekly key creation: check if key exists; if not, set TTL on first ZADD.

## Test Scenarios

1. Add 10 players. Verify ZREVRANGE returns them in correct descending order.
2. ZINCRBY player to overtake another. Verify rank changes.
3. Fetch nearby-rank window for a player in the middle. Verify correct slice.
4. Verify weekly key TTL is set on creation.

## Interview Value

Demonstrates: sorted-set leaderboard design, multi-period key strategy, TTL management.
