# Lab 04: Docker Networking, DNS, And Ports

## Goal

Understand host-to-container and container-to-container traffic.

## Commands

```bash
docker network create docker-mastery-net
docker run -d --name web-a --network docker-mastery-net nginx:alpine
docker run --rm --network docker-mastery-net alpine getent hosts web-a
docker run --rm --network docker-mastery-net alpine wget -qO- http://web-a
docker network inspect docker-mastery-net
docker rm -f web-a
docker network rm docker-mastery-net
```

## Host Port Drill

```bash
docker run -d --name web-port -p 8080:80 nginx:alpine
docker port web-port
curl http://localhost:8080
docker rm -f web-port
```

## Observe

- DNS works by container name on a user-defined network
- host port publishing is separate from container-to-container DNS
- `localhost` depends on where the command runs

## Interview Takeaway

```text
Docker networking answers should first identify traffic direction: host-to-container, container-to-container, or container-to-host. Then check ports, network membership, service names, DNS, and app listen address.
```