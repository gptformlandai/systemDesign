# Elasticsearch Multi-Tenant, ACL, and RAG Retrieval - Gap Fill MAANG Sheet

> Gap-Fill Sheet - Group 04: Scenario Practice
> For: backend/search/GenAI/system design interviews | Level: senior / MAANG | Mode: tenant isolation, document authorization, RAG retrieval safety

This sheet fills one of the highest-risk Elasticsearch design areas:

```text
search results must be relevant, fresh, and authorized before any user or LLM sees them
```

---

## 1. Multi-Tenant Index Strategy

| Strategy | Fit | Risk |
|---|---|---|
| shared index + `tenant_id` filter | many small tenants | filter leak or noisy tenant |
| shared index + custom routing | tenant-local queries | hot shard for large tenants |
| index per large tenant | isolation and custom tuning | too many indices if used for everyone |
| tiered strategy | mix of small and large tenants | more operational complexity |

Strong answer:

```text
I would start with shared index plus mandatory tenant filters for small tenants, isolate very large or regulated tenants, and validate routing/hot-shard behavior before using tenant routing broadly.
```

---

## 2. ACL Modeling

Common fields:

- `tenant_id`
- `owner_id`
- `acl_ids`
- `visibility`
- `document_status`
- `classification`
- `version`

Critical rule:

```text
Authorization filters belong inside the Elasticsearch query, not after the top hits are retrieved.
```

Why:

- post-filtering can leak counts, snippets, highlights, or source metadata
- top-k can be polluted by unauthorized results
- RAG can pass forbidden chunks to the LLM before app-side cleanup

---

## 3. Authorized Search Query Shape

```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "body_chunk": "slow search runbook" } }],
      "filter": [
        { "term": { "tenant_id": "t1" } },
        { "terms": { "acl_ids": ["eng", "search"] } },
        { "term": { "document_status": "published" } }
      ]
    }
  },
  "size": 5
}
```

---

## 4. RAG Retrieval Design

Document chunk fields:

- chunk ID and document ID
- tenant and ACL metadata
- title and body chunk
- source URI and citation metadata
- version and indexed time
- lexical fields
- dense vector field when vector search is used

Retrieval flow:

```text
tenant + ACL filters -> lexical/vector/hybrid retrieval -> rerank -> source/citation validation -> LLM context
```

---

## 5. RAG Safety Tests

| Test | What It Proves |
|---|---|
| tenant leak test | user from tenant A cannot see tenant B chunks |
| ACL downgrade test | removed permission removes search visibility |
| stale chunk test | old document version is not retrieved |
| prompt-source test | answer cites retrieved authorized chunks |
| low-confidence test | system refuses or falls back when retrieval is weak |

---

## 6. Failure Modes

| Failure | Symptom | Fix |
|---|---|---|
| missing tenant filter | cross-tenant results | mandatory server-side query builder and tests |
| post-retrieval ACL filtering | poor recall or leak risk | filter before retrieval |
| noisy tenant | one tenant dominates shard/load | routing, isolation, rate limits |
| stale permission | revoked access still searchable | permission event sync and freshness SLO |
| stale embedding | relevant chunk missing or wrong | embedding version tracking and reindex |

---

## 7. Strong Interview Answer

```text
For multi-tenant search, I would make tenant and ACL filters mandatory in the server-side query builder. Small tenants can share an index with strict filters, while large or regulated tenants may need isolated indices or routing. For RAG, I would index chunks with tenant, ACL, source, version, and citation metadata, apply authorization filters before lexical/vector retrieval, optionally rerank authorized candidates, and test tenant leaks, stale permissions, stale embeddings, recall@k, groundedness, and latency.
```

---

## 8. Revision Notes

- One-line summary: Multi-tenant Elasticsearch and RAG are security-sensitive retrieval systems, not just search boxes.
- Three keywords: tenant filter, ACL, authorized retrieval.
- One interview trap: filtering unauthorized results after retrieval.
- Memory trick: before the LLM sees a chunk, the user must be allowed to see it.