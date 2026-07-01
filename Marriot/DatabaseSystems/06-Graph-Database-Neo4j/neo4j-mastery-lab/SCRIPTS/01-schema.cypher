CREATE CONSTRAINT user_id_unique IF NOT EXISTS
FOR (u:User) REQUIRE u.userId IS UNIQUE;

CREATE CONSTRAINT product_id_unique IF NOT EXISTS
FOR (p:Product) REQUIRE p.productId IS UNIQUE;

CREATE CONSTRAINT account_id_unique IF NOT EXISTS
FOR (a:Account) REQUIRE a.accountId IS UNIQUE;

CREATE CONSTRAINT device_id_unique IF NOT EXISTS
FOR (d:Device) REQUIRE d.deviceId IS UNIQUE;

CREATE CONSTRAINT email_id_unique IF NOT EXISTS
FOR (e:Email) REQUIRE e.email IS UNIQUE;

CREATE CONSTRAINT card_id_unique IF NOT EXISTS
FOR (c:Card) REQUIRE c.cardToken IS UNIQUE;

CREATE CONSTRAINT document_id_unique IF NOT EXISTS
FOR (d:Document) REQUIRE d.documentId IS UNIQUE;

CREATE CONSTRAINT chunk_id_unique IF NOT EXISTS
FOR (c:Chunk) REQUIRE c.chunkId IS UNIQUE;

CREATE CONSTRAINT entity_id_unique IF NOT EXISTS
FOR (e:Entity) REQUIRE e.entityId IS UNIQUE;

CREATE CONSTRAINT group_id_unique IF NOT EXISTS
FOR (g:Group) REQUIRE g.groupId IS UNIQUE;

CREATE CONSTRAINT role_id_unique IF NOT EXISTS
FOR (r:Role) REQUIRE r.roleId IS UNIQUE;

CREATE CONSTRAINT permission_id_unique IF NOT EXISTS
FOR (p:Permission) REQUIRE p.permissionId IS UNIQUE;

CREATE CONSTRAINT resource_id_unique IF NOT EXISTS
FOR (r:Resource) REQUIRE r.resourceId IS UNIQUE;

CREATE CONSTRAINT service_id_unique IF NOT EXISTS
FOR (s:Service) REQUIRE s.serviceId IS UNIQUE;

CREATE CONSTRAINT database_id_unique IF NOT EXISTS
FOR (d:Database) REQUIRE d.databaseId IS UNIQUE;

CREATE CONSTRAINT team_id_unique IF NOT EXISTS
FOR (t:Team) REQUIRE t.teamId IS UNIQUE;

CREATE CONSTRAINT dataset_id_unique IF NOT EXISTS
FOR (d:Dataset) REQUIRE d.datasetId IS UNIQUE;

CREATE CONSTRAINT job_id_unique IF NOT EXISTS
FOR (j:Job) REQUIRE j.jobId IS UNIQUE;

CREATE CONSTRAINT dashboard_id_unique IF NOT EXISTS
FOR (d:Dashboard) REQUIRE d.dashboardId IS UNIQUE;

CREATE INDEX account_status_index IF NOT EXISTS
FOR (a:Account) ON (a.status);

CREATE INDEX product_category_index IF NOT EXISTS
FOR (p:Product) ON (p.category);

CREATE INDEX user_tenant_index IF NOT EXISTS
FOR (u:User) ON (u.tenantId);

CREATE INDEX resource_tenant_index IF NOT EXISTS
FOR (r:Resource) ON (r.tenantId);

CREATE INDEX service_criticality_index IF NOT EXISTS
FOR (s:Service) ON (s.criticality);

CREATE FULLTEXT INDEX chunk_text_index IF NOT EXISTS
FOR (c:Chunk) ON EACH [c.title, c.text];

SHOW CONSTRAINTS;