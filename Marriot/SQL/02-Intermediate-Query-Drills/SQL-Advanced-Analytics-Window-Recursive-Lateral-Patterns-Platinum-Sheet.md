# SQL Advanced Analytics Window Recursive Lateral Patterns Platinum Sheet

Target: intermediate to MAANG SQL rounds where the interviewer wants more than basic joins and `ROW_NUMBER`.

This sheet fills the advanced query-pattern layer: window frames, percentiles, `LATERAL`, recursive CTEs, `GROUPING SETS`, `ROLLUP`, `CUBE`, materialized views, and hard analytics prompts.

---

## 0. Advanced Query Mindset

Advanced SQL is still the same discipline:

```text
grain -> source rows -> grouping/window boundary -> tie/null handling -> performance -> explanation
```

The mistake is to jump to a clever feature before defining the output row.

Strong answer:

```text
The output grain is one row per customer per month. I aggregate orders to month first, then
use a window over customer-month rows. I handle missing months explicitly and validate the
plan with indexes on customer_id and order_date.
```

---

# 1. Window Frames

A window function has two separate ideas:

| Concept | Meaning |
|---|---|
| `PARTITION BY` | which rows belong to the same window group |
| `ORDER BY` | how rows are ordered inside the window |
| frame | which ordered rows are visible to the function for the current row |

Example running total:

```sql
SELECT
    customer_id,
    order_date,
    total_amount,
    SUM(total_amount) OVER (
        PARTITION BY customer_id
        ORDER BY order_date
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS running_total
FROM orders;
```

Interview trap:

```text
If ORDER BY has duplicate values, RANGE and ROWS can behave differently. Use a deterministic
ORDER BY and explicit frame when correctness matters.
```

---

# 2. ROWS vs RANGE

`ROWS` counts physical rows in the ordered result.

`RANGE` groups peer rows with the same ordering value.

Example:

```sql
SELECT
    order_id,
    order_date,
    amount,
    SUM(amount) OVER (
        ORDER BY order_date
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS rows_running_total,
    SUM(amount) OVER (
        ORDER BY order_date
        RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS range_running_total
FROM orders;
```

If multiple orders have the same `order_date`, `RANGE` may include all same-date peers at once.

Strong interview line:

```text
I prefer explicit ROWS frames for row-by-row running totals, and I add a tie-breaker like
order_id to make ordering deterministic.
```

---

# 3. Moving Averages

Problem:

```text
Calculate 7-day moving average revenue per day.
```

Query:

```sql
WITH daily AS (
    SELECT
        order_date::date AS revenue_date,
        SUM(total_amount) AS revenue
    FROM orders
    GROUP BY order_date::date
)
SELECT
    revenue_date,
    revenue,
    AVG(revenue) OVER (
        ORDER BY revenue_date
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS moving_avg_7d
FROM daily
ORDER BY revenue_date;
```

Trap:

```text
This assumes one row per day. If missing days should count as zero, generate a date spine
first and left join daily revenue.
```

---

# 4. Date Spine For Missing Periods

Problem:

```text
Show revenue for every day, including days with no orders.
```

PostgreSQL query:

```sql
WITH date_spine AS (
    SELECT generate_series(
        DATE '2026-01-01',
        DATE '2026-01-31',
        INTERVAL '1 day'
    )::date AS revenue_date
), daily AS (
    SELECT order_date::date AS revenue_date, SUM(total_amount) AS revenue
    FROM orders
    WHERE order_date >= DATE '2026-01-01'
      AND order_date < DATE '2026-02-01'
    GROUP BY order_date::date
)
SELECT
    ds.revenue_date,
    COALESCE(d.revenue, 0) AS revenue
FROM date_spine ds
LEFT JOIN daily d ON d.revenue_date = ds.revenue_date
ORDER BY ds.revenue_date;
```

Interview line:

```text
For retention and time-series metrics, I create the complete time grain first, then left
join facts into it.
```

---

# 5. Percentiles And Median

Problem:

```text
Find p50 and p95 order value.
```

PostgreSQL ordered-set aggregate:

```sql
SELECT
    percentile_cont(0.50) WITHIN GROUP (ORDER BY total_amount) AS p50_order_value,
    percentile_cont(0.95) WITHIN GROUP (ORDER BY total_amount) AS p95_order_value
FROM orders
WHERE created_at >= now() - INTERVAL '30 days';
```

Per group:

```sql
SELECT
    country,
    percentile_cont(0.95) WITHIN GROUP (ORDER BY total_amount) AS p95_order_value
FROM orders
GROUP BY country;
```

Trap:

```text
Percentile calculations can be expensive on large datasets. Production dashboards often use
pre-aggregations, approximate percentiles, or warehouse-specific functions.
```

---

# 6. GROUPING SETS

Problem:

```text
Show revenue by country, by product category, and grand total in one query.
```

Query:

```sql
SELECT
    country,
    category,
    SUM(total_amount) AS revenue
FROM sales
GROUP BY GROUPING SETS (
    (country),
    (category),
    ()
);
```

