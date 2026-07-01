# Docker Commands Cheat Sheet and Decision Map

> Track File #25 of 30 - Group 05: Special Interview Rounds
> For: daily recall | Level: beginner to pro | Mode: command map

## 1. Environment

```bash
docker version
docker info
docker context ls
docker system df
```

## 2. Images

```bash
docker images
docker pull IMAGE
docker build -t app:local .
docker history IMAGE
docker image inspect IMAGE
docker rmi IMAGE
```

## 3. Containers

```bash
docker ps
docker ps -a
docker run --rm IMAGE command
docker stop CONTAINER
docker rm CONTAINER
docker exec -it CONTAINER sh
docker logs CONTAINER
docker inspect CONTAINER
```

## 4. Networks

```bash
docker network ls
docker network inspect NETWORK
docker port CONTAINER
docker exec CONTAINER ss -ltnp
```

## 5. Volumes

```bash
docker volume ls
docker volume inspect VOLUME
docker inspect CONTAINER --format '{{json .Mounts}}'
```

## 6. Compose

```bash
docker compose config
docker compose up -d
docker compose ps
docker compose logs -f
docker compose down
```

## 7. Decision Map

| Symptom | Start With |
|---|---|
| build fails | `docker build`, context, Dockerfile, `.dockerignore` |
| container exits | `docker ps -a`, `docker logs`, `docker inspect` |
| cannot connect | `docker ps`, `docker port`, app listen address, network inspect |
| data missing | mounts, volume inspect, bind path, ownership |
| high resource use | `docker stats`, inspect limits, host metrics |
| wrong image deployed | tag/digest, registry, CI/CD metadata |
| permission denied | container user, mount owner, file mode, capabilities |