# Python Threading Race Condition Example

## What This Demonstrates

Multiple threads increment a shared counter concurrently without a lock. Due to the non-atomic nature of `counter += 1`, increments are lost — the final count is always less than expected. Results are non-deterministic: different on every run.

## Files

- `order_processor.py` — buggy version (race condition)
- `order_processor_fixed.py` — fix using threading.Lock

## Run The Buggy Version

```bash
python3 order_processor.py
```

Expected output (shows non-determinism):

```text
Thread 0: finished batch of 200 orders
Thread 1: finished batch of 200 orders
Thread 2: finished batch of 200 orders
Thread 3: finished batch of 200 orders
Thread 4: finished batch of 200 orders
Run 1: processed_count = 876  <- should be ~900
Run 2: processed_count = 891  <- varies
Run 3: processed_count = 883  <- inconsistent
Run 4: processed_count = 872
Run 5: processed_count = 898
```

## Debug With PyCharm

1. Open this folder in PyCharm.
2. Set a breakpoint on `processed_count += 1`.
3. Right-click breakpoint → Edit → Suspend: **Thread** (not All).
4. Start debug.
5. When paused: switch threads in the Frames dropdown.
6. Observe multiple threads paused at the same increment line simultaneously.

## Run The Fixed Version

```bash
python3 order_processor_fixed.py
```

Expected output (consistent results):

```text
Run 1: processed_count = 901
Run 2: processed_count = 899
Run 3: processed_count = 903
```
