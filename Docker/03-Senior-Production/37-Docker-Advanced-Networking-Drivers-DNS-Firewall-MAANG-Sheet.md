# Docker Advanced Networking: Drivers, DNS, Firewall - MAANG Sheet

> Track File #37 of 40 - Group 03: Senior Production
> For: senior debugging, platform, and system design interviews | Level: senior | Mode: container networking

## 1. Intuition

Docker networking is a set of virtual cables, name resolution rules, NAT/firewall rules, and driver choices.

Beginner networking asks "which port do I expose?" Senior networking asks:

```text
Which namespace, route, DNS name, driver, firewall rule, and host boundary owns this packet?
```

---

## 2. Definition

- Definition: Docker networking connects containers to each other, the host, external networks, and sometimes other hosts using network drivers and Linux networking primitives.
- Category: container runtime networking.
- Core idea: container network behavior depends on namespace, driver, DNS scope, port publishing, and host firewall/NAT configuration.

---

## 3. Why It Exists

Containers need isolated application networking while still reaching dependencies and exposing services.

Without Docker networking:

- every container would share host ports
- service discovery would be manual
- local multi-service systems would be brittle
- production debugging would confuse host, container, and overlay paths

---

## 4. Driver Map

| Driver | Mental Model | Strong Fit |
|---|---|---|
| bridge | private network on one host with NAT | local/default single-host apps |
| host | container shares host network namespace | high-performance or special host networking needs |
| none | no network interface except loopback | batch, sandboxed, or custom network setup |
| overlay | multi-host virtual network | Swarm or supported multi-host setups |
| macvlan | container appears as physical-network peer with MAC | legacy networks needing direct L2 presence |
| ipvlan | direct network integration with less MAC pressure | advanced L2/L3 enterprise networking |

Default answer:

```text
Use user-defined bridge for local single-host service discovery. Move to orchestrator networking for production multi-host systems.
```

---

## 5. Packet Path: Published Port

```text
client -> host IP:published-port -> Docker NAT/proxy/firewall rules -> container IP:target-port -> app process
```

Common mistake:

```text
EXPOSE 8080 does not publish 8080 to the host. It documents/listens metadata. `-p` or Compose `ports` publishes.
```

---

## 6. DNS Rules

| Context | Name That Works |
|---|---|
| same user-defined bridge network | service/container name |
| default bridge without links | not reliable service DNS |
| host to container | published port on host |
| container to host on Docker Desktop | `host.docker.internal` |
| Linux container to host | often `host-gateway` mapping or explicit host IP |
| cross Compose projects | only if attached to shared external network |

Compose mental model:

```text
Inside Compose: use service name and container port.
Outside Compose: use host name/IP and published port.
```

---

## 7. Command Map

```bash
docker network ls
docker network inspect NETWORK
docker network create app-net
docker run --rm --network app-net alpine ip addr
docker run --rm --network app-net alpine nslookup api
docker port CONTAINER
docker inspect CONTAINER --format '{{json .NetworkSettings.Networks}}'
```

Linux host checks:

```bash
ip addr
ip route
ss -lntp
iptables -S
nft list ruleset
```

Use host-level commands carefully in production; firewall and NAT rules may be managed by platform automation.

---

## 8. Compose Network Example

```yaml
services:
  api:
    build: .
    ports:
      - "8080:8080"
    networks:
      - frontend
      - backend

  worker:
    build: .
    command: ./worker
    networks:
      - backend

  db:
    image: postgres:16
    networks:
      - backend

networks:
  frontend:
  backend:
    internal: true
```

Only `api` is reachable from host via published port. `db` is isolated on the backend network.

---

## 9. Advanced Debugging Flow

1. Confirm app listens inside container.
2. Confirm container has expected network.
3. Confirm DNS name resolves inside same network.
4. Confirm target container port is correct.
5. Confirm host published port exists.
6. Confirm host firewall/NAT allows path.
7. Confirm proxy/TLS/load balancer boundary.
8. Confirm Desktop/VM boundary if on macOS/Windows.

Commands:

```bash
docker exec api ss -lntp
docker exec api getent hosts db
docker exec api wget -qO- http://db:5432 || true
docker inspect api
docker compose ps
```

---

## 10. MTU, Proxy, IPv6, Firewall

Senior topics that appear in real incidents:

- MTU mismatch can cause hanging large requests, TLS failures, or packet fragmentation trouble.
- Corporate proxies may affect pulls, builds, and runtime egress differently.
- IPv6 must be enabled and routed intentionally; dual-stack behavior can surprise DNS clients.
- Docker manipulates firewall/NAT rules for bridge networking and published ports.
- Some hosts use iptables, some use nftables, and enterprise firewall agents may interfere.

Interview posture:

```text
I debug networking by proving each boundary: process, namespace, Docker network, host NAT/firewall, and external route.
```

---

## 11. When Not To Use Certain Drivers

| Driver | Avoid When |
|---|---|
| host | you need port isolation or strong namespace isolation |
| macvlan | your network blocks multiple MACs or team cannot operate L2 issues |
| overlay | you are not using a supported multi-host orchestrator model |
| none | app requires normal dependency access |
| default bridge | you need clean service discovery and isolation |

---

## 12. Failure Modes

| Symptom | Likely Cause | Fix |
|---|---|---|
| host cannot reach service | no published port or app binds 127.0.0.1 inside container | bind `0.0.0.0`, publish port |
| container cannot reach `localhost` DB | localhost points to same container | use service name or host gateway |
| service DNS fails | containers not on same user-defined network | attach network or Compose service |
| works on Linux not Mac | Desktop VM/host boundary | use Desktop-supported host name or published ports |
| random TLS hangs | MTU/proxy/firewall issue | test packet size, route, proxy |
| port already allocated | host port collision | change host port or project name |

---

## 13. Scenario

- Product / system: API, worker, Redis, and Postgres running locally and in CI.
- Why advanced networking fits: API should publish one port; internal data services should be private; CI needs deterministic DNS.
- What would go wrong without it: insecure published databases, broken localhost assumptions, and flaky tests.

---

## 14. Strong Answer

I would use user-defined bridge networks for local and CI service discovery, keep internal services on private backend networks, publish only edge services, and use service names inside Compose. For debugging I would verify the app listen address, container network membership, DNS resolution, published ports, and host firewall/NAT. For multi-host production I would not stretch Compose bridge networking; I would use orchestrator networking through Kubernetes, ECS, Nomad, or Swarm where appropriate.

---

## 15. Revision Notes

- One-line summary: Docker networking is namespace plus driver plus DNS plus published ports plus host firewall behavior.
- Three keywords: driver, DNS, NAT.
- One interview trap: using `localhost` from one container to reach another.
- One memory trick: trace packet boundaries in order.
