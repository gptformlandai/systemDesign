# Kubernetes ConfigMaps, Secrets, and Environment Injection Gold Sheet

> Track: K8s Interview Track — Phase 2: Configuration and Storage
> Goal: Master configuration injection patterns, secret management, and how to avoid the most common security mistakes with K8s Secrets.

---

## 0. How To Read This

Beginner focus:
- What ConfigMaps and Secrets are
- Injecting config as env vars and volume mounts
- Basic Secret types

Intermediate focus:
- Immutable ConfigMaps and Secrets
- Secret rotation and live reload without pod restart
- Projected volumes
- External secret management (Vault, AWS Secrets Manager)

Senior / MAANG focus:
- Secrets are base64-encoded, not encrypted at rest by default
- etcd encryption at rest for Secrets
- External Secrets Operator and Secrets Store CSI Driver
- Secret rotation strategies at scale
- Preventing secret leakage in logs and environment dumps

---

# Topic 1: ConfigMap

## 1. Intuition

ConfigMap stores non-sensitive configuration data as key-value pairs. Decouples config from container images — same image, different config per environment.

```text
Without ConfigMap:
  dev-payment-service:latest  → hardcoded DB_HOST=dev-db
  prod-payment-service:latest → hardcoded DB_HOST=prod-db

With ConfigMap:
  payment-service:v1.2.3 (same image everywhere)
  dev ConfigMap:  DB_HOST=dev-db
  prod ConfigMap: DB_HOST=prod-db
```

## 2. Creating ConfigMaps

```yaml
# Literal values
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-service-config
  namespace: prod
data:
  DB_PORT: "5432"
  LOG_LEVEL: "info"
  CACHE_TTL: "300"
  APP_ENV: "production"
```

```yaml
# Multi-line config file (entire file as one value)
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-service-files
  namespace: prod
data:
  application.properties: |
    spring.datasource.port=5432
    spring.jpa.show-sql=false
    management.endpoints.web.exposure.include=health,info,metrics
  logback.xml: |
    <configuration>
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>%d{ISO8601} %level %msg%n</pattern></encoder>
      </appender>
      <root level="INFO"><appender-ref ref="STDOUT"/></root>
    </configuration>
```

CLI:
```bash
kubectl create configmap payment-config \
  --from-literal=DB_PORT=5432 \
  --from-literal=LOG_LEVEL=info

kubectl create configmap payment-files \
  --from-file=application.properties \
  --from-file=logback.xml
```

## 3. Injecting ConfigMap as Environment Variables

```yaml
spec:
  containers:
    - name: payment-service
      image: payment-service:v1.2.3
      # Inject all keys as env vars
      envFrom:
        - configMapRef:
            name: payment-service-config
      # Inject specific keys
      env:
        - name: DATABASE_PORT
          valueFrom:
            configMapKeyRef:
              name: payment-service-config
              key: DB_PORT
```

## 4. Injecting ConfigMap as Volume Mount

```yaml
spec:
  containers:
    - name: payment-service
      image: payment-service:v1.2.3
      volumeMounts:
        - name: config-volume
          mountPath: /etc/config      # each key becomes a file
  volumes:
    - name: config-volume
      configMap:
        name: payment-service-files
        # Optional: set permissions
        defaultMode: 0440
        # Optional: mount specific keys as specific file names
        items:
          - key: application.properties
            path: application.properties
```

Result:
```text
/etc/config/application.properties  ← content of the key
/etc/config/logback.xml             ← content of the key
```

## 5. Live Reload Without Restart

**Volume mount updates** are propagated to pods automatically (kubelet syncs every ~1 min):
```text
ConfigMap updated → kubelet detects change → file in mount updated
If app watches the file (inotify), it can reload without restart.

Spring Boot: @RefreshScope + Spring Cloud Config
NGINX: reads config file per request (no reload needed)
```

**Environment variable injection does NOT live-reload:**
```text
Environment variables are set at container start.
Changing the ConfigMap does NOT update running pod env vars.
Must restart pods (rolling update) to pick up changes.
```

## 6. Immutable ConfigMaps

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-service-config-v3
immutable: true    # prevents accidental modification; improves performance
data:
  VERSION: "v3"
```

Immutable ConfigMaps:
- Cannot be modified after creation
- Pods must be deleted/recreated to use a new version
- Reduces API server load (no watches for immutable objects)
- Best practice for version-pinned configs

---

# Topic 2: Secrets

## 1. What Secrets Are (and Aren't)

Secrets store sensitive data: passwords, tokens, certificates, API keys.

```text
Secret values are base64-encoded — NOT encrypted.

