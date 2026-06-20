# SQL Data Modeling Backend Analytics Case Studies Platinum Sheet

Target: backend engineers who need to design schemas, constraints, and analytical models.

This sheet teaches schema design through practical product cases.

---

## 0. Data Modeling Mindset

A schema is not just storage. It encodes business rules.

```text
Good schema design makes invalid states hard to store.
```

Before designing tables, ask:

- What is the entity?
- What is the lifecycle?
- What is the source of truth?
- What must be unique?
- What can change over time?
- What needs history?
- What is queried frequently?
- What is the concurrency risk?

---

# 1. OLTP vs OLAP

| OLTP | OLAP |
|---|---|
| application transactions | analytics/reporting |
| normalized | denormalized/star schema |
| many small reads/writes | fewer large scans |
| correctness and concurrency | aggregation speed |
| current state | historical trends |

Strong answer:

```text
I do not force one schema to serve both checkout traffic and heavy analytics. OLTP protects
business transactions; OLAP is optimized for analysis.
```

---

# 2. Normalization

Normalization reduces duplication and update anomalies.

Example:

```text
customers(customer_id, name, email)
orders(order_id, customer_id, order_date, status)
order_items(order_item_id, order_id, product_id, quantity, unit_price)
```

Why:

- customer stored once
- order has many items
- product can be referenced
- updates do not duplicate data everywhere

When to denormalize:

- read performance
- search index
- reporting
- cache/read model

---

# 3. Case Study: E-Commerce Orders

## Tables

```sql
CREATE TABLE orders (
    order_id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
    order_item_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL
);

CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    provider_reference VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## Constraints

```sql
ALTER TABLE order_items
ADD CONSTRAINT chk_order_items_quantity CHECK (quantity > 0);

CREATE UNIQUE INDEX uq_payments_provider_reference
ON payments (provider_reference)
WHERE provider_reference IS NOT NULL;
```

## Interview Points

- keep item price snapshot because product price can change
- payment may have multiple attempts
- status lifecycle should be controlled
- use idempotency for payment retries
- index customer order history query

---

# 4. Case Study: Hotel Booking Availability

## Requirement

Prevent overselling rooms for a date.

## Count-Based Inventory

```sql
CREATE TABLE room_inventory (
    hotel_id BIGINT NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    stay_date DATE NOT NULL,
    available_rooms INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (hotel_id, room_type, stay_date),
    CHECK (available_rooms >= 0)
);
```

Atomic reservation:

```sql
UPDATE room_inventory
SET available_rooms = available_rooms - 1,
    version = version + 1
WHERE hotel_id = :hotel_id
  AND room_type = :room_type
  AND stay_date = :stay_date
  AND available_rooms > 0;
```

Affected rows = 1 means success.

## Booking Table

```sql
CREATE TABLE bookings (
    booking_id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CHECK (check_out > check_in)
);
```

Indexes:

```sql
CREATE INDEX idx_bookings_customer_created
ON bookings (customer_id, created_at DESC);

CREATE INDEX idx_bookings_hotel_dates
ON bookings (hotel_id, check_in, check_out);
```

---

# 5. Case Study: Audit Log / Status History

Do not overwrite history when state changes matter.

```sql
CREATE TABLE booking_status_history (
    status_history_id BIGINT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL
);
```

Latest status query:

```sql
SELECT *
FROM booking_status_history
WHERE booking_id = :booking_id
ORDER BY changed_at DESC, status_history_id DESC
LIMIT 1;
```

Index:

```sql
CREATE INDEX idx_booking_status_latest
ON booking_status_history (booking_id, changed_at DESC, status_history_id DESC);
```

When to use:

- audit requirements
- lifecycle debugging
- reporting historical changes
- compliance

---

# 6. Case Study: Idempotency Table

```sql
CREATE TABLE idempotency_keys (
    key VARCHAR(100) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    response_code INT,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);
