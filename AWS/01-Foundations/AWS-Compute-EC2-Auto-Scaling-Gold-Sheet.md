# AWS Compute: EC2 and Auto Scaling Gold Sheet

> Track: AWS Interview Track — Foundations
> Goal: understand EC2 deeply enough to make instance, pricing, and scaling decisions in production and defend them in interviews.

---

## 0. How To Read This

Beginner focus:
- instance types, on-demand vs reserved
- Auto Scaling Group basics
- AMI, user data, security groups

Intermediate focus:
- placement groups, dedicated hosts
- launch templates vs launch configurations
- scaling policies (target tracking, step, scheduled)

Senior / MAANG focus:
- spot interruption handling
- instance store vs EBS-backed trade-offs
- EC2 as compute primitive vs containers/serverless
- right-sizing with Compute Optimizer
- cost model across on-demand / savings plans / spot

---

# Topic 1: EC2 — Elastic Compute Cloud

## 1. Intuition

EC2 is a virtual machine you rent from AWS.

You get CPU, RAM, network, and storage. You control the OS, runtime, and application. You pay while it runs.

The mental model:

```text
EC2 = virtual server
AMI = machine image (OS + software snapshot)
Instance = one running AMI
Security group = stateful firewall per instance
IAM role = permissions the instance uses to call other AWS services
```

## 2. Instance Types And Families

| Family | Optimized For | Use Cases |
|---|---|---|
| `t` (t3, t4g) | burstable CPU | dev/test, low steady-state load |
| `m` (m6i, m7g) | general purpose | web servers, app servers |
| `c` (c6i, c7g) | compute optimized | CPU-heavy, media, ML inference |
| `r` (r6i, r7g) | memory optimized | in-memory DB, large caches, analytics |
| `i` (i3, i4i) | storage optimized | NoSQL, data warehousing, high IOPS |
| `g` / `p` | GPU | ML training, video encoding |
| `x` | extreme memory | SAP HANA, in-memory analytics |
| `inf` | ML inference | AWS Inferentia chips |

Interview trap:

```text
Do not use t-series in production for sustained CPU-heavy workloads.
t-series uses CPU credits. When credits run out, CPU throttles to the baseline.
Under-provisioned t instances look healthy in CloudWatch until traffic spikes.
```

## 3. Pricing Models

| Model | When To Use | Key Point |
|---|---|---|
| On-Demand | unpredictable, short-lived, dev/test | highest hourly rate, no commitment |
| Savings Plans (Compute) | steady baseline load | commit $/hour for 1-3 years, applies across regions/families |
| Reserved Instances (Standard) | steady regional baseline | locked to instance family/size/region |
| Spot | stateless, fault-tolerant, batch | up to 90% cheaper, can be interrupted with 2-min notice |
| Dedicated Host | license compliance, isolation | per-host billing, full server |
| Dedicated Instance | physical isolation from other accounts | per-instance billing |

Production pattern:

```text
Baseline capacity: Savings Plans (Compute) or Reserved Instances
Peak/overflow: Spot with interruption handling
Short-lived jobs: Spot or On-Demand
Compliance: Dedicated Host
```

Interview line:

```text
I use Savings Plans for the predictable baseline and Spot for stateless workers that can
tolerate interruption. Spot interruption must be handled with checkpointing or SQS-based
work queues that auto-retry.
```

## 4. Storage Options

| Type | Characteristic | Use Case |
|---|---|---|
| EBS (gp3) | network-attached block storage, persists after stop | OS disk, databases |
| EBS (io2 Block Express) | high IOPS, provisioned | high-throughput DB workloads |
| Instance Store | physically attached, ephemeral | temp data, caches, scratch space |
| EFS | shared NFS file system | shared file access across instances |

Critical rule:

```text
Instance store data is LOST when the instance stops, terminates, or fails.
Never use instance store for data that must survive instance lifecycle.
Use it for caches, temp files, or replay-safe scratch buffers.
```

## 5. AMI And Launch Templates

AMI (Amazon Machine Image):

```text
OS + installed packages + configurations snapshot
Used to launch identical instances
Region-specific (can be copied across regions)
```

Launch Template (preferred over Launch Configurations):

```text
Instance type, AMI, key pair, security groups, IAM role, user data, tags
Supports versioning
Required for Mixed Instance Policies (Spot + On-Demand)
```

User Data:

```text
Script run on first boot
Install packages, configure app, pull secrets
Keep it idempotent — it can run on every restart if configured to
```

## 6. Placement Groups

| Type | Behavior | Use Case |
|---|---|---|
| Cluster | all instances in same rack, same AZ | low-latency HPC, tightly coupled |
| Spread | each instance on separate hardware | small critical instances needing resilience |
| Partition | groups of instances on separate racks | distributed systems (Kafka, Cassandra) |

Interview trap:

```text
Cluster placement groups have low latency but zero AZ diversity. One hardware failure can
take multiple instances. Never use cluster placement for availability-critical services.
```

---

# Topic 2: EC2 Auto Scaling

## 1. Intuition

Auto Scaling lets the fleet grow and shrink automatically in response to demand.

Without it, you either over-provision (wasteful) or under-provision (dropped requests).

## 2. Auto Scaling Group Core Concepts

| Concept | Meaning |
|---|---|
| Desired capacity | how many instances to run right now |
| Min capacity | floor, never scale below |
| Max capacity | ceiling, never scale above |
| Health check | how ASG knows an instance is healthy (EC2 or ELB) |
| AZ rebalancing | ASG maintains even distribution across AZs |
| Lifecycle hooks | pause instance at launch/termination for custom actions |

