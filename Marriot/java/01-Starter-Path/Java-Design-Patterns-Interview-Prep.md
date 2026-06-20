# Java Design Patterns Interview Prep

Target: Marriott Tech Accelerator / Intervue Java backend round.

This sheet covers the most important design patterns from an interview perspective:
- Creational patterns
- Structural patterns
- Behavioral patterns
- Java/Spring real-world mapping
- Hot questions and traps

The goal is not to memorize pattern names mechanically.

The real goal:

```text
When the interviewer describes a design problem, you should quickly identify the pattern,
explain why it fits, and write a small Java example.
```

---

## 1. Pattern Priority Meter

| Pattern | Category | Interview Priority | Why It Is Asked |
|---|---|---:|---|
| Singleton | Creational | Very high | Thread safety, object lifecycle, Spring beans |
| Factory Method | Creational | Very high | Object creation based on type |
| Abstract Factory | Creational | Medium | Families of related objects |
| Builder | Creational | Very high | Complex object creation, immutable objects |
| Prototype | Creational | Medium | Cloning/copying objects |
| Adapter | Structural | High | Make incompatible APIs work together |
| Decorator | Structural | High | Add behavior without changing original class |
| Proxy | Structural | Very high | Spring AOP, lazy loading, security, transactions |
| Facade | Structural | Very high | Simplify complex subsystem |
| Composite | Structural | Medium | Tree structures |
| Bridge | Structural | Medium | Decouple abstraction from implementation |
| Flyweight | Structural | Low-medium | Memory optimization for many similar objects |
| Strategy | Behavioral | Very high | Replace if-else with interchangeable algorithms |
| Observer | Behavioral | High | Event-driven design |
| Template Method | Behavioral | High | Fixed algorithm skeleton with customizable steps |
| Chain of Responsibility | Behavioral | Very high | Filters, validators, middleware, handlers |
| Command | Behavioral | High | Encapsulate request/action |
| State | Behavioral | Medium-high | Object behavior changes by state |
| Iterator | Behavioral | Medium | Sequential access without exposing internals |
| Mediator | Behavioral | Medium | Reduce object-to-object coupling |
| Memento | Behavioral | Low-medium | Undo/restore state |

---

## 2. Pattern Selection Mind Map

### If The Problem Says...

| Interviewer Says | Think Pattern |
|---|---|
| "Only one instance" | Singleton |
| "Create object based on input type" | Factory Method |
| "Create families of related objects" | Abstract Factory |
| "Object has many optional fields" | Builder |
| "Copy existing object" | Prototype |
| "Existing API does not match expected interface" | Adapter |
| "Add behavior dynamically" | Decorator |
| "Control access to real object" | Proxy |
| "Hide complex subsystem behind simple API" | Facade |
| "Tree-like part-whole structure" | Composite |
| "Multiple dimensions vary independently" | Bridge |
| "Many small repeated objects consuming memory" | Flyweight |
| "Choose algorithm at runtime" | Strategy |
| "Notify many subscribers when state changes" | Observer |
| "Common flow, few customizable steps" | Template Method |
| "Pass request through multiple handlers" | Chain of Responsibility |
| "Represent action as object" | Command |
| "Behavior depends on current state" | State |
| "Traverse collection without exposing internals" | Iterator |
| "Many objects communicating messily" | Mediator |
| "Undo/restore previous state" | Memento |

### Interview Safety Line

```text
Design patterns are not goals by themselves. I use them when they reduce coupling,
remove duplication, make object creation cleaner, or make behavior easier to extend.
```

---

## 3. Creational Patterns

Creational patterns deal with object creation.

They answer:

```text
How should objects be created without making the code tightly coupled or hard to extend?
```

---

## 4. Singleton Pattern

### Intent

Ensure a class has only one instance and provide a global access point to it.

### Mental Model

One application-wide object.

Examples:
- Configuration manager
- Logger
- Cache manager
- Thread pool manager

### Basic Singleton

```java
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    private AppConfig() {
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }
}
```

### Lazy Singleton With Double-Checked Locking

```java
public class LazySingleton {
    private static volatile LazySingleton instance;

    private LazySingleton() {
    }

    public static LazySingleton getInstance() {
        if (instance == null) {
            synchronized (LazySingleton.class) {
                if (instance == null) {
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }
}
```

### Why `volatile`?

Object creation is not a single atomic step.

It roughly has:
1. Allocate memory.
2. Initialize object.
3. Assign reference.

Without `volatile`, instruction reordering can expose a partially constructed object.

### Best Simple Singleton: Enum

```java
public enum AppSingleton {
    INSTANCE;

    public void doWork() {
        System.out.println("Working");
    }
}
```

Usage:

```java
AppSingleton.INSTANCE.doWork();
```

### Why Enum Singleton Is Strong

- Thread-safe by default
- Serialization-safe
- Reflection-resistant compared with normal constructors
- Simple

### When To Use

- One shared object is truly needed.
- Object is stateless or carefully thread-safe.
- Creating many instances would be wasteful or incorrect.

### When Not To Use

- When it hides dependencies.
- When it makes testing hard.
- When global mutable state creates race conditions.
- When dependency injection container can manage lifecycle better.

### Spring Mapping

Spring beans are singleton by default, but Spring singleton means:

```text
One bean instance per Spring container.
```

Not necessarily one instance per JVM.

### Hot Questions

| Question | Strong Answer |
|---|---|
| Is singleton thread-safe? | Depends on implementation |
| Best singleton in Java? | Enum singleton for simple cases |
| Why volatile in double-check locking? | Prevent visibility/reordering issues |
| Can singleton break? | Reflection, serialization, cloning, class loaders |
| Spring singleton vs Java singleton? | Spring singleton is one bean per container |

### Interview Trap

Wrong:

```text
Singleton always means thread-safe.
```

Correct:

```text
Singleton only means one instance. Thread safety depends on construction and state handling.
```

---

## 5. Factory Method Pattern

### Intent

Create objects without exposing creation logic to client code.

### Mental Model

Give a type, get the correct implementation.

Example:

