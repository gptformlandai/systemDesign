package com.urlshortener.port.outbound;

/**
 * Outbound port for short-code generation.
 *
 * <p>Implementations can use different strategies:
 * <ul>
 *   <li>Snowflake + Base62 (default for production — distributed, no collision)</li>
 *   <li>Random Base62 (simple; needs collision retry)</li>
 *   <li>Pre-generated pool (fast creation; pool management overhead)</li>
 * </ul>
 *
 * <p>Implementations must be thread-safe and non-blocking.
 * The returned code is a candidate — atomic reservation in storage is still
 * required via {@link UrlRepository#saveIfAbsent}.
 */
public interface CodeGenerator {

    /**
     * Generates the next candidate short code.
     *
     * @return a URL-safe Base62 code of at least 7 characters
     */
    String nextCode();
}
