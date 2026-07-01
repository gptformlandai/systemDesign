USE cassandra_mastery;

SELECT *
FROM messages_by_room_day
WHERE room_id = 'room-architecture'
  AND bucket_day = '2026-07-01'
LIMIT 10;

SELECT *
FROM messages_by_sender_day
WHERE sender_id = 'u1'
  AND bucket_day = '2026-07-01';

SELECT *
FROM metrics_by_device_hour
WHERE device_id = 'device-1'
  AND bucket_hour = '2026-07-01T10:00:00Z'
LIMIT 10;

SELECT *
FROM audit_events_by_tenant_day
WHERE tenant_id = 'tenant-a'
  AND event_day = '2026-07-01'
LIMIT 10;

SELECT *
FROM orders_by_customer_day
WHERE customer_id = 'customer-1'
  AND order_day = '2026-07-01';

SELECT *
FROM orders_by_status_day
WHERE status = 'PAID'
  AND order_day = '2026-07-01';