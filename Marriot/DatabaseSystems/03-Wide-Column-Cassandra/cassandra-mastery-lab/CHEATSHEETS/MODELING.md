# Cassandra Modeling Cheatsheet

## Modeling Formula

```text
access pattern -> table -> partition key -> clustering keys -> consistency -> operational risk
```

## Good Partition Keys

- high cardinality
- evenly distributed
- query-aligned
- bounded by time or another bucket when needed
- stable

## Bad Partition Keys

- `status`
- `country`
- `event_day` alone
- `tenant_id` alone for highly skewed tenants
- random UUID when the read needs grouped history

## Table Naming

Use query-shaped names:

- `messages_by_room_day`
- `metrics_by_device_hour`
- `orders_by_customer_day`
- `audit_events_by_tenant_day`

## Interview Rule

```text
If a query does not know its partition key, Cassandra probably should not serve it directly.
```