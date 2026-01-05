# Arquitectura de Controladores Reactivos

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                         Cliente / Browser                        │
└────────────┬────────────────────────────────┬───────────────────┘
             │                                │
             │ HTTP Request                   │ EventSource (SSE)
             ▼                                ▼
┌────────────────────────────┐   ┌──────────────────────────────┐
│   AsyncDataController      │   │    StreamController          │
│                            │   │                              │
│  - getData()               │   │  - streamEvents()            │
│  - getCombinedData()       │   │  - streamSensorData()        │
│  - getDataWithError...()   │   │  - streamNotifications()     │
└────────────┬───────────────┘   └──────────┬───────────────────┘
             │                                │
             │ Future[Result]                 │ Source[JsValue]
             │                                │
             ▼                                ▼
┌────────────────────────────┐   ┌──────────────────────────────┐
│   Akka Scheduler           │   │    Akka Streams              │
│                            │   │                              │
│  - after() for delays      │   │  - Source.tick()             │
│  - Non-blocking timeouts   │   │  - Throttle                  │
│  - Future composition      │   │  - Buffer + Backpressure     │
└────────────────────────────┘   └──────────────────────────────┘
```

## Flujo de Datos

### Async Data Pattern (AsyncDataController)

```
Cliente
  │
  │ GET /api/data
  ▼
ProfileController
  │
  │ Action.async
  ▼
Future Composition
  │
  ├─► after(delay) ──► Data Source 1
  │                      │
  ├─► after(delay) ──► Data Source 2
  │                      │
  └─► after(delay) ──► Data Source 3
                         │
                         ▼
                    Combine Results
                         │
                         ▼
                    JSON Response
```

### Stream Pattern (StreamController)

```
Cliente
  │
  │ GET /api/stream/events
  ▼
StreamController
  │
  │ Action (chunked)
  ▼
Source[JsValue]
  │
  ├─► tick(interval)
  │     │
  │     ▼
  │   map() transform
  │     │
  │     ▼
  │   throttle()
  │     │
  │     ▼
  │   buffer()
  │     │
  │     ▼
  └─► take(n)
        │
        ▼
  EventSource.flow
        │
        ▼
  Server-Sent Events
        │
        ▼
     Cliente
```

## Componentes Principales

### 1. Play Framework Controllers

**AsyncDataController**
- Maneja requests HTTP asíncronos
- Usa `Action.async` para non-blocking I/O
- Retorna `Future[Result]`

**StreamController**
- Maneja streams de datos continuos
- Usa `Action` con chunked response
- Retorna `Source[JsValue]` convertido a SSE

### 2. Akka Components

**Akka Scheduler**
- Programa delays sin bloquear threads
- Usado por `akka.pattern.after`
- Gestión de timeouts

**Akka Streams**
- Source: Origen de datos reactivos
- Flow: Transformaciones intermedias
- Sink: Consumidor final (implícito en SSE)

### 3. Reactive Patterns

**Future Composition**
```scala
for {
  users <- futureUsers
  posts <- futurePosts
  comments <- futureComments
} yield Result
```

**Stream Pipeline**
```scala
Source
  .tick(interval)
  .map(transform)
  .throttle(rate)
  .buffer(size)
  .take(n)
  via EventSource.flow
```

**Error Recovery**
```scala
futureData.recover {
  case ex: Exception =>
    FallbackResponse
}
```

**Timeout Management**
```scala
Future.firstCompletedOf(Seq(
  dataFuture,
  timeoutFuture
))
```

## Características de Escalabilidad

### Thread Pool Management

```
Request Thread Pool (non-blocking)
  │
  ├─► AsyncDataController ─────► Future computation
  │                                 │
  │                                 ▼
  │                            Scheduler Thread
  │
  └─► StreamController ────────► Stream processing
                                    │
                                    ▼
                               Stream Materializer
```

### Backpressure Flow

```
Fast Producer (100ms)
        │
        ▼
   buffer(10)
        │
        │ Backpressure signal
        ▼
   throttle(1/sec)
        │
        ▼
Slow Consumer (1000ms)
```

## Integración con Código Existente

```
Existing Controllers
├── ContactController (Already reactive)
│   ├── Uses Akka Typed Actors
│   ├── Message-driven architecture
│   └── Async actions
│
├── ProfileController (Synchronous)
│   ├── Simple page rendering
│   └── No I/O operations
│
└── New Reactive Controllers
    ├── AsyncDataController
    │   ├── Async data fetching
    │   ├── Parallel composition
    │   └── Error handling
    │
    └── StreamController
        ├── Real-time streams
        ├── SSE protocol
        └── Backpressure management
```

## Dependency Graph

```
build.sbt
  │
  ├─► play-guice
  │     └─► Dependency Injection
  │
  ├─► akka-actor-typed
  │     └─► Actor System (Contact)
  │
  └─► akka-stream
        ├─► Source/Flow/Sink
        ├─► Backpressure
        └─► EventSource support
```

## Request/Response Lifecycle

### Async Endpoint Lifecycle

1. Client sends GET /api/data
2. Router matches to AsyncDataController.getData
3. Action.async starts execution (non-blocking)
4. Future chain begins with scheduler
5. Multiple operations run in parallel (if composition)
6. Timeout race condition (firstCompletedOf)
7. Result materialized when Future completes
8. JSON response sent to client

### Stream Endpoint Lifecycle

1. Client opens EventSource connection to /api/stream/events
2. Router matches to StreamController.streamEvents
3. Action starts with chunked transfer encoding
4. Source is materialized on demand
5. Events flow through stream pipeline
6. Each event transformed to SSE format
7. Events pushed to client as they're generated
8. Stream completes after n events or client disconnect

## Deployment Considerations

### Resource Configuration

```
akka {
  actor {
    default-dispatcher {
      type = Dispatcher
      executor = "fork-join-executor"
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 3.0
        parallelism-max = 64
      }
    }
  }
  
  stream {
    materializer {
      max-input-buffer-size = 16
      initial-input-buffer-size = 4
    }
  }
}
```

### Monitoring Points

- Request rate per endpoint
- Future completion time
- Stream connection count
- Backpressure events
- Error rate
- Timeout frequency

## Security Considerations

✅ No Thread.sleep (prevents thread exhaustion attacks)
✅ Timeout management (prevents hanging requests)
✅ Buffer limits (prevents memory exhaustion)
✅ Take limits on streams (prevents infinite streams)
✅ Input validation (existing in forms)

## Performance Characteristics

| Pattern | Latency | Throughput | Resource Usage |
|---------|---------|------------|----------------|
| Async Data | Low | High | Low (non-blocking) |
| Stream SSE | Real-time | Medium | Medium (per connection) |
| Sync (old) | High | Low | High (blocking) |

## References

- [Play Framework Async](https://www.playframework.com/documentation/2.8.x/ScalaAsync)
- [Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html)
- [Reactive Manifesto](https://www.reactivemanifesto.org/)
- [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
