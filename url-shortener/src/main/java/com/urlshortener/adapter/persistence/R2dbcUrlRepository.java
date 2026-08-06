package com.urlshortener.adapter.persistence;

import com.urlshortener.domain.ShortCode;
import com.urlshortener.domain.UrlMapping;
import com.urlshortener.domain.UrlStatus;
import com.urlshortener.port.outbound.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * R2DBC adapter implementing the {@link UrlRepository} port.
 *
 * <p>Uses {@link DatabaseClient} for operations that require SQL-level semantics
 * not available through Spring Data derived methods:
 * <ul>
 *   <li>{@code INSERT ... ON CONFLICT DO NOTHING} for atomic alias reservation</li>
 *   <li>Optimistic-lock status update with version check</li>
 * </ul>
 *
 * <p>Simple reads and scans delegate to {@link UrlMappingR2dbcRepository}
 * for cleaner code.
 */
@Slf4j
@Repository
public class R2dbcUrlRepository implements UrlRepository {

    private final UrlMappingR2dbcRepository springRepo;
    private final DatabaseClient db;
    private final UrlMappingMapper mapper;

    public R2dbcUrlRepository(
            UrlMappingR2dbcRepository springRepo,
            DatabaseClient db,
            UrlMappingMapper mapper) {
        this.springRepo = springRepo;
        this.db = db;
        this.mapper = mapper;
    }

    /**
     * Atomically inserts the mapping using PostgreSQL's {@code ON CONFLICT DO NOTHING}.
     *
     * @return {@code true} if inserted (code was unique); {@code false} if conflict (code already exists)
     */
    @Override
    public Mono<Boolean> saveIfAbsent(UrlMapping mapping) {
        var spec = db.sql("""
                        INSERT INTO url_mapping
                            (code, long_url, user_id, status, created_at, expires_at, custom_alias, version)
                        VALUES
                            (:code, :longUrl, :userId, :status, :createdAt, :expiresAt, :customAlias, :version)
                        ON CONFLICT (code) DO NOTHING
                        """)
                .bind("code", mapping.code().value())
                .bind("longUrl", mapping.longUrl())
                .bind("userId", mapping.userId() != null ? mapping.userId() : "")
                .bind("status", mapping.status().name())
                .bind("createdAt", mapping.createdAt())
                .bind("customAlias", mapping.customAlias())
                .bind("version", mapping.version());

        // Bind nullable expiresAt — R2DBC requires explicit bindNull for null columns
        var boundSpec = mapping.expiresAt() != null
                ? spec.bind("expiresAt", mapping.expiresAt())
                : spec.bindNull("expiresAt", Instant.class);

        return boundSpec.fetch()
                .rowsUpdated()
                .map(rowsAffected -> rowsAffected > 0)
                .doOnSuccess(saved -> log.debug("saveIfAbsent code={} saved={}", mapping.code(), saved));
    }

    @Override
    public Mono<UrlMapping> findByCode(ShortCode code) {
        return springRepo.findById(code.value())
                .map(mapper::toDomain);
    }

    /**
     * Optimistic-lock status update. Uses WHERE version = :expectedVersion
     * to detect concurrent modifications.
     */
    @Override
    public Mono<Boolean> updateStatus(ShortCode code, UrlStatus newStatus, long expectedVersion) {
        return db.sql("""
                        UPDATE url_mapping
                        SET    status  = :status,
                               version = version + 1
                        WHERE  code    = :code
                          AND  version = :expectedVersion
                        """)
                .bind("status", newStatus.name())
                .bind("code", code.value())
                .bind("expectedVersion", expectedVersion)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows > 0)
                .doOnSuccess(updated -> log.debug("updateStatus code={} status={} updated={}",
                        code, newStatus, updated));
    }

    @Override
    public Flux<UrlMapping> findByUserId(String userId, int offset, int limit) {
        return springRepo.findByUserIdOrderByCreatedAtDesc(userId, limit, offset)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<UrlMapping> findExpiredBefore(Instant before) {
        return springRepo.findActiveExpiredBefore(before)
                .map(mapper::toDomain);
    }

}

