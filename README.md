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

El proyecto sigue una **arquitectura de agentes reactivos** con **7 actores Akka Typed** organizados en capas desacopladas:

```mermaid
graph TB
    subgraph Clients["🌐 Clientes"]
        B1["Usuario autenticado"]
        B2["Visitante"]
        B3["Administrador"]
    end

    subgraph Controllers["Controllers (Play Framework)"]
        HC["HomeController"]
        UPC["UserPublicationController"]
        AC["AdminController"]
        AUC["AuthController"]
    end

    subgraph Adapters["Reactive Adapters (Ask / Tell)"]
        RCA["ReactiveContactAdapter"]
        RMA["ReactiveMessageAdapter"]
        RPA["ReactivePublicationAdapter"]
        RGA["ReactiveGamificationAdapter"]
        RNA["ReactiveNotificationAdapter"]
        RMOA["ReactiveModerationAdapter"]
        RAA["ReactiveAnalyticsAdapter"]
    end

    subgraph ActorSystem["Akka Typed Actor System (7 Agents)"]
        CE["🔵 ContactEngine\n(contact-core)"]
        ME["🔵 MessageEngine\n(message-core)"]
        PE["🟢 PublicationEngine\n(publication-core)"]
        GE["🟢 GamificationEngine\n(gamification-core)"]
        NE["🟢 NotificationEngine\n(notification-core)"]
        MOE["🟢 ModerationEngine\n(moderation-core)"]
        AE["🟢 AnalyticsEngine\n(analytics-core)"]
    end

    subgraph Repositories["Repositories (Async / Slick)"]
        CR["ContactRepo"]
        PMR["PrivateMessageRepo"]
        UNR["UserNotificationRepo"]
        PR["PublicationRepo"]
        BR["BadgeRepo"]
    end

    subgraph ExternalServices["External Services"]
        ES["EmailService (SMTP)"]
    end

    subgraph DB["PostgreSQL"]
        DBIcon[("Base de Datos")]
    end

    %% Client → Controller
    B2 -- "POST /contact" --> HC
    B1 -- "POST /send-message" --> UPC
    B1 -- "POST /publication" --> UPC
    B3 -- "POST /approve" --> AC
    B1 -- "POST /register" --> AUC

    %% Controller → Adapter
    HC -- "submitContact()" --> RCA
    UPC -- "sendMessage()" --> RMA
    UPC -- "createPublication()" --> RPA
    UPC -- "trackView()" --> RAA
    AC -- "approve/reject()" --> RPA
    AC -- "moderate()" --> RMOA
    UPC -- "checkBadges()" --> RGA
    AUC -- "notify()" --> RNA

    %% Adapter → Actor (Ask/Tell)
    RCA -- "ask" --> CE
    RMA -- "ask" --> ME
    RPA -- "ask" --> PE
    RGA -- "tell ⚡" --> GE
    RNA -- "tell ⚡" --> NE
    RMOA -- "ask" --> MOE
    RAA -- "tell ⚡" --> AE

    %% Actor → Actor (inter-agent)
    PE -. "notify author" .-> NE
    ME -. "notify receiver" .-> NE
    MOE -. "auto-flag" .-> NE

    %% Actor → Repository
    CE --> CR
    ME --> PMR
    PE --> PR
    GE --> BR
    NE --> UNR
    NE --> ES

    %% Repository → DB
    CR --> DBIcon
    PMR --> DBIcon
    UNR --> DBIcon
    PR --> DBIcon
    BR --> DBIcon

    style ActorSystem fill:#1a365d,stroke:#2b6cb0,color:#fff
    style Adapters fill:#2c5282,stroke:#3182ce,color:#fff
    style Controllers fill:#2d3748,stroke:#4a5568,color:#fff
    style Repositories fill:#1c4532,stroke:#276749,color:#fff
    style ExternalServices fill:#744210,stroke:#975a16,color:#fff
    style DB fill:#553c9a,stroke:#6b46c1,color:#fff
```

### Los 7 Agentes

| # | Agente | Sistema | Patrón | Responsabilidad |
|---|--------|---------|--------|-----------------|
| 🔵 | **ContactEngine** | `contact-core` | Ask | Formularios de contacto: persiste y responde |
| 🔵 | **MessageEngine** | `message-core` | Ask | Mensajería privada + notificaciones al receptor |
| 🟢 | **PublicationEngine** | `publication-core` | Ask | Ciclo de vida: crear → revisar → aprobar/rechazar |
| 🟢 | **GamificationEngine** | `gamification-core` | Tell | Verificación y otorgamiento de badges (fire-and-forget) |
| 🟢 | **NotificationEngine** | `notification-core` | Tell | Hub multi-canal: in-app + email con fan-out |
| 🟢 | **ModerationEngine** | `moderation-core` | Ask | Auto-filtrado de contenido + cola de revisión manual |
| 🟢 | **AnalyticsEngine** | `analytics-core` | Tell | Tracking de métricas in-memory (zero-latency) |

> 🔵 = existente &nbsp; 🟢 = nuevo

### Comunicación inter-agente

Los agentes se comunican entre sí mediante **mensajes tipados**, creando un grafo de eventos:

