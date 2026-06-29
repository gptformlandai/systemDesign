# Python Data Engineering — Pandas & Polars — Gold Sheet

> **Track**: Python Interview Track — Group 2: Intermediate Backend
> **File**: Gap Fill #1 (Track File #12a)
> **Audience**: Java developers learning Python for backend and data engineering interviews
> **Read after**: Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `DataFrame` creation and basic operations | ★★★★★ | No Java standard library equivalent; Java devs default to streams and lists |
| `loc` vs `iloc` — label vs position indexing | ★★★★★ | Most common pandas trap; Java array index logic does NOT apply to `loc` |
| `groupby` — split/apply/combine | ★★★★★ | SQL GROUP BY equivalent; Java devs often loop manually instead |
| Chained indexing — `df["col"]["row"]` trap | ★★★★★ | Java chaining is fine; pandas chaining triggers `SettingWithCopyWarning` and silent bugs |
| Copy vs View — when you mutate the original | ★★★★★ | Java copy semantics are explicit; pandas copy/view depends on operation type |
| Memory-efficient iteration — `itertuples` vs `iterrows` | ★★★★☆ | Java streams are lazy; pandas `iterrows` wraps each row in a `Series` — expensive |
| `merge` / `join` — combining DataFrames | ★★★★☆ | SQL JOIN equivalent; multiple join types and key strategies |
| Reading large files — chunking strategy | ★★★★☆ | Java NIO buffering; pandas `chunksize` for files that don't fit in memory |
| pandas vs polars — when to use which | ★★★★☆ | Polars is rapidly becoming the interview differentiator at senior level |
| Polars lazy API — `scan_csv` vs `read_csv` | ★★★★☆ | Java Stream lazy evaluation analog; polars query optimizer runs on lazy plan |
| `dtype` selection — int8 vs int64 memory impact | ★★★★☆ | Java primitives are fixed size; pandas defaults to int64 wasting 8x memory |
| `apply` vs vectorized operations | ★★★★★ | Java stream `.map()` feels like pandas `.apply()` but `.apply()` is slow — vectorize instead |

---

## 2. pandas Fundamentals

### The DataFrame Mental Model

```
Java mental model:            Python pandas mental model:
  List<Map<String, Object>>     DataFrame = 2D labeled data structure
  or                              rows have an Index (labels, not just positions)
  List<Row>                       columns have names
                                  every column is a Series (1D array)
                                  operations are vectorized over columns by default

Key difference:
  Java: iterate rows, update each one.
  pandas: describe the transformation; pandas applies it to the entire column at once.
```

### DataFrame Creation

```python
import pandas as pd
import numpy as np

# From list of dicts (most common in API/JSON context)
data = [
    {"id": 1, "name": "Alice", "salary": 95000, "active": True},
    {"id": 2, "name": "Bob",   "salary": 88000, "active": False},
    {"id": 3, "name": "Carol", "salary": 110000, "active": True},
]
df = pd.DataFrame(data)

# From dict of lists (column-first — how pandas stores data internally)
df = pd.DataFrame({
    "id":     [1, 2, 3],
    "name":   ["Alice", "Bob", "Carol"],
    "salary": [95000, 88000, 110000],
    "active": [True, False, True],
})

# From CSV (the most common real-world source)
df = pd.read_csv("employees.csv")

# Inspect immediately after loading
print(df.shape)          # (3, 4) — rows, columns
print(df.dtypes)         # column types — check for unexpected object dtype
print(df.head())         # first 5 rows
print(df.info())         # non-null counts, dtypes, memory usage
print(df.describe())     # numeric summary statistics
```

### Java Developer Bridge

| Java | pandas |
|---|---|
| `List<Row>` | `DataFrame` |
| `row.get("salary")` | `df.loc[idx, "salary"]` |
| `list.get(2).get("salary")` | `df.iloc[2]["salary"]` |
| `stream().filter()` | `df[df["active"] == True]` |
| `stream().map()` | `df["salary"].apply(fn)` or vectorized `df["salary"] * 1.1` |
| `stream().collect(groupingBy())` | `df.groupby("dept")["salary"].mean()` |
| `Collections.sort()` | `df.sort_values("salary", ascending=False)` |

