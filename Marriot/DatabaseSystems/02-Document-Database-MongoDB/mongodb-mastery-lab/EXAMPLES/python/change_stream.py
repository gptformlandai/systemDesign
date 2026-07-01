import os

from pymongo import MongoClient

MONGODB_URI = os.getenv(
    "MONGODB_URI",
    "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true",
)

client = MongoClient(MONGODB_URI, serverSelectionTimeoutMS=5000)
db = client.mongodb_mastery

print("Watching orders. Change streams require a replica set. Press Ctrl+C to stop.")

try:
    with db.orders.watch(full_document="updateLookup") as stream:
        for change in stream:
            print(
                {
                    "operationType": change["operationType"],
                    "documentKey": change["documentKey"],
                    "resumeToken": change["_id"],
                    "status": change.get("fullDocument", {}).get("status"),
                }
            )
except Exception as exc:
    print("Change stream failed. Confirm Docker initialized the single-node replica set rs0.")
    print(exc)
finally:
    client.close()
