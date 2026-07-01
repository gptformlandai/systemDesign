const database = db.getSiblingDB('mongodb_mastery');

print('\nTransaction example: reserve inventory and mark order confirmed.');
print('The Docker lab initializes a single-node replica set named rs0 so this transaction can run locally.');

const session = db.getMongo().startSession();
const sessionDb = session.getDatabase('mongodb_mastery');

try {
  session.startTransaction({
    readConcern: { level: 'snapshot' },
    writeConcern: { w: 'majority' }
  });

  const reserveResult = sessionDb.inventory.updateOne(
    { sku: 'SKU-MOUSE-1-BLK', available: { $gte: 1 } },
    { $inc: { available: -1, reserved: 1 }, $currentDate: { updatedAt: true } }
  );

  if (reserveResult.modifiedCount !== 1) {
    throw new Error('Inventory reservation failed');
  }

  sessionDb.orders.updateOne(
    { orderId: 'ORD-1003' },
    { $set: { status: 'CONFIRMED' }, $currentDate: { updatedAt: true } }
  );

  session.commitTransaction();
  print('Committed transaction');
} catch (error) {
  print(`Transaction aborted: ${error.message}`);
  try {
    session.abortTransaction();
  } catch (abortError) {
    print(`Abort ignored: ${abortError.message}`);
  }
} finally {
  session.endSession();
}
