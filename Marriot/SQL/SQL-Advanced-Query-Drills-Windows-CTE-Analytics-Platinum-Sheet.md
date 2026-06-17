# SQL Advanced Query Drills Windows CTE Analytics Platinum Sheet

Target: fast SQL coding revision for intermediate, senior, and MAANG interviews.

This sheet is a pattern bank. Each pattern gives the grain, query, trap, and interview
explanation.

---

## 0. Golden Rule: State The Grain

Before writing SQL, say:

```text
The output grain is one row per <entity/time/group>.
```

Examples:

- one row per customer
- one row per customer per month
- one row per order
- one row per product per category

If you cannot state the grain, the query will likely be wrong.

---

# 1. Latest Row Per Group

## Problem

Find latest status for each order.

## Query

```sql
WITH ranked AS (
    SELECT
        osh.*,
        ROW_NUMBER() OVER (
            PARTITION BY order_id
            ORDER BY changed_at DESC, status_history_id DESC
        ) AS rn
    FROM order_status_history osh
)
SELECT *
FROM ranked
WHERE rn = 1;
```

## Trap

No tie-breaker.

## Explanation

```text
The output grain is one row per order. ROW_NUMBER gives exactly one latest row per order,
and the secondary sort makes ties deterministic.
```

---

# 2. Top N Per Group

## Problem

Top 3 products by revenue per category.

```sql
WITH product_revenue AS (
    SELECT
        p.category_id,
        oi.product_id,
        SUM(oi.quantity * oi.unit_price) AS revenue
    FROM order_items oi
    JOIN products p ON p.product_id = oi.product_id
    GROUP BY p.category_id, oi.product_id
),
ranked AS (
    SELECT
        *,
        DENSE_RANK() OVER (
            PARTITION BY category_id
            ORDER BY revenue DESC
        ) AS revenue_rank
    FROM product_revenue
)
SELECT *
FROM ranked
WHERE revenue_rank <= 3;
```

Use `ROW_NUMBER` if exactly 3 rows are required. Use `DENSE_RANK` if ties should be included.

---

# 3. Running Total

## Problem

Daily running revenue.

```sql
WITH daily AS (
    SELECT
        CAST(order_date AS DATE) AS order_day,
        SUM(total_amount) AS daily_revenue
    FROM orders
    WHERE status = 'PAID'
    GROUP BY CAST(order_date AS DATE)
)
SELECT
    order_day,
    daily_revenue,
    SUM(daily_revenue) OVER (
        ORDER BY order_day
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS running_revenue
FROM daily
ORDER BY order_day;
```

Trap:

```text
Running total should usually run over already-aggregated daily rows, not raw order rows.
```

---

# 4. Month-Over-Month Growth

```sql
WITH monthly AS (
    SELECT
        DATE_TRUNC('month', order_date) AS month_start,
        SUM(total_amount) AS revenue
    FROM orders
    WHERE status = 'PAID'
    GROUP BY DATE_TRUNC('month', order_date)
)
SELECT
    month_start,
    revenue,
    LAG(revenue) OVER (ORDER BY month_start) AS previous_revenue,
    ROUND(
        100.0 * (revenue - LAG(revenue) OVER (ORDER BY month_start))
        / NULLIF(LAG(revenue) OVER (ORDER BY month_start), 0),
        2
    ) AS growth_percent
FROM monthly
ORDER BY month_start;
```

Trap:

- divide by zero
- missing months
- comparing raw rows instead of monthly grain

---

# 5. Customer Retention

## Problem

For each month, count customers who bought in current month and previous month.

```sql
WITH customer_months AS (
    SELECT DISTINCT
        customer_id,
        DATE_TRUNC('month', order_date) AS month_start
    FROM orders
    WHERE status = 'PAID'
),
with_previous AS (
    SELECT
        customer_id,
        month_start,
        LAG(month_start) OVER (
            PARTITION BY customer_id
            ORDER BY month_start
        ) AS previous_month
    FROM customer_months
)
SELECT
    month_start,
    COUNT(*) AS active_customers,
    COUNT(*) FILTER (
        WHERE previous_month = month_start - INTERVAL '1 month'
    ) AS retained_customers
FROM with_previous
GROUP BY month_start
ORDER BY month_start;
```

Output grain:

```text
one row per month
```

---

# 6. Gaps And Islands: Consecutive Active Days

## Problem

Find consecutive login streaks per user.

```sql
WITH login_days AS (
    SELECT DISTINCT
        user_id,
        CAST(login_at AS DATE) AS login_day
    FROM logins
),
numbered AS (
    SELECT
        user_id,
        login_day,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY login_day
        ) AS rn
    FROM login_days
),
grouped AS (
    SELECT
        user_id,
        login_day,
        login_day - (rn * INTERVAL '1 day') AS island_key
    FROM numbered
)
SELECT
    user_id,
    MIN(login_day) AS streak_start,
    MAX(login_day) AS streak_end,
    COUNT(*) AS streak_days
FROM grouped
GROUP BY user_id, island_key
ORDER BY user_id, streak_start;
```

Trap:

```text
Remove duplicate same-day logins first, otherwise streak length is inflated.
```

---

# 7. Overlapping Date Ranges

## Problem

Find bookings that overlap a requested stay.

