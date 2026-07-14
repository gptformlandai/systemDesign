package com.systemdesign.feed.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent implements Serializable {
    private Long postId;
    private Long authorId;
    private boolean isCelebrity;
    private LocalDateTime createdAt;
}
