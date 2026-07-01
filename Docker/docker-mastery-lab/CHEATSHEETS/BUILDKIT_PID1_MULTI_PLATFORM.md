# BuildKit, PID 1, Multi-Platform Cheat Sheet

## PID 1

- container main process is PID 1
- PID 1 must handle signals correctly
- prefer exec-form `CMD` and `ENTRYPOINT`
- use `docker run --init` when process reaping is needed
- app should handle SIGTERM gracefully

Good:

```Dockerfile
CMD ["python", "server.py"]
```

Risky if signal forwarding is not handled:

```Dockerfile
CMD python server.py
```

## BuildKit

```bash
DOCKER_BUILDKIT=1 docker build -t app:local .
docker buildx build --platform linux/amd64,linux/arm64 -t registry/app:1.0.0 .
```

Use BuildKit for:

- cache mounts
- secret mounts
- SSH mounts
- multi-platform builds
- provenance and advanced build metadata where supported

## Secret Rule

Avoid secrets in:

- `ARG`
- `ENV`
- copied files
- image layers
- image history

Prefer BuildKit secret mounts or platform secret stores.

## Multi-Platform Checks

```bash
docker image inspect IMAGE --format '{{.Architecture}}/{{.Os}}'
docker buildx imagetools inspect IMAGE
docker manifest inspect IMAGE
```

Common failure:

```text
exec format error = image architecture does not match runtime platform
```

## Senior One-Liner

```text
I check process signal behavior, safe BuildKit secret/cache usage, and image architecture before blaming the application.
```