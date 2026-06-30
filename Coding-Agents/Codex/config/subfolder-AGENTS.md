# Subfolder AGENTS.md — Template

> Place in a subdirectory that has DIFFERENT conventions from the root.
> Only override what differs — Codex reads both root and subfolder AGENTS.md files.

---

## This Subfolder: [subdirectory name]

**Purpose**: [What this subfolder contains — e.g., "GraphQL API layer" or "ML pipeline jobs"]  
**Overrides**: The following rules apply here instead of the root AGENTS.md rules.

---

## Override: Error Handling

[Only specify if different from root]

```
Example: In this GraphQL layer, use GraphQLError instead of HTTPException.
Errors should include: error_code (string), message (user-facing), field (if validation error).
```

---

## Override: Patterns

[Only specify if different from root]

```
Example: New resolvers should follow the pattern in src/graphql/resolvers/user_resolver.py.
Pagination in GraphQL uses cursor-based pagination (not page/page_size).
```

---

## Override: Testing

[Only specify if different from root]

```
Example: Tests here use pytest-asyncio with @pytest.mark.asyncio on all async tests.
Mock pattern: use AsyncMock for all async database calls (not MagicMock).
```

---

## Override: Verification

[Only if different from root verification command]

```bash
# Example: this subfolder has its own test command
pytest tests/graphql/ -x && pyright src/graphql/
```

---

## What Stays the Same

The following root AGENTS.md rules still apply here:
- Forbidden actions (never push, never run migrations, never log PII)
- Naming conventions
- Type hints required
- Do not modify test files
