# SQL Query Mastery Through Story Mode

> You have a Spring Boot app with PostgreSQL. Every feature you build eventually turns into a SQL query. This guide explains SQL through real queries — what each clause actually does to your rows, how operators like GROUP BY, HAVING, RANK, DENSE_RANK change the result, and how indexing and optimization make things fast. Built for interview query-solving, not textbook theory.

---

# Table of Contents

1. [The Dataset We Use Throughout](#1-the-dataset-we-use-throughout)
2. [SELECT and FROM — The Starting Point](#2-select-and-from--the-starting-point)
3. [WHERE — Filtering Rows Before Anything Else](#3-where--filtering-rows-before-anything-else)
4. [ORDER BY — Sorting the Final Result](#4-order-by--sorting-the-final-result)
5. [DISTINCT — Remove Duplicate Rows](#5-distinct--remove-duplicate-rows)
6. [LIMIT and OFFSET — Pagination](#6-limit-and-offset--pagination)
7. [GROUP BY — Collapse Rows Into Groups](#7-group-by--collapse-rows-into-groups)
8. [Aggregate Functions — COUNT, SUM, AVG, MIN, MAX](#8-aggregate-functions--count-sum-avg-min-max)
9. [HAVING — Filter After Grouping](#9-having--filter-after-grouping)
10. [JOINs — Combining Tables](#10-joins--combining-tables)
11. [Subqueries — Queries Inside Queries](#11-subqueries--queries-inside-queries)
12. [EXISTS and IN — Membership Tests](#12-exists-and-in--membership-tests)
13. [CASE — Conditional Logic Inside a Query](#13-case--conditional-logic-inside-a-query)
14. [Window Functions — The Big Interview Topic](#14-window-functions--the-big-interview-topic)
15. [CTEs — WITH Clause](#15-ctes--with-clause)
16. [UNION, INTERSECT, EXCEPT — Set Operations](#16-union-intersect-except--set-operations)
17. [COALESCE, NULLIF, and NULL Handling](#17-coalesce-nullif-and-null-handling)
18. [INSERT, UPDATE, DELETE, UPSERT](#18-insert-update-delete-upsert)
19. [Indexing — Why Queries Are Fast or Slow](#19-indexing--why-queries-are-fast-or-slow)
20. [Query Optimization — Reading EXPLAIN](#20-query-optimization--reading-explain)
21. [Transactions, ACID, and Isolation Levels](#21-transactions-acid-and-isolation-levels)
22. [Normalization and Denormalization](#22-normalization-and-denormalization)
23. [Classic Interview Query Patterns](#23-classic-interview-query-patterns)
24. [Quick Revision Sheet](#24-quick-revision-sheet)

---

# 1. The Dataset We Use Throughout

Every example in this guide uses these tables. They represent a simple e-commerce system.

```sql
-- employees
CREATE TABLE employees (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100),
    department  VARCHAR(50),
    salary      NUMERIC(10,2),
    manager_id  INT REFERENCES employees(id),
    hire_date   DATE
);

-- orders
CREATE TABLE orders (
    id           SERIAL PRIMARY KEY,
    customer_id  INT,
    amount       NUMERIC(10,2),
    status       VARCHAR(20),   -- 'completed', 'pending', 'cancelled'
    order_date   DATE
);

-- customers
CREATE TABLE customers (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(100),
    city    VARCHAR(50),
    email   VARCHAR(100)
);

-- products
CREATE TABLE products (
    id       SERIAL PRIMARY KEY,
    name     VARCHAR(100),
    category VARCHAR(50),
    price    NUMERIC(10,2)
);

-- order_items
CREATE TABLE order_items (
    id          SERIAL PRIMARY KEY,
    order_id    INT REFERENCES orders(id),
    product_id  INT REFERENCES products(id),
    quantity    INT,
    unit_price  NUMERIC(10,2)
);
```

Sample data (imagine mentally):

```text
employees:
  1 | Alice   | Engineering | 90000  | NULL | 2020-01-15
  2 | Bob     | Engineering | 85000  | 1    | 2021-03-20
  3 | Charlie | Sales       | 70000  | NULL | 2019-06-10
  4 | Diana   | Sales       | 72000  | 3    | 2022-01-05
  5 | Eve     | Engineering | 95000  | 1    | 2020-08-12
  6 | Frank   | HR          | 65000  | NULL | 2023-02-01
```

---

# 2. SELECT and FROM — The Starting Point

## What SELECT Actually Does

SELECT chooses which columns appear in the output.

```sql
SELECT name, salary FROM employees;
```

What happens internally:

```text
1. FROM employees   → engine reads rows from the employees table
2. SELECT name, salary → from each row, keep only these two columns
```

Result:

```text
name    | salary
--------+--------
Alice   | 90000
Bob     | 85000
Charlie | 70000
Diana   | 72000
Eve     | 95000
Frank   | 65000
```

## SELECT * Means All Columns

```sql
SELECT * FROM employees;
```

Returns every column. Convenient for exploration but avoid in production queries because:

- it fetches columns you don't need
- if the table schema changes, your app may break

---

# 3. WHERE — Filtering Rows Before Anything Else

## What WHERE Actually Does

WHERE removes rows that don't match the condition. It runs **before** GROUP BY and before aggregates.

```sql
SELECT name, salary
FROM employees
WHERE department = 'Engineering';
```

What happens:

```text
1. FROM employees → read all 6 rows
2. WHERE department = 'Engineering' → keep only rows where department matches
   → Alice, Bob, Eve pass
3. SELECT name, salary → take only these columns
```

Result:

```text
name  | salary
------+--------
Alice | 90000
Bob   | 85000
Eve   | 95000
```

## Common WHERE Operators

```sql
WHERE salary > 80000                       -- comparison
WHERE department IN ('Engineering', 'HR')  -- membership
WHERE name LIKE 'A%'                       -- pattern (starts with A)
WHERE manager_id IS NULL                   -- null check (not = NULL!)
WHERE salary BETWEEN 70000 AND 90000       -- range (inclusive both ends)
WHERE department = 'Sales' AND salary > 70000  -- logical AND
WHERE department = 'Sales' OR department = 'HR' -- logical OR
```

## The Critical NULL Rule

```text
NULL = NULL   → FALSE (not TRUE!)
NULL != NULL  → FALSE
NULL > 5      → NULL (unknown)

That is why you must use IS NULL and IS NOT NULL, never = NULL.
```

---

# 4. ORDER BY — Sorting the Final Result

## What ORDER BY Actually Does

ORDER BY sorts the output rows. It runs **after** everything else (WHERE, GROUP BY, HAVING, SELECT).

```sql
SELECT name, salary
FROM employees
ORDER BY salary DESC;
```

Result:

```text
Eve     | 95000
Alice   | 90000
Bob     | 85000
Diana   | 72000
Charlie | 70000
Frank   | 65000
```

## Multiple Columns

```sql
ORDER BY department ASC, salary DESC
```

First sort by department alphabetically. Within same department, sort by salary highest first.

## NULL Sorting

By default, NULLs sort last in ASC mode (PostgreSQL). You can control it:

```sql
ORDER BY manager_id NULLS FIRST
ORDER BY manager_id NULLS LAST
```

---

# 5. DISTINCT — Remove Duplicate Rows

## What DISTINCT Actually Does

DISTINCT removes duplicate rows from the output.

```sql
SELECT DISTINCT department FROM employees;
```

Result:

```text
Engineering
Sales
HR
```

Without DISTINCT, you'd get Engineering three times, Sales twice, HR once.

## DISTINCT ON (PostgreSQL-Specific)

```sql
SELECT DISTINCT ON (department) department, name, salary
FROM employees
ORDER BY department, salary DESC;
```

This returns one row per department — the one with the highest salary.

```text
Engineering | Eve     | 95000
HR          | Frank   | 65000
Sales       | Diana   | 72000
```

This is a common interview trick in PostgreSQL.

---

# 6. LIMIT and OFFSET — Pagination

```sql
SELECT * FROM orders ORDER BY order_date DESC LIMIT 10;
```

Returns the 10 most recent orders.

```sql
SELECT * FROM orders ORDER BY order_date DESC LIMIT 10 OFFSET 20;
```

Skips the first 20 rows, then returns 10. This is page 3 (page size 10).

## The Performance Problem

```text
OFFSET 10000 LIMIT 10
→ database still scans 10010 rows, then throws away 10000.
→ slow on large tables.

Better: cursor-based pagination
  WHERE id > last_seen_id ORDER BY id LIMIT 10
→ goes directly to the right row using index. Fast.
```

---

# 7. GROUP BY — Collapse Rows Into Groups

This is one of the most important interview concepts.

## What GROUP BY Actually Does

GROUP BY takes many rows and collapses them into groups. After GROUP BY, each group becomes one output row.

```sql
SELECT department, COUNT(*) AS employee_count
FROM employees
GROUP BY department;
```

What happens:

```text
1. FROM employees → 6 rows
2. GROUP BY department → collapse into 3 groups:
     Group 'Engineering': [Alice, Bob, Eve]
     Group 'Sales':       [Charlie, Diana]
     Group 'HR':          [Frank]
3. SELECT department, COUNT(*) → for each group, output department name and count
```

Result:

```text
department  | employee_count
------------+---------------
Engineering | 3
Sales       | 2
HR          | 1
```

## The Critical Rule After GROUP BY

After GROUP BY, you can only SELECT:

- columns that appear in GROUP BY
- aggregate functions (COUNT, SUM, AVG, etc.)

This will FAIL:

```sql
SELECT department, name, COUNT(*)  -- ❌ name is not in GROUP BY
FROM employees
GROUP BY department;
```

Why? Because each group has multiple names. The database does not know which `name` to pick.

## GROUP BY with Multiple Columns

```sql
SELECT department, status, COUNT(*)
FROM employees e
JOIN orders o ON ...
GROUP BY department, status;
```

Now each unique `(department, status)` combination is a group.

---

# 8. Aggregate Functions — COUNT, SUM, AVG, MIN, MAX

## What They Do

Aggregate functions take many values and return one value.

```sql
SELECT
    COUNT(*)        AS total_employees,   -- count of all rows
    COUNT(manager_id) AS has_manager,     -- count of non-NULL values
    SUM(salary)     AS total_salary,
    AVG(salary)     AS avg_salary,
    MIN(salary)     AS min_salary,
    MAX(salary)     AS max_salary
FROM employees;
```

Result:

```text
total_employees | has_manager | total_salary | avg_salary | min_salary | max_salary
6               | 3           | 477000       | 79500      | 65000      | 95000
```

## COUNT(*) vs COUNT(column) vs COUNT(DISTINCT column)

```text
COUNT(*)              → counts all rows, including NULLs
COUNT(manager_id)     → counts only rows where manager_id is NOT NULL
COUNT(DISTINCT department) → counts unique departments
```

This is a common interview trap.

---

# 9. HAVING — Filter After Grouping

## What HAVING Actually Does

HAVING filters groups. WHERE filters individual rows before grouping. HAVING filters after grouping.

```sql
SELECT department, AVG(salary) AS avg_salary
FROM employees
GROUP BY department
HAVING AVG(salary) > 75000;
```

What happens:

```text
1. FROM → 6 rows
2. GROUP BY department → 3 groups
3. AVG(salary) calculated per group:
     Engineering: 90000
     Sales:       71000
     HR:          65000
4. HAVING AVG(salary) > 75000 → only Engineering passes
5. SELECT → output that one group
```

Result:

```text
department  | avg_salary
------------+-----------
Engineering | 90000
```

## WHERE vs HAVING — The Interview Answer

```text
WHERE  filters ROWS    → runs BEFORE GROUP BY
HAVING filters GROUPS  → runs AFTER GROUP BY

WHERE cannot use aggregates.   ❌ WHERE COUNT(*) > 2
HAVING can use aggregates.     ✅ HAVING COUNT(*) > 2
```

---

# 10. JOINs — Combining Tables

## What a JOIN Actually Does

A JOIN combines rows from two tables based on a condition.

## INNER JOIN

Returns only rows that have a match in both tables.

```sql
SELECT c.name, o.id AS order_id, o.amount
FROM customers c
INNER JOIN orders o ON c.id = o.customer_id;
```

```text
If customer has no orders → excluded.
If order has no matching customer → excluded.
Only matching pairs appear.
```

## LEFT JOIN (LEFT OUTER JOIN)

Returns all rows from the left table, even if there is no match on the right.

```sql
SELECT c.name, o.id AS order_id, o.amount
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id;
```

```text
Customer with no orders → appears with NULL for order columns.
Every customer appears at least once.
```

This is the most common join in practice. "Give me all customers and their orders if any."

## RIGHT JOIN

Same idea but reversed. All rows from the right table, even if no match on the left.

Rarely used in practice. You can just swap tables and use LEFT JOIN.

## FULL OUTER JOIN

Returns all rows from both tables. NULLs fill in where there is no match.

```sql
SELECT c.name, o.id AS order_id
FROM customers c
FULL OUTER JOIN orders o ON c.id = o.customer_id;
```

## CROSS JOIN

Every row from table A paired with every row from table B. No ON condition.

```sql
SELECT e.name, d.department
FROM employees e
CROSS JOIN departments d;
```

If employees has 6 rows and departments has 3 rows, result has 18 rows.

Use case: generating all combinations (e.g., all employees × all possible shifts).

## SELF JOIN

A table joined to itself. Uses aliases.

```sql
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

Result:

```text
employee | manager
---------+---------
Alice    | NULL      (no manager)
Bob      | Alice
Charlie  | NULL
Diana    | Charlie
Eve      | Alice
Frank    | NULL
```

This is the classic "find employee and their manager" question.

---

# 11. Subqueries — Queries Inside Queries

## Scalar Subquery (Returns One Value)

```sql
SELECT name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);
```

The inner query returns one number (79500). The outer query uses it to filter.

## Column Subquery (Returns a List)

```sql
SELECT name
FROM customers
WHERE id IN (SELECT DISTINCT customer_id FROM orders);
```

Returns customers who have placed at least one order.

## Correlated Subquery (References the Outer Query)

```sql
SELECT e.name, e.salary, e.department
FROM employees e
WHERE e.salary > (
    SELECT AVG(e2.salary)
    FROM employees e2
    WHERE e2.department = e.department
);
```

For each row in the outer query, the inner query runs with that row's department.

Result: employees who earn more than their department average.

This is slower than a JOIN or window function approach but is a very common interview question.

---

# 12. EXISTS and IN — Membership Tests

## IN

```sql
SELECT name FROM customers
WHERE id IN (SELECT customer_id FROM orders WHERE amount > 500);
```

Checks if the id is in the list returned by the subquery.

## EXISTS

```sql
SELECT name FROM customers c
WHERE EXISTS (
    SELECT 1 FROM orders o
    WHERE o.customer_id = c.id AND o.amount > 500
);
```

Returns TRUE if the subquery returns at least one row.

## IN vs EXISTS — When to Use Which

```text
IN:
  subquery runs once, builds a list, outer query checks membership
  good when inner result set is small

EXISTS:
  for each outer row, check if inner query returns anything
  good when outer table is small and inner is large with an index
  stops as soon as it finds one match (short-circuits)

In practice for large tables, EXISTS is often faster because it can short-circuit.
```

---

# 13. CASE — Conditional Logic Inside a Query

## What CASE Does

CASE lets you add if/else logic inside SQL.

```sql
SELECT name, salary,
    CASE
        WHEN salary >= 90000 THEN 'Senior'
        WHEN salary >= 70000 THEN 'Mid'
        ELSE 'Junior'
    END AS level
FROM employees;
```

Result:

```text
Alice   | 90000 | Senior
Bob     | 85000 | Mid
Charlie | 70000 | Mid
Diana   | 72000 | Mid
Eve     | 95000 | Senior
Frank   | 65000 | Junior
```

## CASE in GROUP BY

```sql
SELECT
    CASE WHEN salary >= 80000 THEN 'High' ELSE 'Normal' END AS bracket,
    COUNT(*) AS count
FROM employees
GROUP BY
    CASE WHEN salary >= 80000 THEN 'High' ELSE 'Normal' END;
```

Result:

```text
bracket | count
--------+------
High    | 3
Normal  | 3
```

---

# 14. Window Functions — The Big Interview Topic

This section matters the most for interviews. Window functions are the #1 SQL interview differentiator.

## 14.1 What a Window Function Actually Does

A window function performs a calculation **across a set of rows related to the current row**, but **without collapsing the rows**.

That is the core difference from GROUP BY.

```text
GROUP BY:   many rows → one row per group
WINDOW:     many rows → same number of rows, each with additional computed value
```

## 14.2 The Syntax

```sql
function_name() OVER (
    PARTITION BY column     -- optional: divide rows into groups
    ORDER BY column         -- optional: order within each group
)
```

Think of OVER as saying: "apply this function over a window of rows."

## 14.3 ROW_NUMBER()

Assigns a sequential number to each row within a partition.

```sql
SELECT name, department, salary,
    ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
FROM employees;
```

What happens:

```text
Partition by department creates windows:
  Engineering: Eve(95000), Alice(90000), Bob(85000)
  HR:          Frank(65000)
  Sales:       Diana(72000), Charlie(70000)

Within each window, ORDER BY salary DESC:
  Engineering: Eve → rn=1, Alice → rn=2, Bob → rn=3
  HR:          Frank → rn=1
  Sales:       Diana → rn=1, Charlie → rn=2
```

Result:

```text
name    | department  | salary | rn
--------+-------------+--------+---
Eve     | Engineering | 95000  | 1
Alice   | Engineering | 90000  | 2
Bob     | Engineering | 85000  | 3
Frank   | HR          | 65000  | 1
Diana   | Sales       | 72000  | 1
Charlie | Sales       | 70000  | 2
```

## 14.4 RANK()

Like ROW_NUMBER but gives the same rank to ties, then skips.

```sql
-- Imagine two people with salary 90000
RANK() OVER (ORDER BY salary DESC)
```

```text
95000 → rank 1
90000 → rank 2
90000 → rank 2    ← same rank (tie)
85000 → rank 4    ← rank 3 is SKIPPED
```

## 14.5 DENSE_RANK()

Like RANK but does NOT skip after ties.

```sql
DENSE_RANK() OVER (ORDER BY salary DESC)
```

```text
95000 → dense_rank 1
90000 → dense_rank 2
90000 → dense_rank 2   ← same rank (tie)
85000 → dense_rank 3   ← NO skip, next is 3
```

## 14.6 ROW_NUMBER vs RANK vs DENSE_RANK — The Interview Summary

```text
ROW_NUMBER: always unique, no ties          1, 2, 3, 4, 5
RANK:       ties get same rank, then skip   1, 2, 2, 4, 5
DENSE_RANK: ties get same rank, no skip     1, 2, 2, 3, 4
```

## 14.7 LAG() and LEAD()

Access a value from a previous or next row.

```sql
SELECT name, salary,
    LAG(salary, 1) OVER (ORDER BY salary) AS prev_salary,
    LEAD(salary, 1) OVER (ORDER BY salary) AS next_salary
FROM employees;
```

Result:

```text
name    | salary | prev_salary | next_salary
--------+--------+-------------+------------
Frank   | 65000  | NULL        | 70000
Charlie | 70000  | 65000       | 72000
Diana   | 72000  | 70000       | 85000
Bob     | 85000  | 72000       | 90000
Alice   | 90000  | 85000       | 95000
Eve     | 95000  | 90000       | NULL
```

Use case: "compare each row to the previous row" — e.g., month-over-month revenue change.

## 14.8 SUM() OVER — Running Total

```sql
SELECT order_date, amount,
    SUM(amount) OVER (ORDER BY order_date) AS running_total
FROM orders;
```

```text
order_date | amount | running_total
-----------+--------+--------------
2024-01-01 | 100    | 100
2024-01-02 | 250    | 350
2024-01-03 | 75     | 425
2024-01-04 | 200    | 625
```

Each row shows the cumulative sum up to that row.

## 14.9 SUM() OVER with PARTITION BY

```sql
SELECT department, name, salary,
    SUM(salary) OVER (PARTITION BY department) AS dept_total
FROM employees;
```

```text
department  | name    | salary | dept_total
------------+---------+--------+-----------
Engineering | Alice   | 90000  | 270000
Engineering | Bob     | 85000  | 270000
Engineering | Eve     | 95000  | 270000
Sales       | Charlie | 70000  | 142000
Sales       | Diana   | 72000  | 142000
HR          | Frank   | 65000  | 65000
```

Every row keeps its own identity. The department total is added as extra information.

## 14.10 AVG() OVER — Department Average Next to Each Row

```sql
SELECT name, department, salary,
    ROUND(AVG(salary) OVER (PARTITION BY department), 2) AS dept_avg
FROM employees;
```

This lets you compare each employee's salary to their department average without GROUP BY.

## 14.11 NTILE() — Divide Into Buckets

```sql
SELECT name, salary,
    NTILE(3) OVER (ORDER BY salary DESC) AS bucket
FROM employees;
```

Divides 6 employees into 3 equal buckets:

```text
Eve     | 95000 | 1
Alice   | 90000 | 1
Bob     | 85000 | 2
Diana   | 72000 | 2
Charlie | 70000 | 3
Frank   | 65000 | 3
```

Use case: "find the top 25% earners" → NTILE(4), bucket = 1.

## 14.12 The Classic Interview Query: Top N per Group

"Find the highest-paid employee in each department."

```sql
WITH ranked AS (
    SELECT name, department, salary,
        ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
    FROM employees
)
SELECT name, department, salary
FROM ranked
WHERE rn = 1;
```

Result:

```text
Eve   | Engineering | 95000
Frank | HR          | 65000
Diana | Sales       | 72000
```

This pattern (ROW_NUMBER + CTE + WHERE rn = N) is the most asked SQL interview pattern.

---

# 15. CTEs — WITH Clause

## What a CTE Does

CTE (Common Table Expression) is a named temporary result set.

```sql
WITH high_earners AS (
    SELECT name, salary, department
    FROM employees
    WHERE salary > 80000
)
SELECT department, COUNT(*) AS count
FROM high_earners
GROUP BY department;
```

The CTE `high_earners` is like a temporary table that exists for the duration of this query.

## Why CTEs Are Better Than Nested Subqueries

```text
Readability: each step has a name
Reusability: reference the CTE multiple times
Debugging: test each CTE independently
```

## Multiple CTEs

```sql
WITH
dept_stats AS (
    SELECT department, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
),
above_avg AS (
    SELECT e.name, e.department, e.salary, d.avg_salary
    FROM employees e
    JOIN dept_stats d ON e.department = d.department
    WHERE e.salary > d.avg_salary
)
SELECT * FROM above_avg;
```

## Recursive CTE — Walk a Hierarchy

"Find all reports under Alice (employee id=1), at any level."

```sql
WITH RECURSIVE reports AS (
    -- base case: Alice herself
    SELECT id, name, manager_id, 0 AS level
    FROM employees
    WHERE id = 1

    UNION ALL

    -- recursive step: find employees whose manager is in the result
    SELECT e.id, e.name, e.manager_id, r.level + 1
    FROM employees e
    JOIN reports r ON e.manager_id = r.id
)
SELECT * FROM reports;
```

Result:

```text
id | name  | manager_id | level
---+-------+------------+------
1  | Alice | NULL       | 0
2  | Bob   | 1          | 1
5  | Eve   | 1          | 1
```

Recursive CTEs are the tool for hierarchy / tree traversal in SQL.

---

# 16. UNION, INTERSECT, EXCEPT — Set Operations

## UNION

Combines results of two queries. Removes duplicates.

```sql
SELECT name FROM customers WHERE city = 'Delhi'
UNION
SELECT name FROM customers WHERE city = 'Mumbai';
```

If someone is in both cities (unlikely but possible), they appear once.

## UNION ALL

Same but keeps duplicates. Faster because no deduplication step.

## INTERSECT

Returns only rows that appear in both queries.

```sql
SELECT customer_id FROM orders WHERE status = 'completed'
INTERSECT
SELECT customer_id FROM orders WHERE amount > 500;
```

Customers who have both a completed order AND an order over 500.

## EXCEPT

Returns rows from the first query that are NOT in the second.

```sql
SELECT id FROM customers
EXCEPT
SELECT customer_id FROM orders;
```

Customers who have never placed an order.

---

# 17. COALESCE, NULLIF, and NULL Handling

## COALESCE

Returns the first non-NULL value.

```sql
SELECT name, COALESCE(manager_id, 0) AS manager_id
FROM employees;
```

If manager_id is NULL, returns 0 instead.

Common use: provide defaults.

```sql
SELECT COALESCE(nickname, first_name, 'Unknown') AS display_name
FROM users;
```

## NULLIF

Returns NULL if two values are equal. Otherwise returns the first value.

```sql
SELECT NULLIF(total, 0)  -- returns NULL if total is 0
```

Common use: avoid division by zero.

```sql
SELECT revenue / NULLIF(cost, 0) AS margin
```

If cost is 0, NULLIF returns NULL, so the division returns NULL instead of crashing.

---

# 18. INSERT, UPDATE, DELETE, UPSERT

## INSERT

```sql
INSERT INTO customers (name, city, email)
VALUES ('Ravi', 'Hyderabad', 'ravi@example.com');
```

## INSERT from SELECT

```sql
INSERT INTO archived_orders (id, customer_id, amount)
SELECT id, customer_id, amount
FROM orders
WHERE order_date < '2023-01-01';
```

## UPDATE

```sql
UPDATE employees
SET salary = salary * 1.10
WHERE department = 'Engineering';
```

## DELETE

```sql
DELETE FROM orders WHERE status = 'cancelled';
```

## UPSERT (INSERT ... ON CONFLICT — PostgreSQL)

```sql
INSERT INTO customers (id, name, city, email)
VALUES (1, 'Ravi', 'Hyderabad', 'ravi@example.com')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    city = EXCLUDED.city,
    email = EXCLUDED.email;
```

If id=1 already exists, update the row instead of throwing an error.

This is the correct way to handle "insert if not exists, update if exists" in one statement.

---

# 19. Indexing — Why Queries Are Fast or Slow

## 19.1 The Story

Without an index, the database reads every row in the table to find matches. That is a **full table scan**.

With an index, the database uses a shortcut to jump directly to matching rows.

Real-life analogy:

```text
Table without index = reading a book from page 1 to find a topic.
Table with index = checking the book index at the back, then turning to page 247.
```

## 19.2 B-Tree Index (Default)

The most common index type. Works like a sorted tree.

```sql
CREATE INDEX idx_employees_department ON employees(department);
```

Now queries filtering by department can use this index:

```sql
SELECT * FROM employees WHERE department = 'Engineering';
```

Instead of scanning all rows, the database navigates the tree and finds matching rows directly.

## 19.3 Composite Index

An index on multiple columns.

```sql
CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date);
```

This index helps queries that filter by customer_id, or by customer_id AND order_date.

```text
✅ WHERE customer_id = 5
✅ WHERE customer_id = 5 AND order_date > '2024-01-01'
❌ WHERE order_date > '2024-01-01'   (left column not used → index may not help)
```

This is the **leftmost prefix rule**. The index is useful for queries that use columns from left to right.

## 19.4 Covering Index

An index that contains all the columns the query needs. The database can answer the query entirely from the index without reading the table.

```sql
CREATE INDEX idx_covering ON orders(customer_id, order_date, amount);

SELECT order_date, amount
FROM orders
WHERE customer_id = 5;
```

All data needed is in the index. No table lookup required. This is called an **index-only scan**.

## 19.5 When NOT to Index

```text
Small tables (< 1000 rows): full scan is often faster.
Columns with very low cardinality: e.g., boolean or status with 2-3 values.
  Index doesn't help much when most rows match.
Write-heavy tables: every INSERT/UPDATE/DELETE must update the index too.
Too many indexes: each index costs write performance and storage.
```

## 19.6 Unique Index

```sql
CREATE UNIQUE INDEX idx_customers_email ON customers(email);
```

Enforces uniqueness AND speeds up lookups. This is what UNIQUE constraints create internally.

## 19.7 Partial Index (PostgreSQL)

```sql
CREATE INDEX idx_active_orders ON orders(customer_id)
WHERE status = 'pending';
```

Only indexes rows where status is pending. Smaller index, faster for queries specifically targeting pending orders.

---

# 20. Query Optimization — Reading EXPLAIN

## 20.1 The Story

Your query is slow. How do you find out why?

Answer: **EXPLAIN ANALYZE**.

## 20.2 How to Read EXPLAIN

```sql
EXPLAIN ANALYZE
SELECT * FROM orders WHERE customer_id = 5;
```

Output (simplified):

```text
Index Scan using idx_orders_customer on orders
  Index Cond: (customer_id = 5)
  Rows Removed by Filter: 0
  Actual rows: 12
  Planning Time: 0.1 ms
  Execution Time: 0.3 ms
```

Key things to look at:

```text
Seq Scan        → full table scan, reads every row       → often slow on large tables
Index Scan      → uses index to find rows                → fast
Index Only Scan → answers entirely from index            → fastest
Bitmap Index Scan → index identifies matching pages, then reads those pages
Nested Loop     → for each row in table A, scan table B  → slow if inner is large
Hash Join       → build hash table from one side, probe with other → good for large joins
Merge Join      → both sides sorted, merge them          → good when both large and sorted
Sort            → explicit sort step                     → memory or disk (expensive)
```

## 20.3 Common Causes of Slow Queries

```text
1. Missing index on WHERE or JOIN column
   Fix: CREATE INDEX

2. Using function on indexed column
   WHERE LOWER(name) = 'alice'   → index on name is NOT used
   Fix: CREATE INDEX idx ON t (LOWER(name))  (functional index)

3. Implicit type conversion
   WHERE id = '5'   → id is INT, '5' is VARCHAR, cast may break index use
   Fix: use correct types

4. SELECT * when you only need 2 columns
   Fix: SELECT only needed columns

5. N+1 query problem from ORM
   App runs 1 query for list + N queries for details
   Fix: use JOIN or batch fetch (Spring JPA: @EntityGraph, JOIN FETCH)

6. Large OFFSET pagination
   Fix: cursor-based pagination

7. Missing ANALYZE (stale statistics)
   Fix: ANALYZE table_name;  (updates planner stats)
```

---

# 21. Transactions, ACID, and Isolation Levels

## 21.1 ACID

```text
Atomicity:    all statements succeed or all fail
Consistency:  data stays valid according to constraints
Isolation:    concurrent transactions don't interfere
Durability:   committed data survives crashes
```

## 21.2 Transaction Basics

```sql
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```

If the second UPDATE fails, ROLLBACK undoes the first too.

## 21.3 Isolation Levels

```text
Read Uncommitted:
  Can see uncommitted changes from other transactions (dirty reads).
  Almost never used in practice.

Read Committed (PostgreSQL default):
  Can only see committed data.
  But if you read the same row twice in one transaction, the value may change
  if another transaction committed in between (non-repeatable read).

Repeatable Read:
  Once you read a row, you see the same value for the rest of your transaction.
  No dirty reads, no non-repeatable reads.
  But phantom rows (new rows inserted by others) may appear.

Serializable:
  Strongest level.
  Transactions behave as if they ran one after another.
  Slowest, most locking.
```

## 21.4 Interview Summary

```text
Level               | Dirty Read | Non-Repeatable | Phantom
--------------------|------------|----------------|--------
Read Uncommitted    | Yes        | Yes            | Yes
Read Committed      | No         | Yes            | Yes
Repeatable Read     | No         | No             | Yes
Serializable        | No         | No             | No
```

---

# 22. Normalization and Denormalization

## 22.1 Normalization

Normalization reduces data redundancy by separating data into focused tables.

```text
1NF: no repeating groups, each cell has one value
2NF: all non-key columns depend on the whole primary key
3NF: no non-key column depends on another non-key column
```

Example of violating 3NF:

```text
orders table with: customer_name, customer_city
→ customer info should be in customers table, not duplicated in orders
```

## 22.2 Denormalization

Sometimes you deliberately duplicate data to make reads faster.

Example:

```text
Instead of joining orders + customers + products every time,
store a precomputed orders_summary table with customer_name and product_name.
```

Trade-off:

```text
Normalize: less redundancy, easier updates, more joins
Denormalize: faster reads, fewer joins, harder to keep consistent
```

## 22.3 Star Schema (BI Context)

Surendra's domain. Star schema has:

```text
Fact table:      orders_fact (measures: amount, quantity)
Dimension tables: dim_customer, dim_product, dim_date
```

Fact table in the center, dimensions around it like a star. Optimized for analytical queries.

---

# 23. Classic Interview Query Patterns

## 23.1 Second Highest Salary

```sql
SELECT MAX(salary) FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);
```

Or using DENSE_RANK:

```sql
WITH ranked AS (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS dr
    FROM employees
)
SELECT DISTINCT salary FROM ranked WHERE dr = 2;
```

## 23.2 Nth Highest Salary

```sql
WITH ranked AS (
    SELECT DISTINCT salary,
        DENSE_RANK() OVER (ORDER BY salary DESC) AS dr
    FROM employees
)
SELECT salary FROM ranked WHERE dr = :n;
```

## 23.3 Employees Earning More Than Their Manager

```sql
SELECT e.name AS employee, e.salary, m.name AS manager, m.salary AS manager_salary
FROM employees e
JOIN employees m ON e.manager_id = m.id
WHERE e.salary > m.salary;
```

## 23.4 Departments With More Than 2 Employees

```sql
SELECT department, COUNT(*) AS count
FROM employees
GROUP BY department
HAVING COUNT(*) > 2;
```

## 23.5 Customers Who Never Ordered

```sql
SELECT c.name
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
WHERE o.id IS NULL;
```

Or:

```sql
SELECT name FROM customers
WHERE id NOT IN (SELECT customer_id FROM orders WHERE customer_id IS NOT NULL);
```

## 23.6 Duplicate Rows

```sql
SELECT email, COUNT(*)
FROM customers
GROUP BY email
HAVING COUNT(*) > 1;
```

## 23.7 Running Total by Date

```sql
SELECT order_date, amount,
    SUM(amount) OVER (ORDER BY order_date) AS running_total
FROM orders;
```

## 23.8 Month-Over-Month Growth

```sql
WITH monthly AS (
    SELECT DATE_TRUNC('month', order_date) AS month,
           SUM(amount) AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('month', order_date)
)
SELECT month, revenue,
    LAG(revenue) OVER (ORDER BY month) AS prev_month,
    ROUND((revenue - LAG(revenue) OVER (ORDER BY month))
        / LAG(revenue) OVER (ORDER BY month) * 100, 2) AS growth_pct
FROM monthly;
```

## 23.9 Top N per Group

```sql
WITH ranked AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
    FROM employees
)
SELECT * FROM ranked WHERE rn <= 3;
```

Top 3 earners per department.

## 23.10 Delete Duplicates, Keep One

```sql
DELETE FROM customers
WHERE id NOT IN (
    SELECT MIN(id) FROM customers GROUP BY email
);
```

Keeps the row with the smallest id for each email, deletes the rest.

## 23.11 Consecutive Days Active

```sql
WITH numbered AS (
    SELECT user_id, login_date,
        login_date - INTERVAL '1 day' * ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS grp
    FROM user_logins
)
SELECT user_id, COUNT(*) AS streak, MIN(login_date), MAX(login_date)
FROM numbered
GROUP BY user_id, grp
HAVING COUNT(*) >= 3;
```

This finds users with 3+ consecutive login days. The trick: subtracting row_number from date creates a constant for consecutive sequences.

## 23.12 Pivot (Manual)

```sql
SELECT department,
    COUNT(*) FILTER (WHERE salary >= 80000) AS high_earners,
    COUNT(*) FILTER (WHERE salary < 80000) AS normal_earners
FROM employees
GROUP BY department;
```

PostgreSQL FILTER clause makes this clean. Without FILTER, use CASE inside SUM.

---

# 24. Quick Revision Sheet

## SQL Execution Order

This is the most important thing to memorize. SQL does not run in the order you write it.

```text
Written order:     SELECT → FROM → WHERE → GROUP BY → HAVING → ORDER BY → LIMIT
Execution order:   FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
```

That is why WHERE cannot use column aliases defined in SELECT.
That is why HAVING can use aggregates but WHERE cannot.

## One-Line Summaries

```text
WHERE       = filter rows before grouping
GROUP BY    = collapse rows into groups
HAVING      = filter groups after grouping
ORDER BY    = sort the final output
LIMIT       = restrict how many rows are returned
JOIN        = combine rows from multiple tables
INNER JOIN  = only matching rows from both sides
LEFT JOIN   = all rows from left, matching from right
SUBQUERY    = query nested inside another query
CTE         = named temporary result set (WITH clause)
CASE        = conditional logic inside a query
ROW_NUMBER  = unique sequential number per partition         1,2,3,4,5
RANK        = same number for ties, then skip                1,2,2,4,5
DENSE_RANK  = same number for ties, no skip                  1,2,2,3,4
LAG/LEAD    = access previous/next row value
SUM() OVER  = running or partitioned total without collapsing rows
NTILE       = divide rows into N equal buckets
INDEX       = shortcut structure for fast lookups
EXPLAIN     = show how the database plans to run your query
TRANSACTION = group of statements that succeed or fail together
```

## Gold Standard Interview Response

```text
I approach SQL queries by first understanding the execution order: FROM reads the data, WHERE filters rows, GROUP BY collapses groups, HAVING filters groups, SELECT picks columns, ORDER BY sorts, and LIMIT caps the output. For analytics, I use window functions like ROW_NUMBER and DENSE_RANK because they operate across rows without collapsing them. For performance, I use EXPLAIN ANALYZE to identify missing indexes or full table scans, and I prefer covering indexes and cursor-based pagination for large datasets.
```
