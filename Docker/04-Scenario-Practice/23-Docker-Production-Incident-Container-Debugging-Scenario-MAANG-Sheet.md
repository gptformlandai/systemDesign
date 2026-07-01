# Docker Production Incident Container Debugging Scenario - MAANG Sheet

> Track File #23 of 30 - Group 04: Scenario Practice
> For: on-call and SRE interviews | Level: senior | Mode: production incident response

## 1. Scenario

```text
An API running in containers has high errors and some containers are restarting.
```

Goal: mitigate safely while preserving enough evidence for RCA.

---

## 2. Incident Flow

```text
scope -> affected containers -> logs -> inspect -> events -> stats -> host evidence -> mitigate -> validate -> RCA
```

Commands:

```bash
docker ps -a
docker logs CONTAINER --tail 200
docker inspect CONTAINER
docker stats --no-stream
docker events --since 30m
docker inspect CONTAINER --format '{{.State.OOMKilled}} {{.State.ExitCode}}'
```

---

## 3. Scope Questions

- one container or many?
- one image tag/digest or multiple?
- recent deployment or config change?
- OOMKilled, health check failing, or app error?
- host resource pressure or container limit?
- network/volume/registry dependency involved?

---

## 4. Mitigations

- rollback to previous digest
- scale healthy instances
- raise limit only if evidence supports it
- remove bad host from rotation
- restore env/config/secret
- fix volume/permission/network issue

---

## 5. Interview Summary

```text
For Docker production incidents, I scope affected containers and image digests, gather logs, inspect output, stats, events, OOM/exit status, and host evidence, then mitigate with rollback, scaling, config restore, or resource tuning. I validate recovery and write prevention items.
```

---

## 6. Revision Notes

- One-line summary: Docker incident response combines container evidence with host/runtime evidence.
- Three keywords: digest, OOM, rollback.
- One trap: restarting containers repeatedly without checking exit status, OOM, events, or recent image changes.