# GraphQL Schema Query Decision Map

## Schema Design

| Need | Pattern |
|---|---|
| object identity | `id: ID!` and stable entity identity |
| list | connection with `first/after` or bounded pagination |
| write action | mutation input and payload types |
| shared contract | interface or union where appropriate |
| future removal | deprecate with reason |
| client safety | intentional nullability |

## Operation Design

| Need | Pattern |
|---|---|
| reusable input | variables |
| repeated field selection | fragments |
| telemetry | operation name/hash |
| abuse prevention | persisted/safelisted operations |
| cache identity | `id` and `__typename` where client needs normalization |

## Resolver Design

```text
field path -> parent/args/context/info -> auth -> data source -> batching/cache -> trace/error
```