```text
"CARD" -> CardPaymentProcessor
"UPI" -> UpiPaymentProcessor
"WALLET" -> WalletPaymentProcessor
```

### Problem Without Factory

```java
if ("CARD".equals(type)) {
    processor = new CardPaymentProcessor();
} else if ("UPI".equals(type)) {
    processor = new UpiPaymentProcessor();
}
```

Problem:
- Object creation logic spreads everywhere.
- Adding a new type means changing multiple places.

### Java Example

```java
interface PaymentProcessor {
    void pay(int amount);
}

class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public void pay(int amount) {
        System.out.println("Paid by card: " + amount);
    }
}

class UpiPaymentProcessor implements PaymentProcessor {
    @Override
    public void pay(int amount) {
        System.out.println("Paid by UPI: " + amount);
    }
}

class PaymentProcessorFactory {
    public static PaymentProcessor getProcessor(String type) {
        if ("CARD".equalsIgnoreCase(type)) {
            return new CardPaymentProcessor();
        }
        if ("UPI".equalsIgnoreCase(type)) {
            return new UpiPaymentProcessor();
        }
        throw new IllegalArgumentException("Unsupported payment type: " + type);
    }
}
```

Usage:

```java
PaymentProcessor processor = PaymentProcessorFactory.getProcessor("CARD");
processor.pay(1000);
```

### Better Factory With Map

```java
class PaymentProcessorFactory {
    private final Map<String, Supplier<PaymentProcessor>> registry = new HashMap<>();

    PaymentProcessorFactory() {
        registry.put("CARD", CardPaymentProcessor::new);
        registry.put("UPI", UpiPaymentProcessor::new);
    }

    PaymentProcessor getProcessor(String type) {
        Supplier<PaymentProcessor> supplier = registry.get(type.toUpperCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unsupported payment type: " + type);
        }
        return supplier.get();
    }
}
```

### When To Use

- Object type depends on input.
- Client should depend on interface, not concrete class.
- Creation logic is complex.
- You want centralized creation.

### When Not To Use

- Only one implementation exists.
- Constructor call is simple and unlikely to change.
- Factory adds unnecessary indirection.

### Spring Mapping

Spring often replaces manual factories with dependency injection.

Example idea:

```java
Map<String, PaymentProcessor> processors;
```

Spring can inject all implementations, and service can select by key.

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why factory? | Encapsulates object creation |
| Factory vs constructor? | Factory can return different implementations |
| Factory vs strategy? | Factory creates objects; strategy chooses behavior |
| Factory drawback? | Can become large if too many types |

---

## 6. Abstract Factory Pattern

### Intent

Create families of related objects without specifying their concrete classes.

### Mental Model

One factory creates a consistent family.

Example:

```text
LuxuryHotelFactory -> LuxuryRoom + LuxuryRestaurant
BudgetHotelFactory -> BudgetRoom + BudgetRestaurant
```

### Java Example

```java
interface Room {
    void describe();
}

interface Restaurant {
    void describe();
}

class LuxuryRoom implements Room {
    public void describe() {
        System.out.println("Luxury room");
    }
}

class BudgetRoom implements Room {
    public void describe() {
        System.out.println("Budget room");
    }
}

class LuxuryRestaurant implements Restaurant {
    public void describe() {
        System.out.println("Luxury restaurant");
    }
}

class BudgetRestaurant implements Restaurant {
    public void describe() {
        System.out.println("Budget restaurant");
    }
}

interface HotelFactory {
    Room createRoom();
    Restaurant createRestaurant();
}

class LuxuryHotelFactory implements HotelFactory {
    public Room createRoom() {
        return new LuxuryRoom();
    }

    public Restaurant createRestaurant() {
        return new LuxuryRestaurant();
    }
}

class BudgetHotelFactory implements HotelFactory {
    public Room createRoom() {
        return new BudgetRoom();
    }

    public Restaurant createRestaurant() {
        return new BudgetRestaurant();
    }
}
```

Usage:

```java
HotelFactory factory = new LuxuryHotelFactory();
Room room = factory.createRoom();
Restaurant restaurant = factory.createRestaurant();
```

### Factory Method vs Abstract Factory

| Factory Method | Abstract Factory |
|---|---|
| Creates one type of object | Creates family of related objects |
| Usually one factory method | Multiple creation methods |
| Simpler | More structured |

### When To Use

- Products must be used together.
- You need consistency across object families.
- You need to switch product family at runtime/config.

### When Not To Use

- You only create one object type.
- Families are not really related.
- It makes design too abstract.

---

## 7. Builder Pattern

### Intent

Build complex objects step by step.

### Mental Model

Readable object creation when constructor has many parameters.

### Problem Without Builder

```java
Booking booking = new Booking("B1", "H1", "Aravind", true, false, 2, "KING", null);
```

Problems:
- Hard to read.
- Easy to pass parameters in wrong order.
- Too many constructors.

### Java Example

```java
public class Booking {
    private final String bookingId;
    private final String hotelId;
    private final String guestName;
    private final int nights;
    private final boolean breakfastIncluded;
    private final String roomType;

    private Booking(Builder builder) {
        this.bookingId = builder.bookingId;
        this.hotelId = builder.hotelId;
        this.guestName = builder.guestName;
        this.nights = builder.nights;
        this.breakfastIncluded = builder.breakfastIncluded;
        this.roomType = builder.roomType;
    }

    public static class Builder {
        private String bookingId;
        private String hotelId;
        private String guestName;
        private int nights;
        private boolean breakfastIncluded;
        private String roomType;

        public Builder bookingId(String bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder hotelId(String hotelId) {
            this.hotelId = hotelId;
            return this;
        }

        public Builder guestName(String guestName) {
            this.guestName = guestName;
            return this;
        }

        public Builder nights(int nights) {
            this.nights = nights;
            return this;
        }

        public Builder breakfastIncluded(boolean breakfastIncluded) {
            this.breakfastIncluded = breakfastIncluded;
            return this;
        }

        public Builder roomType(String roomType) {
            this.roomType = roomType;
            return this;
        }

        public Booking build() {
            if (bookingId == null || hotelId == null || guestName == null) {
                throw new IllegalStateException("Required fields missing");
            }
            if (nights <= 0) {
                throw new IllegalStateException("Nights must be positive");
            }
            return new Booking(this);
        }
    }
}
```