```sql
SELECT *
FROM bookings
WHERE room_id = :room_id
  AND status IN ('PENDING', 'CONFIRMED')
  AND check_in < :requested_check_out
  AND check_out > :requested_check_in;
```

Why:

```text
Two ranges overlap when each starts before the other ends.
```

Common wrong query:

```sql
WHERE check_in BETWEEN :in AND :out
```

This misses bookings that start before requested check-in but end inside the range.

---

# 8. Duplicate Detection

```sql
SELECT
    email,
    COUNT(*) AS count_rows
FROM users
GROUP BY email
HAVING COUNT(*) > 1;
```

Find full duplicate rows:

```sql
SELECT *
FROM users
WHERE email IN (
    SELECT email
    FROM users
    GROUP BY email
    HAVING COUNT(*) > 1
)
ORDER BY email;
```

Trap:

```text
COUNT(column) ignores NULL. Use COUNT(*) when counting rows.
```

---

# 9. Delete Duplicates Safely

Keep smallest user_id per email:

```sql
WITH ranked AS (
    SELECT
        user_id,
        ROW_NUMBER() OVER (
            PARTITION BY email
            ORDER BY user_id
        ) AS rn
    FROM users
)
DELETE FROM users
WHERE user_id IN (
    SELECT user_id
    FROM ranked
    WHERE rn > 1
);
```

Production safety:

- preview rows first
- take backup if needed
- run in transaction
- add unique constraint after cleanup

---

# 10. Anti-Join: Missing Relationship

Customers with no orders:

```sql
SELECT c.*
FROM customers c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.customer_id
);
```

Why `NOT EXISTS` is often safer:

```text
NOT IN behaves badly if the subquery can return NULL.
```

---

# 11. Conditional Aggregation

```sql
SELECT
    customer_id,
    COUNT(*) AS total_orders,
    SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_orders,
    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders,
    SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END) AS paid_revenue
FROM orders
GROUP BY customer_id;
```

PostgreSQL style:

```sql
SELECT
    customer_id,
    COUNT(*) AS total_orders,
    COUNT(*) FILTER (WHERE status = 'PAID') AS paid_orders,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_orders
FROM orders
GROUP BY customer_id;
```

---

# 12. Join Multiplication Trap

Wrong:

```sql
SELECT
    o.order_id,
    SUM(oi.quantity * oi.unit_price) AS item_total,
    SUM(p.amount) AS paid_amount
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
JOIN payments p ON p.order_id = o.order_id
GROUP BY o.order_id;
```

If one order has 3 items and 2 payments, join creates 6 rows.

Better:

```sql
WITH item_totals AS (
    SELECT order_id, SUM(quantity * unit_price) AS item_total
    FROM order_items
    GROUP BY order_id
),
payment_totals AS (
    SELECT order_id, SUM(amount) AS paid_amount
    FROM payments
    GROUP BY order_id
)
SELECT
    o.order_id,
    it.item_total,
    pt.paid_amount
FROM orders o
LEFT JOIN item_totals it ON it.order_id = o.order_id
LEFT JOIN payment_totals pt ON pt.order_id = o.order_id;
```

Rule:

```text
Pre-aggregate each one-to-many table before joining multiple one-to-many relationships.
```

---

# 13. Pivot With Conditional Aggregation

```sql
SELECT
    customer_id,
    SUM(CASE WHEN EXTRACT(MONTH FROM order_date) = 1 THEN total_amount ELSE 0 END) AS jan,
    SUM(CASE WHEN EXTRACT(MONTH FROM order_date) = 2 THEN total_amount ELSE 0 END) AS feb,
    SUM(CASE WHEN EXTRACT(MONTH FROM order_date) = 3 THEN total_amount ELSE 0 END) AS mar
FROM orders
WHERE order_date >= DATE '2026-01-01'
  AND order_date < DATE '2026-04-01'
GROUP BY customer_id;
```

Trap:

```text
Do not use function predicates on indexed date columns if a range predicate works.
```

---

# 14. Query Debug Checklist

Before final answer:

- Did I state output grain?
- Did I handle ties?
- Did I handle NULL?
- Did joins multiply rows?
- Did I aggregate at correct level?
- Did I filter before grouping when possible?
- Did I use `WHERE` vs `HAVING` correctly?
- Did I mention index/performance for backend query?

---

# 15. Hot Drill Prompts

Practice these until they feel automatic:

1. Latest status per order.
2. Top 3 products per category by revenue.
3. Running revenue by day.
4. Month-over-month revenue growth.
5. Customers active in current and previous month.
6. Consecutive login streaks.
7. Overlapping hotel bookings.
8. Customers with no orders.
9. Duplicate emails and safe delete.
10. Join orders, items, payments without multiplication.

---

# 16. Final Rapid Revision

```text
Latest row -> ROW_NUMBER partition by group order by timestamp desc + tie-breaker.
Top N -> RANK/DENSE_RANK/ROW_NUMBER depending tie requirement.
Running total -> aggregate first, then SUM OVER.
MoM -> monthly CTE + LAG + NULLIF.
Gaps/islands -> date - row_number interval.
Overlap -> start1 < end2 AND end1 > start2.
Anti-join -> NOT EXISTS.
Join multiplication -> pre-aggregate one-to-many tables.
```
