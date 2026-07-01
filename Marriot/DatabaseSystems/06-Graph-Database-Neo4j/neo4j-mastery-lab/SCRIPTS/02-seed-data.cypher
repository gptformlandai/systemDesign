MATCH (n) DETACH DELETE n;

MERGE (u1:User {userId: 'u1'}) SET u1.name = 'Asha', u1.tier = 'gold'
MERGE (u2:User {userId: 'u2'}) SET u2.name = 'Ravi', u2.tier = 'silver'
MERGE (u3:User {userId: 'u3'}) SET u3.name = 'Maya', u3.tier = 'gold'
MERGE (u4:User {userId: 'u4'}) SET u4.name = 'Noah', u4.tier = 'bronze'
MERGE (p1:Product {productId: 'p1'}) SET p1.name = 'Graph Database Handbook', p1.category = 'books'
MERGE (p2:Product {productId: 'p2'}) SET p2.name = 'Noise Cancelling Headphones', p2.category = 'electronics'
MERGE (p3:Product {productId: 'p3'}) SET p3.name = 'Running Shoes', p3.category = 'apparel'
MERGE (u1)-[:FOLLOWS {since: date('2026-01-01')}]->(u2)
MERGE (u1)-[:FOLLOWS {since: date('2026-02-01')}]->(u3)
MERGE (u2)-[:FOLLOWS {since: date('2026-03-01')}]->(u3)
MERGE (u3)-[:FOLLOWS {since: date('2026-04-01')}]->(u4)
MERGE (u1)-[:BOUGHT {orderId: 'o1', at: datetime('2026-07-01T10:00:00Z')}]->(p1)
MERGE (u2)-[:BOUGHT {orderId: 'o2', at: datetime('2026-07-01T10:05:00Z')}]->(p1)
MERGE (u2)-[:BOUGHT {orderId: 'o3', at: datetime('2026-07-01T10:10:00Z')}]->(p2)
MERGE (u3)-[:BOUGHT {orderId: 'o4', at: datetime('2026-07-01T10:15:00Z')}]->(p2)
MERGE (u4)-[:BOUGHT {orderId: 'o5', at: datetime('2026-07-01T10:20:00Z')}]->(p3);

MERGE (a1:Account {accountId: 'a1'}) SET a1.status = 'ACTIVE', a1.riskScore = 0.18
MERGE (a2:Account {accountId: 'a2'}) SET a2.status = 'ACTIVE', a2.riskScore = 0.77
MERGE (a3:Account {accountId: 'a3'}) SET a3.status = 'SUSPENDED', a3.riskScore = 0.93
MERGE (d1:Device {deviceId: 'd1'}) SET d1.kind = 'mobile'
MERGE (d2:Device {deviceId: 'd2'}) SET d2.kind = 'browser'
MERGE (e1:Email {email: 'shared@example.com'})
MERGE (e2:Email {email: 'solo@example.com'})
MERGE (c1:Card {cardToken: 'card-1'})
MERGE (a1)-[:USES_DEVICE {firstSeen: date('2026-06-01'), count: 4}]->(d1)
MERGE (a2)-[:USES_DEVICE {firstSeen: date('2026-06-05'), count: 9}]->(d1)
MERGE (a3)-[:USES_DEVICE {firstSeen: date('2026-06-07'), count: 12}]->(d2)
MERGE (a1)-[:HAS_EMAIL]->(e1)
MERGE (a2)-[:HAS_EMAIL]->(e1)
MERGE (a3)-[:HAS_EMAIL]->(e2)
MERGE (a2)-[:PAID_WITH]->(c1)
MERGE (a3)-[:PAID_WITH]->(c1)
MERGE (a3)-[:FLAGGED_AS {reason: 'chargeback-ring', at: datetime('2026-07-01T11:00:00Z')}]->(:RiskEvent {eventId: 'r1'});

MERGE (doc1:Document {documentId: 'doc1'}) SET doc1.title = 'Neo4j Slow Query Runbook', doc1.tenantId = 't1'
MERGE (doc2:Document {documentId: 'doc2'}) SET doc2.title = 'GraphRAG Design Notes', doc2.tenantId = 't1'
MERGE (ch1:Chunk {chunkId: 'ch1'}) SET ch1.title = 'Slow traversal', ch1.text = 'Slow Neo4j queries often involve missing anchors, Cartesian products, supernodes, or unbounded traversals.', ch1.acl = 'eng'
MERGE (ch2:Chunk {chunkId: 'ch2'}) SET ch2.title = 'GraphRAG retrieval', ch2.text = 'GraphRAG combines vector or text retrieval with entity graph expansion and provenance-aware citations.', ch2.acl = 'eng'
MERGE (neo:Entity {entityId: 'e-neo4j'}) SET neo.name = 'Neo4j', neo.kind = 'database'
MERGE (rag:Entity {entityId: 'e-graphrag'}) SET rag.name = 'GraphRAG', rag.kind = 'architecture'
MERGE (prof:Entity {entityId: 'e-profile'}) SET prof.name = 'PROFILE', prof.kind = 'cypher-tool'
MERGE (doc1)-[:HAS_CHUNK]->(ch1)
MERGE (doc2)-[:HAS_CHUNK]->(ch2)
MERGE (ch1)-[:MENTIONS {confidence: 0.95}]->(neo)
MERGE (ch1)-[:MENTIONS {confidence: 0.91}]->(prof)
MERGE (ch2)-[:MENTIONS {confidence: 0.97}]->(rag)
MERGE (ch2)-[:MENTIONS {confidence: 0.89}]->(neo)
MERGE (rag)-[:USES]->(neo);

MATCH (n)
RETURN labels(n) AS labels, count(n) AS count
ORDER BY labels;