Usage:

```java
Booking booking = new Booking.Builder()
    .bookingId("B1")
    .hotelId("H1")
    .guestName("Aravind")
    .nights(3)
    .breakfastIncluded(true)
    .roomType("KING")
    .build();
```

### When To Use

- Many optional fields.
- Object should be immutable.
- Object creation requires validation.
- Constructor becomes unreadable.

### When Not To Use

- Object has only two or three fields.
- Object creation is simple.
- Builder adds ceremony without value.

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why builder? | Readable construction of complex objects |
| Builder vs factory? | Builder assembles complex object; factory chooses implementation |
| Builder and immutability? | Builder can construct immutable object cleanly |
| Lombok `@Builder`? | Generates builder boilerplate |

---

## 8. Prototype Pattern

### Intent

Create new objects by copying existing objects.

### Mental Model

Use an existing object as a template.

### Java Example

```java
class RoomTemplate implements Cloneable {
    private String type;
    private int basePrice;

    RoomTemplate(String type, int basePrice) {
        this.type = type;
        this.basePrice = basePrice;
    }

    @Override
    protected RoomTemplate clone() {
        try {
            return (RoomTemplate) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }
}
```

Usage:

```java
RoomTemplate standard = new RoomTemplate("STANDARD", 5000);
RoomTemplate copy = standard.clone();
```

### Shallow vs Deep Copy

| Shallow Copy | Deep Copy |
|---|---|
| Copies object only | Copies object and nested objects |
| Nested references shared | Nested objects independent |

### Better Alternative: Copy Constructor

```java
class RoomTemplate {
    private String type;
    private int basePrice;

    RoomTemplate(String type, int basePrice) {
        this.type = type;
        this.basePrice = basePrice;
    }

    RoomTemplate(RoomTemplate other) {
        this.type = other.type;
        this.basePrice = other.basePrice;
    }
}
```

### When To Use

- Object creation is expensive.
- Similar objects are created repeatedly.
- Runtime object configuration should be copied.

### When Not To Use

- Object is simple.
- Deep copy is complicated.
- Cloneable confusion outweighs benefit.

### Hot Question

```text
Prototype is about cloning/copying existing objects. In Java, I prefer copy constructors
or explicit copy methods over Cloneable in most production code because Cloneable is awkward
and shallow by default.
```

---

## 9. Structural Patterns

Structural patterns deal with object composition.

They answer:

```text
How do classes and objects fit together cleanly?
```

---

## 10. Adapter Pattern

### Intent

Make incompatible interfaces work together.

### Mental Model

Travel adapter.

Your charger has one plug shape. Wall socket has another. Adapter converts.

### Java Example

Existing external API:

```java
class LegacyPaymentGateway {
    public void makePayment(int amountInPaise) {
        System.out.println("Paid paise: " + amountInPaise);
    }
}
```

Expected interface:

```java
interface PaymentClient {
    void payInRupees(int rupees);
}
```

Adapter:

```java
class LegacyPaymentAdapter implements PaymentClient {
    private final LegacyPaymentGateway gateway;

    LegacyPaymentAdapter(LegacyPaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void payInRupees(int rupees) {
        gateway.makePayment(rupees * 100);
    }
}
```

Usage:

```java
PaymentClient client = new LegacyPaymentAdapter(new LegacyPaymentGateway());
client.payInRupees(100);
```

### When To Use

- Integrating legacy system.
- External API does not match your interface.
- You want your code to depend on your own interface.

### When Not To Use

- You can directly change the incompatible class.
- You are only renaming methods without meaningful compatibility need.

### Java/Spring Mapping

- `InputStreamReader` adapts byte stream to character reader.
- Spring adapters often convert framework contracts.
- REST clients can be wrapped behind internal adapter interfaces.

### Hot Question

```text
Adapter changes interface compatibility. Decorator adds behavior. Proxy controls access.
```

---

## 11. Decorator Pattern

### Intent

Add behavior to an object dynamically without changing its class.

### Mental Model

Wrap the object with extra layers.

Coffee example:

```text
Coffee
    -> MilkDecorator
    -> SugarDecorator
```

### Java Example

```java
interface PriceCalculator {
    int calculate();
}

class BaseRoomPrice implements PriceCalculator {
    public int calculate() {
        return 5000;
    }
}

class BreakfastDecorator implements PriceCalculator {
    private final PriceCalculator delegate;

    BreakfastDecorator(PriceCalculator delegate) {
        this.delegate = delegate;
    }

    public int calculate() {
        return delegate.calculate() + 800;
    }
}

class SpaDecorator implements PriceCalculator {
    private final PriceCalculator delegate;

    SpaDecorator(PriceCalculator delegate) {
        this.delegate = delegate;
    }

    public int calculate() {
        return delegate.calculate() + 1500;
    }
}
```

Usage:

```java
PriceCalculator calculator = new SpaDecorator(
    new BreakfastDecorator(
        new BaseRoomPrice()
    )
);

System.out.println(calculator.calculate()); // 7300
```

### When To Use

- Need add-ons dynamically.
- Inheritance would create too many subclasses.
- You want composable behavior.

### When Not To Use

- Too many wrappers make debugging hard.
- Behavior order is confusing.
- Simple inheritance/composition is enough.

### Java Mapping

Classic Java IO:

```java
BufferedInputStream bis = new BufferedInputStream(
    new FileInputStream("data.txt")
);
```

### Decorator vs Proxy

| Decorator | Proxy |
|---|---|
| Adds new behavior/responsibility | Controls access to real object |
| Client usually wants enhanced behavior | Client may not know proxy is controlling access |
| Many decorators can be stacked | Proxy usually represents one real object |

---

## 12. Proxy Pattern

### Intent

Provide a substitute object that controls access to a real object.

### Mental Model

