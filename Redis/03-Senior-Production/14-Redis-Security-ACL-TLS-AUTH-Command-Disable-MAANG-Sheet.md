# 14. Redis Security: ACL, TLS, AUTH, Dangerous Commands

## Goal

Harden a production Redis instance with authentication, access control, TLS encryption, and command restrictions.

---

## AUTH: Basic Authentication

```conf
# redis.conf
requirepass your-strong-password-here
```

```bash
# Client authenticates.
AUTH your-strong-password-here
```

Single password is coarse-grained: all clients share one credential. Prefer ACL for multi-user environments.

---

## ACL: Access Control Lists (Redis 6+)

```bash
# List all users.
ACL LIST

# Add a user with specific permissions.
ACL SETUSER readonlyuser on >password123 ~* &* +@read

# ACL rule breakdown:
# on          -> user is enabled
# >password   -> set password
# ~*          -> all key patterns allowed
# &*          -> all channel patterns allowed
# +@read      -> only read commands allowed
# -@dangerous -> disallow dangerous command category

# Add a service user limited to specific keys and commands.
ACL SETUSER orders-service on >svcpassword ~orders:* +GET +SET +DEL +EXPIRE

# Check current user identity.
ACL WHOAMI

# Test what a user can do.
ACL LOG RESET
ACL LOG
```

---

## ACL Categories

| Category | Commands |
|---|---|
| `+@read` | GET, HGET, LRANGE, SMEMBERS, etc. |
| `+@write` | SET, HSET, LPUSH, SADD, etc. |
| `+@admin` | CONFIG, INFO, DEBUG, BGSAVE, etc. |
| `+@dangerous` | DEBUG, CONFIG RESETSTAT, FLUSHALL, etc. |
| `+@all` | all commands |
| `-@dangerous` | subtract dangerous category |

---

## Disabling Dangerous Commands

For environments that cannot use ACL (older Redis or legacy clients):

```conf
# Rename commands to empty string to disable.
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command DEBUG ""
rename-command CONFIG ""
rename-command KEYS ""
```

In Redis 7+, prefer ACL rules over rename-command. Do not rename commands in Cluster mode (breaks cluster coordination).

---

## TLS Encryption

```conf
port 0
tls-port 6380
tls-cert-file /etc/redis/tls/redis.crt
tls-key-file /etc/redis/tls/redis.key
tls-ca-cert-file /etc/redis/tls/ca.crt
tls-auth-clients yes
```

```bash
# Connect with TLS.
redis-cli --tls --cert ./redis.crt --key ./redis.key --cacert ./ca.crt -p 6380 PING
```

---

## Network Isolation

```conf
# Bind to specific interfaces only.
bind 127.0.0.1 10.0.0.5

# Disable protected mode only when binding is explicitly configured.
protected-mode yes
```

Production Redis should never be exposed on a public interface without TLS and authentication.

---

## Security Checklist

- requirepass or ACL with strong passwords
- bind to private network interface only
- TLS between application and Redis
- rename-command or ACL to disable FLUSHALL, DEBUG, KEYS in production
- read-only replica access for reporting services
- firewall rules: restrict Redis port to known application servers
- audit ACL LOG periodically

---

## Interview Sound Bite

Redis security in production means ACL users with least-privilege access, TLS for in-transit encryption, and disabling dangerous commands like FLUSHALL and DEBUG. The bind directive and firewall rules prevent exposure to untrusted networks. Redis 6 ACL replaces the single-password model with per-user command and key pattern restrictions.
