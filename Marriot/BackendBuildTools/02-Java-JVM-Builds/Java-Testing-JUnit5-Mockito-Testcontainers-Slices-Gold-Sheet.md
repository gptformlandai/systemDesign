# Java Testing: JUnit 5, Mockito, Testcontainers, and Spring Boot Test Slices — Gold Sheet

> Topic: unit tests, mocking, integration tests, Spring Boot test slices, and Testcontainers

---

## 1. Intuition

A backend service has many layers: controllers, services, repositories, external API clients. Testing each layer in isolation (with mocks) is fast and catches logic bugs. Testing them together (with a real database) is slower but catches integration bugs. Spring Boot test slices let you choose exactly how much of the application to load for each test.

Beginner version:

> Unit tests mock the world; integration tests use the real thing. Spring Boot slices test one layer with the right amount of context loaded.

---

## 2. Definition

- Definition: Java backend testing combines unit tests (JUnit 5 + Mockito) with integration tests (Spring Boot slices + Testcontainers) to verify behavior at every layer of the application.
- Category: Backend quality engineering.
- Core idea: fast feedback from unit tests + real infrastructure confidence from Testcontainers.

---

## 3. JUnit 5 Annotations

```java
import org.junit.jupiter.api.*;

@DisplayName("Order Service Tests")
class OrderServiceTest {

    @BeforeEach
    void setUp() {
        // runs before each test method
    }

    @AfterEach
    void tearDown() { }

    @Test
    @DisplayName("should calculate total with discount")
    void shouldCalculateTotalWithDiscount() {
        // Arrange
        var order = new Order(List.of(new Item("SKU-1", 100), new Item("SKU-2", 50)));

        // Act
        var total = order.calculateTotal(0.10);  // 10% discount

        // Assert
        assertThat(total).isEqualTo(135.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.1, 0.5, 1.0})
    void shouldApplyAnyDiscount(double discount) {
        var order = new Order(List.of(new Item("SKU", 100)));
        assertThat(order.calculateTotal(discount)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Disabled("pending feature implementation")
    void shouldRefundOrder() { }
}
```

---

## 4. Mockito — Mocking Dependencies

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGatewayClient gatewayClient;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;  // dependencies auto-injected

    @Test
    void shouldChargeAndMarkOrderPaid() {
        // Arrange
        var order = new Order("ORD-123", 200.0, OrderStatus.PENDING);
        when(orderRepository.findById("ORD-123")).thenReturn(Optional.of(order));
        when(gatewayClient.charge(200.0, "CARD-456")).thenReturn(new ChargeResult("CHG-789", true));

        // Act
        paymentService.processPayment("ORD-123", "CARD-456");

        // Assert
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAID));
        verify(gatewayClient).charge(200.0, "CARD-456");
    }

    @Test
    void shouldThrowWhenGatewayFails() {
        when(orderRepository.findById(any())).thenReturn(Optional.of(new Order("ORD-1", 100, OrderStatus.PENDING)));
        when(gatewayClient.charge(any(), any())).thenThrow(new GatewayException("timeout"));

        assertThatThrownBy(() -> paymentService.processPayment("ORD-1", "CARD-1"))
            .isInstanceOf(PaymentFailedException.class)
            .hasMessageContaining("timeout");
    }
}
```

**`@Mock` vs `@MockBean`:**
- `@Mock` (Mockito only): creates a mock outside Spring context — fast, no Spring
- `@MockBean` (Spring Boot): creates a mock and replaces the real bean in the Spring context — use only with `@SpringBootTest` or slices

---

## 5. Spring Boot Test Slices

Spring Boot test slices load only the part of the application needed for the test.

| Slice | Annotation | What Loads | Use For |
|---|---|---|---|
| Web layer | `@WebMvcTest` | Controllers, filters, security | HTTP contract, validation, auth |
| Data layer | `@DataJpaTest` | JPA repositories, H2 in-memory DB | Repository queries, entity mapping |
| Service layer | Plain `@ExtendWith(MockitoExtension.class)` | Nothing Spring | Business logic (fastest) |
| Full context | `@SpringBootTest` | Full ApplicationContext | End-to-end integration |
| REST clients | `@RestClientTest` | RestTemplate/WebClient, Jackson | External API client testing |

---

## 6. `@WebMvcTest` — Controller Slice

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;  // mock the service — not testing it here

    @Test
    void shouldReturn200AndOrderJson() throws Exception {
        var order = new OrderDto("ORD-123", 200.0, "PENDING");
        when(orderService.getOrder("ORD-123")).thenReturn(order);

        mockMvc.perform(get("/api/orders/ORD-123")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("ORD-123"))
            .andExpect(jsonPath("$.total").value(200.0));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderService.getOrder("MISSING")).thenThrow(new OrderNotFoundException("MISSING"));

        mockMvc.perform(get("/api/orders/MISSING"))
            .andExpect(status().isNotFound());
    }
}
```