A gatekeeper in front of the real service.

Proxy can add:
- Security
- Logging
- Caching
- Lazy loading
- Transactions
- Rate limiting
- Remote call handling

### Java Example: Security Proxy

```java
interface BookingService {
    void cancelBooking(String bookingId, String userRole);
}

class RealBookingService implements BookingService {
    public void cancelBooking(String bookingId, String userRole) {
        System.out.println("Booking cancelled: " + bookingId);
    }
}

class BookingServiceSecurityProxy implements BookingService {
    private final BookingService target;

    BookingServiceSecurityProxy(BookingService target) {
        this.target = target;
    }

    public void cancelBooking(String bookingId, String userRole) {
        if (!"ADMIN".equals(userRole)) {
            throw new SecurityException("Only admin can cancel booking");
        }
        target.cancelBooking(bookingId, userRole);
    }
}
```

Usage:

```java
BookingService service = new BookingServiceSecurityProxy(new RealBookingService());
service.cancelBooking("B1", "ADMIN");
```

### Types Of Proxy

| Proxy Type | Purpose |
|---|---|
| Virtual proxy | Lazy object creation |
| Protection proxy | Access control |
| Remote proxy | Represents remote object |
| Caching proxy | Cache expensive result |
| Logging proxy | Add logs around call |
| Smart proxy | Extra behavior like reference counting |

### Spring Mapping

Spring heavily uses proxies:
- `@Transactional`
- `@Async`
- `@Cacheable`
- Spring AOP
- Security method interceptors

Example:

```java
@Transactional
public void createBooking() {
    // Spring proxy starts transaction before method
    // and commits/rolls back after method
}
```

### JDK Dynamic Proxy vs CGLIB Proxy

| JDK Dynamic Proxy | CGLIB Proxy |
|---|---|
| Interface-based | Class subclass-based |
| Requires interface | Can proxy concrete class |
| Built into JDK | Uses bytecode generation |
| Common in Spring when interface exists | Used when no interface |

### Spring Proxy Trap

Self-invocation issue:

```java
class BookingService {
    public void outer() {
        inner();
    }

    @Transactional
    public void inner() {
    }
}
```

If `outer` calls `inner` inside same class, proxy may be bypassed.

Strong answer:

```text
Spring AOP is proxy-based. Internal self-invocation does not go through the proxy, so
annotations like @Transactional may not apply.
```

### When To Use

- Need access control.
- Need lazy loading.
- Need cross-cutting behavior.
- Need remote object abstraction.

### When Not To Use

- Direct call is enough.
- Proxy hides too much behavior.
- Debugging indirection becomes painful.

---

## 13. Facade Pattern

### Intent

Provide a simple interface to a complex subsystem.

### Mental Model

Hotel reception desk.

You ask reception for check-in. Reception internally talks to:
- Room service
- Payment
- Identity verification
- Notification
- Loyalty system

### Java Example

```java
class RoomInventoryService {
    void reserveRoom(String hotelId) {
        System.out.println("Room reserved");
    }
}

class PaymentService {
    void charge(int amount) {
        System.out.println("Payment charged: " + amount);
    }
}

class NotificationService {
    void sendConfirmation(String email) {
        System.out.println("Confirmation sent to " + email);
    }
}

class BookingFacade {
    private final RoomInventoryService inventoryService = new RoomInventoryService();
    private final PaymentService paymentService = new PaymentService();
    private final NotificationService notificationService = new NotificationService();

    public void bookRoom(String hotelId, int amount, String email) {
        inventoryService.reserveRoom(hotelId);
        paymentService.charge(amount);
        notificationService.sendConfirmation(email);
    }
}
```

Usage:

```java
BookingFacade facade = new BookingFacade();
facade.bookRoom("H1", 5000, "guest@example.com");
```

### When To Use

- Subsystem is complex.
- Client should not know internal workflow.
- You need a clean API for use case.
- You want to reduce coupling.

### When Not To Use

- Facade becomes a giant god service.
- It hides important errors too much.
- It becomes a dumping ground for unrelated flows.

### Spring Mapping

Application service layer often acts like a facade:

```text
Controller -> BookingApplicationService -> multiple domain/infrastructure services
```

### Facade vs Adapter

| Facade | Adapter |
|---|---|
| Simplifies complex subsystem | Converts incompatible interface |
| Often wraps many classes | Often wraps one incompatible class |
| Goal is simplicity | Goal is compatibility |

---

## 14. Composite Pattern

### Intent

Treat individual objects and groups of objects uniformly.

### Mental Model

Tree structure.

Example:

```text
Menu
  - MenuItem
  - SubMenu
      - MenuItem
```

### Java Example

```java
interface HotelComponent {
    int getPrice();
}

class Room implements HotelComponent {
    private final int price;

    Room(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}

class RoomPackage implements HotelComponent {
    private final List<HotelComponent> components = new ArrayList<>();

    void add(HotelComponent component) {
        components.add(component);
    }

    public int getPrice() {
        return components.stream()
            .mapToInt(HotelComponent::getPrice)
            .sum();
    }
}
```

Usage:

```java
RoomPackage packageDeal = new RoomPackage();
packageDeal.add(new Room(5000));
packageDeal.add(new Room(7000));

System.out.println(packageDeal.getPrice()); // 12000
```

### When To Use

- Tree structures.
- Individual and group should share same interface.
- Recursive operations are common.

### When Not To Use

- Structure is flat.
- Group and leaf operations are very different.

---

## 15. Bridge Pattern

### Intent

Decouple abstraction from implementation so both can vary independently.

### Mental Model

Two dimensions of variation.

Example:

```text
Notification type: Email, SMS
Message sender: AWS, Twilio
```

Without bridge, combinations explode.

### Java Example

```java
interface MessageSender {
    void send(String message);
}

class AwsSender implements MessageSender {
    public void send(String message) {
        System.out.println("AWS sends: " + message);
    }
}

class TwilioSender implements MessageSender {
    public void send(String message) {
        System.out.println("Twilio sends: " + message);
    }
}

abstract class Notification {
    protected final MessageSender sender;

    Notification(MessageSender sender) {
        this.sender = sender;
    }

    abstract void notifyUser(String message);
}

class BookingNotification extends Notification {
    BookingNotification(MessageSender sender) {
        super(sender);
    }

    void notifyUser(String message) {
        sender.send("Booking: " + message);
    }
}
```

