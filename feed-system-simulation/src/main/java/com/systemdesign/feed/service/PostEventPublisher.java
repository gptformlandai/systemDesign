package com.systemdesign.feed.service;

import com.systemdesign.feed.model.PostCreatedEvent;

public interface PostEventPublisher {
    void publish(PostCreatedEvent event);
}