```mermaid
graph LR
    PE["PublicationEngine"] -- "publication_status" --> NE["NotificationEngine"]
    ME["MessageEngine"] -- "private_message" --> NE
    MOE["ModerationEngine"] -- "content_flagged" --> NE
    PE -- "publication trigger" --> GE["GamificationEngine"]
    AE["AnalyticsEngine"] -.  "metrics (in-memory)" .-> AE

    style PE fill:#276749,color:#fff
    style ME fill:#2b6cb0,color:#fff
    style NE fill:#975a16,color:#fff
    style MOE fill:#9b2c2c,color:#fff
    style GE fill:#553c9a,color:#fff
    style AE fill:#4a5568,color:#fff
```

---

## 🔄 Flujo de Mensajes — Ask vs Tell

El sistema usa dos patrones de comunicación según el caso:

- **Ask** (request-response): cuando el controller necesita el resultado (crear publicación, moderar contenido)
- **Tell** (fire-and-forget): cuando el resultado no bloquea al usuario (analytics, badges, notificaciones)

```mermaid
sequenceDiagram
    participant U as Usuario
    participant C as Controller
    participant A1 as Adapter (Ask)
    participant A2 as Adapter (Tell)
    participant E1 as Engine (Ask)
    participant E2 as Engine (Tell)
    participant NE as NotificationEngine
    participant R as Repository
    participant DB as PostgreSQL

    U->>C: HTTP Request (crear publicación)
    C->>A1: createPublication()
    A1->>E1: ask(CreatePublication, replyTo)
    activate E1
    E1->>R: create() (async)
    R->>DB: SQL INSERT
    DB-->>R: Result
    R-->>E1: Future[Success]
    E1-->>A1: PublicationCreatedOk(id)
    deactivate E1

    par Fire-and-forget (no bloquea)
        C->>A2: trackEvent("publish")
        A2->>E2: tell(TrackEvent)
        Note over E2: In-memory counter++
    and
        C->>A2: checkBadges(userId)
        A2->>E2: tell(CheckBadges)
        E2->>R: award() (async)
    and
        E1->>NE: tell(SendNotification)
        NE->>R: create notification
    end

    A1-->>C: Future[Response]
    C-->>U: HTTP Response (redirect)
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
│   ├── Module.scala                      # DI: provee 7 ActorSystems y 7 Adapters
│   ├── controllers/
│   │   ├── HomeController.scala          # Contacto, páginas públicas
│   │   ├── AuthController.scala          # Login, registro, verificación email
│   │   ├── UserPublicationController     # Publicaciones, mensajería, dashboard
│   │   ├── AdminController.scala         # Panel de administración
│   │   └── actions/
│   │       └── AuthAction.scala          # Acción de autenticación
│   ├── core/                             # 🧠 AGENTES (Akka Typed Actors)
│   │   ├── ContactEngine.scala           # 🔵 Formulario de contacto
│   │   ├── MessageEngine.scala           # 🔵 Mensajería privada
│   │   ├── PublicationEngine.scala       # 🟢 Ciclo de vida de publicaciones
│   │   ├── GamificationEngine.scala      # 🟢 Sistema de badges
│   │   ├── NotificationEngine.scala      # 🟢 Hub multi-canal de notificaciones
│   │   ├── ModerationEngine.scala        # 🟢 Auto-moderación de contenido
│   │   └── AnalyticsEngine.scala         # 🟢 Métricas y tracking
│   ├── services/                         # 🔌 ADAPTERS (Ask/Tell → Actors)
│   │   ├── ReactiveContactAdapter        # Ask → ContactEngine
│   │   ├── ReactiveMessageAdapter        # Ask → MessageEngine
│   │   ├── ReactivePublicationAdapter    # Ask → PublicationEngine
│   │   ├── ReactiveGamificationAdapter   # Tell → GamificationEngine
│   │   ├── ReactiveNotificationAdapter   # Tell → NotificationEngine
│   │   ├── ReactiveModerationAdapter     # Ask → ModerationEngine
│   │   ├── ReactiveAnalyticsAdapter      # Tell → AnalyticsEngine
│   │   ├── EmailService.scala            # SMTP email delivery
│   │   ├── EmailVerificationService      # Verificación de email
│   │   └── GamificationService.scala     # Legacy (reemplazado por Engine)
│   ├── models/                           # Case classes + Slick mappings
│   ├── repositories/                     # Data access layer (async)
│   └── views/                            # Templates Twirl
├── conf/
│   ├── application.conf                  # Configuración general
│   ├── routes                            # Rutas HTTP
│   ├── messages / messages.en            # i18n (es/en)
│   └── evolutions/                       # Migraciones de DB
├── public/                               # Assets estáticos
├── sql/                                  # Scripts SQL de administración
└── build.sbt                             # Definición del proyecto
```

---

## 🎯 Patrones de Diseño

| Patrón | Uso | Ubicación |
|--------|-----|-----------|
| **Actor Model** | Concurrencia sin locks, procesamiento asíncrono | 7 Engines en `core/` |
| **Ask Pattern** | Request-response sobre actores | Contact, Message, Publication, Moderation |
| **Tell Pattern** | Fire-and-forget, zero-latency | Gamification, Notification, Analytics |
| **pipeToSelf** | Convertir Futures en mensajes del actor | Todos los Engines |
| **Fan-out** | Un evento → múltiples canales | NotificationEngine (in-app + email) |
| **Repository** | Abstracción de acceso a datos | 13 Repositories |
| **Adapter** | Puente entre Controllers y Actor System | 7 `Reactive*Adapter` |
| **Command** | Mensajes tipados como objetos | `sealed trait *Command` |
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
