# Python Install, CLI, IDE, uv, and Virtual Environments - Gold Sheet

> **Track File #0 - Group 0: Setup**
> For: true beginner to backend Python | Level: first mile to production-ready workflow

---

## 1. Why This Sheet Exists

Most Python bugs at the beginner stage are not language bugs. They are environment bugs:

- `python` points to a different interpreter than expected.
- `pip` installs into the wrong environment.
- The IDE uses one interpreter while the terminal uses another.
- The app runs locally but fails in CI because dependencies were not locked.
- A script works from one folder but fails from another because imports depend on the working directory.

This sheet gives you the first-mile workflow before the deeper Python track starts.

---

## 2. Mental Model

Think of a Python project as four separate things:

```text
Python version       -> which interpreter runs code
Virtual environment  -> isolated installed packages
Project metadata     -> pyproject.toml / requirements files
Command runner       -> python -m ..., uv run ..., pytest, ruff
```

If you mix them up, debugging becomes painful. If you separate them, Python becomes predictable.

---

## 3. Install Verification Checklist

Run these before trusting any machine:

```bash
python -VV
python -c "import sys; print(sys.executable)"
python -c "import site; print(site.getsitepackages())"
python -m pip --version
```

Expected reasoning:

- `python -VV` shows the version and implementation.
- `sys.executable` shows the exact interpreter binary.
- `site.getsitepackages()` shows where packages install.
- `python -m pip` ensures pip belongs to the same interpreter.

**Interview-safe rule:** prefer `python -m pip` over bare `pip` when teaching or debugging.

---

## 4. Python Version Strategy

### Local Development

Use one of these:

| Tool | Use Case |
|---|---|
| Official installer | Simple beginner setup |
| `pyenv` | Multiple Python versions on one machine |
| `uv python install` | Modern project workflow with Python version management |
| System Python | Avoid for project dependencies |

### Version Policy

For a production backend:

```text
Choose one supported Python minor version.
Pin it in docs, CI, Docker, and local setup.
Do not silently mix Python 3.10, 3.11, 3.12, 3.13, and 3.14 across environments.
```

Example:

```text
Project runtime: Python 3.14
Minimum supported: Python 3.12
CI matrix: 3.12, 3.13, 3.14 only if library support matters
```

---

## 5. `python`, `python3`, and `py`

Different systems expose the interpreter differently:

| Command | Common Meaning |
|---|---|
| `python` | Active Python interpreter; may be Python 2 on old systems, Python 3 on modern ones |
| `python3` | Usually Python 3 on macOS/Linux |
| `py` | Windows Python launcher |

Do not argue from command name. Verify:

```bash
python -VV
python3 -VV
py -3.14 -VV
```

---

## 6. Virtual Environments

### Why They Exist

Without a virtual environment, all projects share one package space.

That causes:

- dependency conflicts
- accidental global installs
- "works on my machine" drift
- broken system tooling

### Create With Standard Library

```bash
python -m venv .venv
```

Activate:

```bash
# macOS/Linux
source .venv/bin/activate

# Windows PowerShell
.venv\Scripts\Activate.ps1
```

Verify:

```bash
python -c "import sys; print(sys.executable)"
```

You should see a path inside `.venv`.

### Install

```bash
python -m pip install fastapi pytest ruff
```

### Deactivate

```bash
deactivate
```

---

## 7. uv Beginner Workflow

`uv` is a modern Python project and package manager. It can manage Python versions, virtual environments, dependencies, lockfiles, and command execution.

### New Project

```bash
uv init booking-api
cd booking-api
uv python pin 3.14
uv add fastapi uvicorn
uv add --dev pytest ruff mypy
uv run python -VV
uv run pytest
```

### Existing Project

```bash
uv sync
uv run python -m app.main
uv run pytest
```

### Production/CI Install

```bash
uv sync --frozen --no-dev
```

### Rule

If the project uses uv, run tools through uv:

```bash
uv run pytest
uv run ruff check .
uv run python scripts/load_data.py
```

Avoid mixing manual `.venv` activation, bare `pip install`, and uv commands in the same project unless you know exactly why.

---

## 8. pip, requirements, Poetry, and uv

| Workflow | Best For | Main Files |
|---|---|---|
| `pip` + `requirements.txt` | Simple scripts, legacy projects | `requirements.txt` |
| `pip-tools` | Deterministic requirements workflow | `requirements.in`, `requirements.txt` |
| Poetry | Project metadata + lockfile, common in existing teams | `pyproject.toml`, `poetry.lock` |
| uv | Fast modern default for new projects | `pyproject.toml`, `uv.lock` |

