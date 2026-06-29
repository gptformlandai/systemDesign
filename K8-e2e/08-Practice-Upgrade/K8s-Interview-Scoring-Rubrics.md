# Kubernetes Interview Scoring Rubrics

> Track: K8s Interview Track — Phase 8: Practice Upgrade
> Use this to self-grade your mock interview answers or have a peer grade your session.
> Score each dimension 1–4. Total max: 24 points. Strong hire: 18+. No hire: <12.

---

## Evaluation Dimensions

| Dimension | Weight | Focus |
|---|---|---|
| Technical Depth | High | Correctness, precision, examples |
| Operational Reasoning | High | Production thinking, tradeoffs |
| Security Mindset | Medium | PCI, zero-trust, supply chain |
| Scalability Thinking | Medium | Design for 10x, not just current load |
| Debugging Approach | High | Systematic, calm, methodical |
| Communication Clarity | Medium | Explain to non-expert, structure |

---

## Dimension 1: Technical Depth

### L4 (Junior — expected score: 2)
**Green flags:**
- Knows Pod, Deployment, Service basics
- Can explain requests vs limits
- Knows what kubectl commands do (get, describe, logs)
- Understands liveness vs readiness probe purpose

**Red flags:**
- Confuses Pod with container
- Doesn't know what a Service does
- Can't explain how HPA works at all

### L5 (Mid — expected score: 3)
**Green flags:**
- Explains control plane components accurately (etcd, scheduler, controller-manager)
- Can walk through rolling update mechanics (ReplicaSet creation, maxSurge/maxUnavailable)
- Knows StatefulSet vs Deployment trade-offs
- Can describe NetworkPolicy and write one from memory
- Understands RBAC: Role vs ClusterRole vs bindings

**Red flags:**
- Doesn't know what kube-scheduler does
- Vague on what "requests" are used for at scheduling time vs runtime
- Can't articulate headless service use case

### L6 (Senior — expected score: 3-4)
**Green flags:**
- Deep understanding of control loop pattern and its implications
- Can explain kube-proxy (iptables vs ipvs) and EndpointSlice
- Knows Operator pattern and CRD design
- Deep RBAC: aggregated ClusterRoles, IRSA, ServiceAccount projections
- Can write PromQL for K8s metrics from memory
- Understands admission webhook pipeline (mutating → validating)

**Red flags:**
- Fuzzy on how a packet traverses from pod to Service to pod
- Can't explain StatefulSet ordered guarantees and implications
- Doesn't know what OPA/Gatekeeper does

### L7 (Staff — expected score: 4)
**Green flags:**
- Can compare cluster topology strategies (one big cluster vs many small, hub/spoke)
- Deep knowledge of etcd: quorum, performance, backup/restore
- Can critique design choices with specific data (e.g., "iptables degrades at 10k services")
- Articulates supply chain security (SLSA, SBOM, Cosign)
- Can design multi-tenant platform from scratch with concrete trade-offs

---

## Dimension 2: Operational Reasoning

**Score 1 (No hire):**
- Answers are theoretical only, no production anecdotes or scenarios
- Doesn't think about what happens when things fail

**Score 2 (Below bar):**
- Some operational thinking but misses key failure modes
- e.g., knows rolling update exists but doesn't mention preStop hook for zero downtime

**Score 3 (Meets bar):**
- Spontaneously brings up failure modes: "what if a pod is terminating but iptables hasn't updated yet?"
- Talks about PDB, pod anti-affinity, PVC retention on StatefulSet delete
- Asks clarifying questions about SLA before designing (latency? availability? compliance?)

**Score 4 (Exceeds bar):**
- Proactively identifies edge cases without prompting
- Talks about chaos engineering, disaster recovery drills
- Discusses cost implications of design choices
- References specific metrics they'd alert on for each component they propose

---

## Dimension 3: Security Mindset

**Score 1 (No hire):**
- Never mentions security unless directly asked
- Says "just use admin credentials" or ignores RBAC

**Score 2 (Below bar):**
- Knows RBAC exists but can't design a least-privilege model
- Doesn't know what PSA does

**Score 3 (Meets bar):**
- Applies principle of least privilege unprompted (one ServiceAccount per app, not default)
- Knows PSA levels (Baseline/Restricted) and when to enforce
- Mentions image scanning as part of CI/CD pipeline
- Knows Secrets should use etcd encryption + ESO

**Score 4 (Exceeds bar):**
- Designs zero-trust by default: NetworkPolicy deny-all, explicit allow list
- Mentions supply chain security: image signing (Cosign), SBOM, admission verification (Kyverno)
- Discusses IRSA vs static credentials with specific security advantage
- Proactively mentions audit logging, incident response plan for security events
- Can articulate SLSA levels and what they protect against

---

## Dimension 4: Scalability Thinking