## 3. Scaling Policies

| Policy Type | How It Works | Best For |
|---|---|---|
| Target Tracking | maintain a target metric value (e.g., 60% CPU) | steady-state scaling |
| Step Scaling | step up/down based on alarm threshold bands | fine-grained control |
| Scheduled Scaling | set desired capacity at specific times | known traffic patterns |
| Predictive Scaling | ML-based forecast + pre-scale | recurring daily/weekly patterns |

Target tracking example:

```text
Target: 60% CPU utilization
Current: 80% CPU
Action: launch new instances until CPU settles near 60%
```

## 4. Mixed Instances Policy

Mix On-Demand and Spot within one ASG:

```json
{
  "MixedInstancesPolicy": {
    "InstancesDistribution": {
      "OnDemandBaseCapacity": 2,
      "OnDemandPercentageAboveBaseCapacity": 25,
      "SpotAllocationStrategy": "capacity-optimized"
    },
    "LaunchTemplate": { ... }
  }
}
```

This keeps 2 On-Demand always on, and fills the rest with 75% Spot / 25% On-Demand.

## 5. Lifecycle Hooks

Lifecycle hooks pause instances at:

- `autoscaling:EC2_INSTANCE_LAUNCHING` — before instance enters InService
- `autoscaling:EC2_INSTANCE_TERMINATING` — before instance is terminated

Use for:

- draining existing connections before termination
- warm-up (cache fill, JVM JIT) before receiving traffic
- bootstrapping custom configuration at launch

## 6. Health Checks And Grace Period

If ELB health check is configured:

- unhealthy target in ELB marks the instance unhealthy in ASG
- ASG terminates and replaces unhealthy instance

Health check grace period:

```text
Time after launch before ASG starts checking health.
Set long enough for your app to fully start.
Too short: ASG kills instances before they finish startup.
Too long: bad instances serve traffic while grace period is still running.
```

## 7. Scale-In Protection

Prevent specific instances from being terminated during scale-in:

```text
Use when:
- instance is processing a long-running job
- draining a queue or completing critical work
- waiting for external confirmation

Combine with lifecycle hooks and SQS-based work queues for robust spot handling.
```

## 8. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Use t-series for sustained CPU | use m or c series for production |
| Skip ELB health check, use only EC2 check | enable ELB health check so bad app = terminated |
| Set same min and max | allow headroom for scaling |
| No lifecycle hook for Spot interruption | catch SIGTERM, checkpoint work to SQS |
| Use hardcoded IAM credentials in user data | attach IAM instance role |
| One AZ in ASG | multi-AZ always for production |

## 9. Production Pattern: Multi-AZ ECS-Style With EC2

```text
VPC
├── Public Subnet AZ-a: ALB node
├── Public Subnet AZ-b: ALB node
├── Private Subnet AZ-a: EC2 instances (ASG)
├── Private Subnet AZ-b: EC2 instances (ASG)
└── Private Subnet AZ-a/b: RDS Multi-AZ, ElastiCache

Traffic:
Internet -> Route 53 -> ALB -> EC2 instances (private)
EC2 instances -> RDS/ElastiCache (private, no internet)
EC2 instances -> S3/Secrets Manager via VPC endpoints
```

## 10. Interview Scenarios

**Scenario**: "Traffic spikes 10x during Black Friday. How do you handle it?"

Strong answer:

```text
I would use target tracking scaling on the ASG keyed to ALB RequestCountPerTarget.
This scales proactively when requests per instance exceed the target.
I would add predictive scaling for the known Black Friday time window.
I would pre-warm instances with a scheduled action the day before.
For cost, I would run baseline On-Demand and spike on Spot with interruption handling via
SQS so no work is lost when Spot instances are reclaimed.
```

**Scenario**: "An EC2 instance is showing 100% CPU. How do you debug it?"

Strong answer:

```text
First check CloudWatch CPU metric trend (sudden spike vs gradual growth).
SSH or use SSM Session Manager to get process-level view (top/htop, jstack for JVM).
Check if GC pressure is driving CPU on a Java app.
Check CloudWatch for correlated changes (new deployment, traffic spike, cron job).
Check ELB request count and error rate.
If the app is stuck, use lifecycle hooks and ASG to replace the instance while
preserving in-flight state in a queue.
```

## 11. Key Numbers

- Minimum instances for HA: 2 (one per AZ)
- Spot interruption notice: 2 minutes
- ELB health check default: 5 checks, 30s interval
- Default ASG termination policy: OldestLaunchTemplate first, then availability-zone imbalance
- EC2 gp3 EBS baseline: 3000 IOPS, 125 MB/s, free for gp3

## 12. Revision Notes

- EC2 is the right answer when you need OS control, legacy apps, custom agents, or stateful self-managed databases
- Pricing: Savings Plans > Reserved Instances for flexibility; Spot for batch/workers
- Instance store is ephemeral; never store critical data on it
- Target tracking is the default scaling policy for most apps
- Always: multi-AZ ASG + ELB health check + lifecycle hooks for graceful shutdown
- Right-size using AWS Compute Optimizer, not instinct

## 13. Official Source Notes

- EC2 instance types: <https://aws.amazon.com/ec2/instance-types/>
- EC2 pricing: <https://aws.amazon.com/ec2/pricing/>
- Auto Scaling: <https://docs.aws.amazon.com/autoscaling/ec2/userguide/what-is-amazon-ec2-auto-scaling.html>
- Spot best practices: <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-best-practices.html>
- AWS Compute Optimizer: <https://aws.amazon.com/compute-optimizer/>
