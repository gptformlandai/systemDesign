# Elasticsearch Relevance, Scoring, Synonyms, and Highlighting - MAANG Master Sheet

> Track File #9 of 27 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate to senior | Mode: ranking, search quality, relevance evaluation

This sheet builds:
- Relevance and BM25 mental model
- Boosts, fuzziness, synonyms, function score, highlighting
- Search quality evaluation for MAANG-level answers

---

## 1. Relevance Is Product Engineering

Search is only useful if results match user intent.

Signals:

- lexical match quality
- field importance
- popularity
- freshness
- availability
- personalization
- business rules
- user feedback
- semantic/vector similarity

---

## 2. BM25 Mental Model

Elasticsearch's traditional text scoring is based on BM25.

Important ideas:

- term frequency: more occurrences can matter
- inverse document frequency: rare terms can matter more
- field length normalization: shorter focused fields can score differently

Interview-safe phrasing:

```text
BM25 scores lexical relevance using term frequency, rarity, and field-length normalization. We usually tune relevance through fields, analyzers, boosts, filters, and evaluation rather than manually calculating scores.
```

---

## 3. Field Boosts

```json
{
  "query": {
    "multi_match": {
      "query": "wireless headphones",
      "fields": ["title^3", "brand^2", "description"]
    }
  }
}
```

Use boosts to encode intent:

- title matches often matter more than description matches
- exact SKU matches should dominate generic text matches
- availability or popularity may be a secondary signal

---

## 4. Synonyms And Fuzziness

Synonyms handle vocabulary mismatch:

```text
tv, television
sneakers, running shoes
```

Fuzziness handles typos:

```json
{ "match": { "title": { "query": "headphnes", "fuzziness": "AUTO" } } }
```

Caution:

- synonyms can reduce precision if too broad
- fuzziness can be expensive and noisy
- apply by field and use case, not globally everywhere

---

## 5. Function Score And Ranking Signals

```json
{
  "query": {
    "function_score": {
      "query": { "match": { "title": "keyboard" } },
      "functions": [
        { "field_value_factor": { "field": "popularity", "factor": 0.2 } }
      ],
      "boost_mode": "sum"
    }
  }
}
```

Use carefully. Business boosts can bury relevance if not evaluated.

---

## 6. Highlighting

Highlighting shows matched text fragments.

```json
{
  "query": { "match": { "description": "wireless keyboard" } },
  "highlight": {
    "fields": {
      "description": {}
    }
  }
}
```

Useful for document search, support search, legal/search review, and logs.

---

## 7. Search Quality Evaluation

Track:

- zero-result rate
- click-through rate
- conversion rate
- reformulation rate
- result abandonment
- manual relevance judgments
- precision/recall for curated query sets
- latency and freshness alongside relevance

MAANG answer:

```text
I would create a golden query set and evaluate relevance changes before rollout, because analyzer/synonym/boost changes can silently regress important queries.
```

---

## 8. Strong Answer

Question:

> How do you improve product search relevance?

Strong answer:

```text
I would start with user intent and a labeled or curated query set. Then I would tune mappings and analyzers, use multi-field matching with title/brand boosts, add synonyms for known vocabulary gaps, handle typos with controlled fuzziness, and add business signals such as popularity or availability carefully. I would measure zero-result rate, clicks, conversions, and manual relevance judgments before rolling changes broadly.
```

---

## 9. Revision Notes

- One-line summary: Relevance is measured product behavior, not just query syntax.
- Three keywords: BM25, boost, synonyms.
- One interview trap: adding synonyms/fuzziness globally without evaluation.
- Memory trick: relevance change needs a test set, not a hunch.