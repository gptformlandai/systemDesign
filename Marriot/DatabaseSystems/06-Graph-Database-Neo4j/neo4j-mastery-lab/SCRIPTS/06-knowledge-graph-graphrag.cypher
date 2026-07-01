CALL db.index.fulltext.queryNodes('chunk_text_index', 'slow traversal')
YIELD node, score
MATCH (doc:Document)-[:HAS_CHUNK]->(node)-[:MENTIONS]->(entity:Entity)
RETURN doc.title AS document, node.chunkId AS chunkId, node.text AS text, collect(entity.name) AS entities, score
ORDER BY score DESC;

MATCH (chunk:Chunk {chunkId: 'ch2'})-[:MENTIONS]->(entity:Entity)-[rel]->(neighbor:Entity)
RETURN chunk.title AS chunk, entity.name AS entity, type(rel) AS relationship, neighbor.name AS neighbor;