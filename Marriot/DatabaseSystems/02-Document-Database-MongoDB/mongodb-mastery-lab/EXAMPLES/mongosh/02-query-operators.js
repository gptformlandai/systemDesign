const database = db.getSiblingDB('mongodb_mastery');

print('\nComparison operators: paid or shipped orders above $100');
printjson(database.orders.find({
  tenantId: 't1',
  status: { $in: ['PAID', 'SHIPPED'] },
  totalCents: { $gt: 10000 }
}, { _id: 0, orderId: 1, status: 1, totalCents: 1 }).toArray());

print('\nLogical operators: high-value paid order or pending Dallas order');
printjson(database.orders.find({
  tenantId: 't1',
  $or: [
    { status: 'PAID', totalCents: { $gte: 10000 } },
    { status: 'PENDING', 'shippingAddress.city': 'Dallas' }
  ]
}, { _id: 0, orderId: 1, status: 1, totalCents: 1 }).toArray());

print('\nArray contains tag');
printjson(database.products.find({ tags: 'wireless' }, { _id: 0, sku: 1, name: 1, tags: 1 }).toArray());

print('\nArray of objects with elemMatch');
printjson(database.products.find({
  variants: { $elemMatch: { color: 'black', inventory: { $gt: 20 } } }
}, { _id: 0, sku: 1, name: 1, variants: 1 }).toArray());

print('\nElement operator: users with loyalty tier');
printjson(database.users.find({ 'profile.loyaltyTier': { $exists: true } }, { _id: 0, email: 1, 'profile.loyaltyTier': 1 }).toArray());

print('\nRegex example: prefix search. For serious search use Atlas Search.');
printjson(database.products.find({ name: { $regex: /^wireless/i } }, { _id: 0, sku: 1, name: 1 }).toArray());
