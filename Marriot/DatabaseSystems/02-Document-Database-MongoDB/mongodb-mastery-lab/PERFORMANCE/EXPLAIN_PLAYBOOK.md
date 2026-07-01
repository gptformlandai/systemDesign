# Explain Playbook

Use this when a query is slow or when you are designing an index.

## Step 1: Capture Query Shape

Write down:

- collection
- filter
- sort
- projection
- limit
- expected result count
- tenant or partition field

Example:

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' })
  .sort({ createdAt: -1 })
  .limit(20)
```

## Step 2: Run Explain

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' })
  .sort({ createdAt: -1 })
  .limit(20)
  .explain('executionStats')
```

## Step 3: Read Key Fields

| Field | Meaning |
|---|---|
| `winningPlan` | Plan chosen by query planner |
| `IXSCAN` | Index scan |
| `COLLSCAN` | Collection scan |
| `FETCH` | Full document fetch after index scan |
| `SORT` | Blocking sort stage |
| `nReturned` | Number of documents returned |
| `totalKeysExamined` | Index entries scanned |
| `totalDocsExamined` | Documents read |

## Step 4: Diagnose

| Symptom | Likely Cause | Fix |
|---|---|---|
| `COLLSCAN` | Missing index | Add index for filter |
| `SORT` stage | Sort not covered | Add sort field to compound index |
| Many docs examined, few returned | Low selectivity or wrong index | Improve index or schema |
| Many keys examined | Range too broad | Add equality prefix or narrow range |
| `FETCH` with many docs | Not covered or too many candidates | Covered query or better predicate |

## Step 5: Re-Test

Create index:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

Run explain again and compare.

## Interview Answer Pattern

When debugging a slow MongoDB query, I capture the exact query shape, run `explain('executionStats')`, inspect whether it is using `IXSCAN` or `COLLSCAN`, compare docs examined to documents returned, check for blocking sort, then design a compound index around equality, sort, and range fields. If the shape is still bad, I consider schema redesign or pre-aggregation.
