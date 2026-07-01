package com.example.mongodbmastery.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {
    Optional<OrderDocument> findByTenantIdAndOrderId(String tenantId, String orderId);

    List<OrderDocument> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status, Pageable pageable);
}
