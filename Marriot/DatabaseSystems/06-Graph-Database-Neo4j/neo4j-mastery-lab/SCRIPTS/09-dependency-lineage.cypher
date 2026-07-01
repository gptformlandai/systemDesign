MERGE (checkout:Service {serviceId: 'svc-checkout'}) SET checkout.name = 'checkout', checkout.criticality = 'high'
MERGE (payments:Service {serviceId: 'svc-payments'}) SET payments.name = 'payments', payments.criticality = 'high'
MERGE (risk:Service {serviceId: 'svc-risk'}) SET risk.name = 'risk', risk.criticality = 'medium'
MERGE (orders:Database {databaseId: 'db-orders'}) SET orders.name = 'orders-db'
MERGE (paymentsDb:Database {databaseId: 'db-payments'}) SET paymentsDb.name = 'payments-db'
MERGE (team:Team {teamId: 'team-platform'}) SET team.name = 'Platform Team'
MERGE (checkout)-[:CALLS {observedAt: datetime('2026-07-01T10:00:00Z')}]->(payments)
MERGE (payments)-[:CALLS {observedAt: datetime('2026-07-01T10:00:00Z')}]->(risk)
MERGE (checkout)-[:DEPENDS_ON]->(orders)
MERGE (payments)-[:DEPENDS_ON]->(paymentsDb)
MERGE (team)-[:OWNS]->(checkout)
MERGE (team)-[:OWNS]->(payments);

MERGE (raw:Dataset {datasetId: 'ds-orders-raw'}) SET raw.name = 'orders_raw'
MERGE (curated:Dataset {datasetId: 'ds-orders-curated'}) SET curated.name = 'orders_curated'
MERGE (dash:Dashboard {dashboardId: 'dash-revenue'}) SET dash.name = 'Revenue Dashboard'
MERGE (job:Job {jobId: 'job-curate-orders'}) SET job.name = 'curate_orders'
MERGE (job)-[:READS_FROM]->(raw)
MERGE (job)-[:WRITES_TO]->(curated)
MERGE (dash)-[:USES]->(curated);

MATCH path = (start:Service {serviceId: 'svc-checkout'})-[:CALLS|DEPENDS_ON*1..3]->(impacted)
RETURN 'downstream' AS view, labels(impacted) AS impactedType, coalesce(impacted.name, impacted.serviceId, impacted.databaseId) AS impacted, length(path) AS distance
ORDER BY distance, impacted;

MATCH path = (changed:Dataset {datasetId: 'ds-orders-raw'})<-[:READS_FROM]-(job:Job)-[:WRITES_TO]->(derived:Dataset)<-[:USES]-(dashboard:Dashboard)
RETURN 'lineage-impact' AS view, changed.name AS changedDataset, job.name AS job, derived.name AS derivedDataset, dashboard.name AS impactedDashboard;