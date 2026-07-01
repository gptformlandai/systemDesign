# Lab 06: Registry Tagging And Digest

## Goal

Understand image identity, tags, digests, and promotion safety.

## Local Tag Drill

```bash
cd ../EXAMPLES/hello-web
docker build -t docker-mastery-hello-web:git-sha-demo .
docker image inspect docker-mastery-hello-web:git-sha-demo
docker image inspect docker-mastery-hello-web:git-sha-demo --format '{{json .RepoDigests}}'
```

Repo digests usually appear after pulling from or pushing to a registry.

## Promotion Design Exercise

Write a promotion table:

| Environment | Image Reference | Gate |
|---|---|---|
| dev | git SHA tag | build and test |
| staging | digest | scan and smoke test |
| prod | same digest | approval and rollout |

## Failure Drill

Explain why this is unsafe:

```text
prod deploys app:latest every hour
```

Then rewrite it using immutable identity:

```text
prod deploys app@sha256:<digest>
```

## Interview Takeaway

```text
Tags are convenient labels, but digests are exact content identity. Production delivery should build once, scan once, push once, and promote the same approved image digest.
```