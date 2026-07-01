# Runbook 08: ACL Key Rotation

## When To Use

- Rotating service account passwords on a schedule
- Responding to a credential leak
- Onboarding a new service with dedicated ACL user

## Steps: Create A New Service User

1. Create user with least-privilege rules.

```bash
redis-cli ACL SETUSER payments-service on >initial-secure-password ~payments:* &* +GET +SET +DEL +EXPIRE +HSET +HGET +HGETALL
```

2. Verify user.

```bash
redis-cli ACL GETUSER payments-service
```

3. Test user permissions.

```bash
redis-cli -u redis://payments-service:initial-secure-password@HOST:PORT PING
redis-cli -u redis://payments-service:initial-secure-password@HOST:PORT SET payments:test "ok"
redis-cli -u redis://payments-service:initial-secure-password@HOST:PORT SET blocked:key "fail"
# Expected: NOPERM for blocked key
```

## Steps: Rotate Password

1. Update user with new password.

```bash
redis-cli ACL SETUSER payments-service >new-secure-password
```

2. Remove old password from user (if needed).

```bash
redis-cli ACL SETUSER payments-service <old-password
# < prefix removes that specific password
```

3. Update application credential store (Vault, Kubernetes Secret, etc.).

4. Restart or reload application to pick up new credentials.

5. Verify service is authenticating successfully.

```bash
redis-cli ACL LOG
# Should be empty (no auth failures)
```

## Steps: Emergency Revoke

```bash
# Disable user immediately.
redis-cli ACL SETUSER compromised-user off

# Delete user.
redis-cli ACL DELUSER compromised-user

# Review ACL log for recent violations.
redis-cli ACL LOG
```

## Persist ACL Changes

```bash
# Persist ACL to aclfile.
redis-cli CONFIG REWRITE

# Or use aclfile directive in redis.conf:
# aclfile /etc/redis/users.acl
# Then: redis-cli ACL SAVE
redis-cli ACL SAVE
```
