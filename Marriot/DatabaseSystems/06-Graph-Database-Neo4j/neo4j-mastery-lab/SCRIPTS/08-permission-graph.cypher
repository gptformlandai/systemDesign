MERGE (u:User {userId: 'u-perm-1'}) SET u.name = 'Priya', u.tenantId = 't1'
MERGE (g1:Group {groupId: 'g-eng'}) SET g1.name = 'Engineering', g1.tenantId = 't1'
MERGE (g2:Group {groupId: 'g-platform'}) SET g2.name = 'Platform', g2.tenantId = 't1'
MERGE (role:Role {roleId: 'r-runbook-reader'}) SET role.name = 'Runbook Reader', role.tenantId = 't1'
MERGE (perm:Permission {permissionId: 'perm-read-runbook'}) SET perm.action = 'READ', perm.scope = 'runbook', perm.tenantId = 't1'
MERGE (res:Resource {resourceId: 'res-slow-query-runbook'}) SET res.name = 'Slow Query Runbook', res.kind = 'document', res.tenantId = 't1'
MERGE (blocked:Resource {resourceId: 'res-payroll-runbook'}) SET blocked.name = 'Payroll Runbook', blocked.kind = 'document', blocked.tenantId = 't1'
MERGE (u)-[:MEMBER_OF]->(g1)
MERGE (g1)-[:MEMBER_OF]->(g2)
MERGE (g2)-[:HAS_ROLE]->(role)
MERGE (role)-[:GRANTS]->(perm)
MERGE (perm)-[:APPLIES_TO]->(res)
MERGE (u)-[:DENIED {reason: 'segregation-of-duties'}]->(blocked);

MATCH (u:User {userId: 'u-perm-1', tenantId: 't1'})
MATCH (r:Resource {resourceId: 'res-slow-query-runbook', tenantId: 't1'})
OPTIONAL MATCH denyPath = (u)-[:DENIED]->(r)
WITH u, r, denyPath
WHERE denyPath IS NULL
MATCH allowPath = (u)-[:MEMBER_OF*1..3]->(:Group)-[:HAS_ROLE]->(:Role)-[:GRANTS]->(p:Permission)-[:APPLIES_TO]->(r)
WHERE p.action = 'READ'
RETURN 'allowed' AS decision, [node IN nodes(allowPath) | coalesce(node.name, node.userId, node.groupId, node.roleId, node.permissionId, node.resourceId)] AS explanation
LIMIT 1;

MATCH (u:User {userId: 'u-perm-1', tenantId: 't1'})-[deny:DENIED]->(r:Resource {resourceId: 'res-payroll-runbook', tenantId: 't1'})
RETURN 'denied' AS decision, deny.reason AS reason, r.name AS resource;