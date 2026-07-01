const database = db.getSiblingDB('mongodb_mastery');

print('\nAggregation 1: Sales by status');
printjson(database.orders.aggregate([
  { $match: { tenantId: 't1' } },
  { $group: { _id: '$status', orders: { $sum: 1 }, revenueCents: { $sum: '$totalCents' } } },
  { $sort: { revenueCents: -1 } }
]).toArray());

print('\nAggregation 2: Unwind order items');
printjson(database.orders.aggregate([
  { $match: { tenantId: 't1', status: { $in: ['PAID', 'SHIPPED'] } } },
  { $unwind: '$items' },
  { $group: { _id: '$items.category', units: { $sum: '$items.quantity' }, revenueCents: { $sum: { $multiply: ['$items.quantity', '$items.priceCents'] } } } },
  { $sort: { revenueCents: -1 } }
]).toArray());

print('\nAggregation 3: Faceted product browsing');
printjson(database.products.aggregate([
  { $match: { tenantId: 't1' } },
  {
    $facet: {
      results: [{ $sort: { priceCents: 1 } }, { $limit: 5 }, { $project: { name: 1, priceCents: 1, brand: 1 } }],
      brands: [{ $group: { _id: '$brand', count: { $sum: 1 } } }],
      categories: [{ $group: { _id: '$categoryId', count: { $sum: 1 } } }]
    }
  }
]).toArray());

print('\nAggregation 4: Join orders with user email after limiting');
printjson(database.orders.aggregate([
  { $match: { tenantId: 't1' } },
  { $sort: { createdAt: -1 } },
  { $limit: 5 },
  { $lookup: { from: 'users', localField: 'customerId', foreignField: '_id', as: 'customer' } },
  { $unwind: '$customer' },
  { $project: { _id: 0, orderId: 1, status: 1, totalCents: 1, customerEmail: '$customer.email' } }
]).toArray());
