CREATE KEYSPACE IF NOT EXISTS cassandra_mastery
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE cassandra_mastery;

DESCRIBE KEYSPACE cassandra_mastery;