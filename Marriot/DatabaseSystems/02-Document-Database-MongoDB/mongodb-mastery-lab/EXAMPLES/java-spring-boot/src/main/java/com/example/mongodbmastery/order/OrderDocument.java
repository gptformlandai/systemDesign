package com.example.mongodbmastery.order;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orders")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_order_unique", def = "{ 'tenantId': 1, 'orderId': 1 }", unique = true),
    @CompoundIndex(name = "tenant_status_created", def = "{ 'tenantId': 1, 'status': 1, 'createdAt': -1 }"),
    @CompoundIndex(name = "tenant_customer_created", def = "{ 'tenantId': 1, 'customerId': 1, 'createdAt': -1 }")
})
public class OrderDocument {
    @Id
    private String id;
    private String tenantId;
    private String orderId;
    private String customerId;
    private String status;
    private List<OrderItem> items;
    private long totalCents;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public record OrderItem(String sku, String productName, String category, int quantity, long priceCents) {
    }
}
