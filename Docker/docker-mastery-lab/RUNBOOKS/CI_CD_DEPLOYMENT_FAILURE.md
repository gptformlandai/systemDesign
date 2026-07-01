# Runbook: Docker CI/CD Deployment Failure

## Symptoms

- wrong image deployed
- release cannot roll back
- scan gate blocks release
- staging works but production differs

## Evidence Commands

```bash
docker image inspect IMAGE
docker pull IMAGE
docker history IMAGE
```

Also inspect pipeline metadata:

```text
git SHA, build number, tag, digest, scan result, environment, deploy timestamp
```

## Check

- was image rebuilt per environment?
- was `latest` used?
- is digest recorded?
- did scan gate fail or get bypassed?
- did registry retention delete rollback image?
- did deploy platform pull the expected architecture?

## Mitigate

- rollback to previous digest
- stop mutable tag deployment
- restore missing rollback image if possible
- rebuild only if the artifact is truly lost and document the exception
- fix pipeline gate before retrying production

## Prevent

- build once and promote artifact
- deploy by digest or immutable tag
- keep release metadata
- retain rollback images
- make scan policy explicit