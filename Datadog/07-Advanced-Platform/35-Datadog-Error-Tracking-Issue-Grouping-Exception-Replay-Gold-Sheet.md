# 35. Datadog Error Tracking: Issue Grouping, Impact, Suspected Commits

## Goal

Understand Error Tracking as the bridge between raw exceptions and actionable engineering issues.

---

## Mental Model

Logs show every error event.

Error Tracking groups repeated errors into issues.

```text
10,000 stack traces
  -> 27 grouped issues
      -> 3 new issues after latest deploy
          -> 1 issue affects checkout users
```

The goal is triage, ownership, and prioritization.

---

## Why It Exists

Raw error logs are too noisy:

- The same exception appears thousands of times.
- Stack traces vary slightly.
- Engineers cannot tell which errors are new.
- It is hard to connect errors to deploys.
- Impact is unclear: one user or thousands?

Error Tracking turns error noise into issue workflow.

---

## Sources

Error Tracking can group errors from:

| Source | Example |
|---|---|
| APM traces | backend exception in Java, Python, Node |
| Logs | structured application error logs |
| Browser RUM | frontend JavaScript errors |
| Mobile RUM | iOS/Android crashes and errors |
| Session Replay | user action context near error |

---

## What An Error Issue Contains

| Field | Purpose |
|---|---|
| Error message | Human-readable failure |
| Stack trace | Code location |
| First seen | When the issue appeared |
| Last seen | Whether it is still active |
| Impact | Users/sessions/requests affected |
| Suspected commit | Code change likely introduced issue |
| Service/env/version | Routing and regression context |
| Status | New, ongoing, resolved, ignored |
| Owner | Team responsible |

---

## Grouping Logic

Datadog groups similar errors using attributes such as:

```text
exception type
exception message pattern
stack trace frames
service/env/version
source
```

Example:

```text
java.lang.NullPointerException: Cannot invoke "Payment.getId()"
  at CheckoutService.authorizePayment(CheckoutService.java:88)
```

All occurrences with the same meaningful fingerprint become one issue.

---

## Triage Workflow

```text
1. Open Error Tracking.
2. Filter by env:production and service:checkout-service.
3. Sort by new issues or impacted users.
4. Open issue.
5. Check first seen time and deployment version.
6. Review stack trace and suspected commit.
7. Pivot to trace, log, session replay, or RUM view.
8. Assign owner and create ticket.
9. Monitor issue volume until resolved.
```

---

## Monitor Patterns

### New Production Issue

```text
Alert when:
  New error issue appears in env:production
  AND service is tier:critical
```

### High Impact Issue

```text
Alert when:
  impacted users > 100 in 15 minutes
  OR error occurrences > 500 in 10 minutes
```

### Regression After Deploy

```text
Alert when:
  issue first_seen is after latest deployment
  AND version equals newly deployed version
```

---

## Backend Example

```text
Issue:
  NullPointerException in CheckoutService.authorizePayment

Context:
  service:checkout-service
  env:production
  version:2.4.1
  first_seen: 12 minutes after deployment
  impacted_requests: 3,842
  suspected_commit: "Refactor payment method selection"

Action:
  Roll back version 2.4.1 or hotfix null handling.
```

---

## Frontend Example

```text
Issue:
  TypeError: cannot read property 'price' of undefined

Context:
  browser: Chrome 126
  page: /cart
  impacted_sessions: 1,240
  session replay: user opens cart after removing final item

Action:
  Fix empty cart rendering condition.
```

---

## Error Tracking vs Log Explorer

| Need | Use |
|---|---|
| Find every raw occurrence | Log Explorer |
| Group repeated stack traces | Error Tracking |
| Prioritize by impacted users | Error Tracking |
| Inspect full request context | Trace Explorer |
| See user actions before browser error | RUM / Session Replay |

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Alerting on every exception log | Alert fatigue | Alert on new/high-impact issues |
| No version tags | Cannot link to deploy | Always set `DD_VERSION` |
| Ignoring frontend errors | Users experience breakage silently | Enable RUM Error Tracking |
| Closing issue without monitor | Regression returns unnoticed | Keep monitor or issue workflow |
| Treating ignored issue as fixed | Risk hidden | Use ignore only for accepted noise |

---

## Practical Question

> After a deploy, logs show 50,000 exceptions across multiple services. How do you avoid drowning in noise?

---

## Strong Answer

I would use Error Tracking to group repeated exceptions into issues, then filter to production and sort by newness and user/request impact. I would look for issues first seen after the latest deployment and compare by service/env/version. For the highest-impact new issue, I would inspect stack trace, suspected commit, trace context, logs, and RUM/session replay if it is user-facing.

The monitor should alert on new production issues or high-impact issue volume, not every raw error log. This keeps the team focused on distinct regressions and customer impact.

---

## Interview Sound Bite

Error Tracking turns repeated exceptions into prioritized issues. Logs tell me every event; Error Tracking tells me which unique failure matters, when it started, who it impacts, and which deploy likely caused it.
