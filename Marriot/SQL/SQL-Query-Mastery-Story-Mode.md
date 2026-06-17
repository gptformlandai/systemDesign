# SQL Query Mastery Through Story Mode

> You have a Spring Boot app with PostgreSQL. Every feature you build eventually turns into a SQL query. This guide explains SQL through real queries — what each clause actually does to your rows, how operators like GROUP BY, HAVING, RANK, DENSE_RANK change the result, and how indexing and optimization make things fast. Built for interview query-solving, not textbook theory.

---

# Table of Contents

0. [Interview Command Center](#0-interview-command-center)
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
25. [Interview Query Pattern Playbook](#25-interview-query-pattern-playbook)
26. [SQL Hot Interview Questions and Strong Answers](#26-sql-hot-interview-questions-and-strong-answers)
27. [Performance and Indexing Master Checklist](#27-performance-and-indexing-master-checklist)
28. [Transactions, Locks, and Concurrency Interview Deep Dive](#28-transactions-locks-and-concurrency-interview-deep-dive)
29. [Backend and Spring Boot SQL Interview Mapping](#29-backend-and-spring-boot-sql-interview-mapping)
30. [PostgreSQL Features Worth Knowing](#30-postgresql-features-worth-knowing)
31. [Final SQL Drill Bank](#31-final-sql-drill-bank)
32. [One-Hour SQL Revision Plan](#32-one-hour-sql-revision-plan)
33. [Wrong Query Clinic — Mistakes, Why Wrong, Correct Query](#33-wrong-query-clinic--mistakes-why-wrong-correct-query)
34. [EXPLAIN ANALYZE Walkthroughs](#34-explain-analyze-walkthroughs)
35. [Schema Design Mini-Cases](#35-schema-design-mini-cases)
36. [MAANG-Style SQL Capstone Problems](#36-maang-style-sql-capstone-problems)
37. [Final Master Checklist](#37-final-master-checklist)

---

# 0. Interview Command Center

Use this section as the first and last revision pass before any SQL interview.

## SQL Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Logical execution order | Very high | Prevents wrong WHERE/HAVING/alias answers |
| Joins | Very high | Almost every real query crosses tables |
| GROUP BY and HAVING | Very high | Tests aggregation clarity |
| Window functions | Very high | Top N, ranking, running totals, latest row |
| Subqueries, CTEs, EXISTS | High | Tests query decomposition |
| NULL handling | High | Causes subtle production bugs |
| Indexes | Very high | Backend roles must reason about performance |
| EXPLAIN / EXPLAIN ANALYZE | High | Shows practical debugging ability |
| Transactions and isolation | High | Tests data correctness under concurrency |
| Normalization | Medium-high | Schema design fundamentals |
| DML: INSERT/UPDATE/DELETE/UPSERT | Medium-high | Practical backend SQL |
| PostgreSQL-specific features | Medium | Useful because JD mentions PostgreSQL |

## How To Use This Guide By Level

This file is intentionally large. Do not try to memorize it line by line on the first pass.
Use it differently depending on your current level.

| Level | What to focus on first | What to skip temporarily | When you are ready for the next level |
|---|---|---|---|
| Beginner | execution order, `WHERE`, `JOIN`, `GROUP BY`, `HAVING`, `ORDER BY`, `LIMIT` | deep indexing, isolation levels, advanced capstones | you can explain what one output row represents before writing SQL |
| Intermediate | window functions, CTEs, subqueries, `EXISTS`, NULL traps, interview query patterns | advanced PostgreSQL internals | you can solve top-N, latest-row, duplicate, running-total, and missing-relationship queries |
| Pro backend | indexes, sargability, `EXPLAIN ANALYZE`, transactions, locking, idempotent inserts | only database-internals trivia | you can debug slow queries and defend database constraints under concurrency |
| MAANG-style | query decomposition, trade-offs, correctness under race conditions, schema design, capstones | memorizing syntax without reasoning | you can discuss grain, correctness, performance, and operational risk in one answer |

Recommended study loop:

```text
Read concept → write query without looking → explain grain → explain execution order
→ explain index/transaction impact → rewrite once for clarity/performance.
```

## What Makes A SQL Answer Senior

A strong SQL interview answer has five parts:

```text
1. Grain:
   "The output should be one row per customer per month."

2. Data path:
   "I start from orders because orders define revenue."

3. Correctness:
   "I use LEFT JOIN because customers with no orders must still appear."

4. Query shape:
   "I aggregate monthly revenue first, then use LAG to compare with previous month."

5. Performance:
   "For production, I would index customer_id and order_date, and validate with EXPLAIN ANALYZE."
```

If you say these five things before or while writing SQL, you sound controlled and practical.

## Active Study Rules

- Never just read SQL. Type it.
- For every query, say the output grain out loud.
- For every join, say whether unmatched rows should survive.
- For every aggregation, say whether you need `WHERE`, `HAVING`, or both.
- For every window function, say whether rows should stay uncollapsed.
- For every slow query, first reduce rows, then check indexes.
- For every concurrent workflow, ask: "What protects correctness if two requests happen at the same time?"

## The Query-Solving Framework

When given a SQL problem, think in this order:

```text
1. What is the output grain?
   One row per employee? Per department? Per customer per month?

2. What tables are needed?
   Start from the entity that defines the output grain.

3. What joins are needed?
   INNER JOIN if matching records are required.
   LEFT JOIN if missing related records must still appear.

4. What filters apply before grouping?
   Put row-level filters in WHERE.

5. Do I need aggregation?
   If yes, GROUP BY the output grain.

6. Do I need to filter aggregate results?
   Put group-level filters in HAVING.

7. Do I need ranking, latest row, running total, or comparison with previous row?
   Use window functions.

8. Do I need readability?
   Use CTEs to split the query into named steps.

9. Do I need performance?
   Check indexes, sargability, row counts, and EXPLAIN ANALYZE.
```

## The Most Important Word: Grain

Grain means:

```text
What does one output row represent?
```

Examples:

| Requirement | Output Grain |
|---|---|
| Total salary by department | One row per department |
| Top 3 salaries per department | One row per employee inside each department rank |
| Monthly revenue | One row per month |
| Customers who never ordered | One row per customer |
| Latest order per customer | One row per customer |

If you identify the grain correctly, the query usually becomes clear.

## Clause Selection Mind Map

| Need | Use |
|---|---|
| Filter raw rows | `WHERE` |
| Filter grouped result | `HAVING` |
| Combine tables | `JOIN` |
| Keep unmatched left-side rows | `LEFT JOIN` |
| Find missing relationship | `LEFT JOIN ... IS NULL` or `NOT EXISTS` |
| Collapse rows into summary | `GROUP BY` |
| Rank rows | `ROW_NUMBER`, `RANK`, `DENSE_RANK` |
| Pick latest row per group | `ROW_NUMBER() OVER (PARTITION BY ... ORDER BY date DESC)` |
| Compare with previous row | `LAG` |
| Compare with next row | `LEAD` |
| Running total | `SUM(...) OVER (ORDER BY ...)` |
| Conditional count/sum | `CASE` or PostgreSQL `FILTER` |
| Reuse query result | `CTE` |
| Test existence | `EXISTS` |
| Replace NULL | `COALESCE` |
| Avoid divide-by-zero | `NULLIF` |
| Improve lookup speed | Index |

## Gold Rule For Interviews

```text
Do not start writing SQL immediately. First say the grain, then joins, then filters,
then aggregation/windowing.
```

Example answer start:

```text
We need one row per department, so department is the grain. I will start from employees,
group by department, calculate COUNT and AVG salary, then use HAVING if we only need
departments above a threshold.
```

This sounds senior and controlled.

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
NULL = NULL   → UNKNOWN (not TRUE)
NULL != NULL  → UNKNOWN
NULL > 5      → UNKNOWN

In a WHERE filter, only TRUE passes. FALSE and UNKNOWN are both filtered out.
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

A star schema is common in analytics and reporting systems. It has:

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

---

# 25. Interview Query Pattern Playbook

This section is the "if they ask this, write that" part.

The goal is fast recall.

---

## 25.1 Latest Row Per Group

Question:

```text
Find the latest order for each customer.
```

Best pattern:

```sql
WITH ranked AS (
    SELECT o.*,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id
               ORDER BY order_date DESC, id DESC
           ) AS rn
    FROM orders o
)
SELECT *
FROM ranked
WHERE rn = 1;
```

Why `ROW_NUMBER`?

```text
We need exactly one latest row per customer.
```

Tie handling:
- If multiple orders have same date, `id DESC` breaks tie.
- If interviewer wants all tied latest rows, use `RANK()` instead.

---

## 25.2 Top N Per Group

Question:

```text
Find top 3 highest paid employees in each department.
```

```sql
WITH ranked AS (
    SELECT e.*,
           DENSE_RANK() OVER (
               PARTITION BY department
               ORDER BY salary DESC
           ) AS salary_rank
    FROM employees e
)
SELECT *
FROM ranked
WHERE salary_rank <= 3;
```

Use:
- `ROW_NUMBER` if exactly 3 rows per department.
- `DENSE_RANK` if ties should be included.
- `RANK` if ranking gaps matter.

---

## 25.3 Find Missing Relationship

Question:

```text
Find customers who never placed an order.
```

Preferred pattern:

```sql
SELECT c.*
FROM customers c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
);
```

Alternative:

```sql
SELECT c.*
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.id IS NULL;
```

Interview line:

```text
I prefer NOT EXISTS because it is NULL-safe and expresses anti-join intent clearly.
```

---

## 25.4 Duplicate Detection

Question:

```text
Find duplicate customer emails.
```

```sql
SELECT email, COUNT(*) AS duplicate_count
FROM customers
GROUP BY email
HAVING COUNT(*) > 1;
```

Find duplicate rows with IDs:

```sql
SELECT *
FROM customers
WHERE email IN (
    SELECT email
    FROM customers
    GROUP BY email
    HAVING COUNT(*) > 1
);
```

---

## 25.5 Delete Duplicates Safely

Question:

```text
Delete duplicate customers by email, keeping the smallest id.
```

PostgreSQL pattern:

```sql
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY email
               ORDER BY id
           ) AS rn
    FROM customers
)
DELETE FROM customers
WHERE id IN (
    SELECT id
    FROM ranked
    WHERE rn > 1
);
```

Interview safety line:

```text
Before running DELETE, I first run the CTE as a SELECT to verify which rows will be deleted.
```

---

## 25.6 Conditional Aggregation

Question:

```text
Count completed, pending, and cancelled orders per customer.
```

Portable SQL:

```sql
SELECT customer_id,
       SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS completed_count,
       SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) AS pending_count,
       SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) AS cancelled_count
FROM orders
GROUP BY customer_id;
```

PostgreSQL:

```sql
SELECT customer_id,
       COUNT(*) FILTER (WHERE status = 'completed') AS completed_count,
       COUNT(*) FILTER (WHERE status = 'pending') AS pending_count,
       COUNT(*) FILTER (WHERE status = 'cancelled') AS cancelled_count
FROM orders
GROUP BY customer_id;
```

---

## 25.7 Running Total

Question:

```text
Show running revenue by order date.
```

```sql
SELECT order_date,
       amount,
       SUM(amount) OVER (
           ORDER BY order_date, id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS running_revenue
FROM orders;
```

Why specify `ROWS`?

```text
It makes the window frame explicit and avoids surprises with duplicate ORDER BY values.
```

---

## 25.8 Month-Over-Month Growth

Question:

```text
Find monthly revenue growth percentage.
```

```sql
WITH monthly AS (
    SELECT DATE_TRUNC('month', order_date) AS month,
           SUM(amount) AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('month', order_date)
),
with_previous AS (
    SELECT month,
           revenue,
           LAG(revenue) OVER (ORDER BY month) AS previous_revenue
    FROM monthly
)
SELECT month,
       revenue,
       previous_revenue,
       ROUND(
           (revenue - previous_revenue) * 100.0 / NULLIF(previous_revenue, 0),
           2
       ) AS growth_pct
FROM with_previous;
```

Why `NULLIF`?

```text
It prevents divide-by-zero.
```

---

## 25.9 Gaps And Islands: Consecutive Days

Question:

```text
Find users active for at least 3 consecutive days.
```

```sql
WITH distinct_logins AS (
    SELECT DISTINCT user_id, login_date
    FROM user_logins
),
numbered AS (
    SELECT user_id,
           login_date,
           login_date - INTERVAL '1 day' * ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY login_date
           ) AS island_key
    FROM distinct_logins
)
SELECT user_id,
       MIN(login_date) AS start_date,
       MAX(login_date) AS end_date,
       COUNT(*) AS streak_days
FROM numbered
GROUP BY user_id, island_key
HAVING COUNT(*) >= 3;
```

Mind trick:

```text
For consecutive dates, date - row_number stays constant.
```

---

## 25.10 Overlapping Date Ranges

Question:

```text
Find bookings that overlap with a requested date range.
```

Given:

```text
requested_start
requested_end
```

Overlap condition:

```sql
existing_start < requested_end
AND existing_end > requested_start
```

Example:

```sql
SELECT *
FROM bookings
WHERE check_in < :requested_check_out
  AND check_out > :requested_check_in;
```

Interview line:

```text
Two ranges overlap if each range starts before the other range ends.
```

For hotel booking, this is extremely important.

---

## 25.11 Pagination

Offset pagination:

```sql
SELECT *
FROM orders
ORDER BY order_date DESC, id DESC
LIMIT 20 OFFSET 1000;
```

Problem:

```text
Large OFFSET gets slower because database still has to walk past skipped rows.
```

Cursor/keyset pagination:

```sql
SELECT *
FROM orders
WHERE (order_date, id) < (:last_order_date, :last_id)
ORDER BY order_date DESC, id DESC
LIMIT 20;
```

Required index:

```sql
CREATE INDEX idx_orders_date_id
ON orders(order_date DESC, id DESC);
```

Interview line:

```text
For large datasets, cursor-based pagination is usually better than OFFSET pagination.
```

---

## 25.12 Join Multiplication Trap

Problem:

```text
Joining orders to order_items multiplies order rows by item count.
```

Wrong if you want order count:

```sql
SELECT c.id, COUNT(*) AS order_count
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id;
```

This counts items, not orders.

Correct:

```sql
SELECT c.id, COUNT(DISTINCT o.id) AS order_count
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id;
```

Even better, pre-aggregate:

```sql
WITH customer_orders AS (
    SELECT customer_id, COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT c.id, COALESCE(co.order_count, 0) AS order_count
FROM customers c
LEFT JOIN customer_orders co ON co.customer_id = c.id;
```

---

# 26. SQL Hot Interview Questions and Strong Answers

## Q1. What is SQL logical execution order?

```text
FROM and JOIN happen first, then WHERE, GROUP BY, HAVING, SELECT, ORDER BY, and LIMIT.
This matters because WHERE cannot filter aggregate results and usually cannot use SELECT aliases.
```

## Q2. WHERE vs HAVING?

```text
WHERE filters rows before grouping. HAVING filters groups after aggregation.
Use WHERE for row-level conditions and HAVING for aggregate conditions like COUNT(*) > 5.
```

## Q3. INNER JOIN vs LEFT JOIN?

```text
INNER JOIN returns only matching rows from both tables. LEFT JOIN returns all rows from the
left table and matching rows from the right table, with NULLs when no match exists.
```

## Q4. When would you use LEFT JOIN?

```text
When I need to preserve all rows from the left table, even if matching child records do not
exist. Example: customers who may or may not have orders.
```

## Q5. How do you find records in one table but not another?

```text
Use NOT EXISTS or LEFT JOIN with IS NULL. I usually prefer NOT EXISTS because it is NULL-safe
and clearly expresses anti-join intent.
```

## Q6. IN vs EXISTS?

```text
IN compares a value against a result list. EXISTS checks whether a matching row exists.
EXISTS is often better for correlated checks and avoids NULL surprises with NOT IN.
```

## Q7. Why is NOT IN dangerous with NULL?

```text
If the subquery returns NULL, NOT IN can return no rows because comparisons with NULL are
UNKNOWN. NOT EXISTS avoids this issue.
```

## Q8. UNION vs UNION ALL?

```text
UNION removes duplicates, which requires extra work. UNION ALL keeps duplicates and is faster.
Use UNION ALL unless duplicate removal is required.
```

## Q9. COUNT(*) vs COUNT(column)?

```text
COUNT(*) counts rows. COUNT(column) counts non-NULL values in that column.
COUNT(DISTINCT column) counts unique non-NULL values.
```

## Q10. ROW_NUMBER vs RANK vs DENSE_RANK?

```text
ROW_NUMBER gives unique sequence numbers. RANK gives same rank for ties but skips numbers.
DENSE_RANK gives same rank for ties and does not skip numbers.
```

## Q11. When do you use window functions instead of GROUP BY?

```text
Use GROUP BY when you want to collapse rows. Use window functions when you want calculations
across related rows while keeping individual row detail.
```

## Q12. CTE vs subquery?

```text
Both can express nested logic. CTEs improve readability by naming intermediate results and
are useful for multi-step queries or recursion.
```

## Q13. Can CTE improve performance?

```text
Sometimes, but primarily CTEs improve readability. Performance depends on the database and
optimizer. I verify with EXPLAIN ANALYZE.
```

## Q14. What is an index?

```text
An index is a data structure, commonly B-tree, that helps the database find rows faster
without scanning the whole table.
```

## Q15. When should we not create an index?

```text
Avoid indexes on tiny tables, very low-selectivity columns, columns rarely filtered/joined,
or write-heavy tables where index maintenance cost outweighs read benefit.
```

## Q16. What is a composite index?

```text
A composite index uses multiple columns. Column order matters because the leftmost prefix
is most useful for filtering and sorting.
```

## Q17. What is a covering index?

```text
A covering index contains all columns needed by a query, allowing the database to answer
from the index without reading the table heap in many cases.
```

## Q18. What is sargability?

```text
A predicate is sargable when it can use an index efficiently. Avoid wrapping indexed columns
in functions in WHERE clauses unless you have a matching functional index.
```

## Q19. Example of non-sargable query?

Bad:

```sql
WHERE DATE(order_date) = DATE '2026-04-25'
```

Better:

```sql
WHERE order_date >= TIMESTAMP '2026-04-25 00:00:00'
  AND order_date <  TIMESTAMP '2026-04-26 00:00:00'
```

## Q20. What is normalization?

```text
Normalization organizes data to reduce duplication and update anomalies. It improves data
integrity, though highly normalized schemas may require more joins.
```

## Q21. What is denormalization?

```text
Denormalization intentionally stores redundant/precomputed data to improve read performance,
at the cost of extra storage and consistency complexity.
```

## Q22. What is ACID?

```text
Atomicity means all-or-nothing, Consistency means valid state transitions, Isolation means
concurrent transactions do not incorrectly interfere, and Durability means committed data
survives failures.
```

## Q23. What is dirty read?

```text
A dirty read happens when one transaction reads uncommitted changes from another transaction.
```

## Q24. What is phantom read?

```text
A phantom read happens when re-running a range query returns new or missing rows because
another transaction inserted/deleted rows matching the condition.
```

## Q25. How do you debug a slow query?

```text
I use EXPLAIN ANALYZE, check whether the query is doing sequential scans, bad joins, large
sorts, or reading too many rows, then verify indexes, predicate selectivity, and whether the
query can be rewritten to reduce data earlier.
```

---

# 27. Performance and Indexing Master Checklist

## The Performance Mindset

SQL performance is usually about:

```text
Read fewer rows.
Join fewer rows.
Sort fewer rows.
Return fewer rows.
Use the right index.
```

## High-Impact Index Rules

| Rule | Explanation |
|---|---|
| Index join columns | Foreign keys often need indexes for joins |
| Index selective filters | High-cardinality filters benefit more |
| Composite index order matters | Put equality filters first, then range/sort columns |
| Avoid over-indexing | Indexes slow writes and take storage |
| Use partial index for common filtered subset | Example: active records only |
| Use covering index for read-heavy queries | Include selected columns where useful |
| Avoid functions on indexed column | Unless functional index exists |

## Composite Index Example

Query:

```sql
SELECT *
FROM orders
WHERE customer_id = 10
  AND status = 'completed'
ORDER BY order_date DESC;
```

Good index:

```sql
CREATE INDEX idx_orders_customer_status_date
ON orders(customer_id, status, order_date DESC);
```

Why this order?

```text
customer_id and status are equality filters. order_date supports sorting within that filtered set.
```

## Leftmost Prefix Rule

Index:

```sql
CREATE INDEX idx_orders_customer_status_date
ON orders(customer_id, status, order_date);
```

Useful for:

```sql
WHERE customer_id = ?
```

Useful for:

```sql
WHERE customer_id = ? AND status = ?
```

Less useful for:

```sql
WHERE status = ?
```

Because `customer_id` is the leftmost column and is missing.

## Sargable vs Non-Sargable

Bad:

```sql
WHERE LOWER(email) = 'a@example.com'
```

Better options:

```sql
WHERE email = 'a@example.com'
```

or create functional index:

```sql
CREATE INDEX idx_customers_lower_email
ON customers(LOWER(email));
```

## EXPLAIN Terms To Recognize

| Term | Meaning |
|---|---|
| Seq Scan | Reads entire table |
| Index Scan | Uses index and visits table rows |
| Index Only Scan | Uses index without reading table rows, when possible |
| Nested Loop | Good for small outer rows with indexed inner lookup |
| Hash Join | Builds hash table for join, good for larger joins |
| Merge Join | Joins sorted inputs |
| Sort | Explicit sorting step |
| Filter | Rows removed after scan |
| Cost | Planner estimate |
| Actual time | Real execution time in EXPLAIN ANALYZE |

## Slow Query Checklist

1. Did we filter early?
2. Are joins using indexed keys?
3. Are we accidentally multiplying rows?
4. Is `ORDER BY` sorting huge data?
5. Is `OFFSET` too large?
6. Are functions blocking index use?
7. Are statistics stale?
8. Is the query returning more columns than needed?
9. Would pre-aggregation reduce join size?
10. Would a partial/composite/covering index help?

## Common Bad Patterns

Bad:

```sql
SELECT * FROM orders;
```

Better:

```sql
SELECT id, customer_id, amount, status
FROM orders
WHERE customer_id = :customer_id;
```

Bad:

```sql
WHERE EXTRACT(YEAR FROM order_date) = 2026
```

Better:

```sql
WHERE order_date >= DATE '2026-01-01'
  AND order_date <  DATE '2027-01-01'
```

Bad:

```sql
WHERE customer_id::TEXT = '10'
```

Better:

```sql
WHERE customer_id = 10
```

## Index Trade-Off Interview Answer

```text
Indexes improve read performance for filters, joins, and sorts, but they add storage cost
and slow down INSERT, UPDATE, and DELETE because every index must be maintained. I create
indexes based on real query patterns and validate with EXPLAIN ANALYZE.
```

---

# 28. Transactions, Locks, and Concurrency Interview Deep Dive

## Transaction Basics

```sql
BEGIN;

UPDATE accounts
SET balance = balance - 100
WHERE id = 1;

UPDATE accounts
SET balance = balance + 100
WHERE id = 2;

COMMIT;
```

If something fails:

```sql
ROLLBACK;
```

## ACID In Interview Language

| Property | Interview Meaning |
|---|---|
| Atomicity | All operations happen or none happen |
| Consistency | Constraints and valid state are preserved |
| Isolation | Concurrent transactions do not corrupt each other |
| Durability | Committed changes survive crashes |

## Isolation Anomalies

| Anomaly | Meaning |
|---|---|
| Dirty read | Read uncommitted data |
| Non-repeatable read | Same row read twice gives different values |
| Phantom read | Same range query returns different row set |
| Lost update | Two transactions overwrite each other's updates |

## Isolation Levels

| Isolation Level | Prevents |
|---|---|
| Read Uncommitted | Almost nothing; dirty reads possible in theory |
| Read Committed | Prevents dirty reads |
| Repeatable Read | Prevents dirty and non-repeatable reads |
| Serializable | Strongest; behaves like transactions ran one by one |

PostgreSQL note:

```text
PostgreSQL treats Read Uncommitted like Read Committed.
```

## Pessimistic Locking

Use when conflicts are likely and you want to lock rows before update.

```sql
BEGIN;

SELECT *
FROM rooms
WHERE id = :room_id
FOR UPDATE;

UPDATE rooms
SET available = false
WHERE id = :room_id;

COMMIT;
```

## Optimistic Locking

Use a version column.

```sql
UPDATE bookings
SET status = 'CONFIRMED',
    version = version + 1
WHERE id = :id
  AND version = :expected_version;
```

If affected rows = 0:

```text
Someone else updated the row first.
```

## Hotel Booking Double-Booking Problem

Question:

```text
Two users try to book the same room for overlapping dates. How do you prevent double booking?
```

Strong answer:

```text
I would enforce correctness at the database level, not only in application code. Use a
transaction, check overlapping bookings, lock the relevant room or availability row, and
ideally enforce a constraint or exclusion constraint where supported. Application checks
alone can race.
```

PostgreSQL exclusion constraint idea:

```sql
-- Conceptual PostgreSQL approach for date-range overlap prevention
-- Requires suitable extensions/types depending schema.
-- Prevent same room from having overlapping booked date ranges.
```

Interview-safe version:

```sql
BEGIN;

SELECT id
FROM rooms
WHERE id = :room_id
FOR UPDATE;

SELECT 1
FROM bookings
WHERE room_id = :room_id
  AND check_in < :requested_check_out
  AND check_out > :requested_check_in
  AND status IN ('CONFIRMED', 'HELD');

-- If no row exists, insert booking.

INSERT INTO bookings(room_id, check_in, check_out, status)
VALUES (:room_id, :requested_check_in, :requested_check_out, 'CONFIRMED');

COMMIT;
```

## Deadlocks

Deadlock example:

```text
Transaction A locks row 1, then waits for row 2.
Transaction B locks row 2, then waits for row 1.
```

Prevention:
- Lock rows in consistent order.
- Keep transactions short.
- Avoid user/network calls inside transaction.
- Add proper indexes so updates lock fewer rows.
- Retry on deadlock errors.

## Transaction Interview Traps

| Trap | Correct View |
|---|---|
| Transactions are only for money transfer | Any multi-step data consistency flow needs transactions |
| Application check is enough for uniqueness | Database constraint is safer |
| Higher isolation is always better | It can reduce concurrency and increase retries |
| Long transaction is fine | It holds locks and hurts concurrency |
| Deadlocks mean database is broken | Deadlocks are possible; handle with ordering and retries |

---

# 29. Backend and Spring Boot SQL Interview Mapping

## Common Backend SQL Problems

| Backend Problem | SQL Concept |
|---|---|
| Search API with filters | WHERE, indexes, dynamic predicates |
| Paginated API | ORDER BY + LIMIT, cursor pagination |
| Dashboard counts | GROUP BY, conditional aggregation |
| Top products | GROUP BY + ORDER BY + LIMIT |
| Latest status per entity | ROW_NUMBER |
| Audit history | LAG/LEAD, time filters |
| Booking availability | Date range overlap, locking |
| Duplicate prevention | Unique constraints, UPSERT |
| Idempotency key | Unique index + safe insert |
| Performance issue | EXPLAIN ANALYZE, indexes |

## N+1 Query Problem

Problem:

```text
Application loads 100 customers, then runs one query per customer to load orders.
Total = 1 + 100 queries.
```

Fix options:
- Join fetch where appropriate.
- Batch fetch.
- Query child rows with `WHERE customer_id IN (...)`.
- Use DTO projection.
- Use pagination carefully.

SQL-style fix:

```sql
SELECT c.id, c.name, o.id AS order_id, o.amount
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE c.id IN (:customer_ids);
```

## API Filter Query Pattern

```sql
SELECT id, customer_id, amount, status, order_date
FROM orders
WHERE (:status IS NULL OR status = :status)
  AND (:customer_id IS NULL OR customer_id = :customer_id)
  AND (:from_date IS NULL OR order_date >= :from_date)
  AND (:to_date IS NULL OR order_date < :to_date)
ORDER BY order_date DESC, id DESC
LIMIT :limit;
```

Performance caution:

```text
Dynamic OR conditions can reduce index usage. For high-performance APIs, build SQL dynamically
for only the filters present, or use query builder/specification carefully.
```

## Idempotent Insert Pattern

```sql
INSERT INTO payments(idempotency_key, amount, status)
VALUES (:key, :amount, 'INITIATED')
ON CONFLICT (idempotency_key)
DO NOTHING;
```

Then fetch existing row:

```sql
SELECT *
FROM payments
WHERE idempotency_key = :key;
```

## Unique Constraint Is Business Logic Protection

Example:

```sql
CREATE UNIQUE INDEX uk_customers_email
ON customers(email);
```

Strong answer:

```text
I validate in application for good error messages, but enforce uniqueness in the database
because concurrent requests can bypass application-only checks.
```

## @Transactional Interview Mapping

Spring:

```java
@Transactional
public void createBooking(...) {
    // multiple DB statements
}
```

SQL meaning:

```text
BEGIN happens before method, COMMIT after success, ROLLBACK on failure depending rollback rules.
```

Interview caution:

```text
Keep transactions short. Do not call slow external APIs inside a DB transaction unless
there is a very strong reason.
```

---

# 30. PostgreSQL Features Worth Knowing

The JD mentions PostgreSQL. These features are worth recognizing.

## SERIAL vs IDENTITY

Older:

```sql
id SERIAL PRIMARY KEY
```

Modern SQL-standard style:

```sql
id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY
```

Interview line:

```text
SERIAL is PostgreSQL-specific shorthand using a sequence. IDENTITY is the more SQL-standard
modern approach.
```

## UPSERT

```sql
INSERT INTO customers(email, name)
VALUES ('a@example.com', 'Aravind')
ON CONFLICT (email)
DO UPDATE SET name = EXCLUDED.name;
```

Use case:
- Idempotency
- Sync jobs
- Insert-or-update flows

## RETURNING

```sql
INSERT INTO customers(name, email)
VALUES ('Aravind', 'a@example.com')
RETURNING id, name, email;
```

Useful for backend APIs because you can insert and return generated values in one round trip.

## DISTINCT ON

PostgreSQL-specific latest row per group:

```sql
SELECT DISTINCT ON (customer_id) *
FROM orders
ORDER BY customer_id, order_date DESC, id DESC;
```

Interview caution:

```text
DISTINCT ON is concise in PostgreSQL, but ROW_NUMBER is more portable across databases.
```

## FILTER Clause

```sql
SELECT customer_id,
       COUNT(*) FILTER (WHERE status = 'completed') AS completed_orders,
       COUNT(*) FILTER (WHERE status = 'cancelled') AS cancelled_orders
FROM orders
GROUP BY customer_id;
```

Portable alternative:

```sql
SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END)
```

## JSONB

```sql
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    payload JSONB
);
```

Query:

```sql
SELECT *
FROM events
WHERE payload ->> 'type' = 'BOOKING_CREATED';
```

Interview line:

```text
JSONB is useful for flexible semi-structured data, but I avoid using it as an excuse to skip
relational modeling for core queryable business entities.
```

## Array Type

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    tags TEXT[]
);
```

Query:

```sql
SELECT *
FROM products
WHERE 'featured' = ANY(tags);
```

## ILIKE

Case-insensitive search:

```sql
SELECT *
FROM customers
WHERE name ILIKE '%aravind%';
```

Performance caution:

```text
Leading wildcard searches usually cannot use a normal B-tree index efficiently.
For serious search, consider trigram indexes or a search engine depending requirements.
```

## Date Functions

```sql
SELECT DATE_TRUNC('month', order_date) AS month,
       SUM(amount)
FROM orders
GROUP BY DATE_TRUNC('month', order_date);
```

## EXPLAIN ANALYZE

```sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE customer_id = 10;
```

Strong answer:

```text
EXPLAIN shows the plan. EXPLAIN ANALYZE actually runs the query and shows real timings,
so use it carefully on write queries or expensive production queries.
```

---

# 31. Final SQL Drill Bank

Practice writing these without looking.

## Beginner-Medium Must Code

1. Find employees in Engineering with salary above 80000.
2. Count employees by department.
3. Departments with more than 2 employees.
4. Customers who never ordered.
5. Duplicate customer emails.
6. Employees earning more than their manager.
7. Total revenue by customer.
8. Top 5 products by revenue.
9. Orders placed in the last 30 days.
10. Completed order count by month.

## Medium Must Code

1. Second highest distinct salary.
2. Nth highest salary.
3. Top 3 salaries per department.
4. Latest order per customer.
5. Running total by order date.
6. Month-over-month revenue growth.
7. Delete duplicates and keep smallest ID.
8. Customers with more than 3 completed orders.
9. Products never ordered.
10. Average order value per customer.

## Advanced But Common

1. Consecutive login days.
2. Date range overlap for bookings.
3. Cursor pagination query.
4. Conditional aggregation pivot.
5. Retention-style query: users active in month 1 and month 2.
6. Find gaps in sequence numbers.
7. Identify customers whose latest order is cancelled.
8. Find first order and latest order per customer.
9. Calculate percentage contribution of each category to total revenue.
10. Find orders whose amount is above customer average.

## Theory Drill

Answer these out loud:

1. Explain SQL execution order.
2. WHERE vs HAVING.
3. INNER JOIN vs LEFT JOIN.
4. IN vs EXISTS.
5. NOT IN with NULL problem.
6. ROW_NUMBER vs RANK vs DENSE_RANK.
7. GROUP BY vs window function.
8. CTE vs subquery.
9. UNION vs UNION ALL.
10. COUNT(*) vs COUNT(column).
11. What is an index?
12. Composite index column order.
13. What is a covering index?
14. What is sargability?
15. How do you debug slow query?
16. ACID.
17. Isolation levels.
18. Dirty/non-repeatable/phantom reads.
19. Optimistic vs pessimistic locking.
20. Normalization vs denormalization.

---

# 32. One-Hour SQL Revision Plan

## First 10 Minutes: Execution and Joins

Revise:
- SQL logical order
- WHERE vs HAVING
- INNER/LEFT/FULL/CROSS/SELF JOIN
- Anti-join with `NOT EXISTS`

Must write:

```sql
SELECT c.*
FROM customers c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
);
```

## Next 15 Minutes: Aggregation

Revise:
- GROUP BY
- COUNT/SUM/AVG
- Conditional aggregation
- Duplicate detection

Must write:

```sql
SELECT department, COUNT(*) AS employee_count
FROM employees
GROUP BY department
HAVING COUNT(*) > 2;
```

## Next 15 Minutes: Window Functions

Revise:
- ROW_NUMBER
- RANK
- DENSE_RANK
- LAG/LEAD
- Running totals

Must write:

```sql
WITH ranked AS (
    SELECT e.*,
           ROW_NUMBER() OVER (
               PARTITION BY department
               ORDER BY salary DESC
           ) AS rn
    FROM employees e
)
SELECT *
FROM ranked
WHERE rn <= 3;
```

## Next 10 Minutes: Performance

Revise:
- Index basics
- Composite index order
- Sargability
- EXPLAIN ANALYZE
- Offset vs cursor pagination

Must say:

```text
I validate performance with EXPLAIN ANALYZE and check row counts, scan type, joins,
sorts, and whether predicates can use indexes.
```

## Final 10 Minutes: Transactions

Revise:
- ACID
- Isolation anomalies
- Locks
- Optimistic vs pessimistic locking
- Double-booking prevention

Must say:

```text
For concurrency-sensitive flows, I do not rely only on application checks. I use database
transactions, constraints, appropriate locking or optimistic version checks, and retry logic
where needed.
```

---

# 33. Wrong Query Clinic — Mistakes, Why Wrong, Correct Query

This section trains interview instincts. Many candidates know syntax but lose points because
their query is subtly wrong under NULLs, joins, duplicates, ties, or concurrency.

The pattern:

```text
Wrong query → why it is wrong → corrected query → interview line.
```

## 33.1 Aggregate In WHERE

Question:

```text
Find departments with more than 2 employees.
```

Wrong:

```sql
SELECT department, COUNT(*) AS employee_count
FROM employees
WHERE COUNT(*) > 2
GROUP BY department;
```

Why wrong:

```text
WHERE runs before GROUP BY. At WHERE time, COUNT(*) does not exist yet.
```

Correct:

```sql
SELECT department, COUNT(*) AS employee_count
FROM employees
GROUP BY department
HAVING COUNT(*) > 2;
```

Interview line:

```text
Row-level filters go in WHERE. Aggregate/group filters go in HAVING.
```

## 33.2 LEFT JOIN Accidentally Turned Into INNER JOIN

Question:

```text
Find all customers and their completed orders if any.
Customers without completed orders should still appear.
```

Wrong:

```sql
SELECT c.id, c.name, o.id AS order_id
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.status = 'completed';
```

Why wrong:

```text
The WHERE condition removes rows where o.status is NULL.
That means customers without orders disappear.
The LEFT JOIN effectively became an INNER JOIN.
```

Correct:

```sql
SELECT c.id, c.name, o.id AS order_id
FROM customers c
LEFT JOIN orders o
    ON o.customer_id = c.id
   AND o.status = 'completed';
```

Interview line:

```text
If I need unmatched left-side rows to survive, filters on the right table often belong in
the JOIN condition, not WHERE.
```

## 33.3 NOT IN With NULL

Question:

```text
Find customers who never ordered.
```

Dangerous:

```sql
SELECT *
FROM customers
WHERE id NOT IN (SELECT customer_id FROM orders);
```

Why dangerous:

```text
If the subquery returns even one NULL customer_id, NOT IN can return no rows because
comparison with NULL becomes UNKNOWN.
```

Safer:

```sql
SELECT c.*
FROM customers c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
);
```

Also acceptable if NULLs are explicitly removed:

```sql
SELECT *
FROM customers
WHERE id NOT IN (
    SELECT customer_id
    FROM orders
    WHERE customer_id IS NOT NULL
);
```

Interview line:

```text
For anti-joins, I usually prefer NOT EXISTS because it handles NULLs safely.
```

## 33.4 Join Multiplication Before Aggregation

Question:

```text
Find total revenue per customer.
```

Risky if the query joins many one-to-many tables:

```sql
SELECT c.id, c.name, SUM(o.amount) AS revenue
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id, c.name;
```

Why wrong:

```text
If each order has multiple order_items, the order row is repeated once per item.
SUM(o.amount) becomes inflated.
```

Correct option 1: aggregate from the right grain.

```sql
SELECT c.id, c.name, SUM(oi.quantity * oi.unit_price) AS revenue
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id, c.name;
```

Correct option 2: pre-aggregate orders before joining to other one-to-many tables.

```sql
WITH order_revenue AS (
    SELECT customer_id, SUM(amount) AS revenue
    FROM orders
    GROUP BY customer_id
)
SELECT c.id, c.name, COALESCE(orv.revenue, 0) AS revenue
FROM customers c
LEFT JOIN order_revenue orv ON orv.customer_id = c.id;
```

Interview line:

```text
Before aggregating, I check whether joins change the row grain. One-to-many joins can
multiply rows and inflate sums.
```

## 33.5 Top N Per Group Using Global LIMIT

Question:

```text
Find top 3 employees per department by salary.
```

Wrong:

```sql
SELECT *
FROM employees
ORDER BY salary DESC
LIMIT 3;
```

Why wrong:

```text
This returns top 3 employees globally, not top 3 inside each department.
```

Correct:

```sql
WITH ranked AS (
    SELECT e.*,
           ROW_NUMBER() OVER (
               PARTITION BY department
               ORDER BY salary DESC, id ASC
           ) AS rn
    FROM employees e
)
SELECT *
FROM ranked
WHERE rn <= 3;
```

Interview line:

```text
Top N per group needs a window function with PARTITION BY, not a global LIMIT.
```

## 33.6 Latest Row Per Group Without Tie-Breaker

Question:

```text
Find the latest order per customer.
```

Almost correct:

```sql
WITH ranked AS (
    SELECT o.*,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id
               ORDER BY order_date DESC
           ) AS rn
    FROM orders o
)
SELECT *
FROM ranked
WHERE rn = 1;
```

What can go wrong:

```text
If two orders have the same order_date, the database can choose either one unless the
ORDER BY is deterministic.
```

Better:

```sql
WITH ranked AS (
    SELECT o.*,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id
               ORDER BY order_date DESC, id DESC
           ) AS rn
    FROM orders o
)
SELECT *
FROM ranked
WHERE rn = 1;
```

Interview line:

```text
For latest-row queries, I include a stable tie-breaker such as id DESC.
```

## 33.7 Deep Pagination With OFFSET

Problem:

```sql
SELECT *
FROM orders
ORDER BY order_date DESC
LIMIT 20 OFFSET 100000;
```

Why bad:

```text
The database still walks many rows before returning the page. Large OFFSET gets slower
as the page number grows.
```

Better cursor pagination:

```sql
SELECT *
FROM orders
WHERE (order_date, id) < (:last_order_date, :last_id)
ORDER BY order_date DESC, id DESC
LIMIT 20;
```

Index:

```sql
CREATE INDEX idx_orders_order_date_id
ON orders(order_date DESC, id DESC);
```

Interview line:

```text
For deep pagination in APIs, I prefer cursor/keyset pagination over OFFSET.
```

## 33.8 Non-Sargable Predicate

Problem:

```sql
SELECT *
FROM orders
WHERE EXTRACT(YEAR FROM order_date) = 2026;
```

Why bad:

```text
Applying a function to the indexed column can prevent normal index usage.
```

Better:

```sql
SELECT *
FROM orders
WHERE order_date >= DATE '2026-01-01'
  AND order_date <  DATE '2027-01-01';
```

Index:

```sql
CREATE INDEX idx_orders_order_date
ON orders(order_date);
```

Interview line:

```text
Keep predicates sargable: compare the raw indexed column to computed constants.
```

## 33.9 COUNT(column) When You Mean COUNT(*)

Question:

```text
How many employees are there?
```

Risky:

```sql
SELECT COUNT(manager_id)
FROM employees;
```

Why wrong:

```text
COUNT(manager_id) skips NULL manager_id values. Employees without managers are not counted.
```

Correct:

```sql
SELECT COUNT(*)
FROM employees;
```

Interview line:

```text
COUNT(*) counts rows. COUNT(column) counts non-NULL values in that column.
```

## 33.10 Application-Only Uniqueness Check

Problem:

```text
Before creating a customer, application checks whether email already exists.
```

Race:

```text
Request A checks email: not found.
Request B checks email: not found.
Both insert.
Duplicate email exists.
```

Correct protection:

```sql
CREATE UNIQUE INDEX uk_customers_email
ON customers(email);
```

Then:

```sql
INSERT INTO customers(email, name)
VALUES (:email, :name)
ON CONFLICT (email)
DO UPDATE SET name = EXCLUDED.name;
```

Interview line:

```text
Application validation is for user experience. Database constraints protect correctness
under concurrency.
```

---

# 34. EXPLAIN ANALYZE Walkthroughs

This is how you sound practical when the interviewer asks:

```text
This query is slow. What do you do?
```

Senior answer:

```text
I do not guess. I run EXPLAIN ANALYZE, inspect scan type, row estimates vs actual rows,
join strategy, sort/hash operations, and whether predicates can use indexes. Then I change
one thing at a time and re-measure.
```

## 34.1 Walkthrough 1: Customer Orders API Is Slow

Query:

```sql
SELECT id, customer_id, amount, status, order_date
FROM orders
WHERE customer_id = 42
ORDER BY order_date DESC
LIMIT 20;
```

Bad plan shape:

```text
Seq Scan on orders
  Filter: customer_id = 42
Sort by order_date DESC
Limit 20
```

What it means:

```text
The database scans many/all orders, filters customer_id, then sorts matching rows.
For a large orders table, this is wasteful.
```

Index:

```sql
CREATE INDEX idx_orders_customer_date
ON orders(customer_id, order_date DESC);
```

Expected better plan shape:

```text
Index Scan using idx_orders_customer_date
  Index Cond: customer_id = 42
Limit 20
```

Why this index works:

```text
customer_id handles the equality filter.
order_date DESC already provides the desired order for that customer's rows.
```

Interview line:

```text
I match the composite index to the query shape: equality filter first, then sort/range column.
```

## 34.2 Walkthrough 2: Monthly Revenue Query Is Slow

Query:

```sql
SELECT DATE_TRUNC('month', order_date) AS month,
       SUM(amount) AS revenue
FROM orders
WHERE status = 'completed'
GROUP BY DATE_TRUNC('month', order_date)
ORDER BY month;
```

Possible issue:

```text
If the completed orders table is huge, grouping all history is expensive.
```

First improvement: bound the time range.

```sql
SELECT DATE_TRUNC('month', order_date) AS month,
       SUM(amount) AS revenue
FROM orders
WHERE status = 'completed'
  AND order_date >= DATE '2026-01-01'
  AND order_date <  DATE '2027-01-01'
GROUP BY DATE_TRUNC('month', order_date)
ORDER BY month;
```

Index option:

```sql
CREATE INDEX idx_orders_completed_date
ON orders(order_date)
WHERE status = 'completed';
```

Why partial index helps:

```text
If most queries only care about completed orders, the index stores only that subset.
It is smaller and faster than indexing all orders.
```

For dashboard scale:

```text
If this query is hit frequently, consider a daily/monthly revenue summary table updated
by batch or stream processing instead of aggregating raw orders every time.
```

Interview line:

```text
For analytics dashboards, I first optimize the raw query, then consider pre-aggregation
if the same expensive aggregation is repeatedly requested.
```

## 34.3 Walkthrough 3: Join Query Explodes

Query:

```sql
SELECT c.id, c.name, COUNT(*) AS row_count
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id, c.name;
```

Problem:

```text
The result counts order_items, not orders or customers.
The join changes the grain to one row per order item.
```

If requirement is order count:

```sql
SELECT c.id, c.name, COUNT(DISTINCT o.id) AS order_count
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id, c.name;
```

Better if order_items are unnecessary:

```sql
SELECT c.id, c.name, COUNT(o.id) AS order_count
FROM customers c
JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name;
```

If requirement is revenue:

```sql
SELECT c.id, c.name, SUM(oi.quantity * oi.unit_price) AS revenue
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.id, c.name;
```

Interview line:

```text
Before optimizing a join, I verify the grain. A fast wrong query is still wrong.
```

## 34.4 EXPLAIN Red Flags

| Red flag | What it often means | First thing to check |
|---|---|---|
| Seq Scan on huge table | missing/unusable index | predicate and index |
| rows estimate far from actual | stale stats or skewed data | ANALYZE/statistics |
| Sort on huge result | missing sort-supporting index | ORDER BY/index |
| Nested Loop with huge outer rows | bad join strategy or missing index | join cardinality/index |
| HashAggregate memory spill | large grouping | pre-aggregate/filter/work_mem |
| Filter after scan removes most rows | predicate not used as index condition | sargability/index |

EXPLAIN answer template:

```text
I see a sequential scan over a large table and a sort after filtering. I would add a composite
index that matches the equality filter and ordering, rerun EXPLAIN ANALYZE, and confirm the
plan changed to an index scan with fewer rows read.
```

---

# 35. Schema Design Mini-Cases

SQL interviews are not only query writing. Backend interviews often ask:

```text
How would you model this data?
What constraints protect correctness?
What indexes support the APIs?
```

## 35.1 E-Commerce Orders

Core tables:

```sql
CREATE TABLE customers (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    status TEXT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL
);
```

Useful indexes:

```sql
CREATE INDEX idx_orders_customer_created
ON orders(customer_id, created_at DESC);

CREATE INDEX idx_order_items_order
ON order_items(order_id);
```

Design notes:

- `orders` is one row per order.
- `order_items` is one row per product inside an order.
- Store `unit_price` on `order_items` because product price may change later.
- Use status transitions carefully: `created -> paid -> shipped -> delivered/cancelled`.

Interview line:

```text
I snapshot price into order_items so historical orders remain correct even if product pricing changes.
```

## 35.2 Hotel Booking Availability

Core tables:

```sql
CREATE TABLE rooms (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hotel_id BIGINT NOT NULL,
    room_number TEXT NOT NULL,
    UNIQUE (hotel_id, room_number)
);

CREATE TABLE bookings (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    user_id BIGINT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (check_in < check_out)
);
```

Overlap query:

```sql
SELECT 1
FROM bookings
WHERE room_id = :room_id
  AND check_in < :requested_check_out
  AND check_out > :requested_check_in
  AND status IN ('HELD', 'CONFIRMED');
```

Useful index:

```sql
CREATE INDEX idx_bookings_room_dates
ON bookings(room_id, check_in, check_out);
```

Correctness note:

```text
The overlap check and insert must happen inside a transaction with appropriate locking
or a database-level constraint. Otherwise two users can book the same room concurrently.
```

Interview line:

```text
For booking systems, database-level correctness matters more than application-only checks.
```

## 35.3 Audit Log / Status History

Use case:

```text
Track every order status change.
```

Schema:

```sql
CREATE TABLE order_status_history (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    old_status TEXT,
    new_status TEXT NOT NULL,
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT now()
);
```

Index:

```sql
CREATE INDEX idx_order_status_history_order_time
ON order_status_history(order_id, changed_at DESC, id DESC);
```

Latest status pattern:

```sql
WITH ranked AS (
    SELECT h.*,
           ROW_NUMBER() OVER (
               PARTITION BY order_id
               ORDER BY changed_at DESC, id DESC
           ) AS rn
    FROM order_status_history h
)
SELECT *
FROM ranked
WHERE rn = 1;
```

Interview line:

```text
For auditability, I append status changes instead of overwriting history.
```

## 35.4 Idempotency Table For Backend APIs

Use case:

```text
Client retries payment or booking request. We must not process it twice.
```

Schema:

```sql
CREATE TABLE idempotency_keys (
    key TEXT PRIMARY KEY,
    request_hash TEXT NOT NULL,
    response_body JSONB,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL
);
```

Flow:

```text
1. Insert key at request start.
2. If conflict, fetch existing row.
3. If request_hash differs, reject.
4. If same request, return stored response or current status.
```

Interview line:

```text
Idempotency is usually a unique key plus stored request/response metadata.
```

## 35.5 Analytics Star Schema

Use case:

```text
Business wants revenue by date, product category, and customer city.
```

Fact table:

```sql
CREATE TABLE fact_order_item_sales (
    order_item_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    date_id INT NOT NULL,
    quantity INT NOT NULL,
    gross_amount NUMERIC(12,2) NOT NULL
);
```

Dimensions:

```text
dim_date(date_id, date, month, quarter, year)
dim_customer(customer_id, city, segment)
dim_product(product_id, category, brand)
```

Interview line:

```text
OLTP schemas optimize transactions. Star schemas optimize analytical reads and dashboard queries.
```

---

# 36. MAANG-Style SQL Capstone Problems

These problems combine grain, joins, windows, CTEs, performance, and correctness.

For each capstone, use this answer shape:

```text
1. State the output grain.
2. Pick the source table that defines that grain.
3. Join only what is needed.
4. Use CTEs to name steps.
5. Use window functions when rows must stay uncollapsed.
6. Mention indexes or constraints.
```

## 36.1 Top 3 Products By Revenue Per Category In Last 30 Days

Output grain:

```text
One row per product within category rank.
```

Query:

```sql
WITH product_revenue AS (
    SELECT
        p.category,
        p.id AS product_id,
        p.name AS product_name,
        SUM(oi.quantity * oi.unit_price) AS revenue
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    JOIN products p ON p.id = oi.product_id
    WHERE o.status = 'completed'
      AND o.order_date >= CURRENT_DATE - INTERVAL '30 days'
    GROUP BY p.category, p.id, p.name
),
ranked AS (
    SELECT *,
           DENSE_RANK() OVER (
               PARTITION BY category
               ORDER BY revenue DESC, product_id ASC
           ) AS rnk
    FROM product_revenue
)
SELECT category, product_id, product_name, revenue, rnk
FROM ranked
WHERE rnk <= 3
ORDER BY category, rnk, product_id;
```

Index thoughts:

```sql
CREATE INDEX idx_orders_status_date
ON orders(status, order_date);

CREATE INDEX idx_order_items_order_product
ON order_items(order_id, product_id);
```

Interview line:

```text
I aggregate revenue at product-category grain first, then rank within each category.
```

## 36.2 Monthly Customer Retention

Question:

```text
Find customers who ordered in January and also ordered in February.
```

Output grain:

```text
One row per retained customer.
```

Query:

```sql
WITH jan_customers AS (
    SELECT DISTINCT customer_id
    FROM orders
    WHERE order_date >= DATE '2026-01-01'
      AND order_date <  DATE '2026-02-01'
      AND status = 'completed'
),
feb_customers AS (
    SELECT DISTINCT customer_id
    FROM orders
    WHERE order_date >= DATE '2026-02-01'
      AND order_date <  DATE '2026-03-01'
      AND status = 'completed'
)
SELECT j.customer_id
FROM jan_customers j
JOIN feb_customers f ON f.customer_id = j.customer_id;
```

Retention count:

```sql
WITH monthly AS (
    SELECT DISTINCT
        customer_id,
        DATE_TRUNC('month', order_date)::DATE AS month
    FROM orders
    WHERE status = 'completed'
),
pairs AS (
    SELECT
        current_month.month,
        current_month.customer_id
    FROM monthly current_month
    JOIN monthly next_month
      ON next_month.customer_id = current_month.customer_id
     AND next_month.month = (current_month.month + INTERVAL '1 month')::DATE
)
SELECT month, COUNT(*) AS retained_customers
FROM pairs
GROUP BY month
ORDER BY month;
```

Interview line:

```text
Retention is usually a self-join over user activity by time bucket.
```

## 36.3 Latest Order Status Per Order

Schema:

```text
order_status_history(order_id, status, changed_at, id)
```

Query:

```sql
WITH ranked AS (
    SELECT h.*,
           ROW_NUMBER() OVER (
               PARTITION BY order_id
               ORDER BY changed_at DESC, id DESC
           ) AS rn
    FROM order_status_history h
)
SELECT order_id, status, changed_at
FROM ranked
WHERE rn = 1;
```

PostgreSQL-specific alternative:

```sql
SELECT DISTINCT ON (order_id)
       order_id, status, changed_at
FROM order_status_history
ORDER BY order_id, changed_at DESC, id DESC;
```

Index:

```sql
CREATE INDEX idx_status_history_order_changed
ON order_status_history(order_id, changed_at DESC, id DESC);
```

Interview line:

```text
ROW_NUMBER is portable. DISTINCT ON is concise in PostgreSQL.
```

## 36.4 Prevent Double Booking

Question:

```text
Two users try to book the same room for overlapping dates. How do you prevent this?
```

Strong transaction answer:

```sql
BEGIN;

SELECT id
FROM rooms
WHERE id = :room_id
FOR UPDATE;

SELECT 1
FROM bookings
WHERE room_id = :room_id
  AND check_in < :requested_check_out
  AND check_out > :requested_check_in
  AND status IN ('HELD', 'CONFIRMED');

-- If no overlap exists:
INSERT INTO bookings(room_id, user_id, check_in, check_out, status)
VALUES (:room_id, :user_id, :requested_check_in, :requested_check_out, 'CONFIRMED');

COMMIT;
```

Interview line:

```text
I lock the room or availability row so two transactions cannot both pass the overlap check.
For PostgreSQL, I would also consider an exclusion constraint for date-range overlap.
```

## 36.5 Payment Idempotency

Question:

```text
A payment API request times out and the client retries. How do we prevent double charge?
```

Schema idea:

```sql
CREATE TABLE payments (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    status TEXT NOT NULL,
    provider_reference TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

Insert pattern:

```sql
INSERT INTO payments(idempotency_key, order_id, amount, status)
VALUES (:key, :order_id, :amount, 'INITIATED')
ON CONFLICT (idempotency_key)
DO NOTHING;
```

Then:

```sql
SELECT *
FROM payments
WHERE idempotency_key = :key;
```

Interview line:

```text
The unique idempotency key ensures repeated requests map to one payment record.
The application can return the existing payment state instead of creating a new charge.
```

## 36.6 Customer Whose Current Month Spend Is Above Their Own Average

Output grain:

```text
One row per customer per month where monthly spend is above that customer's average monthly spend.
```

Query:

```sql
WITH monthly AS (
    SELECT
        customer_id,
        DATE_TRUNC('month', order_date)::DATE AS month,
        SUM(amount) AS monthly_spend
    FROM orders
    WHERE status = 'completed'
    GROUP BY customer_id, DATE_TRUNC('month', order_date)::DATE
),
with_avg AS (
    SELECT
        customer_id,
        month,
        monthly_spend,
        AVG(monthly_spend) OVER (PARTITION BY customer_id) AS avg_monthly_spend
    FROM monthly
)
SELECT customer_id, month, monthly_spend, avg_monthly_spend
FROM with_avg
WHERE monthly_spend > avg_monthly_spend
ORDER BY customer_id, month;
```

Interview line:

```text
I first aggregate to monthly grain, then use a window function to compare each month
against the customer's own average without losing monthly rows.
```

---

# 37. Final Master Checklist

Use this as the final SQL readiness checklist.

## Query Writing Checklist

- Did I state the output grain?
- Did I start from the table that defines that grain?
- Did I choose the correct join type?
- Did I avoid accidental row multiplication?
- Did I put row filters in `WHERE`?
- Did I put aggregate filters in `HAVING`?
- Did I use window functions when rows should not collapse?
- Did I handle NULLs intentionally?
- Did I add deterministic tie-breakers for latest/top queries?

## Performance Checklist

- Am I reading fewer rows than necessary?
- Are filters sargable?
- Do joins use indexed keys?
- Does `ORDER BY` match an index when needed?
- Is deep pagination using cursor/keyset pagination?
- Did I avoid `SELECT *` in production paths?
- Did I validate with `EXPLAIN ANALYZE`?
- Did I compare estimated rows vs actual rows?

## Correctness Checklist

- Are uniqueness rules enforced by database constraints?
- Are multi-step writes inside a transaction?
- Could concurrent requests race?
- Do I need optimistic or pessimistic locking?
- Is idempotency required for retries?
- Do I need to retry deadlocks or serialization failures?
- Are historical values snapshotted where business history matters?

## PostgreSQL Checklist

- Can `DISTINCT ON` simplify latest-row-per-group?
- Can `FILTER` simplify conditional aggregation?
- Can `RETURNING` avoid an extra round trip?
- Can `ON CONFLICT` protect idempotent insert/upsert?
- Would a partial index make a common filtered query faster?
- Would a functional index help a deliberate function predicate?
- Is JSONB appropriate, or should this be relational?

## MAANG Interview Bar

You are SQL-interview ready when you can do this without notes:

```text
1. Solve top-N-per-group with ROW_NUMBER.
2. Solve latest-row-per-group with deterministic ordering.
3. Explain WHERE vs HAVING and GROUP BY vs window functions.
4. Explain INNER JOIN, LEFT JOIN, anti-join, and join multiplication.
5. Debug a slow query with EXPLAIN ANALYZE.
6. Design indexes for filters, joins, sorts, and pagination.
7. Explain transactions, isolation anomalies, locks, and deadlocks.
8. Prevent double booking or duplicate payment under concurrency.
9. Map SQL patterns to backend APIs and Spring @Transactional.
10. State trade-offs clearly instead of only writing syntax.
```

Final memory line:

```text
SQL mastery is grain + correctness + performance.
Grain tells me what one row means.
Correctness protects data under edge cases and concurrency.
Performance makes the right answer fast enough for production.
```

---

# Final Interview Closing Answer

If interviewer asks:

```text
How comfortable are you with SQL?
```

Say:

```text
I am comfortable writing SQL for backend and analytical use cases. My usual approach is to
first identify the output grain, then choose joins, filters, grouping, and window functions.
For performance, I look at EXPLAIN ANALYZE, index usage, row counts, join strategy, sorting,
and pagination pattern. For correctness, I pay attention to NULL handling, transaction
boundaries, isolation, and database constraints, especially for concurrent flows like booking
or payment processing.
```
