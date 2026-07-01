const database = db.getSiblingDB('mongodb_mastery');

print('\nCreate a compound index for tenant/status/date query');
database.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 }, { name: 'tenant_status_created' });

print('\nExplain plan for tenant/status/date query');
const explain = database.orders.find({ tenantId: 't1', status: 'PAID' })
  .sort({ createdAt: -1 })
  .limit(20)
  .explain('executionStats');

printjson({
  nReturned: explain.executionStats.nReturned,
  totalKeysExamined: explain.executionStats.totalKeysExamined,
  totalDocsExamined: explain.executionStats.totalDocsExamined,
  winningPlan: explain.queryPlanner.winningPlan
});

print('\nCovered query example');
database.users.createIndex({ tenantId: 1, email: 1, name: 1 }, { name: 'tenant_email_name_cover' });
const covered = database.users.find(
  { tenantId: 't1', email: 'asha@example.com' },
  { _id: 0, email: 1, name: 1 }
).explain('executionStats');
printjson({
  nReturned: covered.executionStats.nReturned,
  totalKeysExamined: covered.executionStats.totalKeysExamined,
  totalDocsExamined: covered.executionStats.totalDocsExamined,
  winningPlan: covered.queryPlanner.winningPlan
});