kubectl get secret my-secret -o jsonpath='{.data.password}' | base64 -d
→ shows the plaintext password

Secrets provide:
  ✅ Separation of config from images
  ✅ RBAC-controlled access (who can read which secret)
  ✅ Mounted as tmpfs (in-memory) volumes — not written to node disk
  ❌ NOT encryption (without etcd encryption at rest or external KMS)
```

## 2. Secret Types

| Type | Description |
|---|---|
| `Opaque` | arbitrary key-value data (most common) |
| `kubernetes.io/tls` | TLS certificate + key |
| `kubernetes.io/service-account-token` | auto-mounted by K8s for service accounts |
| `kubernetes.io/dockerconfigjson` | container registry credentials |
| `kubernetes.io/ssh-auth` | SSH private key |
| `kubernetes.io/basic-auth` | username + password |

## 3. Creating Secrets

```yaml
# Opaque secret (values must be base64)
apiVersion: v1
kind: Secret
metadata:
  name: payment-db-secret
  namespace: prod
type: Opaque
data:
  host: cGF5bWVudC1kYi5wcm9kLnJkcy5hd3MuY29t    # base64
  username: cGF5bWVudF91c2Vy                      # base64
  password: c3VwZXItc2VjcmV0LXBhc3N3b3Jk          # base64
```

```yaml
# Or use stringData (plaintext, K8s encodes automatically)
apiVersion: v1
kind: Secret
metadata:
  name: payment-db-secret
  namespace: prod
type: Opaque
stringData:
  host: payment-db.prod.rds.amazonaws.com
  username: payment_user
  password: super-secret-password    # stored as base64 in etcd
```

CLI:
```bash
kubectl create secret generic payment-db-secret \
  --from-literal=host=payment-db.prod.rds.amazonaws.com \
  --from-literal=username=payment_user \
  --from-literal=password=super-secret-password

# TLS secret
kubectl create secret tls myapp-tls \
  --cert=tls.crt \
  --key=tls.key

# Docker registry credentials
kubectl create secret docker-registry my-registry-credentials \
  --docker-server=my-registry.example.com \
  --docker-username=myuser \
  --docker-password=mypassword
```

## 4. Injecting Secrets

As environment variable (least secure — shows in `kubectl describe pod`):
```yaml
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: payment-db-secret
        key: password
```

As volume mount (preferred — not exposed in env var dumps):
```yaml
spec:
  containers:
    - name: payment-service
      volumeMounts:
        - name: db-credentials
          mountPath: /etc/secrets/db
          readOnly: true
  volumes:
    - name: db-credentials
      secret:
        secretName: payment-db-secret
        defaultMode: 0400    # read-only by owner only
```

Result:
```text
/etc/secrets/db/host      ← plaintext value
/etc/secrets/db/username  ← plaintext value
/etc/secrets/db/password  ← plaintext value
```

## 5. Immutable Secrets

```yaml
apiVersion: v1
kind: Secret
immutable: true    # prevents modification; also disables watches on this Secret
```

## 6. etcd Encryption At Rest

Secrets stored in etcd are base64-encoded by default (not encrypted). Enable encryption:

```yaml
# /etc/kubernetes/encryption-config.yaml
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
  - resources:
      - secrets
    providers:
      - aescbc:            # AES-CBC encryption with PKCS#7 padding
          keys:
            - name: key1
              secret: <base64-encoded-32-byte-key>
      - identity: {}       # allows reading unencrypted secrets (fallback)
```

Better: use `kms` provider to delegate to cloud KMS (AWS KMS, GCP KMS):
```yaml
      - kms:
          name: aws-kms
          endpoint: unix:///tmp/kms.sock    # kms-plugin socket
          cachesize: 1000
```

---

# Topic 3: External Secret Management

## 1. Why Not Use K8s Secrets Alone

```text
Problem with native K8s Secrets at scale:
  - Secret rotation: update Secret + rolling restart pods
  - GitOps risk: secrets committed to Git (even base64)
  - etcd backup = secret backup (security risk)
  - Audit trail: limited built-in secret access logging
  - Cross-cluster: secrets don't replicate across clusters natively
