# Java IO, NIO, Files, And Serialization FAANG Master Sheet

Target: Java backend interviews where file handling, network IO, resource safety, and serialization depth matter.

This sheet covers:
- Classic IO streams/readers/writers
- try-with-resources
- Files API
- NIO buffers and channels
- Selectors
- Blocking vs non-blocking IO
- Direct buffers
- Serialization internals and risks
- `transient`, `serialVersionUID`, `readResolve`
- Production-safe alternatives

---

## 1. Mental Model

Java IO has two big families:

```text
Classic IO
    -> Stream/Reader/Writer
    -> Simple blocking APIs

NIO
    -> Buffer/Channel/Selector
    -> Better for scalable/network-oriented IO patterns
```

Strong interview line:

```text
Classic IO is easy for simple blocking file or socket operations. NIO gives buffers,
channels, and selectors for more scalable or lower-level IO control.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| InputStream vs Reader | Very high | Bytes vs characters |
| try-with-resources | Very high | Resource safety |
| Buffered streams | High | Performance |
| Files API | High | Modern file operations |
| NIO Buffer | High | Core NIO concept |
| Channel | High | Data transfer |
| Selector | Medium-high | Non-blocking network awareness |
| Direct buffer | Medium-high | Off-heap memory |
| Serialization | High | Common interview and security topic |
| serialVersionUID | High | Versioning |
| transient/static behavior | High | Traps |
| readResolve | Medium-high | Singleton protection |

---

## 3. Bytes vs Characters

| API | Handles | Examples |
|---|---|---|
| InputStream / OutputStream | Bytes | images, PDFs, binary data |
| Reader / Writer | Characters | text, JSON, CSV, logs |

Example:

```java
try (InputStream in = new FileInputStream("image.png")) {
    byte[] bytes = in.readAllBytes();
}
```

Text example:

```java
try (Reader reader = new FileReader("data.txt")) {
    int ch;
    while ((ch = reader.read()) != -1) {
        System.out.print((char) ch);
    }
}
```

Production caution:

```text
Avoid FileReader/FileWriter when encoding matters. Prefer Files APIs or InputStreamReader
with explicit charset.
```

---

## 4. Charset Safety

Bad:

```java
String text = new String(bytes);
```

Why:

```text
Uses platform default charset, which can differ across machines.
```

Better:

```java
import java.nio.charset.StandardCharsets;

