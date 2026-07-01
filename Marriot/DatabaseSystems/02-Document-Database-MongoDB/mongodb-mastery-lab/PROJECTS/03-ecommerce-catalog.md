# Project 03: E-Commerce Catalog

Difficulty: Beginner to Intermediate

Build a product catalog with flexible attributes, variants, categories, reviews, inventory summaries, faceted filtering, and search-friendly projections.

---

## Goal

Practice flexible schema design, attribute modeling, multikey indexes, embedded variants, referenced reviews, and aggregation for catalog filters.

---

## Schema Design

Embed bounded variants and summary fields in `products`. Keep reviews separate because they grow without bound and have different write/read patterns.

```javascript
{
  _id: 'prod_keyboard_1',
  tenantId: 'tenant_shop',
  sku: 'KEY-001',
  slug: 'mechanical-keyboard',
  title: 'Mechanical Keyboard',
  status: 'ACTIVE',
  category: { id: 'cat_keyboards', path: ['electronics', 'accessories', 'keyboards'] },
  brand: 'Northstar',
  attributes: { switchType: 'brown', layout: 'TKL', wireless: false },
  variants: [
    { variantId: 'v_black', color: 'black', priceCents: 8999, inventoryStatus: 'IN_STOCK' },
    { variantId: 'v_white', color: 'white', priceCents: 9299, inventoryStatus: 'LOW_STOCK' }
  ],
  ratingSummary: { average: 4.7, count: 214 },
  createdAt: ISODate('2026-07-01T09:00:00Z'),
  updatedAt: ISODate('2026-07-01T09:00:00Z')
}
```

---

## Sample Data

```javascript
db.products.insertMany([
  {
    _id: 'prod_keyboard_1', tenantId: 'tenant_shop', sku: 'KEY-001', slug: 'mechanical-keyboard',
    title: 'Mechanical Keyboard', status: 'ACTIVE', category: { id: 'cat_keyboards', path: ['electronics', 'accessories', 'keyboards'] },
    brand: 'Northstar', attributes: { switchType: 'brown', layout: 'TKL', wireless: false },
    variants: [{ variantId: 'v_black', color: 'black', priceCents: 8999, inventoryStatus: 'IN_STOCK' }],
    ratingSummary: { average: 4.7, count: 214 }, createdAt: new Date(), updatedAt: new Date()
  },
  {
    _id: 'prod_mouse_1', tenantId: 'tenant_shop', sku: 'MOU-001', slug: 'wireless-mouse',
    title: 'Wireless Mouse', status: 'ACTIVE', category: { id: 'cat_mice', path: ['electronics', 'accessories', 'mice'] },
    brand: 'Northstar', attributes: { dpi: 3200, wireless: true, ergonomic: true },
    variants: [{ variantId: 'v_gray', color: 'gray', priceCents: 3999, inventoryStatus: 'IN_STOCK' }],
    ratingSummary: { average: 4.4, count: 98 }, createdAt: new Date(), updatedAt: new Date()
  }
])

db.productReviews.insertOne({
  _id: 'rev_1001', tenantId: 'tenant_shop', productId: 'prod_keyboard_1', userId: 'usr_1001',
  rating: 5, title: 'Excellent typing feel', body: 'Great build quality.', createdAt: new Date()
})
```

---

## CRUD Operations

Create product:

```javascript
db.products.insertOne({
  _id: 'prod_monitor_1', tenantId: 'tenant_shop', sku: 'MON-001', slug: '27-inch-monitor',
  title: '27 Inch Monitor', status: 'ACTIVE', category: { id: 'cat_monitors', path: ['electronics', 'monitors'] },
  brand: 'Northstar', attributes: { sizeInches: 27, refreshRateHz: 144 },
  variants: [{ variantId: 'v_default', color: 'black', priceCents: 24999, inventoryStatus: 'IN_STOCK' }],
  ratingSummary: { average: 0, count: 0 }, createdAt: new Date(), updatedAt: new Date()
})
```

Find catalog page:

```javascript
db.products.find(
  { tenantId: 'tenant_shop', status: 'ACTIVE', 'category.path': 'accessories', 'attributes.wireless': true },
  { projection: { title: 1, slug: 1, brand: 1, variants: 1, ratingSummary: 1 } }
).sort({ 'ratingSummary.average': -1 }).limit(24)
```

Update variant price:

```javascript
db.products.updateOne(
  { tenantId: 'tenant_shop', _id: 'prod_keyboard_1', 'variants.variantId': 'v_black' },
  { $set: { 'variants.$.priceCents': 8499, updatedAt: new Date() } }
)
```

Add review and update summary:

```javascript
db.productReviews.insertOne({ _id: 'rev_1002', tenantId: 'tenant_shop', productId: 'prod_keyboard_1', userId: 'usr_1002', rating: 4, title: 'Solid', body: 'Good value.', createdAt: new Date() })
db.products.updateOne({ tenantId: 'tenant_shop', _id: 'prod_keyboard_1' }, { $inc: { 'ratingSummary.count': 1 }, $set: { updatedAt: new Date() } })
```

---

## Indexes

```javascript
db.products.createIndex({ tenantId: 1, sku: 1 }, { unique: true })
db.products.createIndex({ tenantId: 1, slug: 1 }, { unique: true })
db.products.createIndex({ tenantId: 1, status: 1, 'category.path': 1, brand: 1 })
db.products.createIndex({ tenantId: 1, 'attributes.wireless': 1, 'ratingSummary.average': -1 })
db.productReviews.createIndex({ tenantId: 1, productId: 1, createdAt: -1 })
```

---

## Aggregation Queries

Facet counts by brand:

```javascript
db.products.aggregate([
  { $match: { tenantId: 'tenant_shop', status: 'ACTIVE', 'category.path': 'accessories' } },
  { $group: { _id: '$brand', count: { $sum: 1 }, avgRating: { $avg: '$ratingSummary.average' } } },
  { $sort: { count: -1 } }
])
```

Price ranges:

```javascript
db.products.aggregate([
  { $match: { tenantId: 'tenant_shop', status: 'ACTIVE' } },
  { $unwind: '$variants' },
  { $bucket: { groupBy: '$variants.priceCents', boundaries: [0, 5000, 10000, 25000, 50000], default: '50000+', output: { products: { $sum: 1 } } } }
])
```

---

## Performance Considerations

- Keep product documents below growth risk by referencing reviews.
- Avoid indexing every dynamic attribute; index only common filter fields.
- Use projections for catalog cards to avoid loading full descriptions.
- Consider Atlas Search for full-text search and faceting.

---

## Scaling Considerations

- Shard by `{ tenantId: 1, sku: 1 }` or catalog-specific key for very large tenants.
- Move inventory reservations to a separate inventory service/collection.
- Precompute category counts for high-traffic catalog pages.
- Use change streams to sync product documents to search indexes.

---

## Security Considerations

- Validate dynamic attributes against category-specific allowed fields.
- Restrict price and status edits to admin roles.
- Prevent untrusted HTML in product descriptions.
- Audit catalog changes, especially price changes.

---

## Optional API Layer

- `POST /products`
- `GET /products/{slug}`
- `GET /products?category=accessories&brand=Northstar`
- `PATCH /products/{productId}/variants/{variantId}`
- `POST /products/{productId}/reviews`

---

## Interview Discussion Points

- Why embed variants but reference reviews?
- How do dynamic attributes affect indexing?
- When is MongoDB better than PostgreSQL for product catalogs?
- What would you precompute for a high-traffic category page?
- How do you keep search indexes in sync?
