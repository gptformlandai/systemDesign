# Cheatsheet 03: Streams Commands

## Producer

| Command | Syntax | Notes |
|---|---|---|
| XADD | `XADD stream [MAXLEN [~] n] * f v` | append entry |
| XTRIM | `XTRIM stream MAXLEN [~] n` | trim stream |

## Consumer (Simple)

| Command | Syntax | Notes |
|---|---|---|
| XREAD | `XREAD [COUNT n] [BLOCK ms] STREAMS stream id` | read from id |
| XRANGE | `XRANGE stream - + [COUNT n]` | range ascending |
| XREVRANGE | `XREVRANGE stream + - [COUNT n]` | range descending |
| XLEN | `XLEN stream` | entry count |

## Consumer Groups

| Command | Syntax | Notes |
|---|---|---|
| XGROUP CREATE | `XGROUP CREATE stream group id [MKSTREAM]` | create group |
| XREADGROUP | `XREADGROUP GROUP g consumer [COUNT n] [BLOCK ms] STREAMS stream >` | read undelivered |
| XACK | `XACK stream group id [id...]` | acknowledge |
| XPENDING | `XPENDING stream group - + count` | list pending |
| XAUTOCLAIM | `XAUTOCLAIM stream group consumer min-idle start [COUNT n]` | re-claim idle |
| XGROUP SETID | `XGROUP SETID stream group id` | reposition group offset |
| XGROUP DELCONSUMER | `XGROUP DELCONSUMER stream group consumer` | remove consumer |
| XDEL | `XDEL stream id [id...]` | delete specific entry |

## Inspection

| Command | Syntax | Notes |
|---|---|---|
| XINFO STREAM | `XINFO STREAM stream` | stream metadata |
| XINFO GROUPS | `XINFO GROUPS stream` | all groups + lag |
| XINFO CONSUMERS | `XINFO CONSUMERS stream group` | consumer info |

## ID Format

```text
<millisecondsTimestamp>-<sequenceNumber>
1720000000000-0

Special IDs:
- = minimum
+ = maximum
$ = current stream end (for blocking reads)
> = next undelivered (consumer groups)
0 = beginning
```
