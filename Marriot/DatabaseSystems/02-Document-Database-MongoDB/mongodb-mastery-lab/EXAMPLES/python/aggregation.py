import os
from pprint import pprint

from pymongo import MongoClient

MONGODB_URI = os.getenv(
    "MONGODB_URI",
    "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true",
)

client = MongoClient(MONGODB_URI, serverSelectionTimeoutMS=5000)
db = client.mongodb_mastery

pipeline = [
    {"$match": {"tenantId": "t1", "status": {"$in": ["PAID", "SHIPPED"]}}},
    {"$unwind": "$items"},
    {
        "$group": {
            "_id": "$items.category",
            "units": {"$sum": "$items.quantity"},
            "revenueCents": {
                "$sum": {"$multiply": ["$items.quantity", "$items.priceCents"]}
            },
        }
    },
    {"$sort": {"revenueCents": -1}},
]

for row in db.orders.aggregate(pipeline):
    pprint(row)

client.close()
