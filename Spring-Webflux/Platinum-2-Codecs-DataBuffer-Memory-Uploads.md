# Platinum 2 - WebFlux Codecs, DataBuffer, Memory, Uploads, Downloads

> Goal: understand how WebFlux reads and writes bytes safely. This is the production layer
> behind JSON bodies, streaming responses, file upload/download, memory limits, and leak
> prevention.

---

## 0. Mental Model First

Most WebFlux notes focus on `Mono` and `Flux`.

Production WebFlux also needs byte-level awareness:

```text
HTTP bytes -> DataBuffer -> codec -> object
object -> codec -> DataBuffer -> HTTP bytes
```

If you understand only operators but not codecs and buffers, you can accidentally create:

- memory spikes
- file upload failures
- DataBuffer leaks
- oversized request body incidents
- slow streaming downloads
- broken content negotiation

### One-Line Rule

```text
Codecs convert bytes to objects; DataBuffers carry bytes; memory limits protect the server.
```

---

## 1. What Are Codecs?

Codecs encode and decode HTTP bodies.

| Direction | Meaning |
|---|---|
| decode | request bytes -> Java object |
| encode | Java object -> response bytes |

Common codecs:

- JSON with Jackson
- String/text
- byte arrays
- resources/files
- form data
- multipart data
- server-sent events

### Request Flow

```text
Client sends JSON bytes
-> Netty receives bytes
-> WebFlux exposes DataBuffers
-> JSON decoder creates BookingRequest
-> controller receives Mono<BookingRequest> or BookingRequest
```

### Code Sample

```java
@PostMapping("/api/bookings")
Mono<BookingResponse> create(@RequestBody Mono<BookingRequest> requestMono) {
    return requestMono.flatMap(bookingService::create);
}
```

The controller does not parse JSON manually. WebFlux codecs do it.

---

## 2. Content Negotiation

Content negotiation decides what representation is accepted and returned.

Inputs:

- `Content-Type`: what the client is sending
- `Accept`: what the client wants back

Example:

```java
@PostMapping(
    value = "/api/bookings",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
Mono<BookingResponse> create(@RequestBody Mono<BookingRequest> requestMono) {
    return requestMono.flatMap(bookingService::create);
}
```

### Interview Trap

Ignoring `Content-Type` and then debugging mysterious 415 errors.

```text
415 Unsupported Media Type usually means the request Content-Type does not match a decoder.
406 Not Acceptable usually means the server cannot produce what the client requested.
```

---

## 3. Configuring Codec Memory Limits

WebFlux has memory limits for buffering request/response bodies.

Why:

- prevent huge request bodies from consuming heap
- protect codecs that need to aggregate bytes
- fail fast for accidental large payloads

Spring Boot style:

```properties
spring.codec.max-in-memory-size=2MB
```

Programmatic style:

```java
@Bean
WebFluxConfigurer codecCustomizer() {
    return new WebFluxConfigurer() {
        @Override
        public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
            configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024);
        }
    };
}
```

### Senior Answer

```text
For small JSON APIs, bounded in-memory aggregation is fine. For large uploads/downloads, I
avoid aggregating the full body and stream DataBuffers or resources instead.
```

---

## 4. DataBuffer

`DataBuffer` is Spring's abstraction over byte buffers.

It appears when you work closer to the byte stream:

- file upload/download
- streaming raw bytes
- proxying response bodies
- custom codecs
- low-level WebClient handling

### Important Rule

If you manually handle pooled buffers, you must understand release ownership.

Wrong manual handling can leak memory outside normal heap visibility.

---

## 5. DataBuffer Leak Pattern

Common leak:

```java
Flux<DataBuffer> body = request.getBody();

body.subscribe(buffer -> {
    // read something
    // forget to release if manually retained/consumed outside normal pipeline
});
```

Better:

- avoid manual subscribe
- use framework decoders when possible
- use `DataBufferUtils`
- release buffers when manual low-level code takes ownership

Example:

```java
Mono<Void> save(ServerRequest request, Path path) {
    return DataBufferUtils.write(request.exchange().getRequest().getBody(), path)
        .then();
}
```

### Interview Punchline

```text
In WebFlux, memory bugs are not only heap collections. Pooled network buffers can leak when
manual DataBuffer ownership is wrong.
```

---

## 6. File Upload With Multipart

### Simple File Part

```java
@PostMapping(value = "/api/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
Mono<Void> upload(@RequestPart("file") Mono<FilePart> filePartMono) {
    return filePartMono.flatMap(filePart ->
        filePart.transferTo(Path.of("/tmp/uploads/" + filePart.filename()))
    );
}
```

### Why This Matters

`FilePart.transferTo(...)` can avoid loading the entire file into memory.

### Production Controls

- max file size
- allowed content types
- virus scanning path
- filename sanitization
- storage path isolation
- timeout
- user/tenant ownership

### Anti-Pattern

```java
filePart.content().collectList()
```

This can aggregate the whole file in memory.

---

## 7. Streaming File Download

### Resource Response

```java
@GetMapping("/api/files/{id}")
Mono<ResponseEntity<Resource>> download(@PathVariable String id) {
    return fileService.findResource(id)
        .map(resource -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.bin\"")
            .body(resource));
}
```

### Raw DataBuffer Streaming

```java
@GetMapping(value = "/api/files/{id}/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
Flux<DataBuffer> stream(@PathVariable String id) {
    Path path = fileService.pathFor(id);
    return DataBufferUtils.read(path, new DefaultDataBufferFactory(), 64 * 1024);
}
```

### Strong Answer

```text
For large downloads, I stream rather than load the whole file into memory. I also set content
type, content disposition, authorization checks, and range/caching behavior if needed.
```

