# Project 02: Blog Platform

Difficulty: Beginner

Build a blog platform with authors, posts, comments, tags, drafts, publishing workflow, and basic reader analytics.

---

## Goal

Practice embedding bounded author snapshots, referencing unbounded comments, designing indexes for feed queries, and using aggregation for tag and author analytics.

---

## Schema Design

Use `posts` as the main collection. Embed a small author snapshot because feed pages need it. Keep comments in a separate `comments` collection because comments can grow without bound.

```javascript
// posts
{
  _id: 'post_1001',
  tenantId: 'tenant_media',
  slug: 'mongodb-schema-design',
  title: 'MongoDB Schema Design That Survives Scale',
  status: 'PUBLISHED',
  author: { userId: 'usr_1001', displayName: 'Asha Rao' },
  tags: ['mongodb', 'backend', 'system-design'],
  body: 'Long markdown body...',
  summary: 'How to model documents around access patterns.',
  stats: { views: 1200, comments: 2, likes: 88 },
  publishedAt: ISODate('2026-07-01T09:00:00Z'),
  createdAt: ISODate('2026-06-30T12:00:00Z'),
  updatedAt: ISODate('2026-07-01T09:00:00Z')
}

// comments
{
  _id: 'comment_5001',
  tenantId: 'tenant_media',
  postId: 'post_1001',
  authorId: 'usr_2001',
  body: 'Great explanation of bounded arrays.',
  status: 'VISIBLE',
  createdAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.posts.insertMany([
  {
    _id: 'post_1001', tenantId: 'tenant_media', slug: 'mongodb-schema-design',
    title: 'MongoDB Schema Design That Survives Scale', status: 'PUBLISHED',
    author: { userId: 'usr_1001', displayName: 'Asha Rao' }, tags: ['mongodb', 'backend'],
    body: 'Long markdown body...', summary: 'Model documents around access patterns.',
    stats: { views: 1200, comments: 2, likes: 88 },
    publishedAt: ISODate('2026-07-01T09:00:00Z'), createdAt: ISODate('2026-06-30T12:00:00Z'), updatedAt: ISODate('2026-07-01T09:00:00Z')
  },
  {
    _id: 'post_1002', tenantId: 'tenant_media', slug: 'indexing-for-feeds',
    title: 'Indexing for Blog Feeds', status: 'DRAFT',
    author: { userId: 'usr_1002', displayName: 'Miguel Santos' }, tags: ['mongodb', 'indexes'],
    body: 'Draft body...', summary: 'Indexes for feed and author pages.',
    stats: { views: 0, comments: 0, likes: 0 },
    publishedAt: null, createdAt: ISODate('2026-07-01T08:00:00Z'), updatedAt: ISODate('2026-07-01T08:00:00Z')
  }
])

db.comments.insertMany([
  { _id: 'comment_5001', tenantId: 'tenant_media', postId: 'post_1001', authorId: 'usr_2001', body: 'Great explanation.', status: 'VISIBLE', createdAt: ISODate('2026-07-01T10:00:00Z') },
  { _id: 'comment_5002', tenantId: 'tenant_media', postId: 'post_1001', authorId: 'usr_2002', body: 'The index section helped.', status: 'VISIBLE', createdAt: ISODate('2026-07-01T10:10:00Z') }
])
```

---

## CRUD Operations

Create draft:

```javascript
db.posts.insertOne({
  _id: 'post_1003', tenantId: 'tenant_media', slug: 'aggregation-basics',
  title: 'Aggregation Basics', status: 'DRAFT', author: { userId: 'usr_1001', displayName: 'Asha Rao' },
  tags: ['mongodb', 'aggregation'], body: 'Draft...', summary: 'Aggregation pipeline intro.',
  stats: { views: 0, comments: 0, likes: 0 }, publishedAt: null, createdAt: new Date(), updatedAt: new Date()
})
```

Publish post:

```javascript
db.posts.updateOne(
  { tenantId: 'tenant_media', _id: 'post_1003', status: 'DRAFT' },
  { $set: { status: 'PUBLISHED', publishedAt: new Date(), updatedAt: new Date() } }
)
```

List feed:

```javascript
db.posts.find(
  { tenantId: 'tenant_media', status: 'PUBLISHED' },
  { projection: { body: 0 } }
).sort({ publishedAt: -1 }).limit(20)
```

Add comment and increment counter:

```javascript
db.comments.insertOne({ _id: 'comment_5003', tenantId: 'tenant_media', postId: 'post_1001', authorId: 'usr_2003', body: 'Bookmarked.', status: 'VISIBLE', createdAt: new Date() })
db.posts.updateOne({ tenantId: 'tenant_media', _id: 'post_1001' }, { $inc: { 'stats.comments': 1 }, $set: { updatedAt: new Date() } })
```

---

## Indexes

```javascript
db.posts.createIndex({ tenantId: 1, slug: 1 }, { unique: true })
db.posts.createIndex({ tenantId: 1, status: 1, publishedAt: -1 })
db.posts.createIndex({ tenantId: 1, 'author.userId': 1, publishedAt: -1 })
db.posts.createIndex({ tenantId: 1, tags: 1, publishedAt: -1 })
db.comments.createIndex({ tenantId: 1, postId: 1, createdAt: -1 })
```

---

## Aggregation Queries

Top tags:

```javascript
db.posts.aggregate([
  { $match: { tenantId: 'tenant_media', status: 'PUBLISHED' } },
  { $unwind: '$tags' },
  { $group: { _id: '$tags', posts: { $sum: 1 }, views: { $sum: '$stats.views' } } },
  { $sort: { posts: -1, views: -1 } }
])
```

Author leaderboard:

```javascript
db.posts.aggregate([
  { $match: { tenantId: 'tenant_media', status: 'PUBLISHED' } },
  { $group: { _id: '$author.userId', posts: { $sum: 1 }, totalViews: { $sum: '$stats.views' }, name: { $first: '$author.displayName' } } },
  { $sort: { totalViews: -1 } }
])
```

---

## Performance Considerations

- Exclude `body` from feed projections.
- Keep comments separate to avoid unbounded post growth.
- Use cursor pagination on `publishedAt` and `_id` instead of deep `skip`.
- Treat counters as eventually consistent if write volume is high.

---

## Scaling Considerations

- Shard large platforms by `{ tenantId: 1, _id: 1 }` or a feed-oriented key.
- Consider search indexes or a dedicated search engine for full-text relevance.
- Move view events into an event pipeline and roll up counters asynchronously.
- Archive old comments if moderation queries do not need hot storage.

---

## Security Considerations

- Sanitize rendered markdown or HTML.
- Enforce author permissions before draft edits.
- Keep moderation actions audited.
- Do not expose deleted or draft posts through public feed queries.

---

## Optional API Layer

- `POST /posts`
- `PATCH /posts/{postId}`
- `POST /posts/{postId}/publish`
- `GET /posts?tag=mongodb&cursor=`
- `POST /posts/{postId}/comments`
- `GET /posts/{postId}/comments?cursor=`

---

## Interview Discussion Points

- Why embed author display name but reference comments?
- How would you support full-text search?
- How do counters drift, and how do you rebuild them?
- Which index supports the home feed?
- What breaks if comments are embedded forever?
