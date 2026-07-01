# Linux Production Incident and On-Call Scenario - MAANG Sheet

> Track File #22 of 30 - Group 04: Scenario Practice
> For: SRE/on-call interviews | Level: senior | Mode: incident response

## 1. Scenario

```text
An alert fires: API latency p99 is high on a Linux service fleet.
```

Goal: reduce impact while collecting enough evidence to avoid blind changes.

---

## 2. Incident Flow

```text
acknowledge -> scope -> impact -> evidence -> mitigation -> validation -> communication -> RCA
```

Commands:

```bash
systemctl status app
journalctl -u app --since "30 minutes ago"
top
free -h
df -h
ss -s
ss -ltnp
dmesg -T | tail -100
```

---

## 3. Scope Questions

- One host or many?
- One service or dependency?
- One region or global?
- Recent deploy or config change?
- Error rate, latency, saturation, or traffic spike?
- Is there a safe rollback?

---

## 4. Safe Mitigations

- rollback recent deploy
- shift traffic away from bad hosts
- restart only confirmed unhealthy process
- increase capacity if resource saturation is clear
- apply rate limits if overload is external
- disable expensive feature flag if safe

---

## 5. RCA Notes

Capture:

- timeline
- detection gap
- customer impact
- root cause
- contributing factors
- what worked
- what failed
- prevention action items

---

## 6. Interview Summary

```text
In a Linux production incident, I first scope impact and gather evidence from service state, logs, resources, kernel messages, network state, and recent changes. I mitigate safely with rollback, traffic shift, capacity, restart, or rate limiting, then validate recovery and write RCA prevention items.
```

---

## 7. Revision Notes

- One-line summary: On-call Linux work balances fast mitigation with evidence.
- Three keywords: scope, mitigate, validate.
- One trap: changing many things at once and losing the causal trail.