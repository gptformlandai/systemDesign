# Python Packaging, Virtual Environments, Poetry, uv, Wheels - Gold Sheet

> Goal: understand how Python backend code becomes a reproducible, installable, testable, deployable service.

---

## 1. Intuition

Python packaging is not only "install some libraries." It is three connected problems:

```text
Code
  -> choose Python version
  -> create isolated environment
  -> resolve dependencies
  -> lock exact versions
  -> build artifact
  -> run with predictable imports
```

The biggest beginner mistake is thinking Python dependency management is only `pip install`. At production level, the real question is:

> Can a fresh CI machine, Docker image, or teammate laptop reproduce the same environment every time?

---

## 2. Definition

- Definition: Python packaging is the process of describing, resolving, installing, building, and distributing Python projects and their dependencies.
- Category: Build system, dependency management, artifact generation.
- Core idea: keep runtime dependencies isolated, reproducible, versioned, and easy to deploy.

---

## 3. Why It Exists

Without packaging discipline:

- the app works on one laptop but fails in CI
- one dependency upgrade silently breaks production
- imports depend on global machine state
- Docker builds become slow and non-deterministic
- security scanning cannot reliably identify dependency versions
- native packages fail because OS libraries are missing

Python gives flexibility. Build tools give repeatability.

---

## 4. Core Pipeline

```text
Source code
  -> pyproject.toml / requirements.txt
  -> dependency resolver
  -> lock exact versions
  -> virtual environment
  -> run tests and quality checks
  -> build wheel or source distribution
  -> container image or package registry
  -> runtime execution
```

Backend version:

```text
Build -> Package -> Runtime -> Serve -> Scale
```

Example FastAPI service:

```text
app/
  main.py
pyproject.toml
uv.lock
tests/
Dockerfile
```

---

## 5. Virtual Environments

### What

A virtual environment is an isolated directory containing:

- a Python executable reference
- installed packages
- scripts such as `uvicorn`, `pytest`, `black`
- metadata for dependency resolution

Typical directory:

```text
.venv/
  bin/python
  bin/pip
  lib/python3.x/site-packages/
```

### Why

It prevents project dependencies from leaking into each other.

Without a virtual environment:

```text
Project A needs pydantic 1.x
Project B needs pydantic 2.x
Global Python has one site-packages directory
Result: one project breaks
```

### Basic commands

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install -U pip
python -m pip install fastapi uvicorn
```

### Developer mistakes

- Installing dependencies globally.
- Committing `.venv` to git.
- Using one virtual environment for multiple projects.
- Running `pip` without checking which Python it belongs to.
- Assuming local Python version equals production Python version.

Interview insight:

> A virtual environment isolates installed packages. A lockfile makes the selected versions reproducible. You usually need both.

---

## 6. requirements.txt vs pyproject.toml vs Lockfile

### requirements.txt

Good for simple deployments and legacy projects.

```txt
fastapi==0.116.0
uvicorn[standard]==0.35.0
sqlalchemy==2.0.41
```

Benefits:

- easy to read
- supported everywhere
- works well with `pip install -r requirements.txt`

Limits:

- no standard project metadata
- weak separation of runtime/dev/test dependency groups
- not enough by itself for modern packaging

### pyproject.toml

Modern Python project configuration lives here.

```toml
[project]
name = "booking-api"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
  "fastapi>=0.116.0",
  "uvicorn[standard]>=0.35.0",
]

[dependency-groups]
dev = [
  "pytest>=8.0.0",
  "ruff>=0.12.0",
]

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"
```

Why it matters:

- standard metadata for tools
- dependency groups
- build backend configuration
- tool configuration under `[tool.*]`

### Lockfile

A lockfile records exact resolved versions.

```text
fastapi 0.116.1
starlette 0.47.2
pydantic 2.11.7
typing-extensions 4.14.1
```

Purpose:

- reproducible CI
- reproducible Docker builds
- safer upgrades
- clearer dependency diff in pull requests

Decision rule:

| Use case | Recommended |
|---|---|
| Tiny script | `requirements.txt` can be enough |
| Service or product app | `pyproject.toml` plus lockfile |
| Shared library | `pyproject.toml`; avoid over-pinning transitive versions for consumers |
| CI deployment | frozen/synced lockfile install |

---

## 7. pip

### What

`pip` installs Python packages from package indexes such as PyPI or internal repositories.

### Internal flow

```text
pip install fastapi
  -> read requirement
  -> query package index
  -> resolve compatible versions
  -> download wheel or source distribution
  -> build if needed
  -> install into environment
  -> write package metadata
