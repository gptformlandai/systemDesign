# Neo4j Modeling Cheatsheet

| Need | Choice |
|---|---|
| entity | node |
| category | label |
| connection | relationship type |
| connection-specific fact | relationship property |
| identity | uniqueness constraint |
| lookup start | indexed property |
| text entry point | full-text index |
| semantic entry point | vector index |

Golden rules:

- Start from domain questions.
- Use specific relationship types.
- Put facts about connections on relationships.
- Add constraints before imports.
- Bound traversals.
- Watch high-degree nodes.
- Use PROFILE before guessing.