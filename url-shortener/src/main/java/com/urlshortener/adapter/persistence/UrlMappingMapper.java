package com.urlshortener.adapter.persistence;

import com.urlshortener.domain.ShortCode;
import com.urlshortener.domain.UrlMapping;
import com.urlshortener.domain.UrlStatus;
import org.springframework.stereotype.Component;

/**
 * Bidirectional mapper between the domain {@link UrlMapping} and the
 * persistence {@link UrlMappingEntity}.
 *
 * <p>This mapper is the only place where the adapter layer touches domain objects.
 * It ensures that adapter-layer concerns (column naming, version handling) are
 * fully isolated from the domain model.
 */
@Component
public class UrlMappingMapper {

    public UrlMappingEntity toEntity(UrlMapping domain) {
        return UrlMappingEntity.builder()
                .code(domain.code().value())
                .longUrl(domain.longUrl())
                .userId(domain.userId())
                .status(domain.status().name())
                .createdAt(domain.createdAt())
                .expiresAt(domain.expiresAt())
                .customAlias(domain.customAlias())
                .version(domain.version())
                .build();
    }

    public UrlMapping toDomain(UrlMappingEntity entity) {
        return new UrlMapping(
                new ShortCode(entity.getCode()),
                entity.getLongUrl(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                UrlStatus.valueOf(entity.getStatus()),
                entity.isCustomAlias(),
                entity.getVersion()
        );
    }
}
