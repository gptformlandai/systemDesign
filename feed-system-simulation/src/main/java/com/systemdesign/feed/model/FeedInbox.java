package com.systemdesign.feed.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("feed_inbox")
public class FeedInbox {
    @Id
    private Long id;
    private Long viewerId;
    private Long postId;
    private Long authorId;
    private LocalDateTime createdAt;
}
