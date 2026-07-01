# Modeling Cheatsheet

Minimum useful vector metadata:

- tenant_id
- acl_group or ACL list
- doc_id
- chunk_id
- source
- title/section
- doc_type
- updated_at
- embedding_model
- content_hash

Golden rule:

```text
If a field is needed for security, citation, freshness, filtering, or debugging, store it as metadata.
```