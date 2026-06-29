---
name: Create GitHub Action
description: Generate a complete, secure GitHub Actions workflow file
---

Create a GitHub Actions workflow for:

Purpose: ${input:What should this workflow do? (e.g., CI for Python, deploy to AWS, publish npm package)}

Trigger: ${input:When should it run? (e.g., push to main, pull_request, workflow_dispatch)}

Environment: ${input:Runtime needed? (e.g., ubuntu-latest + Python 3.12 + poetry, or ubuntu-latest + Node 20)}

Generate a complete workflow file with:

Structure requirements:
1. Trigger section (on:) matching the described trigger
2. concurrency group to cancel stale PR runs
3. One or more jobs with descriptive names
4. Cache step for package manager dependencies
5. timeout-minutes on each job

Security requirements:
- Pin ALL action versions to specific tags (e.g., actions/checkout@v4 NOT @latest)
- Use ${{ secrets.NAME }} for all credentials — never hardcoded
- Limit permissions to minimum required

Quality requirements:
- Descriptive step names (no "Step 1" or "Run command")
- Fail-fast: true for matrix jobs
- Post-job: upload test results or coverage as artifact if applicable

Output:
1. Complete YAML file ready to save in .github/workflows/
2. List of GitHub Secrets that must be configured
3. Any environment variables that need to be set at the repo level

Rules:
- All actions pinned: never @latest or @main
- Always include concurrency group
- No hardcoded tokens or passwords
