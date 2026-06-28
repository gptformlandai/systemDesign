# Deployment Strategies — Blue-Green, Canary, Helm, Zero-Downtime — Gold Sheet

> Topic: How to ship code to production without downtime — rolling updates, blue-green switches, canary releases, and Helm chart management

---

## 1. Intuition

Marriott.com cannot go down during booking. A deploy at 2pm is not a scheduled outage. Zero-downtime deployment is not optional — it's a baseline requirement. The question is: how do you swap running code for new code while thousands of users are actively using the old code? Blue-green, rolling, and canary each answer this differently. Helm manages the YAML that Kubernetes uses to run your code.

Beginner version:

> Zero-downtime deployment = don't kill the old version until the new version is proven healthy. Blue-green has two full environments. Canary sends 5% of users to the new version first. Rolling replaces pods one at a time.

---

## 2. Definition

- **Rolling update:** Replace pods one at a time — kill one old pod, start one new pod, wait for health check, repeat.
- **Blue-green deployment:** Run two identical production environments. Switch traffic in one step. Instant rollback by switching back.
- **Canary deployment:** Route a small percentage of traffic to the new version. Increase percentage as metrics look good. Full rollback by sending 0% to canary.
- **Helm:** Package manager for Kubernetes — bundles all YAML (Deployment, Service, ConfigMap, Ingress) into a versioned chart with templating.

---

## 3. Kubernetes Deployment Strategies

### Rolling Update (default)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking-service
spec:
  replicas: 10
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1    # At most 1 pod unavailable at a time
      maxSurge: 2          # Can temporarily have 12 pods (10+2) during rollout
  template:
    spec:
      containers:
      - name: booking-service
        image: marriott/booking-service:1.5.0
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          failureThreshold: 3        # 3 failures → pod not ready → rollout pauses
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        lifecycle:
          preStop:
            exec:
              command: ["sh", "-c", "sleep 10"]  # Wait for load balancer to drain connections
      terminationGracePeriodSeconds: 60          # Time to finish in-flight requests
```

**Rolling update sequence:**
```
Initial: 10 pods v1.4
Step 1:  Create 2 surge pods v1.5 → 12 pods total
Step 2:  Terminate 1 pod v1.4 → 11 pods (2 v1.5, 9 v1.4)
Step 3:  Wait for v1.5 pods to pass readiness → repeat until 10 v1.5 pods
```

### Recreate (not zero-downtime)

```yaml
strategy:
  type: Recreate   # Kill ALL old pods first, then create new ones
                   # Use ONLY for stateful workloads that can't have two versions running
```

---

## 4. Blue-Green Deployment

```
BLUE (active — v1.4)          GREEN (new — v1.5)
  booking-service: 10 pods  ←  booking-service: 10 pods (idle)
         ↑
  Load Balancer / Ingress → 100% traffic to BLUE

SWITCH:
  Update Ingress selector:  app=booking-service-blue → app=booking-service-green
  
Result:
BLUE (idle — v1.4)            GREEN (active — v1.5)
  booking-service: 10 pods      booking-service: 10 pods
                                        ↑
                        Load Balancer → 100% traffic to GREEN

Rollback:
  Switch Ingress back to BLUE — takes seconds
```

**Kubernetes implementation with selector switch:**

```yaml
# Production Service — points to active color via label selector
apiVersion: v1
kind: Service
metadata:
  name: booking-service
spec:
  selector:
    app: booking-service
    color: green           # ← Change this to blue/green to switch traffic
  ports:
  - port: 80
    targetPort: 8080
```

```bash
# Deploy green
kubectl apply -f booking-service-green.yaml

# Verify green is healthy
kubectl rollout status deployment/booking-service-green

# Switch traffic (atomic — no downtime)
kubectl patch service booking-service -p '{"spec":{"selector":{"color":"green"}}}'

# Monitor metrics for 15 minutes
# If healthy: scale down blue
kubectl scale deployment booking-service-blue --replicas=0

# If unhealthy: rollback in seconds
kubectl patch service booking-service -p '{"spec":{"selector":{"color":"blue"}}}'
```

**Requirements for blue-green:**
- Database must be backward-compatible with both v1.4 and v1.5 simultaneously
- Double the compute cost during the switch window
- External state (Redis, DB) must be shared between blue and green

---

## 5. Canary Deployment

Canary routes a fraction of traffic to the new version. Kubernetes native approach uses weighted services via Ingress or a service mesh.

```yaml
# NGINX Ingress canary annotation approach
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: booking-service-canary
  annotations:
    nginx.ingress.kubernetes.io/canary: "true"
    nginx.ingress.kubernetes.io/canary-weight: "10"  # 10% of traffic to canary
