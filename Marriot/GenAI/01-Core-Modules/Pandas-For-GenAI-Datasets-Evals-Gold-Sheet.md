# Pandas for GenAI Datasets & Evaluations - Gold Sheet

> **GenAI Python Support Stack - File 3 of 5**
> For: all learners | Level: beginner to production | Mode: prepare, inspect, and evaluate GenAI data

---

## 1. Why Pandas Matters In GenAI

Pandas is the practical tool for working with tabular GenAI data:

- prompt test cases
- question/answer eval datasets
- RAG golden sets
- annotation exports
- chunk quality reports
- model comparison tables
- failure analysis
- token/cost analysis
- offline experiment results

Most GenAI teams eventually ask questions like:

```text
Which prompt version failed most often?
Which topics have bad retrieval?
Which documents produce the highest hallucination rate?
Which model is cheaper at the same quality?
```

Pandas is how you answer those quickly.

---

## 2. Mental Model

A DataFrame is like a table.

```python
import pandas as pd

df = pd.DataFrame([
    {"question": "What is GIL?", "topic": "python", "passed": True, "score": 5},
    {"question": "What is RAG?", "topic": "genai", "passed": False, "score": 2},
])

print(df)
```

Rows are records. Columns are fields.

For GenAI evals, one row often represents one test case:

```text
question | expected_answer | retrieved_context | model_answer | score | failure_reason
```

---

## 3. Java Developer Bridge

| Java / Backend Concept | Pandas Equivalent |
|---|---|
| List of DTOs | DataFrame rows |
| SQL table result | DataFrame |
| Stream filter | boolean mask |
| Stream map | `assign`, column operations, `apply` when needed |
| GroupingBy collector | `groupby` |
| Join | `merge` |
| CSV parser | `read_csv` |
| Batch report | `groupby().agg()` |

Key difference: Pandas operations are column-oriented. Prefer vectorized column operations over row-by-row loops.

---

## 4. Reading GenAI Data

### CSV

```python
import pandas as pd

df = pd.read_csv("eval_questions.csv")
print(df.head())
print(df.info())
```

### JSONL

JSONL is common for prompts/evals.

```python
df = pd.read_json("eval_results.jsonl", lines=True)
```

### Parquet

Parquet is better for large typed datasets.

```python
df = pd.read_parquet("rag_eval_results.parquet")
```

### Basic Checks

```python
print(df.shape)
print(df.columns)
print(df.isna().sum())
print(df.duplicated().sum())
```

Always inspect before trusting a dataset.

---

## 5. Building A RAG Eval Dataset

```python
import pandas as pd

questions = pd.DataFrame([
    {
        "question_id": "q1",
        "topic": "python",
        "question": "What is the GIL?",
        "expected_answer": "The GIL allows one thread to execute Python bytecode at a time in CPython.",
    },
    {
        "question_id": "q2",
        "topic": "fastapi",
        "question": "What does Depends do?",
        "expected_answer": "Depends declares dependencies resolved by FastAPI per request.",
    },
])

questions.to_csv("rag_eval_questions.csv", index=False)
```

Recommended columns:

| Column | Purpose |
|---|---|
| `question_id` | stable ID for tracking |
| `topic` | group results by skill area |
| `question` | user question |
| `expected_answer` | golden answer |
| `required_sources` | expected citations/doc IDs |
| `difficulty` | easy/medium/hard |
| `owner` | who created/validated case |
| `version` | dataset version |

---

## 6. Analyzing Eval Results

Example result table:

```python
results = pd.DataFrame([
    {"question_id": "q1", "model": "gpt-4o-mini", "prompt_version": "v1", "passed": True, "score": 5, "latency_ms": 900, "cost_usd": 0.002},
    {"question_id": "q2", "model": "gpt-4o-mini", "prompt_version": "v1", "passed": False, "score": 2, "latency_ms": 1100, "cost_usd": 0.002},
    {"question_id": "q1", "model": "gpt-4o", "prompt_version": "v2", "passed": True, "score": 5, "latency_ms": 1500, "cost_usd": 0.020},
])
```

Pass rate by model:

```python
summary = results.groupby("model").agg(
    total=("question_id", "count"),
    pass_rate=("passed", "mean"),
    avg_score=("score", "mean"),
    avg_latency_ms=("latency_ms", "mean"),
    total_cost_usd=("cost_usd", "sum"),
)

print(summary)
```

Pass rate is boolean mean: `True` = 1, `False` = 0.

---

## 7. Joining Questions With Results

```python
full = results.merge(questions, on="question_id", how="left")

print(full[["question_id", "topic", "model", "passed", "score"]])
```

Find weak topics:

```python
weak_topics = full.groupby("topic").agg(
    total=("question_id", "count"),
    pass_rate=("passed", "mean"),
    avg_score=("score", "mean"),
).sort_values("pass_rate")

print(weak_topics)
```

This is how you convert eval failures into targeted improvement work.

---

## 8. Failure Analysis

```python
failures = pd.DataFrame([
    {"question_id": "q1", "failure_reason": "none"},
    {"question_id": "q2", "failure_reason": "missing_context"},
    {"question_id": "q3", "failure_reason": "hallucination"},
    {"question_id": "q4", "failure_reason": "missing_context"},
])

counts = failures[failures["failure_reason"] != "none"].value_counts("failure_reason")
print(counts)
```

Common GenAI failure reasons:

- missing context
- wrong retrieval
- hallucination
- refusal
- unsafe response
- formatting error
- tool call failed
- timeout
- prompt injection vulnerability
- citation missing

---

## 9. Chunk Quality Reports

RAG quality starts with chunks.

