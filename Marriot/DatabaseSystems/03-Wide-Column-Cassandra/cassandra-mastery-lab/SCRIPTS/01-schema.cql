USE cassandra_mastery;

DROP TABLE IF EXISTS messages_by_room_day;
DROP TABLE IF EXISTS messages_by_sender_day;
DROP TABLE IF EXISTS metrics_by_device_hour;
DROP TABLE IF EXISTS audit_events_by_tenant_day;
DROP TABLE IF EXISTS session_by_id;
DROP TABLE IF EXISTS sessions_by_user;
DROP TABLE IF EXISTS order_by_id;
DROP TABLE IF EXISTS orders_by_customer_day;
DROP TABLE IF EXISTS orders_by_status_day;
DROP TABLE IF EXISTS processed_events_by_consumer;

CREATE TABLE messages_by_room_day (
  room_id text,
  bucket_day date,
  room_name text static,
  message_ts timestamp,
  message_id uuid,
  sender_id text,
  body text,
  PRIMARY KEY ((room_id, bucket_day), message_ts, message_id)
) WITH CLUSTERING ORDER BY (message_ts DESC, message_id ASC);

CREATE TABLE messages_by_sender_day (
  sender_id text,
  bucket_day date,
  message_ts timestamp,
  message_id uuid,
  room_id text,
  body text,
  PRIMARY KEY ((sender_id, bucket_day), message_ts, message_id)
) WITH CLUSTERING ORDER BY (message_ts DESC, message_id ASC);

CREATE TABLE metrics_by_device_hour (
  device_id text,
  bucket_hour timestamp,
  metric_ts timestamp,
  metric_name text,
  value double,
  tags map<text, text>,
  PRIMARY KEY ((device_id, bucket_hour), metric_ts, metric_name)
) WITH CLUSTERING ORDER BY (metric_ts DESC, metric_name ASC)
  AND default_time_to_live = 2592000;

CREATE TABLE audit_events_by_tenant_day (
  tenant_id text,
  event_day date,
  event_ts timestamp,
  event_id uuid,
  actor_id text,
  event_type text,
  resource_id text,
  details text,
  PRIMARY KEY ((tenant_id, event_day), event_ts, event_id)
) WITH CLUSTERING ORDER BY (event_ts DESC, event_id ASC);

CREATE TABLE session_by_id (
  session_id text PRIMARY KEY,
  user_id text,
  created_at timestamp,
  expires_at timestamp,
  ip_address text
) WITH default_time_to_live = 86400;

CREATE TABLE sessions_by_user (
  user_id text,
  created_day date,
  created_at timestamp,
  session_id text,
  expires_at timestamp,
  ip_address text,
  PRIMARY KEY ((user_id, created_day), created_at, session_id)
) WITH CLUSTERING ORDER BY (created_at DESC, session_id ASC)
  AND default_time_to_live = 86400;

CREATE TABLE order_by_id (
  order_id uuid PRIMARY KEY,
  customer_id text,
  status text,
  created_at timestamp,
  total_cents bigint
);

CREATE TABLE orders_by_customer_day (
  customer_id text,
  order_day date,
  created_at timestamp,
  order_id uuid,
  status text,
  total_cents bigint,
  PRIMARY KEY ((customer_id, order_day), created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC, order_id ASC);

CREATE TABLE orders_by_status_day (
  status text,
  order_day date,
  created_at timestamp,
  order_id uuid,
  customer_id text,
  total_cents bigint,
  PRIMARY KEY ((status, order_day), created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC, order_id ASC);

CREATE TABLE processed_events_by_consumer (
  consumer_name text,
  event_id text,
  processed_at timestamp,
  PRIMARY KEY (consumer_name, event_id)
);

DESCRIBE TABLES;