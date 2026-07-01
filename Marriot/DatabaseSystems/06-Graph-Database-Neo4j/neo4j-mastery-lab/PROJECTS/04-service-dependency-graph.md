# Project 04: Service Dependency Graph

Goal: model service dependencies and outage blast radius.

---

## Requirements

- store services, databases, queues, endpoints, teams
- traverse dependency paths
- identify impacted services
- keep topology fresh from deployment/platform events

---

## Graph Model

```text
(:Service)-[:DEPENDS_ON]->(:Database)
(:Service)-[:CALLS]->(:Endpoint)
(:Service)-[:PUBLISHES_TO]->(:Queue)
(:Team)-[:OWNS]->(:Service)
```

---

## Interview Talking Points

- dependency direction
- max traversal depth
- stale topology detection
- incident response and ownership lookup