---

## 3. Selection and Filtering

### loc vs iloc — The Most Common Trap

```python
# iloc — INTEGER position (0-based like Java arrays)
df.iloc[0]          # first row by POSITION
df.iloc[0:3]        # rows at positions 0, 1, 2 (exclusive end — like Java)
df.iloc[0, 1]       # row 0, column 1 by POSITION

# loc — LABEL-based (uses the Index labels, not positions)
df.loc[0]           # row with INDEX LABEL 0 (same as iloc[0] only if index is default RangeIndex)
df.loc[0:3]         # rows with labels 0, 1, 2, 3 — INCLUSIVE end (unlike Java/iloc!)
df.loc[0, "name"]   # row label 0, column label "name"

# TRAP: if index is not default (e.g., after filtering), loc and iloc diverge
filtered = df[df["active"] == True]
# filtered still has original index labels [0, 2] (positions 0 and 1)
filtered.iloc[1]    # second row by position = Carol (index label 2)
filtered.loc[1]     # row with label 1 = KeyError! Label 1 is Bob, who was filtered out

# Safe pattern — always reset_index after filtering if you plan to use iloc
clean = df[df["active"] == True].reset_index(drop=True)
```

### Column Selection

```python
# Single column → returns Series
df["name"]
df.name        # attribute style — avoid in production; breaks if column name is a method

# Multiple columns → returns DataFrame
df[["name", "salary"]]

# Boolean filtering — vectorized, not a loop
active = df[df["active"] == True]
high_earners = df[df["salary"] > 90000]
combined = df[(df["active"] == True) & (df["salary"] > 90000)]   # & not 'and'
```

---

## 4. The Chained Indexing Trap — Silent Data Bugs

### Why It Matters

```python
# WRONG — chained indexing: pandas may return a COPY, mutation doesn't affect original
df["salary"]["Alice"] = 200000       # SettingWithCopyWarning; df unchanged

# Also wrong:
df[df["active"] == True]["salary"] = 999999   # modifies a temporary copy

# CORRECT — use .loc with both row and column in one operation
df.loc[df["active"] == True, "salary"] = 999999   # modifies df in place correctly

# Rule: when reading, chaining is fine. When WRITING, use .loc in one step.
```

### Copy vs View

```python
# View — shares memory with original; mutations propagate back
view = df[["name", "salary"]]       # may be a view (implementation-defined)

# Explicit copy — independent; mutations do not affect original
copy = df[["name", "salary"]].copy()

# Safe pattern for derived DataFrames:
subset = df[df["active"] == True].copy()
subset["salary"] = subset["salary"] * 1.1   # safe; modifies copy, not original
```

---

## 5. GroupBy — Split / Apply / Combine

### Must Know

```python
# GroupBy = SQL GROUP BY
# Pattern: df.groupby(keys)[columns].aggregation()

# Mean salary by department
df.groupby("department")["salary"].mean()

# Multiple aggregations
df.groupby("department")["salary"].agg(["mean", "min", "max", "count"])

# Named aggregations (pandas 0.25+) — most readable
df.groupby("department").agg(
    avg_salary=("salary", "mean"),
    max_salary=("salary", "max"),
    headcount=("id", "count"),
)

# Multiple group keys
df.groupby(["department", "active"])["salary"].sum()

# GroupBy + transform — adds group result back as a column (same length as original)
df["dept_avg_salary"] = df.groupby("department")["salary"].transform("mean")

# GroupBy + filter — keep groups meeting a condition
df.groupby("department").filter(lambda g: g["salary"].mean() > 90000)
```

### Java Developer Bridge — GroupBy

```java
// Java: manual grouping with streams
Map<String, Double> avgByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.averagingDouble(Employee::getSalary)
    ));
```

```python
# pandas: one line, vectorized
avg_by_dept = df.groupby("department")["salary"].mean()
```

---

## 6. Merge and Join Operations

