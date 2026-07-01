# Neo4j Cypher Cheatsheet

```cypher
RETURN 1 AS ok;
MATCH (n) RETURN count(n);
CREATE (:User {userId: 'u1'});
MERGE (u:User {userId: 'u1'});
MATCH (u:User {userId: 'u1'}) RETURN u;
MATCH (u:User)-[:FOLLOWS]->(friend:User) RETURN friend;
MATCH path = (u:User)-[:FOLLOWS*1..2]->(candidate:User) RETURN path;
EXPLAIN MATCH (u:User {userId: 'u1'}) RETURN u;
PROFILE MATCH (u:User {userId: 'u1'}) RETURN u;
```

Local lab run pattern:

```bash
bash SCRIPTS/run-cypher.sh SCRIPTS/03-basic-traversals.cypher
```