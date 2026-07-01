import { closeDb, getDb } from './db.js';

async function main() {
  const db = await getDb();
  const orders = db.collection('orders');

  const report = await orders.aggregate([
    { $match: { tenantId: 't1', status: { $in: ['PAID', 'SHIPPED'] } } },
    { $unwind: '$items' },
    {
      $group: {
        _id: '$items.category',
        units: { $sum: '$items.quantity' },
        revenueCents: { $sum: { $multiply: ['$items.quantity', '$items.priceCents'] } }
      }
    },
    { $sort: { revenueCents: -1 } }
  ]).toArray();

  console.table(report.map((row) => ({ category: row._id, units: row.units, revenueCents: row.revenueCents })));
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(closeDb);
