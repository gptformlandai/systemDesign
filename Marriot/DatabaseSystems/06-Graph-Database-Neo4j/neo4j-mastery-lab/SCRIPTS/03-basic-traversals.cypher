MATCH (u:User {userId: 'u1'})-[:FOLLOWS]->(friend:User)
RETURN u.name AS user, collect(friend.name) AS follows;

MATCH (u:User {userId: 'u1'})-[:BOUGHT]->(p:Product)
RETURN u.name AS user, p.name AS product, p.category AS category;

MATCH path = (:User {userId: 'u1'})-[:FOLLOWS*1..2]->(candidate:User)
RETURN candidate.userId AS candidateId, candidate.name AS candidateName, length(path) AS hops
ORDER BY hops, candidateId;