package com.urlshortener.adapter.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity for the {@code url_mapping} table.
 *
 * <p>This is an infrastructure concern — it lives in the adapter layer and is
 * never referenced from the domain or service layer.
 *
 * <p>Mapping to/from the domain {@link com.urlshortener.domain.UrlMapping} is
 * handled by {@link UrlMappingMapper}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("url_mapping")
public class UrlMappingEntity {

    @Id
    @Column("code")
    private String code;

    @Column("long_url")
    private String longUrl;

    @Column("user_id")
    private String userId;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("custom_alias")
    private boolean customAlias;

    @Column("url_hash")
    private String urlHash;

    @Version
    @Column("version")
    private long version;
}