String text = new String(bytes, StandardCharsets.UTF_8);
byte[] output = text.getBytes(StandardCharsets.UTF_8);
```

Interview line:

```text
Always specify charset for portable text handling.
```

---

## 5. try-with-resources

Any resource implementing AutoCloseable can be used.

```java
try (BufferedReader reader = Files.newBufferedReader(Path.of("input.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}
```

Why:

- Ensures close on success.
- Ensures close on exception.
- Avoids finally boilerplate.
- Handles suppressed exceptions.

Strong answer:

```text
try-with-resources is the standard Java way to manage IO resources safely. It prevents
resource leaks even when exceptions occur.
```

---

## 6. Buffered Streams

Unbuffered IO may call OS/native operations frequently.

Buffered IO reduces calls by reading/writing chunks.

Example:

```java
try (
    InputStream in = new BufferedInputStream(new FileInputStream("in.bin"));
    OutputStream out = new BufferedOutputStream(new FileOutputStream("out.bin"))
) {
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
    }
}
```

Interview line:

```text
Buffered streams improve performance by reducing small repeated read/write calls.
```

---

## 7. Files API

Modern Java file handling uses `java.nio.file`.

Common operations:

```java
Path path = Path.of("data.txt");

String content = Files.readString(path);
Files.writeString(path, "hello");

List<String> lines = Files.readAllLines(path);
boolean exists = Files.exists(path);
long size = Files.size(path);
```

Large file caution:

```text
Do not use readAllBytes/readString/readAllLines for huge files. Stream lines or process chunks.
```

Streaming:

```java
try (Stream<String> lines = Files.lines(Path.of("large.log"))) {
    long errors = lines.filter(line -> line.contains("ERROR")).count();
}
```

---

## 8. NIO Buffer

A Buffer is a memory container with:

- capacity
- position
- limit
- mark

Important methods:

| Method | Meaning |
|---|---|
| put | Write into buffer |
| flip | Switch from write mode to read mode |
| get | Read from buffer |
| clear | Prepare for writing again |
| compact | Keep unread data, prepare for more writing |

Example:

```java
ByteBuffer buffer = ByteBuffer.allocate(1024);

buffer.put("hello".getBytes(StandardCharsets.UTF_8));
buffer.flip();

while (buffer.hasRemaining()) {
    System.out.print((char) buffer.get());
}
```

Trap:

```text
Forgetting flip() is the classic NIO beginner bug.
```

---

## 9. Channels

Channels represent connections to IO sources/sinks.

Examples:

- FileChannel
- SocketChannel
- ServerSocketChannel
- DatagramChannel

FileChannel example:

```java
try (FileChannel channel = FileChannel.open(Path.of("data.txt"), StandardOpenOption.READ)) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int read = channel.read(buffer);
    System.out.println(read);
}
```

Copy using transfer:

```java
try (
    FileChannel source = FileChannel.open(Path.of("in.bin"), StandardOpenOption.READ);
    FileChannel target = FileChannel.open(
        Path.of("out.bin"),
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
    )
) {
    source.transferTo(0, source.size(), target);
}
```

Interview line:

```text
Channels work with buffers and can support efficient data transfer patterns.
```

---

## 10. Blocking vs Non-Blocking IO

Blocking IO:

```text
Thread waits until data is available or operation completes.
```

Non-blocking IO:

```text
Operation returns immediately if no data is available; program checks readiness.
```

With selectors:

```text
One thread can monitor many channels for readiness events.
```

Use non-blocking IO when:

- Many connections.
- Event-driven network server.
- You need efficient connection multiplexing.

Use blocking IO when:

- Simpler code is enough.
- Number of connections is manageable.
- Virtual threads make blocking model scalable enough.

---

## 11. Selector

Selector monitors multiple non-blocking channels.

Conceptual flow:

1. Open selector.
2. Configure channel non-blocking.
3. Register channel with interest ops.
4. Call `select()`.
5. Process ready keys.

Skeleton:

```java
Selector selector = Selector.open();
ServerSocketChannel server = ServerSocketChannel.open();
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();
    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

    while (keys.hasNext()) {
        SelectionKey key = keys.next();
        keys.remove();

        if (key.isAcceptable()) {
            // accept connection
        } else if (key.isReadable()) {
            // read data
        }
    }
}
```

Interview line:

```text
Selectors allow one or few threads to manage many channels by reacting to readiness events.
```

---

## 12. Direct ByteBuffer

Heap buffer:

```java
ByteBuffer heap = ByteBuffer.allocate(1024);
```

Direct buffer:

```java
ByteBuffer direct = ByteBuffer.allocateDirect(1024);
```

Direct buffers use off-heap memory.

Benefits:

- Useful for native IO.
- Can reduce copying in some IO paths.

Costs:

- Allocation/deallocation can be more expensive.
- Can cause direct memory OOM.
- Harder to observe than normal heap.

Strong answer:

```text
Direct buffers can improve IO interaction with native code, but they use off-heap memory
and must be monitored separately from normal heap.
```

---

## 13. Memory-Mapped Files

Memory-mapped files map file content into memory.

Example:

```java
try (FileChannel channel = FileChannel.open(Path.of("data.bin"), StandardOpenOption.READ)) {
    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    byte first = buffer.get(0);
    System.out.println(first);
}
```

Use cases:

- Large file random access.
- High-performance file reading.
- Specialized storage/index systems.

Caution:

```text
Memory-mapped files are powerful but can complicate resource lifecycle and memory diagnosis.
```

---

## 14. Java Serialization

Java built-in serialization converts objects to bytes and back.

```java
class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private transient String password;
}
```

Serialize:

```java
try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("user.ser"))) {
    out.writeObject(user);
}
```

Deserialize:

```java
try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("user.ser"))) {
    User user = (User) in.readObject();
}
```

Interview line:

```text
Java serialization is convenient but risky and usually avoided for external APIs.
```

---

## 15. serialVersionUID

`serialVersionUID` is a version identifier for serialized classes.

Why:

```text
It helps verify that the class used during deserialization is compatible with the serialized form.
```

If missing:

```text
JVM computes one from class structure, and small code changes can break compatibility.
```

Strong answer:

```text
For Serializable classes, explicitly define serialVersionUID to control version compatibility.
```

---

## 16. Serialization Traps

| Feature | Behavior |
|---|---|
| `transient` field | Not serialized |
| `static` field | Not part of object serialization |
| Constructor | Serializable class constructor is not called normally during deserialization |
| Object graph | Referenced serializable objects are serialized too |
| Non-serializable field | Can fail unless transient |

Example:

```java
class Session implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private transient String token;
    private static String appName = "booking";
}
```

After deserialization:

```text
token -> null
appName -> current class static value, not serialized object state
```

---

## 17. readResolve And Singleton

Serialization can break Singleton by creating a new object.

Fix:

```java
class AppConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AppConfig INSTANCE = new AppConfig();

    private AppConfig() {
    }

    static AppConfig getInstance() {
        return INSTANCE;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
```

Best simple option:

```java
enum Config {
    INSTANCE
}
```

Interview line:

```text
Enum singleton is serialization-safe by design and simpler than manual readResolve.
```

---

## 18. Serialization Security

Deserialization of untrusted data is dangerous.

Risks:

- Remote code execution gadget chains
- Object injection
- Denial of service
- Data tampering

Safer alternatives:

- JSON
- Protocol Buffers
- Avro
- Thrift
- Custom DTO mapping

Strong answer:

```text
I avoid Java native serialization for untrusted or external data. For APIs and distributed
systems, I prefer explicit schemas or safer formats like JSON, Protobuf, or Avro.
```

---

## 19. Externalizable

Externalizable gives full control over serialization.

```java
class UserExternal implements Externalizable {
    private String id;

    public UserExternal() {
        // required public no-arg constructor
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        id = in.readUTF();
    }
}
```

Use rarely.

Trap:

```text
Externalizable requires a public no-arg constructor and puts correctness burden on you.
```

---

## 20. Mini Program: Safe File Copy

```java
import java.io.IOException;
import java.nio.file.*;

public class SafeCopy {
    public static void main(String[] args) throws IOException {
        Path source = Path.of("input.txt");
        Path target = Path.of("output.txt");

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
```

Chunked version:

```java
try (
    InputStream in = Files.newInputStream(Path.of("input.bin"));
    OutputStream out = Files.newOutputStream(Path.of("output.bin"))
) {
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
    }
}
```

---

## 21. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Not closing streams | Resource leak | try-with-resources |
| Using default charset | Environment-dependent | Specify UTF-8 |
| Loading huge file into memory | OOM risk | Stream/chunk |
| Forgetting `flip()` | Buffer read fails | flip after writing before reading |
| Assuming NIO is always faster | Complexity may not pay off | Choose based on workload |
| Ignoring direct memory | Off-heap OOM | Monitor direct buffers |
| Java serialization for external APIs | Security/versioning risk | JSON/Protobuf/Avro |
| Missing serialVersionUID | Compatibility surprises | Define explicitly |
| Storing secrets in serialized object | Leakage risk | transient/encryption/redaction |

---

## 22. FAANG-Level Question

Question:

> You need to process a 20 GB log file in Java and count error lines. What would you do?

Strong answer:

```text
I would not load the whole file into memory. I would stream it line by line using Files.lines
or a BufferedReader with explicit charset. I would measure whether single-threaded streaming
is enough. If not, I would split by byte ranges carefully on line boundaries or use a log
processing system. I would monitor memory, IO throughput, and avoid creating unnecessary
objects per line.
```

---

## 23. Rapid Revision

Must-say lines:

```text
Streams handle bytes; readers/writers handle characters.
```

```text
Always specify charset when converting bytes and text.
```

```text
try-with-resources prevents resource leaks.
```

```text
NIO uses buffers, channels, and selectors.
```

```text
Direct buffers use off-heap memory.
```

```text
Java native serialization is risky for untrusted data and external APIs.
```

---

## 24. Official Source Notes

Use official sources when refreshing:

- Java IO/NIO API docs: `https://docs.oracle.com/en/java/javase/`
- Java serialization specification: `https://docs.oracle.com/javase/`
- Java API docs: `https://docs.oracle.com/en/java/javase/`

Interview safety line:

```text
For production data exchange, I prefer explicit schemas and controlled formats over native
Java serialization.
```
