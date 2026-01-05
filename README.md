# Reactive-Manifiesto

Esta aplicación implementa los principios del Manifiesto Reactivo utilizando Play Framework, Scala y Akka.

## Nuevas Características Reactivas

### Controladores Reactivos Implementados

La aplicación ahora incluye dos nuevos controladores que demuestran patrones de programación reactiva:

1. **AsyncDataController** - Manejo asíncrono de datos
   - Endpoints para obtención de datos de forma no bloqueante
   - Composición de Futures para operaciones paralelas
   - Manejo reactivo de errores con fallbacks
   - Gestión de timeouts para resiliencia

2. **StreamController** - Streaming en tiempo real
   - Server-Sent Events (SSE) para datos en tiempo real
   - Implementación con Akka Streams
   - Backpressure para control de flujo
   - Múltiples tipos de streams (eventos, sensores, notificaciones)

### Endpoints Disponibles

#### Datos Asíncronos
- `GET /api/data` - Obtención asíncrona de datos con timeout
- `GET /api/data/combined` - Combinación paralela de múltiples fuentes
- `GET /api/data/with-error-handling` - Manejo de errores con fallback

#### Streaming en Tiempo Real
- `GET /api/stream/events` - Stream de eventos en tiempo real
- `GET /api/stream/sensors` - Stream de datos de sensores simulados
- `GET /api/stream/notifications` - Stream de notificaciones
- `GET /api/stream/backpressure` - Demostración de backpressure

#### Demo Interactivo
- `GET /reactive-demo` - Página de demostración interactiva de los endpoints reactivos

### Documentación

Para información detallada sobre los patrones reactivos implementados, casos de uso y ejemplos, consulta [REACTIVE_CONTROLLERS.md](REACTIVE_CONTROLLERS.md).

## Principios del Manifiesto Reactivo Implementados

✅ **Responsive** - Respuestas oportunas con timeouts
✅ **Resilient** - Manejo de errores con fallbacks
✅ **Elastic** - I/O no bloqueante para escalabilidad
✅ **Message Driven** - Uso de Akka Actors y Streams

## Pruebas

Ejecutar todas las pruebas:
```bash
sbt test
```

Ejecutar pruebas específicas:
```bash
sbt "testOnly controllers.AsyncDataControllerSpec"
sbt "testOnly controllers.StreamControllerSpec"
```

## Desarrollo

Para ejecutar la aplicación en modo desarrollo:
```bash
sbt run
```

La aplicación estará disponible en http://localhost:9000

Visita http://localhost:9000/reactive-demo para ver una demostración interactiva de los endpoints reactivos.