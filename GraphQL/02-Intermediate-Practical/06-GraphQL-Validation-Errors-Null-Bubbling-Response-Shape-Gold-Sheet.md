# GraphQL Validation, Errors, Null Bubbling, Response Shape - Gold Sheet

> Track File #6 of 30 - Group 02: Intermediate Practical
> For: error model | Level: intermediate | Mode: validation and execution errors

## 1. Core Idea

GraphQL has validation errors before execution and execution errors during resolver execution.

```text
parse -> validate against schema -> execute resolvers -> data + errors
```

---

## 2. Response Shape

Successful partial response example:

```json
{
  "data": {
    "product": null
  },
  "errors": [
    {
      "message": "Product service unavailable",
      "path": ["product"],
      "extensions": { "code": "UPSTREAM_UNAVAILABLE" }
    }
  ]
}
```

---

## 3. Null Bubbling

If a non-null field returns null or throws, GraphQL bubbles null up to the nearest nullable parent.

This is why nullability is a production contract:

- too strict: small failure can null a large response
- too loose: clients handle too many optional states

---

## 4. Error Types

| Error Type | When It Happens |
|---|---|
| parse error | invalid GraphQL document |
| validation error | operation does not match schema |
| execution error | resolver throws or returns invalid value |
| domain error | business rule failure modeled in payload or extensions |

---

## 5. Interview Summary

```text
GraphQL can return partial data and structured errors. I distinguish validation from execution errors, design nullability intentionally, and use stable error codes/extensions so clients can respond safely.
```

---

## 6. Revision Notes

- One-line summary: GraphQL errors can coexist with partial data.
- Three keywords: validation, errors, null bubbling.
- One trap: treating every resolver exception as HTTP 500 without a client-safe error contract.