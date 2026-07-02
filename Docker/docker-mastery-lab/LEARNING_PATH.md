# Docker Mastery Lab Learning Path

## Phase 1: Environment And CLI

- Run [SCRIPTS/01-docker-snapshot.sh](SCRIPTS/01-docker-snapshot.sh).
- Complete [LABS/01-docker-environment-snapshot.md](LABS/01-docker-environment-snapshot.md).
- Explain client, daemon, image, container, network, volume, and registry.

## Phase 2: Build And Run

- Build [EXAMPLES/hello-web](EXAMPLES/hello-web).
- Complete [LABS/02-dockerfile-build-and-cache.md](LABS/02-dockerfile-build-and-cache.md).
- Explain build context, layers, cache, `CMD`, `ENTRYPOINT`, and `.dockerignore`.

## Phase 3: Compose, Networking, And Storage

- Run `docker compose up` in [EXAMPLES/hello-web](EXAMPLES/hello-web).
- Complete [LABS/03-compose-multi-service.md](LABS/03-compose-multi-service.md).
- Complete [LABS/04-networking-dns-ports.md](LABS/04-networking-dns-ports.md).
- Complete [LABS/05-volumes-permissions-persistence.md](LABS/05-volumes-permissions-persistence.md).

## Phase 4: Registry And Supply Chain

- Complete [LABS/06-registry-tagging-digest.md](LABS/06-registry-tagging-digest.md).
- Review [CHEATSHEETS/BUILDKIT_PID1_MULTI_PLATFORM.md](CHEATSHEETS/BUILDKIT_PID1_MULTI_PLATFORM.md).
- Review [CHEATSHEETS/DOCKERFILE_BEST_PRACTICES.md](CHEATSHEETS/DOCKERFILE_BEST_PRACTICES.md).
- Review [CHEATSHEETS/PRODUCTION_SAFETY_RULES.md](CHEATSHEETS/PRODUCTION_SAFETY_RULES.md).

## Phase 5: Production Debugging

- Run [SCRIPTS/02-container-debug.sh](SCRIPTS/02-container-debug.sh) against a test container.
- Run [SCRIPTS/03-network-debug.sh](SCRIPTS/03-network-debug.sh) against a test network.
- Run [SCRIPTS/04-volume-debug.sh](SCRIPTS/04-volume-debug.sh) against a test volume.
- Run [SCRIPTS/07-docker-disk-pressure-safe.sh](SCRIPTS/07-docker-disk-pressure-safe.sh) on a safe local Docker environment.
- Complete [LABS/07-production-incident-debugging.md](LABS/07-production-incident-debugging.md).
- Review [RUNBOOKS/DOCKER_DAEMON_DISK_PRESSURE_PRUNE.md](RUNBOOKS/DOCKER_DAEMON_DISK_PRESSURE_PRUNE.md).

## Phase 6: Portfolio And Interview Readiness

- Complete each file in [PROJECTS](PROJECTS).
- Complete [../06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md](../06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md).
- Review senior gap-fill sheets #32-#39 for Compose, daemon operations, security hardening, supply chain, Buildx/Bake, networking, storage, and platform boundaries.
- Memorize [INTERVIEW_PREP/SENIOR_ANSWER_PATTERNS.md](INTERVIEW_PREP/SENIOR_ANSWER_PATTERNS.md).
- Practice [INTERVIEW_PREP/DOCKER_QUESTION_BANK.md](INTERVIEW_PREP/DOCKER_QUESTION_BANK.md).
- Use the runbooks until your troubleshooting flow is automatic.