Interview answer:

> For a new backend service, I prefer a lockfile-based workflow. uv is a strong modern default because it handles Python versions, virtual environments, dependency resolution, lockfiles, and command execution in one tool. In an existing company codebase, I follow the established workflow unless there is a clear migration reason.

---

## 9. `pyproject.toml`

Modern Python project metadata usually lives in `pyproject.toml`.

Example:

```toml
[project]
name = "booking-api"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "fastapi>=0.115",
    "uvicorn[standard]>=0.35",
]

[dependency-groups]
dev = [
    "pytest>=8",
    "ruff>=0.12",
    "mypy>=1.16",
]

[tool.ruff]
line-length = 100

[tool.mypy]
python_version = "3.14"
strict = true
```

Key idea:

```text
pyproject.toml describes intent.
lockfile captures exact resolved versions.
CI should install from the lockfile.
```

---

## 10. IDE Interpreter Setup

### PyCharm

Check:

```text
Settings -> Project -> Python Interpreter
```

Use:

```text
<project>/.venv/bin/python
```

or the uv-created environment used by the project.

### VS Code

Use:

```text
Python: Select Interpreter
```

Pick the `.venv` inside the project.

### Red Flags

- IDE can import a package but terminal cannot.
- Terminal can run tests but IDE cannot.
- `pytest` uses a different Python than the app.
- Debugger shows a different `sys.executable`.

Fix by making terminal, IDE, and CI use the same interpreter path and dependency workflow.

---

## 11. Running Code

### Script

```bash
python hello.py
```

### Module

```bash
python -m package.module
```

Prefer module execution for package-aware code.

Example:

```text
app/
    __init__.py
    main.py
```

Run:

```bash
python -m app.main
```

Why:

- Python resolves imports from the package root more predictably.
- You avoid some relative import traps.

---

## 12. Import and Working Directory Traps

Bad pattern:

```bash
cd app
python main.py
```

This can make imports work accidentally from the wrong root.

Better:

```bash
python -m app.main
```

or:

```bash
uv run python -m app.main
```

Production mindset:

```text
Imports should not depend on a developer manually cd-ing into a lucky directory.
The project root, package names, and runner command should be explicit.
```

---

## 13. First Python Program

`hello.py`:

```python
def greet(name: str) -> str:
    return f"Hello, {name}"


def main() -> None:
    print(greet("Python"))


if __name__ == "__main__":
    main()
```

Run:

```bash
python hello.py
```

Why this structure matters:

- Functions make logic testable.
- `main()` gives a clear entry point.
- `if __name__ == "__main__"` prevents code from running on import.

---

## 14. First Test

`test_hello.py`:

```python
from hello import greet


def test_greet() -> None:
    assert greet("Python") == "Hello, Python"
```

Run:

```bash
python -m pytest
```

or:

```bash
uv run pytest
```

---

## 15. Beginner Debugging Commands

```bash
python -VV
python -c "import sys; print(sys.executable)"
python -c "import sys; print('\\n'.join(sys.path))"
python -m pip list
python -m pip show fastapi
python -m site
python -m pytest -q
```

Use these before guessing.

---

## 16. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Running bare `pip install` | May install into the wrong interpreter | Use `python -m pip` or `uv add` |
| Using global Python for projects | Dependency conflicts | Use `.venv` or uv |
| Not committing lockfile for apps | CI can install different versions | Commit `uv.lock`, `poetry.lock`, or pinned requirements |
| IDE interpreter differs from terminal | Debugging lies to you | Point IDE at project env |
| Running package files directly | Import behavior changes | Use `python -m package.module` |
| Ignoring Python minor version | Syntax/library mismatch | Pin and document runtime |

---

## 17. Practical Question

> A teammate says, "The FastAPI app works in PyCharm, but `uvicorn` fails in the terminal with `ModuleNotFoundError`. What do you check first?"

Strong answer:

> I first check whether PyCharm and terminal are using the same interpreter by printing `sys.executable`. Then I check whether the package is installed in that environment with `python -m pip show` or `uv run python -c`. Next I verify the command is run from the project root and uses the correct app path, such as `uvicorn app.main:app`. If the project uses uv or Poetry, I run through that tool instead of relying on global commands.

---

## 18. Revision Notes

- One-line summary: Python mastery starts with controlling interpreter, environment, dependencies, and command execution.
- Three keywords: interpreter, venv, lockfile.
- One interview trap: saying "pip installed it" without proving which Python pip belonged to.
- One memory trick: always ask, "Which Python is running this?"

