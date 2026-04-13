Explain the direct DB flow
Explain what each Python/graph component does
Show initial load, incremental refresh, and user query flow
Keep it practical and architecture-focused
Short answer
Yes — if you want to read directly from DB, the flow is much simpler than Kafka.
Think of it like this:
Your main app keeps doing what it already does
It writes request/response/provider audit data into MSSQL
A separate Python service reads that audit data in read-only mode
That Python service transforms relational rows into graph nodes + relationships
It stores them in Neo4j
FastAPI exposes analytics endpoints
Streamlit or another UI shows dashboards
So the graph system is not involved in runtime recommendation generation. It is a sidecar analytics brain built from your audit history.
 
1. Big picture architecture
PRE-NG / Main App
   |
   | writes request/response audits
   v
MSSQL Audit DB
   |
   | read-only polling / scheduled sync
   v
Python Sync Service
   |
   | transform rows -> graph entities
   v
Neo4j
   |
   | query
   +--> FastAPI analytics API
   |
   +--> Streamlit dashboard
 
2. What each tech component does
A. MSSQL
This is your source of truth for historical audit data.
It stores things like:
request info
response info
providers returned
reason codes / reason descriptions
timestamps
member geography / plan / div / panel
This is where the Python service reads from.
Role
persistent audit storage
historical dataset
source for initial backfill + incremental loads
 
B. pyodbc
This is the actual database connector from Python to SQL Server.
Role
opens the connection to MSSQL
executes SQL queries
fetches raw rows
Think of it as the wire/protocol layer.
 
C. sqlalchemy
This gives structure around DB access.
Role
manages DB engine / sessions / connection pooling
helps write cleaner SQL access logic
can be used with raw SQL or lightweight models
Think of it as the DB access manager.
If you want very simple version 1, you can even use mostly raw SQL with pyodbc and minimal SQLAlchemy.
 
D. pandas
This is the transformation layer.
Your DB rows are relational and often denormalized or split across tables.
pandas helps you reshape them into graph-friendly records.
Role
join request/response/provider rows
clean nulls / deduplicate
normalize columns
prepare final payloads to write into Neo4j
Think of it as the data shaping layer.
Example:
SQL gives you rows from request, response, provider
pandas combines them into:
one member context
one provider record
one recommendation edge
 
E. neo4j Python driver
This is how Python talks to Neo4j.
Role
opens Bolt connection to Neo4j
runs Cypher queries
upserts nodes and relationships
Think of it as the writer/query client for the graph DB.
 
F. Neo4j
This is the graph database.
It stores:
nodes like Member, Provider, Plan, Location
relationships like:
(:Member)-[:ENROLLED_IN]->(:Plan)
(:Member)-[:WAS_RECOMMENDED]->(:Provider)
(:Provider)-[:PRACTICES_IN]->(:Location)
Role
stores the connected model
makes traversal and relationship-based analytics easy
supports Cypher queries for graph analysis
Think of it as the analytics brain / relationship engine.
 
G. FastAPI
This is the analytics API layer.
Role
exposes endpoints like:
/default-pcp-rates
/cosmos-rejection-hotspots
/network-adequacy-gaps
receives HTTP requests from UI or consumers
runs Neo4j queries
returns JSON responses
Think of it as the doorway to the graph.
 
H. Uvicorn
This is just the server that runs FastAPI.
Role
hosts the FastAPI app
handles incoming HTTP traffic
Think of it as the engine running the API.
 
I. APScheduler or Celery
This is the job runner / sync scheduler.
If you're reading directly from DB, you need something to say:
run every 5 minutes
run every hour
run nightly backfill
Role
trigger sync jobs
manage refresh cadence
Which one to use?
APScheduler = simple, enough for most cases
Celery = useful only if you need distributed workers / retries / queues
For your use case, APScheduler is enough for v1.
 
J. Streamlit
This is the dashboard/UI layer.
Role
shows charts, tables, filters
calls FastAPI or Neo4j directly
helps business users explore data visually
Think of it as the presentation layer.
 
K. NetworkX
This is optional.
You do not need it for the initial architecture.
Use it only if you want:
centrality analysis
graph algorithms in Python memory
experiments beyond Cypher
Role
advanced graph experimentation
offline analytics
For v1: skip it unless you have a concrete algorithmic need.
 
3. Direct DB flow: step by step
Now let’s go through the actual flow you asked for.
 
