# Docker Networking, Ports, DNS, and Bridge Networks - Gold Sheet

> Track File #8 of 30 - Group 02: Intermediate Practical
> For: local and production debugging | Level: intermediate | Mode: container networking

## 1. Core Idea

Docker containers have their own network namespace. Published ports connect host traffic to container ports.

```text
host port -> Docker NAT/proxy -> container IP:container port -> app process
```

---

## 2. Key Concepts

| Concept | Meaning |
|---|---|
| bridge network | default isolated network for containers on one host |
| published port | host port mapped to container port with `-p` |
| container DNS | service/container names resolve on user-defined networks |
| localhost trap | `localhost` inside container means container itself, not host |
| host network | container shares host network namespace on Linux |

---

## 3. Commands

```bash
docker network ls
docker network inspect bridge
docker run --rm -p 8080:80 nginx:alpine
docker port CONTAINER
docker exec CONTAINER ss -ltnp
docker inspect CONTAINER --format '{{json .NetworkSettings.Networks}}'
```

---

## 4. Debug Flow

```text
client URL -> host port published -> container port listening -> app binds 0.0.0.0 -> network/DNS rules
```

Checks:

```bash
docker ps
docker logs CONTAINER
curl -v http://localhost:8080
docker exec CONTAINER curl -v http://localhost:PORT
```

---

## 5. Failure Modes

- app listens on `127.0.0.1` inside container and is unreachable externally
- wrong host port mapped to wrong container port
- containers on different networks cannot resolve each other
- using host `localhost` from inside container
- firewall/security group blocks host port

---

## 6. Interview Summary

```text
Docker networking depends on container network namespaces, bridge networks, port publishing, and DNS on user-defined networks. I debug by checking published ports, app listen address, container DNS, network membership, and whether localhost refers to host or container.
```

---

## 7. Revision Notes

- One-line summary: Published ports bridge host traffic into a container network namespace.
- Three keywords: bridge, port, DNS.
- One trap: using `localhost` inside a container when you mean another container or the host.