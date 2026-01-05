# Arquitectura del Sistema Reactivo

## Índice

1. [Visión General](#visión-general)
2. [Arquitectura de Alto Nivel](#arquitectura-de-alto-nivel)
3. [Capas de la Aplicación](#capas-de-la-aplicación)
4. [Patrones de Diseño](#patrones-de-diseño)
5. [Flujos de Comunicación](#flujos-de-comunicación)
6. [Modelo de Actores](#modelo-de-actores)
7. [Gestión de Estado](#gestión-de-estado)
8. [Manejo de Errores](#manejo-de-errores)
9. [Escalabilidad](#escalabilidad)
10. [Decisiones de Diseño](#decisiones-de-diseño)

## Visión General

El sistema está diseñado siguiendo los principios del **Manifiesto Reactivo** y utiliza una arquitectura en capas basada en el patrón **Hexagonal (Ports & Adapters)**. Esta arquitectura garantiza:

- **Separación de responsabilidades** clara entre capas
- **Testabilidad** mediante inyección de dependencias
- **Independencia** del framework en la lógica de dominio
- **Flexibilidad** para cambiar implementaciones sin afectar el core

## Arquitectura de Alto Nivel

```
┌──────────────────────────────────────────────────────────────────┐
│                         HTTP LAYER                                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Play Framework Routes                          │ │
│  │  GET  /contact          → ContactController.form           │ │
│  │  POST /contact          → ContactController.submit         │ │
│  │  GET  /contact/health   → ContactController.health         │ │
│  │  GET  /contact/stats    → ContactController.stats          │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                     PRESENTATION LAYER                            │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  ContactController                                          │ │
│  │  - Form binding & validation                                │ │
│  │  - HTTP response handling                                   │ │
│  │  - Error presentation                                       │ │
│  │  - Monitoring endpoints                                     │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                      SERVICE LAYER                                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  ReactiveContactAdapter                                     │ │
│  │  - HTTP → Actor translation                                 │ │
│  │  - Input sanitization                                       │ │
│  │  - Backpressure management                                  │ │
│  │  - Timeout handling                                         │ │
│  │  - Circuit breaker logic                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬───────────────────────────────────┘
                               │ Ask Pattern (Request/Response)
┌──────────────────────────────▼───────────────────────────────────┐
│                       DOMAIN LAYER (CORE)                         │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  ContactEngine (Akka Typed Actor)                           │ │
│  │  - Business logic                                           │ │
│  │  - Domain validation                                        │ │
│  │  - State management                                         │ │
│  │  - Event emission                                           │ │
│  │  - Supervision strategy                                     │ │
│  └────────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  ContactProtocol                                            │ │
│  │  - Command messages (SubmitContact, GetContactStats)        │ │
│  │  - Response messages (ContactAccepted, ContactRejected)     │ │
│  │  - Event messages (ContactSubmitted, ContactValidated)      │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

## Capas de la Aplicación

### 1. Capa de Presentación (Presentation Layer)

**Responsabilidades:**
- Manejar requests HTTP
- Validar datos de entrada usando Play Forms
- Renderizar vistas Twirl
- Transformar respuestas del dominio en HTTP responses
- Proveer endpoints de monitoreo

**Componentes:**
- `ContactController` - Controlador principal
- Templates Twirl (`contactForm.scala.html`, `contactResult.scala.html`)

**Principios aplicados:**
- **Thin Controller**: Lógica mínima, delega al servicio
- **Async/Non-Blocking**: Todos los métodos retornan `Future`
- **Validation at the Edge**: Primera línea de defensa

**Ejemplo:**
```scala
def submit: Action[AnyContent] =
  Action.async { implicit request =>
    contactForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(...)),
      validData => adapter.submit(...).map { ... }
    )
  }
```

### 2. Capa de Servicios (Service Layer)

**Responsabilidades:**
- Adaptar entre el mundo HTTP y el mundo de actores
- Sanitizar inputs para seguridad
- Gestionar timeouts y backpressure
- Implementar circuit breakers
- Transformar mensajes entre protocolos

**Componentes:**
- `ReactiveContactAdapter` - Adaptador principal

**Principios aplicados:**
- **Adapter Pattern**: Traduce entre protocolos
- **Resilience**: Manejo de timeouts y errores
- **Security**: Sanitización de inputs
- **Ask Pattern**: Request-response sobre actores

**Ejemplo:**
```scala
def submit(...): Future[Either[String, Unit]] = {
  system.ask[ContactResponse](replyTo =>
    SubmitContact(sanitize(name), sanitize(email), sanitize(message), replyTo)
  ).map {
    case ContactAccepted => Right(())
    case ContactRejected(reason) => Left(reason)
  }.recover {
    case _: AskTimeoutException => Left("Alta carga")
  }
}
```

### 3. Capa de Dominio (Domain Layer / Core)

**Responsabilidades:**
- Lógica de negocio pura
- Validación de reglas de dominio
- Gestión de estado inmutable
- Emisión de eventos
- Recuperación ante fallos

**Componentes:**
- `ContactEngine` - Actor con lógica de negocio
- `ContactProtocol` - Definición de mensajes

**Principios aplicados:**
- **Pure Domain Logic**: Sin dependencias de frameworks
- **Immutability**: Todo el estado es inmutable
- **Message-Driven**: Comunicación mediante mensajes tipados
- **Supervision**: Estrategias de recuperación ante fallos

**Ejemplo:**
```scala
def stateful(state: State): Behavior[ContactCommand] =
  Behaviors.receive { (context, command) =>
    command match {
      case SubmitContact(name, email, message, replyTo) =>
        validateContact(name, email, message) match {
          case Left(error) =>
            replyTo ! ContactRejected(error)
            stateful(state.copy(rejected = state.rejected + 1))
          case Right(_) =>
            replyTo ! ContactAccepted
            stateful(state.copy(accepted = state.accepted + 1))
        }
    }
  }
```

## Patrones de Diseño

### 1. Hexagonal Architecture (Ports & Adapters)

**Objetivo**: Aislar la lógica de negocio de los detalles de infraestructura.

**Implementación**:
- **Core (Hexágono)**: `ContactEngine` y `ContactProtocol`
- **Ports (Puertos)**: Interfaces definidas por mensajes Akka
- **Adapters (Adaptadores)**: `ReactiveContactAdapter`, `ContactController`

**Beneficios**:
- Testabilidad: El core se puede testear sin HTTP ni actores
- Flexibilidad: Podemos cambiar Play por otro framework
- Claridad: Separación clara de responsabilidades

### 2. Actor Model (Akka Typed)

**Objetivo**: Manejar concurrencia mediante actores aislados que se comunican con mensajes.

**Implementación**:
```scala
ActorSystem(ContactEngine.supervised(), "reactive-contact-system")
```

**Características**:
- **Type Safety**: Mensajes tipados en compile-time
- **Isolation**: Estado encapsulado por actor
- **Location Transparency**: Puede escalar a múltiples nodos
- **Supervision**: Jerarquía de supervisión para resiliencia

### 3. CQRS Light

**Objetivo**: Separar comandos (escritura) de queries (lectura).

**Implementación**:
- **Commands**: `SubmitContact` - Modifica estado
- **Queries**: `GetContactStats` - Solo lectura

**Beneficios**:
- Escalabilidad independiente
- Optimizaciones específicas
- Auditoría y logging diferenciados

### 4. Event Sourcing (Preparado)

**Objetivo**: Mantener historial de eventos para auditoría y debugging.

**Implementación**:
```scala
sealed trait ContactEvent
case class ContactSubmitted(name: String, email: String, message: String, timestamp: Instant)
case class ContactValidated(timestamp: Instant)
case class ContactRejectedEvent(reason: String, timestamp: Instant)
```

**Estado actual**: Infraestructura preparada, no persistido
**Futuro**: Integración con Akka Persistence para persistir eventos

### 5. Circuit Breaker

**Objetivo**: Prevenir cascadas de fallos en servicios downstream.

**Implementación**:
- Timeouts estrictos (5 segundos)
- Recovery automático con mensajes amigables
- Supervisión de actores con reinicio limitado

## Flujos de Comunicación

### Flujo de Envío de Contacto (Happy Path)

```
1. Usuario → HTTP POST /contact
         ↓
2. Play Framework recibe request
         ↓
3. ContactController.submit
         ↓
4. Play Forms valida datos
         ↓ [válido]
5. ReactiveContactAdapter.submit
         ↓
6. Sanitización de inputs
         ↓
7. Ask Pattern → ContactEngine
         ↓
8. ContactEngine: validateContact
         ↓ [válido]
9. Actualiza estado (accepted++)
         ↓
10. ContactEngine ! ContactAccepted
         ↓
11. ReactiveContactAdapter recibe respuesta
         ↓
12. Transforma a Either[String, Unit]
         ↓
13. ContactController renderiza vista éxito
         ↓
14. Usuario recibe HTML de confirmación
```

### Flujo de Error (Validación Falla)

```
1. Usuario → HTTP POST /contact (datos inválidos)
         ↓
2. Play Framework recibe request
         ↓
3. ContactController.submit
         ↓
4. Play Forms detecta error
         ↓ [inválido]
5. Retorna BadRequest con formulario + errores
         ↓
6. Usuario ve errores de validación
```

### Flujo de Timeout

```
1. Usuario → HTTP POST /contact
         ↓
2-7. [pasos normales]
         ↓
8. ContactEngine sobrecargado (no responde)
         ↓
9. Ask Pattern timeout (5 segundos)
         ↓
10. ReactiveContactAdapter.recover
         ↓
11. Retorna Left("Sistema con alta carga")
         ↓
12. ContactController renderiza error amigable
         ↓
13. Usuario ve mensaje de reintentar
```

## Modelo de Actores

### Jerarquía de Supervisión

```
ActorSystem (Guardian)
    │
    └─── ContactEngine (Supervised)
            │
            ├─── (Future: EmailSender Actor)
            ├─── (Future: DatabaseWriter Actor)
            └─── (Future: NotificationActor)
```

### Estrategia de Supervisión

```scala
def supervised(): Behavior[ContactCommand] =
  Behaviors.supervise(apply())
    .onFailure[Exception](
      SupervisorStrategy.restart
        .withLimit(maxNrOfRetries = 3, withinTimeRange = 1.minute)
    )
```

**Comportamiento**:
- Ante una excepción, el actor se reinicia
- Máximo 3 reintentos en 1 minuto
- Si se excede el límite, el error se propaga al supervisor
- El estado se pierde en el reinicio (por diseño - fail fast)

## Gestión de Estado

### Estado del Actor

```scala
private case class State(
  received: Long = 0,
  accepted: Long = 0,
  rejected: Long = 0
)
```

**Características**:
- **Inmutable**: Cada modificación crea nuevo estado
- **Privado**: Solo accesible por el actor
- **Thread-Safe**: Un solo thread procesa mensajes
- **Volatile**: Se pierde en reinicio (por diseño)

### Persistencia (Futura)

Para producción, se recomienda:

```scala
EventSourcedBehavior[ContactCommand, ContactEvent, State](
  persistenceId = PersistenceId.ofUniqueId("contact-engine"),
  emptyState = State(),
  commandHandler = commandHandler,
  eventHandler = eventHandler
)
```

## Manejo de Errores

### Niveles de Defensa

#### 1. Cliente (HTML5)
```html
<input required minlength="2" maxlength="100">
```

#### 2. Controlador (Play Forms)
```scala
"name" -> nonEmptyText(minLength = 2, maxLength = 100)
```

#### 3. Servicio (Sanitización)
```scala
private def sanitize(input: String): String = {
  input.trim
    .replaceAll("<", "&lt;")
    .take(10000)
}
```

#### 4. Dominio (Validación de Negocio)
```scala
if (name.trim.length < 2)
  Left("Nombre debe tener al menos 2 caracteres")
```

### Estrategia de Errores

1. **Fail Fast**: Detectar errores lo antes posible
2. **Explicit**: Usar `Either[Error, Success]` en lugar de excepciones
3. **Recovery**: Siempre proveer fallback o mensaje claro
4. **Logging**: Registrar errores para análisis posterior

## Escalabilidad

### Escalado Vertical

**Configuración de Akka**:
```hocon
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
}
```

### Escalado Horizontal

**Opción 1: Load Balancer Simple**
```
            ┌─── Instance 1 (Actor System 1)
Load Balancer ─── Instance 2 (Actor System 2)
            └─── Instance 3 (Actor System 3)
```

**Opción 2: Akka Cluster**
```
           ┌─── Node 1 (Seed)
Cluster ───┼─── Node 2
           └─── Node 3

Routing Strategy: Consistent Hashing
```

### Métricas para Auto-Scaling

El endpoint `/contact/stats` expone:
- `totalReceived`: Total de requests
- `totalAccepted`: Requests exitosos
- `totalRejected`: Requests fallidos

**Reglas de escalado recomendadas**:
- Si `rejected / received > 0.1` → Escalar
- Si promedio de latencia > 2s → Escalar
- Si CPU > 70% por 5 minutos → Escalar

## Decisiones de Diseño

### ¿Por qué Akka Typed en lugar de Akka Classic?

✅ **Type Safety**: Errores detectados en compile-time
✅ **Better Tooling**: IntelliJ autocomplete completo
✅ **Future Proof**: Akka Classic en mantenimiento
✅ **Cleaner APIs**: Behaviors más expresivos

### ¿Por qué no usar Future directamente?

❌ Falta de backpressure
❌ Difícil de testear con concurrencia
❌ No hay supervision automática
✅ Actors proveen todo esto + escalabilidad

### ¿Por qué separar en tantas capas?

✅ **Testabilidad**: Cada capa se testea independiente
✅ **Mantenibilidad**: Cambios localizados
✅ **Reusabilidad**: El core puede usarse en otros contextos
✅ **Claridad**: Responsabilidades explícitas

### ¿Por qué no persistir eventos aún?

- **YAGNI**: No hay requerimiento de auditoría aún
- **Simplicidad**: Menos complejidad operacional
- **Preparación**: La estructura está lista para agregarlo
- **Decisión reversible**: Se puede agregar sin refactorizar

## Diagramas de Secuencia

### Envío Exitoso

```
┌──────┐    ┌────────────┐    ┌─────────┐    ┌────────────┐
│Client│    │Controller  │    │ Adapter │    │  Engine    │
└──┬───┘    └─────┬──────┘    └────┬────┘    └─────┬──────┘
   │              │                 │                │
   │ POST /contact│                 │                │
   │─────────────>│                 │                │
   │              │                 │                │
   │              │ submit(...)     │                │
   │              │────────────────>│                │
   │              │                 │                │
   │              │                 │ SubmitContact  │
   │              │                 │───────────────>│
   │              │                 │                │
   │              │                 │   [validate]   │
   │              │                 │                │
   │              │                 │ContactAccepted │
   │              │                 │<───────────────│
   │              │                 │                │
   │              │  Right(())      │                │
   │              │<────────────────│                │
   │              │                 │                │
   │   200 OK     │                 │                │
   │<─────────────│                 │                │
   │              │                 │                │
```

## Referencias

- [Akka Documentation](https://doc.akka.io/docs/akka/current/)
- [Play Framework Best Practices](https://www.playframework.com/documentation/2.9.x/ScalaBestPractices)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Reactive Design Patterns](https://www.reactivedesignpatterns.com/)
