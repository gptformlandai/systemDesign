# Python Data Processing Interview Scenarios — Gold Sheet

> **Track File #22 of 31 · Group 4: Scenario Practice**
> For: Java developer | Level: MAANG scenario depth | Mode: data pipeline + memory drills

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Generator pipeline for large data | ★★★★★ | HIGH — Java Streams are lazy, generators lazier |
| `csv` / `json` streaming parse | ★★★★★ | HIGH — Python stdlib vs Java Jackson/OpenCSV |
| `collections.Counter` | ★★★★★ | HIGH — replaces manual frequency map |
| `itertools.groupby` trap | ★★★★★ | HIGH — must pre-sort, unlike Java `groupingBy` |
| `collections.defaultdict` grouping | ★★★★★ | HIGH — no Java direct equivalent in style |
| Sorting with `key=` | ★★★★★ | MEDIUM — similar to Java Comparator |
| `itertools.chain` / `islice` | ★★★★☆ | MEDIUM |
| `operator.itemgetter` / `attrgetter` | ★★★★☆ | MEDIUM — maps to `Comparator.comparing()` |
| Memory-efficient batch processing | ★★★★★ | HIGH |
| Functional: `map` / `filter` / `reduce` | ★★★★☆ | MEDIUM — maps to Java Stream `map/filter/reduce` |

---

## 2. Generator Pipelines — Memory-Efficient Data Processing

### Scenario 2-A — OOM Processing a Large CSV

**Interviewer:** "Your ETL job crashes with OOM on a 2GB CSV file. Walk me through how you fix it."

```python
# BUG — loads entire file into memory
def process_transactions(filepath: str) -> list:
    with open(filepath) as f:
        rows = f.readlines()          # 2GB into RAM immediately
    return [parse(row) for row in rows]   # another 2GB list!
```

**Fix — Generator pipeline, O(1) memory:**

```python
import csv
from typing import Iterator

def read_csv_rows(filepath: str) -> Iterator[dict]:
    """Yields one row at a time — never loads full file."""
    with open(filepath, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            yield row

def filter_high_value(rows: Iterator[dict], threshold: float) -> Iterator[dict]:
    """Lazy filter — only evaluates when consumed."""
    for row in rows:
        if float(row["amount"]) > threshold:
            yield row

def enrich_row(rows: Iterator[dict]) -> Iterator[dict]:
    """Lazy transform."""
    for row in rows:
        row["currency"] = row.get("currency", "USD").upper()
        yield row

def process_transactions(filepath: str):
    pipeline = read_csv_rows(filepath)
    pipeline = filter_high_value(pipeline, threshold=1000.0)
    pipeline = enrich_row(pipeline)

    # Only here does data actually flow — pull-based
    for row in pipeline:
        db_insert(row)
```

**Memory profile:** At any moment, only ONE row is in memory — 2GB file processed with ~1KB RAM.

**Strong Answer:**
> "Generator pipelines are lazy — no data flows until consumed. Each generator in the chain only pulls from the upstream when the consumer asks for the next item. This gives O(1) memory regardless of file size. The key insight is that the `for row in pipeline` at the end is what drives all the work."

**Java Bridge:** Java `Stream` is pull-based and lazy in the same way — `stream.filter().map().forEach()` doesn't execute until the terminal operation. Python generators compose the same way, but without type safety unless you add type hints.

---

### Scenario 2-B — Batching a Generator for Bulk Insert

```python
from itertools import islice
from typing import Iterator, TypeVar

T = TypeVar("T")

def batched(iterable: Iterator[T], n: int) -> Iterator[list[T]]:
    """Yield successive n-sized batches from an iterator."""
    it = iter(iterable)
    while True:
        batch = list(islice(it, n))
        if not batch:
            break
        yield batch

# Process 1M records, inserting 500 at a time
pipeline = read_csv_rows("transactions.csv")
for batch in batched(pipeline, 500):
    db.bulk_insert(batch)   # 500 rows per INSERT — far faster than 1-by-1
```