```python
employees = pd.DataFrame({
    "id":   [1, 2, 3],
    "name": ["Alice", "Bob", "Carol"],
    "dept_id": [10, 20, 10],
})
departments = pd.DataFrame({
    "dept_id": [10, 20, 30],
    "dept_name": ["Engineering", "Marketing", "Finance"],
})

# Inner join (default) — only matching rows
result = pd.merge(employees, departments, on="dept_id")

# Left join — all employees, NaN where no department match
result = pd.merge(employees, departments, on="dept_id", how="left")

# Merge on different column names
result = pd.merge(
    employees, departments,
    left_on="dept_id", right_on="dept_id",   # same name here, but can differ
    how="left",
)

# DataFrame.join() — joins on index (simpler for index-keyed DataFrames)
df1.join(df2, how="inner")
```

---

## 7. apply vs Vectorized Operations — Performance Critical

### The Trap

```python
# SLOW — apply calls Python function once per row (defeats vectorization)
df["salary_k"] = df["salary"].apply(lambda x: x / 1000)

# FAST — vectorized operation on the entire Series at once (NumPy under the hood)
df["salary_k"] = df["salary"] / 1000

# String operations — use .str accessor, not apply
df["name_upper"] = df["name"].str.upper()    # vectorized
df["name_len"] = df["name"].str.len()        # vectorized

# When apply IS acceptable:
# - Complex logic that cannot be expressed as vectorized operations
# - Operations on multiple columns at once (axis=1)
df["label"] = df.apply(
    lambda row: f"{row['name']} ({row['department']})", axis=1
)
# Even here: prefer string formatting via Series operations when possible:
df["label"] = df["name"] + " (" + df["department"] + ")"   # faster
```

### Performance Hierarchy (fastest to slowest)

```
1. Built-in vectorized pandas/NumPy operations    (C speed)
2. .str / .dt accessor methods                    (C speed)
3. NumPy ufuncs applied to Series                 (C speed)
4. .apply() with a Python lambda                  (Python speed — 10-100x slower)
5. .itertuples() loop                             (Python speed, but faster than iterrows)
6. .iterrows() loop                               (Slowest — wraps each row in Series)
```

---

## 8. Memory Optimization — dtype Selection

```python
# pandas defaults:
# integers → int64 (8 bytes per value)
# floats   → float64 (8 bytes per value)
# strings  → object (Python str pointer — no fixed size, high overhead)
# booleans → bool (1 byte in NumPy array, but object if mixed with NaN)

# Check memory usage
df.memory_usage(deep=True)

# Downcast integers — safe when value range fits
df["age"] = df["age"].astype("int8")         # -128 to 127
df["year"] = df["year"].astype("int16")      # -32768 to 32767
df["salary"] = df["salary"].astype("int32")  # up to ~2.1 billion

# Categorical for low-cardinality string columns
df["department"] = df["department"].astype("category")
# Stores one copy of each string; each row stores an int code instead
# Huge savings when column has few unique values relative to row count

# Example savings for 1M row DataFrame:
# "department" as object:   ~64 MB (Python str per cell)
# "department" as category:  ~1 MB (int codes + lookup table)

# Read-time dtype specification
df = pd.read_csv("big.csv", dtype={
    "age": "int8",
    "department": "category",
    "salary": "int32",
})
```

---

## 9. Chunked Reading for Large Files

```python
# Files that don't fit in memory — process in chunks
chunk_size = 100_000

totals = []
for chunk in pd.read_csv("billion_rows.csv", chunksize=chunk_size):
    # Process each chunk independently
    chunk_total = chunk[chunk["active"] == True]["salary"].sum()
    totals.append(chunk_total)

total_salary = sum(totals)

# For aggregations, can use chunked approach:
running_sum = 0
running_count = 0
for chunk in pd.read_csv("billion_rows.csv", chunksize=chunk_size):
    running_sum += chunk["salary"].sum()
    running_count += len(chunk)

mean_salary = running_sum / running_count
```

---

## 10. Polars — Why It Matters at Senior Level

### Why Polars Exists