Usage:

```java
Notification notification = new BookingNotification(new AwsSender());
notification.notifyUser("Confirmed");
```

### When To Use

- Two independent dimensions vary.
- Inheritance hierarchy is exploding.
- Need runtime implementation swapping.

### Bridge vs Adapter

| Bridge | Adapter |
|---|---|
| Designed upfront | Used after incompatibility exists |
| Separates abstraction and implementation | Makes incompatible API fit |

---

## 16. Flyweight Pattern

### Intent

Reduce memory usage by sharing common object state.

### Mental Model

Many similar objects share reusable intrinsic data.

Example:
- Character objects in text editor
- Icons
- Room type metadata
- Product category metadata

### Java Example

```java
class RoomType {
    private final String type;
    private final String description;

    RoomType(String type, String description) {
        this.type = type;
        this.description = description;
    }
}

class RoomTypeFactory {
    private final Map<String, RoomType> cache = new HashMap<>();

    RoomType getRoomType(String type) {
        return cache.computeIfAbsent(type, key -> new RoomType(key, key + " room"));
    }
}
```

Usage:

```java
RoomTypeFactory factory = new RoomTypeFactory();
RoomType standard1 = factory.getRoomType("STANDARD");
RoomType standard2 = factory.getRoomType("STANDARD");

System.out.println(standard1 == standard2); // true
```

### When To Use

- Huge number of similar objects.
- Shared state can be separated from unique state.
- Memory optimization matters.

### When Not To Use

- Few objects.
- Shared state separation makes code confusing.

---

## 17. Behavioral Patterns

Behavioral patterns deal with communication and responsibility between objects.

They answer:

```text
How should objects collaborate without becoming tightly coupled?
```

---

## 18. Strategy Pattern

### Intent

Define a family of algorithms and make them interchangeable.

### Mental Model

Same task, different strategy.

Example:

```text
Discount calculation:
GOLD -> 20%
SILVER -> 10%
REGULAR -> 0%
```

### Problem Without Strategy

```java
if ("GOLD".equals(customerType)) {
    return amount * 0.8;
} else if ("SILVER".equals(customerType)) {
    return amount * 0.9;
}
```

Problems:
- If-else grows.
- Harder to test each rule separately.
- Violates open/closed principle.

### Java Example

```java
interface DiscountStrategy {
    int applyDiscount(int amount);
}

class GoldDiscountStrategy implements DiscountStrategy {
    public int applyDiscount(int amount) {
        return amount - (amount * 20 / 100);
    }
}

class SilverDiscountStrategy implements DiscountStrategy {
    public int applyDiscount(int amount) {
        return amount - (amount * 10 / 100);
    }
}

class NoDiscountStrategy implements DiscountStrategy {
    public int applyDiscount(int amount) {
        return amount;
    }
}
```

Context:

```java
class PriceService {
    private final DiscountStrategy discountStrategy;

    PriceService(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }

    int finalPrice(int amount) {
        return discountStrategy.applyDiscount(amount);
    }
}
```

Usage:

```java
PriceService service = new PriceService(new GoldDiscountStrategy());
System.out.println(service.finalPrice(1000)); // 800
```

### Strategy With Lambda

```java
DiscountStrategy gold = amount -> amount - (amount * 20 / 100);
```

### Spring Mapping

Inject all strategies:

```java
Map<String, DiscountStrategy> strategies;
```

Then select by customer type.

### When To Use

- Multiple algorithms.
- Algorithm selected at runtime.
- You want to remove complex if-else.
- Each algorithm should be independently testable.

### When Not To Use

- Only one simple algorithm.
- If-else is tiny and unlikely to grow.

### Hot Questions

| Question | Strong Answer |
|---|---|
| Strategy vs Factory? | Factory creates object; strategy executes interchangeable behavior |
| Strategy vs State? | Strategy chosen by client/context; State changes as object state changes |
| Java example? | Comparator is a strategy |

---

## 19. Observer Pattern

### Intent

When one object changes, notify multiple dependent objects automatically.

### Mental Model

YouTube channel and subscribers.

Booking created:
- Send email
- Send SMS
- Publish Kafka event
- Update loyalty points

### Java Example

```java
interface BookingObserver {
    void onBookingCreated(String bookingId);
}

class EmailObserver implements BookingObserver {
    public void onBookingCreated(String bookingId) {
        System.out.println("Email sent for " + bookingId);
    }
}

class SmsObserver implements BookingObserver {
    public void onBookingCreated(String bookingId) {
        System.out.println("SMS sent for " + bookingId);
    }
}

class BookingEventPublisher {
    private final List<BookingObserver> observers = new ArrayList<>();

    void register(BookingObserver observer) {
        observers.add(observer);
    }

    void publishBookingCreated(String bookingId) {
        for (BookingObserver observer : observers) {
            observer.onBookingCreated(bookingId);
        }
    }
}
```

Usage:

```java
BookingEventPublisher publisher = new BookingEventPublisher();
publisher.register(new EmailObserver());
publisher.register(new SmsObserver());

publisher.publishBookingCreated("B1");
```

### When To Use

- One event triggers many reactions.
- Publisher should not know all concrete subscribers.
- Event-driven behavior.

### When Not To Use

- Execution order is critical and complex.
- Failures need strict transactional behavior.
- Too many observers make debugging hard.

### Spring Mapping

Spring events:

```java
applicationEventPublisher.publishEvent(new BookingCreatedEvent("B1"));
```

Kafka consumers also follow observer/event-driven style at system level.

### Hot Questions

| Question | Strong Answer |
|---|---|
| Observer use case? | Event notification to multiple subscribers |
| Drawback? | Harder debugging, ordering/failure complexity |
| Sync vs async observer? | Observer can be either; implementation decides |

---