**Python 3.12+ — `itertools.batched` is built in:**

```python
from itertools import batched   # Python 3.12+

for batch in batched(pipeline, 500):
    db.bulk_insert(batch)
```

---

### Scenario 2-C — Generator Composition with `yield from`

```python
def read_all_log_dirs(dirs: list[str]) -> Iterator[str]:
    """Flatten multiple directory sources into one stream."""
    for directory in dirs:
        yield from read_log_files(directory)   # delegate to sub-generator

def read_log_files(directory: str) -> Iterator[str]:
    import os
    for filename in os.listdir(directory):
        if filename.endswith(".log"):
            with open(os.path.join(directory, filename)) as f:
                yield from f   # line by line

# Consumer sees one flat stream regardless of directory count
for line in read_all_log_dirs(["/var/logs/app", "/var/logs/nginx"]):
    process(line)
```

**`yield from` delegates iteration to a sub-generator.** All items from the sub-generator are yielded as if they came from the outer generator. Equivalent to `for item in sub: yield item` but more efficient (C-level loop).

---

## 3. CSV and JSON Parsing Scenarios

### Scenario 3-A — Robust CSV Parsing

```python
import csv
from pathlib import Path

def parse_csv(filepath: str | Path) -> Iterator[dict]:
    with open(filepath, newline="", encoding="utf-8-sig") as f:
        # utf-8-sig strips BOM (Byte Order Mark) from Windows-generated CSVs
        reader = csv.DictReader(f)
        for row in reader:
            # Strip whitespace from keys and values (common in dirty CSVs)
            yield {k.strip(): v.strip() for k, v in row.items()}

# Write CSV
def write_csv(filepath: str, rows: list[dict], fieldnames: list[str]):
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
```

**Common CSV traps:**
- Missing `newline=""` → double newlines on Windows
- Not handling BOM → first column key has `\ufeff` prefix
- Not stripping whitespace → `" amount"` ≠ `"amount"` as dict key

---

### Scenario 3-B — Streaming JSON (NDJSON / JSON Lines)

```python
import json
from typing import Iterator

def stream_ndjson(filepath: str) -> Iterator[dict]:
    """Parse Newline-Delimited JSON (one JSON object per line)."""
    with open(filepath, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                yield json.loads(line)

# Write NDJSON — append friendly, streamable
def append_ndjson(filepath: str, record: dict):
    with open(filepath, "a", encoding="utf-8") as f:
        f.write(json.dumps(record) + "\n")
```

**Why NDJSON over regular JSON for large datasets:**
- Regular `json.load()` must parse the entire file before returning
- NDJSON can be streamed line by line — O(1) memory
- Append-friendly — add new records without rewriting the file
- Easy to `grep`, `wc -l` for record count

---

### Scenario 3-C — Large JSON with `ijson` (Streaming Parser)

```python
# BUG — large JSON fully loaded
import json

with open("events.json") as f:
    data = json.load(f)    # loads entire file (e.g., 500MB) into RAM

# Fix — streaming JSON with ijson
import ijson

def stream_json_events(filepath: str) -> Iterator[dict]:
    with open(filepath, "rb") as f:
        for event in ijson.items(f, "events.item"):
            yield event

# events.json structure:
# { "events": [ {...}, {...}, ... ] }
# ijson.items(f, "events.item") yields each array element lazily
```

---

## 4. Filtering and Transforming Scenarios

### Scenario 4-A — Comprehension vs `map`/`filter`

```python
data = [{"name": "Alice", "score": 85}, {"name": "Bob", "score": 45}, {"name": "Carol", "score": 92}]

# List comprehension — most Pythonic for transform + filter
passing = [d["name"] for d in data if d["score"] >= 60]
# ["Alice", "Carol"]

# map + filter — functional style, returns iterators (lazy)
passing = list(filter(lambda d: d["score"] >= 60, data))
names = list(map(lambda d: d["name"], passing))

# Prefer comprehensions for readability — map/filter need lambda (verbose)
# Use map/filter when composing large lazy pipelines or wrapping existing functions
```

