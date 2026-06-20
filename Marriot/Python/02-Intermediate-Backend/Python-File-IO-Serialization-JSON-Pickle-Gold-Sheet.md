# Python File I/O, Serialization, JSON, CSV & Pickle — Gold Sheet

> **Track**: Python Interview Track — Group 2: Intermediate Backend  
> **File**: 4 of 5 (Track File #11)  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-Modules-Packaging-Venv-Pip-Poetry-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `open()` modes — `r`, `w`, `a`, `rb`, `wb`, `x` | ★★★★★ | Java has `FileReader`/`FileWriter`/`FileInputStream` — Python unifies into one call |
| `with open(...) as f:` — context manager for files | ★★★★★ | Java `try-with-resources`; Python equivalent; forgetting it causes file handle leaks |
| `pathlib.Path` vs `os.path` | ★★★★☆ | `pathlib` is the modern API (Python 3.4+); `os.path` is legacy; interviews expect `pathlib` |
| Text vs binary mode — encoding traps | ★★★★☆ | Java streams have explicit `charset` param; Python defaults to system locale (trap!) |
| `json.dumps` / `json.loads` — serialization options | ★★★★★ | `indent`, `default` hook, `object_hook`, `sort_keys`, datetime serialization |
| Custom JSON encoder/decoder | ★★★★☆ | Jackson `@JsonSerializer` equivalent; required for `datetime`, `UUID`, `Decimal` |
| `pickle` — security danger | ★★★★★ | Deserializing pickle from untrusted source = arbitrary code execution — OWASP issue |
| `csv` module — `DictReader` / `DictWriter` | ★★★☆☆ | Java has no stdlib CSV; Apache Commons CSV equivalent |
| `io.StringIO` / `io.BytesIO` — in-memory file objects | ★★★★☆ | Testing file code without touching disk; Java `StringReader`/`ByteArrayInputStream` |
| Buffered vs unbuffered I/O | ★★★☆☆ | `flush()`, `buffering` param, line-buffered stdout |
| `tempfile` — safe temporary files | ★★★☆☆ | Java `File.createTempFile()`; Python has safer alternatives |

---

## 2. `open()` — The Core File Function

### Must Know

```python
# Signature: open(file, mode='r', buffering=-1, encoding=None,
#                 errors=None, newline=None, closefd=True, opener=None)

# ALWAYS use context manager — guarantees file is closed even on exception
with open("data.txt", "r", encoding="utf-8") as f:
    content = f.read()
# File is closed here regardless of exceptions
```

### Mode Reference

| Mode | Meaning | Raises if not exists | Truncates | Java Equivalent |
|---|---|---|---|---|
| `"r"` | Read text | `FileNotFoundError` | No | `FileReader` |
| `"w"` | Write text | Creates file | Yes — wipes content! | `FileWriter` (new) |
| `"a"` | Append text | Creates file | No — adds to end | `FileWriter(file, true)` |
| `"x"` | Exclusive create | `FileExistsError` if exists | N/A | No direct equivalent |
| `"r+"` | Read + write | `FileNotFoundError` | No | `RandomAccessFile("rw")` |
| `"w+"` | Write + read | Creates file | Yes | `RandomAccessFile` + truncate |
| `"rb"` | Read binary | `FileNotFoundError` | No | `FileInputStream` |
| `"wb"` | Write binary | Creates file | Yes | `FileOutputStream` |
| `"ab"` | Append binary | Creates file | No | `FileOutputStream(file, true)` |

```python
# Exclusive create — fails if file already exists
# Safer than "w" for config files or one-time writes
with open("config.json", "x", encoding="utf-8") as f:
    f.write('{"version": "1.0"}')

# Append mode — never truncates; adds to end
with open("app.log", "a", encoding="utf-8") as f:
    f.write("INFO 2024-01-15 Service started\n")
```

### Reading Methods

```python
with open("data.txt", "r", encoding="utf-8") as f:
    # Read entire file into string
    content: str = f.read()

    # Read specific number of characters
    f.seek(0)               # Reset to beginning
    first_100: str = f.read(100)

    # Read one line (includes trailing \n)
    f.seek(0)
    line: str = f.readline()    # "First line\n"
    line_stripped = line.rstrip("\n")

    # Read all lines into a list
    f.seek(0)
    lines: list[str] = f.readlines()    # ["line1\n", "line2\n", ...]
    lines_clean = [l.rstrip("\n") for l in lines]

# BEST PRACTICE: iterate line by line — memory efficient for large files
with open("large_file.txt", "r", encoding="utf-8") as f:
    for line in f:   # f is iterable — reads one line at a time (buffered)
        process(line.rstrip("\n"))

# Read all lines, strip newlines, skip empty
with open("data.txt", "r", encoding="utf-8") as f:
    lines = [line.strip() for line in f if line.strip()]
```

### Writing Methods

```python
with open("output.txt", "w", encoding="utf-8") as f:
    # Write a string (no automatic newline!)
    f.write("Hello, World\n")
    f.write("Second line\n")

    # Write multiple lines at once
    lines = ["line one\n", "line two\n", "line three\n"]
    f.writelines(lines)   # writelines does NOT add newlines!

# print() can write to files
with open("output.txt", "w", encoding="utf-8") as f:
    print("Hello", file=f)           # Adds newline automatically
    print("World", end="", file=f)   # No newline
    print(42, "items", sep=", ", file=f)
```

---

## 3. Encoding — The Number One Trap

### Must Know

```python
# Java: Every Reader/Writer requires an explicit charset
# Python: open() defaults to platform locale — DANGER!

import sys
print(sys.getdefaultencoding())       # 'utf-8' (Python's internal)
print(sys.stdout.encoding)            # Platform-dependent! Could be 'ascii' on some servers

# On Windows: default might be cp1252
# On Linux CI: might be ascii or latin-1
# On macOS: usually utf-8

# ALWAYS specify encoding explicitly:
with open("data.txt", "r", encoding="utf-8") as f:   # Explicit — safe
    content = f.read()

# NEVER rely on the default:
with open("data.txt", "r") as f:   # Implicit — platform-dependent, DANGEROUS
    content = f.read()

# encoding="utf-8-sig" handles UTF-8 BOM (from Windows tools)
with open("windows_file.csv", "r", encoding="utf-8-sig") as f:
    ...

# errors parameter controls what happens on undecodable bytes:
# "strict" (default) — raises UnicodeDecodeError
# "ignore"           — silently skips undecodable bytes
# "replace"          — replaces with U+FFFD replacement character
with open("data.txt", "r", encoding="utf-8", errors="replace") as f:
    content = f.read()
```

### Binary vs Text Mode

```python
# Text mode ("r", "w"):
# - Decodes bytes to str using encoding
# - Translates newlines: \r\n → \n on Windows (reading), \n → \r\n on Windows (writing)
# - Returns str objects

# Binary mode ("rb", "wb"):
# - No encoding/decoding
# - No newline translation
# - Returns bytes objects
# - Use for: images, PDFs, audio, network data, exact binary copies

# When to use binary:
with open("image.png", "rb") as f:
    header = f.read(8)   # Read PNG magic bytes
    print(header)        # b'\x89PNG\r\n\x1a\n'

with open("image.png", "rb") as src, open("copy.png", "wb") as dst:
    dst.write(src.read())   # Exact binary copy

# Reading JSON/CSV: text mode with explicit encoding
# Reading images/audio/video: binary mode
# Reading ZIP/Excel: binary mode
```

---

## 4. `pathlib` — The Modern Path API

### Must Know

`pathlib.Path` (Python 3.4+) is the modern, object-oriented path API. It replaces `os.path`, `os.makedirs`, `glob.glob`, and related functions. Use `pathlib` in all new code.

```python
from pathlib import Path

# Create a Path object (does NOT create the file/directory)
p = Path("/home/alice/data/report.csv")
p = Path(".")           # Current directory
p = Path.home()         # Home directory: /home/alice or /Users/alice
p = Path.cwd()          # Current working directory

# Path components
p = Path("/home/alice/data/report.csv")
print(p.name)       # "report.csv"     — filename with extension
print(p.stem)       # "report"         — filename without extension
print(p.suffix)     # ".csv"           — extension including dot
print(p.suffixes)   # [".csv"]         — list (multiple for .tar.gz → ['.tar', '.gz'])
print(p.parent)     # /home/alice/data — parent directory
print(p.parents[0]) # /home/alice/data
print(p.parents[1]) # /home/alice
print(p.parts)      # ('/', 'home', 'alice', 'data', 'report.csv')
print(p.anchor)     # '/'              — root
```

### Path Construction and Traversal

```python
from pathlib import Path

# / operator joins paths (cleaner than os.path.join)
base = Path("/home/alice")
config = base / "config" / "settings.json"   # /home/alice/config/settings.json

# Always prefer / over string concatenation or os.path.join
# Java equivalent: Paths.get("/home/alice").resolve("config").resolve("settings.json")

# Build relative path
project = Path(".")
src = project / "src" / "myapp"
tests = project / "tests"
config_file = project / "pyproject.toml"

# Absolute path resolution
rel = Path("../data/file.txt")
abs_path = rel.resolve()   # Returns absolute Path, resolves .., symlinks
print(abs_path)             # /home/alice/data/file.txt (fully resolved)

# Change file name/extension
p = Path("archive/report.csv")
p_json = p.with_suffix(".json")    # archive/report.json
p_renamed = p.with_name("data.csv")  # archive/data.csv
p_stem = p.with_stem("summary")    # archive/summary.csv (Python 3.9+)
```

### File/Directory Operations

```python
from pathlib import Path

p = Path("data/output/report.json")

# Existence and type checks
p.exists()       # True if exists (file or dir)
p.is_file()      # True if exists and is a file
p.is_dir()       # True if exists and is a directory
p.is_symlink()   # True if it's a symlink

# Create directories
p.parent.mkdir(parents=True, exist_ok=True)
# parents=True — creates all intermediate dirs (like mkdir -p)
# exist_ok=True — no error if already exists

# Read and write (simple convenience methods)
text = p.read_text(encoding="utf-8")    # Reads entire file as str
p.write_text("content\n", encoding="utf-8")  # Writes str; creates or overwrites

data = p.read_bytes()     # Reads entire file as bytes
p.write_bytes(b"\x00\x01\x02")  # Writes bytes

# File metadata
print(p.stat().st_size)    # File size in bytes
print(p.stat().st_mtime)   # Last modified timestamp

# Rename and delete
p.rename(p.parent / "report_v2.json")   # Rename (moves to new path)
p.unlink(missing_ok=True)               # Delete file; no error if missing (Python 3.8+)
p.parent.rmdir()                        # Remove empty directory

# Remove non-empty directory
import shutil
shutil.rmtree(Path("build/"))   # Like rm -rf — USE WITH CARE

# Copy
shutil.copy2(Path("source.txt"), Path("dest.txt"))  # Copies file with metadata
shutil.copytree(Path("src/"), Path("dst/"))          # Copies directory tree
```

### Globbing and Directory Iteration

```python
from pathlib import Path

base = Path("src/")

# glob — find files matching a pattern
py_files = list(base.glob("*.py"))           # Direct children only
all_py = list(base.rglob("*.py"))            # Recursive — all .py files in tree
md_files = list(base.glob("**/*.md"))        # ** = any depth (same as rglob("*.md"))

# Iterate directory contents
for item in base.iterdir():
    if item.is_file():
        print(f"File: {item.name}")
    elif item.is_dir():
        print(f"Dir: {item.name}/")

# Find all Python files with specific naming pattern
test_files = [f for f in base.rglob("test_*.py") if f.is_file()]

# Open a pathlib Path directly
p = Path("data.txt")
with open(p, "r", encoding="utf-8") as f:    # pathlib.Path works with open()
    content = f.read()

# Or use the path's open method
with p.open("r", encoding="utf-8") as f:
    content = f.read()
```

---

## 5. `io` Module — In-Memory File Objects

### Must Know

`io.StringIO` and `io.BytesIO` are in-memory file-like objects with the same API as real files. Critical for testing code that reads/writes files without touching disk.

```python
import io

# StringIO — in-memory text file (like Java StringReader/StringWriter)
buffer = io.StringIO()
buffer.write("Hello\n")
buffer.write("World\n")
print(f"Value: '{buffer.getvalue()}'")   # "Hello\nWorld\n"

# Reset and read
buffer.seek(0)
line = buffer.readline()   # "Hello\n"

# Initial content
buffer2 = io.StringIO("pre-loaded content\n")
buffer2.seek(0)
print(buffer2.read())   # "pre-loaded content\n"

# BytesIO — in-memory binary file (like Java ByteArrayInputStream/ByteArrayOutputStream)
bio = io.BytesIO()
bio.write(b"\x89PNG\r\n\x1a\n")   # Write bytes
bio.seek(0)
header = bio.read(4)   # b'\x89PNG'
print(bio.getvalue())  # All bytes written so far

# Testing with StringIO
import csv

def parse_csv_from_string(csv_text: str) -> list[dict]:
    reader = csv.DictReader(io.StringIO(csv_text))
    return list(reader)

data = parse_csv_from_string("name,age\nAlice,30\nBob,25")
print(data)   # [{'name': 'Alice', 'age': '30'}, {'name': 'Bob', 'age': '25'}]
```

---

## 6. `json` Module — Serialization

### Must Know

```python
import json

# Python object → JSON string (serialization)
data = {
    "name": "Alice",
    "age": 30,
    "scores": [95, 87, 92],
    "active": True,
    "address": {"city": "Boston", "zip": "02101"},
    "notes": None
}

json_str = json.dumps(data)               # Compact — no spaces
json_str = json.dumps(data, indent=2)     # Pretty-print with 2-space indent
json_str = json.dumps(data, sort_keys=True)  # Alphabetical keys — stable output
json_str = json.dumps(data, indent=2, sort_keys=True, ensure_ascii=False)
# ensure_ascii=False: allow Unicode chars directly (default True escapes them)

# JSON string → Python object (deserialization)
parsed = json.loads(json_str)
print(type(parsed))   # <class 'dict'>

# File-based: write to file
with open("data.json", "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

# File-based: read from file
with open("data.json", "r", encoding="utf-8") as f:
    loaded = json.load(f)
```

### Python ↔ JSON Type Mapping

| Python | JSON | Notes |
|---|---|---|
| `dict` | `{}` object | Keys must be strings in JSON; `json.dumps` converts int keys to strings |
| `list`, `tuple` | `[]` array | Both serialize to array; `json.loads` always returns `list` |
| `str` | `"string"` | Direct mapping |
| `int` | number | Direct mapping |
| `float` | number | `nan`, `inf` raise `ValueError` by default |
| `True` / `False` | `true` / `false` | Case-sensitive JSON keywords |
| `None` | `null` | Direct mapping |
| `datetime` | **NOT SUPPORTED** | Must use custom encoder |
| `bytes` | **NOT SUPPORTED** | Must encode to base64 or str first |
| `Decimal` | **NOT SUPPORTED** | Must convert to `float` or `str` |
| `UUID` | **NOT SUPPORTED** | Must convert to `str` |

### Custom JSON Encoder

```python
import json
from datetime import datetime, date
from decimal import Decimal
from uuid import UUID
from enum import Enum

class AppJSONEncoder(json.JSONEncoder):
    """Custom encoder for types not supported by stdlib json."""

    def default(self, obj):
        # datetime → ISO 8601 string
        if isinstance(obj, datetime):
            return obj.isoformat()
        if isinstance(obj, date):
            return obj.isoformat()
        # Decimal → string (preserves precision; use float if precision loss is OK)
        if isinstance(obj, Decimal):
            return str(obj)
        # UUID → string
        if isinstance(obj, UUID):
            return str(obj)
        # Enum → value
        if isinstance(obj, Enum):
            return obj.value
        # Pydantic models
        if hasattr(obj, "model_dump"):
            return obj.model_dump()
        # Fallback to default behavior (raises TypeError for unsupported types)
        return super().default(obj)

data = {
    "id": UUID("12345678-1234-5678-1234-567812345678"),
    "created_at": datetime(2024, 1, 15, 10, 30, 0),
    "price": Decimal("99.99"),
}
json_str = json.dumps(data, cls=AppJSONEncoder, indent=2)
print(json_str)
# {
#   "id": "12345678-1234-5678-1234-567812345678",
#   "created_at": "2024-01-15T10:30:00",
#   "price": "99.99"
# }
```

### Custom JSON Decoder — `object_hook`

```python
import json
from datetime import datetime
from decimal import Decimal

def object_hook(dct: dict) -> dict:
    """Transform dict values during deserialization."""
    # Convert ISO datetime strings to datetime objects
    for key, value in dct.items():
        if isinstance(value, str) and "T" in value:
            try:
                dct[key] = datetime.fromisoformat(value)
            except ValueError:
                pass  # Not a datetime string
    return dct

json_str = '{"name": "Event", "created_at": "2024-01-15T10:30:00", "count": 5}'
parsed = json.loads(json_str, object_hook=object_hook)
print(type(parsed["created_at"]))   # <class 'datetime.datetime'>

# parse_float — control how float values are parsed
parsed_decimal = json.loads('{"price": 99.99}', parse_float=Decimal)
print(type(parsed_decimal["price"]))   # <class 'decimal.Decimal'>
print(parsed_decimal["price"])         # Decimal('99.99') — exact, no float imprecision
```

### JSON Trap — Duplicate Keys

```python
import json

# JSON spec does not prohibit duplicate keys
# Python json.loads silently takes the LAST value for duplicate keys
json_str = '{"key": "first", "key": "second"}'
result = json.loads(json_str)
print(result)   # {'key': 'second'} — first value silently overwritten!

# Java's Jackson raises an exception for duplicate keys by default
# Python silently ignores — a potential data integrity bug
```

---

## 7. `csv` Module

### Reading CSV

```python
import csv
from pathlib import Path

# DictReader — reads each row as a dict with column headers as keys
with open("employees.csv", "r", encoding="utf-8", newline="") as f:
    # newline="" is REQUIRED for csv module — prevents double newline translation
    reader = csv.DictReader(f)
    print(reader.fieldnames)   # ['name', 'department', 'salary']
    for row in reader:
        print(row)   # {'name': 'Alice', 'department': 'Engineering', 'salary': '95000'}
        # All values are strings — must convert manually
        salary = int(row["salary"])

# reader.fieldnames is populated after first iteration OR after construction if header exists

# Custom delimiter (TSV, pipe-separated, etc.)
with open("data.tsv", "r", encoding="utf-8", newline="") as f:
    reader = csv.DictReader(f, delimiter="\t")
    rows = list(reader)

# With explicit fieldnames (no header row in file)
with open("data.csv", "r", encoding="utf-8", newline="") as f:
    reader = csv.DictReader(f, fieldnames=["id", "name", "score"])
    rows = list(reader)

# Raw reader — each row is a list of strings
with open("data.csv", "r", encoding="utf-8", newline="") as f:
    reader = csv.reader(f)
    header = next(reader)   # Skip header row
    for row in reader:
        print(row)          # ['Alice', '30', 'Engineering']
```

### Writing CSV

```python
import csv
from dataclasses import dataclass, asdict
from typing import Iterable

@dataclass
class Employee:
    name: str
    department: str
    salary: int

employees = [
    Employee("Alice", "Engineering", 95000),
    Employee("Bob", "Marketing", 75000),
]

# DictWriter
with open("out.csv", "w", encoding="utf-8", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=["name", "department", "salary"])
    writer.writeheader()   # Writes: name,department,salary
    for emp in employees:
        writer.writerow(asdict(emp))   # {"name": "Alice", ...}

    # Or write all rows at once
    writer.writerows([asdict(e) for e in employees])

# csv.writer — write list of values
with open("out.csv", "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f, quoting=csv.QUOTE_NONNUMERIC)
    writer.writerow(["name", "department", "salary"])
    writer.writerows([["Alice", "Engineering", 95000], ["Bob", "Marketing", 75000]])

# TRAP: always use newline="" in open() for CSV writing on Windows
# Without it, csv.writer adds an extra blank line between rows on Windows (\r\n → \r\r\n)
```

### CSV Dialect Configuration

```python
import csv

# Built-in dialects
print(csv.list_dialects())   # ['excel', 'excel-tab', 'unix']
# excel = default (comma separator, \r\n line terminator, quotechar='"')
# unix = Unix-style (\n line terminator)

# Custom dialect
csv.register_dialect(
    "pipe_separated",
    delimiter="|",
    quotechar='"',
    quoting=csv.QUOTE_MINIMAL,
    lineterminator="\n",
)

with open("data.psv", "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f, dialect="pipe_separated")
    writer.writerow(["name", "value"])
    writer.writerow(["Alice|Developer", 42])   # Field with | will be quoted automatically
```

---

## 8. `pickle` — Binary Serialization (SECURITY WARNING)

### Must Know — Security First

```python
# pickle serializes Python objects to binary format and deserializes them back
# CRITICAL SECURITY WARNING: NEVER deserialize pickle data from untrusted sources
# Deserializing malicious pickle data executes arbitrary code on your system
# This is an OWASP A08 (Software and Data Integrity Failures) issue

# Malicious pickle that runs shell command on deserialization:
import pickle, os

class Exploit:
    def __reduce__(self):
        return (os.system, ("rm -rf /",))  # This runs on pickle.loads()!

# If you receive this from an API or user input and call pickle.loads() → compromised!
```

### Legitimate Uses of Pickle

```python
import pickle
from pathlib import Path

# Safe use: serialize/deserialize your OWN data within your own system
# Example: caching ML model objects, serializing complex Python state

data = {
    "model_weights": [1.0, 2.5, 3.7],
    "config": {"layers": [64, 32, 16], "activation": "relu"},
    "metadata": {"trained_at": "2024-01-15", "accuracy": 0.95}
}

# Serialize to file (always use binary mode!)
with open("model_cache.pkl", "wb") as f:
    pickle.dump(data, f, protocol=pickle.HIGHEST_PROTOCOL)

# Deserialize — ONLY from trusted, internal sources
with open("model_cache.pkl", "rb") as f:
    loaded = pickle.load(f)

print(loaded["config"])   # {'layers': [64, 32, 16], 'activation': 'relu'}

# Serialize to bytes (in-memory)
serialized: bytes = pickle.dumps(data)
deserialized = pickle.loads(serialized)

# Protocol versions:
# pickle.HIGHEST_PROTOCOL — most efficient, newest format
# 0 = ASCII-compatible (very old)
# 5 = Python 3.8+ best protocol
# Always use HIGHEST_PROTOCOL for new code
```

### When to Use Pickle vs JSON vs Other Formats

| Format | Speed | Human-readable | Language-portable | Handles Python types | Security |
|---|---|---|---|---|---|
| `json` | Fast | Yes | Yes | Only basic types | Safe |
| `pickle` | Very fast | No | No (Python only) | All Python types | DANGEROUS from untrusted |
| `csv` | Fast | Yes | Yes | Strings only | Safe |
| `msgpack` | Fastest | No | Yes | Basic types | Safe |
| Pydantic JSON | Fast | Yes | Yes | Model + types | Safe |
| `shelve` | Medium | No | No | All Python types | DANGEROUS (uses pickle) |

```python
# Safer alternatives to pickle for cross-service communication:
# 1. JSON + custom encoder (for datetimes, UUIDs, etc.)
# 2. Pydantic model_dump_json()
# 3. Protocol Buffers (protobuf) — language-neutral binary
# 4. MessagePack — compact binary, multi-language

# For ML models specifically:
# - PyTorch: torch.save() / torch.load() (uses pickle internally — internal use only)
# - scikit-learn: joblib.dump() / joblib.load() (safer interface around pickle)
# - ONNX: cross-platform model exchange format (preferred for production)
```

---

## 9. Buffered I/O and `flush()`

### How Python I/O Layers Work

```
Application code
     ↓
Python io.BufferedWriter     ← in-memory buffer (usually 8 KB)
     ↓
Python io.FileIO             ← OS file descriptor
     ↓
OS kernel buffer
     ↓
Disk
```

```python
import sys

# Text files — line-buffered when writing to a terminal; block-buffered to files
# Default buffer size: 8192 bytes for disk files

with open("log.txt", "w", encoding="utf-8") as f:
    f.write("Starting...\n")
    # Data is in Python's buffer — NOT yet written to disk!
    f.flush()   # Force flush to OS (but OS may still buffer)
    # File is flushed to OS kernel buffer

# Close (via context manager exit) automatically flushes + closes
# with open(...) as f: guarantees flush on exit

# Force flush to print output immediately in scripts:
print("Processing...", flush=True)
sys.stdout.flush()   # Equivalent

# Unbuffered binary I/O (pass buffering=0)
with open("raw.bin", "wb", buffering=0) as f:
    f.write(b"\x00\x01\x02")   # Written immediately, no buffer
    # Only works in binary mode! Text mode cannot be unbuffered.

# Line-buffered mode (buffering=1) — flushes on every \n
with open("realtime.log", "w", buffering=1, encoding="utf-8") as f:
    f.write("Line 1\n")   # Flushed immediately
    f.write("Line 2\n")   # Flushed immediately

# Custom buffer size
with open("large.bin", "rb", buffering=65536) as f:   # 64 KB buffer
    data = f.read()
```

---

## 10. `tempfile` — Secure Temporary Files

```python
import tempfile
from pathlib import Path

# Named temporary file — automatically deleted when closed (delete=True by default)
with tempfile.NamedTemporaryFile(mode="w", suffix=".csv", encoding="utf-8", delete=True) as f:
    f.write("name,age\nAlice,30\n")
    temp_path = f.name   # Full path while file exists
    print(f"Temp file: {temp_path}")
# File is deleted here (context manager closes and deletes)

# Keep the file after closing (delete=False)
with tempfile.NamedTemporaryFile(mode="wb", suffix=".pkl", delete=False) as f:
    f.write(b"data")
    temp_path = Path(f.name)

# File still exists here — remember to clean up!
temp_path.unlink()

# Temporary directory — auto-deleted
with tempfile.TemporaryDirectory() as tmpdir:
    tmp = Path(tmpdir)
    (tmp / "config.json").write_text('{"debug": true}', encoding="utf-8")
    # Process files...
# Directory and all contents deleted here

# mkstemp — low-level, returns (fd, path); you manage cleanup
fd, path = tempfile.mkstemp(suffix=".json")
try:
    with open(fd, "w", encoding="utf-8") as f:   # fd = file descriptor integer
        f.write('{"key": "value"}')
finally:
    Path(path).unlink()   # Must manually delete!

# SECURITY: tempfile uses os.urandom() for unique file names — safe against prediction attacks
# Java: File.createTempFile() has similar guarantees
```

---

## 11. Advanced Patterns

### Reading Large Files Efficiently

```python
from pathlib import Path

# Chunked reading — for files too large to fit in memory
def process_large_file(path: Path, chunk_size: int = 65536) -> None:
    with open(path, "rb") as f:
        while True:
            chunk = f.read(chunk_size)
            if not chunk:
                break
            process_chunk(chunk)

# Line-by-line iteration — memory efficient for text files
def count_lines(path: Path) -> int:
    with open(path, "r", encoding="utf-8") as f:
        return sum(1 for _ in f)

# Generator for lazy line reading
def read_lines(path: Path):
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            yield line.rstrip("\n")

# Usage — only reads one line at a time into memory
for line in read_lines(Path("huge_file.txt")):
    process(line)
```

### Writing Atomically — Prevent Partial Writes

```python
import tempfile
import shutil
from pathlib import Path

def write_atomically(dest: Path, content: str, encoding: str = "utf-8") -> None:
    """Write to a temp file, then atomically rename to destination.
    Prevents corrupt files if the process crashes mid-write."""
    tmp_fd, tmp_path = tempfile.mkstemp(dir=dest.parent, suffix=".tmp")
    try:
        with open(tmp_fd, "w", encoding=encoding) as f:
            f.write(content)
            f.flush()
        Path(tmp_path).replace(dest)   # Atomic on same filesystem (POSIX rename)
    except Exception:
        Path(tmp_path).unlink(missing_ok=True)
        raise

# Java equivalent: write to temp file, then Files.move(tmp, dest, ATOMIC_MOVE)
```

### File Locking (Cross-Process)

```python
# Python's built-in file operations are NOT thread-safe for concurrent writes
# For cross-process file locking, use filelock library:
# pip install filelock

from filelock import FileLock
from pathlib import Path

lock_path = Path("data.json.lock")
data_path = Path("data.json")

with FileLock(lock_path):
    # Only one process can be in this block at a time
    data = data_path.read_text(encoding="utf-8")
    # modify data...
    data_path.write_text(data, encoding="utf-8")
```

---

## 12. Java Developer Bridge

| Concept | Java | Python |
|---|---|---|
| Open a file | `new FileReader("f.txt")` | `open("f.txt", "r", encoding="utf-8")` |
| Auto-close file | `try (FileReader fr = ...)` | `with open(...) as f:` |
| Read entire file | `Files.readString(path)` | `Path("f.txt").read_text(encoding="utf-8")` |
| Read lines | `Files.readAllLines(path)` | `Path("f.txt").read_text().splitlines()` or iterate `f` |
| Write to file | `Files.writeString(path, str)` | `Path("f.txt").write_text(str, encoding="utf-8")` |
| Path construction | `Paths.get("/a/b/c")` | `Path("/a/b/c")` |
| Join paths | `path.resolve("child")` | `path / "child"` |
| File exists | `Files.exists(path)` | `path.exists()` |
| Make directories | `Files.createDirectories(path)` | `path.mkdir(parents=True, exist_ok=True)` |
| Delete file | `Files.delete(path)` | `path.unlink()` |
| Copy file | `Files.copy(src, dst)` | `shutil.copy2(src, dst)` |
| Move file | `Files.move(src, dst)` | `src.rename(dst)` or `shutil.move(src, dst)` |
| List directory | `Files.list(dir)` | `dir.iterdir()` |
| Recursive find | `Files.walk(dir)` | `dir.rglob("**")` or `dir.rglob("*.py")` |
| In-memory text stream | `new StringReader(str)` | `io.StringIO(str)` |
| In-memory byte stream | `new ByteArrayInputStream(bytes)` | `io.BytesIO(bytes)` |
| Serialize to JSON | `Jackson objectMapper.writeValueAsString()` | `json.dumps()` |
| Deserialize JSON | `objectMapper.readValue(str, Type.class)` | `json.loads(str)` |
| Custom serializer | `@JsonSerializer` | `json.JSONEncoder.default()` |
| Object serialization | `ObjectOutputStream` (Serializable) | `pickle.dumps()` — DANGEROUS from untrusted |
| Temp file | `File.createTempFile()` | `tempfile.NamedTemporaryFile()` |
| Atomic write | `Files.move(tmp, dst, ATOMIC_MOVE)` | `Path.replace()` after writing to temp |
| CSV parsing | Apache Commons CSV | `csv.DictReader` |
| Flush buffer | `writer.flush()` | `f.flush()` |
| Charset | `Charset.forName("UTF-8")` | `encoding="utf-8"` parameter |

---

## 13. Hot Interview Q&A

**Q: Why should you always use `with open(...)` instead of `open()` followed by `f.close()`?**  
A: The `with` statement guarantees the file is closed even if an exception is raised inside the block. Without it, if your code raises an exception after `open()` but before `f.close()`, the file descriptor leaks. On Linux, there is a per-process limit of open file descriptors (typically 1024). In long-running services, leaked descriptors eventually cause `OSError: [Errno 24] Too many open files`. This mirrors Java's `try-with-resources`.

**Q: What is the danger of `pickle` with untrusted data?**  
A: Deserializing pickle data executes arbitrary Python code. The `__reduce__` protocol allows a pickled object to specify any callable with any arguments — including `os.system("rm -rf /")`. Any pickle data received from an HTTP request, network socket, or user-uploaded file must NEVER be passed to `pickle.loads()`. Use JSON, Pydantic, or Protocol Buffers for external data. This is an OWASP A08 vulnerability.

**Q: What is the difference between `json.dump()` and `json.dumps()`?**  
A: `json.dumps()` (dump-string) serializes to a Python `str`. `json.dump()` serializes directly to a file-like object (writes to a file). Similarly, `json.loads()` deserializes from a `str`; `json.load()` reads from a file. The `s` suffix = "string" — a consistent naming convention across the module.

**Q: Why does `csv.DictReader` need `newline=""` in the `open()` call?**  
A: The CSV module handles its own newline translation. If you open the file in text mode without `newline=""`, Python's universal newline translation runs first — converting `\r\n` to `\n` — and then the CSV reader sees different line endings. Worse, on Windows this can produce blank rows because `\r\n` fields with embedded `\r` characters get split incorrectly. `newline=""` disables Python's newline translation and lets the csv module handle it correctly.

**Q: What is `pathlib` and why prefer it over `os.path`?**  
A: `pathlib.Path` is an object-oriented path API introduced in Python 3.4. It combines `os.path`, `os.makedirs`, `glob.glob`, `shutil.copy`, and `open()` into a coherent, chainable object. `os.path.join(base, "sub", "file.txt")` becomes `base / "sub" / "file.txt"`. Path operations like `p.exists()`, `p.mkdir()`, `p.read_text()`, and `p.write_text()` are more readable and less error-prone than their `os.path` equivalents. All new Python code should use `pathlib`.

**Q: How do you serialize Python `datetime` objects to JSON?**  
A: The stdlib `json` module does not support `datetime` by default and raises `TypeError`. The standard approaches: (1) Create a custom `json.JSONEncoder` subclass that overrides `default()` and returns `obj.isoformat()` for datetime instances. (2) Convert datetimes to strings before passing to `json.dumps()`. (3) Use Pydantic's `model_dump_json()` — it handles datetimes automatically as ISO 8601. In production FastAPI code, Pydantic handles all serialization; the custom encoder pattern is used in non-Pydantic code.

**Q: What is `io.StringIO` used for in testing?**  
A: `io.StringIO` creates an in-memory object that behaves exactly like a real file (supports `read()`, `write()`, `seek()`, `readline()`, etc.) but lives in memory. In tests, you can pass a `StringIO` object to any function that expects a file object, without creating temporary files on disk. This makes tests faster, self-contained, and free of cleanup concerns. Same pattern as Java's `StringReader`/`StringWriter` for `BufferedReader`/`PrintWriter` tests.

**Q: How do you safely handle encoding when reading files from multiple platforms?**  
A: Always specify `encoding="utf-8"` explicitly in every `open()` call. Never rely on the default — it varies by platform (UTF-8 on macOS/Linux, CP1252 on Windows). For files that might have a BOM (Windows UTF-8 tools), use `encoding="utf-8-sig"`. For files with unknown encoding, use the `chardet` library to detect it. For strict safety in a `try`/`except` block, use `errors="replace"` or `errors="ignore"` to handle undecodable bytes gracefully.

---

## 14. Final Revision Checklist

### File I/O Basics

- [ ] I always use `with open(...) as f:` — never bare `open()` + `close()`
- [ ] I always specify `encoding="utf-8"` explicitly — never rely on the default
- [ ] I know all mode strings: `r`, `w`, `a`, `x`, `r+`, `rb`, `wb`, `ab`
- [ ] I know `"w"` truncates silently; `"x"` raises `FileExistsError` if file exists
- [ ] I iterate `for line in f:` for large files instead of `f.readlines()` (memory efficiency)

### pathlib

- [ ] I use `Path` for all path operations — not `os.path`
- [ ] I use `/` to join paths: `base / "sub" / "file.txt"`
- [ ] I use `p.read_text()`, `p.write_text()`, `p.exists()`, `p.mkdir(parents=True, exist_ok=True)`
- [ ] I use `p.rglob("*.py")` for recursive file search
- [ ] I know `p.resolve()` gives absolute path with symlinks resolved

### JSON

- [ ] I know `json.dumps()` → `str`, `json.dump()` → file, `json.loads()` ← `str`, `json.load()` ← file
- [ ] I know `json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True)` options
- [ ] I know datetime/UUID/Decimal are NOT supported — must use custom `JSONEncoder`
- [ ] I can write a `JSONEncoder.default()` override for custom types
- [ ] I know `object_hook` for custom deserialization and `parse_float=Decimal` for precision

### CSV

- [ ] I use `csv.DictReader` (rows as dicts) over raw `csv.reader` (rows as lists)
- [ ] I always pass `newline=""` to `open()` when using `csv` module
- [ ] I know all values from DictReader are strings — explicit type conversion required

### pickle and Security

- [ ] I know `pickle.loads()` from untrusted source = arbitrary code execution (OWASP A08)
- [ ] I only use pickle for internal caching of my own Python objects
- [ ] I prefer JSON + Pydantic for any data crossing system/service boundaries

### Java Developer Reminders

- [ ] `with open(...) as f:` = Java `try-with-resources` / `try (Reader r = ...)`
- [ ] `pathlib.Path("/a/b")` = Java `Paths.get("/a/b")`; `/` operator = `.resolve()`
- [ ] `json.dumps()` = Jackson `writeValueAsString()`; `json.loads()` = `readValue()`
- [ ] `io.StringIO()` = Java `StringReader`/`StringWriter`
- [ ] `pickle` is NOT like Java's `Serializable` — it is far more dangerous with no security checks

---

*File 4 of 5 — Group 2: Intermediate Backend*  
*Next: Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md*
