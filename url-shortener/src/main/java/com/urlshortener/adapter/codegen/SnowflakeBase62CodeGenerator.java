package com.urlshortener.adapter.codegen;

import com.urlshortener.port.outbound.CodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Snowflake-style distributed ID generator encoded in Base62.
 *
 * <p>Bit layout (63 bits total):
 * <pre>
 *   [41 bits: timestamp millis since epoch]
 *   [10 bits: node/machine ID]
 *   [12 bits: per-millisecond sequence]
 * </pre>
 *
 * <p>This gives:
 * <ul>
 *   <li>~139 years of unique IDs without rollover</li>
 *   <li>4096 IDs per millisecond per node</li>
 *   <li>1024 nodes in a cluster</li>
 *   <li>No coordination between nodes (no DB round-trip)</li>
 * </ul>
 *
 * <p>The 64-bit ID is then encoded in Base62 to produce a compact 7-character
 * URL-safe string (~3.5 trillion capacity as per design doc).
 *
 * <p>Thread-safety: this class is safe for concurrent access via volatile reads
 * and atomic compare-and-set.
 */
@Component
public class SnowflakeBase62CodeGenerator implements CodeGenerator {

    // Custom epoch: 2024-01-01T00:00:00Z (reduces timestamp bits needed)
    private static final long CUSTOM_EPOCH = 1_704_067_200_000L;

    private static final int NODE_ID_BITS      = 10;
    private static final int SEQUENCE_BITS     = 12;
    private static final long MAX_NODE_ID      = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE     = (1L << SEQUENCE_BITS) - 1;
    private static final long NODE_ID_SHIFT    = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT  = SEQUENCE_BITS + NODE_ID_BITS;

    private static final char[] BASE62_ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final long nodeId;
    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private final AtomicLong sequence      = new AtomicLong(0L);

    public SnowflakeBase62CodeGenerator(@Value("${app.codegen.node-id:1}") long nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(
                    "nodeId must be between 0 and " + MAX_NODE_ID + ", got: " + nodeId);
        }
        this.nodeId = nodeId;
    }

    @Override
    public synchronized String nextCode() {
        long timestamp = currentTimeMillis();
        long last = lastTimestamp.get();

        if (timestamp < last) {
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate IDs for " + (last - timestamp) + " ms");
        }

        long seq;
        if (timestamp == last) {
            seq = sequence.incrementAndGet() & MAX_SEQUENCE;
            if (seq == 0) {
                // Sequence exhausted in this millisecond — wait for next ms
                timestamp = waitNextMillis(last);
            }
        } else {
            sequence.set(0);
            seq = 0;
        }

        lastTimestamp.set(timestamp);

        long id = ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (nodeId << NODE_ID_SHIFT)
                | seq;

        return toBase62(id);
    }

    private String toBase62(long number) {
        if (number == 0) return "0";
        StringBuilder sb = new StringBuilder();
        long n = number;
        while (n > 0) {
            sb.append(BASE62_ALPHABET[(int) (n % BASE62_ALPHABET.length)]);
            n /= BASE62_ALPHABET.length;
        }
        return sb.reverse().toString();
    }

    private long waitNextMillis(long lastMs) {
        long ts = currentTimeMillis();
        while (ts <= lastMs) {
            ts = currentTimeMillis();
        }
        return ts;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
