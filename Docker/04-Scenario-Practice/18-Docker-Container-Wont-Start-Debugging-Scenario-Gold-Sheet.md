# Docker Container Won't Start Debugging Scenario - Gold Sheet

> Track File #18 of 40 - Group 04: Scenario Practice
> For: Docker troubleshooting | Level: intermediate | Mode: startup failure

## 1. Scenario

```text
A container exits immediately or keeps restarting.
```

Goal: prove whether the issue is command, entrypoint, env, file permission, dependency, missing file, or resource limit.

---

## 2. Debug Flow

```text
docker ps -a -> exit code -> logs -> inspect command/env/mounts -> run shell -> rebuild/config fix
```

Commands:

```bash
docker ps -a
docker logs CONTAINER --tail 100
docker inspect CONTAINER
docker run --rm -it --entrypoint sh IMAGE
docker inspect CONTAINER --format '{{.State.ExitCode}} {{.State.OOMKilled}}'
```

---

## 3. Common Causes

- command exits successfully after completing
- missing environment variable
- wrong `CMD` or `ENTRYPOINT`
- script lacks execute permission
- file missing due to `.dockerignore` or build context
- bind mount hides expected files
- app dependency not ready
- OOMKilled due to low memory

---

## 4. Mitigation

- fix Dockerfile command/entrypoint
- pass required env vars or config
- fix permissions in image build
- add wait/retry logic for dependencies
- inspect mounts
- tune memory or fix leak

---

## 5. Interview Summary

```text
For a container that will not start, I check docker ps -a, exit code, logs, inspect output, command/entrypoint, env vars, mounts, permissions, missing files, dependency readiness, and OOMKilled state. Then I fix image or runtime config rather than manually editing the container.
```

---

## 6. Revision Notes

- One-line summary: Startup failures are usually command, config, file, permission, dependency, or limit problems.
- Three keywords: logs, entrypoint, env.
- One trap: trying to `exec` into a container that is not running instead of overriding entrypoint with a shell.