```

Design points:

- key must be unique
- request hash prevents key reuse for different request
- store final response for safe retry
- expire old keys after retention period
- do not use idempotency key as security credential

---

# 7. Case Study: Product Catalog

Tables:

```sql
CREATE TABLE products (
    product_id BIGINT PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE product_prices (
    price_id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP,
    CHECK (price >= 0)
);
```

Why separate price history:

- price changes over time
- orders need price snapshot
- promotions/audit need history

Trap:

```text
Do not compute old order totals from current product price.
```

---

# 8. Case Study: Star Schema For Analytics

For reporting revenue:

```text
fact_order_items
dim_date
dim_customer
dim_product
dim_category
dim_location
```

Fact table:

```sql
CREATE TABLE fact_order_items (
    date_key INT NOT NULL,
    customer_key BIGINT NOT NULL,
    product_key BIGINT NOT NULL,
    location_key BIGINT NOT NULL,
    quantity INT NOT NULL,
    revenue DECIMAL(12,2) NOT NULL
);
```

Dimension:

```sql
CREATE TABLE dim_product (
    product_key BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    category_name VARCHAR(255) NOT NULL
);
```

Why:

- faster aggregations
- stable historical attributes
- simpler BI queries

---

# 9. Slowly Changing Dimensions

Problem:

```text
Customer city changes. Historical reports should show old city for old orders.
```

Type 2 dimension:

```sql
CREATE TABLE dim_customer (
    customer_key BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    city VARCHAR(100),
    valid_from DATE NOT NULL,
    valid_to DATE,
    is_current BOOLEAN NOT NULL
);
```

Use when historical attributes matter.

---

# 10. Constraints Checklist

Use constraints to protect data:

| Constraint | Use |
|---|---|
| PRIMARY KEY | row identity |
| FOREIGN KEY | relationship validity |
| UNIQUE | business uniqueness |
| CHECK | allowed values/ranges |
| NOT NULL | required fields |
| EXCLUDE/database-specific | range overlap prevention |

Strong answer:

```text
Application validation improves user experience, but database constraints protect truth
under concurrency, bugs, and multiple writers.
```

---

# 11. Index Design By Access Pattern

For every API, write:

```text
Endpoint:
Filters:
Sort:
Limit:
Join keys:
Expected cardinality:
Index:
```

Example:

```text
GET /customers/{id}/orders
Filters: customer_id, status
Sort: created_at desc
Limit: 20
Index: (customer_id, status, created_at desc)
```

---

# 12. Soft Delete

Pattern:

```sql
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
```

Pros:

- recoverable
- audit friendly

Cons:

- every query must filter active rows
- uniqueness gets tricky
- storage grows

Partial unique index:

```sql
CREATE UNIQUE INDEX uq_users_email_active
ON users (email)
WHERE deleted_at IS NULL;
```

---

# 13. Multi-Tenant Schema Design

Options:

| Model | Pros | Cons |
|---|---|---|
| shared DB, tenant_id column | simple, efficient | isolation bugs possible |
| schema per tenant | better isolation | operational complexity |
| database per tenant | strongest isolation | expensive and complex |

Shared table rule:

```text
Every tenant-owned table must include tenant_id and every query must filter by trusted
tenant_id.
```

Index:

```sql
CREATE INDEX idx_orders_tenant_customer_created
ON orders (tenant_id, customer_id, created_at DESC);
```

---

# 14. Common Modeling Mistakes

| Mistake | Better Approach |
|---|---|
| No unique constraints | encode business uniqueness |
| Current price used for old orders | snapshot price at purchase |
| Updating status without history | status history table |
| JSON for everything | relational columns for queried fields |
| No tenant_id in indexes | tenant-aware index prefix |
| Analytics on OLTP hot tables | summary/star schema/read replica |
| Soft delete without filtered indexes | partial indexes and query discipline |

---

# 15. Interview Question

> Design the SQL schema for a hotel booking system.

Strong answer:

```text
I would model hotels and room types separately from date-level inventory. The room_inventory
table has primary key hotel_id, room_type, stay_date and available_rooms with a CHECK >= 0.
Reservation uses an atomic update where available_rooms > 0 to prevent oversell. Bookings
store customer, hotel, room type, check-in/check-out, status, and timestamps, with a CHECK
that checkout is after checkin. I add booking_status_history for audit, idempotency_keys for
safe retries, and indexes for customer booking history and hotel/date lookup. Payment is a
separate table with unique provider reference to prevent duplicate side effects.
```

---

# 16. Final Rapid Revision

```text
Schema encodes business truth.
Normalize OLTP, denormalize read models/analytics.
Use constraints for invariants.
Snapshot values that change later.
Use history tables when lifecycle matters.
Design indexes from endpoint filters/sorts.
Use idempotency table for retry-safe POST.
Use star schema for analytics.
Tenant ID must be part of query and index design.
```

---

# 17. Official Source Notes

- PostgreSQL constraints: https://www.postgresql.org/docs/current/ddl-constraints.html
- PostgreSQL indexes: https://www.postgresql.org/docs/current/indexes.html
- PostgreSQL EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