**Score 1 (No hire):**
- Designs for current load only, no thought to 10x growth
- Single replica Deployments in answers

**Score 2 (Below bar):**
- Knows HPA exists but can't discuss its limitations or KEDA

**Score 3 (Meets bar):**
- Discusses HPA with behavior tuning (scale-down stabilization window)
- Knows KEDA for event-driven autoscaling, scale-to-zero
- Mentions Karpenter for faster node provisioning
- Designs multi-AZ from the start
- Discusses ResourceQuota + LimitRange for multi-tenant fairness

**Score 4 (Exceeds bar):**
- Compares Cluster Autoscaler vs Karpenter with specific numbers (30 sec vs 3 min)
- Knows iptables scaling limits (~10k services) and ipvs advantage
- Discusses VPA + HPA co-existence conflict on CPU
- Can size a cluster (nodes, instance types) given TPS and latency requirements
- Mentions predictive scaling for known traffic patterns

---

## Dimension 5: Debugging Approach

**Score 1 (No hire):**
- Jumps to solutions without diagnosing
- Guesses: "probably a DNS issue" without verifying
- Panics when given a novel scenario

**Score 2 (Below bar):**
- Limited to basic `kubectl get pods / describe pod / logs`
- Doesn't structure their approach
- Misses systematic steps (e.g., checks logs before checking if endpoints exist)

**Score 3 (Meets bar):**
- Uses the framework: observe → hypothesize → test → resolve
- Knows when to roll back vs dig deeper (rolls back production, then investigates in staging)
- Checks: events → logs → previous logs → describe (for probe failures) → exec into pod → network test
- Can decode common error states: CrashLoopBackOff, ImagePullBackOff, OOMKilled, Pending, Evicted

**Score 4 (Exceeds bar):**
- Has a mental 5-minute debug framework they can articulate clearly
- Uses non-obvious commands: `kubectl top pods --containers`, `kubectl get events --sort-by=.lastTimestamp`
- Thinks about blast radius: "I'll isolate this pod first before touching the Deployment"
- Mentions post-incident practices: blameless postmortem, runbook update, alert improvement

---

## Dimension 6: Communication Clarity

**Score 1 (No hire):**
- Answers are rambling, no structure
- Uses only jargon without explanation
- Can't explain to a non-K8s engineer why something matters

**Score 2 (Below bar):**
- Some structure but loses the thread mid-answer
- Doesn't check if the interviewer is following

**Score 3 (Meets bar):**
- Uses layered explanation: big picture → specific → why it matters
- Can give a 30-second answer when asked a simple question
- Uses analogies naturally ("kube-proxy is like a routing table that updates automatically")
- Stops at key points and asks: "does that answer your question or go deeper?"

**Score 4 (Exceeds bar):**
- Structures complex answers using explicit framework: "There are three parts to this..."
- Adapts depth to the interviewer (goes deeper when pushed, summarizes when time-boxed)
- Can whiteboard architecture clearly (even verbally in a virtual interview)
- Writes clean YAML from memory without hesitating

---

## Scoring Table

| Dimension | Score (1-4) | Notes |
|---|---|---|
| Technical Depth | | |
| Operational Reasoning | | |
| Security Mindset | | |
| Scalability Thinking | | |
| Debugging Approach | | |
| Communication Clarity | | |
| **Total** | **/24** | |

---

## Decision Guide

| Total | Decision | Notes |
|---|---|---|
| 21-24 | Strong Hire | Outstanding across all dimensions |
| 18-20 | Hire | Solid, one minor gap acceptable |
| 15-17 | Weak Hire / Lean | Good foundation, growth needed in 1-2 areas |
| 12-14 | No Hire | Significant gaps in core areas |
| <12 | No Hire | Foundational knowledge missing |

---

## Red Flags — Auto-Disqualifiers

Any of these should shift the decision toward No Hire regardless of total score:

- Says "we don't need security controls for internal services"
- Proposes running as root because "it's easier"
- Doesn't know how to roll back a failed deployment
- Can't explain what happens when a pod crashes (doesn't know about ReplicaSet reconciliation)
- Says "I'd just restart the cluster" as a first response to a pod issue
- Gets defensive when interviewer questions a design choice
- Copies from memory a K8s template without understanding what it does

---

## Green Flags — Differentiators

These elevate a candidate from Hire to Strong Hire:

- Asks clarifying questions before designing ("What's the latency requirement? What's the RTO?")
- Mentions trade-offs unprompted ("This approach is simpler but doesn't scale past X")
- Cites real experience ("At my previous job, we hit this issue when...")
- Knows official K8s enhancement proposals or blog posts by name
- Can estimate rough numbers ("iptables starts degrading around 10k services")
- Has opinions based on experience, not just textbook knowledge
- Proactively mentions cost implications ("LoadBalancer per service costs $18/month each; Ingress consolidates")