spec:
  rules:
  - host: booking.marriott.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: booking-service-canary    # New version
            port:
              number: 80
```

**Canary promotion workflow:**

```
Week 1:   5% traffic to v1.5
          Monitor: error rate, P99 latency, business metrics (booking conversion)
          
Week 2:   If metrics healthy → promote to 25%
          
Week 3:   → 50%

Week 4:   → 100% (decommission old Deployment)
```

**Automated canary with Argo Rollouts:**

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: booking-service
spec:
  strategy:
    canary:
      steps:
      - setWeight: 10        # 10% of traffic
      - pause: {duration: 10m}  # Wait 10 minutes
      - analysis:            # Check metrics before promoting
          templates:
          - templateName: booking-error-rate
      - setWeight: 50
      - pause: {duration: 30m}
      - analysis:
          templates:
          - templateName: booking-error-rate
      - setWeight: 100
```

---

## 6. Feature Flags

Feature flags decouple deployment from release — code ships to production but the feature is turned off.

### Spring — `@ConditionalOnProperty`

```java
// Feature is disabled unless flag is true in config
@ConditionalOnProperty(name = "features.newPricingEngine.enabled", havingValue = "true")
@Service
public class NewPricingEngineService implements PricingService {
    // Only instantiated when feature flag is on
}

// In application.yml:
features:
  newPricingEngine:
    enabled: ${FEATURE_NEW_PRICING:false}  # Default off; set env var to enable
```

### Unleash (Open Source Feature Flag Platform)

```java
@Component
@RequiredArgsConstructor
public class BookingController {

    private final Unleash unleash;

    public ResponseEntity<BookingResponse> getAvailability(BookingRequest request) {
        if (unleash.isEnabled("new-availability-algorithm", UnleashContext.builder()
                .userId(request.getUserId())
                .build())) {
            return newAvailabilityService.check(request);
        }
        return legacyAvailabilityService.check(request);
    }
}
```

Unleash supports gradual rollout by user segment, user ID hash (10% of users), and environment.

### LaunchDarkly (SaaS Feature Flags)

```java
LDUser user = new LDUser.Builder(userId)
    .custom("loyaltyTier", "platinum")
    .build();

boolean showNewUI = ldClient.boolVariation("new-hotel-search-ui", user, false);
// false = default value if LaunchDarkly is unreachable
```

---

## 7. Helm — Package Manager for Kubernetes

A Helm **chart** is a collection of Kubernetes YAML templates with parameterized values.

**Chart structure:**

```
booking-service/
  Chart.yaml          # Chart metadata (name, version, appVersion)
  values.yaml         # Default parameter values
  templates/          # YAML templates using Go template syntax
    deployment.yaml
    service.yaml
    ingress.yaml
    configmap.yaml
    hpa.yaml          # HorizontalPodAutoscaler
    _helpers.tpl      # Reusable template functions (prefix for chart)
```

**`Chart.yaml`:**

```yaml
apiVersion: v2
name: booking-service
description: Marriott Booking Service Helm Chart
type: application
version: 2.1.0        # Chart version (bump this on chart changes)
appVersion: "1.5.0"   # App version (the Docker image tag)
```

**`values.yaml`:**

```yaml
replicaCount: 10

image:
  repository: marriott/booking-service
  tag: "1.5.0"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 2000m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 5
  maxReplicas: 50
  targetCPUUtilizationPercentage: 70

env:
  SPRING_PROFILES_ACTIVE: production
  REDIS_HOST: redis-cluster.marriott.internal
```

