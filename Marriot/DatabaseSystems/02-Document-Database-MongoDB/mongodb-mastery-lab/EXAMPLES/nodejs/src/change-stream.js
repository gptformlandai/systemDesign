import { getDb } from './db.js';

async function main() {
  const db = await getDb();
  const orders = db.collection('orders');

  console.log('Watching orders. In another shell, update an order status. Press Ctrl+C to stop.');

  const changeStream = orders.watch([], { fullDocument: 'updateLookup' });

  for await (const change of changeStream) {
    console.log({
      operationType: change.operationType,
      documentKey: change.documentKey,
      resumeToken: change._id,
      status: change.fullDocument?.status
    });
  }
}

main().catch((error) => {
  console.error('Change stream failed. Confirm Docker initialized the single-node replica set rs0.');
  console.error(error);
  process.exitCode = 1;
});
