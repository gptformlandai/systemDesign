package com.example.mongodbmastery.order;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants/{tenantId}/orders")
public class OrderController {
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{orderId}")
    public OrderDocument getOrder(@PathVariable String tenantId, @PathVariable String orderId) {
        return orderRepository.findByTenantIdAndOrderId(tenantId, orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @GetMapping
    public List<OrderDocument> listByStatus(
        @PathVariable String tenantId,
        @RequestParam(defaultValue = "PAID") String status,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return orderRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
            tenantId,
            status,
            PageRequest.of(0, Math.min(limit, 100))
        );
    }
}
