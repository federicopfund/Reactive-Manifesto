# ⚡ Reactive Manifesto

Aplicación web que implementa los principios del [Manifiesto Reactivo](https://www.reactivemanifesto.org/) con **Play Framework**, **Akka Typed** y **Scala**.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| **Backend** | Play Framework 3.0.1 |
| **Lenguaje** | Scala 2.13.12 |
| **Sistema Reactivo** | Akka Typed 2.8.5 |
| **Persistencia** | Slick 3 + H2 (dev) / PostgreSQL (prod) |
| **Frontend** | Twirl templates, CSS3, Vanilla JS |
| **DI** | Guice |
| **Build** | SBT 1.9.7 |

---

## 🚀 Inicio Rápido

```bash
# Clonar
git clone https://github.com/federicopfund/Reactive-Manifiesto.git
cd Reactive-Manifiesto

# Ejecutar
sbt run
```

Disponible en **http://localhost:9000**

```bash
# Comando todo-en-uno (limpia, compila e inicia)
fuser -k 9000/tcp 2>/dev/null && sbt clean compile run
```

---

## 🏗️ Arquitectura de Agentes

El proyecto sigue una **arquitectura de agentes reactivos** con 4 capas desacopladas:

```mermaid
graph TB
    subgraph Clients["🌐 Clientes"]
        B1["Usuario autenticado"]
        B2["Visitante"]
    end

    subgraph Controllers["Controllers (Play Framework)"]
        HC["HomeController"]
        UPC["UserPublicationController"]
    end

    subgraph Adapters["Reactive Adapters (Ask Pattern)"]
        RCA["ReactiveContactAdapter"]
        RMA["ReactiveMessageAdapter"]
    end

    subgraph ActorSystem["Akka Typed Actor System"]
        CE["ContactEngine\n(contact-core)"]
        ME["MessageEngine\n(message-core)"]
    end

    subgraph Repositories["Repositories (Async / Slick)"]
        CR["ContactRepository"]
        PMR["PrivateMessageRepository"]
        UNR["UserNotificationRepository"]
    end

    subgraph DB["PostgreSQL"]
        DBIcon[("Base de Datos")]
    end

    subgraph DI["Module (Guice DI)"]
        MOD["Module.scala\nprovide ActorSystem\nprovide Adapters"]
    end

    B2 -- "POST /contact" --> HC
    B1 -- "POST /send-message" --> UPC

    HC -- "submitContact()" --> RCA
    UPC -- "sendMessage()" --> RMA

    RCA -- "ask (Future)" --> CE
    RMA -- "ask (Future)" --> ME

    CE -- "save()" --> CR
    ME -- "create()" --> PMR
    ME -- "create notification" --> UNR

    CR --> DBIcon
    PMR --> DBIcon
    UNR --> DBIcon

    MOD -. "provides" .-> CE
    MOD -. "provides" .-> ME
    MOD -. "provides" .-> RCA
    MOD -. "provides" .-> RMA

    style ActorSystem fill:#1a365d,stroke:#2b6cb0,color:#fff
    style Adapters fill:#2c5282,stroke:#3182ce,color:#fff
    style Controllers fill:#2d3748,stroke:#4a5568,color:#fff
    style Repositories fill:#1c4532,stroke:#276749,color:#fff
    style DB fill:#553c9a,stroke:#6b46c1,color:#fff
    style DI fill:#744210,stroke:#975a16,color:#fff
```

### Capas del sistema

#### 1. Agentes (Actors) — `core/`

| Actor | Sistema | Responsabilidad |
|-------|---------|-----------------|
| **ContactEngine** | `contact-core` | Procesa formularios de contacto: persiste en DB y responde |
| **MessageEngine** | `message-core` | Mensajería privada: persiste mensaje → crea notificación → responde |

Ambos usan **Akka Typed** con `Behaviors.receive` y el patrón `pipeToSelf` para manejar futuros asíncronos sin romper el modelo de actores.

#### 2. Adaptadores Reactivos — `services/`

| Adapter | Actor target |
|---------|-------------|
| **ReactiveContactAdapter** | ContactEngine |
| **ReactiveMessageAdapter** | MessageEngine |

Exponen una interfaz `Future`-based usando el **Ask Pattern** (`system.ask`) con timeout de 5 segundos, permitiendo a los controllers consumir respuestas de los actores como Futures estándar.

#### 3. Inyección de Dependencias — `Module.scala`

Guice provee los `ActorSystem[T]` como singletons, creando cada actor con sus repositorios inyectados.

#### 4. Persistencia — Repositories + Slick

Acceso a datos asíncrono (non-blocking) mediante el patrón Repository con Slick. Base de datos H2 en desarrollo, PostgreSQL en producción.

---

## 🔄 Flujo de Mensajes — Patrón Ask

```mermaid
sequenceDiagram
    participant U as Usuario
    participant C as Controller
    participant A as Adapter (Ask)
    participant E as Engine (Actor)
    participant R as Repository
    participant DB as PostgreSQL

    U->>C: HTTP Request
    C->>A: sendMessage() / submitContact()
    A->>E: ask(Command, replyTo)
    activate E
    E->>R: create() / save() (async)
    R->>DB: SQL INSERT
    DB-->>R: Result
    R-->>E: Future[Success/Failure]

    alt Success
        E->>E: pipeToSelf → Persisted
        E-->>A: Response(id) via replyTo
        E->>R: create notification (fire & forget)
        R->>DB: INSERT notification
    else Failure
        E->>E: pipeToSelf → Failed
        E-->>A: Error(reason) via replyTo
    end
    deactivate E

    A-->>C: Future[Response]
    C-->>U: HTTP Response (redirect/flash)
```

---

## ✅ Principios Reactivos Implementados

| Principio | Implementación |
|-----------|---------------|
| **Responsive** | Non-blocking I/O en todas las capas. Timeouts de 5s en Ask Pattern. Fast-fail con manejo de errores |
| **Resilient** | Errores capturados con `pipeToSelf(Failure)` sin crashear el actor. Connection pooling con recuperación automática |
| **Elastic** | Actor model permite escalado horizontal. Stateless controllers. Preparado para Akka Cluster |
| **Message-Driven** | Comunicación asíncrona vía mensajes tipados (`sealed trait`). Location transparency entre actores |

---

## 📁 Estructura del Proyecto

```
Reactive-Manifiesto/
├── app/
│   ├── Module.scala                   # DI: provee ActorSystems y Adapters
│   ├── controllers/
│   │   ├── HomeController.scala       # Contacto, páginas públicas
│   │   ├── AuthController.scala       # Login, registro, verificación email
│   │   ├── UserPublicationController  # Publicaciones, mensajería, dashboard
│   │   ├── AdminController.scala      # Panel de administración
│   │   └── actions/
│   │       └── AuthAction.scala       # Acción de autenticación
│   ├── core/
│   │   ├── ContactEngine.scala        # Actor: formulario de contacto
│   │   └── MessageEngine.scala        # Actor: mensajería privada + notificaciones
│   ├── services/
│   │   ├── ReactiveContactAdapter     # Ask pattern → ContactEngine
│   │   ├── ReactiveMessageAdapter     # Ask pattern → MessageEngine
│   │   ├── EmailService.scala         # Envío de emails (SMTP)
│   │   ├── EmailVerificationService   # Verificación de email
│   │   └── GamificationService.scala  # Sistema de badges y puntos
│   ├── models/                        # Case classes + Slick mappings
│   ├── repositories/                  # Data access layer (async)
│   └── views/                         # Templates Twirl
├── conf/
│   ├── application.conf               # Configuración general
│   ├── routes                         # Rutas HTTP
│   ├── messages / messages.en         # i18n (es/en)
│   └── evolutions/                    # Migraciones de DB
├── public/                            # Assets estáticos
├── sql/                               # Scripts SQL de administración
└── build.sbt                          # Definición del proyecto
```

---

## 🎯 Patrones de Diseño

| Patrón | Uso | Ubicación |
|--------|-----|-----------|
| **Actor Model** | Concurrencia sin locks, procesamiento asíncrono | `ContactEngine`, `MessageEngine` |
| **Ask Pattern** | Request-response sobre actores | `ReactiveContactAdapter`, `ReactiveMessageAdapter` |
| **Repository** | Abstracción de acceso a datos | `*Repository.scala` |
| **Adapter** | Puente entre Controllers y Actor System | `Reactive*Adapter` |
| **Command** | Mensajes tipados como objetos | `ContactCommand`, `MessageCommand` |
| **Dependency Injection** | Inversión de control (Guice) | `Module.scala` |
| **MVC** | Separación de responsabilidades | Controllers + Views + Models |

---

## 📝 Internacionalización

Soporte para español (predeterminado) e inglés via `conf/messages` y `conf/messages.en`.

---

## 👤 Autor

**Federico Pfund** — [@federicopfund](https://github.com/federicopfund)

## 📄 Licencia

MIT

---

<p align="center"><strong>Responsive • Resilient • Elastic • Message-Driven</strong></p>
