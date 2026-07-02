# gRPC Mastery Lab

Hands-on practice area for the gRPC Mastery Track.

Use this lab to practice `.proto` design, RPC debugging, deadlines, status codes, metadata, streaming design, service discovery, security, observability, and production incident response.

---

## Lab Structure

| Folder | Purpose |
|---|---|
| `EXAMPLES` | small proto examples and request templates |
| `SCRIPTS` | safe helper scripts and incident templates |
| `LABS` | focused hands-on drills |
| `PROJECTS` | portfolio-ready mini projects |
| `CHEATSHEETS` | fast references for commands, statuses, proto rules |
| `INTERVIEW_PREP` | interview prompts and answer structures |
| `RUNBOOKS` | production debugging runbooks |

---

## Suggested Flow

1. Read [../gRPC-Mastery-Sheet-System.md](../gRPC-Mastery-Sheet-System.md).
2. Inspect [EXAMPLES/greeter/greeter.proto](EXAMPLES/greeter/greeter.proto).
3. Run the safe scripts in `SCRIPTS` to generate local review templates.
4. Complete `LABS` in order.
5. Build at least two `PROJECTS`; include Project 06 if you want runnable implementation proof.
6. Practice `RUNBOOKS` until incident debugging becomes automatic.

---

## Tooling Notes

The lab is useful even without local gRPC tooling.

Optional tools:

- `protoc`
- `buf`
- `grpcurl`
- `grpcui`
- `ghz` or another gRPC load-test tool
- language-specific gRPC plugins
- Docker or Kubernetes for deployment practice

If tools are missing, use the scripts and markdown templates for design, review, and interview practice.

---

## Safety Rule

Scripts in this lab are read-only or template-generating. They do not modify clusters, certificates, deployments, or production services.