**When `map` wins:**

```python
import json

# map with a named function is clean
raw_lines = ['{"id": 1}', '{"id": 2}', '{"id": 3}']
records = map(json.loads, raw_lines)   # lazy, no lambda needed
```

---

### Scenario 4-B — `functools.reduce` for Aggregation

```python
from functools import reduce

# Sum with reduce (prefer sum() for numbers, but reduce for custom aggregation)
total = reduce(lambda acc, x: acc + x["amount"], transactions, 0.0)

# Build a merged dict from a list of dicts
merged = reduce(lambda a, b: {**a, **b}, [{"a": 1}, {"b": 2}, {"c": 3}])
# {"a": 1, "b": 2, "c": 3}

# Running product
from operator import mul
product = reduce(mul, [1, 2, 3, 4, 5])   # 120
```

**Java Bridge:** Java `Stream.reduce(identity, accumulator)` is identical in semantics. Python's `reduce` is imported from `functools` — it is NOT a built-in in Python 3 (it was in Python 2). This surprises Java devs.

---

### Scenario 4-C — Flattening and Transformation Pipelines

```python
from itertools import chain

# Flatten a list of lists
nested = [[1, 2], [3, 4], [5, 6]]
flat = list(chain.from_iterable(nested))
# [1, 2, 3, 4, 5, 6]

# Alternative: nested comprehension
flat = [x for sublist in nested for x in sublist]

# Transform + flatten (flatMap equivalent)
sentences = ["hello world", "foo bar baz"]
words = [word for sentence in sentences for word in sentence.split()]
# ["hello", "world", "foo", "bar", "baz"]

# Java Stream equivalent:
# sentences.stream().flatMap(s -> Arrays.stream(s.split(" "))).collect(toList())
```

---

## 5. Grouping and Aggregation Scenarios

### Scenario 5-A — `itertools.groupby` Pre-Sort Trap

**Interviewer:** "Your groupby is creating duplicate groups. Why?"

```python
from itertools import groupby

data = [
    {"dept": "eng", "name": "Alice"},
    {"dept": "hr", "name": "Bob"},
    {"dept": "eng", "name": "Carol"},   # eng appears again!
]

# BUG — not sorted: groupby creates a new group on each consecutive change
for dept, members in groupby(data, key=lambda x: x["dept"]):
    print(dept, list(members))
# eng  [Alice]
# hr   [Bob]
# eng  [Carol]   ← duplicate group!

# Fix — sort first
data.sort(key=lambda x: x["dept"])
for dept, members in groupby(data, key=lambda x: x["dept"]):
    print(dept, list(members))
# eng  [Alice, Carol]
# hr   [Bob]
```

**Root Cause:** `itertools.groupby` groups CONSECUTIVE elements with the same key. If the same key appears non-consecutively, it creates separate groups. Java's `Collectors.groupingBy()` scans the entire stream — no pre-sort needed.

**Strong Answer:**
> "`itertools.groupby` is a streaming algorithm — it only looks at adjacent elements. This gives O(1) memory but requires the data to be sorted by the group key first. Java's `groupingBy` builds a full HashMap in memory — O(n) memory but no pre-sort needed. For large data, Python's sorted groupby uses O(n log n) time but O(1) additional memory."

---

### Scenario 5-B — `defaultdict` for Group-Build (No Pre-Sort)

