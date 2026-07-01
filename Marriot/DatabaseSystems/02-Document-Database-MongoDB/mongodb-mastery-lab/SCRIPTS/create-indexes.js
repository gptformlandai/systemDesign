const database = db.getSiblingDB('mongodb_mastery');

print('\nCreating production-style indexes...');

database.users.createIndex({ tenantId: 1, email: 1 }, { unique: true, name: 'uniq_tenant_email' });
database.users.createIndex({ tenantId: 1, updatedAt: -1 }, { name: 'tenant_updated_desc' });

database.products.createIndex({ tenantId: 1, sku: 1 }, { unique: true, name: 'uniq_tenant_sku' });
database.products.createIndex({ tenantId: 1, categoryId: 1, priceCents: 1 }, { name: 'tenant_category_price' });
database.products.createIndex({ tenantId: 1, brand: 1 }, { name: 'tenant_brand' });
database.products.createIndex({ tenantId: 1, 'attributes.k': 1, 'attributes.v': 1 }, { name: 'tenant_attributes' });
database.products.createIndex({ tags: 1 }, { name: 'tags_multikey' });

database.reviews.createIndex({ tenantId: 1, productId: 1, createdAt: -1 }, { name: 'tenant_product_reviews' });

database.orders.createIndex({ tenantId: 1, orderId: 1 }, { unique: true, name: 'uniq_tenant_order' });
database.orders.createIndex({ tenantId: 1, customerId: 1, createdAt: -1 }, { name: 'tenant_customer_created' });
database.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 }, { name: 'tenant_status_created' });
database.orders.createIndex({ tenantId: 1, createdAt: -1, _id: -1 }, { name: 'tenant_cursor_pagination' });

database.auditLogs.createIndex({ tenantId: 1, createdAt: -1 }, { name: 'tenant_audit_time' });
database.auditLogs.createIndex({ tenantId: 1, actorId: 1, createdAt: -1 }, { name: 'tenant_actor_time' });
database.auditLogs.createIndex({ tenantId: 1, 'target.type': 1, 'target.id': 1, createdAt: -1 }, { name: 'tenant_target_time' });

database.notifications.createIndex({ tenantId: 1, userId: 1, readAt: 1, createdAt: -1 }, { name: 'tenant_user_unread_time' });

database.ragChunks.createIndex({ tenantId: 1, sourceDocumentId: 1, chunkId: 1 }, { unique: true, name: 'uniq_tenant_source_chunk' });
database.ragChunks.createIndex({ tenantId: 1, 'metadata.tags': 1 }, { name: 'tenant_rag_tags' });

database.inventory.createIndex({ tenantId: 1, sku: 1 }, { unique: true, name: 'uniq_tenant_inventory_sku' });

database.dailyRevenue.createIndex({ tenantId: 1, day: -1 }, { name: 'tenant_day_desc' });

for (const collectionName of ['users', 'products', 'orders', 'auditLogs', 'ragChunks']) {
  print(`\nIndexes for ${collectionName}:`);
  printjson(database[collectionName].getIndexes().map((index) => ({ name: index.name, key: index.key })));
}
