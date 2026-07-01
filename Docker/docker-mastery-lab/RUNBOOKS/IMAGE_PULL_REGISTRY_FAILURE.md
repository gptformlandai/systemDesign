# Runbook: Image Pull Or Registry Failure

## Symptoms

- deployment cannot pull image
- image tag not found
- auth denied
- wrong architecture image pulled

## Evidence Commands

```bash
docker pull IMAGE
docker image inspect IMAGE
docker login REGISTRY
docker manifest inspect IMAGE 2>/dev/null || true
```

## Check

- registry auth and token expiry
- image tag exists
- digest exists and was retained
- network access to registry
- platform/architecture compatibility
- retention policy did not delete rollback image

## Mitigate

- refresh registry credentials
- deploy known-good digest
- restore or rebuild missing image if policy allows
- fix architecture selection
- pause rollout until image identity is verified

## Prevent

- deploy by digest or immutable tag
- retain rollback images
- record image metadata in releases
- validate registry access in CI/CD