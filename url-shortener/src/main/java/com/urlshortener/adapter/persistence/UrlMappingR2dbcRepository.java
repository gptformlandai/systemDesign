package com.urlshortener.adapter.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Spring Data R2DBC repository for basic CRUD operations.
 *
 * <p>Complex operations requiring conditional writes (INSERT ... ON CONFLICT)
 * or optimistic-lock updates are handled by {@link R2dbcUrlRepository} using
 * {@code DatabaseClient} directly.
 */
public interface UrlMappingR2dbcRepository extends ReactiveCrudRepository<UrlMappingEntity, String> {

    @Query("SELECT * FROM url_mapping WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<UrlMappingEntity> findByUserIdOrderByCreatedAtDesc(String userId, int limit, int offset);

    @Query("SELECT * FROM url_mapping WHERE expires_at IS NOT NULL AND expires_at < :before AND status = 'ACTIVE'")
    Flux<UrlMappingEntity> findActiveExpiredBefore(Instant before);
}