```python
from collections import defaultdict

# In-memory groupby — no sort required, but loads all data
def group_by_dept(employees: list[dict]) -> dict[str, list]:
    groups = defaultdict(list)
    for emp in employees:
        groups[emp["dept"]].append(emp)
    return dict(groups)

result = group_by_dept(employees)
# {"eng": [Alice, Carol], "hr": [Bob]}

# Multi-level grouping
def group_by_dept_and_level(employees: list[dict]) -> dict:
    groups = defaultdict(lambda: defaultdict(list))
    for emp in employees:
        groups[emp["dept"]][emp["level"]].append(emp)
    return {k: dict(v) for k, v in groups.items()}
```

---

### Scenario 5-C — `collections.Counter` for Frequency Analysis

```python
from collections import Counter

# Word frequency
text = "the quick brown fox jumps over the lazy dog the fox"
word_freq = Counter(text.split())
# Counter({"the": 3, "fox": 2, "quick": 1, ...})

# Most common N
print(word_freq.most_common(3))
# [("the", 3), ("fox", 2), ("quick", 1)]

# Counter arithmetic
a = Counter({"apple": 3, "banana": 1})
b = Counter({"apple": 1, "cherry": 2})
print(a + b)   # Counter({"apple": 4, "cherry": 2, "banana": 1})
print(a - b)   # Counter({"apple": 2, "banana": 1})  — negative counts dropped
print(a & b)   # Counter({"apple": 1})  — min of each
print(a | b)   # Counter({"apple": 3, "cherry": 2, "banana": 1})  — max of each

# Count from a generator without building intermediate list
file_counter = Counter(word for line in open("data.txt") for word in line.split())
```

**Java Bridge:** Java has no `Counter` equivalent. The Java idiom is `Map.merge(key, 1, Integer::sum)` or `Collectors.groupingBy(identity, counting())`. Python's `Counter` is dramatically more concise.

---

## 6. Sorting Scenarios

### Scenario 6-A — Sort by Multiple Keys

```python
employees = [
    {"name": "Alice", "dept": "eng", "salary": 120000},
    {"name": "Bob",   "dept": "hr",  "salary": 80000},
    {"name": "Carol", "dept": "eng", "salary": 95000},
    {"name": "Dave",  "dept": "eng", "salary": 120000},
]

# Sort by dept ascending, then salary descending
sorted_emp = sorted(
    employees,
    key=lambda e: (e["dept"], -e["salary"])   # negate for descending
)

# With operator.itemgetter (faster than lambda — C implementation)
from operator import itemgetter
sorted_emp = sorted(employees, key=itemgetter("dept", "salary"))
# Note: itemgetter does ascending only — use lambda for mixed directions
```

---

### Scenario 6-B — Stable Sort Property

```python
# Python sort is stable — equal elements retain original order
records = [
    {"name": "Alice", "score": 85, "date": "2024-01-01"},
    {"name": "Bob",   "score": 85, "date": "2024-01-02"},
    {"name": "Carol", "score": 90, "date": "2024-01-01"},
]

# Sort by score — stable sort preserves Alice before Bob (both score=85)
sorted_records = sorted(records, key=lambda r: r["score"], reverse=True)
# Carol (90), Alice (85, appears first as original order preserved), Bob (85)
```

**Java Bridge:** Java's `List.sort()` and `Arrays.sort()` are also stable. Python's `sorted()` / `list.sort()` are guaranteed stable (TimSort). Both give the same guarantee.

---

### Scenario 6-C — Custom Object Sorting

```python
from dataclasses import dataclass, field
from functools import total_ordering

@dataclass
@total_ordering
class Employee:
    name: str
    salary: float
    dept: str

    def __lt__(self, other):
        return self.salary < other.salary

    def __eq__(self, other):
        return self.salary == other.salary

employees = [Employee("Alice", 120000, "eng"), Employee("Bob", 80000, "hr")]
sorted_emp = sorted(employees)   # uses __lt__ — ascending salary

# Or use key= without implementing comparison methods
sorted_emp = sorted(employees, key=lambda e: e.salary)
# key= is preferred — simpler and avoids total_ordering overhead
```

---

## 7. `itertools` Toolkit for Data Processing

