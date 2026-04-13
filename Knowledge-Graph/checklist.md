Python Tech Stack for PRE-NG Knowledge Graph Analytics Service
Yes, absolutely. Python is actually the BETTER choice here. Since this is a separate analytics service with zero coupling to PRE-NG's Java hot path, Python gives you significant advantages.

Why Python Is the Right Call for THIS Service
Factor	Python Advantage
Graph + Data Science ecosystem	Neo4j, NetworkX, pandas, matplotlib — all Python-first
Speed of development	3x–5x faster to prototype graph queries and analytics
Visualization	Plotly, Dash, Streamlit — build dashboards in hours, not days
ML/AI readiness	If you ever want to predict provider assignments, Python is the only sane choice
Kafka consumer	confluent-kafka or aiokafka — battle-tested, production-grade
MSSQL connectivity	pyodbc or pymssql — reads your audit DB directly
Separate service	No dependency on PRE-NG's Java libs. Clean boundary.
Complete Python Tech Stack
Core Framework
Component	Library	Version	Purpose
Web Framework	FastAPI	0.115+	REST API for graph queries (async, fast, auto-docs)
ASGI Server	Uvicorn	0.34+	Production server for FastAPI
Task Scheduling	APScheduler / Celery	—	Nightly batch ETL jobs
Graph Database
Component	Library	Purpose
Neo4j Driver	neo4j (official)	Bolt protocol driver for Neo4j
Graph Analysis	networkx	In-memory graph algorithms (PageRank, community detection)
Graph ORM (optional)	neomodel	Django-style ORM for Neo4j nodes/relationships
Data Pipeline
Component	Library	Purpose
Kafka Consumer	confluent-kafka	Read audit events from PRE-NG's Kafka
MSSQL Reader	pyodbc + sqlalchemy	Direct read from your audit DB as fallback/batch
Data Transform	pandas	Transform audit rows → graph nodes/edges
Visualization / Dashboard
Component	Library	Purpose
Option A	Streamlit	Fastest: build interactive dashboards in pure Python
Option B	Plotly Dash	More customizable, still Python-only
Option C	Grafana + API	Your team already knows Grafana; Python API feeds it
Project Structure
pre-ng-knowledge-graph/
├── app/
│   ├── __init__.py
│   ├── main.py                     # FastAPI app entry point
│   ├── config.py                   # Settings (Neo4j, Kafka, MSSQL)
│   │
│   ├── models/                     # Graph node/relationship models
│   │   ├── __init__.py
│   │   ├── member.py               # Member node
│   │   ├── provider.py             # Provider node
│   │   ├── plan.py                 # Plan node
│   │   ├── location.py             # Location node
│   │   └── assignment.py           # Assignment relationship
│   │
│   ├── ingestion/                  # Data pipeline
│   │   ├── __init__.py
│   │   ├── kafka_consumer.py       # Consumes audit events from PRE-NG
│   │   ├── mssql_reader.py         # Batch read from audit DB
│   │   └── graph_writer.py         # Writes nodes/edges to Neo4j
│   │
│   ├── analytics/                  # Graph query endpoints
│   │   ├── __init__.py
│   │   ├── provider_coverage.py    # Coverage gap analysis
│   │   ├── pcp_migration.py        # PCP migration patterns
│   │   ├── cosmos_rejections.py    # COSMOS rejection hotspots
│   │   ├── default_pcp_analysis.py # Default PCP assignment rates
│   │   └── network_adequacy.py     # Network adequacy by geography
│   │
│   ├── api/                        # REST API routes
│   │   ├── __init__.py
│   │   ├── routes.py               # FastAPI endpoints
│   │   └── schemas.py              # Pydantic request/response models
│   │
│   └── dashboard/                  # Optional Streamlit dashboard
│       └── app.py
│
├── tests/
│   ├── test_ingestion.py
│   ├── test_analytics.py
│   └── test_api.py
│
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
└── README.md
requirements.txt
# === Web Framework ===
fastapi==0.115.6
uvicorn[standard]==0.34.0
pydantic==2.10.3
pydantic-settings==2.7.1

# === Neo4j ===
neo4j==5.27.0

# === Kafka ===
confluent-kafka==2.6.1

# === MSSQL (read audit DB) ===
pyodbc==5.2.0
sqlalchemy==2.0.36

# === Data Processing ===
pandas==2.2.3

# === Graph Analysis ===
networkx==3.4.2

# === Dashboard (pick one) ===
streamlit==1.41.1
# plotly==5.24.1
# dash==2.18.2

# === Utilities ===
python-dotenv==1.0.1
httpx==0.28.1
apscheduler==3.10.4

