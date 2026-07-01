# Lab 04: Pub/Sub And Streams

## Objective

Practice Pub/Sub fire-and-forget messaging and Streams with consumer groups.

## Exercises

### Exercise 1: Basic Pub/Sub (requires two terminals)

Terminal 1:
```bash
SUBSCRIBE events:orders
```

Terminal 2:
```bash
PUBLISH events:orders '{"order_id":"5001","status":"placed"}'
PUBLISH events:orders '{"order_id":"5002","status":"shipped"}'
```

Terminal 1 should receive both messages.

### Exercise 2: Pattern Subscribe

Terminal 1:
```bash
PSUBSCRIBE events:*
```

Terminal 2:
```bash
PUBLISH events:orders '{"type":"order"}'
PUBLISH events:payments '{"type":"payment"}'
```

Both messages should arrive in Terminal 1.

### Exercise 3: Streams Producer

```bash
DEL events:lab:stream

XADD events:lab:stream * action login user_id 1001
XADD events:lab:stream * action purchase user_id 1001 amount 4500
XADD events:lab:stream * action logout user_id 1001

XLEN events:lab:stream
# Expected: 3

XRANGE events:lab:stream - +
# Expected: all 3 entries with auto-generated IDs
```

### Exercise 4: Stream Consumer Group

```bash
XGROUP CREATE events:lab:stream notifications 0 MKSTREAM

# Read undelivered messages.
XREADGROUP GROUP notifications worker-1 COUNT 10 STREAMS events:lab:stream >

# Capture the entry ID from above output.
# ACK the first entry.
# Replace <id> with actual entry ID.
XACK events:lab:stream notifications <id>

# Check pending after ACK.
XPENDING events:lab:stream notifications - + 100
# Expected: 2 remaining (unacked entries)
```

### Exercise 5: Stream Trimming

```bash
# Add more entries.
for i in $(seq 1 20); do
  redis-cli XADD events:lab:stream '*' seq "$i" >/dev/null
done

XLEN events:lab:stream
# Expected: 23+

# Trim to 10.
XTRIM events:lab:stream MAXLEN 10
XLEN events:lab:stream
# Expected: 10
```

### Exercise 6: Compare Delivery Guarantees

```bash
# Pub/Sub: miss test.
# Start subscribing AFTER publish.
PUBLISH events:test "missed message"
SUBSCRIBE events:test
# Subscriber will NOT receive the published message.

# Stream: replay.
XRANGE events:lab:stream - +
# Subscriber can always read historical entries.
```

## Reflection

- What happens to a Pub/Sub message if no subscriber is connected?
- What does the `>` mean in XREADGROUP?
- When would you use XAUTOCLAIM?
