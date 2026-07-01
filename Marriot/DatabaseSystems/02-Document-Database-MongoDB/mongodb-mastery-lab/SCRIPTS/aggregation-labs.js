const database = db.getSiblingDB('mongodb_mastery');

print('\nLab 1: Orders by status');
printjson(database.orders.aggregate([
  { $match: { tenantId: 't1' } },
  { $group: { _id: '$status', count: { $sum: 1 }, revenueCents: { $sum: '$totalCents' } } },
  { $sort: { revenueCents: -1 } }
]).toArray());

print('\nLab 2: Top products by units sold');
printjson(database.orders.aggregate([
  { $match: { tenantId: 't1', status: { $in: ['PAID', 'SHIPPED'] } } },
  { $unwind: '$items' },
  { $group: { _id: '$items.sku', productName: { $first: '$items.productName' }, units: { $sum: '$items.quantity' } } },
  { $sort: { units: -1 } }
]).toArray());

print('\nLab 3: Join users to recent orders');
printjson(database.orders.aggregate([
  { $match: { tenantId: 't1' } },
  { $sort: { createdAt: -1 } },
  { $limit: 10 },
  { $lookup: { from: 'users', localField: 'customerId', foreignField: '_id', as: 'customer' } },
  { $unwind: '$customer' },
  { $project: { _id: 0, orderId: 1, status: 1, totalCents: 1, customerEmail: '$customer.email' } }
]).toArray());

print('\nLab 4: Product faceting by brand and price band');
printjson(database.products.aggregate([
  { $match: { tenantId: 't1' } },
  {
    $facet: {
      brands: [{ $group: { _id: '$brand', count: { $sum: 1 } } }, { $sort: { count: -1 } }],
      priceBands: [
        {
          $bucket: {
            groupBy: '$priceCents',
            boundaries: [0, 5000, 10000, 50000],
            default: '50000+',
            output: { count: { $sum: 1 }, products: { $push: '$name' } }
          }
        }
      ]
    }
  }
]).toArray());

print('\nLab 5: Seven-day style moving average over dailyRevenue sample');
printjson(database.dailyRevenue.aggregate([
  { $match: { tenantId: 't1' } },
  {
    $setWindowFields: {
      partitionBy: '$tenantId',
      sortBy: { day: 1 },
      output: {
        movingAverageRevenueCents: {
          $avg: '$revenueCents',
          window: { documents: [-2, 0] }
        }
      }
    }
  },
  { $project: { tenantId: 1, day: 1, revenueCents: 1, movingAverageRevenueCents: 1 } }
]).toArray());