```
pandas problems at scale:
  1. Single-threaded — cannot use multiple CPU cores
  2. Python GIL limits parallelism even with multiple cores attempted
  3. object dtype columns use Python objects — slow and memory-hungry
  4. API surface is inconsistent (many ways to do same thing, subtly different results)
  5. Chained indexing ambiguity (copy vs view is implementation-defined)

Polars solutions:
  1. Written in Rust — zero Python overhead in core operations
  2. Multi-threaded by default — uses all CPU cores for operations
  3. Apache Arrow columnar format — cache-efficient, avoids Python objects
  4. Query optimizer — lazy API reorders and fuses operations automatically
  5. Consistent, explicit API — no SettingWithCopyWarning, no copy/view ambiguity
```

### Java Developer Bridge — Polars

| Concept | pandas | polars |
|---|---|---|
| Execution | Eager only | Eager or lazy |
| Thread safety | Single-threaded core | Multi-threaded by default |
| In-memory format | NumPy arrays | Apache Arrow |
| Mutability | DataFrames are mutable | DataFrames are immutable |
| Null handling | `NaN` (float) and `None` mixed | `null` — consistent |
| String handling | Python `object` dtype | Arrow `Utf8` — efficient |

### Polars Basics

```python
import polars as pl

# Eager mode — immediately evaluates
df = pl.read_csv("data.csv")

# Inspect
print(df.schema)           # column names and types (no dtype — it's schema in polars)
print(df.shape)
print(df.head())

# Selection and filtering — method chaining, always returns new DataFrame (immutable)
result = (
    df
    .filter(pl.col("active") == True)
    .filter(pl.col("salary") > 90000)
    .select(["name", "salary", "department"])
    .sort("salary", descending=True)
)

# No .loc/.iloc — polars uses .filter() and .select() consistently
```

### Polars Lazy API — The Interview Differentiator

```python
# Lazy mode — builds a query plan, does NOT execute until .collect()
lazy = (
    pl.scan_csv("billion_rows.csv")         # scan_csv = lazy; read_csv = eager
    .filter(pl.col("active") == True)
    .groupby("department")
    .agg(pl.col("salary").mean().alias("avg_salary"))
    .sort("avg_salary", descending=True)
)

# Nothing has run yet. Polars has a query plan.
# .collect() triggers execution:
result = lazy.collect()

# What the query optimizer may do:
#   1. Predicate pushdown — filter rows before reading all columns
#   2. Projection pushdown — only read columns actually needed
#   3. Operation fusion — combine sequential operations into one pass
#   4. Parallel execution — run independent sub-plans on different threads
```

### When to Use pandas vs polars

```
Use pandas when:
  - Working with existing codebase built on pandas (migration cost is real)
  - Integrating with libraries that require pandas (sklearn, some ML pipelines)
  - Data is small enough that performance is not a concern
  - Jupyter notebooks for exploratory analysis (pandas ecosystem is mature)

Use polars when:
  - Files > 1GB or > 10M rows
  - Need multi-core processing (polars uses all cores by default)
  - Need lazy evaluation for query optimization
  - Building new data pipelines from scratch
  - Production ETL where performance matters

Interview signal:
  Knowing polars exists and can articulate why it outperforms pandas
  on large data signals senior-level production awareness.
```

---

## 11. Common Data Engineering Interview Scenarios

### Scenario 1 — Filter, Aggregate, Sort Pipeline

```python
# "Find the top 3 departments by average salary for active employees"

# pandas solution
result = (
    df[df["active"] == True]
    .groupby("department")
    .agg(avg_salary=("salary", "mean"))
    .sort_values("avg_salary", ascending=False)
    .head(3)
)

# polars lazy solution
result = (
    pl.scan_csv("employees.csv")
    .filter(pl.col("active") == True)
    .groupby("department")
    .agg(pl.col("salary").mean().alias("avg_salary"))
    .sort("avg_salary", descending=True)
    .head(3)
    .collect()
)
```

### Scenario 2 — Memory-Efficient Large File Processing

```python
# "Process a 5GB CSV file — total salary spend by department"

# pandas chunked approach
dept_totals = {}
for chunk in pd.read_csv("big.csv", chunksize=100_000):
    for dept, total in chunk.groupby("department")["salary"].sum().items():
        dept_totals[dept] = dept_totals.get(dept, 0) + total

# polars lazy approach (more elegant)
result = (
    pl.scan_csv("big.csv")
    .groupby("department")
    .agg(pl.col("salary").sum().alias("total_salary"))
    .collect()
)
# polars reads only what it needs, processes in parallel — much simpler code
```

