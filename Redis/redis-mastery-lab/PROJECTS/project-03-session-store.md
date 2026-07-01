# Project 03: Session Store With Rolling TTL

## Objective

Build a session management module that stores user sessions as Redis hashes with rolling idle TTL.

## Requirements

- Create session on login: generate UUID token, store HSET user_id, email, roles, created_at, last_active
- Validate session: read all fields, refresh TTL (rolling 30-minute idle timeout)
- Invalidate session on logout: DEL the session key
- List active sessions for a user (optional stretch goal)

## Key Redis Patterns Used

- Hash: `HSET`, `HGETALL`, `EXPIRE`
- Key pattern: `session:{uuid}`
- Optional: set `sessions:user:{userId}` for reverse lookup

## Implementation Notes

Rolling TTL: every `validate_session` call must call `EXPIRE session:{uuid} 1800` to extend the TTL.

If HGETALL returns empty (key expired or missing): return 401 Unauthorized.

Session data must never include sensitive fields like password hash.

## Test Scenarios

1. Create session. Verify HGETALL returns all fields. Verify TTL is ~1800.
2. Validate session. Verify TTL is refreshed.
3. Wait for TTL expiry (or manually expire). Verify next validation returns not-found.
4. Logout. Verify DEL removes the session. Verify next validation returns not-found.

## Interview Value

Demonstrates: hash as entity store, rolling TTL pattern, session invalidation.
