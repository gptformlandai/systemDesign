import os
from datetime import datetime, timezone

from pymongo import MongoClient

MONGODB_URI = os.getenv(
    "MONGODB_URI",
    "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true",
)

client = MongoClient(MONGODB_URI, serverSelectionTimeoutMS=5000)
db = client.mongodb_mastery
users = db.users

now = datetime.now(timezone.utc)

users.update_one(
    {"tenantId": "t1", "email": "python-demo@example.com"},
    {
        "$set": {
            "name": "Python Demo",
            "roles": ["USER"],
            "profile": {"city": "Raleigh", "state": "NC"},
            "updatedAt": now,
        },
        "$setOnInsert": {"createdAt": now},
    },
    upsert=True,
)

user = users.find_one(
    {"tenantId": "t1", "email": "python-demo@example.com"},
    {"_id": 0, "email": 1, "name": 1, "profile": 1},
)
print(user)

users.update_one(
    {"tenantId": "t1", "email": "python-demo@example.com"},
    {"$addToSet": {"roles": "EDITOR"}, "$currentDate": {"updatedAt": True}},
)

users.delete_one({"tenantId": "t1", "email": "python-demo@example.com"})
client.close()
