# Docker Mastery Sheet System - Start Here

This folder is a modular beginner-to-pro Docker mastery track.

It has a 30-file core learning path plus appendix #31 for senior MAANG-level production internals.

Start with:

- [Docker-Mastery-Track-Index.md](Docker-Mastery-Track-Index.md)
- [docker-mastery-lab/README.md](docker-mastery-lab/README.md)
- [docker-mastery-lab/LEARNING_PATH.md](docker-mastery-lab/LEARNING_PATH.md)

The goal:

```text
Docker mastery = package applications as reliable images, run containers safely, and debug containerized systems in production
```

The modular track has:

- `01-Foundations` for Docker mental model, daemon/CLI, images, containers, registries, and Dockerfile basics.
- `02-Intermediate-Practical` for daily commands, Dockerfile cache, volumes, networking, Compose, and registries.
- `03-Senior-Production` for security, resource limits, build optimization, observability, CI/CD, orchestration bridge, and appendix #31.
- `04-Scenario-Practice` for containerizing apps, startup, network, volume, build, registry, and production incident scenarios.
- `05-Special-Interview-Rounds` for interview Q&A, command maps, anti-patterns, and debugging traps.
- `06-Practice-Upgrade` for active recall, command drills, mini projects, and production readiness.
- `docker-mastery-lab` for Dockerfiles, Compose examples, scripts, labs, projects, cheatsheets, interview prep, and runbooks.

Use [03-Senior-Production/31-Docker-Pro-Gap-Fill-PID1-BuildKit-MultiPlatform-Disk-Pressure-MAANG-Sheet.md](03-Senior-Production/31-Docker-Pro-Gap-Fill-PID1-BuildKit-MultiPlatform-Disk-Pressure-MAANG-Sheet.md) after the senior production sheets when you want the deeper architect-level explanation format.

Core mental model:

```text
source code + Dockerfile + build context = image
image + config + runtime resources = container
container + network + volume + logs + limits = debuggable service
```

Use the root index as the source of truth for study order.
