# Greeter Sample Requests

## Unary Request

```json
{
  "name": "Aravind",
  "request_id": "local-001"
}
```

Expected behavior:

- valid name returns `OK`
- empty name returns `INVALID_ARGUMENT`
- duplicate `request_id` can be used for idempotency or tracing practice

---

## Server Streaming Request

```json
{
  "name": "Aravind",
  "resume_token": ""
}
```

Expected behavior:

- server emits greeting events
- each event includes a resume token
- client can cancel the stream
- server should stop work after cancellation

---

## Metadata To Practice

```text
authorization: Bearer local-test-token
x-request-id: local-001
x-tenant-id: training
```

Do not log raw authorization values in real services.