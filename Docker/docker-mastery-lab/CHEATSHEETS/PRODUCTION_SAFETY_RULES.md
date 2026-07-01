# Docker Production Safety Rules

## Do

- deploy by digest or immutable tag
- promote the same image across environments
- keep rollback images available
- set resource limits intentionally
- use non-root runtime users
- scan images before release
- log to stdout/stderr
- preserve incident evidence before cleanup

## Avoid

- `latest` in production
- secrets in Dockerfiles or image layers
- `--privileged` without a written reason
- casual `docker system prune -a`
- casual `docker compose down -v`
- manual hotfixes inside running containers
- bind mounts for production state without a plan

## Incident Principle

```text
Inspect before changing state. Record the image identity before rollback or rebuild.
```