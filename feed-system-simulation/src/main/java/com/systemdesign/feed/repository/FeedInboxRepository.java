package com.systemdesign.feed.repository;

import com.systemdesign.feed.model.FeedInbox;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface FeedInboxRepository extends R2dbcRepository<FeedInbox, Long> {
    Flux<FeedInbox> findAllByViewerIdOrderByCreatedAtDesc(Long viewerId);
}
