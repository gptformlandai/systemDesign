# 09. Auth: mTLS, JWT Metadata, Per-Method Authorization

## Goal

Secure gRPC services with identity, transport protection, and method-level policy.

```text
TLS/mTLS identity + metadata credentials + interceptor policy + handler authorization
```

---

## Authentication Options

| Mechanism | Best For |
|---|---|
| TLS | encrypted transport and server identity |
| mTLS | service-to-service identity in trusted platforms |
| JWT metadata | end-user or workload claims passed with request |
| OAuth2 token | user/application authorization flows |
| SPIFFE/SPIRE | workload identity and cert automation |

---

## Metadata Auth Pattern

```text
client interceptor reads token
-> adds authorization metadata
-> server interceptor validates token
-> auth context is attached to request
-> method policy checks permission
```

Do not let every handler parse tokens independently. Central validation belongs in interceptors or shared auth middleware, while business-specific authorization can remain near the method.

---

## mTLS Pattern

```text
client verifies server certificate
server verifies client certificate
certificate identity maps to workload/service identity
policy decides which methods are allowed
cert rotation keeps identity fresh
```

mTLS proves workload identity. It does not automatically prove user authorization.

---

## Per-Method Authorization

Example policy questions:

- Can this caller invoke `payments.v1.PaymentService/CapturePayment`?
- Is the caller allowed for this tenant?
- Is the requested resource owned by the caller?
- Does the method require stronger service identity?
- Is this an admin-only or batch-only method?

---

## Sensitive Metadata Rules

- Redact tokens from logs.
- Do not forward all incoming metadata blindly.
- Bound metadata size.
- Separate end-user identity from service identity.
- Propagate only approved headers/metadata.
- Rotate certificates and keys automatically.

---

## Debugging Auth Failures

| Status | Likely Meaning |
|---|---|
| `UNAUTHENTICATED` | identity missing, expired, invalid, or failed TLS/mTLS validation |
| `PERMISSION_DENIED` | identity is valid but not authorized for the method/resource |
| `UNAVAILABLE` | TLS/proxy handshake or connectivity problem before auth decision |
| `INVALID_ARGUMENT` | auth metadata shape is malformed |

---

## Interview Sound Bite

For gRPC security, I separate transport identity, request credentials, and authorization. mTLS identifies workloads, JWT or OAuth metadata can represent users or apps, interceptors validate credentials, and per-method/resource policy decides whether the call is allowed.