## 20. Template Method Pattern

### Intent

Define the skeleton of an algorithm in a base class and let subclasses customize specific steps.

### Mental Model

Recipe with fixed steps, but ingredients vary.

### Java Example

```java
abstract class BookingProcessor {
    public final void processBooking() {
        validate();
        reserveInventory();
        collectPayment();
        sendConfirmation();
    }

    protected abstract void validate();

    protected void reserveInventory() {
        System.out.println("Inventory reserved");
    }

    protected abstract void collectPayment();

    protected void sendConfirmation() {
        System.out.println("Confirmation sent");
    }
}

class PrepaidBookingProcessor extends BookingProcessor {
    protected void validate() {
        System.out.println("Validate prepaid booking");
    }

    protected void collectPayment() {
        System.out.println("Collect full payment");
    }
}
```

Usage:

```java
BookingProcessor processor = new PrepaidBookingProcessor();
processor.processBooking();
```

### Why `final` On Template Method?

```text
To prevent subclasses from changing the algorithm order.
```

### When To Use

- Algorithm has fixed structure.
- Some steps vary by subclass.
- You want reuse of common flow.

### When Not To Use

- Too much subclassing.
- Composition/strategy would be more flexible.
- Subclasses need to change entire flow.

### Spring Mapping

Examples:
- `JdbcTemplate`
- `RestTemplate`

They manage common workflow and let caller provide specific logic.

### Template vs Strategy

| Template Method | Strategy |
|---|---|
| Inheritance-based | Composition-based |
| Base class fixes algorithm skeleton | Strategy object provides algorithm |
| Compile-time subclassing | Runtime algorithm swapping |

---

## 21. Chain Of Responsibility Pattern

### Intent

Pass a request through a chain of handlers until one handles it or all process it.

### Mental Model

Airport security checks:
1. ID check
2. Baggage check
3. Boarding pass check

### Java Example: Booking Validation Chain

```java
class BookingRequest {
    String hotelId;
    int nights;
    boolean paymentValid;
}

interface BookingValidator {
    void setNext(BookingValidator next);
    void validate(BookingRequest request);
}

abstract class BaseBookingValidator implements BookingValidator {
    private BookingValidator next;

    public void setNext(BookingValidator next) {
        this.next = next;
    }

    protected void validateNext(BookingRequest request) {
        if (next != null) {
            next.validate(request);
        }
    }
}

class HotelValidator extends BaseBookingValidator {
    public void validate(BookingRequest request) {
        if (request.hotelId == null) {
            throw new IllegalArgumentException("Hotel is required");
        }
        validateNext(request);
    }
}

class NightsValidator extends BaseBookingValidator {
    public void validate(BookingRequest request) {
        if (request.nights <= 0) {
            throw new IllegalArgumentException("Nights must be positive");
        }
        validateNext(request);
    }
}

class PaymentValidator extends BaseBookingValidator {
    public void validate(BookingRequest request) {
        if (!request.paymentValid) {
            throw new IllegalArgumentException("Payment invalid");
        }
        validateNext(request);
    }
}
```

Usage:

```java
BookingValidator hotel = new HotelValidator();
BookingValidator nights = new NightsValidator();
BookingValidator payment = new PaymentValidator();

hotel.setNext(nights);
nights.setNext(payment);

hotel.validate(request);
```

### When To Use

- Multiple handlers can process a request.
- You want flexible ordering.
- Sender should not know exact handler.
- Filters/interceptors/validators.

### When Not To Use

- All handlers and order are fixed and simple.
- Debugging chain becomes difficult.
- Request must always be handled but chain may silently drop it.

### Java/Spring Mapping

- Servlet filters
- Spring Security filter chain
- Interceptors
- Validation pipeline
- Logging middleware

### Hot Questions

| Question | Strong Answer |
|---|---|
| Use case? | Filters, validators, middleware |
| Benefit? | Decouples sender from handlers |
| Drawback? | Debugging order/failure can be harder |

---

## 22. Command Pattern

### Intent

Encapsulate a request as an object.

### Mental Model

A remote control button stores a command.

### Java Example

```java
interface Command {
    void execute();
}

class CancelBookingCommand implements Command {
    private final String bookingId;

    CancelBookingCommand(String bookingId) {
        this.bookingId = bookingId;
    }

    public void execute() {
        System.out.println("Cancelled booking: " + bookingId);
    }
}

class CommandExecutor {
    void execute(Command command) {
        command.execute();
    }
}
```

Usage:

```java
Command command = new CancelBookingCommand("B1");
new CommandExecutor().execute(command);
```

### When To Use

- Queue actions.
- Retry actions.
- Log actions.
- Undo actions.
- Decouple invoker from receiver.

### Backend Mapping

- Job queue task
- Retryable operation
- Audit log command
- CQRS command object

### Command vs Strategy

| Command | Strategy |
|---|---|
| Encapsulates action/request | Encapsulates algorithm |
| Often has execute method | Often has calculate/process method |
| Can queue, log, undo | Used to swap behavior |

---

## 23. State Pattern

### Intent

Allow an object to change behavior when its internal state changes.

### Mental Model

Booking behaves differently based on status:
- Created
- Confirmed
- Cancelled

### Problem Without State

```java
if (status == CREATED) {
    // behavior
} else if (status == CONFIRMED) {
    // behavior
} else if (status == CANCELLED) {
    // behavior
}
```

This grows messy.

### Java Example

```java
interface BookingState {
    void cancel(BookingContext context);
}

class CreatedState implements BookingState {
    public void cancel(BookingContext context) {
        System.out.println("Booking cancelled from created state");
        context.setState(new CancelledState());
    }
}

class ConfirmedState implements BookingState {
    public void cancel(BookingContext context) {
        System.out.println("Refund initiated and booking cancelled");
        context.setState(new CancelledState());
    }
}

class CancelledState implements BookingState {
    public void cancel(BookingContext context) {
        throw new IllegalStateException("Already cancelled");
    }
}

class BookingContext {
    private BookingState state;

    BookingContext(BookingState state) {
        this.state = state;
    }

    void setState(BookingState state) {
        this.state = state;
    }

    void cancel() {
        state.cancel(this);
    }
}
```

