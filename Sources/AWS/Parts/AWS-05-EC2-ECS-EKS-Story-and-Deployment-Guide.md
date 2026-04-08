# AWS Deep Dive: EC2, ECS, EKS Through Story Mode + Spring Boot/React Deployment Journey

> A detailed guide for understanding EC2, ECS, and EKS in a practical way. This note first explains them through a story of a growing product team, then walks from a local Java Spring Boot + React codebase to production-grade AWS deployments using each option.

---

# Table of Contents

1. [Why This Guide Exists](#1-why-this-guide-exists)
2. [The Story Mode: How Teams Usually Evolve](#2-the-story-mode-how-teams-usually-evolve)
3. [Starting Point: Your Local Application](#3-starting-point-your-local-application)
4. [What Must Be Done Before Any AWS Deployment](#4-what-must-be-done-before-any-aws-deployment)
5. [Path 1: Deploying with EC2](#5-path-1-deploying-with-ec2)
6. [Path 2: Deploying with ECS](#6-path-2-deploying-with-ecs)
7. [Path 3: Deploying with EKS](#7-path-3-deploying-with-eks)
8. [How the React Frontend Fits in Each Model](#8-how-the-react-frontend-fits-in-each-model)
9. [Networking, Security, Scaling, and Observability Across All Three](#9-networking-security-scaling-and-observability-across-all-three)
10. [Cost Comparison: EC2 vs ECS vs EKS](#10-cost-comparison-ec2-vs-ecs-vs-eks)
11. [Migration Journey: EC2 -> ECS -> EKS](#11-migration-journey-ec2---ecs---eks)
12. [How to Choose in Real Life](#12-how-to-choose-in-real-life)
13. [Interview-Ready Answer Framework](#13-interview-ready-answer-framework)
14. [Final Revision Sheet](#14-final-revision-sheet)

---

# 1. Why This Guide Exists

When people ask about EC2, ECS, and EKS, they often get three shallow answers:

- EC2 is virtual machines
- ECS is AWS containers
- EKS is managed Kubernetes

That is factually correct but operationally useless.

What you really need to know is:

- where your code runs
- what you are responsible for
- how deployment changes from one model to another
- what your team gains and what your team must now operate
- how a real application moves from local development to AWS production

This guide answers exactly that.

---

# 2. The Story Mode: How Teams Usually Evolve

Let us imagine a product team building an internal-to-external platform.

The application is:

- React frontend
- Java Spring Boot backend
- PostgreSQL database
- Redis cache later
- file uploads later
- APIs consumed by web and maybe mobile clients later

At the start, the team has only local code.

## 2.1 Phase 1: "We just need this live"

The team is small.

- 2 backend engineers
- 1 frontend engineer
- 1 DevOps-minded engineer or maybe none
- one staging environment
- one production environment
- traffic is low

The team says:

"We need something understandable. We do not want to learn Kubernetes right now. We just want our app running on AWS."

That usually leads to **EC2**.

Why?

- easiest mental model if the team already knows servers
- simple SSH-style debugging, at least initially
- no immediate container orchestration learning curve
- direct control over JVM tuning, OS packages, reverse proxy, and deployment scripts

The team might do:

- React build served by Nginx
- Spring Boot JAR running as a systemd service
- both hosted on one or more EC2 instances
- ALB in front

This works. It is not elegant, but it works.

## 2.2 Phase 2: "Deployments are getting messy"

Now the team has more traffic and more releases.

Pain starts showing:

- "Which server has which version?"
- "Why does prod behave differently from staging?"
- "Why are we patching machines manually?"
- "Why did this deployment break one node but not the others?"

At this point, the team wants:

- immutable deployments
- predictable runtime packaging
- easier rollback
- easier autoscaling
- less server-level operational work

That usually leads to **ECS**.

Why?

- package app as Docker images
- deployment becomes image-based, not server-script-based
- Fargate removes server/node management entirely
- good AWS-native integration with ALB, IAM, CloudWatch, Secrets Manager
- lower operational overhead than Kubernetes

Now the team runs:

- Spring Boot container as an ECS service
- maybe React as a separate Nginx container or, more commonly, React static files on S3 + CloudFront
- deployments through ECR + ECS service updates

This is the point where many companies stop. They do not need EKS.

## 2.3 Phase 3: "We are now a platform team, not just an app team"

The company grows.

- many microservices
- different teams
- shared platform capabilities
- sidecars, operators, service mesh, Helm charts, GitOps, policy enforcement
- maybe some multi-cloud or Kubernetes standardization requirement

Now the company says:

"We want Kubernetes because the ecosystem itself matters to us."

That leads to **EKS**.

Why?

- Kubernetes APIs and ecosystem
- rich scheduling and workload primitives
- Helm, CRDs, operators, admission controls
- standardized platform across many teams
- advanced autoscaling and workload policies

But the cost is real:

- more complexity
- more moving parts
- more cluster expertise required
- more failure modes
- more responsibility even though AWS manages the control plane

So the story is not:

"EC2 is old, ECS is better, EKS is best."

The real story is:

- EC2 is simplest when you want server control
- ECS is most pragmatic for AWS-native container operations
- EKS is correct when Kubernetes capabilities are truly needed

## 2.4 Visual Timeline of a Typical Team's Evolution

```text
  Month 1-3           Month 4-9           Month 12+
  ─────────           ─────────           ─────────
  "Just ship it"      "Stabilize ops"     "Platform thinking"

  ┌──────────┐       ┌──────────┐        ┌──────────┐
  │   EC2    │  ───> │   ECS    │  ───>  │   EKS    │
  │          │       │ (Fargate)│        │          │
  └──────────┘       └──────────┘        └──────────┘

  JAR on server       Docker images       K8s manifests
  systemd             Task definitions    Helm/operators
  Manual deploy       ECR + CI/CD         GitOps
  SSH debugging       CloudWatch logs     Service mesh
  1-2 services        3-10 services       10-50+ services
  1 team              2-3 teams           Platform team

  Ops burden: LOW     Ops burden: MEDIUM  Ops burden: HIGH
  (at first)          (but controlled)    (but powerful)

  ⚠ This is not a mandatory progression.
  Many companies stay at ECS permanently. That is valid.
  Move to EKS only when K8s capabilities are truly needed.
```

---

# 3. Starting Point: Your Local Application

Let us assume your laptop currently has something like this:

```text
my-app/
  backend/
    src/main/java/...
    pom.xml
    Dockerfile           (maybe not yet)
  frontend/
    src/...
    package.json
    Dockerfile           (maybe not yet)
  docker-compose.yml     (maybe not yet)
  README.md
```

The backend is a Spring Boot application.

- exposes REST APIs
- connects to PostgreSQL
- maybe uses Redis
- may use environment variables for DB credentials, JWT secret, API keys

The frontend is React.

- calls backend APIs
- gets built into static assets
- may need environment-specific API base URL

Before AWS, it likely runs as:

- backend on `localhost:8080`
- frontend on `localhost:3000` or `5173`
- local DB in Docker or installed locally

That local setup is good for development, but AWS introduces questions local machines hide:

- where does the database live?
- how does the frontend find the backend?
- where are secrets stored?
- how do you deploy safely?
- how do you scale?
- how do you recover from instance or container loss?

---

# 4. What Must Be Done Before Any AWS Deployment

This section is critical. Whether you choose EC2, ECS, or EKS, these preparations matter.

## 4.1 Separate Build-Time and Run-Time Configuration

Your app should not hardcode environment details.

Backend examples:

- `SPRING_PROFILES_ACTIVE`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `REDIS_HOST`

Frontend examples:

- API base URL
- feature flags
- analytics IDs

The goal:

- local, staging, and prod should use the same code artifact
- only config should change

### How This Looks in Spring Boot

```yaml
# application.yml — single file, environment-driven
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:myapp}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

server:
  port: ${SERVER_PORT:8080}

app:
  jwt-secret: ${JWT_SECRET:dev-secret-do-not-use-in-prod}
```

This means:

- locally it falls back to `localhost` defaults
- on AWS you inject real values via environment variables, Secrets Manager, or Parameter Store
- the same JAR or Docker image works everywhere

### How This Looks in React

Create a `.env.production` file:

```text
REACT_APP_API_BASE_URL=https://api.yourapp.com
```

Or for Vite:

```text
VITE_API_BASE_URL=https://api.yourapp.com
```

Then in code:

```javascript
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
```

This URL changes per environment at build time.

## 4.2 Decide How the Frontend Will Be Served

For a React SPA, there are two common models:

### Model A: Static Hosting

- build React into static assets
- upload to S3
- serve through CloudFront

This is usually the best model for a normal SPA.

### Model B: Frontend Container

- build React app
- copy static output into Nginx image
- run that container on EC2, ECS, or EKS

This is valid if:

- the organization wants everything in containers
- frontend is SSR or has server logic
- deployment model is standardized around containers

For a plain React SPA, static hosting is usually simpler and cheaper.

## 4.3 Externalize State

Do not keep important state inside the app container or inside one EC2 machine.

Use managed services where possible:

- RDS for PostgreSQL/MySQL
- ElastiCache for Redis
- S3 for file storage
- SQS/EventBridge for asynchronous workflows

If you keep state inside your compute layer, scaling and recovery become hard.

## 4.4 Containerize the Backend Even If You Start with EC2

Even if your first deployment is EC2, having a Docker image is a strong move.

Why?

- consistent runtime
- easier migration to ECS/EKS later
- easier local parity
- easier CI/CD

### Backend Dockerfile (Spring Boot)

```dockerfile
# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline          # cache dependencies
COPY src ./src
RUN mvn clean package -DskipTests      # produce JAR

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Why multi-stage?

- build tools stay out of the runtime image
- final image is smaller and more secure
- no source code ships to production

### Frontend Dockerfile (React/Vite with Nginx)

```dockerfile
# ---- Build Stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build                       # produces /app/dist or /app/build

# ---- Serve Stage ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

The `nginx.conf` should handle SPA routing:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;   # SPA fallback
    }

    location /api/ {
        proxy_pass http://backend-host:8080;  # only if co-located
    }
}
```

Remember: if React is hosted on S3 + CloudFront, you do not need this frontend Dockerfile at all.

### docker-compose.yml for Local Development

```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: myapp
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      REDIS_HOST: redis
    depends_on:
      - db
      - redis

  frontend:
    build: ./frontend
    ports:
      - "3000:80"

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine

volumes:
  pgdata:
```

This gives your team a one-command local environment that mirrors production structure.

## 4.5 Add Health Endpoints

Spring Boot should expose health endpoints, ideally through Actuator.

You want at least:

- liveness-style signal: process is alive
- readiness-style signal: app can serve traffic

This matters because ALB, ECS, and Kubernetes all depend on health checks.

### Spring Boot Actuator Setup

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Add to `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true          # enables /actuator/health/liveness and /readiness
      show-details: never      # do not leak internals publicly
  server:
    port: ${MANAGEMENT_PORT:8080}  # can run on separate port for security
```

Now you have:

- `/actuator/health` — general health
- `/actuator/health/liveness` — Kubernetes liveness / ALB basic check
- `/actuator/health/readiness` — Kubernetes readiness / ALB deep check

### How Each Platform Uses These

| Platform | Health endpoint used | Configured where |
|---|---|---|
| ALB | `/actuator/health` | Target group health check settings |
| ECS | `/actuator/health` | Task definition `healthCheck` or ALB target group |
| EKS | `/actuator/health/liveness` and `/readiness` | Pod `livenessProbe` and `readinessProbe` |

## 4.6 Handle Database Migrations

This is often forgotten until the first production deployment breaks.

Your schema must evolve with your code. Use a migration tool.

### Flyway (most common with Spring Boot)

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Place SQL migrations in `src/main/resources/db/migration/`:

```text
V1__create_users_table.sql
V2__add_email_column.sql
V3__create_orders_table.sql
```

Spring Boot auto-runs Flyway on startup. Migrations are versioned and tracked.

Why this matters for AWS:

- on EC2, the first instance to start runs migrations
- on ECS, only one task should run migrations before others start (use init containers or a pre-deploy task)
- on EKS, use a Kubernetes Job or init container for migrations before the main Deployment rolls out

Never manually run DDL scripts against production via SSH.

## 4.7 Create a CI/CD Baseline

At minimum your pipeline should:

- run unit tests
- build backend artifact or image
- build frontend assets or image
- scan dependencies and images if possible
- publish artifact
- deploy to target environment

The deployment target changes by EC2/ECS/EKS, but the discipline should be there from day one.

---

# 5. Path 1: Deploying with EC2

EC2 means you are deploying onto virtual machines.

You manage:

- OS
- package/runtime installation
- process management
- patching
- scaling policy
- deployment scripts or deployment tooling

## 5.1 Story Version of EC2

The team says:

"We understand Linux servers. We want direct control. We are okay managing instances. We need the shortest path from local app to production."

So they choose EC2.

## 5.2 Recommended EC2 Architecture for Spring Boot + React

### Option 1: Pragmatic Production Pattern

```text
Users
  ->
Route 53
  ->
CloudFront
  ->
S3 (React static files)

API calls
  ->
ALB
  ->
EC2 Auto Scaling Group in private subnets
  ->
Spring Boot service
  ->
RDS / ElastiCache / S3
```

This is often the best EC2-based architecture.

Reason:

- React does not need a VM if it is a static SPA
- backend uses EC2 where server control matters
- ALB gives health checks and multi-instance traffic distribution

### Option 2: Everything on EC2

```text
Users
  ->
ALB
  ->
EC2 instances running:
     - Nginx serving React build
     - Spring Boot JAR or Docker container
  ->
RDS
```

This works but is usually less clean than putting React on S3 + CloudFront.

## 5.3 What You Actually Do from Local Code

### Step 1: Build the frontend

You create a production build:

- `npm install`
- `npm run build`

This generates static files like:

- HTML
- CSS
- JS bundles

### Step 2: Build the Spring Boot backend

You package the backend:

- Maven: `mvn clean package`
- Gradle: `./gradlew build`

Now you have a JAR file.

### Step 3: Create the EC2 machine image strategy

You have two ways:

#### Basic way

- launch instance
- SSH in
- install Java, Nginx, maybe Docker
- copy files
- configure systemd service

This is okay for learning, not ideal for mature production.

#### Better way

- use a launch template
- use user data or baked AMIs
- automate setup
- let Auto Scaling create identical instances

This is the professional pattern.

### Step 4: Run Spring Boot as a service

Typical pattern:

- create Linux user for app
- place JAR under app directory
- create systemd unit
- inject environment variables
- start service on boot

#### Actual systemd Unit File

```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My Spring Boot Application
After=network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/myapp/app.jar
EnvironmentFile=/opt/myapp/.env
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

The `/opt/myapp/.env` file holds runtime config:

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=myapp-db.xxxx.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=myapp
DB_USERNAME=myapp_user
DB_PASSWORD=retrieved-from-secrets-manager
JWT_SECRET=retrieved-from-secrets-manager
```

In production, prefer fetching secrets at startup from Secrets Manager rather than storing them in a file. You can use a bootstrap script that calls `aws secretsmanager get-secret-value` and writes the env file before starting the service.

#### Enable and Start

```bash
sudo systemctl daemon-reload
sudo systemctl enable myapp.service
sudo systemctl start myapp.service
sudo systemctl status myapp.service
# View logs
journalctl -u myapp.service -f
```

Now EC2 behaves like a stable service host.

### Step 5: Put ALB in front

ALB does:

- HTTPS termination
- routing
- health checks
- traffic distribution across instances

If you run multiple backend instances, ALB targets them and only routes to healthy ones.

### Step 6: Move backend into private subnets

Good production design:

- ALB in public subnets
- EC2 app instances in private subnets
- no public IPs on backend instances
- access outbound internet via NAT Gateway if needed

### Step 7: Use RDS for the database

Do not keep PostgreSQL on the same EC2 server as your backend unless this is just a demo.

Use RDS so that:

- database lifecycle is separate from app lifecycle
- backups are easier
- high availability is possible
- scaling and patching improve

## 5.4 How Deployment Works on EC2

There are several models.

### Model A: SSH-based deployment

- copy new JAR or Docker image to server
- restart service

Simple, but risky and less reproducible.

### Model B: Blue-Green with Auto Scaling Groups

- create new launch template version or AMI
- start new instances with new app version
- ALB health checks validate them
- shift traffic
- drain old instances

This is much safer.

## 5.5 How Scaling Works on EC2

Scaling is at the instance level.

You use Auto Scaling Groups and define:

- min instances
- desired instances
- max instances
- scaling triggers

Triggers can be:

- CPU usage
- memory via CloudWatch custom metrics
- request count per target
- queue depth

Important caveat:

If your app stores user session state locally on one instance, horizontal scaling becomes painful.

Better patterns:

- stateless backend
- JWT or distributed session store
- Redis for shared session/cache state if needed

## 5.6 Security on EC2

You must think about:

- security groups for ALB and EC2
- IAM role attached to EC2 instances
- secret retrieval from Secrets Manager or Parameter Store
- OS patching
- SSH minimization or elimination via SSM Session Manager

A mature answer is:

"I avoid broad SSH access and prefer SSM Session Manager, private subnets, least-privilege security groups, and instance IAM roles instead of static credentials."

## 5.7 Operational Burden on EC2

This is the real trade-off.

You still own:

- machine patching
- JVM/runtime upgrades
- disk management
- instance replacement strategy
- deployment tooling
- log shipping and monitoring agents

EC2 is powerful because it gives control.
EC2 is expensive in effort because it gives control.

## 5.8 When EC2 Is the Right Answer

Use EC2 when:

- you need full OS/runtime control
- you have a legacy or non-container-ready stack
- your team is comfortable with VM operations
- compliance or agent installation requires machine-level access
- the app is simple enough that container orchestration is unnecessary

---

# 6. Path 2: Deploying with ECS

ECS means your unit of deployment becomes the **container**, not the machine.

You still think about compute, but at a higher abstraction level.

With ECS on Fargate, you do not manage servers at all.

## 6.1 Story Version of ECS

The team says:

"We want the consistency of containers and easier deployments, but we do not want Kubernetes complexity."

That is a classic ECS team.

## 6.2 Best ECS Architecture for Spring Boot + React

### Recommended Architecture

```text
Users
  ->
Route 53
  ->
CloudFront
  ->
S3 (React static site)

API calls
  ->
ALB
  ->
ECS Service (Spring Boot containers on Fargate or EC2)
  ->
RDS / ElastiCache / S3 / SQS
```

This is the cleanest model for most business applications.

### Alternative: Frontend Also on ECS

```text
Users
  ->
ALB
  ->
ECS service 1: Nginx serving React build
ECS service 2: Spring Boot API
```

This is fine if the team wants full container standardization.

## 6.3 What Changes from Local Development

Now you create Docker images.

### Backend container

The backend Docker image usually:

- starts from a JDK/JRE base image
- copies the Spring Boot JAR
- exposes port 8080
- starts the app with `java -jar`

### Frontend container, if containerized

The frontend Docker image usually:

- builds the React app in one stage
- copies the static output into Nginx in another stage
- exposes port 80

This is where your local code becomes deployable infrastructure artifacts.

## 6.4 Core ECS Concepts You Must Understand

- **Cluster**: logical place where ECS workloads run
- **Task Definition**: blueprint of container config
- **Task**: running container set from task definition
- **Service**: keeps desired number of tasks alive
- **Launch Type**: EC2 or Fargate

For a Spring Boot API, a task definition includes:

- image URI from ECR
- CPU and memory
- port mapping
- environment variables
- secrets
- logging config
- task IAM role

## 6.5 ECS with Fargate vs ECS with EC2

### ECS with Fargate

Use when:

- you want minimal ops
- app is stateless
- team wants containers without node management

Benefits:

- no EC2 fleet management
- no patching of worker nodes by you
- simple scaling model

Trade-offs:

- less low-level control
- can cost more than EC2 at sustained large scale

### ECS with EC2

Use when:

- you want ECS scheduling but also node-level control
- you have special agent/runtime needs
- you want to optimize cost on large steady workloads

Trade-off:

- now you are back to managing worker instances

For most app teams, **ECS on Fargate** is the default starting point.

## 6.6 Step-by-Step Journey from Local Code to ECS

### Step 1: Write Dockerfiles

You containerize backend and optionally frontend.

### Step 2: Push images to ECR

ECR is your private container registry.

Typical flow:

- build image locally or in CI
- tag image
- push image to ECR

#### Actual ECR Commands

```bash
# 1. Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# 2. Create repository (first time only)
aws ecr create-repository --repository-name myapp-backend

# 3. Build the image
docker build -t myapp-backend:latest ./backend

# 4. Tag for ECR
docker tag myapp-backend:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp-backend:v1.0.0

# 5. Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp-backend:v1.0.0
```

In CI/CD, this is automated. The version tag usually comes from the git commit SHA or a semantic version.

Now AWS has a versioned artifact to deploy.

### Step 3: Create the ECS task definition

For Spring Boot, define:

- container image
- port 8080
- memory and CPU
- health check path or container health command
- environment variables and secrets
- CloudWatch logs configuration

#### Actual Task Definition (JSON)

```json
{
  "family": "myapp-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789012:role/myapp-backend-task-role",
  "containerDefinitions": [
    {
      "name": "myapp-backend",
      "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp-backend:v1.0.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" },
        { "name": "DB_HOST", "value": "myapp-db.xxxx.us-east-1.rds.amazonaws.com" },
        { "name": "DB_PORT", "value": "5432" },
        { "name": "DB_NAME", "value": "myapp" }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:myapp/db-password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:myapp/jwt-secret"
        }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/myapp-backend",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "essential": true
    }
  ]
}
```

Key details to notice:

- `executionRoleArn` lets ECS pull the image and push logs
- `taskRoleArn` lets the application itself call AWS services like S3 or SQS
- `secrets` pulls values from Secrets Manager at task startup — they appear as environment variables in the container
- `healthCheck` keeps unhealthy containers from receiving traffic
- `startPeriod` gives Spring Boot time to initialize before health checks begin failing

### Step 4: Create the ECS service

The ECS service ensures:

- desired number of tasks is running
- failed tasks are replaced
- deployment rollout is managed

### Step 5: Attach to ALB

ALB routes traffic to the ECS service.

If multiple services exist:

- `/api/*` -> Spring Boot service
- `/` -> frontend service if frontend is containerized

Or, if React is on S3 + CloudFront:

- browser loads frontend from CloudFront
- frontend calls backend API domain served by ALB

### Step 6: Put tasks in private subnets

Best practice:

- ALB public
- ECS tasks private
- RDS private

The tasks do not need public inbound reachability.

### Step 7: Configure service autoscaling

ECS can scale tasks based on:

- CPU
- memory
- ALB request count per target
- custom CloudWatch metrics

This is one of the biggest gains over hand-managed EC2 deployment.

## 6.7 How Deployment Works on ECS

Deployment flow:

```text
Code change
  ->
CI builds Docker image
  ->
image pushed to ECR
  ->
ECS service updated to new task definition
  ->
new tasks start
  ->
ALB health checks pass
  ->
old tasks drained and removed
```

This is much more reproducible than copying JARs to VMs.

## 6.8 Security on ECS

Key ideas:

- task IAM roles, not static AWS keys
- Secrets Manager / Parameter Store for secrets
- security groups attached to tasks or ENIs depending on mode
- private subnets for backend tasks

One particularly important concept:

- **task role** gives AWS permissions to the running application
- **execution role** allows ECS to pull image and send logs

Do not confuse these two.

## 6.9 Operational Burden on ECS

Compared with EC2, ECS removes or reduces:

- app packaging inconsistency
- server snowflake problems
- host-level deployment complexity
- some scaling complexity

With Fargate, it also removes node management.

But you still own:

- container image quality
- task sizing
- deployment safety
- secrets handling
- app observability
- database architecture

## 6.10 When ECS Is the Right Answer

Use ECS when:

- you want containers without Kubernetes overhead
- you are AWS-first
- your platform needs are moderate, not highly customized
- your team wants faster operational maturity than raw EC2

For many companies, ECS is the best practical answer for Spring Boot microservices on AWS.

---

# 7. Path 3: Deploying with EKS

EKS means you are now operating on top of Kubernetes.

AWS manages the control plane, but you still operate substantial platform complexity.

## 7.1 Story Version of EKS

The company says:

"We need Kubernetes itself, not just containers. We want standardized platform abstractions across many services and teams."

That is the right reason to choose EKS.

The wrong reason is:

"Kubernetes is popular."

## 7.2 Recommended EKS Architecture for Spring Boot + React

### Most Practical Model

```text
Users
  ->
Route 53
  ->
CloudFront
  ->
S3 (React static site)

API calls
  ->
ALB Ingress
  ->
EKS Deployment/Service for Spring Boot
  ->
RDS / ElastiCache / S3 / SQS
```

### Full Kubernetes Model

```text
Users
  ->
ALB Ingress Controller / AWS Load Balancer Controller
  ->
Frontend Deployment + Service
  ->
Backend Deployment + Service
  ->
Stateful external services
```

Again, for a pure React SPA, S3 + CloudFront is usually simpler than running frontend pods.

## 7.3 What You Need Beyond Containers

EKS requires Kubernetes resources such as:

- Namespace
- Deployment
- Service
- Ingress
- ConfigMap
- Secret
- HorizontalPodAutoscaler
- ServiceAccount

If your team is not comfortable with these concepts, EKS will slow you down.

## 7.4 Step-by-Step Journey from Local Code to EKS

### Step 1: Containerize the applications

Same as ECS.

You still build Docker images and push to ECR.

### Step 2: Create Kubernetes manifests or Helm charts

For Spring Boot backend, you define:

- Deployment with replica count
- Service exposing pods internally
- Ingress for external HTTP routing
- ConfigMap for non-secret configuration
- Secret or external secret integration for credentials

#### Actual Kubernetes Manifests

**Namespace:**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: myapp
```

**ConfigMap — non-secret config:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: myapp-backend-config
  namespace: myapp
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DB_HOST: "myapp-db.xxxx.us-east-1.rds.amazonaws.com"
  DB_PORT: "5432"
  DB_NAME: "myapp"
```

**Secret (or use External Secrets Operator to pull from Secrets Manager):**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: myapp-backend-secrets
  namespace: myapp
type: Opaque
stringData:
  DB_PASSWORD: "your-db-password"      # in practice, sealed or external
  JWT_SECRET: "your-jwt-secret"
```

**ServiceAccount with IRSA:**

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: myapp-backend-sa
  namespace: myapp
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/myapp-backend-role
```

**Deployment:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-backend
  namespace: myapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp-backend
  template:
    metadata:
      labels:
        app: myapp-backend
    spec:
      serviceAccountName: myapp-backend-sa
      containers:
        - name: myapp-backend
          image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp-backend:v1.0.0
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: myapp-backend-config
            - secretRef:
                name: myapp-backend-secrets
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1024Mi"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
```

**Service:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: myapp-backend
  namespace: myapp
spec:
  selector:
    app: myapp-backend
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

**Ingress (using AWS Load Balancer Controller):**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-backend-ingress
  namespace: myapp
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:123456789012:certificate/xxxxx
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
spec:
  rules:
    - host: api.yourapp.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: myapp-backend
                port:
                  number: 80
```

**HorizontalPodAutoscaler:**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: myapp-backend-hpa
  namespace: myapp
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: myapp-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

This is significantly more YAML than ECS requires. That is the trade-off.

### Step 3: Create the EKS cluster and worker capacity

You choose:

- managed node groups
- self-managed nodes
- Fargate profiles for some pods

Managed node groups are the usual default.

### Step 4: Install ingress and cluster add-ons

Typical EKS platform components:

- AWS Load Balancer Controller
- metrics server
- external-dns maybe
- cluster autoscaler or Karpenter
- logging/monitoring stack
- external secrets solution maybe

This is where EKS becomes a platform, not just a deployment target.

### Step 5: Deploy the Spring Boot app

The Deployment ensures the desired number of pods.

Kubernetes rolling updates can:

- create new pods
- wait for readiness
- gradually terminate old pods

This is conceptually similar to ECS service deployment but with more knobs and more complexity.

### Step 6: Expose externally through Ingress

On AWS, a common pattern is:

- Kubernetes Ingress resource
- AWS Load Balancer Controller provisions ALB
- ALB routes to Kubernetes services

### Step 7: Add autoscaling

You can scale at multiple levels:

- Horizontal Pod Autoscaler scales pod count
- Cluster Autoscaler or Karpenter scales nodes
- VPA may adjust resource requests in some setups

This flexibility is powerful, but it requires disciplined resource tuning.

## 7.5 Security on EKS

Security becomes broader.

You now manage:

- Kubernetes RBAC
- namespace isolation
- pod security posture
- image policies
- network policies if used
- IAM integration for workloads

The key AWS concept here is **IRSA** or the newer **EKS Pod Identity**.

For example:

- backend pod needs S3 read/write
- instead of static keys, bind pod identity to IAM permissions

This is the Kubernetes equivalent of good task/instance role hygiene.

## 7.6 Operational Burden on EKS

This is where many teams underestimate the cost.

Even with managed control plane, you still own:

- node upgrades
- cluster add-ons
- ingress controller behavior
- resource requests/limits
- pod disruption policies
- DNS and certificate integration
- observability stack
- security policies
- upgrade testing across Kubernetes versions

EKS is not "ECS but more advanced." It is a different operational category.

## 7.7 When EKS Is the Right Answer

Use EKS when:

- Kubernetes expertise exists or is strategically important
- multiple teams need a shared platform model
- you need Helm, operators, CRDs, advanced scheduling, or broader K8s ecosystem tools
- platform portability matters

If your goal is simply "run Spring Boot containers on AWS," EKS is often unnecessary.

---

# 8. How the React Frontend Fits in Each Model

This needs separate attention because teams often overcomplicate frontend hosting.

## 8.1 Best Default for a React SPA

Best default:

- build React static files
- host on S3
- serve through CloudFront

Why?

- cheap
- scalable
- globally cacheable
- no app servers needed
- clean separation from backend compute choice

Then choose EC2, ECS, or EKS only for the backend API.

## 8.2 When to Put Frontend on EC2/ECS/EKS

You may run frontend on compute if:

- you use SSR framework behavior
- you need runtime rendering or middleware
- organizational deployment standards require containerized frontend
- you want same deployment pattern for frontend and backend

## 8.3 Practical Recommendation

For your scenario, a strong default architecture is:

```text
React -> S3 + CloudFront
Spring Boot API -> EC2 or ECS or EKS
Database -> RDS
Files -> S3
Cache -> ElastiCache
Secrets -> Secrets Manager
DNS -> Route 53
```

This lets you compare EC2/ECS/EKS mainly for the backend layer where the compute trade-off actually matters.

## 8.4 Solving the Frontend-Backend Connectivity Problem

This is a gap many guides skip. When React is on CloudFront and the API is on ALB, they are on different domains. That means **CORS** (Cross-Origin Resource Sharing) becomes relevant.

### The Problem

```text
React on:  https://app.yourcompany.com   (CloudFront)
API on:    https://api.yourcompany.com   (ALB)

Browser makes a request from app.yourcompany.com to api.yourcompany.com.
This is a cross-origin request.
Browser blocks it unless the API explicitly allows it via CORS headers.
```

### Solution 1: CORS in Spring Boot (Most Common)

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "https://app.yourcompany.com",
                    "http://localhost:3000"           // local dev
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

Make the allowed origins configurable via environment variable for different environments:

```yaml
# application.yml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

### Solution 2: Same Domain via CloudFront (Avoids CORS Entirely)

An elegant alternative: put both frontend and API behind the **same CloudFront distribution**.

```text
https://app.yourcompany.com/         -> CloudFront -> S3 (React)
https://app.yourcompany.com/api/*    -> CloudFront -> ALB (Spring Boot)
```

CloudFront path-based routing:

- Default behavior: serves S3 static files
- `/api/*` behavior: forwards to ALB origin

Benefits:

- no CORS needed (same origin)
- single TLS certificate
- CloudFront handles both static assets and API proxying

Trade-off:

- slightly more CloudFront configuration
- API responses pass through CloudFront (ensure caching is disabled for API paths)

### Solution 3: API Gateway Custom Domain

If using API Gateway instead of ALB:

- set custom domain `api.yourcompany.com` on API Gateway
- API Gateway handles CORS configuration natively

### Recommendation

For most teams: **Solution 2** (same CloudFront distribution) is cleanest.
If separate domains are required: **Solution 1** (Spring Boot CORS config) works fine.

---

# 9. Networking, Security, Scaling, and Observability Across All Three

No matter which compute path you choose, these patterns remain important.

## 9.1 VPC Layout

Good baseline:

- two or more AZs
- public subnets for ALB and maybe NAT Gateway
- private subnets for app compute
- private subnets for database

This reduces exposure and improves resilience.

## 9.2 Security Pattern

Minimum good pattern:

- HTTPS at ALB
- backend compute in private subnets
- security groups with least privilege
- IAM roles for workloads
- secrets in Secrets Manager or Parameter Store
- no hardcoded credentials

## 9.3 Scaling Pattern

- EC2 scales by adding/removing instances
- ECS scales by adding/removing tasks
- EKS scales by adding/removing pods and maybe nodes

The more abstract the platform, the more granular the scaling unit becomes.

## 9.4 Logging and Metrics

You need:

- application logs
- request metrics
- error rates
- JVM metrics
- health check insight
- tracing if possible

Typical AWS-friendly observability:

- CloudWatch logs
- CloudWatch metrics/alarms
- OpenTelemetry + managed tracing stack if available
- dashboards for latency, error rate, CPU, memory, saturation

## 9.5 Deployment Safety

No matter what you use:

- prefer rolling or blue-green over ad-hoc restarts
- verify readiness checks
- keep artifacts immutable
- have rollback strategy

## 9.6 Service Discovery

When you have multiple backend services that need to find each other:

| Platform | Service Discovery Method | How It Works |
|---|---|---|
| EC2 | ALB/Route 53 or manual config | Services discover each other via DNS or load balancer endpoints |
| ECS | AWS Cloud Map | ECS auto-registers tasks into Cloud Map. Other services query DNS names like `backend.myapp.local` |
| EKS | Kubernetes DNS (CoreDNS) | Services get stable DNS names like `myapp-backend.myapp.svc.cluster.local` automatically |

This matters once you move beyond a single backend service.

---

# 10. Cost Comparison: EC2 vs ECS vs EKS

This is a question interviewers and managers both ask. Numbers below are approximate US East 1 prices to build intuition.

## 10.1 Base Platform Cost

```text
EC2:
  No platform fee. You pay only for the instances you run.
  t3.medium (2 vCPU, 4 GB): ~$30/month On-Demand

ECS on Fargate:
  No cluster fee. You pay per task vCPU + memory.
  0.5 vCPU, 1 GB task running 24/7: ~$18/month
  1 vCPU, 2 GB task running 24/7: ~$36/month

ECS on EC2:
  Same as EC2 instance pricing. ECS scheduling is free.

EKS:
  $0.10/hour for control plane = ~$73/month (just for the cluster existing)
  + worker node costs (EC2 instances or Fargate)
  So EKS always has a baseline cost even before any workloads run.
```

## 10.2 Realistic Small App Cost (2 backend replicas + database)

```text
EC2 path:
  2x t3.medium                = ~$60/month
  ALB                         = ~$22/month
  RDS db.t3.micro             = ~$15/month
  NAT Gateway                 = ~$35/month
  Total                       ≈ $132/month

ECS Fargate path:
  2 tasks (0.5 vCPU, 1 GB)    = ~$36/month
  ALB                         = ~$22/month
  RDS db.t3.micro             = ~$15/month
  NAT Gateway                 = ~$35/month
  Total                       ≈ $108/month

EKS path:
  EKS control plane           = ~$73/month
  2x t3.medium worker nodes   = ~$60/month
  ALB                         = ~$22/month
  RDS db.t3.micro             = ~$15/month
  NAT Gateway                 = ~$35/month
  Total                       ≈ $205/month
```

## 10.3 Cost Insight

```text
Key takeaway:

  ECS Fargate is often cheapest for small-medium workloads.
  EC2 becomes cheaper at scale when you use Reserved Instances or Savings Plans.
  EKS has a fixed overhead ($73/month) that only makes sense when many services share the cluster.

  A single-service team paying $73/month just for the K8s control plane
  when ECS has zero platform fee — that is a common cost mistake.

  EKS cost justification usually starts at 5-10+ services sharing one cluster.
```

## 10.4 Hidden Costs to Watch

- **NAT Gateway**: $32/month + $0.045/GB. Use VPC endpoints for S3/DynamoDB/Secrets Manager.
- **Cross-AZ traffic**: $0.02/GB round trip. Co-locate chatty services or use caching.
- **ALB proliferation**: Each ALB costs ~$22/month minimum. Consolidate with path-based routing.
- **ECR storage**: $0.10/GB/month. Clean up old images with lifecycle policies.
- **CloudWatch logs**: $0.50/GB ingested. Log wisely, not everything.

---

# 11. Migration Journey: EC2 -> ECS -> EKS

This progression matters because many teams do not start with the final platform.

## 11.1 Local -> EC2

Best when:

- learning phase
- small team
- simple deployment needs
- server familiarity is high

What improves first:

- external DB
- ALB
- Auto Scaling Group
- proper health checks

## 11.2 EC2 -> ECS

This is the most common maturity jump.

What usually drives it:

- server drift
- painful deployments
- need for immutable artifacts
- desire for autoscaling and faster rollout

What changes technically:

- JAR deployment becomes Docker image deployment
- ASG-centric thinking becomes service/task-centric thinking
- host-level config becomes task definition config

## 11.3 ECS -> EKS

This should happen only when platform needs justify it.

Drivers may include:

- many teams and many services
- Kubernetes standardization
- operator/CRD ecosystem need
- advanced traffic and policy controls
- GitOps or service mesh adoption

If those are absent, staying on ECS is usually the better decision.

---

# 12. How to Choose in Real Life

## 12.1 Choose EC2 If

- you need OS-level control
- app is not yet containerized and you need speed of first deployment
- team is strong in Linux/VM ops and weak in containers/orchestration
- workload needs custom agents, drivers, or machine-level tuning

## 12.2 Choose ECS If

- you want containers with low operational overhead
- you are AWS-centric
- team does not want Kubernetes complexity
- you want the best balance of simplicity and operational maturity

## 12.3 Choose EKS If

- Kubernetes is a platform requirement
- multiple teams need advanced orchestration capabilities
- your org already operates K8s well
- ecosystem flexibility matters more than simplicity

## 12.4 My Practical Recommendation for Your Spring Boot + React App

If you are starting from local code today and want a strong, realistic AWS path:

### Recommendation order

1. Put React on S3 + CloudFront
2. Put PostgreSQL on RDS
3. Containerize Spring Boot
4. Deploy Spring Boot on ECS Fargate unless you have a strong reason for EC2 or EKS

Why this is strong:

- low ops
- clean architecture
- good scalability
- simple rollback path
- easy to explain in interviews

Choose EC2 instead if you explicitly need machine control.
Choose EKS instead if Kubernetes is truly required by the org/platform.

---

# 13. Interview-Ready Answer Framework

If an interviewer asks:

"You have a local Spring Boot backend and React frontend. How would you leverage EC2, ECS, or EKS on AWS?"

Here is the structure of a strong answer.

## 13.1 Start With Workload Framing

Say:

```text
First I would separate frontend hosting, backend compute, and stateful services.
For a React SPA, I would usually host static assets on S3 + CloudFront.
For the Spring Boot API, I would choose EC2, ECS, or EKS based on the required level of control versus operational complexity.
Database would be externalized to RDS, cache to ElastiCache if needed, and secrets to Secrets Manager.
```

## 13.2 Then Compare the Three Clearly

Say:

```text
If I need full server control or have legacy constraints, I would use EC2 behind an ALB with Auto Scaling Groups.

If I want containers with the lowest operational burden on AWS, I would containerize Spring Boot, push the image to ECR, and run it on ECS Fargate behind an ALB.

If the organization is standardized on Kubernetes and needs Helm, operators, or advanced workload policies, I would run the same container on EKS with Deployments, Services, and ALB-backed Ingress.
```

## 13.3 Then Finish With Trade-Offs

Say:

```text
My default recommendation for a normal business application would be React on S3/CloudFront and Spring Boot on ECS Fargate, because it gives container benefits without Kubernetes overhead. I would move to EC2 for machine control, or EKS when Kubernetes capabilities are a real platform requirement.
```

That is a senior answer because it is not tool-recitation. It is architecture reasoning.

---

# 14. Final Revision Sheet

## EC2 in One Line

Run your app on VMs when you need maximum control and accept the operational cost of managing machines.

## ECS in One Line

Run your app as containers in an AWS-native way when you want strong operational simplicity without Kubernetes.

## EKS in One Line

Run your app on Kubernetes when the Kubernetes ecosystem and platform capabilities themselves are required.

## What Changes as You Move EC2 -> ECS -> EKS

| Dimension | EC2 | ECS | EKS |
|---|---|---|---|
| Deployment unit | server or process | container task | pod/workload |
| You manage | OS + app | container + service config | Kubernetes platform + workloads |
| Scaling unit | instance | task | pod/node |
| Operational complexity | low to medium initially, higher over time | medium | high |
| Best fit | VM control | pragmatic AWS containers | Kubernetes platform needs |

## Gold Standard Architecture for Most Spring Boot + React Apps

```text
React SPA -> S3 + CloudFront
Spring Boot API -> ECS Fargate behind ALB
Database -> RDS
Secrets -> Secrets Manager
Files -> S3
Metrics/Logs -> CloudWatch
DNS -> Route 53
```

## Gold Standard Decision Sentence

```text
I choose EC2 when I need machine-level control, ECS when I want the most pragmatic AWS container platform, and EKS when Kubernetes capabilities are strategically necessary. For a standard Spring Boot API with a React SPA, my default would usually be React on S3/CloudFront and the backend on ECS Fargate unless a strong constraint pushes me toward EC2 or EKS.
```