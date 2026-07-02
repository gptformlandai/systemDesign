# Docker Mastery Sheet System - Start Here

This folder is a modular beginner-to-pro Docker mastery track.

It has the original 30-file core learning path, appendix #31 for senior MAANG-level production internals, senior gap-fill sheets #32-#39, and final capstone #40.

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
- `03-Senior-Production` for security, resource limits, build optimization, observability, CI/CD, orchestration bridge, appendix #31, advanced Compose, daemon operations, hardening, supply chain, Buildx/Bake, networking, storage, and platform boundaries.
- `04-Scenario-Practice` for containerizing apps, startup, network, volume, build, registry, and production incident scenarios.
- `05-Special-Interview-Rounds` for interview Q&A, command maps, anti-patterns, and debugging traps.
- `06-Practice-Upgrade` for active recall, command drills, mini projects, production readiness, and the final capstone.
- `docker-mastery-lab` for Dockerfiles, Compose examples, scripts, labs, projects, cheatsheets, interview prep, and runbooks.

Use [03-Senior-Production/31-Docker-Pro-Gap-Fill-PID1-BuildKit-MultiPlatform-Disk-Pressure-MAANG-Sheet.md](03-Senior-Production/31-Docker-Pro-Gap-Fill-PID1-BuildKit-MultiPlatform-Disk-Pressure-MAANG-Sheet.md) after the senior production sheets when you want the deeper architect-level internals format.

Then use these new pro gap-fill sheets to close the operator-level gaps:

- [03-Senior-Production/32-Docker-Compose-Advanced-Profiles-Overrides-Secrets-Watch-MAANG-Sheet.md](03-Senior-Production/32-Docker-Compose-Advanced-Profiles-Overrides-Secrets-Watch-MAANG-Sheet.md)
- [03-Senior-Production/33-Docker-Daemon-Host-Operations-Contexts-Logging-TLS-MAANG-Sheet.md](03-Senior-Production/33-Docker-Daemon-Host-Operations-Contexts-Logging-TLS-MAANG-Sheet.md)
- [03-Senior-Production/34-Docker-Advanced-Security-Hardening-CIS-Namespaces-LSM-MAANG-Sheet.md](03-Senior-Production/34-Docker-Advanced-Security-Hardening-CIS-Namespaces-LSM-MAANG-Sheet.md)
- [03-Senior-Production/35-Docker-Supply-Chain-SBOM-Scout-Provenance-Signing-MAANG-Sheet.md](03-Senior-Production/35-Docker-Supply-Chain-SBOM-Scout-Provenance-Signing-MAANG-Sheet.md)
- [03-Senior-Production/36-Docker-Buildx-BuildKit-Bake-Advanced-MAANG-Sheet.md](03-Senior-Production/36-Docker-Buildx-BuildKit-Bake-Advanced-MAANG-Sheet.md)
- [03-Senior-Production/37-Docker-Advanced-Networking-Drivers-DNS-Firewall-MAANG-Sheet.md](03-Senior-Production/37-Docker-Advanced-Networking-Drivers-DNS-Firewall-MAANG-Sheet.md)
- [03-Senior-Production/38-Docker-Advanced-Storage-Overlay2-Tmpfs-Volumes-MAANG-Sheet.md](03-Senior-Production/38-Docker-Advanced-Storage-Overlay2-Tmpfs-Volumes-MAANG-Sheet.md)
- [03-Senior-Production/39-Docker-Desktop-Platform-Boundaries-Registry-Alternatives-MAANG-Sheet.md](03-Senior-Production/39-Docker-Desktop-Platform-Boundaries-Registry-Alternatives-MAANG-Sheet.md)
- [06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md](06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md)

Core mental model:

```text
source code + Dockerfile + build context = image
image + config + runtime resources = container
container + network + volume + logs + limits = debuggable service
```

Use the root index as the source of truth for study order.
