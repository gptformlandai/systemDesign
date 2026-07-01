MATCH (me:User {userId: 'u1'})-[:BOUGHT]->(:Product)<-[:BOUGHT]-(similar:User)-[:BOUGHT]->(rec:Product)
WHERE NOT (me)-[:BOUGHT]->(rec)
RETURN rec.productId AS productId, rec.name AS product, count(DISTINCT similar) AS sharedBuyers
ORDER BY sharedBuyers DESC, productId
LIMIT 10;

MATCH (me:User {userId: 'u1'})-[:FOLLOWS]->(:User)-[:FOLLOWS]->(candidate:User)
WHERE candidate <> me
  AND NOT (me)-[:FOLLOWS]->(candidate)
RETURN candidate.userId AS candidateId, candidate.name AS candidateName, count(*) AS mutualPaths
ORDER BY mutualPaths DESC, candidateId
LIMIT 10;