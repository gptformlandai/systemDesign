const database = db.getSiblingDB('mongodb_mastery');

print('\nPerformance Lab 1: Query that should use tenant_status_created index after running create-indexes.js');
printjson(database.orders.find({ tenantId: 't1', status: 'PAID' }).sort({ createdAt: -1 }).explain('executionStats').executionStats);

print('\nPerformance Lab 2: Cursor pagination query');
const firstPage = database.orders.find({ tenantId: 't1' }).sort({ createdAt: -1, _id: -1 }).limit(2).toArray();
printjson(firstPage.map((order) => ({ orderId: order.orderId, createdAt: order.createdAt })));

if (firstPage.length > 0) {
  const last = firstPage[firstPage.length - 1];
  print('\nNext page using createdAt + _id cursor:');
  printjson(database.orders.find({
    tenantId: 't1',
    $or: [
      { createdAt: { $lt: last.createdAt } },
      { createdAt: last.createdAt, _id: { $lt: last._id } }
    ]
  }).sort({ createdAt: -1, _id: -1 }).limit(2).toArray());
}

print('\nPerformance Lab 3: Projection reduces returned document size');
printjson(database.products.find(
  { tenantId: 't1', categoryId: 'cat-keyboards' },
  { _id: 0, sku: 1, name: 1, priceCents: 1 }
).toArray());

print('\nPerformance Lab 4: Index stats');
printjson(database.orders.aggregate([{ $indexStats: {} }]).toArray().map((item) => ({ name: item.name, accesses: item.accesses.ops })));

print('\nPerformance Lab 5: Slow-query profiler setup example. Uncomment to enable in local lab.');
print('// database.setProfilingLevel(1, { slowms: 50 });');
print('// database.system.profile.find().sort({ ts: -1 }).limit(5).pretty();');