Flow 1: Initial backfill
This happens once when you first build the graph.
Step 1: Python service connects to MSSQL
Using:
pyodbc
optionally wrapped with sqlalchemy
It runs SQL like:
SELECT ...
FROM request r
JOIN response s ON s.request_id = r.id
JOIN provider p ON p.response_id = s.id
WHERE r.created_at >= '2025-01-01'
Step 2: Fetch audit rows
The service pulls batches, for example:
10,000 requests at a time
or by ID range
or by created timestamp
Step 3: pandas transforms the data
This is where you convert table rows into graph-ready objects.
Example relational row:
request_id = 1001
member_zip = 85001
member_county = Maricopa
member_div = MA
member_plan = HMOPOS
provider_id = 00010556682
npi = 1639667405
provider_city = Phoenix
reason = Standard PRE Lookup
distance = 0.46
This becomes graph concepts:
Member node
Plan node
Provider node
Location node
WAS_RECOMMENDED relationship with metadata
Step 4: Write to Neo4j
Using the Neo4j driver, Python runs Cypher MERGE statements.
Why MERGE? Because you want idempotent upserts:
don’t create the same provider twice
don’t create the same plan twice
don’t create duplicate member-plan relationships unnecessarily
Step 5: Repeat in batches
The backfill continues until all historical records are loaded.
 
4. Incremental sync flow
After initial backfill, you don’t want to reload everything every time.
So you use a watermark.
What is a watermark?
A stored “last processed point”, for example:
last request ID processed
last updated timestamp processed
Example:
last processed request ID = 982345
Next run:
SELECT ...
FROM ...
WHERE request_id > 982345
ORDER BY request_id
Incremental flow
Scheduler wakes up every 5 or 15 minutes
Python queries only new records
pandas transforms them
Neo4j driver upserts them
watermark is updated
This is the normal steady-state flow.
 
5. User query flow
Now imagine someone opens a dashboard or calls your analytics API.
Example request
GET /api/v1/analytics/default-pcp-rates
What happens?
FastAPI receives the request
It calls analytics code
Analytics code runs a Cypher query against Neo4j
Neo4j returns results
FastAPI serializes them to JSON
UI displays the results
Important point
At query time, you are not hitting MSSQL.
MSSQL is for ingestion.
Neo4j is for analytics querying.
That separation is the whole point.
 
6. Why not query MSSQL directly for analytics every time?
You can do that for simple reporting.
But graph helps when your questions become relationship-heavy, like:
Which providers are repeatedly recommended across multiple plans and counties?
Which counties have high default PCP assignments and low real provider diversity?
Which plans keep routing members to the same few providers?
What prior PCP transitions happen most often?
Which providers appear in multiple cluster patterns across geography/network?
SQL can do some of this, but:
joins get ugly
queries get slow and hard to maintain
relationship analysis is less natural
So:
MSSQL = storage + ingestion source
Neo4j = connected analytics layer
 
7. What exactly gets modeled in the graph?
A simple practical model:
Nodes
MemberContext
Provider
Plan
Location
maybe Client
maybe Reason
I said MemberContext instead of raw Member because in healthcare data, true member identity can be sensitive and messy. Often it’s better to store an analyzed context rather than a person-centric full identity node.
Example nodes
(:MemberContext {zip, county, state, div})
(:Plan {planName, panel, groupNo})
(:Provider {providerId, npi, specialty, firstName, lastName})
(:Location {zip, city, county, state})
(:Reason {reasonDescription})
Relationships
(MemberContext)-[:ENROLLED_IN]->(Plan)
(MemberContext)-[:WAS_RECOMMENDED {distance, requestId, timestamp}]->(Provider)
(Provider)-[:PRACTICES_IN]->(Location)
(Provider)-[:PARTICIPATES_IN]->(Plan)
(Provider)-[:HAS_REASON]->(Reason)
Why this works
Because your analytics questions are usually about:
who got recommended
for what member context
under what plan/network/geography
with what outcome / reason
That is graph-friendly.
 
8. Very practical “who does what”
Here’s the simplest mental map.
MSSQL
“Here is all the history.”
Python DB reader (pyodbc / sqlalchemy)
“Let me fetch the new history.”
pandas
“Let me reshape this messy table data into graph-ready records.”
Neo4j driver
“Let me write those records into the graph.”
Neo4j
“I will store the relationships and answer connected-data questions.”
APScheduler
“I will run the sync job every N minutes.”
FastAPI
“I will expose the graph answers over HTTP.”
Uvicorn
“I will run the FastAPI service.”
Streamlit
“I will show the answers in charts and filters.”
 