```

### Common commands

```bash
python -m pip install fastapi
python -m pip install -r requirements.txt
python -m pip freeze > requirements.txt
python -m pip check
```

### Real-world use

- simple services
- Docker images
- legacy systems
- CI workflows where lockfiles are generated by another tool

### Mistakes

- Using `pip freeze` as architecture design. It captures everything installed, including accidental packages.
- Installing without a virtual environment.
- Using unpinned versions in production.
- Ignoring native build dependencies for packages like `psycopg`, `cryptography`, `numpy`.

Interview insight:

> pip is an installer and resolver. It is not a complete project manager by itself.

---

## 8. setuptools

### What

`setuptools` is a long-standing Python build backend used to package Python projects.

Legacy style:

```python
# setup.py
from setuptools import setup, find_packages

setup(
    name="booking-api",
    version="0.1.0",
    packages=find_packages(),
    install_requires=["fastapi"],
)
```

Modern style in `pyproject.toml`:

```toml
[build-system]
requires = ["setuptools>=69", "wheel"]
build-backend = "setuptools.build_meta"
```

When to know it:

- many existing enterprise projects use it
- many open-source packages still depend on it
- useful when debugging package builds

Mistake:

- Treating `setup.py install` as a modern install strategy. Prefer standard build frontends and isolated builds.

---

## 9. Poetry

### What

Poetry is a Python dependency and packaging tool that manages:

- dependency declaration
- lockfile
- virtual environments
- packaging metadata
- publishing

Example:

```bash
poetry init
poetry add fastapi uvicorn
poetry add --group dev pytest ruff
poetry install
poetry run pytest
```

Strengths:

- simple mental model
- lockfile built in
- clean dependency groups
- good for application projects

Trade-offs:

- slower than newer tools in some large environments
- its lockfile is Poetry-specific
- teams need to standardize command usage

Interview insight:

> Poetry moved Python teams away from ad hoc `requirements.txt` workflows toward project-level dependency management.

---

## 10. uv

### What

`uv` is a fast Python package and project manager written in Rust. It can replace many common tools:

- `pip`
- `virtualenv`
- `pip-tools`
- `pipx`
- parts of `poetry`
- Python version management workflows

### Why teams like it

```text
Traditional:
  pyenv + virtualenv + pip + pip-tools + build + twine

uv:
  uv python install
  uv init
  uv add
  uv run
  uv lock
  uv sync
  uv build
```

### Typical workflow

```bash
uv init booking-api
cd booking-api
uv add fastapi uvicorn
uv add --dev pytest ruff
uv run pytest
uv lock
uv sync --frozen
uv build
```

### What happens internally

```text
uv add fastapi
  -> updates pyproject.toml
  -> resolves dependency graph
  -> writes uv.lock
  -> creates or updates .venv
  -> uses global cache for packages
```

### uv run

```bash
uv run uvicorn app.main:app --reload
```

Flow:

```text
read pyproject.toml
  -> check uv.lock
  -> create/sync .venv if needed
  -> run command inside project environment
```

### uv sync

```bash
uv sync --frozen
```

This is a CI-friendly command:

- do not change the lockfile
- install exactly what the lockfile says
- fail if the lockfile is stale

### Mistakes

- Manually running `pip install` inside a uv-managed `.venv`.
- Forgetting to commit `uv.lock` for applications.
- Treating `uv run` as only a command runner. It also ensures the environment is synchronized.
- Not pinning `requires-python`.

Interview insight:

> uv improves the pipeline by making resolution, environment creation, caching, and command execution fast and consistent.

---

## 11. Wheel vs Source Distribution

### Wheel

A wheel is a built package.

```text
booking_api-0.1.0-py3-none-any.whl
```

Benefits:

- faster install
- no build step during deployment
- better for CI artifact promotion
- avoids needing compilers in runtime containers

### Source distribution

An sdist is source packaged for building later.

```text
booking_api-0.1.0.tar.gz
```

Benefits:

- useful for source publication
- consumers can build for their environment

Trade-off:

| Artifact | Best for | Risk |
|---|---|---|
| Wheel | Deployment speed and reproducibility | Platform compatibility must match |
| sdist | Source distribution | Requires build tooling at install time |

Backend rule:

> For production services, prefer building once and deploying the built artifact or image. Do not compile native dependencies on every container startup.

---

## 12. Build and Test Pipeline

```text
Developer push
  -> CI checkout
  -> install Python version
  -> restore package cache
  -> sync locked dependencies
  -> lint
  -> type check
  -> test
  -> coverage report
  -> build wheel
  -> build container image
  -> scan
  -> deploy
