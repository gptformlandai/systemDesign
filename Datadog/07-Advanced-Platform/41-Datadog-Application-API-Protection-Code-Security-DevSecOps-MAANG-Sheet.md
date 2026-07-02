# 41. Datadog Application/API Protection, Code Security, DevSecOps

## Goal

Understand Datadog's application security depth beyond CSPM and Cloud SIEM: API protection, attack detection, vulnerability management, code security, and DevSecOps workflows.

---

## Mental Model

Cloud security asks:

```text
Is my cloud configured safely?
```

Application security asks:

```text
Is my running application being attacked or exposing vulnerable code/API behavior?
```

Code security asks:

```text
Did we introduce insecure code, dependencies, secrets, or IaC before production?
```

---

## Product Areas

| Area | Purpose |
|---|---|
| Application/API Protection | Detect and block attacks against running apps and APIs |
| API Security/Posture | Discover APIs, sensitive endpoints, auth gaps, schema drift |
| Vulnerability Management | Find vulnerable libraries in running services |
| Code Security | SAST, SCA, IaC, secret scanning, SBOM |
| Cloud SIEM | Detect threats from logs/events |
| CSPM/CWS | Cloud posture and workload runtime security |

---

## Application/API Protection Flow

```text
HTTP request
  -> application tracer/security module
  -> attack detection rules inspect request context
  -> signal generated or request blocked
  -> trace/log/security signal linked in Datadog
```

Common detections:

```text
SQL injection
command injection
SSRF
path traversal
XSS
credential stuffing
account takeover behavior
API abuse
scanner activity
```

---

## API Security Questions

| Question | Why It Matters |
|---|---|
| Which APIs exist? | Shadow APIs increase attack surface |
| Which endpoints expose sensitive data? | Prioritize protection and review |
| Which endpoints lack authentication? | Prevent accidental exposure |
| Which APIs changed recently? | Detect breaking/security drift |
| Which clients call this endpoint? | Identify abuse and blast radius |

---

## Code Security Coverage

| Scan Type | Finds |
|---|---|
| SAST | insecure code patterns |
| SCA | vulnerable open-source dependencies |
| IAST | runtime code vulnerabilities during tests |
| IaC scanning | insecure Terraform/Kubernetes/cloud config |
| Secret scanning | keys/tokens/passwords in source |
| SBOM | inventory of software components |

---

## DevSecOps Workflow

```text
1. Developer opens pull request.
2. Code Security scans code, dependencies, IaC, and secrets.
3. Critical findings block merge or require approval.
4. CI Visibility links finding to repo/commit/pipeline.
5. Runtime Vulnerability Management checks running services.
6. Software Catalog maps vulnerable service to owner.
7. Jira/ServiceNow/GitHub issue routes remediation.
8. Security dashboard tracks SLA by team and severity.
```

---

## Runtime Vulnerability Prioritization

Not every CVE deserves the same urgency.

Prioritize by:

```text
severity
exploit availability
internet exposure
service criticality
whether vulnerable function is actually loaded/called
data sensitivity
owner/team
SLO/customer impact
```

Runtime context helps avoid endless low-value CVE queues.

---

## Security Monitor Examples

### Credential Stuffing

```text
Alert:
  many failed logins
  grouped by client IP / account / user agent
  followed by successful login
```

### API Abuse

```text
Alert:
  endpoint:/api/v1/export
  request rate 10x baseline
  from one client or region
```

### Critical Runtime CVE

```text
Alert:
  critical vulnerability
  service tier:critical
  internet_exposed:true
  owner exists
```

---

## API Protection vs WAF

| Layer | Strength |
|---|---|
| WAF | perimeter request filtering |
| App/API Protection | runtime context inside instrumented app |
| SIEM | cross-signal threat correlation |
| Code Security | pre-production prevention |

These are complementary. A WAF can block obvious bad traffic; runtime app security can see application context.

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Treating CSPM as all security | Misses app/API attacks | Add AAP and Code Security |
| Alerting on every CVE | Teams ignore security | Prioritize by runtime risk |
| No service ownership | Findings become orphaned | Link to Software Catalog |
| Blocking too aggressively | False positives break users | Start detect-only, tune, then block |
| No CI integration | Vulnerabilities reach prod | Shift left with PR scans |

---

## Practical Question

> Security reports many vulnerable libraries and suspicious API traffic. Engineering says the list is too noisy. How do you design Datadog security operations?

---

## Strong Answer

I would separate prevention, runtime detection, and response. Code Security should scan PRs for SAST, SCA, IaC, and secrets before deployment. Runtime Vulnerability Management should prioritize vulnerabilities that are actually running, internet-exposed, exploited, or attached to critical services. Application/API Protection should detect attacks such as injection, path traversal, credential stuffing, and API abuse using application context.

All findings should be tagged with service/team/env and linked to Software Catalog ownership. Critical runtime risks page teams or create tickets with SLA; low-risk findings go into backlog. For blocking, I would begin in detect mode, tune false positives, then enable blocking for high-confidence rules.

---

## Interview Sound Bite

Datadog security is strongest when code, runtime, API, cloud, and SIEM signals share the same service ownership model. Do not drown teams in raw CVEs; prioritize by exploitability, exposure, service criticality, and runtime context.
