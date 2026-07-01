    # MongoDB Core Architecture, BSON and ObjectId - Gold Sheet

    > **Track File #2 of 28 - Group 01: Starter Path**
    > For: backend/database/system design interviews | Level: beginner to intermediate | Mode: document model mechanics and SQL bridge

    This sheet builds:
    - Database, collection, document, field, BSON, ObjectId
- SQL-to-MongoDB concept mapping
- Schema validation and nested document behavior

Original master-map sections included here:
- 2. MongoDB Core Architecture

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 2. MongoDB Core Architecture

### Core Concepts

| Concept | Explanation | Why It Matters |
|---|---|---|
| Database | Logical namespace containing collections | Separates applications or bounded contexts |
| Collection | Group of documents | Similar to a table, but schema-flexible |
| Document | BSON object | Main unit of storage and atomic update |
| Field | Key-value pair in a document | Similar to a column but can be nested |
| BSON | Binary JSON-like format | Enables types, efficient traversal, ObjectId, dates |
| ObjectId | Common `_id` type | Globally unique-ish identifier with timestamp component |
| `_id` | Primary key field | Required, unique, automatically indexed |
| Embedded document | Document inside a document | Models data read or updated together |
| Array field | Ordered list of values/documents | Natural for tags, items, bounded children |
| Flexible schema | Documents in a collection can vary | Enables evolution, but requires discipline |
| Schema validation | Rules enforced by collection validator | Keeps flexibility from becoming chaos |

### SQL vs MongoDB Mapping

| SQL | MongoDB |
|---|---|
| Database | Database |
| Table | Collection |
| Row | Document |
| Column | Field |
| Join | Embed / Reference / `$lookup` |
| Primary key | `_id` |
| Index | Index |
| View | View / aggregation / materialized collection |
| Foreign key | Application rule / reference / validator pattern |

### BSON

MongoDB uses BSON instead of plain JSON because BSON supports richer types and efficient binary encoding.

Why BSON matters:

- Stores dates as dates, not strings.
- Supports ObjectId, Decimal128, binary, timestamps, and other types.
- Enables efficient parsing and traversal by the database engine.
- Preserves type distinctions important for indexes and comparisons.

Common BSON types:

| Type | Example | Notes |
|---|---|---|
| String | `"alice"` | UTF-8 |
| Int32 / Int64 | `42` | Driver may choose based on language |
| Double | `9.99` | Floating point |
| Decimal128 | `NumberDecimal("19.99")` | Money-like precision when used carefully |
| Boolean | `true` | Standard boolean |
| Date | `ISODate("2026-07-01T00:00:00Z")` | Stored as UTC milliseconds |
| ObjectId | `ObjectId("...")` | Common `_id` value |
| Array | `["a", "b"]` | Creates multikey indexes when indexed |
| Embedded document | `{ address: { city: "Dallas" } }` | Dot notation queries |
| Null | `null` | Distinguish from missing with care |
| Binary | `BinData(...)` | Files usually use GridFS or object storage |

### ObjectId Structure

At a high level, ObjectId contains:

- timestamp component
- process/random component
- counter component

Implications:

- It is usually sortable by creation time, but do not treat it as a perfect business timestamp.
- It is generated client-side by drivers when needed.
- It is unique enough for distributed applications under normal driver usage.
- It leaks approximate creation time, which may matter for privacy-sensitive APIs.

### `_id` Primary Key

Every MongoDB document has an `_id` field. If you do not provide one, MongoDB or the driver creates it.

```javascript
db.users.insertOne({
  name: "Asha",
  email: "asha@example.com"
})
```

Result shape:

```javascript
{
  _id: ObjectId("..."),
  name: "Asha",
  email: "asha@example.com"
}
```

Best practices:

- Use ObjectId for most documents.
- Use natural keys only when they are stable and truly unique.
- Avoid exposing raw `_id` when business IDs are safer or more meaningful.
- Create unique indexes for business uniqueness, such as email per tenant.

### Document Size Limit

MongoDB has a 16 MB maximum BSON document size. This prevents a single document from becoming an unbounded mini-database.

Design impact:

- Do not store unbounded comments, events, or logs inside one parent document.
- Embed bounded child data such as order line items.
- Use references or buckets for growing child collections.

### Nested Document Behavior

Nested documents are powerful because they let you store data in the same shape your app reads.

```javascript
{
  _id: ObjectId("..."),
  name: "Asha",
  address: {
    city: "Dallas",
    state: "TX",
    zip: "75001"
  },
  preferences: {
    channels: ["email", "sms"],
    language: "en"
  }
}
```

Query nested fields with dot notation:

```javascript
db.users.find({ "address.city": "Dallas" })
```

### Flexible Schema With Validation

MongoDB is schema-flexible, not schema-less. You can enforce validation:

```javascript
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["email", "createdAt"],
      properties: {
        email: { bsonType: "string" },
        createdAt: { bsonType: "date" },
        age: { bsonType: ["int", "long", "null"] }
      }
    }
  }
})
```

Why validation matters:

- Prevents bad writes from multiple services.
- Makes data migrations safer.
- Documents the contract close to storage.
- Reduces runtime surprises in aggregation and indexing.

---

---
