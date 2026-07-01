USE cassandra_mastery;

INSERT INTO session_by_id (session_id, user_id, created_at, expires_at, ip_address)
VALUES ('short-session', 'u2', toTimestamp(now()), '2026-07-01T12:05:00Z', '10.0.0.20')
USING TTL 60;

SELECT TTL(user_id), session_id, user_id, created_at, expires_at
FROM session_by_id
WHERE session_id = 'short-session';

DELETE FROM sessions_by_user
WHERE user_id = 'u1'
  AND created_day = '2026-07-01'
  AND created_at = '2026-07-01T09:00:00Z'
  AND session_id = 'session-1';

SELECT 'TTL and delete demo executed; explain tombstone behavior in your notes.' AS reminder
FROM system.local;