```

## 2. External Secrets Operator (ESO)

ESO syncs secrets from external stores (AWS Secrets Manager, Vault, GCP Secret Manager) into K8s Secrets:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: payment-db-secret
  namespace: prod
spec:
  refreshInterval: 1h           # check for updates every hour
  secretStoreRef:
    name: aws-secretsmanager    # configured SecretStore
    kind: ClusterSecretStore
  target:
    name: payment-db-secret     # K8s Secret to create/update
    creationPolicy: Owner
  data:
    - secretKey: password       # K8s Secret key
      remoteRef:
        key: prod/payment/db    # AWS Secrets Manager path
        property: password      # JSON property within the secret
```

ESO creates and keeps the K8s Secret synced. When the AWS secret rotates, ESO updates the K8s Secret, and if mounted as a volume, the pod gets the new value without restart.

## 3. Secrets Store CSI Driver

Alternative approach: mount secrets directly from external store into pods as volumes (no K8s Secret created):

```yaml
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: payment-db-secrets
  namespace: prod
spec:
  provider: aws
  parameters:
    objects: |
      - objectName: "prod/payment/db"
        objectType: "secretsmanager"
        jmesPath:
          - path: "password"
            objectAlias: "password"
```

```yaml
spec:
  volumes:
    - name: db-secrets
      csi:
        driver: secrets-store.csi.k8s.io
        readOnly: true
        volumeAttributes:
          secretProviderClass: payment-db-secrets
```

Advantage: secrets never stored in etcd. Mounted directly via CSI.

---

# Topic 4: Projected Volumes

Combine multiple sources into a single volume mount:

```yaml
volumes:
  - name: combined-secrets
    projected:
      sources:
        - secret:
            name: payment-db-secret
        - configMap:
            name: payment-service-config
        - serviceAccountToken:            # auto-rotating SA token
            audience: api.myapp.com
            expirationSeconds: 3600
            path: token
```

---

# Topic 5: Common Mistakes and Best Practices

## 1. Anti-Patterns

| Anti-Pattern | Problem | Fix |
|---|---|---|
| Injecting secrets as env vars | Shows in `kubectl describe pod`, logs, crash dumps | Use volume mounts |
| Committing Secrets to Git (even base64) | Base64 is not encryption; anyone with repo access sees plaintext | Use ESO or Sealed Secrets |
| `imagePullSecret` with admin registry credentials | Blast radius too large | Create scoped read-only registry credentials |
| No etcd encryption | Secrets readable in etcd backup | Enable KMS encryption |
| Hardcoded secrets in Helm `values.yaml` | values.yaml checked into Git | Use `--set` flag or ESO |
| No secret rotation | Compromised secret stays valid forever | Automate rotation with ESO or Vault |

## 2. Production Pattern: GitOps-Safe Secrets

```text
Problem: GitOps requires everything declarative in Git, but secrets can't be in Git.

Solution 1: Sealed Secrets (Bitnami)
  kubeseal encrypts secrets with cluster-public-key → SealedSecret YAML in Git
  Controller decrypts on cluster → creates K8s Secret
  Only the cluster can decrypt

Solution 2: External Secrets Operator
  Only ExternalSecret manifest in Git (references path in Vault/AWS)
  No secret data in Git at all
  ESO pulls from source of truth at runtime
```

## 3. Revision Notes

- ConfigMap: non-sensitive config; env var or volume mount; volume mounts live-reload; env vars don't
- Immutable ConfigMaps/Secrets: can't be edited; improves performance; use for versioned configs
- Secret: base64-encoded (NOT encrypted); provides RBAC-controlled access; mounted as tmpfs
- Secret types: Opaque (default), TLS, service-account-token, dockerconfigjson
- etcd encryption: enable `aescbc` or `kms` provider to protect secrets at rest
- External Secrets Operator: syncs from AWS SM/Vault/GCP into K8s Secrets; auto-rotation
- Secrets Store CSI Driver: mounts directly from external store; no K8s Secret in etcd
- Projected volumes: combine secrets, configmaps, and SA tokens into one mount

## 4. Official Source Notes

- ConfigMaps: <https://kubernetes.io/docs/concepts/configuration/configmap/>
- Secrets: <https://kubernetes.io/docs/concepts/configuration/secret/>
- Encryption at rest: <https://kubernetes.io/docs/tasks/administer-cluster/encrypt-data/>
- External Secrets Operator: <https://external-secrets.io/>
- Secrets Store CSI: <https://secrets-store-csi-driver.sigs.k8s.io/>
