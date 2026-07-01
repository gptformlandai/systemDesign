# Lab 02: Mappings And Analyzers

Goal: understand field types and tokenization.

---

## Run

```bash
bash SCRIPTS/run-request.sh SCRIPTS/05-analyze.sh
```

---

## Exercise

Inspect the `products-v1` mapping:

```bash
curl http://localhost:9200/products-v1/_mapping?pretty
```

Explain:

- why `title` is `text`
- why `brand` and `category` are `keyword`
- why `price` is numeric
- why `title.autocomplete` uses edge n-grams
- why dynamic mapping is strict in this lab

---

## Completion Gate

- You can explain `text` vs `keyword`.
- You can explain analyzer tokens from `_analyze`.
- You can name mapping explosion risk.