---

## 7. `@DataJpaTest` — Repository Slice

```java
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindPendingOrdersByCustomerId() {
        // Arrange: insert test data
        var customer = entityManager.persist(new Customer("CUST-1", "alice@example.com"));
        entityManager.persist(new Order(customer, 150.0, OrderStatus.PENDING));
        entityManager.persist(new Order(customer, 75.0, OrderStatus.PAID));
        entityManager.flush();

        // Act
        var pendingOrders = orderRepository.findByCustomerIdAndStatus("CUST-1", OrderStatus.PENDING);

        // Assert
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getTotal()).isEqualTo(150.0);
    }
}
```

`@DataJpaTest` uses an in-memory H2 database by default. To test against the real database type, use Testcontainers.

---

## 8. Testcontainers — Real Infrastructure in Tests

Testcontainers starts a real Docker container during tests and tears it down when the test finishes.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)  // do not replace with H2
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payments_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldPersistAndRetrieveOrder() {
        var saved = orderRepository.save(new Order("CUST-1", 200.0, OrderStatus.PENDING));
        var found = orderRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTotal()).isEqualTo(200.0);
    }
}
```

**Best practice:** Use `@Container static` so the container starts once per test class (not once per test method).

---

## 9. `@SpringBootTest` — Full Integration Test

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndRetrieveOrder() {
        // Create order
        var createRequest = new CreateOrderRequest("CUST-1", List.of(new OrderItem("SKU-1", 2, 50.0)));
        var created = restTemplate.postForEntity("/api/orders", createRequest, OrderDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Retrieve it
        var orderId = created.getBody().id();
        var retrieved = restTemplate.getForEntity("/api/orders/" + orderId, OrderDto.class);
        assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retrieved.getBody().total()).isEqualTo(100.0);
    }
}
```

---

## 10. Test Pyramid Strategy

```
          /\
         /  \
        / E2E \       (few, slow, expensive — Selenium/Playwright)
       /--------\
      / Integration \  (medium — @SpringBootTest + Testcontainers)
     /--------------\
    /   Unit Tests   \ (many, fast — JUnit 5 + Mockito)
   /------------------\
```

**Rule of thumb:** 70% unit, 20% integration (@DataJpaTest, @WebMvcTest), 10% full integration/E2E.

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Everything in `@SpringBootTest` | Tests are slow; full context loaded for simple logic | Use slices: `@WebMvcTest` for controllers, `@DataJpaTest` for repos |
| `@MockBean` outside `@SpringBootTest` | Spring context loaded unnecessarily | Use `@Mock` with `@ExtendWith(MockitoExtension.class)` for pure unit tests |
| Starting a new Testcontainer per test method | Each test class takes 5-10s to start | Use `static @Container` to share per class |
| Asserting only HTTP status, not body | Misses contract regressions | Assert response body structure and key fields |
| H2 in `@DataJpaTest` without Testcontainers | Postgres-specific SQL fails against H2 | Use `@AutoConfigureTestDatabase(replace=NONE)` + Testcontainers |

---

## 12. Interview Insight

Strong answer:

> I use a three-tier strategy: pure unit tests with JUnit 5 and Mockito for business logic, Spring Boot slices (`@WebMvcTest` for controllers, `@DataJpaTest` for repositories) for layer isolation, and Testcontainers for integration tests that need real PostgreSQL, Redis, or Kafka. Testcontainers eliminates the H2-vs-PostgreSQL compatibility problem because the test runs against the actual database engine. I use `static @Container` to start the container once per test class rather than once per test method to keep integration test suites under 2-3 minutes.

Follow-up trap:

> What is the difference between `@MockBean` and `@Mock`?

Good answer:

> `@Mock` creates a Mockito mock outside the Spring container — it's fast and has no Spring overhead. Use it in unit tests with `@ExtendWith(MockitoExtension.class)`. `@MockBean` creates a Mockito mock AND registers it as a Spring bean, replacing any real bean with that type in the ApplicationContext. Use it only in `@WebMvcTest` or `@SpringBootTest` when you need the Spring context but want to replace a dependency with a mock. The downside of `@MockBean` is that it resets the ApplicationContext cache, making tests slower.

---

## 13. Revision Notes

- One-line summary: Unit tests mock dependencies; test slices load only the right Spring context; Testcontainers provide real infrastructure.
- Three keywords: slice, mock, container.
- One interview trap: `@MockBean` resets the Spring context cache — overuse slows the suite.
- Memory trick: Pyramid base = fast unit tests; tip = slow full tests.
