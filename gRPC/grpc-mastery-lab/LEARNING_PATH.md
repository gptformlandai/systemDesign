# gRPC Lab Learning Path

## Phase 1: Contract Foundations

- Read the Greeter proto.
- Identify package, service, method, request, response, and field numbers.
- Explain how generated stubs would be created.
- Review field-number safety rules.

Outcome: you can read and explain a proto contract confidently.

---

## Phase 2: Unary RPC Debugging

- Write grpcurl commands from the command templates.
- Map invalid input to `INVALID_ARGUMENT`.
- Map missing method registration to `UNIMPLEMENTED`.
- Map dependency outage to `UNAVAILABLE`.

Outcome: you can debug basic RPC reachability and status codes.

---

## Phase 3: Deadline And Cancellation

- Use the deadline incident template.
- Create a caller budget and downstream sub-budgets.
- Explain server-side cancellation behavior.

Outcome: you can reason about time budgets instead of vague timeout guesses.

---

## Phase 4: Streaming And Backpressure

- Design a server-streaming method.
- Add resume token and cancellation behavior.
- Write slow-consumer risk notes.

Outcome: you can design streaming APIs with operational boundaries.

---

## Phase 5: Production Operations

- Practice mTLS, service discovery, load balancing, and proxy timeout runbooks.
- Write an RCA for a gRPC incident.
- Score yourself against the production readiness checklist.

Outcome: you can explain gRPC like a production owner.