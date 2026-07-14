package com.systemdesign.feed.repository;

import com.systemdesign.feed.model.Post;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.Collection;

@Repository
public interface PostRepository extends R2dbcRepository<Post, Long> {
    Flux<Post> findAllByAuthorIdOrderByCreatedAtDesc(Long authorId);
    
    Flux<Post> findAllByAuthorIdIn(Collection<Long> authorIds);
}
