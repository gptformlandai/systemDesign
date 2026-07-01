const database = db.getSiblingDB('mongodb_mastery');

const collections = [
  'users',
  'products',
  'reviews',
  'orders',
  'auditLogs',
  'deviceMetrics',
  'notifications',
  'ragChunks',
  'inventory',
  'dailyRevenue'
];

for (const collectionName of collections) {
  database[collectionName].drop();
}

const now = new Date();
const day = (offset) => new Date(now.getTime() + offset * 24 * 60 * 60 * 1000);
const minutesAgo = (minutes) => new Date(now.getTime() - minutes * 60 * 1000);

const users = [
  {
    _id: 'u1',
    tenantId: 't1',
    email: 'asha@example.com',
    name: 'Asha Rao',
    roles: ['USER', 'BUYER'],
    profile: { city: 'Dallas', state: 'TX', loyaltyTier: 'GOLD' },
    preferences: { channels: ['email', 'sms'], language: 'en' },
    createdAt: day(-90),
    updatedAt: day(-1)
  },
  {
    _id: 'u2',
    tenantId: 't1',
    email: 'ben@example.com',
    name: 'Ben Carter',
    roles: ['USER'],
    profile: { city: 'Austin', state: 'TX', loyaltyTier: 'SILVER' },
    preferences: { channels: ['email'], language: 'en' },
    createdAt: day(-60),
    updatedAt: day(-2)
  },
  {
    _id: 'u3',
    tenantId: 't2',
    email: 'chen@example.com',
    name: 'Chen Li',
    roles: ['ADMIN'],
    profile: { city: 'Seattle', state: 'WA', loyaltyTier: 'PLATINUM' },
    preferences: { channels: ['push'], language: 'en' },
    createdAt: day(-40),
    updatedAt: day(-1)
  }
];

database.users.insertMany(users);

const products = [
  {
    _id: 'p-keyboard',
    tenantId: 't1',
    sku: 'SKU-KEYBOARD-1',
    name: 'Wireless Mechanical Keyboard',
    categoryId: 'cat-keyboards',
    brand: 'Acme',
    priceCents: 7999,
    tags: ['wireless', 'keyboard', 'mechanical'],
    attributes: [
      { k: 'color', v: 'black' },
      { k: 'switch', v: 'brown' },
      { k: 'layout', v: 'US' }
    ],
    variants: [
      { sku: 'SKU-KEYBOARD-1-BLK', color: 'black', inventory: 42 },
      { sku: 'SKU-KEYBOARD-1-WHT', color: 'white', inventory: 12 }
    ],
    averageRating: 4.7,
    reviewCount: 1842,
    recentReviews: [
      { reviewId: 'r1', rating: 5, text: 'Excellent typing feel', createdAt: day(-3) }
    ],
    createdAt: day(-180),
    updatedAt: day(-1)
  },
  {
    _id: 'p-mouse',
    tenantId: 't1',
    sku: 'SKU-MOUSE-1',
    name: 'Ergonomic Wireless Mouse',
    categoryId: 'cat-mice',
    brand: 'Acme',
    priceCents: 3999,
    tags: ['wireless', 'mouse', 'ergonomic'],
    attributes: [
      { k: 'color', v: 'black' },
      { k: 'dpi', v: 16000 }
    ],
    variants: [{ sku: 'SKU-MOUSE-1-BLK', color: 'black', inventory: 101 }],
    averageRating: 4.4,
    reviewCount: 933,
    recentReviews: [],
    createdAt: day(-150),
    updatedAt: day(-2)
  },
  {
    _id: 'p-monitor',
    tenantId: 't1',
    sku: 'SKU-MONITOR-1',
    name: '27 Inch 4K Monitor',
    categoryId: 'cat-monitors',
    brand: 'Northstar',
    priceCents: 32999,
    tags: ['monitor', '4k', 'display'],
    attributes: [
      { k: 'size_inches', v: 27 },
      { k: 'resolution', v: '4k' }
    ],
    variants: [{ sku: 'SKU-MONITOR-1', color: 'black', inventory: 18 }],
    averageRating: 4.6,
    reviewCount: 411,
    recentReviews: [],
    createdAt: day(-120),
    updatedAt: day(-1)
  }
];

database.products.insertMany(products);

database.reviews.insertMany([
  { _id: 'r1', tenantId: 't1', productId: 'p-keyboard', userId: 'u1', rating: 5, text: 'Excellent typing feel', createdAt: day(-3) },
  { _id: 'r2', tenantId: 't1', productId: 'p-keyboard', userId: 'u2', rating: 4, text: 'Good, but loud', createdAt: day(-10) },
  { _id: 'r3', tenantId: 't1', productId: 'p-mouse', userId: 'u1', rating: 4, text: 'Comfortable for long work sessions', createdAt: day(-7) }
]);