# === Testing ===
pytest==8.3.4
pytest-asyncio==0.24.0
Key Code Samples
config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # Neo4j
    neo4j_uri: str = "bolt://localhost:7687"
    neo4j_user: str = "neo4j"
    neo4j_password: str = "password123"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "pre-ng-kg-consumer"
    kafka_audit_topic: str = "pre-ng.audit.events"

    # MSSQL Audit DB (read-only)
    mssql_connection_string: str = (
        "mssql+pyodbc://prepcpdbid:***@wp000032337cls.ms.ds.uhc.com/"
        "prepcpprod?driver=ODBC+Driver+18+for+SQL+Server"
        "&encrypt=yes&TrustServerCertificate=yes"
    )

    class Config:
        env_file = ".env"

settings = Settings()
main.py (FastAPI)
from fastapi import FastAPI
from contextlib import asynccontextmanager
from app.ingestion.kafka_consumer import start_kafka_consumer
from app.api.routes import router
from neo4j import GraphDatabase
from app.config import settings

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: connect Neo4j + start Kafka consumer
    app.state.neo4j = GraphDatabase.driver(
        settings.neo4j_uri,
        auth=(settings.neo4j_user, settings.neo4j_password)
    )
    start_kafka_consumer()  # background thread
    yield
    # Shutdown
    app.state.neo4j.close()

app = FastAPI(
    title="PRE-NG Knowledge Graph Analytics",
    version="1.0.0",
    lifespan=lifespan
)
app.include_router(router, prefix="/api/v1/analytics")
graph_writer.py (Audit Event → Neo4j)
from neo4j import Driver

class GraphWriter:
    def __init__(self, driver: Driver):
        self.driver = driver

    def ingest_audit_event(self, event: dict):
        """Transform a PRE-NG audit event into graph nodes + relationships."""
        with self.driver.session() as session:
            session.execute_write(self._create_graph, event)

    @staticmethod
    def _create_graph(tx, event: dict):
        tx.run("""
            // Member node
            MERGE (m:Member {zip: $memberZip, county: $memberCounty, 
                             state: $memberState, div: $memberDiv})

            // Provider node
            MERGE (p:Provider {providerId: $providerId, npi: $npiId})
            ON CREATE SET p.firstName = $providerFirstName,
                          p.lastName = $providerLastName,
                          p.specialty = $specialty

            // Plan node
            MERGE (pl:Plan {planName: $planName, panel: $panel, 
                            groupNo: $groupNo})

            // Location node
            MERGE (loc:Location {zip: $providerZip, city: $providerCity, 
                                 state: $providerState})

            // Relationships
            MERGE (m)-[:WAS_RECOMMENDED {
                timestamp: datetime($timestamp),
                requestId: $requestId,
                reasonCode: $reasonCode,
                reasonDescription: $reasonDescription,
                distance: $distance,
                clientName: $clientName
            }]->(p)

            MERGE (m)-[:ENROLLED_IN]->(pl)
            MERGE (p)-[:PRACTICES_IN]->(loc)
            MERGE (p)-[:PARTICIPATES_IN]->(pl)
        """, **event)
analytics/provider_coverage.py (Example Query)
from neo4j import Driver

class ProviderCoverageAnalytics:
    def __init__(self, driver: Driver):
        self.driver = driver

    def default_pcp_rate_by_county(self):
        """Find counties where default PCPs are assigned most frequently."""
        with self.driver.session() as session:
            result = session.run("""
                MATCH (m:Member)-[r:WAS_RECOMMENDED]->(p:Provider)
                WHERE p.providerId IN [
                    '00000099000', '00000099001', '00000099005',
                    '00000077000', '00000088000', '00000099002'
                ]
                RETURN m.county AS county, m.state AS state,
                       count(r) AS defaultAssignments,
                       collect(DISTINCT p.providerId) AS defaultPCPs
                ORDER BY defaultAssignments DESC
            """)
            return [dict(record) for record in result]

    def cosmos_rejection_hotspots(self):
        """Providers that pass PES but get flagged in COSMOS."""
        with self.driver.session() as session:
            result = session.run("""
                MATCH (p:Provider)<-[r:WAS_RECOMMENDED]-(m:Member)
                WHERE r.reasonDescription CONTAINS 'mismatch in COSMOS'
                RETURN p.npi AS npi, p.firstName + ' ' + p.lastName AS name,
                       count(r) AS rejectionCount,
                       collect(DISTINCT m.county) AS affectedCounties
                ORDER BY rejectionCount DESC
                LIMIT 20
            """)
            return [dict(record) for record in result]

    def network_adequacy_gaps(self):
        """Counties with insufficient provider coverage."""
        with self.driver.session() as session:
            result = session.run("""
                MATCH (m:Member)-[r:WAS_RECOMMENDED]->(p:Provider)
                WITH m.county AS county, m.state AS state,
                     count(DISTINCT p) AS uniqueProviders,
                     avg(r.distance) AS avgDistance,
                     count(r) AS totalRequests
                WHERE uniqueProviders < 5
                RETURN county, state, uniqueProviders, 
                       round(avgDistance, 1) AS avgDistance, totalRequests
                ORDER BY uniqueProviders ASC
            """)
            return [dict(record) for record in result]
