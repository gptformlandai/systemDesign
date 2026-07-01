# API Cheatsheet

| Operation | Qdrant Lab | Pinecone Mental Model |
|---|---|---|
| create container | create collection | create index |
| add records | upsert points | upsert vectors |
| search | points search | query |
| metadata | payload | metadata |
| logical partition | collection/payload strategy | namespace |
| delete | delete points | delete vectors |

Core flow:

```text
embed -> upsert -> query with filter -> rerank -> return sources
```