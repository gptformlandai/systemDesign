# Docker Advanced Security Hardening: Namespaces, LSM, CIS - MAANG Sheet

> Track File #34 of 40 - Group 03: Senior Production
> For: senior security and platform interviews | Level: senior | Mode: runtime hardening

## 1. Intuition

A container is not a tiny VM. It is a process tree using the host kernel with isolation controls wrapped around it.

Senior Docker security is reducing what that process can see, do, write, call, mount, and escape to.

```text
least image + least user + least capabilities + least filesystem + least syscalls + least daemon access
```

---

## 2. Definition

- Definition: Docker hardening is the set of image, runtime, daemon, host, registry, and pipeline controls that reduce container blast radius.
- Category: container runtime security.
- Core idea: assume containers share kernel risk; use defense in depth.

---

## 3. Why It Exists

Default containers are useful, but production systems face higher risk:

- vulnerable dependencies
- root processes
- overbroad Linux capabilities
- writable root filesystems
- host path mounts
- exposed daemon socket
- weak registry trust
- missing audit and policy gates

Hardening makes a compromise less useful to an attacker.

---

## 4. Security Control Stack

| Layer | Controls |
|---|---|
| image | minimal base, patched packages, SBOM, scan gates, no secrets |
| user | non-root `USER`, stable UID/GID, no passwordless package managers at runtime |
| capabilities | `--cap-drop=ALL`, add back only required capabilities |
| filesystem | `--read-only`, tmpfs for writable paths, no sensitive host mounts |
| namespaces | PID, network, mount, IPC, UTS, user namespace remapping |
| seccomp | syscall allow/deny profile |
| LSM | AppArmor or SELinux policy |
| daemon | protected socket, rootless where feasible, TLS/SSH for remote access |
| registry/pipeline | signing/provenance, digest promotion, immutable tags |

---

## 5. Runtime Hardening Command

```bash
docker run --rm \
  --user 10001:10001 \
  --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=64m \
  --cap-drop ALL \
  --security-opt no-new-privileges:true \
  --pids-limit 256 \
  --memory 512m \
  --cpus 1.0 \
  --name hardened-api \
  my-api@sha256:...
```

If the app needs a writable directory, make it explicit:

```bash
docker run --rm \
  --read-only \
  --tmpfs /tmp \
  --mount type=volume,src=api-cache,dst=/app/cache \
  my-api:local
```

---

## 6. Capabilities Mental Model

Linux capabilities split root-like power into smaller permissions.

| Need | Capability Example | Better Question |
|---|---|---|
| bind low port | `NET_BIND_SERVICE` | can the app bind high port behind proxy instead? |
| change ownership | `CHOWN` | can ownership be fixed at build time? |
| change network settings | `NET_ADMIN` | should this be a sidecar/host job instead? |
| trace processes | `SYS_PTRACE` | is this only for debugging profile? |
| broad admin | `SYS_ADMIN` | usually a red flag |

Default posture:

```text
drop all -> run app -> add one capability only if evidence proves it is required
```

---

## 7. Seccomp, AppArmor, SELinux

| Control | What It Limits |
|---|---|
| seccomp | Linux syscalls a process can invoke |
| AppArmor | file, capability, network, and process rules by profile on supported Linux distros |
| SELinux | mandatory access labels and policies, common on RHEL/Fedora-style environments |

Use them when:

- running untrusted or high-risk workloads
- operating multi-tenant platforms
- meeting compliance requirements
- defending against kernel attack surface

Do not start by writing custom profiles blindly. First run with defaults, observe real syscall/file behavior, then tighten with tests.

---

## 8. User Namespace Remapping

User namespace remapping maps container root to an unprivileged host UID range.

```text
container root != host root
```

Benefits:

- reduces host blast radius if a container escapes a simple file ownership boundary
- limits damage from root inside the container

Costs:

- volume ownership surprises
- some privileged workloads break
- operational complexity with shared bind mounts

---

## 9. Rootless Mode

Rootless Docker runs daemon and containers without root privileges for many workflows.

Strong fit:

- developer machines
- CI runners with reduced host privilege
- single-user build hosts
- workloads that do not need privileged networking/storage features

Watch-outs:

- low port binding, overlay networking, storage drivers, and performance can differ by platform and setup
- rootless is not a substitute for image scanning, least privilege, or socket protection

---

## 10. Docker Socket Risk

Mounting `/var/run/docker.sock` gives the container the ability to ask the daemon to start containers, mount host paths, and affect the host.

Safer alternatives:

- remote builder with scoped credentials
- rootless builder
- BuildKit sidecar with narrow access
- CI platform native build service
- Kaniko/Buildah/BuildKit rootless where appropriate

If socket mounting is unavoidable, isolate the runner, restrict who can submit jobs, and treat it as privileged infrastructure.

---

## 11. CIS-Style Checklist

- host kernel and Docker Engine patched
- Docker daemon socket protected
- remote API uses SSH or TLS, never unauthenticated TCP
- containers do not run privileged unless explicitly approved
- host PID/network/mount namespaces not shared casually
- root filesystem read-only where practical
- capabilities minimized
- seccomp/AppArmor/SELinux defaults not disabled without reason
- image scans and SBOM checks in CI
- logs and audit events retained
- secrets never baked into image layers

---

## 12. Failure Modes

| Symptom | Cause | Fix |
|---|---|---|
| app cannot write temp files | `--read-only` without tmpfs | add tmpfs for `/tmp` or app temp path |
| permission denied on volume | UID/GID mismatch or userns-remap | align UID/GID, fix ownership, avoid blind `chmod 777` |
| debugger fails | dropped `SYS_PTRACE` or seccomp block | use debug profile only |
| app cannot bind port 80 | non-root without capability | bind 8080 and use proxy, or add `NET_BIND_SERVICE` |
| runtime breaks after LSM policy | profile too tight | observe and refine policy with tests |

---

## 13. Scenario

- Product / system: customer-facing API handling payment tokens.
- Why hardening fits: compromise blast radius must be small and auditable.
- What would go wrong without it: root process with broad capabilities and writable filesystem could make lateral movement and persistence easier.

---

## 14. Strong Answer

I would harden from image to runtime. The Dockerfile should use a minimal patched base, lock dependencies, avoid secrets, and create a non-root user. At runtime I would run with a stable UID, drop capabilities, use read-only root filesystem with explicit tmpfs/volumes, enforce seccomp and AppArmor/SELinux defaults, avoid host mounts and Docker socket access, set resource limits, scan the image, generate SBOM/provenance, and deploy by digest. I would only relax controls with evidence and a documented reason.

---

## 15. Revision Notes

- One-line summary: Docker hardening reduces what a compromised container can do to the host, data, and network.
- Three keywords: capabilities, seccomp, socket.
- One interview trap: saying "containers are isolated like VMs."
- One memory trick: ask "user, caps, fs, syscalls, socket, image?"