api/routes.py
from fastapi import APIRouter, Depends
from app.analytics.provider_coverage import ProviderCoverageAnalytics

router = APIRouter()

@router.get("/default-pcp-rates")
async def get_default_pcp_rates():
    analytics = ProviderCoverageAnalytics(get_neo4j_driver())
    return analytics.default_pcp_rate_by_county()

@router.get("/cosmos-rejection-hotspots")
async def get_cosmos_rejections():
    analytics = ProviderCoverageAnalytics(get_neo4j_driver())
    return analytics.cosmos_rejection_hotspots()

@router.get("/network-adequacy-gaps")
async def get_network_gaps():
    analytics = ProviderCoverageAnalytics(get_neo4j_driver())
    return analytics.network_adequacy_gaps()

@router.get("/pcp-migration/{county}")
async def get_pcp_migration(county: str):
    """Show PCP reassignment patterns for a specific county."""
    ...
docker-compose.yml (Full Local Dev)
version: '3.8'
services:
  neo4j:
    image: neo4j:5-community
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      - NEO4J_AUTH=neo4j/password123
      - NEO4J_PLUGINS=["apoc","graph-data-science"]
    volumes:
      - neo4j_data:/data

  knowledge-graph-api:
    build: .
    ports:
      - "8100:8100"
    environment:
      - NEO4J_URI=bolt://neo4j:7687
      - NEO4J_USER=neo4j
      - NEO4J_PASSWORD=password123
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - MSSQL_CONNECTION_STRING=mssql+pyodbc://prepcpdbid:***@wp000032337cls.ms.ds.uhc.com/prepcpprod?driver=ODBC+Driver+18+for+SQL+Server&encrypt=yes&TrustServerCertificate=yes
    depends_on:
      - neo4j

volumes:
  neo4j_data:
Dockerfile
FROM python:3.12-slim

WORKDIR /app

# ODBC driver for MSSQL
RUN apt-get update && apt-get install -y \
    curl gnupg2 unixodbc-dev \
    && curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add - \
    && curl https://packages.microsoft.com/config/debian/12/prod.list > /etc/apt/sources.list.d/mssql-release.list \
    && apt-get update && ACCEPT_EULA=Y apt-get install -y msodbcsql18 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ app/

EXPOSE 8100
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8100"]
Architecture Summary
┌─────────────────────────────────────────┐
│  PRE-NG (Java, unchanged)               │
│    │                                     │
│    ├─▶ Audit DB (MSSQL) ────────────┐   │
│    └─▶ Kafka: pre-ng.audit.events ──┼── │
└────────────────────────���────────────┼───┘
                                      │
                                      ▼
┌──────────────────────────────────────────┐
│  Knowledge Graph Service (Python)        │
│                                          │
│  FastAPI 0.115                           │
│    ├── Kafka Consumer (confluent-kafka)  │
│    ├── MSSQL Reader (pyodbc/sqlalchemy)  │
│    ├── Graph Writer (neo4j driver)       │
│    ├── Analytics Engine (Cypher queries) │
│    └── REST API (/api/v1/analytics/*)    │
│                                          │
│  Neo4j 5.x                               │
│    ├── Member, Provider, Plan, Location  │
│    ├── APOC plugin                       │
│    └── Graph Data Science (optional)     │
│                                          │
│  Streamlit Dashboard (optional)          │
│    └── localhost:8501                     │
└─────────────────��────────────────────────┘
Why This Works Perfectly as Python
Zero coupling — PRE-NG stays 100% Java. Python service reads Kafka/MSSQL independently.
Python's graph ecosystem is unmatched — Neo4j driver, NetworkX, pandas, all native.
FastAPI gives you auto-generated Swagger docs — your team can explore analytics endpoints instantly at /docs.
If you ever add ML (predict which provider will be assigned, anomaly detection on rejection spikes), you're already in Python.
Your Kubernetes cluster already runs containers — a Python Docker container deploys the same way as your Java services.