9. End-to-end example
Let’s walk through one example.
In MSSQL you have:
A request:
member zip = 85001
county = Maricopa
div = MA
plan = HMOPOS
panel = 001
A response:
provider ID = 00010556682
NPI = 1639667405
specialty = Primary Care
city = Phoenix
reasonDescription = Standard PRE Lookup
distance = 0.46
Python sync job reads that row
Using SQL query.
pandas transforms it
Into a clean record like:
{
  "memberZip": "85001",
  "memberCounty": "Maricopa",
  "memberState": "AZ",
  "memberDiv": "MA",
  "planName": "HMOPOS",
  "panel": "001",
  "groupNo": "ABC123",
  "providerId": "00010556682",
  "npiId": "1639667405",
  "providerFirstName": "Craig",
  "providerLastName": "Tambatamba",
  "specialty": "Primary Care",
  "providerCity": "Phoenix",
  "providerState": "AZ",
  "providerZip": "85004",
  "reasonDescription": "Standard PRE Lookup",
  "distance": 0.46,
  "requestId": "1001",
  "timestamp": "2026-04-12T10:10:00"
}
Neo4j stores:
(:MemberContext {zip:'85001', county:'Maricopa', state:'AZ', div:'MA'})
(:Plan {planName:'HMOPOS', panel:'001', groupNo:'ABC123'})
(:Provider {providerId:'00010556682', npi:'1639667405'})
(:Location {city:'Phoenix', state:'AZ', zip:'85004'})
Relationships:
(MemberContext)-[:ENROLLED_IN]->(Plan)
(MemberContext)-[:WAS_RECOMMENDED {distance:0.46, reasonDescription:'Standard PRE Lookup'}]->(Provider)
(Provider)-[:PRACTICES_IN]->(Location)
(Provider)-[:PARTICIPATES_IN]->(Plan)
Then FastAPI answers questions like:
“Show counties with highest default PCP rate”
“Show plans with lowest provider diversity”
“Show providers repeatedly returned in Maricopa for HMOPOS”
 
10. Recommended direct-DB version for v1
If you want the cleanest version with least moving parts:
Keep
Python
pyodbc
sqlalchemy
pandas
neo4j
Neo4j DB
FastAPI
Uvicorn
APScheduler
Optional for later
Streamlit
NetworkX
Celery
My honest v1 recommendation
Start with this:
MSSQL --> Python scheduled sync --> Neo4j --> FastAPI
That’s enough.
Then add:
Streamlit
only when business users want dashboards.
 
11. How the scheduled sync usually works
A practical schedule:
Every 10 minutes
Read last watermark
Query new audit rows from MSSQL
Batch them
Transform with pandas
Upsert into Neo4j
Save new watermark
Log success/failure
Once nightly
Run reconciliation:
compare row counts
reprocess failed IDs
catch late-arriving updates
This makes the system reliable.
 
12. Important design decisions you should make early
A. What is the unique member key?
Don’t blindly use raw PHI.
You may want:
hashed member ID
or a contextual node instead of a true member identity node
B. What is the sync watermark?
Use:
created_at
or monotonic request_id
Best is usually:
updated_at if rows can change later
otherwise request_id
C. What is the graph granularity?
Do you want:
one node per member
one node per member context
one node per request
For analytics, often request-level relationships + member context nodes are more useful than person-centric modeling.
D. Will Neo4j store everything or only curated data?
Best practice:
store only fields needed for analytics
don’t dump every raw column into graph
 
13. Honest pros and cons of direct DB approach
Pros
simplest architecture
no Kafka dependency
easiest to explain
fewer moving parts
fast to deliver for v1
Cons
more load on MSSQL
polling introduces delay
harder to get real-time updates
need careful watermark logic
schema changes in DB can break your reader
read access/security needs governance
My honest opinion
For your use case, direct DB is a very reasonable v1 if:
analytics do not need real-time freshness
5–15 minute delay is acceptable
you can get read-only DB access safely
If later you want near-real-time, then move to Kafka.
 
14. The one-line summary
MSSQL is the source, Python is the mover/transformer, Neo4j is the relationship brain, FastAPI is the access layer, and Streamlit is the visualization layer.
 
If you want, next I can give you one of these:
a super simple real-world sequence diagram
a sample SQL-to-Neo4j mapping design
a v1 folder structure only for the direct-DB approach
a step-by-step implementation order for the team