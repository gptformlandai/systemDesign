RETURN 'neo4j-ready' AS status;

MATCH (n)
RETURN labels(n) AS labels, count(n) AS count
ORDER BY labels;

MATCH ()-[r]->()
RETURN type(r) AS relationshipType, count(r) AS count
ORDER BY relationshipType;