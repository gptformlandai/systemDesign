import 'dotenv/config';
import { MongoClient } from 'mongodb';

const uri = process.env.MONGODB_URI ?? 'mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true';
const databaseName = process.env.MONGODB_DB ?? 'mongodb_mastery';

let client;

export async function getDb() {
  if (!client) {
    client = new MongoClient(uri, {
      maxPoolSize: 20,
      retryWrites: true,
      serverSelectionTimeoutMS: 5000
    });
    await client.connect();
  }
  return client.db(databaseName);
}

export async function closeDb() {
  if (client) {
    await client.close();
    client = undefined;
  }
}