```python
chunks = pd.DataFrame([
    {"doc_id": "d1", "chunk_id": "c1", "token_count": 120, "text": "..."},
    {"doc_id": "d1", "chunk_id": "c2", "token_count": 900, "text": "..."},
    {"doc_id": "d2", "chunk_id": "c3", "token_count": 20, "text": "..."},
])

print(chunks["token_count"].describe())
```

Find suspicious chunks:

```python
too_short = chunks[chunks["token_count"] < 50]
too_long = chunks[chunks["token_count"] > 800]

print("too short", len(too_short))
print("too long", len(too_long))
```

Group by document:

```python
by_doc = chunks.groupby("doc_id").agg(
    chunks=("chunk_id", "count"),
    avg_tokens=("token_count", "mean"),
    max_tokens=("token_count", "max"),
)
print(by_doc)
```

---

## 10. Token And Cost Analysis

```python
runs = pd.DataFrame([
    {"model": "model-a", "input_tokens": 1000, "output_tokens": 200, "cost_usd": 0.003},
    {"model": "model-a", "input_tokens": 1500, "output_tokens": 400, "cost_usd": 0.005},
    {"model": "model-b", "input_tokens": 1000, "output_tokens": 200, "cost_usd": 0.010},
])

cost = runs.groupby("model").agg(
    requests=("model", "count"),
    input_tokens=("input_tokens", "sum"),
    output_tokens=("output_tokens", "sum"),
    total_cost=("cost_usd", "sum"),
    avg_cost=("cost_usd", "mean"),
)

print(cost)
```

This helps decide if a better model is worth the cost.

---

## 11. Prompt Experiment Comparison

```python
experiments = pd.DataFrame([
    {"prompt_version": "v1", "passed": True, "score": 4},
    {"prompt_version": "v1", "passed": False, "score": 2},
    {"prompt_version": "v2", "passed": True, "score": 5},
    {"prompt_version": "v2", "passed": True, "score": 4},
])

comparison = experiments.groupby("prompt_version").agg(
    pass_rate=("passed", "mean"),
    avg_score=("score", "mean"),
    count=("score", "count"),
)

print(comparison.sort_values("pass_rate", ascending=False))
```

Do not ship prompt changes based on a single anecdote. Use eval tables.

---

## 12. Common Pandas Traps In GenAI

| Trap | Symptom | Fix |
|---|---|---|
| Not checking missing values | eval rows silently invalid | `df.isna().sum()` |
| Duplicate question IDs | misleading pass rates | `df.duplicated("question_id")` |
| Row loops for large data | slow notebooks/scripts | vectorized ops / `groupby` |
| `SettingWithCopyWarning` | changes may not apply | use `.loc` and `.copy()` |
| Mixing old/new prompt versions | bad conclusions | include version columns |
| Using averages only | hides hard-topic failures | group by topic/difficulty |
| Treating LLM judge as truth | biased evals | sample manually, compare judge models |

---

## 13. Mini-Lab: Eval Report

Create `lab_pandas_eval_report.py`:

```python
import pandas as pd

results = pd.DataFrame([
    {"id": "q1", "topic": "rag", "model": "small", "passed": True, "score": 4, "failure": "none"},
    {"id": "q2", "topic": "rag", "model": "small", "passed": False, "score": 2, "failure": "missing_context"},
    {"id": "q3", "topic": "agents", "model": "small", "passed": False, "score": 2, "failure": "tool_error"},
    {"id": "q4", "topic": "agents", "model": "large", "passed": True, "score": 5, "failure": "none"},
    {"id": "q5", "topic": "rag", "model": "large", "passed": True, "score": 5, "failure": "none"},
])

print("By model")
print(results.groupby("model").agg(pass_rate=("passed", "mean"), avg_score=("score", "mean")))

print("\nBy topic")
print(results.groupby("topic").agg(pass_rate=("passed", "mean"), avg_score=("score", "mean")))

print("\nFailures")
print(results[results["failure"] != "none"].value_counts("failure"))
```

Challenge:

- Add a `cost_usd` column.
- Compute score per dollar.
- Decide which model you would ship.

---

## 14. Hot Interview Q&A

**Q1: Why is Pandas useful in GenAI?**
> GenAI development is experiment-heavy. Pandas helps inspect datasets, compare model/prompt runs, analyze failures, track cost/latency, and build eval reports.

**Q2: What should every eval dataset include?**
> Stable ID, question, expected answer, topic, difficulty, required sources, dataset version, and owner/validation metadata.

**Q3: Why group evals by topic?**
> Overall pass rate can hide weak areas. A model may score 85 percent overall but fail badly on a specific domain, document type, or tool workflow.

**Q4: When should you avoid Pandas?**
> For heavy vector math use NumPy or a vector DB. For data too large for memory, use streaming, DuckDB, Polars, Spark, or a database.

**Q5: What is `SettingWithCopyWarning`?**
> It means Pandas is unsure whether you are modifying a view or a copy. Use `.loc` and explicit `.copy()` to avoid silent bugs.

---

## 15. Final Revision Checklist

- [ ] Can read CSV, JSONL, and Parquet with Pandas
- [ ] Can inspect shape, columns, missing values, and duplicates
- [ ] Can build a RAG eval dataset table
- [ ] Can compute pass rate by model, topic, and prompt version
- [ ] Can join questions with result rows using `merge`
- [ ] Can analyze failure reasons
- [ ] Can report chunk token-count quality
- [ ] Can analyze token/cost data
- [ ] Can avoid common Pandas traps in eval workflows
- [ ] Can explain Pandas to a Java developer using table/stream/grouping analogies
