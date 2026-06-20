# Python Modules, Packaging, Venv, Pip & Poetry — Gold Sheet

> **Track**: Python Interview Track — Group 2: Intermediate Backend  
> **File**: 3 of 5 (Track File #10)  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| How Python finds a module — `sys.path` search order | ★★★★★ | Java has classpath; Python has `sys.path` — same concept, different pitfalls |
| `import` vs `from x import y` — what actually executes | ★★★★★ | Every Python file runs on first import; Java classes are loaded lazily on demand |
| Circular imports — why they happen and how to fix | ★★★★★ | No Java equivalent; causes `ImportError` or partial import bugs |
| `__init__.py` — package vs directory | ★★★★☆ | Java has no concept; directory = package automatically in Java |
| Relative vs absolute imports | ★★★★☆ | Relative imports `from . import x` confuse Java developers |
| Virtual environment — why mandatory | ★★★★★ | Java dependency isolation is per-project via Maven/Gradle; Python is global by default |
| `requirements.txt` vs `pyproject.toml` vs Poetry lock file | ★★★★☆ | Maven `pom.xml` vs Poetry `pyproject.toml`; lock file = `pom.xml` with resolved versions |
| `pip install -e .` — editable install | ★★★☆☆ | No Java equivalent; lets you import your own package while developing |
| `__all__` — controlling what `from module import *` exports | ★★★☆☆ | Java has no equivalent; Python's `__all__` is the nearest thing to package access control |
| Namespace packages (no `__init__.py`) | ★★☆☆☆ | Rarely asked but appears in large monorepo codebases |
| Poetry dependency groups — main vs dev vs test | ★★★☆☆ | Maven `<scope>test</scope>` equivalent |

---

## 2. The Python Module System — What Is a Module?

### Must Know

A **module** is any Python file (`.py`). A **package** is a directory containing Python files. The Python **import system** resolves names to files and executes them the first time they are imported.

```
Java:  Classes are units of compilation. Package = directory. Classpath = search path.
Python: Files are units of execution. Package = directory with __init__.py (or none, for namespace packages).
        sys.path = search path.
```

### Everything Runs on Import

```python
# Java: loading a class does NOT run code outside methods
# Python: importing a module EXECUTES the entire file, top to bottom

# module: counter.py
print("counter.py is being imported!")   # This runs on import!

count = 0   # Module-level code executes immediately

def increment():
    global count
    count += 1

class Counter:
    pass

# importer.py
import counter   # Prints: "counter.py is being imported!"
import counter   # Prints NOTHING — already in sys.modules cache
```

### `if __name__ == "__main__"` — Module vs Script

```python
# Python uses __name__ to distinguish:
# - Running as a script: __name__ == "__main__"
# - Imported as a module: __name__ == module's dotted name

# module: myapp/utils.py
def compute(x: int) -> int:
    return x * 2

if __name__ == "__main__":
    # This block ONLY runs when you do: python utils.py
    # It does NOT run when another module does: import myapp.utils
    print(compute(21))   # Test/demo code

# This pattern is the Python equivalent of Java's public static void main(String[] args)
# It is the standard way to make a module runnable as a script AND importable as a library
```

---

## 3. `import` Statement Mechanics

### How Import Works

```python
# When Python encounters: import mymodule

# Step 1: Check sys.modules (import cache)
#   If "mymodule" is already there → return cached module object (no re-execution!)
#   If not → proceed to step 2

# Step 2: Find the module — search sys.path left to right
#   sys.path[0] = directory of the script being run (or "" for REPL)
#   sys.path[1...] = PYTHONPATH env var entries
#   sys.path[...] = site-packages (installed packages)
#   sys.path[-1] = standard library

# Step 3: Load the module
#   Execute the file top to bottom
#   Create a module object, populate its __dict__
#   Add module object to sys.modules

# Step 4: Bind the name in the current namespace

import sys
print(sys.path)   # Shows the full search path
print(sys.modules.keys())   # Shows all cached modules
```

### `import` vs `from x import y`

```python
# import module — imports the module object; access via attribute
import os
import os.path

os.getcwd()          # Access via module name
os.path.join("/", "home")  # Dotted attribute access

# from module import name — imports specific name(s) into current namespace
from os import getcwd
from os.path import join, exists

getcwd()             # Direct access — no prefix
join("/", "home")    # No os.path prefix

# from module import * — imports everything (filtered by __all__ if defined)
from os.path import *   # Not recommended — pollutes namespace

# TRAP: from x import y takes a snapshot of the value at import time
# If y is reassigned in x after import, you have the old value

# module_a.py:
count = 0
def increment():
    global count
    count += 1

# user.py:
from module_a import count   # count is now 0 in this namespace
import module_a

module_a.increment()
print(module_a.count)   # 1 — correct! Module attribute updated
print(count)            # 0 — WRONG! This is a local copy of the int value
                        # Use module_a.count when you need the live value
```

### Lazy Imports

```python
# Standard imports run at module load time — can slow startup
# Lazy imports defer until first use

# Pattern 1: Import inside function (most common)
def process_image(path: str) -> bytes:
    from PIL import Image   # Only imported when this function is called
    img = Image.open(path)
    return img.tobytes()

# Pattern 2: importlib for programmatic lazy loading
import importlib

def get_backend(name: str):
    module = importlib.import_module(f"backends.{name}")   # Dynamic import
    return module.Backend()

# Pattern 3: TYPE_CHECKING (for type hints only)
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from myapp.models import User   # Never imported at runtime

def process(user: "User") -> None: ...
```

---

## 4. Package Structure and `__init__.py`

### Must Know

A **package** is a directory. To make it importable as a package in Python 3.2 and earlier, it needed an `__init__.py`. In Python 3.3+, directories without `__init__.py` become **namespace packages** (explained later).

```
myapp/
├── __init__.py         ← Makes myapp a regular package
├── models/
│   ├── __init__.py     ← Makes models a sub-package
│   ├── user.py
│   └── order.py
├── services/
│   ├── __init__.py
│   ├── user_service.py
│   └── order_service.py
└── utils/
    ├── __init__.py
    └── helpers.py
```

### What `__init__.py` Does

```python
# myapp/__init__.py can be completely empty — just marks the directory as a package
# OR it can define what is exposed at the package level

# myapp/__init__.py
"""MyApp — a sample application package."""

# Re-export commonly used classes at the package level
from myapp.models.user import User
from myapp.models.order import Order
from myapp.services.user_service import UserService

__version__ = "1.0.0"
__author__ = "Dev Team"

# After this, users can do:
from myapp import User         # Instead of: from myapp.models.user import User
from myapp import UserService  # Instead of: from myapp.services.user_service import UserService
import myapp
print(myapp.__version__)
```

### `__init__.py` Execution Order

```python
# When you do: from myapp.models.user import User
# Python executes (in order):
# 1. myapp/__init__.py
# 2. myapp/models/__init__.py
# 3. myapp/models/user.py
# Then binds User to the current namespace

# TRAP: if myapp/__init__.py imports from myapp.models,
# and myapp/models/__init__.py imports from myapp,
# you get a circular import!
```

### `__all__` — Control Wildcard Exports

```python
# myapp/models/user.py
__all__ = ["User", "UserRole"]   # Only these are exported by "from user import *"

class User:
    pass

class UserRole:
    pass

class _InternalHelper:   # Leading underscore = private by convention
    pass

# Without __all__, "from user import *" imports everything without a leading underscore
# With __all__, "from user import *" imports ONLY what is listed

# Also useful for IDEs and documentation generators — __all__ signals public API
```

---

## 5. Relative vs Absolute Imports

### Must Know

```python
# Absolute import — from the package root (recommended)
from myapp.models.user import User
from myapp.utils.helpers import format_date

# Relative import — relative to current file's location
# Uses leading dots: . = current package, .. = parent package
from . import helpers               # From same package
from .user import User              # user.py in same directory
from ..utils.helpers import format_date  # Two levels up, then down

# myapp/services/user_service.py
from myapp.models.user import User        # Absolute — clear and unambiguous
from . import order_service                # Relative — same services package
from ..models import Order                # Relative — go up to myapp, then models
```

### When to Use Each

```python
# Absolute imports:
# - Always safe and clear
# - Required for top-level scripts run directly
# - Recommended by PEP 8 for new code

# Relative imports:
# - Useful for internal package structure that might be renamed/moved
# - Prevent name collisions with standard library modules
# - CANNOT be used in scripts run directly (no parent package context)

# Running: python myapp/services/user_service.py
# __package__ is None → relative imports fail!
# python -m myapp.services.user_service   # Module mode → relative imports WORK

# TRAP for Java devs: You cannot do relative imports in a file you run directly
# Solution: Always use python -m package.module or absolute imports
```

---

## 6. `sys.path` and `PYTHONPATH`

### How Python Finds Modules

```python
import sys

print(sys.path)
# ['', '/usr/lib/python311.zip', '/usr/lib/python3.11',
#  '/usr/lib/python3.11/lib-dynload',
#  '/home/user/.local/lib/python3.11/site-packages',
#  '/usr/local/lib/python3.11/dist-packages']

# sys.path order (searched left to right, first match wins):
# [0] = "" = directory of the script being run (or current dir in REPL)
# [1+] = PYTHONPATH environment variable entries (colon-separated on Linux/Mac)
# [n+] = site-packages directories (where pip installs packages)
# [last] = standard library directories

# TRAP: sys.path[0] = "" means the current directory is ALWAYS searched first
# If you name a file "os.py" or "json.py", it shadows the standard library module!
```

### Manipulating `sys.path`

```python
import sys
from pathlib import Path

# Add a directory to sys.path at runtime
# (Usually a code smell — prefer proper packaging)
sys.path.insert(0, str(Path(__file__).parent.parent))   # Add grandparent dir

# Better pattern: configure PYTHONPATH before running
# export PYTHONPATH=/path/to/myproject:$PYTHONPATH

# Even better: use a proper package with pip install -e .
```

### `PYTHONPATH` vs `sys.path`

```
PYTHONPATH (env var):
  - Set before running Python
  - Applied to all Python processes in that shell
  - Java equivalent: CLASSPATH environment variable

sys.path (runtime list):
  - Can be modified at runtime in code
  - Starts as: [""] + PYTHONPATH entries + stdlib + site-packages
  - Java equivalent: URLClassLoader at runtime

site-packages:
  - Where pip installs packages
  - Usually: ~/.local/lib/python3.x/site-packages (user installs)
  - Or: /usr/lib/python3.x/site-packages (system installs)
  - Java equivalent: JAR files on the classpath
```

---

## 7. Circular Imports — Detection and Fixes

### How Circular Imports Happen

```python
# models.py
from services import UserService   # imports services.py

class User:
    service: "UserService"

# services.py
from models import User            # imports models.py — circular!

class UserService:
    def create(self) -> User:
        return User()

# Python starts executing models.py
# models.py tries to import services.py
# services.py tries to import models.py
# models.py is already being loaded — Python returns the PARTIALLY EXECUTED module
# User class might not exist yet in the partial module!
# Result: ImportError or AttributeError at runtime
```

### Fix 1 — Restructure (Best)

```python
# Extract shared types into a separate module with no intra-package imports
# models/base.py — no imports from services
class User:
    name: str

# services/user_service.py
from models.base import User   # imports base, not models
class UserService:
    def create(self) -> User:
        return User(name="new")

# models/user.py
from models.base import User
from services.user_service import UserService   # No circle now
```

### Fix 2 — Import Inside Function

```python
# models.py
class User:
    def get_service(self):
        from services import UserService   # Deferred — circular resolved
        return UserService()

# services.py
from models import User

class UserService:
    def create(self) -> User:
        return User()
```

### Fix 3 — TYPE_CHECKING Guard

```python
# models.py
from __future__ import annotations
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from services import UserService   # Only for type checker, not at runtime

class User:
    service: "UserService"   # String annotation — not evaluated at runtime
```

---

## 8. Namespace Packages (Python 3.3+)

```python
# Regular package — has __init__.py
myapp/
├── __init__.py
└── models/
    ├── __init__.py
    └── user.py

# Namespace package — NO __init__.py
myapp/
└── models/
    └── user.py   # Can still be imported as myapp.models.user

# Namespace packages allow the SAME package to span multiple directories
# Used in large organizations where multiple repos contribute to the same package

# repo1/com/company/utils/formatter.py
# repo2/com/company/utils/validator.py
# Both on sys.path → "from com.company.utils import formatter, validator" works!
# Python merges them into a single namespace package

# TRAP: Never put __init__.py in a namespace package directory
# Once you add __init__.py, it becomes a regular package — namespace merging stops
```

---

## 9. Virtual Environments — Why They Are Mandatory

### Must Know

```
Java:   Each project has its own pom.xml/build.gradle; Maven/Gradle resolves deps per-project.
        Dependencies are stored in ~/.m2/repository but scoped per build.

Python: By default, pip installs packages GLOBALLY into site-packages.
        If project A needs requests==2.28 and project B needs requests==2.31 → CONFLICT.
        Virtual environments create an isolated Python + site-packages per project.
```

### Creating and Using venv

```bash
# Create a virtual environment named .venv in current directory
python3 -m venv .venv

# Directory structure created:
# .venv/
# ├── bin/             (Mac/Linux) or Scripts/ (Windows)
# │   ├── python       → symlink to Python interpreter
# │   ├── python3      → symlink
# │   └── pip          → pip for this venv
# ├── include/
# ├── lib/
# │   └── python3.x/
# │       └── site-packages/   ← isolated packages go here
# └── pyvenv.cfg

# Activate (adds .venv/bin to PATH, sets VIRTUAL_ENV env var)
source .venv/bin/activate    # Mac/Linux
.venv\Scripts\activate.bat   # Windows

# Verify activation
which python   # Should show .venv/bin/python
python --version
pip --version

# Install packages into the venv
pip install requests pydantic

# Deactivate (restore original PATH)
deactivate

# Good practice: add .venv to .gitignore
echo ".venv/" >> .gitignore
```

### Python Version Management — `pyenv`

```bash
# pyenv manages multiple Python versions on the same machine
# Like Java's SDKMAN or jenv

# Install pyenv (Mac)
brew install pyenv

# List available Python versions
pyenv install --list | grep "3\."

# Install a specific version
pyenv install 3.11.8
pyenv install 3.12.2

# Set global default
pyenv global 3.11.8

# Set version for current project directory
pyenv local 3.12.2   # Creates .python-version file

# Check what version is active
pyenv version
python --version

# Common workflow
cd myproject
pyenv local 3.11.8
python -m venv .venv
source .venv/bin/activate
```

---

## 10. pip — Package Installer for Python

### Core pip Commands

```bash
# Install a package
pip install requests
pip install "requests==2.31.0"        # Pin exact version
pip install "requests>=2.28,<3.0"     # Version range
pip install "requests[security]"      # Install with extras (optional dependencies)

# Upgrade a package
pip install --upgrade requests

# Uninstall
pip uninstall requests -y

# List installed packages
pip list
pip list --outdated   # Show packages with newer versions available

# Show package info
pip show requests     # Version, location, dependencies, home page

# Search (deprecated on PyPI; use pypi.org website)
# pip search requests   # Removed in pip 21.1

# Install from requirements.txt
pip install -r requirements.txt

# Freeze current environment to requirements.txt
pip freeze > requirements.txt

# Install without cache (useful when a cached version is corrupted)
pip install --no-cache-dir requests

# Editable install — installs your package in development mode
# Changes to source files are immediately reflected without reinstall
pip install -e .   # . refers to current directory (must have pyproject.toml or setup.py)
# Java equivalent: mvn install + classpath pointing to src/ instead of compiled JAR
```

### `requirements.txt` Format

```
# requirements.txt — simple list of dependencies
requests==2.31.0
pydantic==2.5.0
fastapi>=0.100.0,<0.200.0
uvicorn[standard]==0.24.0

# Separate test deps by convention
pytest==7.4.0
pytest-asyncio==0.21.0
httpx==0.25.0     # Test client for FastAPI

# Include other requirements files
-r base.txt
-r dev-requirements.txt

# Install from git (avoid in production)
git+https://github.com/org/repo.git@main#egg=mylib

# Install from local path
-e /path/to/local/package
```

### `requirements.txt` Pitfalls

```bash
# pip freeze > requirements.txt captures ALL packages including transitive dependencies
# This can make requirements.txt noisy (100+ lines for 5 direct deps)

# Better practice: requirements.in for direct deps + pip-tools for pinning
# pip install pip-tools
# pip-compile requirements.in   # Generates requirements.txt with all pinned transitive deps
# pip-sync requirements.txt     # Installs exactly what's in the file, removes extras

# requirements.txt has NO concept of dev vs prod dependencies
# Convention: maintain separate files
# requirements.txt       — production deps
# requirements-dev.txt   — development deps (pip install -r requirements-dev.txt)
# requirements-test.txt  — test deps

# Java Maven equivalent:
# requirements.txt ≈ pom.xml <dependencies> (scope=compile)
# requirements-dev.txt ≈ pom.xml <dependencies> (scope=provided + scope=test)
```

---

## 11. `pyproject.toml` — The Modern Python Manifest

### Must Know

`pyproject.toml` (PEP 517/518/621) is the modern standard for Python project configuration. It replaces `setup.py`, `setup.cfg`, `requirements.txt`, and consolidates tool configuration (`pytest`, `mypy`, `ruff`, `black`) into a single file.

```toml
# pyproject.toml — the Python equivalent of pom.xml

[build-system]
requires = ["hatchling"]   # or "setuptools>=61", "poetry-core", "flit-core"
build-backend = "hatchling.build"

[project]
name = "my-awesome-app"
version = "1.2.3"
description = "A production-grade Python service"
readme = "README.md"
requires-python = ">=3.11"
license = {text = "MIT"}
authors = [
  {name = "Alice Dev", email = "alice@example.com"}
]

# Direct runtime dependencies (like pom.xml <scope>compile</scope>)
dependencies = [
    "fastapi>=0.100.0",
    "pydantic>=2.0.0",
    "sqlalchemy>=2.0.0",
    "httpx>=0.25.0",
]

[project.optional-dependencies]
# Optional dependency groups — installed with: pip install my-app[dev]
dev = [
    "pytest>=7.4.0",
    "pytest-asyncio>=0.21.0",
    "mypy>=1.7.0",
    "ruff>=0.1.0",
]
docs = [
    "mkdocs>=1.5.0",
    "mkdocs-material>=9.4.0",
]

[project.scripts]
# CLI entry points — like Maven exec:java
my-app = "myapp.cli:main"   # Run: my-app from command line after install

[tool.pytest.ini_options]
# pytest configuration — no need for pytest.ini
testpaths = ["tests"]
asyncio_mode = "auto"

[tool.mypy]
python_version = "3.11"
strict = true
ignore_missing_imports = true

[tool.ruff]
line-length = 100
select = ["E", "F", "I"]    # Error, Pyflakes, import sorting
```

---

## 12. Poetry — Dependency Management at MAANG Scale

### Must Know

Poetry is a complete dependency management and packaging tool. It provides:

1. **Dependency resolution** — like Maven's dependency resolver, resolves the full transitive dependency tree
2. **Lock file** — `poetry.lock` pins every package to an exact version (including transitive deps)
3. **Virtual environment management** — automatically creates and manages venvs
4. **Build and publish** — packages and publishes to PyPI

```bash
# Install Poetry
curl -sSL https://install.python-poetry.org | python3 -

# Create a new project
poetry new myapp
# Creates:
# myapp/
# ├── pyproject.toml
# ├── README.md
# ├── myapp/
# │   └── __init__.py
# └── tests/
#     └── __init__.py

# Initialize Poetry in an existing project
cd existing-project
poetry init   # Interactive setup

# Add dependencies
poetry add fastapi                        # Adds to [tool.poetry.dependencies]
poetry add "pydantic>=2.0.0"
poetry add --group dev pytest mypy ruff  # Dev dependencies group
poetry add --group test pytest-asyncio   # Test group

# Install all deps (creates venv automatically if not exists)
poetry install

# Install without dev deps (production)
poetry install --only main

# Run a command inside the venv without activating it
poetry run python -m pytest
poetry run uvicorn myapp.main:app --reload

# Activate the venv in current shell
poetry shell

# Show dependency tree
poetry show --tree

# Update dependencies (respecting constraints in pyproject.toml)
poetry update
poetry update requests   # Update one package

# Check for security vulnerabilities
poetry check

# Export to requirements.txt (for Docker or systems that don't use Poetry)
poetry export -f requirements.txt --output requirements.txt --without-hashes
poetry export -f requirements.txt --with dev --output requirements-dev.txt
```

### Poetry `pyproject.toml` — Full Structure

```toml
[tool.poetry]
name = "my-service"
version = "0.1.0"
description = "Production Python microservice"
authors = ["Alice Dev <alice@example.com>"]
packages = [{include = "myservice"}]

[tool.poetry.dependencies]
python = "^3.11"
fastapi = "^0.104.0"
pydantic = "^2.5.0"
pydantic-settings = "^2.1.0"
sqlalchemy = {version = "^2.0", extras = ["asyncio"]}   # With extra
httpx = "^0.25.0"

[tool.poetry.group.dev.dependencies]
pytest = "^7.4.0"
pytest-asyncio = "^0.21.0"
pytest-cov = "^4.1.0"
mypy = "^1.7.0"
ruff = "^0.1.0"
black = "^23.11.0"

[tool.poetry.group.test.dependencies]
httpx = "^0.25.0"
testcontainers = {extras = ["postgresql"], version = "^3.7.0"}

[build-system]
requires = ["poetry-core"]
build-backend = "poetry.core.masonry.api"

# Semver constraints explained:
# ^1.2.3  = >=1.2.3, <2.0.0   (caret — compatible release, major locked)
# ~1.2.3  = >=1.2.3, <1.3.0   (tilde — minor locked)
# >=1.2,<2.0 = explicit range
# *       = any version (avoid!)
```

### `poetry.lock` — The Lock File

```bash
# poetry.lock pins EVERY dependency (including transitive) to an exact version

# Example entry in poetry.lock:
# [[package]]
# name = "requests"
# version = "2.31.0"
# description = "Python HTTP for Humans."
# ...
# [package.dependencies]
# charset-normalizer = ">=2,<4"
# idna = ">=2.5,<4"
# urllib3 = ">=1.21.1,<3"

# Key behaviors:
# - poetry install with lock file = exact reproducible install
# - poetry install without lock file = resolves fresh and creates lock file
# - poetry update = re-resolves and updates lock file within constraints
# - ALWAYS commit poetry.lock to git for applications (reproducible CI builds)
# - For libraries, commit poetry.lock but allow others to resolve their own version

# Java equivalent: Maven's pom.xml with <dependency> versions pinned (but Maven
# does not have a separate lock file — pins are in pom.xml directly.
# Closer equivalent: Gradle's gradle.lockfile)
```

---

## 13. Java Developer Bridge — Complete Comparison

| Concept | Java | Python |
|---|---|---|
| Project manifest | `pom.xml` / `build.gradle` | `pyproject.toml` / `poetry pyproject.toml` |
| Dependency declaration | `<dependency>` in `pom.xml` | `[project.dependencies]` or `[tool.poetry.dependencies]` |
| Dependency resolution | Maven/Gradle resolver | `pip` resolver / Poetry resolver |
| Lock file | No native lock (Gradle lockfile exists) | `poetry.lock` / `pip-tools requirements.txt` |
| Dependency repository | Maven Central / Nexus | PyPI (`pypi.org`) / private PyPI (Artifactory) |
| Install command | `mvn install` | `pip install -r requirements.txt` or `poetry install` |
| Add dependency | Edit `pom.xml` + `mvn install` | `poetry add requests` or `pip install + update requirements.txt` |
| Dev dependency scope | `<scope>test</scope>` | `poetry add --group dev` or `requirements-dev.txt` |
| Project-local install | Maven local repo (`~/.m2`) | `pip install -e .` (editable install) |
| Package binary | JAR / WAR / EAR | wheel (`.whl`) / sdist (`.tar.gz`) |
| Upload to registry | `mvn deploy` to Nexus | `poetry publish` to PyPI |
| Search path | Classpath (`-cp`) | `sys.path` / `PYTHONPATH` |
| Package unit | Package = directory (auto) | Package = directory + `__init__.py` (or namespace) |
| Version manager | SDKMAN, jenv | pyenv |
| Isolation | Build tool scopes deps | `venv` — must activate per project |
| `import` execution | Class loading is lazy | Module-level code runs on first import |
| Module cache | JVM class cache per ClassLoader | `sys.modules` dict |
| Private package | Package access modifier | Leading underscore convention; `__all__` |
| Multi-module build | Maven modules, Gradle subprojects | Monorepo with namespace packages or separate packages |

---

## 14. Project Layout Best Practices

### Recommended Layout (src layout — preferred by Poetry and pypa)

```
my-service/
├── pyproject.toml          ← project manifest (replaces pom.xml)
├── poetry.lock             ← lock file (commit this!)
├── README.md
├── .python-version         ← pyenv version file
├── .env                    ← environment variables (NEVER commit — add to .gitignore)
├── .env.example            ← template for .env (commit this)
├── .gitignore
│
├── src/
│   └── myservice/          ← source package (src layout prevents accidental imports)
│       ├── __init__.py
│       ├── main.py         ← application entry point
│       ├── config.py       ← settings (Pydantic BaseSettings)
│       ├── models/
│       │   ├── __init__.py
│       │   └── user.py
│       ├── services/
│       │   ├── __init__.py
│       │   └── user_service.py
│       ├── api/
│       │   ├── __init__.py
│       │   └── routes.py
│       └── db/
│           ├── __init__.py
│           └── session.py
│
└── tests/
    ├── conftest.py         ← pytest fixtures
    ├── unit/
    │   └── test_user.py
    └── integration/
        └── test_api.py
```

### Why src Layout?

```
Without src/ layout:          import myservice picks up ./myservice directory (your source)
                              even WITHOUT pip install — can hide missing __init__.py issues

With src/ layout:             import myservice only works after pip install -e .
                              or when src/ is on sys.path
                              Forces you to test the installed package, not the raw source
```

---

## 15. Common Module Traps and Fixes

### Trap 1 — Shadowing Standard Library

```python
# NEVER name your files the same as standard library modules
# os.py, json.py, collections.py, io.py, email.py → will shadow stdlib!

# Bad project layout:
myproject/
├── json.py       # Shadows stdlib json module!
└── main.py

# main.py
import json       # Now imports YOUR json.py, not the standard library!
json.loads(...)   # AttributeError if your json.py doesn't have loads()

# Fix: rename the file, use package structure, or use absolute imports with sys.path cleanup
```

### Trap 2 — Running a Module Directly That Uses Relative Imports

```python
# myapp/services/user_service.py
from ..models import User   # Relative import

# Running directly:
python myapp/services/user_service.py
# ImportError: attempted relative import with no known parent package

# Fix: run as a module with -m
python -m myapp.services.user_service   # Works! Package context established
```

### Trap 3 — Mutating a Module-Level Mutable Default

```python
# config.py
ALLOWED_HOSTS: list[str] = ["localhost"]   # Mutable module-level variable

# module_a.py
from config import ALLOWED_HOSTS
ALLOWED_HOSTS.append("10.0.0.1")   # Mutates the list IN config module's namespace!

# module_b.py
from config import ALLOWED_HOSTS
print(ALLOWED_HOSTS)   # ["localhost", "10.0.0.1"] — shared mutable state!

# Fix: use tuples for immutable config, or access via module reference
import config
config.ALLOWED_HOSTS.append(...)   # At least explicit
```

### Trap 4 — Import Side Effects in `__init__.py`

```python
# mypackage/__init__.py
from mypackage.heavy_module import HeavyClass   # Runs heavy computation at import time!

# Any import of mypackage pays the cost:
import mypackage   # Slow!

# Fix: use lazy imports or don't import in __init__.py unless necessary
# mypackage/__init__.py
def get_heavy_class():
    from mypackage.heavy_module import HeavyClass
    return HeavyClass
```

### Trap 5 — `pip install` into Wrong Environment

```python
# VERY common bug: installing into global Python instead of venv
pip install requests   # Did you activate the venv first?

# Check: which python / which pip
which pip     # Should show: /path/to/project/.venv/bin/pip

# Safest: use python -m pip instead of bare pip
python -m pip install requests   # Always uses the python currently on PATH

# VS Code pitfall: each terminal may have a different activated venv
# Always confirm with: pip --version   (shows the Python it's tied to)
```

---

## 16. Hot Interview Q&A

**Q: What happens when Python imports the same module twice?**  
A: The second import does nothing — Python checks `sys.modules` first. If the module is already there, the cached module object is returned immediately without re-executing the file. This is why module-level state is shared across all importers — they all get the same module object.

**Q: What is the difference between `import os` and `from os import getcwd`?**  
A: `import os` imports the module object into the current namespace — you access its members via `os.getcwd()`. `from os import getcwd` copies the reference to `getcwd` into the current namespace. The key trap: if `getcwd` is later rebound in the `os` module, `from os import getcwd` still points to the old value, but `os.getcwd` would see the new one. For mutable values (functions rarely change), this is rarely an issue.

**Q: What is a virtual environment and why is it required in Python?**  
A: A virtual environment is an isolated Python installation with its own `site-packages` directory. Without one, `pip install` installs packages globally, causing version conflicts between projects. Virtual environments ensure each project has its own exact set of package versions. In Java, Maven/Gradle handle isolation per-build automatically — Python has no such built-in mechanism, so venvs are mandatory.

**Q: What is the difference between `requirements.txt` and `poetry.lock`?**  
A: `requirements.txt` can list direct dependencies with loose version ranges or pinned versions — it's a simple text file with no metadata. `poetry.lock` is a comprehensive, machine-generated lock file that pins every package (direct AND transitive) to an exact version, along with hashes for integrity verification. `poetry install` with a lock file is fully deterministic and reproducible; `pip install -r requirements.txt` with loose versions is not.

**Q: What is `pip install -e .` (editable install)?**  
A: An editable install installs your package as a link pointing to the source directory, rather than copying files to `site-packages`. This means changes to your source code are immediately reflected without reinstalling. It requires a `pyproject.toml` (or `setup.py`). Used for active development. The Java equivalent would be setting your classpath to point to the compiled `target/classes` directory instead of a JAR.

**Q: What is `__all__` and when should you define it?**  
A: `__all__` is a list of strings defining the public API of a module — only names in `__all__` are exported by `from module import *`. You should define `__all__` whenever you want to explicitly declare the public API of a module, especially in library code. It also serves as documentation. In Java, all `public` symbols are the public API; Python has no access modifiers, so `__all__` and leading underscore conventions serve a similar purpose.

**Q: How do you avoid circular imports?**  
A: Three main strategies: (1) Restructure — extract shared types into a base module with no imports from the modules that form the cycle. (2) Deferred import — move the import inside the function that needs it, so it only runs when the function is called, not at module load time. (3) `TYPE_CHECKING` guard — use `if TYPE_CHECKING:` for imports that are only needed for type annotations, which avoids the runtime import entirely.

**Q: What is the difference between a package and a namespace package?**  
A: A regular package requires an `__init__.py` file in its directory. A namespace package (Python 3.3+) has no `__init__.py` — Python treats the directory as a package automatically. Namespace packages can span multiple directories on `sys.path`, allowing different repositories to contribute to the same package namespace. Regular packages are the norm for most projects; namespace packages are used in large monorepos or plugin architectures.

---

## 17. Final Revision Checklist

### Module System

- [ ] I know a Python module is any `.py` file; a package is a directory with `__init__.py`
- [ ] I know module-level code runs ONCE on first import, then `sys.modules` returns the cached object
- [ ] I know `if __name__ == "__main__":` guards code that should only run as a script
- [ ] I know `sys.path` is searched left to right; `sys.path[0]` is the script's directory

### Import Mechanics

- [ ] I know `import x` vs `from x import y` — `from` takes a snapshot of the reference
- [ ] I know absolute imports are recommended (PEP 8); relative imports (`.`) are valid inside packages
- [ ] I know relative imports fail when a file is run directly (need `python -m` syntax)
- [ ] I know how to detect and fix circular imports (restructure, defer, or TYPE_CHECKING)

### Package Structure

- [ ] I know `__init__.py` marks a regular package and runs on package import
- [ ] I know `__all__` controls what `from module import *` exports
- [ ] I understand src layout vs flat layout and why src layout is preferred for libraries

### Virtual Environments and pip

- [ ] I know why venvs are mandatory — Python pip installs globally by default
- [ ] I can create (`python -m venv .venv`), activate (`source .venv/bin/activate`), and deactivate a venv
- [ ] I know `pip install -r requirements.txt`, `pip freeze`, `pip list`, `pip show`
- [ ] I know `pip install -e .` for editable development installs
- [ ] I know `python -m pip` is safer than bare `pip` to avoid wrong-environment installs

### pyproject.toml and Poetry

- [ ] I know `pyproject.toml` replaces `setup.py`, `setup.cfg`, and consolidates tool configs
- [ ] I know Poetry `pyproject.toml` — `[tool.poetry.dependencies]` vs `[tool.poetry.group.dev.dependencies]`
- [ ] I know `poetry.lock` pins all transitive deps — always commit it for applications
- [ ] I know `poetry add`, `poetry install`, `poetry run`, `poetry shell`, `poetry update`
- [ ] I know `poetry export` converts to `requirements.txt` for Docker/systems without Poetry

### Java Developer Reminders

- [ ] Java classpath = Python `sys.path` / `PYTHONPATH`
- [ ] `pom.xml` = `pyproject.toml`; `mvn install` = `pip install -r requirements.txt` or `poetry install`
- [ ] JAR = Python wheel (`.whl`) or sdist (`.tar.gz`)
- [ ] Maven Central = PyPI; Nexus/Artifactory private registry = private PyPI index
- [ ] Maven `<scope>test</scope>` = Poetry `--group dev/test`; `requirements-dev.txt`

---

*File 3 of 5 — Group 2: Intermediate Backend*  
*Next: Python-File-IO-Serialization-JSON-Pickle-Gold-Sheet.md*
