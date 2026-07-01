# GraphQL Subscriptions Realtime Scenario - Gold Sheet

> Track File #22 of 30 - Group 04: Scenario Practice
> For: real-time design interviews | Level: intermediate to senior | Mode: subscriptions

## 1. Scenario

```text
Design real-time order status updates for clients using GraphQL.
```

Goal: decide whether subscriptions, polling, WebSockets, SSE, or event push is the right fit.

---

## 2. Design Flow

```text
event source -> authorization -> subscription filter -> connection protocol -> delivery semantics -> client recovery
```

---

## 3. Key Questions

- Is real-time truly required?
- How many concurrent clients?
- What auth applies per event?
- What happens after disconnect?
- Do clients need replay or only live updates?
- How are backpressure and fanout handled?

---

## 4. Failure Modes

- subscription leaks events across users
- WebSocket fleet cannot scale connection count
- no replay after disconnect
- event filter is too broad
- client assumes exactly-once delivery
- resolver does database polling per connection

---

## 5. Interview Summary

```text
For GraphQL subscriptions, I validate real-time need, choose WebSocket/SSE/polling based on scale and delivery needs, enforce per-event authorization, manage connection fanout/backpressure, and define reconnect/replay behavior.
```

---

## 6. Revision Notes

- One-line summary: Subscriptions are event delivery contracts, not just live queries.
- Three keywords: event, auth, reconnect.
- One trap: using subscriptions for everything when polling or invalidation is enough.