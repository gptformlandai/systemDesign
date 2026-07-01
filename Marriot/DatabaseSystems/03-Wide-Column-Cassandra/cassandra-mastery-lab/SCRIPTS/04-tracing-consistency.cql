USE cassandra_mastery;

CONSISTENCY ONE;
TRACING ON;

SELECT *
FROM messages_by_room_day
WHERE room_id = 'room-architecture'
  AND bucket_day = '2026-07-01'
LIMIT 5;

TRACING OFF;
CONSISTENCY ONE;