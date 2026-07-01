# Project 11: Multi-Tenant SaaS

Difficulty: Advanced

Build a multi-tenant SaaS backend with tenant-scoped users, projects, billing plans, audit logs, role-based access, tenant export, and data isolation.

---

## Goal

Practice tenant isolation, tenant-scoped unique indexes, access control boundaries, noisy-neighbor scaling, and shard key design for SaaS platforms.

---

## Schema Design

Every tenant-owned document includes `tenantId`. Shared admin collections like `tenants` can omit tenant prefix because they define tenants.

```javascript
{
  _id: 'proj_1001',
  tenantId: 'tenant_acme',
  name: 'Payments Migration',
  slug: 'payments-migration',
  ownerUserId: 'usr_1001',
  memberIds: ['usr_1001', 'usr_1002'],
  status: 'ACTIVE',
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.tenants.insertMany([
  { _id: 'tenant_acme', name: 'Acme Corp', plan: 'ENTERPRISE', region: 'us-east', status: 'ACTIVE', createdAt: new Date() },
  { _id: 'tenant_beta', name: 'Beta LLC', plan: 'STARTER', region: 'us-west', status: 'ACTIVE', createdAt: new Date() }
])

db.projects.insertOne({ _id: 'proj_1001', tenantId: 'tenant_acme', name: 'Payments Migration', slug: 'payments-migration', ownerUserId: 'usr_1001', memberIds: ['usr_1001', 'usr_1002'], status: 'ACTIVE', createdAt: new Date(), updatedAt: new Date() })
```

---

## CRUD Operations

Create tenant project:

```javascript
db.projects.insertOne({ _id: 'proj_1002', tenantId: 'tenant_acme', name: 'Search Upgrade', slug: 'search-upgrade', ownerUserId: 'usr_1001', memberIds: ['usr_1001'], status: 'ACTIVE', createdAt: new Date(), updatedAt: new Date() })
```

List tenant projects:

```javascript
db.projects.find({ tenantId: 'tenant_acme', status: 'ACTIVE' }).sort({ updatedAt: -1 }).limit(20)
```

Rename project:

```javascript
db.projects.updateOne({ tenantId: 'tenant_acme', _id: 'proj_1002' }, { $set: { name: 'Search Platform Upgrade', updatedAt: new Date() } })
```

Tenant export query:

```javascript
db.projects.find({ tenantId: 'tenant_acme' })
db.users.find({ tenantId: 'tenant_acme' })
db.auditLogs.find({ tenantId: 'tenant_acme' })
```

---

## Indexes

```javascript
db.tenants.createIndex({ region: 1, status: 1 })
db.projects.createIndex({ tenantId: 1, slug: 1 }, { unique: true })
db.projects.createIndex({ tenantId: 1, ownerUserId: 1, updatedAt: -1 })
db.projects.createIndex({ tenantId: 1, memberIds: 1, updatedAt: -1 })
```

---

## Aggregation Queries

Project count by tenant plan:

```javascript
db.projects.aggregate([
  { $group: { _id: '$tenantId', projects: { $sum: 1 } } },
  { $lookup: { from: 'tenants', localField: '_id', foreignField: '_id', as: 'tenant' } },
  { $unwind: '$tenant' },
  { $group: { _id: '$tenant.plan', tenants: { $sum: 1 }, projects: { $sum: '$projects' } } }
])
```

Tenant activity:

```javascript
db.auditLogs.aggregate([
  { $match: { tenantId: 'tenant_acme' } },
  { $group: { _id: { day: { $dateTrunc: { date: '$createdAt', unit: 'day' } }, action: '$action' }, count: { $sum: 1 } } },
  { $sort: { '_id.day': -1 } }
])
```

---

## Performance Considerations

- Every hot query should include `tenantId`.
- Tenant ID should be a leading index field for tenant-owned collections.
- Avoid cross-tenant dashboard scans on the OLTP cluster.
- Watch index cache pressure from many tenants.

---

## Scaling Considerations

- Shared collections are simple but need strong application isolation.
- Large enterprise tenants may need dedicated clusters or zones.
- `tenantId` alone can create hot shards when tenant sizes are skewed.
- Consider shard keys like `{ tenantId: 1, _id: 1 }` for tenant locality plus distribution.

---

## Security Considerations

- Derive `tenantId` from auth context.
- Add repository-level tests that fail if tenant filters are missing.
- Use tenant-scoped roles and permissions.
- Audit tenant export/delete operations.

---

## Optional API Layer

- `POST /admin/tenants`
- `GET /projects?cursor=`
- `POST /projects`
- `PATCH /projects/{projectId}`
- `POST /tenants/{tenantId}/export`

---

## Interview Discussion Points

- Shared collection vs database per tenant?
- Why must unique indexes include tenant ID?
- What breaks with tenantId-only sharding?
- How do you prevent cross-tenant leaks?
- How do you isolate noisy enterprise tenants?