Output grain:

```text
mixed grain: one row per country, one row per category, and one grand total row
```

Add labels:

```sql
SELECT
    CASE WHEN GROUPING(country) = 1 THEN 'ALL_COUNTRIES' ELSE country END AS country_label,
    CASE WHEN GROUPING(category) = 1 THEN 'ALL_CATEGORIES' ELSE category END AS category_label,
    SUM(total_amount) AS revenue
FROM sales
GROUP BY GROUPING SETS ((country), (category), ());
```

---

# 7. ROLLUP

`ROLLUP` creates hierarchical subtotals.

Problem:

```text
Show revenue by country, region, city, plus subtotal levels.
```

Query:

```sql
SELECT
    country,
    region,
    city,
    SUM(total_amount) AS revenue
FROM sales
GROUP BY ROLLUP (country, region, city)
ORDER BY country, region, city;
```

Produces:

```text
country + region + city
country + region subtotal
country subtotal
grand total
```

Strong answer:

```text
ROLLUP is useful when subtotal levels follow a hierarchy.
```

---

# 8. CUBE

`CUBE` creates all combinations of subtotals.

Problem:

```text
Show revenue by country, device, and every subtotal combination.
```

Query:

```sql
SELECT
    country,
    device_type,
    SUM(total_amount) AS revenue
FROM sales
GROUP BY CUBE (country, device_type);
```

Produces:

```text
country + device
country subtotal
device subtotal
grand total
```

Trap:

```text
CUBE can create many rows as dimensions increase. Use it intentionally.
```

---

# 9. LATERAL Joins

`LATERAL` lets a subquery use columns from the row on its left.

Problem:

```text
For each customer, fetch their latest 3 orders.
```

Query:

```sql
SELECT
    c.customer_id,
    c.email,
    recent.order_id,
    recent.created_at,
    recent.total_amount
FROM customers c
LEFT JOIN LATERAL (
    SELECT o.order_id, o.created_at, o.total_amount
    FROM orders o
    WHERE o.customer_id = c.customer_id
    ORDER BY o.created_at DESC, o.order_id DESC
    LIMIT 3
) recent ON true;
```

Why this is useful:

```text
The subquery is evaluated per customer and can use an index on orders(customer_id, created_at desc).
```

Alternative:

```sql
WITH ranked AS (
    SELECT
        o.*,
        ROW_NUMBER() OVER (
            PARTITION BY customer_id
            ORDER BY created_at DESC, order_id DESC
        ) AS rn
    FROM orders o
)
SELECT c.customer_id, c.email, r.order_id, r.created_at, r.total_amount
FROM customers c
LEFT JOIN ranked r ON r.customer_id = c.customer_id AND r.rn <= 3;
```

Trade-off:

```text
Window approach ranks all orders. LATERAL can be faster when each parent needs only a small
LIMIT and the right index exists.
```

---

# 10. Recursive CTE Deep Dive

Problem:

```text
Find all reports under a manager.
```

Query:

```sql
WITH RECURSIVE org AS (
    SELECT
        employee_id,
        manager_id,
        employee_name,
        0 AS depth,
        ARRAY[employee_id] AS path
    FROM employees
    WHERE employee_id = :manager_id

    UNION ALL

    SELECT
        e.employee_id,
        e.manager_id,
        e.employee_name,
        org.depth + 1 AS depth,
        org.path || e.employee_id AS path
    FROM employees e
    JOIN org ON e.manager_id = org.employee_id
    WHERE NOT e.employee_id = ANY(org.path)
)
SELECT *
FROM org
ORDER BY path;
```

Important details:

- anchor query starts recursion
- recursive query adds next level
- `path` prevents cycles
- `depth` helps limit traversal

Interview trap:

```text
Recursive CTEs need cycle protection and depth limits in production-like data.
```

---

# 11. Hierarchy Query With Rollup Counts

Problem:

```text
For each manager, count all downstream reports.
```

Query idea:

```sql
WITH RECURSIVE tree AS (
    SELECT
        employee_id AS manager_id,
        employee_id AS report_id
    FROM employees

    UNION ALL

    SELECT
        tree.manager_id,
        e.employee_id AS report_id
    FROM tree
    JOIN employees e ON e.manager_id = tree.report_id
)
SELECT
    manager_id,
    COUNT(*) - 1 AS downstream_reports
FROM tree
GROUP BY manager_id;
```

Trap:

```text
This can be expensive on large org trees. Consider closure tables/materialized paths if this
query is frequent.
```

---

# 12. Materialized Views And Summary Tables

Use when:

- query is expensive
- result can be slightly stale
- dashboard reads far outnumber writes
- exact ad-hoc freshness is not required

Example:

```sql
CREATE MATERIALIZED VIEW monthly_revenue_mv AS
SELECT
    date_trunc('month', created_at)::date AS revenue_month,
    country,
    SUM(total_amount) AS revenue,
    COUNT(*) AS order_count
FROM orders
GROUP BY date_trunc('month', created_at)::date, country;
```