Usage:

```java
BookingContext booking = new BookingContext(new ConfirmedState());
booking.cancel();
```

### When To Use

- Behavior depends heavily on state.
- Many if-else/switch statements based on state.
- State transitions are important.

### When Not To Use

- Only two simple states.
- State logic is trivial.

### State vs Strategy

| State | Strategy |
|---|---|
| Behavior changes as internal state changes | Algorithm selected externally/contextually |
| State objects may transition context | Strategy usually does not change context state |

---

## 24. Iterator Pattern

### Intent

Provide a way to access elements sequentially without exposing internal representation.

### Java Mapping

Java Collections use Iterator.

```java
List<String> names = Arrays.asList("A", "B", "C");

Iterator<String> iterator = names.iterator();

while (iterator.hasNext()) {
    System.out.println(iterator.next());
}
```

### Why Iterator?

- Hides collection internals.
- Provides common traversal API.
- Supports safe removal through iterator.

### Safe Remove

```java
List<String> names = new ArrayList<>(Arrays.asList("A", "B", "C"));
Iterator<String> iterator = names.iterator();

while (iterator.hasNext()) {
    String value = iterator.next();
    if ("B".equals(value)) {
        iterator.remove();
    }
}
```

### Hot Question

```text
Iterator pattern is already built into Java collections. It lets clients traverse data
without knowing whether the collection is backed by array, linked nodes, tree, or another
structure.
```

---

## 25. Mediator Pattern

### Intent

Reduce direct communication between many objects by introducing a central mediator.

### Mental Model

Air traffic control tower.

Planes do not coordinate directly with every other plane. They communicate through tower.

### Java Example

```java
interface ChatMediator {
    void sendMessage(String message, User sender);
    void addUser(User user);
}

class ChatRoom implements ChatMediator {
    private final List<User> users = new ArrayList<>();

    public void addUser(User user) {
        users.add(user);
    }

    public void sendMessage(String message, User sender) {
        for (User user : users) {
            if (user != sender) {
                user.receive(message);
            }
        }
    }
}

class User {
    private final String name;
    private final ChatMediator mediator;

    User(String name, ChatMediator mediator) {
        this.name = name;
        this.mediator = mediator;
    }

    void send(String message) {
        mediator.sendMessage(message, this);
    }

    void receive(String message) {
        System.out.println(name + " received: " + message);
    }
}
```

### When To Use

- Many objects communicate with each other.
- Object graph becomes tangled.
- You want centralized coordination.

### When Not To Use

- Mediator becomes a god object.
- Communication is simple.

### Backend Mapping

- Service orchestrator
- Workflow coordinator
- Event bus
- Chat room

---

## 26. Memento Pattern

### Intent

Capture and restore an object's previous state without exposing internal details.

### Mental Model

Undo button.

### Java Example

```java
class BookingDraft {
    private String guestName;
    private String roomType;

    void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    Memento save() {
        return new Memento(guestName, roomType);
    }

    void restore(Memento memento) {
        this.guestName = memento.guestName;
        this.roomType = memento.roomType;
    }

    static class Memento {
        private final String guestName;
        private final String roomType;

        Memento(String guestName, String roomType) {
            this.guestName = guestName;
            this.roomType = roomType;
        }
    }
}
```

### When To Use

- Undo/redo.
- Checkpoint state.
- Restore previous state safely.

### When Not To Use

- State is huge.
- Copying state is expensive.
- Audit/event sourcing is a better model.

---

## 27. Pattern Comparisons Interviewers Love

### Factory vs Abstract Factory vs Builder

| Pattern | Focus |
|---|---|
| Factory Method | Create one object based on type |
| Abstract Factory | Create families of related objects |
| Builder | Step-by-step construction of complex object |

### Adapter vs Decorator vs Proxy vs Facade

| Pattern | Main Purpose |
|---|---|
| Adapter | Convert incompatible interface |
| Decorator | Add behavior dynamically |
| Proxy | Control access to real object |
| Facade | Simplify complex subsystem |

### Strategy vs State vs Command

| Pattern | Main Purpose |
|---|---|
| Strategy | Swap algorithm |
| State | Change behavior based on internal state |
| Command | Wrap request/action as object |

### Observer vs Chain Of Responsibility

| Observer | Chain Of Responsibility |
|---|---|
| One event notifies many observers | One request moves through handler chain |
| Publisher does not know subscribers deeply | Sender does not know which handler handles |
| Event-driven | Pipeline/filter-driven |

### Template Method vs Strategy

| Template Method | Strategy |
|---|---|
| Inheritance | Composition |
| Fixed algorithm skeleton | Interchangeable algorithm |
| Compile-time subclass behavior | Runtime behavior swapping |

---

## 28. Spring And Java Framework Mapping

| Framework Feature | Pattern |
|---|---|
| Spring beans default scope | Singleton-like container-managed lifecycle |
| `@Transactional` | Proxy |
| Spring AOP | Proxy |
| `@Cacheable` | Proxy |
| Spring Security filter chain | Chain of Responsibility |
| Servlet filters | Chain of Responsibility |
| `JdbcTemplate` | Template Method / Callback |
| `RestTemplate` | Template-style helper |
| Spring `ApplicationEventPublisher` | Observer |
| Dependency injection | Factory / IoC |
| BeanFactory / ApplicationContext | Factory |
| Strategy beans selected by type/name | Strategy |
| Controller -> service orchestration | Facade-like |
| Repository abstraction | Proxy / Adapter-like depending implementation |
| Java IO streams | Decorator |
| `Comparator` | Strategy |
| `Iterator` | Iterator |
| `Runnable` / job object | Command-like |

### Strong Spring Answer

```text
Spring uses many patterns internally. The most visible ones are Singleton for bean scope,
Proxy for AOP and transactions, Factory for bean creation, Template Method in JdbcTemplate,
Observer in application events, and Chain of Responsibility in filters/security chains.
```

---

## 29. Design Pattern Hot Interview Questions

