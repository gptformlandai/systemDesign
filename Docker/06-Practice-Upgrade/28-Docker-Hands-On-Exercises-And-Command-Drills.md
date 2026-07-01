# Docker Hands-On Exercises and Command Drills

> Track File #28 of 30 - Group 06: Practice Upgrade
> For: command fluency | Level: beginner to pro | Mode: hands-on drills

## 1. Drill Rules

- type commands manually
- explain what Docker object each command touches
- inspect before deleting
- avoid destructive volume cleanup unless the lab says so
- write a one-line lesson after each drill

---

## 2. Foundation Drills

```bash
docker version
docker info
docker pull nginx:alpine
docker run --rm -p 8080:80 nginx:alpine
docker ps
docker logs CONTAINER
docker stop CONTAINER
```

Explain:

- image pulled
- container started
- port published
- logs streamed

---

## 3. Build Drills

1. Create a small app Dockerfile.
2. Add `.dockerignore`.
3. Build with a local tag.
4. Change only source and rebuild.
5. Change dependency file and rebuild.
6. Compare cache behavior.

Commands:

```bash
docker build -t drill-app:v1 .
docker history drill-app:v1
docker image inspect drill-app:v1
```

---

## 4. Runtime Debug Drills

```bash
docker ps -a
docker inspect CONTAINER
docker logs CONTAINER --tail 50
docker exec -it CONTAINER sh
docker stats --no-stream
```

Practice questions:

- what command is the container running?
- what user is it running as?
- what env vars are present?
- what networks and mounts are attached?

---

## 5. Networking Drills

```bash
docker network create appnet
docker run -d --name web --network appnet nginx:alpine
docker run --rm --network appnet alpine getent hosts web
docker network inspect appnet
docker rm -f web
docker network rm appnet
```

---

## 6. Volume Drills

```bash
docker volume create appdata
docker run --rm -v appdata:/data alpine sh -c 'date > /data/created.txt'
docker run --rm -v appdata:/data alpine cat /data/created.txt
docker volume inspect appdata
```

Cleanup only after confirming contents:

```bash
docker volume rm appdata
```

---

## 7. Senior Drill

Simulate an incident:

1. Start a container with a bad env var.
2. Observe exit code and logs.
3. Fix env var.
4. Restart with memory limit.
5. Inspect limits and health.
6. Write the RCA in five bullets.