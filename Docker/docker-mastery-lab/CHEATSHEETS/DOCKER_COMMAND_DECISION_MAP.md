# Docker Command Decision Map

## Build

```bash
docker build -t app:local .
docker history app:local
docker image inspect app:local
```

## Run And Inspect

```bash
docker run --rm -p 8080:8080 app:local
docker ps -a
docker logs CONTAINER
docker inspect CONTAINER
docker exec -it CONTAINER sh
```

## Network

```bash
docker network ls
docker network inspect NETWORK
docker port CONTAINER
```

## Storage

```bash
docker volume ls
docker volume inspect VOLUME
docker inspect CONTAINER --format '{{json .Mounts}}'
```

## Production Symptoms

| Symptom | First Commands |
|---|---|
| container exits | `docker ps -a`, `docker logs`, `docker inspect` |
| unreachable service | `docker port`, `docker inspect`, app listen address |
| data missing | mounts, volume inspect, cleanup history |
| high memory | `docker stats`, OOMKilled inspect field |
| wrong release | image tag, digest, registry, deploy metadata |