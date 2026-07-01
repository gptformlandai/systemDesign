const database = db.getSiblingDB('mongodb_mastery');

print('\nCreate: insert one demo user');
database.users.insertOne({
  _id: 'u-demo',
  tenantId: 't1',
  email: 'demo@example.com',
  name: 'Demo User',
  roles: ['USER'],
  profile: { city: 'Phoenix', state: 'AZ' },
  preferences: { channels: ['email'], language: 'en' },
  createdAt: new Date(),
  updatedAt: new Date()
});

print('\nRead: find by tenant and email');
printjson(database.users.findOne({ tenantId: 't1', email: 'demo@example.com' }, { passwordHash: 0 }));

print('\nUpdate: add role and update timestamp');
database.users.updateOne(
  { tenantId: 't1', email: 'demo@example.com' },
  { $addToSet: { roles: 'EDITOR' }, $currentDate: { updatedAt: true } }
);
printjson(database.users.findOne({ _id: 'u-demo' }));

print('\nDelete: remove demo user');
database.users.deleteOne({ _id: 'u-demo' });
printjson(database.users.findOne({ _id: 'u-demo' }));
