# VectorDB Semantic Product Search and Recommendations - Gold Sheet

> Track File #18 of 30 - Group 04: Scenario Practice
> For: search/ecommerce interviews | Level: senior | Mode: semantic search, recommendations

## 1. Product Search Use Case

User says:

```text
"comfortable shoes for standing all day"
```

Catalog says:

```text
"arch-support walking sneaker"
```

Vector search bridges intent and catalog language.

---

## 2. Record Schema

Metadata:

- product_id
- category
- brand
- price_range
- inventory_status
- region
- language
- safety/compliance flags

---

## 3. Retrieval Flow

```text
query embedding -> category/region/inventory filters -> topK candidates -> business rerank -> final results
```

Business rerank may include inventory, margin, personalization, diversity, and sponsored constraints.

---

## 4. Recommendations

Vector recommendations can use:

- product text embeddings
- user preference embeddings
- session embeddings
- multimodal product image embeddings
- collaborative signals blended outside the vector DB

---

## 5. Interview Summary

```text
For semantic product search, I would embed product titles, descriptions, attributes, and possibly images, then filter by category, region, inventory, and compliance. Vector results should be reranked with business and personalization signals. I would evaluate search success, conversion, diversity, zero-result rate, and relevance regressions.
```

---

## 6. Revision Notes

- One-line summary: Vector product search maps user intent to catalog meaning.
- Three keywords: semantic, filters, rerank.
- One trap: ignoring inventory and business constraints.