# Project 04: Job Queue With Worker

## Objective

Build a background job queue where producers enqueue work and workers process with blocking pop.

## Requirements

- Producer: RPUSH job payload JSON to `jobs:{type}:queue`
- Worker: BLPOP with timeout to retrieve job
- Retry: on failure, RPUSH to `jobs:{type}:retry` with attempt count
- Dead letter: after 3 retries, RPUSH to `jobs:dead`
- Priority variant: separate high/low priority queues, worker checks high first

## Key Redis Patterns Used

- List: `RPUSH`, `BLPOP`, `LLEN`
- Key patterns: `jobs:{type}:queue`, `jobs:{type}:retry`, `jobs:dead`
- Priority: `BLPOP jobs:high:queue jobs:low:queue 5` (checks high first)

## Implementation Notes

Safe queue pattern: `RPOPLPUSH jobs:queue jobs:processing` moves job atomically to in-progress list. On success, LREM from processing. On crash, jobs in processing can be recovered on startup.

Include `retry_count` in the job payload JSON. Check on dequeue: if retry_count >= 3, route to dead letter.

## Test Scenarios

1. Enqueue 5 jobs. Verify LLEN is 5.
2. Start worker. Verify BLPOP processes each job.
3. Simulate failure. Verify job moves to retry queue with incremented count.
4. After 3 failures. Verify job moves to dead queue.
5. Priority: enqueue 3 low-priority and 2 high-priority jobs. Verify high-priority jobs processed first.

## Interview Value

Demonstrates: list as queue, BLPOP blocking pattern, retry-with-count, dead-letter routing, priority queues.
