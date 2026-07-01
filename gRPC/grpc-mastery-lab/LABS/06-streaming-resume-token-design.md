# Lab 06: Streaming Resume Token Design

## Task

Design a server-streaming method `WatchInventory`.

Include:

- request with `resume_token`
- event envelope with `event_id`
- ordering guarantee
- duplicate behavior
- heartbeat behavior
- max stream duration
- cancellation behavior

## Risk Review

- What happens if the client stops reading?
- How are buffers bounded?
- How does the client reconnect?
- What metric reveals slow consumers?
- What status is returned when the server sheds a slow client?

## Done When

Your stream design includes flow control, resume, dedupe, and cancellation notes.