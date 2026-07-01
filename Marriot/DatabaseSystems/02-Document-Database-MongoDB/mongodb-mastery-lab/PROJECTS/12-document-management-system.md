# Project 12: Document Management System

Difficulty: Advanced

Build a document management system with folders, document metadata, versions, permissions, retention, tags, and content search integration.

---

## Goal

Practice metadata modeling, version history, ACL design, folder paths, search-friendly indexes, and secure document access.

---

## Schema Design

Store document metadata in MongoDB and binary content in object storage. Keep versions as separate documents if version history can grow.

```javascript
// documents
{
  _id: 'doc_1001',
  tenantId: 'tenant_docs',
  title: 'Vendor Contract 2026',
  folderId: 'folder_legal',
  ownerUserId: 'usr_1001',
  currentVersion: 3,
  contentType: 'application/pdf',
  tags: ['legal', 'vendor', 'contract'],
  acl: { users: ['usr_1001'], groups: ['legal'] },
  status: 'ACTIVE',
  retention: { policy: 'seven_years', legalHold: false },
  createdAt: ISODate('2026-07-01T09:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}

// documentVersions
{
  _id: 'doc_1001:v3',
  tenantId: 'tenant_docs',
  documentId: 'doc_1001',
  version: 3,
  objectKey: 'tenant_docs/doc_1001/v3.pdf',
  sizeBytes: 245000,
  checksum: 'sha256:abc123',
  uploadedBy: 'usr_1001',
  createdAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.documents.insertMany([
  { _id: 'doc_1001', tenantId: 'tenant_docs', title: 'Vendor Contract 2026', folderId: 'folder_legal', ownerUserId: 'usr_1001', currentVersion: 3, contentType: 'application/pdf', tags: ['legal', 'vendor'], acl: { users: ['usr_1001'], groups: ['legal'] }, status: 'ACTIVE', retention: { policy: 'seven_years', legalHold: false }, createdAt: new Date(), updatedAt: new Date() },
  { _id: 'doc_1002', tenantId: 'tenant_docs', title: 'Architecture Notes', folderId: 'folder_eng', ownerUserId: 'usr_1002', currentVersion: 1, contentType: 'text/markdown', tags: ['engineering'], acl: { users: ['usr_1002'], groups: ['engineering'] }, status: 'ACTIVE', retention: { policy: 'standard', legalHold: false }, createdAt: new Date(), updatedAt: new Date() }
])

db.documentVersions.insertOne({ _id: 'doc_1001:v3', tenantId: 'tenant_docs', documentId: 'doc_1001', version: 3, objectKey: 'tenant_docs/doc_1001/v3.pdf', sizeBytes: 245000, checksum: 'sha256:abc123', uploadedBy: 'usr_1001', createdAt: new Date() })
```

---

## CRUD Operations

Create document metadata:

```javascript
db.documents.insertOne({ _id: 'doc_1003', tenantId: 'tenant_docs', title: 'Runbook', folderId: 'folder_ops', ownerUserId: 'usr_1003', currentVersion: 1, contentType: 'text/markdown', tags: ['ops'], acl: { users: ['usr_1003'], groups: ['platform'] }, status: 'ACTIVE', retention: { policy: 'standard', legalHold: false }, createdAt: new Date(), updatedAt: new Date() })
```

Upload new version:

```javascript
db.documentVersions.insertOne({ _id: 'doc_1003:v2', tenantId: 'tenant_docs', documentId: 'doc_1003', version: 2, objectKey: 'tenant_docs/doc_1003/v2.md', sizeBytes: 64000, checksum: 'sha256:def456', uploadedBy: 'usr_1003', createdAt: new Date() })
db.documents.updateOne({ tenantId: 'tenant_docs', _id: 'doc_1003' }, { $set: { currentVersion: 2, updatedAt: new Date() } })
```

List folder documents:

```javascript
db.documents.find({ tenantId: 'tenant_docs', folderId: 'folder_legal', status: 'ACTIVE' }).sort({ updatedAt: -1 }).limit(50)
```

Soft delete document:

```javascript
db.documents.updateOne({ tenantId: 'tenant_docs', _id: 'doc_1003' }, { $set: { status: 'DELETED', deletedAt: new Date(), updatedAt: new Date() } })
```

---

## Indexes

```javascript
db.documents.createIndex({ tenantId: 1, folderId: 1, updatedAt: -1 })
db.documents.createIndex({ tenantId: 1, ownerUserId: 1, updatedAt: -1 })
db.documents.createIndex({ tenantId: 1, tags: 1, updatedAt: -1 })
db.documents.createIndex({ tenantId: 1, 'acl.users': 1, updatedAt: -1 })
db.documentVersions.createIndex({ tenantId: 1, documentId: 1, version: -1 }, { unique: true })
```

---

## Aggregation Queries

Storage by content type:

```javascript
db.documentVersions.aggregate([
  { $match: { tenantId: 'tenant_docs' } },
  { $lookup: { from: 'documents', localField: 'documentId', foreignField: '_id', as: 'doc' } },
  { $unwind: '$doc' },
  { $group: { _id: '$doc.contentType', versions: { $sum: 1 }, bytes: { $sum: '$sizeBytes' } } },
  { $sort: { bytes: -1 } }
])
```

Documents by tag:

```javascript
db.documents.aggregate([
  { $match: { tenantId: 'tenant_docs', status: 'ACTIVE' } },
  { $unwind: '$tags' },
  { $group: { _id: '$tags', docs: { $sum: 1 } } },
  { $sort: { docs: -1 } }
])
```

---

## Performance Considerations

- Store binaries in object storage, not MongoDB documents.
- Keep ACL arrays bounded; use group references for large access lists.
- Use projections for folder listing.
- Avoid `$lookup` on hot download path; denormalize current version metadata if needed.

---

## Scaling Considerations

- Shard by `{ tenantId: 1, _id: 1 }` for tenant-local access.
- Send content indexing to a search service asynchronously.
- Archive old versions based on retention policy.
- Use object storage lifecycle rules for binary retention.

---

## Security Considerations

- Enforce ACL checks before signed URL generation.
- Encrypt object storage content.
- Record download and permission-change audit logs.
- Support legal hold that blocks deletion.

---

## Optional API Layer

- `POST /documents`
- `POST /documents/{documentId}/versions`
- `GET /folders/{folderId}/documents?cursor=`
- `GET /documents/{documentId}/download-url`
- `PATCH /documents/{documentId}/acl`

---

## Interview Discussion Points

- Why store metadata in MongoDB but content in object storage?
- How do you model document versions?
- How do ACL filters affect indexes?
- What is the delete behavior under legal hold?
- When should document search move to a search engine?
