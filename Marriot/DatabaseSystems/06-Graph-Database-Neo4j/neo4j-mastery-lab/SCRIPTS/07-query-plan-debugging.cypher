EXPLAIN MATCH (u:User {userId: 'u1'})-[:FOLLOWS*1..2]->(candidate:User)
RETURN candidate.userId
LIMIT 20;

PROFILE MATCH (u:User {userId: 'u1'})-[:BOUGHT]->(p:Product)
RETURN p.productId, p.name;

EXPLAIN MATCH (u:User), (p:Product)
RETURN u.userId, p.productId
LIMIT 5;