### Scenario 7-A — `itertools.islice` for Top-N Without Sorting All

```python
import heapq
from itertools import islice

records = (parse(line) for line in open("data.csv"))

# Top 10 by score — heapq.nlargest is O(n log k), not O(n log n)
top_10 = heapq.nlargest(10, records, key=lambda r: r["score"])

# islice — take first n items from any iterator without consuming the rest
first_100 = list(islice(records, 100))
```

---

### Scenario 7-B — `itertools.chain` for Multi-Source Processing

```python
from itertools import chain
import csv

def read_csv_stream(filepath):
    with open(filepath) as f:
        yield from csv.DictReader(f)

# Process multiple CSV files as one logical stream
all_records = chain(
    read_csv_stream("jan.csv"),
    read_csv_stream("feb.csv"),
    read_csv_stream("mar.csv"),
)

for record in all_records:
    process(record)   # O(1) memory — only current record in RAM
```

---

### Scenario 7-C — `itertools.accumulate` for Running Totals

```python
from itertools import accumulate
import operator

sales = [100, 200, 150, 300, 250]

# Running sum
running_totals = list(accumulate(sales))
# [100, 300, 450, 750, 1000]

# Running maximum
running_max = list(accumulate(sales, max))
# [100, 200, 200, 300, 300]

# Running product
running_product = list(accumulate(sales, operator.mul))
# [100, 20000, 3000000, 900000000, 225000000000]
```

---

### Scenario 7-D — `itertools.tee` — Split One Iterator into N

```python
from itertools import tee

records = read_csv_rows("data.csv")

# Split into two independent iterators (WARNING: memory cost!)
records_a, records_b = tee(records, 2)

# records_a and records_b are independent — consuming one buffers for the other
count = sum(1 for _ in records_a)            # consume a
high_value = [r for r in records_b if float(r["amount"]) > 1000]   # consume b

# WARNING: tee buffers elements until both iterators have consumed them
# If one is consumed far ahead of the other, memory grows proportionally
# Prefer: process in a single pass or use a list if data fits in memory
```

---

## 8. Data Transformation Patterns

### Scenario 8-A — Normalize Nested JSON

```python
def flatten_dict(d: dict, parent_key: str = "", sep: str = ".") -> dict:
    """Recursively flatten a nested dict into dot-notation keys."""
    items = {}
    for k, v in d.items():
        new_key = f"{parent_key}{sep}{k}" if parent_key else k
        if isinstance(v, dict):
            items.update(flatten_dict(v, new_key, sep))
        else:
            items[new_key] = v
    return items

nested = {"user": {"id": 1, "address": {"city": "NYC", "zip": "10001"}}}
flat = flatten_dict(nested)
# {"user.id": 1, "user.address.city": "NYC", "user.address.zip": "10001"}
```

---

### Scenario 8-B — Pivot / Aggregate by Multiple Dimensions

```python
from collections import defaultdict

def pivot(records: list[dict], row_key: str, col_key: str, val_key: str) -> dict:
    """
    Pivot records into a 2D summary:
    { row_value: { col_value: sum(val) } }
    """
    result = defaultdict(lambda: defaultdict(float))
    for rec in records:
        result[rec[row_key]][rec[col_key]] += float(rec[val_key])
    return {k: dict(v) for k, v in result.items()}

# Example
transactions = [
    {"region": "US", "product": "A", "revenue": 100},
    {"region": "EU", "product": "A", "revenue": 200},
    {"region": "US", "product": "B", "revenue": 150},
    {"region": "US", "product": "A", "revenue": 50},
]

table = pivot(transactions, row_key="region", col_key="product", val_key="revenue")
# {"US": {"A": 150, "B": 150}, "EU": {"A": 200}}
```

---

### Scenario 8-C — Deduplication Strategies