### Scenario 3 — JSON API Data to DataFrame

```python
import requests
import pandas as pd

# Common pattern: fetch paginated API, normalize nested JSON into flat DataFrame
def fetch_all_users(base_url: str) -> pd.DataFrame:
    all_records = []
    page = 1
    while True:
        resp = requests.get(f"{base_url}/users", params={"page": page})
        data = resp.json()
        if not data["users"]:
            break
        all_records.extend(data["users"])
        page += 1

    df = pd.json_normalize(all_records)    # flattens nested dicts to columns
    return df

# pd.json_normalize handles nested keys:
# {"user": {"id": 1, "address": {"city": "NY"}}}
# → columns: user.id, user.address.city
```

---

## 12. pandas Date and Time Operations

```python
# Parse date columns (often read as strings from CSV)
df["created_at"] = pd.to_datetime(df["created_at"])

# .dt accessor — vectorized datetime operations
df["year"] = df["created_at"].dt.year
df["month"] = df["created_at"].dt.month
df["day_of_week"] = df["created_at"].dt.day_name()

# Filter by date
cutoff = pd.Timestamp("2024-01-01")
recent = df[df["created_at"] >= cutoff]

# Date arithmetic
df["days_since_created"] = (pd.Timestamp.now() - df["created_at"]).dt.days

# Time-based groupby (resample)
df.set_index("created_at").resample("M")["salary"].sum()   # monthly totals
```

---

## 13. Strong Interview Answers

### "When would you choose polars over pandas?"

```text
I would choose polars when working with datasets above a few hundred thousand rows
or when performance is production-critical. Polars is written in Rust with Apache
Arrow as its in-memory format, which means operations are cache-efficient and
multi-threaded by default. For large ETL jobs, I would use polars' lazy API with
scan_csv which applies predicate pushdown and projection pushdown automatically —
only reading rows and columns actually needed. pandas is still my choice for smaller
datasets, exploratory analysis in notebooks, and when integrating with libraries
like scikit-learn that require pandas DataFrames.
```

### "How do you handle a CSV file that is too large to fit in memory with pandas?"

```text
Two strategies. First, if I am building a new pipeline, I would use polars' lazy
API, which streams and processes the file without loading it all into memory,
applying query optimization automatically. Second, if I am using pandas, I would
use read_csv with the chunksize parameter. This returns an iterator of DataFrames;
I process each chunk and accumulate partial results, combining them at the end.
I would also specify dtype for each column at read time to avoid pandas defaulting
to int64 and object, which wastes memory.
```

### "What is the apply trap in pandas?"

```text
Calling .apply() with a Python lambda function defeats the purpose of pandas.
Under the hood, pandas uses NumPy arrays where operations are vectorized in C.
.apply() breaks out of that and calls a Python function once per row or once per
element, introducing Python interpreter overhead for every iteration. The result
is often 10 to 100 times slower than the equivalent vectorized operation.
The fix is to use built-in pandas or NumPy operations — arithmetic operators,
.str accessor methods, .dt accessor methods — which stay in the vectorized C path.
I only use .apply() for logic complex enough that it genuinely cannot be expressed
as a combination of vectorized operations.
```

---

## 14. Revision Checklist

- [ ] Can create a DataFrame from list of dicts and from a CSV
- [ ] Can explain `loc` vs `iloc` and the inclusive/exclusive difference
- [ ] Knows the chained indexing trap and how to use `.loc` to write safely
- [ ] Can write a `groupby` with multiple aggregations using named syntax
- [ ] Can perform left/inner joins with `pd.merge`
- [ ] Can explain why `apply` is slow and the vectorized alternative
- [ ] Can use `astype("category")` and explain the memory benefit
- [ ] Can read a large file in chunks with pandas
- [ ] Can explain why polars outperforms pandas on large data (Rust, Arrow, multi-threaded, lazy API)
- [ ] Can write a basic polars lazy pipeline with `scan_csv`, `filter`, `groupby`, `collect`
