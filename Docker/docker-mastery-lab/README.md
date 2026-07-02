# Docker Mastery Lab

> Hands-on companion for the Docker Mastery track.

This lab turns the study sheets into repeatable command practice, safe diagnostics, incident runbooks, and portfolio projects.

## Lab Map

| Area | Purpose |
|---|---|
| [LEARNING_PATH.md](LEARNING_PATH.md) | ordered lab sequence |
| [SCRIPTS](SCRIPTS) | read-only Docker diagnostic helpers |
| [EXAMPLES/hello-web](EXAMPLES/hello-web) | small containerized web app with its own [README](EXAMPLES/hello-web/README.md) |
| [LABS](LABS) | guided hands-on exercises |
| [PROJECTS](PROJECTS) | portfolio-grade Docker projects |
| [CHEATSHEETS](CHEATSHEETS) | fast command and design recall |
| [INTERVIEW_PREP](INTERVIEW_PREP) | senior answer patterns and questions |
| [RUNBOOKS](RUNBOOKS) | incident response playbooks |

## Recommended Flow

1. Read [../Docker-Mastery-Sheet-System.md](../Docker-Mastery-Sheet-System.md).
2. Complete the foundation and intermediate sheets in order.
3. Run the scripts in [SCRIPTS](SCRIPTS) on a safe local Docker environment.
4. Build and run [EXAMPLES/hello-web](EXAMPLES/hello-web).
5. Complete the labs, projects, and runbooks.
6. Complete the production capstone in [../06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md](../06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md).
7. Use interview prep to turn command fluency into clear answers.

## Safety Rules

- Inspect before deleting Docker objects.
- Avoid `docker system prune -a` unless you fully understand what will be removed.
- Avoid `docker compose down -v` on stateful projects unless the lab explicitly asks for it.
- Do not put real secrets into images, Compose files, or sample env files.
- Prefer local throwaway images and containers for practice.

## Minimum Tools

- Docker Desktop or Docker Engine
- Docker Compose plugin
- Bash-compatible shell
- Optional: image scanner such as Docker Scout, Trivy, Grype, or the scanner used by your platform

## Lab Completion Definition

You have completed this lab when you can explain:

```text
How an image is built, cached, secured, attested, scanned, tagged, shipped by digest, run, observed, debugged, limited, and rolled back.
```
