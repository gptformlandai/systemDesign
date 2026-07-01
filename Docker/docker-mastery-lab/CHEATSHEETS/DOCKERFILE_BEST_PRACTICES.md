# Dockerfile Best Practices

## Build Quality

- keep build context small
- use `.dockerignore`
- copy dependency lock files before source
- use multi-stage builds for compiled/bundled apps
- keep runtime image minimal
- remove package manager caches in the same layer

## Runtime Quality

- run as non-root when possible
- expose/document app port
- use env vars for runtime config
- log to stdout/stderr
- add a meaningful health check
- avoid shell scripts as PID 1 unless they handle signals correctly

## Security Quality

- do not bake secrets into images
- pin or record base image identity
- scan images
- avoid `--privileged`
- avoid mounting Docker socket
- keep base images updated

## Senior Summary

```text
A production Dockerfile should be reproducible, cache-efficient, minimal, secure, observable, and explainable.
```