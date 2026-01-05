# Reactive Manifiesto - Sistema de Contacto Reactivo

[![Scala](https://img.shields.io/badge/Scala-2.13.12-red.svg)](https://www.scala-lang.org/)
[![Play Framework](https://img.shields.io/badge/Play-2.9-green.svg)](https://www.playframework.com/)
[![Akka](https://img.shields.io/badge/Akka-2.8.5-blue.svg)](https://akka.io/)
[![Reactive Manifesto](https://img.shields.io/badge/Reactive-Manifesto-orange.svg)](https://www.reactivemanifesto.org/)

## 📋 Descripción

Sistema de portafolio profesional con sistema de contacto que implementa de forma rigurosa los **cuatro principios fundamentales del Manifiesto Reactivo**:

1. **Responsivo (Responsive)** - Respuestas rápidas y consistentes
2. **Resiliente (Resilient)** - Tolerante a fallos con recuperación automática
3. **Elástico (Elastic)** - Capaz de escalar bajo carga variable
4. **Orientado a Mensajes (Message-Driven)** - Arquitectura basada en paso asíncrono de mensajes

## 🏗️ Arquitectura

La aplicación utiliza una arquitectura en capas que separa las responsabilidades siguiendo el patrón **Hexagonal/Ports & Adapters**:

```
┌─────────────────────────────────────────────────────────────┐
│                    Capa de Presentación                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Views (Twirl Templates)                             │   │
│  │  - contactForm.scala.html                            │   │
│  │  - contactResult.scala.html                          │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    Capa de Controladores                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ContactController                                   │   │
│  │  - Validación de formularios (Play Forms)            │   │
│  │  - Manejo asíncrono de requests                      │   │
│  │  - Endpoints de monitoreo (/health, /stats)         │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    Capa de Servicios                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ReactiveContactAdapter                              │   │
│  │  - Adaptador entre HTTP y Actores                    │   │
│  │  - Manejo de backpressure                            │   │
│  │  - Sanitización de inputs                            │   │
│  │  - Circuit breaker para resiliencia                  │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    Capa de Dominio (Core)                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ContactEngine (Actor Typed)                         │   │
│  │  - Lógica de negocio pura                            │   │
│  │  - Validación de dominio                             │   │
│  │  - Manejo de estado inmutable                        │   │
│  │  - Estrategia de supervisión                         │   │
│  │                                                       │   │
│  │  ContactProtocol                                     │   │
│  │  - Mensajes (Commands, Responses, Events)           │   │
│  │  - Contratos inmutables                              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de un Request de Contacto

```
Usuario → HTTP POST → ContactController
                           ↓
              [Validación Play Forms]
                           ↓
              ReactiveContactAdapter
                           ↓
        [Sanitización + Ask Pattern]
                           ↓
            ContactEngine (Actor)
                           ↓
          [Validación de Dominio]
                           ↓
         [Emit Event + State Update]
                           ↓
              ContactResponse
                           ↓
        ReactiveContactAdapter
                           ↓
           ContactController
                           ↓
              View Template
                           ↓
              Usuario (HTML)
```

## 🎯 Principios del Manifiesto Reactivo Implementados

### 1. Responsivo (Responsive)

**Objetivo**: El sistema responde de manera oportuna siempre que sea posible.

**Implementación**:
- ✅ Timeouts configurables (5 segundos en `ReactiveContactAdapter`)
- ✅ Validación inmediata en múltiples niveles (formulario, controlador, dominio)
- ✅ Respuestas HTTP rápidas sin bloqueos
- ✅ Feedback visual inmediato en la UI
- ✅ Manejo de errores con mensajes claros y útiles

**Código**:
```scala
// Timeout configurado para garantizar respuesta
private implicit val timeout: Timeout = 5.seconds

// Validación rápida antes de procesar
contactForm.bindFromRequest().fold(
  formWithErrors => Future.successful(BadRequest(...)),
  validData => processAsync(validData)
)
```

### 2. Resiliente (Resilient)

**Objetivo**: El sistema permanece responsivo ante fallos.

**Implementación**:
- ✅ Supervisión de actores con reinicio automático
- ✅ Manejo exhaustivo de errores en todos los niveles
- ✅ Recovery strategies con mensajes descriptivos
- ✅ Circuit breaker implícito mediante timeouts
- ✅ Estado aislado por actor (no hay estado compartido mutable)

**Código**:
```scala
// Supervisión con reinicio limitado
def supervised(): Behavior[ContactCommand] =
  Behaviors.supervise(apply())
    .onFailure[Exception](
      SupervisorStrategy.restart
        .withLimit(maxNrOfRetries = 3, withinTimeRange = 1.minute)
    )

// Recovery en el adapter
.recover {
  case _: AskTimeoutException =>
    Left("Sistema con alta carga. Intenta nuevamente.")
  case ex: Exception =>
    system.log.error("Error processing contact", ex)
    Left("Error interno del sistema.")
}
```

### 3. Elástico (Elastic)

**Objetivo**: El sistema permanece responsivo bajo cargas de trabajo variables.

**Implementación**:
- ✅ Arquitectura message-driven permite scaling horizontal
- ✅ Actores Typed para procesamiento distribuido
- ✅ Sin bloqueos ni estado compartido
- ✅ Endpoints de monitoreo para decisiones de escalado (`/contact/stats`)
- ✅ Backpressure natural del patrón ask

**Código**:
```scala
// Estadísticas para monitoreo y auto-scaling
case class ContactStatsResponse(
  totalReceived: Long,
  totalAccepted: Long,
  totalRejected: Long
)

// Endpoint de métricas
def stats: Action[AnyContent] =
  Action.async { implicit request =>
    adapter.getStats().map { stats =>
      Ok(Json.toJson(stats))
    }
  }
```

### 4. Orientado a Mensajes (Message-Driven)

**Objetivo**: Los componentes se comunican mediante paso de mensajes asíncrono.

**Implementación**:
- ✅ Akka Typed Actors como base de la comunicación
- ✅ Mensajes inmutables (sealed traits)
- ✅ Ask pattern para request-response
- ✅ Event sourcing preparado con `ContactEvent`
- ✅ Desacoplamiento total entre capas

**Código**:
```scala
// Protocolo de mensajes inmutables
sealed trait ContactCommand
case class SubmitContact(
  name: String,
  email: String,
  message: String,
  replyTo: ActorRef[ContactResponse]
) extends ContactCommand

// Comunicación asíncrona
system.ask[ContactResponse](replyTo =>
  SubmitContact(name, email, message, replyTo)
)
```

## 🚀 Tecnologías

### Backend
- **Scala 2.13.12** - Lenguaje funcional con tipado fuerte
- **Play Framework 2.9** - Framework web reactivo
- **Akka Typed 2.8.5** - Actores con tipado para concurrencia
- **SBT** - Sistema de build

### Frontend
- **Twirl Templates** - Motor de plantillas tipado de Play
- **HTML5 Semántico** - Estructura accesible
- **CSS3 Moderno** - Diseño responsive profesional
- **Bootstrap 5.3.2** - Framework CSS para componentes

### Patrones y Prácticas
- **Hexagonal Architecture** - Separación de capas
- **CQRS Light** - Comandos y eventos separados
- **Event Sourcing** - Preparado para auditoría
- **Supervision Trees** - Jerarquía de actores resilientes
- **Immutability** - Estado inmutable en todos los niveles

## 📦 Instalación

### Prerrequisitos

- Java JDK 11 o superior
- SBT 1.9.0 o superior
- Git

### Pasos

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/federicopfund/Reactive-Manifiesto.git
   cd Reactive-Manifiesto
   ```

2. **Compilar el proyecto**
   ```bash
   sbt compile
   ```

3. **Ejecutar en modo desarrollo**
   ```bash
   sbt run
   ```

4. **Acceder a la aplicación**
   ```
   http://localhost:9000
   ```

## 🎮 Uso

### Formulario de Contacto

1. Navega a `/contact`
2. Completa los campos:
   - **Nombre**: 2-100 caracteres
   - **Email**: Formato válido
   - **Asunto**: Opcional, máx 200 caracteres
   - **Mensaje**: 10-5000 caracteres
3. Envía el formulario
4. Recibe confirmación inmediata

### Endpoints de Monitoreo

#### Health Check
```bash
GET /contact/health
```
Respuesta:
```
OK
```

#### Estadísticas
```bash
GET /contact/stats
```
Respuesta:
```json
{
  "received": 150,
  "accepted": 142,
  "rejected": 8
}
```

## 🧪 Testing

### Ejecutar tests
```bash
sbt test
```

### Ejecutar tests con cobertura
```bash
sbt clean coverage test coverageReport
```

### Tests implementados
- ✅ Validación de formularios
- ✅ Flujos de actores
- ✅ Endpoints del controlador
- ✅ Lógica de dominio

## 📊 Métricas y Monitoreo

El sistema expone métricas clave para monitoreo:

- **Total de mensajes recibidos**
- **Total de mensajes aceptados**
- **Total de mensajes rechazados**
- **Estado de salud del sistema**

Estas métricas pueden integrarse con:
- Prometheus
- Grafana
- Datadog
- New Relic

## 🔒 Seguridad

### Medidas Implementadas

1. **Sanitización de Inputs**
   - Limpieza de HTML y scripts
   - Límites de tamaño
   - Escape de caracteres especiales

2. **Validación en Múltiples Capas**
   - Validación en el cliente (HTML5)
   - Validación en el controlador (Play Forms)
   - Validación en el dominio (ContactEngine)

3. **CSRF Protection**
   - Tokens CSRF en todos los formularios
   - Validación automática por Play Framework

4. **Rate Limiting**
   - Backpressure natural del sistema de actores
   - Timeouts para prevenir DoS

## 📈 Escalabilidad

### Estrategias de Escalado

1. **Escalado Vertical**
   - Aumentar memoria JVM
   - Ajustar pool de threads de Akka

2. **Escalado Horizontal**
   - Múltiples instancias de la aplicación
   - Load balancer (Nginx, HAProxy)
   - Akka Cluster para distribución

3. **Configuración Recomendada**
   ```hocon
   akka {
     actor {
       default-dispatcher {
         fork-join-executor {
           parallelism-min = 8
           parallelism-factor = 3.0
           parallelism-max = 64
         }
       }
     }
   }
   ```

## 🗂️ Estructura del Proyecto

```
Reactive-Manifiesto/
├── app/
│   ├── Module.scala                    # Configuración de DI
│   ├── controllers/
│   │   ├── ContactController.scala     # Controlador HTTP
│   │   └── ProfileController.scala     # Controlador de perfil
│   ├── core/
│   │   ├── ContactEngine.scala         # Actor principal
│   │   └── ContactProtocol.scala       # Mensajes y eventos
│   ├── service/
│   │   └── ReactiveContactAdapter.scala # Adaptador HTTP-Actor
│   └── views/
│       ├── contactForm.scala.html      # Vista del formulario
│       ├── contactResult.scala.html    # Vista de resultado
│       ├── main.scala.html             # Layout principal
│       └── ...
├── conf/
│   ├── application.conf                # Configuración de Play/Akka
│   ├── routes                          # Rutas HTTP
│   └── messages                        # Internacionalización
├── public/
│   ├── stylesheets/
│   │   └── main.css                    # Estilos principales
│   ├── javascripts/
│   └── images/
├── test/
│   └── controllers/
│       └── HomeControllerSpec.scala    # Tests
├── build.sbt                           # Configuración de build
├── README.md                           # Este archivo
├── ARCHITECTURE.md                     # Documentación de arquitectura
└── REACTIVE_PRINCIPLES.md              # Principios reactivos detallados
```

## 📚 Documentación Adicional

- [**ARCHITECTURE.md**](ARCHITECTURE.md) - Arquitectura detallada del sistema
- [**REACTIVE_PRINCIPLES.md**](REACTIVE_PRINCIPLES.md) - Principios reactivos aplicados
- [**API.md**](API.md) - Documentación de endpoints
- [**DEPLOYMENT.md**](DEPLOYMENT.md) - Guía de despliegue

## 🤝 Contribuir

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama de feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 👤 Autor

**Federico Pfund**
- GitHub: [@federicopfund](https://github.com/federicopfund)
- Email: [Contacto a través del formulario](/contact)

## 🙏 Agradecimientos

- [Reactive Manifesto](https://www.reactivemanifesto.org/) - Por establecer los principios
- [Akka Team](https://akka.io/) - Por el excelente toolkit reactivo
- [Lightbend](https://www.lightbend.com/) - Por Play Framework y el ecosistema Scala

## 📖 Referencias

- [The Reactive Manifesto](https://www.reactivemanifesto.org/)
- [Akka Documentation](https://doc.akka.io/docs/akka/current/)
- [Play Framework Documentation](https://www.playframework.com/documentation/2.9.x/Home)
- [Scala Documentation](https://docs.scala-lang.org/)
- [Reactive Design Patterns](https://www.reactivedesignpatterns.com/)

---

**Built with ❤️ following the Reactive Manifesto principles**
