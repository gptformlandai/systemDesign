# Java Spring Boot With dd-java-agent: Complete Example

## docker-compose.yml

```yaml
version: "3.8"

services:
  datadog-agent:
    image: gcr.io/datadoghq/agent:7
    environment:
      DD_API_KEY: "${DD_API_KEY}"
      DD_SITE: datadoghq.com
      DD_APM_ENABLED: "true"
      DD_APM_NON_LOCAL_TRAFFIC: "true"
      DD_DOGSTATSD_NON_LOCAL_TRAFFIC: "true"
      DD_LOGS_ENABLED: "true"
      DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL: "true"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    ports:
      - "8125:8125/udp"
      - "8126:8126/tcp"

  orders-api:
    build: .
    environment:
      DD_SERVICE: orders-api
      DD_ENV: dev
      DD_VERSION: "1.0.0"
      DD_AGENT_HOST: datadog-agent
      DD_TRACE_AGENT_PORT: "8126"
      DD_LOGS_INJECTION: "true"
      DD_TRACE_SAMPLE_RATE: "1.0"
      DD_RUNTIME_METRICS_ENABLED: "true"
    ports:
      - "8080:8080"
    depends_on:
      - datadog-agent
    labels:
      com.datadoghq.ad.logs: '[{"source":"java","service":"orders-api"}]'
```

## Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre

# Download dd-java-agent during build.
ADD https://dtdg.co/latest-java-tracer /dd-java-agent.jar

COPY target/orders-api.jar /app.jar

ENTRYPOINT ["java", \
  "-javaagent:/dd-java-agent.jar", \
  "-jar", "/app.jar"]
```

## pom.xml (relevant dependencies)

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  
  <!-- For manual spans via OpenTracing API -->
  <dependency>
    <groupId>com.datadoghq</groupId>
    <artifactId>dd-trace-api</artifactId>
    <version>1.25.0</version>
  </dependency>
  <dependency>
    <groupId>io.opentracing</groupId>
    <artifactId>opentracing-api</artifactId>
    <version>0.33.0</version>
  </dependency>
  
  <!-- JSON logging -->
  <dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
  </dependency>
</dependencies>
```

## logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>dd.trace_id</includeMdcKeyName>
      <includeMdcKeyName>dd.span_id</includeMdcKeyName>
      <includeMdcKeyName>dd.service</includeMdcKeyName>
      <includeMdcKeyName>dd.env</includeMdcKeyName>
      <includeMdcKeyName>dd.version</includeMdcKeyName>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON_CONSOLE"/>
  </root>
</configuration>
```

## OrderController.java

```java
package com.example.orders;

import datadog.trace.api.Trace;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @PostMapping
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        log.info("Creating order for customer {}", request.getCustomerId());
        return processOrder(request);
    }

    @Trace(operationName = "orders.process", resourceName = "OrderController.processOrder")
    private OrderResponse processOrder(OrderRequest request) {
        Span span = GlobalTracer.get().activeSpan();
        if (span != null) {
            span.setTag("order.customer_id", request.getCustomerId());
            span.setTag("order.total", request.getTotal());
        }

        // Simulate work.
        String orderId = "ORD-" + System.currentTimeMillis();
        log.info("Order created: {}", orderId);
        return new OrderResponse(orderId, "CREATED");
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {
        log.info("Fetching order {}", id);
        return new OrderResponse(id, "PENDING");
    }
}
```

## Expected Output

Calling `POST http://localhost:8080/orders` produces:

```json
{
  "@timestamp": "2024-01-15T10:23:45.123Z",
  "level": "INFO",
  "message": "Creating order for customer CUST-001",
  "logger_name": "com.example.orders.OrderController",
  "dd.trace_id": "8423012345678901234",
  "dd.span_id": "1234567890123456789",
  "dd.service": "orders-api",
  "dd.env": "dev",
  "dd.version": "1.0.0"
}
```

In Datadog: APM → Services → orders-api shows the trace with `orders.process` span.
