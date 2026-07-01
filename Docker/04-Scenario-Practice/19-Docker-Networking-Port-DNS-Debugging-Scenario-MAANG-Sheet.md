# Docker Networking, Port, and DNS Debugging Scenario - MAANG Sheet

> Track File #19 of 30 - Group 04: Scenario Practice
> For: container networking interviews | Level: senior | Mode: network failure

## 1. Scenario

```text
The app runs in Docker, but the host or another container cannot reach it.
```

Goal: separate host port mapping, container listen address, Docker network, DNS, and app behavior.

---

## 2. Debug Flow

```text
host request -> published port -> container port -> app listen address -> Docker network -> DNS/service name
```

Commands:

```bash
docker ps
docker port CONTAINER
docker inspect CONTAINER --format '{{json .NetworkSettings.Networks}}'
docker exec CONTAINER ss -ltnp
docker logs CONTAINER
curl -v http://localhost:HOST_PORT
```

---

## 3. Container-To-Container Checks

On user-defined networks, containers can resolve service/container names.

```bash
docker network ls
docker network inspect NETWORK
docker exec CONTAINER getent hosts OTHER_SERVICE
docker exec CONTAINER curl -v http://OTHER_SERVICE:PORT
```

---

## 4. Common Causes

- no `-p host:container` mapping
- wrong host port or container port
- app listens on `127.0.0.1` inside container
- containers not on same network
- using `localhost` when service name is needed
- host firewall blocks published port

---

## 5. Interview Summary

```text
For Docker networking issues, I check published ports, container listen address, logs, Docker network membership, container DNS, service names, and host firewall. I always clarify whether traffic is host-to-container, container-to-container, or container-to-host.
```

---

## 6. Revision Notes

- One-line summary: Docker network debugging starts by naming the traffic direction.
- Three keywords: port, bridge, DNS.
- One trap: assuming `localhost` means the same thing on host and inside container.