---

## 8. Streaming JSON vs JSON Array

### JSON Array

```text
[{"id":1},{"id":2},{"id":3}]
```

Often requires collecting enough data to write a valid array boundary.

### NDJSON / Streaming JSON

```text
{"id":1}
{"id":2}
{"id":3}
```

Better for long streams.

Example:

```java
@GetMapping(value = "/api/events", produces = MediaType.APPLICATION_NDJSON_VALUE)
Flux<EventDto> events() {
    return eventService.stream();
}
```

### SSE

```java
@GetMapping(value = "/api/events/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ServerSentEvent<EventDto>> sse() {
    return eventService.stream()
        .map(event -> ServerSentEvent.builder(event).event("booking-update").build());
}
```

---

## 9. Multipart vs Streaming Body

| Need | Better Choice |
|---|---|
| upload file plus metadata | multipart |
| upload raw binary stream | `Flux<DataBuffer>` |
| upload JSON command | normal JSON codec |
| stream event updates | SSE or NDJSON |
| full-duplex messages | WebSocket or RSocket |

### Interview Trap

Using multipart for everything. Multipart is useful, but raw streaming or JSON may be
simpler depending on the API.

---

## 10. WebClient Body Memory Traps

### Risky For Large Responses

```java
Mono<String> body = webClient.get()
    .uri("/large-report")
    .retrieve()
    .bodyToMono(String.class);
```

This may aggregate a large response into memory.

### Streaming Alternative

```java
Flux<DataBuffer> data = webClient.get()
    .uri("/large-report")
    .retrieve()
    .bodyToFlux(DataBuffer.class);
```

Then stream to file:

```java
Mono<Void> downloadToFile(Path target) {
    Flux<DataBuffer> data = webClient.get()
        .uri("/large-report")
        .retrieve()
        .bodyToFlux(DataBuffer.class);

    return DataBufferUtils.write(data, target).then();
}
```

---

## 11. DataBufferUtils Cheat Sheet

| Utility | Use |
|---|---|
| `read(...)` | read file/resource as `Flux<DataBuffer>` |
| `write(...)` | write `Flux<DataBuffer>` to file/channel |
| `join(...)` | aggregate buffers; use carefully with limits |
| `release(...)` | release buffer when manually owning it |

### Rule

```text
Prefer framework-managed body decoding. Use DataBufferUtils when you intentionally work with
raw bytes.
```

---

## 12. Backpressure and Bytes

Backpressure matters for byte streams:

- slow client download
- slow disk write
- slow upstream service
- upload faster than processing

Good streaming design:

```text
Do not collect all chunks.
Let demand move through the pipeline.
Bound memory.
Cancel and cleanup on disconnect.
```

### `doFinally` For Cleanup

```java
return DataBufferUtils.read(path, bufferFactory, 64 * 1024)
    .doFinally(signal -> temporaryFileRegistry.cleanupIfNeeded(path, signal));
```

---

## 13. Large Request Protection

Controls:

- codec memory limit
- reverse proxy body size limit
- gateway body size limit
- application validation
- multipart max size
- timeout
- authentication before accepting expensive upload
- content-type allowlist

### Strong Answer

```text
I protect large body endpoints at multiple layers: gateway/proxy limits, WebFlux codec
limits, multipart/file constraints, timeout, and streaming storage instead of full memory
aggregation.
```

---

## 14. Production Story: Hotel Image Upload

Use case:

```text
Hotel admin uploads a 25 MB room image.
```

Bad design:

```text
Read entire file into byte[] -> validate -> upload to object storage.
```

Problems:

- heap spike
- GC pressure
- slow request ties resources
- no cleanup on cancel

Better design:

1. Authenticate admin.
2. Validate metadata and content type.
3. Stream `FilePart` to temporary storage or object storage adapter.
4. Scan/validate image asynchronously if needed.
5. Save metadata after storage success.
6. Cleanup temp file on cancel/error.
7. Return stable file ID.

---

## 15. Interview Hot Questions

### 1. What is a codec in WebFlux?

A codec converts HTTP body bytes to Java objects and Java objects back to bytes.

### 2. Why can file upload break a WebFlux app?

If the app aggregates large files in memory instead of streaming, it can cause memory spikes
or exceed codec limits.

### 3. What is DataBuffer?

It is Spring's byte-buffer abstraction used for low-level body streaming.

### 4. What is a DataBuffer leak?

It happens when manual low-level buffer handling takes ownership but does not release pooled
buffers correctly.

### 5. When should you use NDJSON or SSE instead of a JSON array?

When data is long-lived or progressive and the client should receive items as they arrive.

### 6. How do you protect large request bodies?

Use gateway/proxy limits, codec memory limits, multipart limits, streaming, validation,
timeouts, and cleanup on cancellation.

---

## 16. Final Revision Notes

```text
Codec = bytes <-> objects.
DataBuffer = byte chunks in WebFlux.
Do not collect large files into memory.
Use FilePart.transferTo or DataBufferUtils.write for uploads.
Use Resource or Flux<DataBuffer> for downloads.
NDJSON/SSE streams progressively.
bodyToMono(String.class) can aggregate large responses.
Memory limits protect codecs, not every possible stream bug.
Manual DataBuffer work needs ownership/release awareness.
Backpressure matters for bytes as much as objects.
```

---

## 17. Official Source Notes

- Spring WebFlux reference: https://docs.spring.io/spring-framework/reference/web/webflux.html
- Spring Framework Data Buffers and Codecs: https://docs.spring.io/spring-framework/reference/core/databuffer-codec.html
- Spring Boot WebFlux codecs: https://docs.spring.io/spring-boot/reference/web/reactive.html
- Spring WebClient reference: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
