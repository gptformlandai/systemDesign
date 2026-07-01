# Project 01: User Profile Service

Difficulty: Beginner

Build a tenant-aware user profile service that supports profile creation, lookup by email, preference updates, soft deletion, and basic profile analytics.

---

## Goal

Practice core MongoDB document modeling, tenant-scoped uniqueness, partial updates, soft deletes, projections, and simple aggregations.

You should finish this project able to explain why user preferences belong inside the user document, why email uniqueness must include `tenantId`, and how to avoid leaking profile data across tenants.

---

## Schema Design

Use one primary `users` collection. Embed bounded profile and preference fields because they are owned by the user and usually read with the user document.

```javascript
{
  _id: 'usr_1001',
  tenantId: 'tenant_acme',
  email: 'asha@example.com',
  emailNormalized: 'asha@example.com',
  displayName: 'Asha Rao',
  status: 'ACTIVE',
  roles: ['USER'],
  profile: {
    firstName: 'Asha',
    lastName: 'Rao',
    city: 'Dallas',
    state: 'TX',
    timezone: 'America/Chicago'
  },
  preferences: {
    language: 'en',
    channels: ['email', 'push'],
    marketingOptIn: false
  },
  lastLoginAt: ISODate('2026-07-01T10:00:00Z'),
  deletedAt: null,
  createdAt: ISODate('2026-07-01T09:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}
```

Optional audit trail collection:

```javascript
{
  _id: ObjectId(),
  tenantId: 'tenant_acme',
  userId: 'usr_1001',
  action: 'PROFILE_UPDATED',
  changedFields: ['profile.city', 'preferences.channels'],
  actorId: 'usr_admin',
  createdAt: ISODate('2026-07-01T10:05:00Z')
}
```

---

## Sample Data

```javascript
db.users.insertMany([
  {
    _id: 'usr_1001', tenantId: 'tenant_acme', email: 'asha@example.com', emailNormalized: 'asha@example.com',
    displayName: 'Asha Rao', status: 'ACTIVE', roles: ['USER'],
    profile: { firstName: 'Asha', lastName: 'Rao', city: 'Dallas', state: 'TX', timezone: 'America/Chicago' },
    preferences: { language: 'en', channels: ['email', 'push'], marketingOptIn: false },
    lastLoginAt: ISODate('2026-07-01T10:00:00Z'), deletedAt: null,
    createdAt: ISODate('2026-07-01T09:00:00Z'), updatedAt: ISODate('2026-07-01T10:00:00Z')
  },
  {
    _id: 'usr_1002', tenantId: 'tenant_acme', email: 'miguel@example.com', emailNormalized: 'miguel@example.com',
    displayName: 'Miguel Santos', status: 'ACTIVE', roles: ['ADMIN'],
    profile: { firstName: 'Miguel', lastName: 'Santos', city: 'Austin', state: 'TX', timezone: 'America/Chicago' },
    preferences: { language: 'en', channels: ['email'], marketingOptIn: true },
    lastLoginAt: ISODate('2026-06-29T12:00:00Z'), deletedAt: null,
    createdAt: ISODate('2026-06-01T09:00:00Z'), updatedAt: ISODate('2026-06-29T12:00:00Z')
  }
])
```

---

## CRUD Operations

Create a user:

```javascript
db.users.insertOne({
  _id: 'usr_1003',
  tenantId: 'tenant_acme',
  email: 'lin@example.com',
  emailNormalized: 'lin@example.com',
  displayName: 'Lin Chen',
  status: 'ACTIVE',
  roles: ['USER'],
  profile: { firstName: 'Lin', lastName: 'Chen', city: 'Seattle', state: 'WA', timezone: 'America/Los_Angeles' },
  preferences: { language: 'en', channels: ['push'], marketingOptIn: false },
  deletedAt: null,
  createdAt: new Date(),
  updatedAt: new Date()
})
```

Lookup by tenant and email:

```javascript
db.users.findOne(
  { tenantId: 'tenant_acme', emailNormalized: 'asha@example.com', deletedAt: null },
  { projection: { email: 1, displayName: 1, roles: 1, profile: 1, preferences: 1 } }
)
```

Patch preferences:

```javascript
db.users.updateOne(
  { tenantId: 'tenant_acme', _id: 'usr_1001', deletedAt: null },
  {
    $set: {
      'preferences.channels': ['email', 'sms'],
      updatedAt: new Date()
    }
  }
)
```

Soft delete:

```javascript
db.users.updateOne(
  { tenantId: 'tenant_acme', _id: 'usr_1003' },
  { $set: { status: 'DELETED', deletedAt: new Date(), updatedAt: new Date() } }
)
```

---

## Indexes

```javascript
db.users.createIndex(
  { tenantId: 1, emailNormalized: 1 },
  { unique: true, partialFilterExpression: { deletedAt: null } }
)

db.users.createIndex({ tenantId: 1, status: 1, updatedAt: -1 })
db.users.createIndex({ tenantId: 1, 'profile.state': 1, lastLoginAt: -1 })
db.userProfileAudit.createIndex({ tenantId: 1, userId: 1, createdAt: -1 })
```

---

## Aggregation Queries

Active users by state:

```javascript
db.users.aggregate([
  { $match: { tenantId: 'tenant_acme', status: 'ACTIVE', deletedAt: null } },
  { $group: { _id: '$profile.state', activeUsers: { $sum: 1 } } },
  { $sort: { activeUsers: -1 } }
])
```

Channel preference breakdown:

```javascript
db.users.aggregate([
  { $match: { tenantId: 'tenant_acme', deletedAt: null } },
  { $unwind: '$preferences.channels' },
  { $group: { _id: '$preferences.channels', users: { $sum: 1 } } },
  { $sort: { users: -1 } }
])
```

---

## Performance Considerations

- Normalize email before writing so lookups are exact and indexed.
- Use projections to avoid returning unnecessary PII.
- Keep embedded preferences bounded and predictable.
- Avoid regex email search on the hot login path.
- Track `docsExamined / nReturned` for user lookup queries.

---

## Scaling Considerations

- Include `tenantId` in every query and index prefix.
- For very large SaaS tenants, consider sharding by `{ tenantId: 1, _id: 1 }` rather than `tenantId` alone.
- Keep audit history in a separate collection to avoid growing user documents.
- Move cross-tenant analytics to a warehouse or summary collection.

---

## Security Considerations

- Never trust `tenantId` from the request body; derive it from auth context.
- Redact email and profile fields in logs.
- Use field-level validation for required PII fields.
- Restrict profile export/delete operations to privileged roles.
- Record profile changes in an append-only audit trail.

---

## Optional API Layer

- `POST /tenants/{tenantId}/users`
- `GET /tenants/{tenantId}/users/{userId}`
- `GET /tenants/{tenantId}/users?email=asha@example.com`
- `PATCH /tenants/{tenantId}/users/{userId}/preferences`
- `DELETE /tenants/{tenantId}/users/{userId}`

Implementation rule: repository methods should always require `tenantId` as a parameter.

---

## Interview Discussion Points

- Why embed preferences but keep audit history separate?
- How does tenant-scoped unique email work?
- What changes if users can belong to many tenants?
- How would you implement GDPR delete vs soft delete?
- How would you prevent cross-tenant data leakage?
- What index supports login by tenant and email?
