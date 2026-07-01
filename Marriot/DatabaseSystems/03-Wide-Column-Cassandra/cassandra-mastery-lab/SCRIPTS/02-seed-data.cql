USE cassandra_mastery;

INSERT INTO messages_by_room_day (room_id, bucket_day, room_name, message_ts, message_id, sender_id, body)
VALUES ('room-architecture', '2026-07-01', 'Architecture Room', '2026-07-01T10:00:00Z', 11111111-1111-4111-8111-111111111111, 'u1', 'Cassandra table design starts with the query.');

INSERT INTO messages_by_room_day (room_id, bucket_day, room_name, message_ts, message_id, sender_id, body)
VALUES ('room-architecture', '2026-07-01', 'Architecture Room', '2026-07-01T10:01:00Z', 22222222-2222-4222-8222-222222222222, 'u2', 'Partition key choice decides scaling behavior.');

INSERT INTO messages_by_room_day (room_id, bucket_day, room_name, message_ts, message_id, sender_id, body)
VALUES ('room-architecture', '2026-07-01', 'Architecture Room', '2026-07-01T10:02:00Z', 33333333-3333-4333-8333-333333333333, 'u1', 'Bucketing keeps partitions bounded.');

INSERT INTO messages_by_sender_day (sender_id, bucket_day, message_ts, message_id, room_id, body)
VALUES ('u1', '2026-07-01', '2026-07-01T10:00:00Z', 11111111-1111-4111-8111-111111111111, 'room-architecture', 'Cassandra table design starts with the query.');

INSERT INTO messages_by_sender_day (sender_id, bucket_day, message_ts, message_id, room_id, body)
VALUES ('u1', '2026-07-01', '2026-07-01T10:02:00Z', 33333333-3333-4333-8333-333333333333, 'room-architecture', 'Bucketing keeps partitions bounded.');

INSERT INTO metrics_by_device_hour (device_id, bucket_hour, metric_ts, metric_name, value, tags)
VALUES ('device-1', '2026-07-01T10:00:00Z', '2026-07-01T10:00:01Z', 'temperature', 72.4, {'region': 'us-central', 'firmware': '1.0.0'});

INSERT INTO metrics_by_device_hour (device_id, bucket_hour, metric_ts, metric_name, value, tags)
VALUES ('device-1', '2026-07-01T10:00:00Z', '2026-07-01T10:01:01Z', 'temperature', 72.8, {'region': 'us-central', 'firmware': '1.0.0'});

INSERT INTO metrics_by_device_hour (device_id, bucket_hour, metric_ts, metric_name, value, tags)
VALUES ('device-2', '2026-07-01T10:00:00Z', '2026-07-01T10:00:05Z', 'humidity', 44.1, {'region': 'us-east', 'firmware': '1.1.0'});

INSERT INTO audit_events_by_tenant_day (tenant_id, event_day, event_ts, event_id, actor_id, event_type, resource_id, details)
VALUES ('tenant-a', '2026-07-01', '2026-07-01T09:00:00Z', 44444444-4444-4444-8444-444444444444, 'u1', 'LOGIN', 'session-1', 'successful login');

INSERT INTO audit_events_by_tenant_day (tenant_id, event_day, event_ts, event_id, actor_id, event_type, resource_id, details)
VALUES ('tenant-a', '2026-07-01', '2026-07-01T09:15:00Z', 55555555-5555-4555-8555-555555555555, 'u2', 'ROLE_CHANGE', 'role-admin', 'temporary elevation');

INSERT INTO session_by_id (session_id, user_id, created_at, expires_at, ip_address)
VALUES ('session-1', 'u1', '2026-07-01T09:00:00Z', '2026-07-02T09:00:00Z', '10.0.0.10');

INSERT INTO sessions_by_user (user_id, created_day, created_at, session_id, expires_at, ip_address)
VALUES ('u1', '2026-07-01', '2026-07-01T09:00:00Z', 'session-1', '2026-07-02T09:00:00Z', '10.0.0.10');

INSERT INTO order_by_id (order_id, customer_id, status, created_at, total_cents)
VALUES (66666666-6666-4666-8666-666666666666, 'customer-1', 'PAID', '2026-07-01T11:00:00Z', 12999);

INSERT INTO orders_by_customer_day (customer_id, order_day, created_at, order_id, status, total_cents)
VALUES ('customer-1', '2026-07-01', '2026-07-01T11:00:00Z', 66666666-6666-4666-8666-666666666666, 'PAID', 12999);

INSERT INTO orders_by_status_day (status, order_day, created_at, order_id, customer_id, total_cents)
VALUES ('PAID', '2026-07-01', '2026-07-01T11:00:00Z', 66666666-6666-4666-8666-666666666666, 'customer-1', 12999);

INSERT INTO processed_events_by_consumer (consumer_name, event_id, processed_at)
VALUES ('audit-read-model-consumer', 'event-001', '2026-07-01T12:00:00Z');

SELECT 'seed complete' AS status FROM system.local;