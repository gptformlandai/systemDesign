# Lab 11: Modern Redis - JSON, Search, Functions, And Client-Side Caching

## Objective

Practice the modern Redis surface area introduced in Sheets 32-35. Some commands require Redis builds or services with JSON/Search/Stack capabilities enabled. If a command is unsupported locally, record that as a compatibility finding instead of treating it as a failure.

---

## Compatibility Check

```bash
redis-cli INFO server
redis-cli COMMAND INFO JSON.SET
redis-cli COMMAND INFO FT.CREATE
redis-cli COMMAND INFO FUNCTION
redis-cli COMMAND INFO CLIENT
```

Expected:

- supported commands return metadata
- unsupported commands return empty or nil-like output

Reflection:

```text
Which modern features does your local Redis support today?
Which features would require Redis Stack, Redis Cloud, or a provider-specific offering?
```

---

## Exercise 1: JSON Document Pattern

Run only if `JSON.SET` is supported.

```bash
DEL user:1001
JSON.SET user:1001 $ '{"id":1001,"name":"Alice","plan":"pro","prefs":{"theme":"dark"},"login_count":0}'
JSON.GET user:1001 $
JSON.GET user:1001 $.prefs.theme
JSON.SET user:1001 $.prefs.theme '"light"'
JSON.NUMINCRBY user:1001 $.login_count 1
JSON.GET user:1001 $.prefs.theme $.login_count
```

Reflection:

- What would be worse about storing this as one opaque string?
- Which fields would you index if this became a searchable user directory?

---

## Exercise 2: Search Index Pattern

Run only if `FT.CREATE` is supported.

```bash
DEL product:1001 product:1002 product:1003

FT.CREATE idx:products ON JSON PREFIX 1 product: SCHEMA \
  $.name AS name TEXT \
  $.brand AS brand TAG \
  $.category AS category TAG \
  $.price AS price NUMERIC SORTABLE

JSON.SET product:1001 $ '{"name":"Trail Shoe","brand":"Acme","category":"shoes","price":89.99}'
JSON.SET product:1002 $ '{"name":"City Boot","brand":"Acme","category":"boots","price":129.99}'
JSON.SET product:1003 $ '{"name":"Road Runner","brand":"Bolt","category":"shoes","price":74.50}'

FT.SEARCH idx:products '@category:{shoes} @price:[50 100]' RETURN 3 $.name $.brand $.price
```

Cleanup:

```bash
FT.DROPINDEX idx:products DD
```

Reflection:

- Which fields were exact filters?
- Which fields were full-text searchable?
- What memory cost did `SORTABLE` introduce?

---

## Exercise 3: Function-Based Fixed Window Rate Limiter

Create `rate_limiter_v1.lua`:

```lua
#!lua name=rate_limiter_v1

redis.register_function('allow_fixed_window', function(keys, args)
  local key = keys[1]
  local limit = tonumber(args[1])
  local ttl_seconds = tonumber(args[2])

  local count = redis.call('INCR', key)
  if count == 1 then
    redis.call('EXPIRE', key, ttl_seconds)
  end

  if count <= limit then
    return {1, count}
  end

  return {0, count}
end)
```

Load and call:

```bash
redis-cli FUNCTION LOAD "$(cat rate_limiter_v1.lua)"
redis-cli FCALL allow_fixed_window 1 rate:lab:user:1001 3 60
redis-cli FCALL allow_fixed_window 1 rate:lab:user:1001 3 60
redis-cli FCALL allow_fixed_window 1 rate:lab:user:1001 3 60
redis-cli FCALL allow_fixed_window 1 rate:lab:user:1001 3 60
redis-cli TTL rate:lab:user:1001
redis-cli FUNCTION LIST
```

Expected:

- first three calls return allowed
- fourth call returns denied
- key has TTL

Cleanup:

```bash
redis-cli FUNCTION DELETE rate_limiter_v1
redis-cli DEL rate:lab:user:1001
```

Reflection:

- Why is this safer than `INCR` plus `EXPIRE` split across client commands?
- How would you version `allow_fixed_window_v2`?

---

## Exercise 4: Client-Side Caching Compatibility

Open terminal 1:

```bash
redis-cli
HELLO 3
CLIENT TRACKING ON
GET user:cache:1001
```

Open terminal 2:

```bash
redis-cli SET user:cache:1001 "Alice v1"
redis-cli SET user:cache:1001 "Alice v2"
```

Observe terminal 1 for invalidation behavior. Client display varies by Redis version and redis-cli behavior.

Reflection:

- Does your local client show invalidation push messages?
- Would your application client expose invalidation callbacks?
- What should your app do after reconnect?

Cleanup:

```bash
redis-cli DEL user:cache:1001
```

---

## Exercise 5: Modern Feature Decision Memo

Write a short memo for this product requirement:

```text
Build a low-latency product discovery API:
- exact filters: category, brand
- numeric filter: price
- text search: product name and description
- semantic "similar products"
- hot query cache
```

Include:

- Redis features you would use
- features you would avoid
- index memory risks
- fallback if corpus grows beyond memory
- cache invalidation plan

---

## Completion Gate

You are done when you can explain:

- why JSON is different from storing JSON strings
- why indexes cost memory
- why vector search is not automatically better than lexical search
- why Functions need versioned deployment
- why client-side caching must be invalidated and bounded