```

Example with uv:

```bash
uv sync --frozen
uv run ruff check .
uv run pytest --cov=app --cov-report=xml
uv build
```

Example with pip:

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
pytest --cov=app --cov-report=xml
python -m build
```

---

## 13. Docker Pattern

```dockerfile
FROM python:3.12-slim AS builder
WORKDIR /app

COPY pyproject.toml uv.lock ./
RUN pip install uv && uv sync --frozen --no-dev

COPY app ./app
RUN uv build

FROM python:3.12-slim
WORKDIR /app

COPY --from=builder /app/.venv /app/.venv
COPY app ./app

ENV PATH="/app/.venv/bin:$PATH"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Production improvements:

- use a non-root user
- avoid dev dependencies
- pin base image by digest in high-security systems
- keep dependency layer separate from source layer for Docker cache reuse
- do not run `--reload` in production

---

## 14. Real-World Use Cases

### FastAPI microservice

- `pyproject.toml` declares FastAPI, Uvicorn, SQLAlchemy.
- `uv.lock` is committed.
- CI runs `uv sync --frozen`.
- Docker image runs Uvicorn or Gunicorn plus Uvicorn worker.

### Data processing job

- dependency lock prevents pandas/numpy drift
- wheel artifact can be reused across jobs
- runtime pins Python version to match native dependencies

### Internal shared library

- `pyproject.toml` defines package metadata
- wheel is published to internal package registry
- apps depend on versioned releases

---

## 15. Production Checklist

- Pin Python version.
- Use isolated environment.
- Commit lockfile for applications.
- Separate runtime and dev dependencies.
- Build artifacts in CI.
- Keep Docker dependency layers cacheable.
- Scan dependencies.
- Generate coverage reports.
- Avoid global installs.
- Use health checks and structured logs at runtime.

---

## 16. Interview Questions

### Question

> How would you design the build and dependency pipeline for a Python FastAPI service?

Strong answer:

1. I would declare project metadata and dependencies in `pyproject.toml`.
2. I would commit a lockfile for reproducible app builds.
3. CI would use a frozen install such as `uv sync --frozen`.
4. The pipeline would lint, test, generate coverage, build an artifact or image, scan it, and promote the same artifact across environments.
5. Runtime would not install dependencies dynamically.
6. I would monitor startup logs for dependency, import, configuration, and DB connectivity failures.

### Question

> What is the difference between a wheel and a source distribution?

Strong answer:

> A wheel is already built and installs quickly. An sdist contains source and may require build tools at install time. For production service deployments, I prefer building once in CI and deploying a wheel or container image so startup is deterministic.

### Question

> Why use uv instead of pip alone?

Strong answer:

> pip is an installer. uv gives a faster project workflow around dependency resolution, lockfiles, virtual environments, command execution, Python versions, and caching. For teams, that reduces CI time and environment drift.

---

## 17. Common Failure Modes

| Symptom | Likely cause | Fix |
|---|---|---|
| Works locally, fails in Docker | missing system package or Python mismatch | pin Python, install OS libs in image |
| ImportError in production | dependency not installed or wrong module path | check lockfile, package layout, entrypoint |
| CI changes lockfile unexpectedly | non-frozen install | use frozen/sync mode |
| Slow Docker build | dependency layer invalidated by source changes | copy lock files before source |
| Native package build fails | no compiler or headers | use wheel-compatible image or install build deps |
| Different behavior across machines | no lockfile or global packages | use venv plus lockfile |

---

## 18. Revision Notes

- One-line summary: Python backend builds are about reproducible environments, locked dependencies, and deployable artifacts.
- Three keywords: `pyproject.toml`, lockfile, wheel.
- One interview trap: saying `pip install` is a complete production build strategy.
- One memory trick: Python production means "same Python, same deps, same artifact, same startup."

