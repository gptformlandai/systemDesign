---
name: Bootstrap Project
description: Scaffold a new project with best-practice structure using Agent Mode
---

Bootstrap a new project with the following requirements.

Project type: ${input:What kind of project? (e.g., Python FastAPI REST API, React TypeScript SPA, Node.js Express service, CLI tool)}

Project name: ${input:Project name (used for directory names and package names)}

Core requirements:
${input:List 3-5 core requirements or features for this project}

Additional context:
${input:Any constraints? (e.g., must use PostgreSQL, deploy to AWS, team uses Poetry for Python)}

Create:

1. **Project structure** (directory tree with explanation of each folder)

2. **Core configuration files**:
   - Package manager config (pyproject.toml / package.json / pom.xml)
   - Linting config (ruff.toml / .eslintrc / checkstyle)
   - Git: .gitignore appropriate for this stack
   - VS Code: .vscode/settings.json and extensions.json

3. **GitHub setup**:
   - .github/copilot-instructions.md (project-specific rules)
   - .github/workflows/ci.yml (lint + test CI)
   - AGENTS.md (behavioral rules for this project)

4. **Application skeleton**:
   - Entry point file (main.py / index.ts / App.tsx / etc.)
   - One example module following the intended pattern
   - One example test file

5. **README.md** with: setup, run, test commands

Rules:
- Plan the structure first — wait for approval before creating files
- All commands in README must be copy-paste runnable
- Pin all tool versions (no "latest")
- Every file must be production-starting-point quality (not hello world)
- Include .env.example with all required environment variables (no real values)