database.orders.insertMany([
  {
    _id: 'o1001',
    tenantId: 't1',
    orderId: 'ORD-1001',
    customerId: 'u1',
    status: 'PAID',
    items: [
      { sku: 'SKU-KEYBOARD-1-BLK', productId: 'p-keyboard', productName: 'Wireless Mechanical Keyboard', category: 'keyboards', quantity: 1, priceCents: 7999 },
      { sku: 'SKU-MOUSE-1-BLK', productId: 'p-mouse', productName: 'Ergonomic Wireless Mouse', category: 'mice', quantity: 1, priceCents: 3999 }
    ],
    shippingAddress: { city: 'Dallas', state: 'TX' },
    totalCents: 11998,
    createdAt: day(-14),
    updatedAt: day(-14)
  },
  {
    _id: 'o1002',
    tenantId: 't1',
    orderId: 'ORD-1002',
    customerId: 'u2',
    status: 'SHIPPED',
    items: [
      { sku: 'SKU-MONITOR-1', productId: 'p-monitor', productName: '27 Inch 4K Monitor', category: 'monitors', quantity: 2, priceCents: 32999 }
    ],
    shippingAddress: { city: 'Austin', state: 'TX' },
    totalCents: 65998,
    createdAt: day(-9),
    updatedAt: day(-8)
  },
  {
    _id: 'o1003',
    tenantId: 't1',
    orderId: 'ORD-1003',
    customerId: 'u1',
    status: 'PENDING',
    items: [
      { sku: 'SKU-MOUSE-1-BLK', productId: 'p-mouse', productName: 'Ergonomic Wireless Mouse', category: 'mice', quantity: 3, priceCents: 3999 }
    ],
    shippingAddress: { city: 'Dallas', state: 'TX' },
    totalCents: 11997,
    createdAt: day(-1),
    updatedAt: day(-1)
  }
]);

database.inventory.insertMany([
  { _id: 'SKU-KEYBOARD-1-BLK', tenantId: 't1', sku: 'SKU-KEYBOARD-1-BLK', available: 42, reserved: 0, updatedAt: now },
  { _id: 'SKU-MOUSE-1-BLK', tenantId: 't1', sku: 'SKU-MOUSE-1-BLK', available: 101, reserved: 0, updatedAt: now },
  { _id: 'SKU-MONITOR-1', tenantId: 't1', sku: 'SKU-MONITOR-1', available: 18, reserved: 0, updatedAt: now }
]);

database.auditLogs.insertMany([
  { tenantId: 't1', actorId: 'u1', action: 'ORDER_CREATED', target: { type: 'ORDER', id: 'o1001' }, metadata: { totalCents: 11998 }, createdAt: day(-14) },
  { tenantId: 't1', actorId: 'u2', action: 'ORDER_SHIPPED', target: { type: 'ORDER', id: 'o1002' }, metadata: { carrier: 'UPS' }, createdAt: day(-8) },
  { tenantId: 't1', actorId: 'system', action: 'PRICE_UPDATED', target: { type: 'PRODUCT', id: 'p-monitor' }, metadata: { oldPriceCents: 34999, newPriceCents: 32999 }, createdAt: day(-2) }
]);

const metricDocs = [];
for (let i = 0; i < 120; i += 1) {
  metricDocs.push({
    ts: minutesAgo(120 - i),
    metadata: { tenantId: 't1', deviceId: i % 2 === 0 ? 'device-a' : 'device-b', region: 'us' },
    temperature: 70 + (i % 10),
    humidity: 0.35 + (i % 5) / 100
  });
}
database.deviceMetrics.insertMany(metricDocs);

database.notifications.insertMany([
  { _id: 'n1', tenantId: 't1', userId: 'u1', type: 'ORDER_PAID', title: 'Order paid', readAt: null, createdAt: day(-14) },
  { _id: 'n2', tenantId: 't1', userId: 'u2', type: 'ORDER_SHIPPED', title: 'Order shipped', readAt: null, createdAt: day(-8) }
]);

database.ragChunks.insertMany([
  {
    _id: 'chunk-1',
    tenantId: 't1',
    sourceDocumentId: 'doc-mongodb-architecture',
    chunkId: 'doc-mongodb-architecture:0001',
    text: 'MongoDB stores data as BSON documents inside collections. Documents can contain nested objects and arrays.',
    embedding: [0.012, -0.044, 0.091, 0.018],
    metadata: { title: 'MongoDB Architecture', page: 1, tags: ['mongodb', 'architecture'], acl: ['team-db'] },
    embeddingModel: 'demo-embedding-4d',
    createdAt: now
  },
  {
    _id: 'chunk-2',
    tenantId: 't1',
    sourceDocumentId: 'doc-indexing',
    chunkId: 'doc-indexing:0001',
    text: 'Compound indexes should usually place equality fields before sort fields and range fields.',
    embedding: [0.044, -0.012, 0.073, 0.021],
    metadata: { title: 'Indexing Guide', page: 3, tags: ['mongodb', 'indexing'], acl: ['team-db'] },
    embeddingModel: 'demo-embedding-4d',
    createdAt: now
  }
]);

database.dailyRevenue.insertMany([
  { _id: 't1:2026-06-20', tenantId: 't1', day: ISODate('2026-06-20T00:00:00Z'), orders: 12, revenueCents: 940000 },
  { _id: 't1:2026-06-21', tenantId: 't1', day: ISODate('2026-06-21T00:00:00Z'), orders: 18, revenueCents: 1350000 },
  { _id: 't1:2026-06-22', tenantId: 't1', day: ISODate('2026-06-22T00:00:00Z'), orders: 9, revenueCents: 680000 }
]);

print('Seeded mongodb_mastery database');
