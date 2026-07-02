# 41. Safe Debugging, Incident Response, RCA, Secrets, PII

## Goal

Debug under production pressure without creating a bigger incident: protect users, secrets, PII, availability, evidence, and the incident timeline.

---

## Core Principle

Debugging actions are production changes.

```text
turning on debug logs
taking a heap dump
attaching a debugger
running tcpdump
redriving DLQ messages
restarting pods
changing log level
```

All can change system behavior or expose sensitive data.

---

## Safety Checklist Before Debugging

Ask:

- Could this pause or slow production?
- Could this expose secrets or PII?
- Could this delete evidence?
- Could this restart a process?
- Could this duplicate customer actions?
- Is this action reversible?
- Is the owner/on-call aware?
- Is the action recorded in the incident timeline?

---

## Sensitive Debug Artifacts

Treat these as restricted:

| Artifact | Risk |
|---|---|
| heap dump | passwords, tokens, PII, payloads |
| core dump | memory contents and secrets |
| thread dump | URLs, SQL, request details |
| logs | PII, auth headers, business data |
| packet capture | credentials, payloads, cookies |
| browser HAR | cookies, tokens, user data |
| diagnostic report | env vars, process args, paths |

Store with access controls, expiry, and audit trail.

---

## Redaction Rules

Never log or share raw:

```text
password
authorization header
cookie
session token
refresh token
API key
private key
credit card
SSN / national ID
full email when not needed
medical/regulated data
```

Prefer:

```text
hash
last four characters
token type only
tenant ID with approval
synthetic reproduction payload
```

---

## Incident Debugging Workflow

```text
1. Declare incident if user impact is active.
2. Assign incident commander.
3. Record timeline from the start.
4. Preserve evidence before restarting.
5. Mitigate customer impact.
6. Continue root cause investigation after stabilization.
7. Verify recovery with user-facing metrics.
8. Write RCA/postmortem.
9. Add prevention: tests, monitors, runbooks, guards.
```

Incident rule:

```text
Mitigation first when impact is active.
Root cause depth after users are safe.
```

---

## Evidence Collection Order

Before restart/rollback if time allows:

```text
1. current deployment/version/config
2. logs around incident start
3. traces or request IDs
4. metrics dashboard snapshot
5. thread dump / task dump if hung
6. heap/core dump only if necessary and safe
7. events: deploys, autoscaling, config, dependency alerts
```

Do not spend 30 minutes collecting perfect evidence during a major outage if rollback is obvious and safe.

---

## RCA Template

```text
Title:
Date/time:
Severity:
Duration:
Customer impact:
Services affected:

Timeline:
  T0 detection
  T1 triage
  T2 mitigation
  T3 recovery

Root cause:
Contributing factors:
Detection gap:
What worked:
What failed:

Action items:
  owner:
  due date:
  prevention:
  verification:
```

---

## Debugging With Feature Flags

Feature flags help mitigation:

- disable new code path
- reduce rollout percentage
- disable expensive feature
- isolate tenant/region

Debug questions:

- Which users had the flag?
- Did server and client agree on flag state?
- Was the flag cached?
- Was there a stale default?
- Did a flag change align with incident start?

---

## Rollback vs Fix Forward

| Choose Rollback When | Choose Fix Forward When |
|---|---|
| recent deploy clearly correlated | rollback unsafe or impossible |
| database schema still compatible | data already migrated irreversibly |
| customer impact active | tiny config/code patch is faster |
| rollback tested | old version has worse bug |

Senior habit: know rollback safety before you deploy.

---

## Break-Glass Debugging

For risky debug access:

```text
approval
time-box
least privilege
audit logging
explicit scope
artifact handling plan
rollback plan
post-action review
```

Examples:

- temporary production shell
- elevated database query access
- packet capture
- heap dump download
- debug port attach in isolated environment

---

## Practical Question

> During a SEV-1, an engineer wants to capture a heap dump from production because memory is high. What do you consider?

---

## Strong Answer

I would first ask whether the heap dump is necessary for mitigation or whether we should roll back/restart to restore service. Heap dumps can be large, slow, and sensitive because they may contain tokens, PII, and payload data. If we need the dump, I would capture it in a controlled way, restrict access, record the action in the incident timeline, store it securely, and delete it after analysis according to policy.

If impact is active and restart is the fastest safe mitigation, I would collect lighter evidence first, such as metrics, logs, thread dump, and deployment metadata, then restart or roll back. Root cause analysis can continue after customers recover.

---

## Interview Sound Bite

Production debugging must be safe, auditable, and reversible. Collect enough evidence to learn, but do not let investigation prolong user impact. Dumps, packet captures, debug ports, and verbose logs are powerful but sensitive tools.
