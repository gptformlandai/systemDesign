# Lab 04: Errors, Nullability, And Response Shape

## Goal

Practice reasoning about partial data and null bubbling.

## Exercise

Imagine `Product.seller: Seller!` throws for one product.

Answer:

1. Which path appears in `errors`?
2. Which parent becomes null?
3. Does the whole response fail?
4. Would nullable `seller: Seller` change client impact?

## Response Sketch

```json
{
  "data": {
    "products": {
      "edges": [null]
    }
  },
  "errors": [
    { "path": ["products", "edges", 0, "node", "seller"] }
  ]
}
```

## Interview Takeaway

```text
Nullability is a production contract because resolver failures bubble through non-null parents and can turn small upstream failures into larger response failures.
```