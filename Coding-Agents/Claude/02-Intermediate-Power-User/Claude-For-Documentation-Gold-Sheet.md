# Claude For Documentation — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 6 of 7 (Track File #12)
> **Read after**: Claude-For-Testing-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|-------|--------|--------------------------|
| Docstring generation at codebase scale | ★★★★★ | Writing docstrings is tedious — Claude writes better ones faster than most developers |
| README generation from code | ★★★★★ | Projects stay undocumented because writing READMEs is painful |
| Architecture Decision Records (ADRs) | ★★★★☆ | ADRs are one of the highest-value documents a codebase can have — Claude drafts them in 2 min |
| Onboarding documentation | ★★★★☆ | New team members waste days figuring out setup — Claude generates accurate onboarding docs |
| API documentation from code | ★★★★★ | REST API docs are required but nobody wants to write them |
| Release notes generation | ★★★★☆ | Release notes from git log + Claude = professional changelogs without manual effort |

---

## 1. Docstring Generation

### Function-Level Docstrings

```
"Generate Google-style Python docstrings for all public functions in:
@file:src/services/user_service.py

For each function:
  - Summary: what the function does (1 sentence)
  - Args: each parameter with type and description
  - Returns: return type and what it represents
  - Raises: exceptions that can be raised and why
  - Example: one realistic usage example

Do NOT change the function signatures or implementation.
Output: the complete file with docstrings added."
```

### Class-Level Documentation

```
"Generate a class docstring and attribute documentation for:
@file:src/models/order.py

Class docstring format:
  - What this class represents
  - Key responsibilities (2-3 bullets)
  - Usage example (how to instantiate and use it)

Attribute documentation:
  - Each attribute: name, type, what it stores, valid values if constrained"
```

### TypeScript / Java Variants

```
TypeScript (JSDoc):
"Generate JSDoc comments for all exported functions in:
@file:src/utils/validation.ts
Include: @param, @returns, @throws, @example"

Java (Javadoc):
"Generate Javadoc for all public methods in:
@file:src/main/java/com/example/service/UserService.java
Include: @param, @return, @throws, @since"
```

---

## 2. README Generation

### Full README From Codebase

```
"Generate a professional README.md for this project.
Using @codebase (or reference the key files below), write a README that includes:

## [Project Name]
  Short description (2 sentences)

## What It Does
  3-4 bullet points of key capabilities

## Tech Stack
  List frameworks, databases, key dependencies with versions

## Prerequisites
  What needs to be installed before setup

## Getting Started
  Step-by-step setup commands (use actual commands from the project)

## Configuration
  Environment variables — list all required ones with descriptions

## Running Tests
  Actual test command

## Project Structure
  Directory tree with what each folder contains (2-3 words per folder)

## API Reference (if applicable)
  Key endpoints with method, path, description

Use the actual file structure and code — do not fabricate anything."
```

### README Section Update

```
"Update the Getting Started section in @file:README.md.
Current section is outdated — the actual setup steps are:
[describe what the real setup steps are now]

Rewrite only the Getting Started section.
Keep all other sections unchanged."
```

---

## 3. Architecture Decision Records (ADRs)

### What an ADR Is

```
ADR = Architecture Decision Record
A short document that records:
  1. The context (what situation forced a decision)
  2. The decision (what was chosen)
  3. The alternatives considered
  4. The consequences (trade-offs, future constraints)

WHY: In 6 months, no one remembers why a technical choice was made.
ADRs prevent "why did we do this?" from being answered with "nobody knows."
```

### ADR Generation Prompt

```
"Generate an Architecture Decision Record for this decision:

Decision: [e.g., Use Redis for session storage instead of database sessions]

Context I can provide:
  - The problem we were solving: [describe]
  - The constraints we had: [performance/scalability/cost/etc.]
  - What we chose and why: [describe]
  - Alternatives we considered: [list them]

Format:
  # ADR-[number]: [title]
  **Date**: [today]
  **Status**: Accepted

  ## Context
  ## Decision
  ## Alternatives Considered
  ## Consequences
  ## References (if any)

Keep it under 400 words. Technical, not marketing."
```

---

## 4. API Documentation

### REST API Docs from Route Code

```
"Generate API documentation for the routes in:
@file:src/api/users.py

For each endpoint, generate:
  ## [METHOD] [path]
  **Description**: what this endpoint does
  **Authentication**: required auth type (bearer token, API key, public)
  **Request Body**: (if POST/PUT/PATCH) — JSON schema with field descriptions
  **Query Parameters**: (if GET) — parameter, type, required/optional, description
  **Response 200**: response schema with field descriptions
  **Response 4xx/5xx**: all error responses with codes and when they occur
  **Example Request**: curl command
  **Example Response**: sample JSON

Do not fabricate field names — use only what's in the actual code."
```

### OpenAPI / Swagger Spec

```
"Generate an OpenAPI 3.0 spec for the routes in:
@file:src/api/[router].py

Follow the existing schema models from @file:src/schemas/[schemas].py.
Output: valid YAML OpenAPI spec I can save as openapi.yaml."
```

---

## 5. Onboarding Documentation

### New Developer Onboarding Guide

```
"Generate a new developer onboarding guide for this project.

Using @codebase, cover:

## Day 1 — Setup
  Prerequisites and installation steps (use actual commands)
  How to verify setup is working
  Environment variables needed (list them with explanations)

## Understanding the Codebase
  What this service does in plain English
  Request lifecycle: how a request flows from API to database
  Key domain concepts: the 3-5 most important models/entities
  Directory structure: what each directory is for

## First Tasks
  3 good first issues for a new developer (explain what they are and why they're good starters)

## Team Conventions
  Naming conventions, code style rules, testing requirements, PR process

## Common Pitfalls
  3-5 things that confused previous developers in their first week

Use only information present in the codebase — do not invent."
```

---

## 6. Release Notes

### From Git Log

```
"Generate release notes for v[X.Y.Z] from this git log:

[paste: git log v[previous]..HEAD --oneline]

Categorize changes into:
  ## New Features
  ## Bug Fixes
  ## Performance Improvements
  ## Breaking Changes (flag clearly with ⚠️)
  ## Dependencies Updated

Format: professional changelog, user-facing language (not commit message language).
Do not include merge commits or 'fix typo' type entries.
Audience: technical users who will be upgrading."
```

---

## 7. Beginner Tier — First Documentation with Claude

### Scenario B1 — Document One Function (5 min)

```
"Add a docstring to this Python function:

def calculate_shipping_cost(weight_kg, distance_km, express=False):
    base_rate = 2.50
    cost = base_rate + (weight_kg * 0.15) + (distance_km * 0.05)
    if express:
        cost *= 1.5
    return round(cost, 2)

Use Google docstring style. Include: what it does, each parameter, return value, one example."
```

### Scenario B2 — Explain a File Before Documenting (5 min)

```
"Before I document this file:
@file:[your file]

1. Summarize what this module does in 2 sentences
2. What are the 3 most important functions?
3. Are there any functions that are hard to understand without more context?

I'll add the docstrings after I understand it."
```

---

## 8. Revision Checklist

- [ ] Can generate docstrings for all functions in a file with one prompt
- [ ] Can generate a complete project README from the codebase
- [ ] Can generate an ADR for an architectural decision in 2 minutes
- [ ] Can generate API documentation from route code without fabricating fields
- [ ] Can generate a new developer onboarding guide
- [ ] Can generate release notes from a git log
