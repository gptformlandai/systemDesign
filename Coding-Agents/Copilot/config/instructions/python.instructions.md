---
applyTo: "**/*.py"
---
# Python Conventions

## Language Version
Python 3.11+. Use match/case for type dispatching.
Use `X | Y` union syntax instead of `Union[X, Y]`.
Use `list[X]` not `List[X]`. Use `dict[str, X]` not `Dict[str, X]`.
Use `X | None` not `Optional[X]`.

## Async
All I/O-bound operations must use async/await.
Never use time.sleep() in async functions — use asyncio.sleep().
Never use the requests library in async code — use httpx.AsyncClient.
Never use blocking file I/O in async context — use aiofiles or run_in_executor.

## Type Hints
Required on all public function signatures.
Return types required on all functions (including `-> None`).
Avoid `Any` unless bridging legacy code — add a comment explaining why.

## Error Handling
Never use bare `except:`. Always catch specific exception types.
Always log exceptions with `logger.exception()` to include the full stack trace.
Custom exceptions must inherit from a base AppError or AppException class.
Never swallow exceptions silently.

## Formatting
Strings: use f-strings. Never use % formatting or .format().
Line length: 100 characters max (configured in pyproject.toml).
Import order: stdlib, third-party, local (separated by blank lines).

## Do NOT
- Do not use print() for logging — use the logging module or structlog
- Do not use os.system() — use subprocess.run() with shell=False
- Do not use mutable default arguments (def fn(lst=[])) — use None and initialize in body
- Do not use global variables for application state
- Do not use deprecated standard library modules