Refresh:

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY monthly_revenue_mv;
```

Interview line:

```text
I would use a materialized view for expensive repeated analytics, but I would document
freshness, refresh cost, and whether concurrent refresh is required.
```

---

# 13. Cohort Retention Query

Problem:

```text
For each signup month, calculate month 0, month 1, and month 2 retention.
```

Query:

```sql
WITH users_with_cohort AS (
    SELECT
        user_id,
        date_trunc('month', signup_at)::date AS cohort_month
    FROM users
), activity_months AS (
    SELECT DISTINCT
        user_id,
        date_trunc('month', activity_at)::date AS activity_month
    FROM user_activity
), cohort_activity AS (
    SELECT
        u.cohort_month,
        ((date_part('year', a.activity_month) - date_part('year', u.cohort_month)) * 12
         + (date_part('month', a.activity_month) - date_part('month', u.cohort_month)))::int AS month_number,
        COUNT(DISTINCT u.user_id) AS active_users
    FROM users_with_cohort u
    JOIN activity_months a ON a.user_id = u.user_id
    WHERE a.activity_month >= u.cohort_month
    GROUP BY u.cohort_month, month_number
), cohort_size AS (
    SELECT cohort_month, COUNT(*) AS users_in_cohort
    FROM users_with_cohort
    GROUP BY cohort_month
)
SELECT
    ca.cohort_month,
    ca.month_number,
    ca.active_users,
    cs.users_in_cohort,
    ROUND(ca.active_users::numeric / cs.users_in_cohort, 4) AS retention_rate
FROM cohort_activity ca
JOIN cohort_size cs ON cs.cohort_month = ca.cohort_month
WHERE ca.month_number BETWEEN 0 AND 2
ORDER BY ca.cohort_month, ca.month_number;
```

Explanation:

```text
The cohort grain is signup month. The activity grain is user-month. I deduplicate activity
per user-month before calculating retention.
```

---

# 14. Funnel Conversion Query

Problem:

```text
Calculate checkout funnel conversion: viewed_product -> added_to_cart -> paid.
```

Query:

```sql
WITH user_steps AS (
    SELECT
        user_id,
        MIN(event_time) FILTER (WHERE event_name = 'viewed_product') AS viewed_at,
        MIN(event_time) FILTER (WHERE event_name = 'added_to_cart') AS cart_at,
        MIN(event_time) FILTER (WHERE event_name = 'paid') AS paid_at
    FROM events
    WHERE event_time >= now() - INTERVAL '7 days'
    GROUP BY user_id
)
SELECT
    COUNT(*) FILTER (WHERE viewed_at IS NOT NULL) AS viewed_users,
    COUNT(*) FILTER (WHERE cart_at IS NOT NULL AND cart_at >= viewed_at) AS cart_users,
    COUNT(*) FILTER (WHERE paid_at IS NOT NULL AND paid_at >= cart_at) AS paid_users,
    ROUND(
        COUNT(*) FILTER (WHERE paid_at IS NOT NULL AND paid_at >= cart_at)::numeric
        / NULLIF(COUNT(*) FILTER (WHERE viewed_at IS NOT NULL), 0),
        4
    ) AS view_to_paid_rate
FROM user_steps;
```

Trap:

```text
A real funnel often needs session boundaries and ordering windows, not only first event per user.
```

---

# 15. Latest State From Event History

Problem:

```text
Find current status per order from append-only status history.
```

Query:

```sql
SELECT order_id, status, changed_at
FROM (
    SELECT
        osh.*,
        ROW_NUMBER() OVER (
            PARTITION BY order_id
            ORDER BY changed_at DESC, status_history_id DESC
        ) AS rn
    FROM order_status_history osh
) ranked
WHERE rn = 1;
```

PostgreSQL alternative:

```sql
SELECT DISTINCT ON (order_id)
    order_id,
    status,
    changed_at
FROM order_status_history
ORDER BY order_id, changed_at DESC, status_history_id DESC;
```

Trap:

```text
Always include a deterministic tie-breaker.
```

---

# 16. Advanced Analytics Drill Prompts

Practice these without notes:

1. Daily active users with missing days filled as zero.
2. Seven-day moving average revenue.
3. p50/p95 order value by country.
4. Top 3 products per category with ties.
5. Latest three orders per customer using `LATERAL`.
6. Manager hierarchy with cycle protection.
7. Revenue rollup by country, region, city.
8. Funnel conversion with ordered steps.
9. Cohort retention by signup month.
10. Latest state from status history.
11. Percent change from previous month with missing months handled.
12. Dashboard query redesigned as a materialized view.

---

# 17. Final Interview Answer

```text
For advanced SQL, I still start with grain. Then I choose the right mechanism: window frames
for row-relative calculations, recursive CTEs for hierarchies, LATERAL for per-row top-N
lookups, grouping sets/rollup/cube for subtotal reports, percentiles for distribution
questions, and materialized views when repeated analytics need precomputation. I explain
ties, NULLs, missing dates, and index support because those decide correctness and performance.
```