### Q1. What is a design pattern?

```text
A design pattern is a reusable solution to a common software design problem. It is not a
library or code snippet, but a proven way to structure classes and responsibilities.
```

### Q2. What are the three main categories?

```text
Creational patterns handle object creation, structural patterns handle object composition,
and behavioral patterns handle object collaboration and responsibility.
```

### Q3. Which design patterns have you used?

Strong answer:

```text
In backend Java, I commonly use Strategy for interchangeable business rules, Factory for
creating implementations based on type, Builder for complex request/DTO construction,
Facade at the service layer to hide orchestration complexity, and Proxy indirectly through
Spring AOP features like @Transactional and @Cacheable.
```

### Q4. Explain Singleton and its issues.

```text
Singleton ensures only one instance, but it can introduce hidden global state and testing
difficulty. It must be thread-safe if used in concurrent systems. In Java, enum singleton
is a strong simple option, while Spring singleton is container-managed per application context.
```

### Q5. Factory vs Strategy?

```text
Factory is about object creation. Strategy is about interchangeable behavior. A factory may
create a strategy, but they solve different problems.
```

### Q6. Builder vs Factory?

```text
Builder is used to construct a complex object step by step, especially with many optional
fields. Factory chooses which implementation/object to create based on input or configuration.
```

### Q7. Proxy vs Decorator?

```text
Decorator adds behavior to an object while preserving the same interface. Proxy controls
access to the real object and may add security, lazy loading, caching, transactions, or remote
communication.
```

### Q8. Adapter vs Facade?

```text
Adapter makes an incompatible API fit the expected interface. Facade provides a simplified
interface over a complex subsystem.
```

### Q9. Strategy vs State?

```text
Strategy changes algorithm based on external selection. State changes behavior because the
object's internal state changes, and state objects may transition the context.
```

### Q10. Template Method vs Strategy?

```text
Template Method uses inheritance to fix algorithm skeleton and let subclasses customize steps.
Strategy uses composition to swap complete algorithms at runtime.
```

### Q11. Which pattern removes if-else?

```text
Strategy often removes algorithm-based if-else. Factory can remove creation-based if-else
from client code. State can remove state-based if-else.
```

### Q12. Which pattern is used in Spring transactions?

```text
Proxy pattern. Spring creates a proxy around the bean and applies transaction behavior before
and after method invocation.
```

### Q13. Which pattern is used in Java IO?

```text
Decorator pattern. For example, BufferedInputStream wraps FileInputStream to add buffering.
```

### Q14. Which pattern is used by Comparator?

```text
Strategy pattern. Different Comparator implementations represent different sorting strategies.
```

### Q15. Which pattern is used by Iterator?

```text
Iterator pattern. It provides sequential access without exposing collection internals.
```

---

## 30. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Forcing pattern everywhere | Adds unnecessary complexity | Use only when it solves real design pain |
| Calling every service a facade | Facade should simplify a subsystem intentionally | Explain what complexity it hides |
| Confusing Factory and Strategy | Creation vs behavior | Say factory may create strategies |
| Making Singleton mutable | Shared mutable state causes concurrency bugs | Keep stateless or thread-safe |
| Overusing inheritance for Template Method | Rigid hierarchy | Prefer Strategy when runtime flexibility matters |
| Huge Chain of Responsibility | Hard to debug | Keep handlers small and observable |
| God Facade | Too many responsibilities | Split use-case services |
| Too many decorators | Debugging wrapper stack is hard | Use only for composable add-ons |
| Ignoring Spring proxy self-invocation | AOP may not apply internally | Call through proxy or refactor service boundaries |
| Using Builder for tiny objects | Boilerplate without benefit | Use constructor/static factory |

---

## 31. One-Hour Design Pattern Revision Plan

### First 15 Minutes: Must-Know Creational

Revise:
- Singleton
- Factory Method
- Builder

Be able to code:
- Thread-safe singleton
- PaymentProcessorFactory
- Booking builder

### Next 15 Minutes: Must-Know Structural

Revise:
- Adapter
- Decorator
- Proxy
- Facade

Be able to explain:
- Java IO as Decorator
- Spring `@Transactional` as Proxy
- Service orchestration as Facade

### Next 15 Minutes: Must-Know Behavioral

Revise:
- Strategy
- Observer
- Template Method
- Chain of Responsibility
- Command

Be able to code:
- Discount strategy
- Validation chain

### Last 15 Minutes: Comparisons

Revise:
- Factory vs Builder
- Proxy vs Decorator
- Adapter vs Facade
- Strategy vs State
- Template vs Strategy
- Observer vs Chain

---

## 32. Final Cheat Sheet

| Need | Pattern |
|---|---|
| One instance | Singleton |
| Create object by type | Factory |
| Create related object families | Abstract Factory |
| Build complex object | Builder |
| Copy existing object | Prototype |
| Convert interface | Adapter |
| Add behavior dynamically | Decorator |
| Control access | Proxy |
| Simplify subsystem | Facade |
| Tree structure | Composite |
| Separate abstraction and implementation | Bridge |
| Share common state | Flyweight |
| Swap algorithm | Strategy |
| Notify subscribers | Observer |
| Fixed algorithm skeleton | Template Method |
| Request pipeline | Chain of Responsibility |
| Action as object | Command |
| Behavior changes by state | State |
| Traverse collection | Iterator |
| Central coordination | Mediator |
| Undo/restore | Memento |

---

## 33. Strong Closing Answer

If interviewer asks:

```text
How do you use design patterns in real projects?
```

Say:

```text
I use design patterns as practical tools, not as forced abstractions. In Java backend work,
I most often use Strategy to separate business rules, Factory to centralize implementation
creation, Builder for complex immutable objects, Facade for service orchestration, and Chain
of Responsibility for validation/filter pipelines. In Spring, I also rely heavily on patterns
provided by the framework, especially Proxy for transactions/AOP, Singleton for bean lifecycle,
Template Method in JdbcTemplate-style APIs, and Observer for application events.
```

This is a strong, realistic answer for a Java backend interview.

