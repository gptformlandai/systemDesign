-- URL Shortener — Flyway schema migration V1
-- Executed at startup via Flyway (JDBC, blocking, runs once before Netty starts)

CREATE TABLE IF NOT EXISTS url_mapping (
    code            VARCHAR(32)     PRIMARY KEY,
    long_url        TEXT            NOT NULL,
    user_id         VARCHAR(64),
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    custom_alias    BOOLEAN         NOT NULL DEFAULT FALSE,
    url_hash        VARCHAR(64),                                 -- SHA-256 of long_url for dedup queries
    version         BIGINT          NOT NULL DEFAULT 1,

    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'BLOCKED'))
);

-- Primary access pattern: redirect lookup by short code (already covered by PK)

-- Secondary: user dashboard listing (ordered by created_at DESC)
CREATE INDEX IF NOT EXISTS idx_url_mapping_user_created
    ON url_mapping (user_id, created_at DESC);

-- Secondary: expiry cleanup background job
CREATE INDEX IF NOT EXISTS idx_url_mapping_expires_at
    ON url_mapping (expires_at)
    WHERE expires_at IS NOT NULL;

-- Optional: deduplication query by long URL hash (only if dedup is enabled)
CREATE INDEX IF NOT EXISTS idx_url_mapping_url_hash
    ON url_mapping (url_hash)
    WHERE url_hash IS NOT NULL;
