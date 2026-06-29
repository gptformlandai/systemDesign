---
applyTo: ".github/workflows/**"
---
# GitHub Actions Rules

## Action Version Pinning
Always pin action versions to a specific tag (major+minor minimum).
Use: actions/checkout@v4 — NOT @main, @latest, @master, or @v3
Use: actions/setup-python@v5 — NOT @v4 or @latest
Reason: @latest silently breaks when the action updates.

## Secrets and Variables
All sensitive values must use: ${{ secrets.SECRET_NAME }}
Non-sensitive configuration: ${{ vars.VAR_NAME }}
Never hardcode tokens, API keys, passwords, or connection strings in workflow files.

## Concurrency
Always add a concurrency group to cancel stale runs on PR:
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

## Timeouts
Set timeout-minutes on each job to prevent stuck jobs consuming CI minutes.
Default recommendation: 15 minutes for test jobs, 30 minutes for build/deploy jobs.

## Step Names
Every step must have a descriptive name.
BAD: "Run step", "Execute", "Step 1"
GOOD: "Install Python dependencies", "Run ruff linting", "Execute pytest suite"

## Caching
Always cache package manager dependencies for faster runs.
Python: cache pip using hashFiles('**/requirements*.txt') or poetry.lock
Node: cache npm/yarn using hashFiles('**/package-lock.json')
Java: cache Maven/Gradle using appropriate hash

## Security
Never use pull_request_target with code checkout from a fork without explicit review gate.
Limit GITHUB_TOKEN permissions to minimum: contents: read, or specify per-job.
Do not echo secrets to logs (Actions automatically masks them, but do not explicitly echo).

## Do NOT
- Do not use runs-on: self-hosted without security review
- Do not skip required status checks with workarounds
- Do not commit workflow changes that disable security scans
- Do not use curl | bash patterns for installing tools — use official actions or pinned scripts
