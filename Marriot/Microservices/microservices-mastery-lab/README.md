# Microservices Mastery Lab

This lab turns the Microservices track into something you can run locally.

It is intentionally small and dependency-free. The first version uses Python standard library
only so a learner can focus on the microservice mechanics:

- API Gateway/BFF entry point
- Booking Service
- Payment Service
- idempotency key handling
- service-to-service HTTP call
- request ID propagation
- Booking table
- Payment audit/idempotency table
- transactional outbox table
- async Notification worker
- duplicate request replay
- payment timeout/unknown state simulation

---

## 1. Run

From this folder:

```bash
python3 booking_platform_simulation.py
```

The script starts:

| Component | Port |
|---|---:|
| Gateway | 8080 |
| Booking Service | 8081 |
| Payment Service | 8082 |
| Notification Worker | background thread |

It creates a local SQLite file:

```text
booking_lab.sqlite3
```

Remove that file when you want a clean state.

---

## 2. Happy Path

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-key-1' \
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120}'
```

Expected:

- Gateway creates or propagates a request ID.
- Booking Service creates a booking.
- Booking calls Payment Service.
- Payment authorizes.
- Booking confirms.
- Booking writes an outbox event.
- Notification worker publishes the event.

---

## 3. Idempotency Replay

Run the exact same command again with the same `Idempotency-Key`.

Expected:

- no second booking
- no second payment charge
- stored response is returned with `idempotentReplay: true`

Then try same key with a different payload:

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-key-1' \
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"suite","amount":220}'
```

Expected:

- conflict response
- same idempotency key cannot represent a different request

---

## 4. Payment Decline

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-key-decline' \
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120,"simulatePayment":"decline"}'
```

Expected:

- Booking ends in `PAYMENT_FAILED`.
- No confirmed-booking notification should be produced.

---

## 5. Payment Timeout

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-key-timeout' \
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120,"simulatePayment":"timeout"}'
```

Expected:

- Booking treats the payment result as `PAYMENT_UNKNOWN`.
- This teaches the important rule: timeout does not prove payment failure.
- A real system would reconcile with the payment provider using the same idempotency key.

---

## 6. Inspect Booking

Use the booking ID returned by a create call:

```bash
curl -s http://localhost:8081/bookings/<bookingId>
```

---

## 7. What To Notice

While the lab runs, watch the logs:

- same `requestId` appears across Gateway, Booking, and Payment
- duplicate request replays stored response
- payment call has its own idempotency key
- outbox event is persisted before notification worker publishes it
- async side effects do not block booking response after persistence

---

## 8. Interview Explanation

Use this 60-second explanation after running the lab:

```text
This local capstone models a booking flow with gateway, booking, payment, and notification.
The gateway creates a request ID. Booking uses an idempotency key to prevent duplicate
booking/payment side effects. Payment also uses an idempotency key because retries can happen
across a timeout. Booking writes a confirmed event to an outbox, and a notification worker
publishes it asynchronously. The important production lesson is that service calls can fail,
timeout, or duplicate, so the design needs idempotency, persistent state, outbox, and
observable request IDs.
```

