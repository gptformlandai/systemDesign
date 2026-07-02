# Docker Active Recall Question Bank

> Track File #27 of 40 - Group 06: Practice Upgrade
> For: spaced repetition | Level: beginner to pro | Mode: question bank

## 1. Beginner Recall

1. What problem does Docker solve?
2. What is the difference between image and container?
3. What is the Docker daemon?
4. What does `docker run` do internally?
5. Why is `.dockerignore` important?
6. What is build context?
7. What does `EXPOSE` do and not do?
8. What is the difference between `CMD` and `ENTRYPOINT`?

## 2. Intermediate Recall

1. How do Docker layers affect rebuild speed?
2. Why do we copy dependency lock files before source files?
3. What is a multi-stage build?
4. When should you use a named volume vs bind mount?
5. How does Docker container DNS work on a user-defined bridge network?
6. What does `depends_on` guarantee in Compose?
7. What is the difference between tag and digest?
8. Why should applications log to stdout/stderr?

## 3. Senior Recall

1. How do cgroups affect Docker memory and CPU behavior?
2. How do you debug an OOMKilled container?
3. Why is rootless Docker useful, and what are its tradeoffs?
4. What are Linux capabilities?
5. Why is `--privileged` risky?
6. How do you design secure image promotion through CI/CD?
7. Why should production deploy by digest or immutable tag?
8. What evidence do you gather during a Docker production incident?
9. How do Compose profiles change which services run?
10. What is Docker Compose env precedence?
11. Why should you check Docker context before debugging?
12. What belongs in `daemon.json` and what should not?
13. Why is unauthenticated remote Docker API access dangerous?
14. How do seccomp, AppArmor, and SELinux reduce blast radius?
15. What problem does user namespace remapping solve?
16. What is the difference between SBOM, provenance, signature, and digest?
17. How do Buildx builders and drivers affect CI builds?
18. When would you use BuildKit secret, cache, and SSH mounts?
19. What network driver would you choose for local single-host service discovery?
20. How do overlay2 writable layers differ from named volumes?
21. When should you use tmpfs with a read-only root filesystem?
22. Why can Docker Desktop behave differently from Linux Docker Engine?
23. How do containerd, runc, CRI-O, Podman, and Docker relate?

## 4. Scenario Recall

1. A container exits immediately. What are your first five commands?
2. A containerized app is unreachable from host. What layers do you inspect?
3. Data vanished after Compose cleanup. What likely happened?
4. Builds are slow after every code change. What Dockerfile smell do you suspect?
5. A production tag points to a different image than yesterday. What process failed?
6. Health checks are flapping. What do you inspect?
7. A non-root container cannot write to a bind mount. What do you check?
8. A slim image has no shell. How do you debug differently?
9. `docker compose up` starts debug tools unexpectedly. What do you change?
10. The same Compose file uses different env values on two machines. How do you prove the final config?
11. CI builds are slow and fill disk. How do you design BuildKit cache and prune policy?
12. An arm64 image fails but amd64 works. What build and runtime evidence do you gather?
13. A registry tag changed after scan. What pipeline control failed?
14. A container cannot reach a service by name. Which DNS/network boundaries do you inspect?
15. A read-only container crashes on startup. Which write paths do you identify?
16. A developer Mac has slow Docker file IO. What platform boundary explains it?

## 5. Answer Pattern

Use this structure for every senior answer:

```text
Docker object -> Linux/runtime behavior -> evidence command -> likely failure -> fix -> prevention
```

For pro-level Docker answers, add this layer:

```text
context/daemon -> build identity -> runtime limits -> network/storage boundary -> supply-chain proof -> rollback digest
```
