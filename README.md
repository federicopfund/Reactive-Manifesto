# ⚡ Reactive Manifesto

Una aplicación web moderna que demuestra los principios del [Manifiesto Reactivo](https://www.reactivemanifesto.org/) utilizando Play Framework y Akka Typed.

![Desktop View](https://github.com/user-attachments/assets/a42bfee1-78f3-4c63-88a3-1ddee5982b33)

## 🎯 Descripción

Esta aplicación web presenta los cuatro pilares fundamentales del Manifiesto Reactivo (Responsive, Resilient, Elastic, Message-Driven) a través de un diseño moderno y profesional, con un formulario de contacto que implementa arquitectura reactiva mediante Akka Typed actors.

## ✨ Características

### Diseño Moderno y Profesional
- **Interfaz atractiva**: Hero section con gradiente púrpura
- **Layout basado en tarjetas**: Presentación clara de conceptos
- **Tipografía profesional**: Uso de la fuente Inter
- **Animaciones suaves**: Transiciones y efectos hover

### Diseño Responsivo
- **Mobile-first**: Optimizado desde 375px (móvil) hasta 1200px+ (desktop)
- **Flexbox/Grid**: Layouts modernos y adaptativos
- **Touch-friendly**: Elementos interactivos optimizados para móviles

### Arquitectura Reactiva
- **Message-Driven**: Sistema de actores Akka Typed
- **Responsive**: Respuestas rápidas y UI fluida
- **Resilient**: Manejo robusto de errores
- **Elastic**: Sistema escalable basado en actores

### Funcionalidades Interactivas
- Navegación con scroll suave
- Validación de formularios en tiempo real
- Mensajes de éxito/error auto-desaparecibles
- Animaciones al hacer scroll

## 🛠️ Stack Tecnológico

- **Backend**: Play Framework 3.0.1
- **Lenguaje**: Scala 2.13.12
- **Sistema Reactivo**: Akka Typed 2.8.5
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **Build Tool**: SBT 1.9.7

## 📋 Requisitos Previos

- Java 17 o superior
- SBT 1.9.x

## 🚀 Instalación y Ejecución

### Deployment en Producción

**Para instrucciones completas de deployment, consulta: [DEPLOYMENT.md](DEPLOYMENT.md)**

Esta aplicación se puede deployar en múltiples plataformas:
- 🐳 **Docker / Docker Compose** - Containerizado
- 🎯 **Heroku** - Deploy con un click
- 🚂 **Railway** - Alternativa moderna a Heroku
- 🎨 **Render** - Free tier generoso
- ☁️ **Google Cloud Run** - Serverless
- 🌐 **AWS ECS/Elastic Beanstalk** - Escalable

#### Quick Deploy con Script Helper

```bash
# Ejecutar el script de deployment interactivo
./deploy.sh
```

#### Quick Deploy con Docker

```bash
# Construir y ejecutar
docker build -t reactive-manifesto .
docker run -p 9000:9000 -e APPLICATION_SECRET="tu-secret-aqui" reactive-manifesto
```

#### Quick Deploy en Heroku

```bash
# Crear app y deployar
heroku create mi-reactive-app
heroku config:set APPLICATION_SECRET=$(openssl rand -base64 32)
git push heroku main
```

### Comandos Rápidos para Levantar la Aplicación

#### 1️⃣ Liberar puerto 9000 (si está ocupado)
```bash
# Matar proceso en puerto 9000
fuser -k 9000/tcp 2>/dev/null

# O usando lsof
lsof -ti:9000 | xargs kill -9 2>/dev/null
```

#### 2️⃣ Limpiar compilaciones previas
```bash
cd /workspaces/Reactive-Manifiesto && sbt clean
```

#### 3️⃣ Compilar el proyecto
```bash
sbt compile
```

#### 4️⃣ Iniciar el servidor
```bash
sbt run
```

**El servidor estará disponible en:** http://localhost:9000

### 🎯 Comando Todo-en-Uno
```bash
# Liberar puerto, limpiar, compilar e iniciar
fuser -k 9000/tcp 2>/dev/null && sbt clean compile run
```

### 🔄 Modo Desarrollo con Auto-reload
```bash
# Recarga automática al detectar cambios
sbt ~run
```

### 🛑 Detener el Servidor

**Desde terminal sbt:**
- Presiona `Enter` o `Ctrl+D`

**Desde otra terminal:**
```bash
fuser -k 9000/tcp
```

### 📋 Instalación Completa

#### 1. Clonar el repositorio

```bash
git clone https://github.com/federicopfund/Reactive-Manifiesto.git
cd Reactive-Manifiesto
```

#### 2. Ejecutar la aplicación

```bash
sbt run
```

La aplicación estará disponible en: `http://localhost:9000`

#### 3. Compilar el proyecto

```bash
sbt compile
```

#### 4. Ejecutar tests

```bash
sbt test
```

## 🔧 Comandos Útiles

### Verificar estado del servidor
```bash
# Ver procesos sbt activos
ps aux | grep "[s]bt run"

# Ver qué proceso usa el puerto 9000
lsof -i:9000

# Probar conectividad
curl http://localhost:9000/
```

### Limpieza completa
```bash
# Eliminar archivos compilados
sbt clean

# Limpieza profunda (incluye caché)
rm -rf target/ project/target/ ~/.ivy2/cache
```

### Recargar dependencias
```bash
sbt
> reload
> update
> compile
```

### Ejecutar en puerto diferente
```bash
# Opción 1
sbt "run 8080"

# Opción 2
export PLAY_HTTP_PORT=8080
sbt run
```

### 🐛 Troubleshooting

**Error: Puerto 9000 en uso**
```bash
fuser -k 9000/tcp
```

**Error: Compilación falla**
```bash
sbt clean
rm -rf target/
sbt update
sbt compile
```

**Error: Dependencias no resueltas**
```bash
sbt clean
rm -rf ~/.ivy2/cache/
sbt update
```

## 📁 Estructura del Proyecto

```
Reactive-Manifiesto/
├── app/
│   ├── controllers/          # Controladores HTTP
│   │   └── HomeController.scala
│   ├── core/                 # Lógica de negocio y actores
│   │   └── ContactEngine.scala
│   ├── services/             # Servicios y adaptadores
│   │   └── ReactiveContactAdapter.scala
│   ├── views/                # Templates Twirl
│   │   ├── main.scala.html
│   │   └── index.scala.html
│   └── Module.scala          # Configuración de inyección de dependencias
├── conf/
│   ├── application.conf      # Configuración de la aplicación
│   ├── routes                # Definición de rutas HTTP
│   ├── messages              # Mensajes i18n (español)
│   ├── messages.en           # Mensajes i18n (inglés)
│   └── logback.xml           # Configuración de logging
├── public/
│   ├── stylesheets/
│   │   └── main.css          # Estilos CSS principales
│   └── javascripts/
│       └── main.js           # JavaScript para interactividad
├── project/
│   ├── build.properties      # Versión de SBT
│   └── plugins.sbt           # Plugins de SBT
└── build.sbt                 # Definición del proyecto
```

## 🎨 Características del Diseño

### Secciones Principales

1. **Hero Section**
   - Título impactante con degradado
   - Subtítulo descriptivo
   - Botones CTA para navegación

2. **Los 4 Pilares del Manifiesto Reactivo**
   - 📱 Responsivo: Respuestas oportunas
   - 🛡️ Resiliente: Tolerante a fallos
   - 📈 Elástico: Escalabilidad automática
   - 💬 Orientado a Mensajes: Comunicación asíncrona

3. **¿Por qué Reactive?**
   - Mejor experiencia de usuario
   - Escalabilidad mejorada
   - Mayor confiabilidad

4. **Formulario de Contacto**
   - Validación en tiempo real
   - Procesamiento asíncrono con Akka
   - Feedback inmediato al usuario

## 🏗️ Arquitectura de la Aplicación

### Visión General

La aplicación implementa una **arquitectura reactiva en capas** que combina Play Framework para la capa web, Akka Typed para la lógica de negocio concurrente, y Slick para el acceso reactivo a datos. Todos los componentes siguen los principios del Manifiesto Reactivo: **Responsive, Resilient, Elastic y Message-Driven**.

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                       │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  Browser    │  │  Templates   │  │   Static Assets  │   │
│  │  (HTML/CSS/ │→ │  (Twirl)     │  │   (CSS/JS)       │   │
│  │   JS)       │  └──────────────┘  └──────────────────┘   │
│  └─────────────┘                                             │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP/WebSocket
┌────────────────────────────▼────────────────────────────────┐
│                    WEB/CONTROLLER LAYER                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         HomeController (Play Framework)              │   │
│  │  • Manejo de requests HTTP                           │   │
│  │  • Validación de formularios                         │   │
│  │  • Renderizado de vistas                             │   │
│  │  • API endpoints (/api/contacts, /api/contacts/stats)│  │
│  └────────────┬─────────────────────────────────────────┘   │
└───────────────┼─────────────────────────────────────────────┘
                │
┌───────────────▼─────────────────────────────────────────────┐
│                   SERVICE/ADAPTER LAYER                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │       ReactiveContactAdapter (Service)               │   │
│  │  • Abstracción del sistema de actores                │   │
│  │  • Conversión de Futures a respuestas HTTP           │   │
│  │  • Patrón Ask para comunicación con actores          │   │
│  └────────────┬─────────────────────────────────────────┘   │
└───────────────┼─────────────────────────────────────────────┘
                │ Message Passing
┌───────────────▼─────────────────────────────────────────────┐
│                    BUSINESS LOGIC LAYER                      │
│           (Actor System - Akka Typed)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           ContactEngine (Typed Actor)                │   │
│  │  • Procesamiento asíncrono de contactos              │   │
│  │  • Manejo de mensajes: SubmitContact                 │   │
│  │  • Integración con capa de persistencia              │   │
│  │  • Manejo de errores y reintentos                    │   │
│  │  • pipeToSelf para operaciones async                 │   │
│  └────────────┬─────────────────────────────────────────┘   │
└───────────────┼─────────────────────────────────────────────┘
                │ Database Operations
┌───────────────▼─────────────────────────────────────────────┐
│                  DATA ACCESS LAYER                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │        ContactRepository (Slick ORM)                 │   │
│  │  • CRUD operations: save, findById, list, delete     │   │
│  │  • Queries reactivas con Future[T]                   │   │
│  │  • Connection pooling optimizado                     │   │
│  │  • Patrón Repository completo                        │   │
│  └────────────┬─────────────────────────────────────────┘   │
│  ┌────────────▼─────────────────────────────────────────┐   │
│  │         ContactsTable (Slick Table Mapping)          │   │
│  │  • Mapeo ORM de ContactRecord a tabla SQL            │   │
│  │  • Definición de columnas e índices                  │   │
│  └────────────┬─────────────────────────────────────────┘   │
└───────────────┼─────────────────────────────────────────────┘
                │ JDBC/SQL
┌───────────────▼─────────────────────────────────────────────┐
│                   PERSISTENCE LAYER                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │     H2 Database (Development) / PostgreSQL (Prod)    │   │
│  │  • Tabla: contacts (id, name, email, message, ...)   │   │
│  │  • Índices: email, created_at, status                │   │
│  │  • Evolutions para migraciones                       │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 📦 Componentes Clave

#### 1. **Presentation Layer (Frontend)**

**Responsabilidades:**
- Renderizado de vistas con Twirl templates
- Interacción del usuario (formularios, navegación)
- Dark mode y animaciones CSS
- Validación del lado del cliente

**Tecnologías:**
- HTML5 con templates Twirl (Scala)
- CSS3 con variables para theming
- Vanilla JavaScript para interactividad
- Responsive design (mobile-first)

**Archivos principales:**
```
app/views/
├── main.scala.html          # Layout principal
├── index.scala.html         # Página de inicio
├── publicaciones.scala.html # Lista de artículos
├── portafolio.scala.html    # Proyectos
└── articulos/               # Artículos individuales
    ├── articleLayout.scala.html
    ├── akkaActors.scala.html
    └── ...
```

#### 2. **Web/Controller Layer**

**Responsabilidades:**
- Manejo de peticiones HTTP
- Validación de datos de entrada
- Enrutamiento de requests
- Serialización/deserialización JSON
- Manejo de sesiones y CSRF

**Componente principal: `HomeController`**
```scala
class HomeController @Inject()(
  cc: ControllerComponents,
  contactAdapter: ReactiveContactAdapter,
  repository: ContactRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc)
```

**Endpoints:**
- `GET /` - Página principal
- `POST /contact` - Enviar formulario de contacto
- `GET /publicaciones` - Lista de artículos
- `GET /portafolio` - Proyectos
- `GET /articulos/:name` - Artículo específico
- `GET /api/contacts` - API para listar contactos (admin)
- `GET /api/contacts/stats` - Estadísticas de contactos

#### 3. **Service/Adapter Layer**

**Responsabilidades:**
- Abstracción del sistema de actores
- Conversión entre el modelo de actores y HTTP
- Patrón Ask para comunicación request-response
- Manejo de timeouts

**Componente principal: `ReactiveContactAdapter`**
```scala
@Singleton
class ReactiveContactAdapter @Inject()(
  system: ActorSystem[ContactCommand]
)(implicit ec: ExecutionContext) {
  
  def submitContact(contact: Contact): Future[ContactResponse] = {
    implicit val timeout: Timeout = 5.seconds
    system.ask[ContactResponse](replyTo => 
      SubmitContact(contact, replyTo)
    )
  }
}
```

**Patrón utilizado:** Ask Pattern (request-response sobre actores)

#### 4. **Business Logic Layer (Actor System)**

**Responsabilidades:**
- Procesamiento asíncrono de mensajes
- Lógica de negocio
- Manejo de concurrencia sin locks
- Integración con capa de datos
- Supervisión y recuperación ante fallos

**Componente principal: `ContactEngine`**
```scala
object ContactEngine {
  sealed trait ContactCommand
  case class SubmitContact(contact: Contact, replyTo: ActorRef[ContactResponse]) 
    extends ContactCommand
  private case class ContactSaved(savedContact: ContactRecord, 
                                  replyTo: ActorRef[ContactResponse]) 
    extends ContactCommand
  private case class ContactSaveFailed(exception: Throwable, 
                                       replyTo: ActorRef[ContactResponse]) 
    extends ContactCommand

  def apply(repository: ContactRepository)
           (implicit ec: ExecutionContext): Behavior[ContactCommand]
}
```

**Flujo de procesamiento:**
1. Recibe mensaje `SubmitContact`
2. Crea `ContactRecord` para la DB
3. Llama a `repository.save()` de forma asíncrona
4. Usa `context.pipeToSelf` para convertir Future en mensaje
5. Maneja `ContactSaved` o `ContactSaveFailed`
6. Responde al remitente con `ContactSubmitted` o `ContactError`

**Patrón utilizado:** Actor Model + Event-driven + Non-blocking I/O

#### 5. **Data Access Layer**

**Responsabilidades:**
- Abstracción de acceso a datos
- Queries reactivas con Futures
- Connection pooling
- Transacciones (si son necesarias)
- Mapeo objeto-relacional

**Componente principal: `ContactRepository`**
```scala
@Singleton
class ContactRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  
  private val db = dbConfigProvider.get[JdbcProfile].db
  private val contacts = TableQuery[ContactsTable]
  
  // Operaciones CRUD
  def save(contact: ContactRecord): Future[ContactRecord]
  def findById(id: Long): Future[Option[ContactRecord]]
  def list(page: Int, pageSize: Int): Future[Seq[ContactRecord]]
  def findByEmail(email: String): Future[Seq[ContactRecord]]
  def updateStatus(id: Long, status: String): Future[Int]
  def count(): Future[Int]
  def delete(id: Long): Future[Int]
}
```

**Modelo de dominio: `ContactRecord`**
```scala
case class ContactRecord(
  id: Option[Long] = None,
  name: String,
  email: String,
  message: String,
  createdAt: Instant = Instant.now(),
  status: String = "pending"
)
```

**Patrón utilizado:** Repository Pattern + Active Record (Slick)

#### 6. **Persistence Layer**

**Responsabilidades:**
- Almacenamiento físico de datos
- Gestión de transacciones ACID
- Índices para optimización de queries
- Migraciones de schema

**Base de datos:**
- **Desarrollo:** H2 in-memory (modo PostgreSQL)
- **Producción:** PostgreSQL (recomendado)

**Schema (tabla contacts):**
```sql
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'pending'
);

CREATE INDEX idx_contacts_email ON contacts(email);
CREATE INDEX idx_contacts_created_at ON contacts(created_at);
CREATE INDEX idx_contacts_status ON contacts(status);
```

### 🔄 Flujo de Datos: Ejemplo de Envío de Contacto

```
1. Usuario completa formulario
         │
         ▼
2. JavaScript valida datos
         │
         ▼
3. POST /contact → HomeController.submitContact()
         │
         ├─ Validación con Play Forms
         │
         ▼
4. contactAdapter.submitContact(Contact) → ReactiveContactAdapter
         │
         ├─ Pattern: Ask (timeout: 5s)
         │
         ▼
5. ActorSystem ! SubmitContact(contact, replyTo) → ContactEngine
         │
         ├─ Crea ContactRecord
         │
         ▼
6. repository.save(contactRecord) → ContactRepository
         │
         ├─ Future[ContactRecord]
         │
         ▼
7. context.pipeToSelf(...) → ContactEngine
         │
         ├─ Success → ContactSaved(savedContact, replyTo)
         ├─ Failure → ContactSaveFailed(exception, replyTo)
         │
         ▼
8. replyTo ! ContactSubmitted(id) OR ContactError(msg)
         │
         ▼
9. Future.map → HomeController
         │
         ├─ Success: Redirect con flash message
         ├─ Error: BadRequest con errores
         │
         ▼
10. Usuario ve confirmación o error
```

### 🎯 Patrones de Diseño Utilizados

#### 1. **Actor Model (Akka Typed)**
- **Propósito:** Concurrencia sin locks, procesamiento asíncrono
- **Ubicación:** `ContactEngine`
- **Beneficios:** Thread-safe, escalable, resiliente

#### 2. **Repository Pattern**
- **Propósito:** Abstracción del acceso a datos
- **Ubicación:** `ContactRepository`
- **Beneficios:** Testeable, mantenible, desacoplado

#### 3. **Dependency Injection (Guice)**
- **Propósito:** Inversión de control, desacoplamiento
- **Ubicación:** `Module.scala`, constructores con `@Inject`
- **Beneficios:** Testeable, flexible, mantenible

#### 4. **MVC (Model-View-Controller)**
- **Propósito:** Separación de responsabilidades
- **Ubicación:** Toda la aplicación
- **Beneficios:** Organización clara, mantenible

#### 5. **Service Layer / Adapter Pattern**
- **Propósito:** Abstracción de la lógica de negocio
- **Ubicación:** `ReactiveContactAdapter`
- **Beneficios:** Desacoplamiento entre web y actores

#### 6. **Command Pattern**
- **Propósito:** Encapsulación de operaciones como objetos
- **Ubicación:** `ContactCommand` (SubmitContact, etc.)
- **Beneficios:** Extensible, type-safe, auditable

#### 7. **Future/Promise Pattern**
- **Propósito:** Programación asíncrona no bloqueante
- **Ubicación:** Toda la aplicación (controladores, repositorio, actores)
- **Beneficios:** Non-blocking I/O, alto throughput

#### 8. **Template Method (Twirl)**
- **Propósito:** Reutilización de estructura HTML
- **Ubicación:** `main.scala.html`, `articleLayout.scala.html`
- **Beneficios:** DRY, consistencia visual

### ⚙️ Configuración y Dependency Injection

**Archivo: `app/Module.scala`**
```scala
class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ActorSystem[ContactCommand]])
      .toProvider(classOf[ActorSystemProvider])
      .asEagerSingleton()
  }
}

class ActorSystemProvider @Inject()(
  repository: ContactRepository
)(implicit ec: ExecutionContext) extends Provider[ActorSystem[ContactCommand]] {
  override def get(): ActorSystem[ContactCommand] = {
    ActorSystem(ContactEngine(repository), "contact-core")
  }
}
```

**Configuración: `conf/application.conf`**
```conf
# Slick Database Configuration
slick.dbs.default {
  profile = "slick.jdbc.H2Profile$"
  db {
    driver = "org.h2.Driver"
    url = "jdbc:h2:mem:play;MODE=PostgreSQL"
    numThreads = 10
    maxConnections = 10
  }
}

# Evolutions
play.evolutions {
  enabled = true
  autoApply = true
  db.default.enabled = true
}

# Akka Configuration
akka {
  loglevel = "INFO"
  actor {
    default-dispatcher {
      fork-join-executor {
        parallelism-min = 4
        parallelism-factor = 2.0
        parallelism-max = 16
      }
    }
  }
}
```

### 🔒 Principios Reactivos Implementados

#### ✅ Responsive (Responsivo)
- **Non-blocking I/O** en todos los niveles
- **Futures** para operaciones asíncronas
- **Timeouts** configurados (5s en Ask pattern)
- **Fast fail** con manejo de errores apropiado

#### ✅ Resilient (Resiliente)
- **Actor supervision** (actores se reinician ante fallos)
- **Error handling** en cada capa
- **Graceful degradation** con mensajes de error claros
- **Database connection pooling** con recuperación automática

#### ✅ Elastic (Elástico)
- **Actor model** permite escalado horizontal
- **Stateless controllers** facilitan load balancing
- **Connection pooling** ajustable según carga
- **Arquitectura preparada** para clustering con Akka Cluster

#### ✅ Message-Driven (Orientado a Mensajes)
- **Akka Actors** como base de comunicación
- **Asynchronous message passing** entre componentes
- **Backpressure** implícito en sistema de actores
- **Location transparency** (actores pueden estar en diferentes nodos)

### 📊 Escalabilidad y Performance

**Estrategias implementadas:**
1. **Non-blocking I/O:** Toda operación I/O usa Futures
2. **Connection pooling:** 10 conexiones máximas a DB
3. **Actor concurrency:** Procesa múltiples requests en paralelo
4. **Static asset caching:** Assets servidos con cache headers
5. **Database indexes:** Queries optimizadas en email, fecha y status

**Capacidad estimada (hardware modesto):**
- **Throughput:** 1,000+ requests/segundo
- **Latencia p99:** < 100ms (con DB local)
- **Concurrent connections:** 10,000+ (limitado por DB connections)
- **Memory footprint:** ~200MB (JVM + Play + Akka)

### 🧪 Testing

**Estrategias de testing por capa:**

1. **Controllers:** Play Test helpers, FakeRequest
2. **Actores:** Akka TestKit, TestProbe
3. **Repository:** Base de datos H2 en memoria
4. **Integration:** TestContainers para PostgreSQL real
5. **UI:** Selenium/Playwright para E2E tests

### 📚 Recursos y Referencias

- **Play Framework:** https://www.playframework.com/
- **Akka Typed:** https://doc.akka.io/docs/akka/current/typed/
- **Slick:** https://scala-slick.org/
- **Reactive Manifesto:** https://www.reactivemanifesto.org/
- **Reactive Design Patterns:** https://www.reactivedesignpatterns.com/

## 🔧 Arquitectura Reactiva

### Flujo del Formulario de Contacto

```scala
Usuario → HomeController → ReactiveContactAdapter → ContactEngine (Akka Actor)
                                                            ↓
                                                     Procesamiento Asíncrono
                                                            ↓
Usuario ← Flash Message ← HomeController ← ContactResponse
```

### Componentes Clave

**ContactEngine**: Actor Akka Typed que procesa mensajes de forma asíncrona
```scala
sealed trait ContactCommand
case class SubmitContact(contact: Contact, replyTo: ActorRef[ContactResponse])
```

**ReactiveContactAdapter**: Adaptador que permite la comunicación entre Play y Akka
```scala
def submitContact(contact: Contact): Future[ContactResponse]
```

**HomeController**: Controlador que maneja peticiones HTTP y delega al sistema de actores
```scala
def submitContact() = Action.async { implicit request =>
  // Validación y delegación al adapter
}
```

## 📱 Diseño Responsivo

La aplicación se adapta perfectamente a diferentes tamaños de pantalla:

- **Mobile**: 375px - 767px
- **Tablet**: 768px - 1023px
- **Desktop**: 1024px+


## 🧪 Testing

El proyecto incluye tests unitarios para validar:
- Lógica de actores Akka
- Validación de formularios
- Respuestas del controlador

## 📝 Internacionalización

Soporte para múltiples idiomas:
- Español (es) - predeterminado
- Inglés (en)

Los mensajes se definen en `conf/messages` y `conf/messages.en`.

## 🤝 Contribuir

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT.

## 👤 Autor

**Federico Pfund**
- GitHub: [@federicopfund](https://github.com/federicopfund)

## 🙏 Agradecimientos

- [The Reactive Manifesto](https://www.reactivemanifesto.org/)
- [Play Framework](https://www.playframework.com/)
- [Akka](https://akka.io/)

---

**Responsive • Resilient • Elastic • Message-Driven**
