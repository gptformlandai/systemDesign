# 06. Datadog APM: Java Instrumentation, dd-java-agent, Spring Boot, Manual Spans

## Goal

Instrument a Java application for Datadog distributed tracing using the dd-java-agent, configure all required environment variables, and create manual custom spans.

---

## How dd-java-agent Works

The Datadog Java agent is a JVM agent that attaches at startup and byte-codes injects tracing into supported frameworks automatically — no application code changes required for basic tracing.

```text
JVM startup:
  -javaagent:/path/to/dd-java-agent.jar

Agent intercepts:
  - Servlet/JAX-RS/Spring MVC requests (creates root span)
  - JDBC queries (creates DB spans)
  - HTTP clients (OkHttp, Apache HttpClient, RestTemplate, WebClient)
  - Message brokers (Kafka, RabbitMQ, JMS)
  - Redis (Jedis, Lettuce)
  - gRPC
```

---

## Download And Attach

```bash
# Download the latest agent.
curl -Lo dd-java-agent.jar \
  'https://dtdg.co/latest-java-tracer'

# Run your application with the agent attached.
java \
  -javaagent:./dd-java-agent.jar \
  -DD_AGENT_HOST=localhost \
  -DD_TRACE_AGENT_PORT=8126 \
  -jar myapp.jar
```

---

## Required Environment Variables

```bash
DD_SERVICE=orders-service          # service name in Datadog
DD_ENV=production                  # environment: production/staging/dev
DD_VERSION=1.2.3                   # version for deployment tracking
DD_AGENT_HOST=localhost            # Datadog Agent host
DD_TRACE_AGENT_PORT=8126           # Datadog Agent APM port
DD_LOGS_INJECTION=true             # auto-inject trace_id into logs (requires JSON logging)
DD_TRACE_SAMPLE_RATE=1.0           # 100% sampling (lower in production to 0.1)
```

---

## Spring Boot Application: Full Setup

### pom.xml (no Datadog dependency required for auto-instrumentation)

```xml
<!-- For manual spans only, add the tracing API: -->
<dependency>
  <groupId>com.datadoghq</groupId>
  <artifactId>dd-trace-api</artifactId>
  <version>1.25.0</version>
</dependency>

<!-- For OpenTracing API: -->
<dependency>
  <groupId>io.opentracing</groupId>
  <artifactId>opentracing-api</artifactId>
  <version>0.33.0</version>
</dependency>
```

### application.yaml

```yaml
# Application-level config.
spring:
  application:
    name: orders-service
```

### startup script

```bash
java \
  -javaagent:/opt/datadog/dd-java-agent.jar \
  -Ddd.service=orders-service \
  -Ddd.env=production \
  -Ddd.version=1.2.3 \
  -Ddd.agent.host=datadog-agent \
  -Ddd.trace.agent.port=8126 \
  -Ddd.logs.injection=true \
  -Ddd.trace.http.client.tag.query-string=true \
  -jar orders-service.jar
```

System property form: `-Ddd.service` (equivalent to `DD_SERVICE` env var).

---

## Docker Environment Setup

```dockerfile
FROM eclipse-temurin:17-jre

# Download agent during image build.
ADD https://dtdg.co/latest-java-tracer /dd-java-agent.jar

COPY target/orders-service.jar /app.jar

ENTRYPOINT ["java", "-javaagent:/dd-java-agent.jar", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml environment section.
environment:
  DD_SERVICE: orders-service
  DD_ENV: production
  DD_VERSION: "1.2.3"
  DD_AGENT_HOST: datadog-agent
  DD_TRACE_AGENT_PORT: "8126"
  DD_LOGS_INJECTION: "true"
```

---

## Manual Custom Spans

For operations not auto-instrumented, create spans manually.

### Using dd-trace-api Annotation

```java
import datadog.trace.api.Trace;

@Service
public class OrderService {

    @Trace(operationName = "orders.validate", resourceName = "OrderService.validate")
    public void validateOrder(Order order) {
        // This method creates a span automatically.
        // The span inherits the active trace context.
    }
}
```

### Using OpenTracing API

```java
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@Service
public class PaymentService {

    private final Tracer tracer = GlobalTracer.get();

    public PaymentResult processPayment(String orderId, BigDecimal amount) {
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("payment.process")
            .withTag("order.id", orderId)
            .withTag("payment.amount", amount.doubleValue());

        try (Span span = spanBuilder.start()) {
            try {
                // Set resource name for grouping in APM.
                span.setTag("resource.name", "PaymentService.processPayment");
                span.setTag("payment.provider", "stripe");

                PaymentResult result = stripeClient.charge(orderId, amount);
                span.setTag("payment.status", result.getStatus());
                return result;

            } catch (Exception e) {
                // Mark span as error.
                span.setTag("error", true);
                span.setTag("error.message", e.getMessage());
                span.setTag("error.type", e.getClass().getName());
                throw e;
            }
        }
    }
}
```

---

## Custom Tags On Spans

```java
import datadog.trace.api.DDTags;
import io.opentracing.util.GlobalTracer;

// Add tags to the currently active span.
Span activeSpan = GlobalTracer.get().activeSpan();
if (activeSpan != null) {
    activeSpan.setTag("order.id", orderId);
    activeSpan.setTag("customer.tier", customer.getTier());
    activeSpan.setTag(DDTags.SERVICE_NAME, "orders-service");
    activeSpan.setTag(DDTags.RESOURCE_NAME, "POST /orders");
}
```

---

## Trace Propagation With RestTemplate

Datadog auto-instruments RestTemplate. The trace headers are injected automatically when you use standard RestTemplate:

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        // No special setup needed; dd-java-agent instruments this automatically.
        return new RestTemplate();
    }
}

// Usage - trace headers are injected automatically.
ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
    "http://payments-service/charge",
    request,
    OrderResponse.class
);
```

---

## What Gets Auto-Instrumented

| Framework/Library | Span Type |
|---|---|
| Spring MVC / Spring WebFlux | HTTP server span |
| JDBC (any driver) | DB query span |
| RestTemplate / WebClient | HTTP client span |
| OkHttp / Apache HttpClient | HTTP client span |
| Spring Kafka / KafkaProducer/Consumer | Kafka span |
| Spring Data Redis (Jedis/Lettuce) | Redis span |
| gRPC | gRPC client/server spans |
| Spring Security | Authentication span |
| Hibernate / JPA | DB ORM span |

---

## Interview Sound Bite

The dd-java-agent is a JVM javaagent that auto-instruments Spring Boot, JDBC, HTTP clients, Kafka, and Redis without code changes. It requires five env vars: DD_SERVICE, DD_ENV, DD_VERSION, DD_AGENT_HOST, and DD_LOGS_INJECTION. For custom spans, use the @Trace annotation or the OpenTracing API with GlobalTracer. Custom tags on spans add business context (order ID, customer tier) that makes traces searchable in the Trace Explorer.
