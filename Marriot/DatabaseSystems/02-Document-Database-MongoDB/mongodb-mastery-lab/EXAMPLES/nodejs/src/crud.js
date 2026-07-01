import { closeDb, getDb } from './db.js';

async function main() {
  const db = await getDb();
  const users = db.collection('users');

  await users.updateOne(
    { tenantId: 't1', email: 'node-demo@example.com' },
    {
      $set: {
        name: 'Node Demo',
        roles: ['USER'],
        profile: { city: 'Denver', state: 'CO' },
        updatedAt: new Date()
      },
      $setOnInsert: { createdAt: new Date() }
    },
    { upsert: true }
  );

  const user = await users.findOne(
    { tenantId: 't1', email: 'node-demo@example.com' },
    { projection: { _id: 0, email: 1, name: 1, profile: 1 } }
  );

  console.log('User:', user);

  await users.updateOne(
    { tenantId: 't1', email: 'node-demo@example.com' },
    { $addToSet: { roles: 'EDITOR' }, $currentDate: { updatedAt: true } }
  );

  await users.deleteOne({ tenantId: 't1', email: 'node-demo@example.com' });
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(closeDb);
