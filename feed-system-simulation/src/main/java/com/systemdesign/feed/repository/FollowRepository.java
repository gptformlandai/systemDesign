package com.systemdesign.feed.repository;

import com.systemdesign.feed.model.Follow;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface FollowRepository extends R2dbcRepository<Follow, Long> {
    Flux<Follow> findAllByFollowerId(Long followerId);
    Flux<Follow> findAllByFollowedId(Long followedId);
}