```python
# Dedup preserving first occurrence
seen = set()
unique = []
for item in records:
    key = item["id"]
    if key not in seen:
        seen.add(key)
        unique.append(item)

# One-liner dedup for dicts — preserving order (Python 3.7+)
unique = list({item["id"]: item for item in records}.values())
# dict comprehension: later items overwrite earlier ones → use reversed() to keep first:
unique = list({item["id"]: item for item in reversed(records)}.values())[::-1]

# For hashable items only — set dedup (order NOT preserved)
unique_ids = list(set(r["id"] for r in records))
```

---

## 9. `collections` Module Toolkit

### Scenario 9-A — `namedtuple` vs `dataclass`

```python
from collections import namedtuple
from dataclasses import dataclass

# namedtuple — immutable, tuple-like, memory-efficient
Point = namedtuple("Point", ["x", "y"])
p = Point(1.0, 2.0)
print(p.x, p[0])   # attribute AND index access
# Hashable — can be used as dict key / set element

# dataclass — mutable by default, class-like, more features
@dataclass
class Point:
    x: float
    y: float
    # NOT hashable by default (mutable), add frozen=True for hashability
```

**Use namedtuple when:** read-only record, needs to be a dict key, working with tuple-based APIs.
**Use dataclass when:** fields need mutation, need methods, need `__post_init__` validation.

---

### Scenario 9-B — `collections.deque` for Sliding Window

```python
from collections import deque

def sliding_window_max(data: list[int], k: int) -> list[int]:
    """Return max of each k-element sliding window — O(n)."""
    dq = deque()   # stores indices
    result = []

    for i, val in enumerate(data):
        # Remove elements outside window
        while dq and dq[0] < i - k + 1:
            dq.popleft()
        # Remove elements smaller than current (they can never be max)
        while dq and data[dq[-1]] < val:
            dq.pop()
        dq.append(i)
        if i >= k - 1:
            result.append(data[dq[0]])

    return result

# deque is O(1) appendleft/popleft vs list O(n) insert(0, ...)
```

---

### Scenario 9-C — `collections.OrderedDict` LRU Cache Pattern

```python
from collections import OrderedDict

class LRUCache:
    def __init__(self, capacity: int):
        self.cache = OrderedDict()
        self.capacity = capacity

    def get(self, key: int) -> int:
        if key not in self.cache:
            return -1
        self.cache.move_to_end(key)   # mark as most recently used
        return self.cache[key]

    def put(self, key: int, value: int) -> None:
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.capacity:
            self.cache.popitem(last=False)   # evict least recently used

# Python 3.2+ — use functools.lru_cache for read-only caching
from functools import lru_cache

@lru_cache(maxsize=128)
def expensive_compute(n: int) -> int:
    return sum(range(n))
```

---

## 10. End-to-End Data Pipeline Scenario

### Scenario 10 — Production ETL Pipeline

**Interviewer:** "Design a memory-efficient pipeline that reads 500MB of transaction logs (NDJSON), filters fraudulent transactions (amount > 10,000 or country in blocklist), enriches them with a currency conversion, and writes results to a CSV. What does your code look like?"