**`templates/deployment.yaml`:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "booking-service.fullname" . }}
  labels:
    {{- include "booking-service.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "booking-service.selectorLabels" . | nindent 6 }}
  template:
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - containerPort: 8080
        env:
        {{- range $key, $val := .Values.env }}
        - name: {{ $key }}
          value: {{ $val | quote }}
        {{- end }}
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
```

---

## 8. Key Helm Commands

```bash
# Package and install
helm install booking-service ./booking-service -n production

# Upgrade (deploy new image version)
helm upgrade booking-service ./booking-service \
  --set image.tag=1.5.1 \
  --namespace production

# Upgrade with custom values file (prod overrides)
helm upgrade booking-service ./booking-service \
  -f values-production.yaml \
  --namespace production

# Dry run — see what would be applied without applying
helm upgrade booking-service ./booking-service \
  --dry-run --debug

# View deployed release history
helm history booking-service -n production

# Rollback to previous release
helm rollback booking-service 2 -n production  # Roll back to revision 2

# Uninstall
helm uninstall booking-service -n production

# Template rendering (debug output)
helm template booking-service ./booking-service -f values-production.yaml
```

---

## 9. `kubectl rollout` Commands

```bash
# Check rollout status (blocks until complete or failed)
kubectl rollout status deployment/booking-service -n production

# View rollout history
kubectl rollout history deployment/booking-service -n production

# Rollback to previous version
kubectl rollout undo deployment/booking-service -n production

# Rollback to specific revision
kubectl rollout undo deployment/booking-service --to-revision=3 -n production

# Pause a rollout (e.g., suspicious metrics)
kubectl rollout pause deployment/booking-service -n production

# Resume a paused rollout
kubectl rollout resume deployment/booking-service -n production
```

---

## 10. Zero-Downtime Deployment Checklist

```
PRE-DEPLOY
  ✓ DB migration is backward-compatible (additive only — no column drops)
  ✓ API change is backward-compatible (new field optional, not required)
  ✓ Feature flag wraps breaking changes (off by default)
  ✓ PodDisruptionBudget (PDB) configured — min available replicas set

IN-DEPLOYMENT
  ✓ readinessProbe configured — traffic not sent until app reports ready
  ✓ livenessProbe configured — Kubernetes restarts unhealthy pods
  ✓ preStop hook configured — sleep 10s to drain LB connections before termination
  ✓ terminationGracePeriodSeconds ≥ longest request timeout (e.g., 60s)
  ✓ maxUnavailable: 1 (never more than 1 pod down at a time)

POST-DEPLOY
  ✓ Monitor error rate (< threshold)
  ✓ Monitor P99 latency (< SLO)
  ✓ Monitor business metric (booking conversion not degraded)
  ✓ Ready to rollback: `helm rollback` or `kubectl rollout undo` prepared
```

**PodDisruptionBudget:**

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: booking-service-pdb
spec:
  minAvailable: 8        # At least 8 of 10 pods must be running at all times
  selector:
    matchLabels:
      app: booking-service
```

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| No readiness probe | New pod receives traffic before Spring Boot finishes startup → 503 errors | Always configure readiness probe on `/actuator/health/readiness` |
| No preStop hook | Pod terminates mid-request → in-flight requests get connection reset | Add `preStop: sleep 10` to drain load balancer connections |
| Dropping DB columns in same deploy as app | Old pods still running can't find the column → crash | Separate deploy: first deploy app that doesn't use column; then drop column in next deploy |
| Helm upgrade without `--atomic` in CI | Failed upgrade leaves chart in partially upgraded state | Use `helm upgrade --atomic --timeout 5m` — rolls back automatically on failure |

---

## 12. Interview Insight

Strong answer:

> For zero-downtime deploys, I configure three things: readiness probe so Kubernetes only sends traffic to healthy pods; preStop hook to drain load balancer connections before termination; and terminationGracePeriodSeconds to allow in-flight requests to complete. Strategy-wise: rolling update is the default and works for most cases. Blue-green gives instant rollback by switching the Service selector — costs 2x compute during the window. Canary routes 5–10% of traffic to the new version and promotes based on error rate and latency SLOs. Helm manages all the Kubernetes YAML as a versioned, parameterized chart — `helm rollback` gives one-command rollback to any previous release.

Follow-up trap:

> What's the biggest risk of deploying a DB schema change alongside an application change?

Good answer:

> If you drop or rename a column in the same deployment, the old application pods (still running during rolling update) reference the old schema and crash. The fix is the expand-contract migration pattern: first deploy that adds the new column (additive, backward-compatible). Run both columns simultaneously. In a second deploy, migrate data. In a third deploy, remove the old column after all old pods are gone. This means DB migrations must always be additive in any single deploy — never destructive.

---

## 13. Revision Notes

- One-line summary: Rolling update replaces pods one at a time; blue-green switches traffic in one atomic step with instant rollback; canary routes a traffic percentage with metric-gated promotion; Helm packages all Kubernetes YAML as versioned, parameterized charts.
- Three keywords: readiness probe, blue-green switch, canary promotion.
- One interview trap: deploying a destructive DB change (DROP COLUMN) in the same release as the app change breaks rolling update — old pods crash.
- Memory trick: Blue-green = light switch (instant). Canary = dimmer (gradual). Rolling = one bulb at a time.
