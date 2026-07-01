# Lab 08: Security — ACL And AUTH

## Objective

Configure Redis ACL users with least-privilege access and test restrictions.

## Exercises

### Exercise 1: Inspect Default ACL

```bash
ACL LIST
# Expected: user default on nopass ~* &* +@all

ACL WHOAMI
# Expected: default
```

### Exercise 2: Create A Read-Only User

```bash
ACL SETUSER readonly-user on >readpassword ~* &* +@read -@admin

# Test: authenticate as readonly-user.
AUTH readonly-user readpassword

# Allowed: read command.
GET some:key

# Denied: write command.
SET blocked:key "value"
# Expected: NOPERM this user has no permissions to run the 'set' command
```

### Exercise 3: Create A Service User With Key Restrictions

```bash
# Switch back to default user.
AUTH default ""

ACL SETUSER orders-service on >svcpass123 ~orders:* &* +GET +SET +DEL +EXPIRE

# Authenticate as service user.
AUTH orders-service svcpass123

# Allowed: access orders key.
SET orders:5001 '{"status":"placed"}'
GET orders:5001

# Denied: different key prefix.
SET user:1001 "value"
# Expected: NOPERM No permissions to access a key

# Denied: unallowed command.
DEL orders:5001
KEYS *
# Expected: NOPERM
```

### Exercise 4: Review ACL Log

```bash
AUTH default ""
ACL LOG RESET

AUTH orders-service svcpass123
SET user:1001 "forbidden"

AUTH default ""
ACL LOG
# Expected: entry showing NOPERM violation
```

### Exercise 5: Delete A User

```bash
ACL DELUSER readonly-user
ACL LIST
# Expected: readonly-user no longer in list
```

### Exercise 6: Inspect ACL Categories

```bash
ACL CAT
# Lists all categories: read, write, admin, string, hash, etc.

ACL CAT read
# Lists all read commands.
```

## Reflection

- Why is `nopass` dangerous in production?
- What does `~orders:*` restrict?
- Why should `CONFIG`, `DEBUG`, and `FLUSHALL` be restricted for service accounts?