```python
import json
import csv
from typing import Iterator
from decimal import Decimal

BLOCKLIST = {"KP", "IR", "CU"}
EUR_TO_USD = 1.08

# Stage 1 — Source (lazy)
def read_ndjson(filepath: str) -> Iterator[dict]:
    with open(filepath, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                yield json.loads(line)

# Stage 2 — Filter (lazy)
def is_suspicious(txn: dict) -> bool:
    return (
        float(txn.get("amount", 0)) > 10_000
        or txn.get("country") in BLOCKLIST
    )

def filter_suspicious(txns: Iterator[dict]) -> Iterator[dict]:
    for txn in txns:
        if is_suspicious(txn):
            yield txn

# Stage 3 — Enrich / Transform (lazy)
def convert_currency(txns: Iterator[dict]) -> Iterator[dict]:
    for txn in txns:
        if txn.get("currency") == "EUR":
            txn["amount_usd"] = round(float(txn["amount"]) * EUR_TO_USD, 2)
        else:
            txn["amount_usd"] = float(txn["amount"])
        yield txn

# Stage 4 — Sink (eager — drives the pipeline)
def write_csv(txns: Iterator[dict], out_path: str, batch_size: int = 500):
    FIELDS = ["id", "amount", "amount_usd", "currency", "country", "timestamp"]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDS, extrasaction="ignore")
        writer.writeheader()
        batch = []
        for txn in txns:
            batch.append(txn)
            if len(batch) >= batch_size:
                writer.writerows(batch)
                batch.clear()
        if batch:
            writer.writerows(batch)

# Assemble and run
def run_pipeline(input_path: str, output_path: str):
    pipeline = read_ndjson(input_path)
    pipeline = filter_suspicious(pipeline)
    pipeline = convert_currency(pipeline)
    write_csv(pipeline, output_path)

run_pipeline("transactions.ndjson", "suspicious.csv")
```

**Architecture recap:**
- Stages 1–3: generators — no data in memory until consumed
- Stage 4 (sink): drives the pull chain; batches writes for I/O efficiency
- Total memory: one row at a time in flight + a 500-row write buffer
- Testable: each stage is a pure function accepting and returning an iterator

---

## 11. Java Developer Bridge — Data Processing Mapping

| Python Pattern | Java Equivalent | Key Difference |
|---|---|---|
| Generator function (`yield`) | `Stream` / `Iterator` | Python lazier; generators stateful by default |
| `(x for x in data if cond)` | `stream.filter(pred)` | Python: generator expression; Java: Stream method |
| `[f(x) for x in data]` | `stream.map(f).collect(toList())` | Both eager materialization |
| `map(f, data)` | `stream.map(f)` | Both lazy |
| `filter(pred, data)` | `stream.filter(pred)` | Both lazy |
| `reduce(f, data, init)` | `stream.reduce(init, f)` | Same semantics; `reduce` imported in Python 3 |
| `itertools.chain(*iters)` | `Stream.concat` / `flatMap` | Python: varargs; Java: binary |
| `itertools.islice(it, n)` | `stream.limit(n)` | Both lazy |
| `itertools.groupby` | `Collectors.groupingBy` | **Python requires pre-sort; Java does not** |
| `defaultdict(list)` groupby | `groupingBy(toList())` | Python: explicit dict; Java: collector |
| `Counter(data)` | `groupingBy(counting())` | Python Counter much more concise |
| `sorted(key=fn)` | `stream.sorted(Comparator.comparing(fn))` | Both stable; Python key= cleaner |
| `operator.itemgetter` | `Comparator.comparing(Bean::getField)` | Similar purpose |
| `heapq.nlargest(n, it)` | `stream.sorted(reverseOrder()).limit(n)` | heapq is O(n log k); stream O(n log n) |
| `yield from sub` | N/A (use flatMap) | Python: sub-generator delegation |
| `itertools.accumulate` | `Stream.scan` (not built-in) | Java has no built-in scan |
| `collections.deque` | `ArrayDeque` | Same O(1) both ends; same use cases |
| `namedtuple` | `record` (Java 14+) | Both immutable value objects |
| `@dataclass` | POJO / `record` | dataclass more flexible than record |
| `lru_cache` | Caffeine / Guava cache | Python stdlib; Java needs a library |

---

## 12. Hot Interview Q&A

**Q1: What is the difference between a list comprehension and a generator expression in terms of memory?**
> `[x for x in data]` immediately creates a full list in memory — O(n) space. `(x for x in data)` creates a generator object that yields one item at a time — O(1) space. The generator is lazy: no work happens until you iterate it. Use generators when the full sequence isn't needed at once, especially for large files or infinite streams.

**Q2: Why must data be sorted before using `itertools.groupby`?**
> `groupby` is a streaming algorithm — it emits a new group each time the key value changes from the previous element. If equal-key elements are not adjacent (i.e., data is unsorted), the same key will appear as multiple separate groups. Java's `Collectors.groupingBy` builds an in-memory map and requires no pre-sort, at the cost of O(n) memory.

**Q3: What is `yield from` and when do you use it?**
> `yield from iterable` delegates iteration to a sub-iterator, yielding each item as if the outer generator produced it. It is equivalent to `for item in iterable: yield item` but is C-level optimized and correctly handles `.send()`, `.throw()`, and `.close()` propagation to the sub-generator. Use it to flatten nested generators or compose pipeline stages cleanly.

**Q4: How would you find the top-10 highest-revenue transactions in a 1M-row dataset without sorting all 1M rows?**
> `heapq.nlargest(10, records, key=lambda r: r["revenue"])`. `heapq.nlargest(k, iterable)` uses a min-heap of size k — O(n log k) time, O(k) space. Sorting would be O(n log n) time and O(n) space. For k << n, heapq is significantly faster.

**Q5: What is `collections.Counter` and how is it different from a regular dict frequency count?**
> `Counter` is a dict subclass specialized for counting. It accepts any iterable and counts occurrences. It provides `.most_common(n)`, arithmetic operators (`+`, `-`, `&`, `|`), and handles missing keys by returning 0 instead of raising `KeyError`. A manual dict with `d.get(k, 0) + 1` is equivalent in function but requires boilerplate for every operation.

**Q6: When should you use `itertools.tee` and what is its risk?**
> `tee(iterator, n)` splits one iterator into n independent iterators. Use it when you must make multiple passes over a single-use iterator without materializing it fully. The risk: `tee` buffers items internally. If one iterator is consumed far ahead of the other, the buffer grows proportionally. If one iterator is always ahead by k elements, memory is O(k). For large datasets, it can be better to iterate twice (if re-reads are cheap) or collect into a list.

**Q7: What is the difference between `sorted()` and `list.sort()` and why does `list.sort()` return `None`?**
> `sorted(iterable)` returns a new sorted list; the original is unchanged. `list.sort()` sorts in-place and returns `None` — this is Python's design to signal mutation. The most common bug is `my_list = my_list.sort()` which replaces the list with `None`. `sorted()` works on any iterable; `.sort()` is a method only on lists.

---

## 13. Final Revision Checklist

- [ ] Can build a 3-stage generator pipeline for large CSV processing with O(1) memory
- [ ] Can explain why generator pipelines are pull-based and when data actually flows
- [ ] Can batch a generator with `itertools.islice` for bulk inserts
- [ ] Can use `yield from` to flatten nested generator sources
- [ ] Can parse CSV with `csv.DictReader` handling BOM and whitespace
- [ ] Can stream NDJSON line by line vs loading full JSON with `json.load()`
- [ ] Can explain `itertools.groupby` pre-sort requirement and contrast with Java `groupingBy`
- [ ] Can group with `defaultdict(list)` without pre-sorting
- [ ] Can use `Counter.most_common()` and Counter arithmetic operators
- [ ] Can sort by multiple keys with mixed directions using `lambda` and `operator.itemgetter`
- [ ] Can use `heapq.nlargest`/`nsmallest` for top-k without full sort
- [ ] Can flatten a list of lists with `chain.from_iterable` and nested comprehension
- [ ] Can implement pivot/aggregate using `defaultdict(lambda: defaultdict(float))`
- [ ] Can deduplicate preserving insertion order with dict comprehension
- [ ] Can implement sliding window with `collections.deque` O(1) popleft
- [ ] Can implement LRU cache using `OrderedDict.move_to_end`
- [ ] Can flatten nested JSON dict to dot-notation keys
- [ ] Can explain the risk of `itertools.tee` and when to avoid it
- [ ] Can describe the full ETL pipeline pattern: source → filter → transform → batch sink
