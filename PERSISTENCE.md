# Sistema de Persistencia de Contactos

## 📦 Stack Tecnológico

- **Slick**: ORM reactivo para Scala
- **H2**: Base de datos en memoria (desarrollo)
- **Play Evolutions**: Migraciones de base de datos
- **Akka Typed**: Sistema de actores para procesamiento asíncrono

## 🗄️ Esquema de Base de Datos

### Tabla: `contacts`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL | ID único auto-incremental |
| name | VARCHAR(255) | Nombre del contacto |
| email | VARCHAR(255) | Email del contacto |
| message | TEXT | Mensaje del contacto |
| created_at | TIMESTAMP | Fecha de creación |
| status | VARCHAR(50) | Estado (pending, processed, archived) |

### Índices
- `idx_contacts_email`: Para búsquedas por email
- `idx_contacts_created_at`: Para ordenamiento temporal
- `idx_contacts_status`: Para filtrado por estado

## 🚀 Uso

### Guardar un contacto (Automático)
El formulario de contacto guarda automáticamente en la base de datos:
```scala
POST /contact
{
  name: "Juan Pérez",
  email: "juan@example.com",
  message: "Mensaje de prueba"
}
```

### API de Consulta

#### Listar contactos (paginado)
```bash
GET /api/contacts?page=0
```

#### Estadísticas
```bash
GET /api/contacts/stats
```

Respuesta:
```json
{
  "total": 42,
  "timestamp": "2026-01-05T21:00:00Z"
}
```

## 🔧 Configuración

### Desarrollo (H2 en memoria)
```conf
slick.dbs.default {
  profile = "slick.jdbc.H2Profile$"
  db {
    driver = "org.h2.Driver"
    url = "jdbc:h2:mem:play;MODE=PostgreSQL"
  }
}
```

### Producción (PostgreSQL)
```conf
slick.dbs.default {
  profile = "slick.jdbc.PostgresProfile$"
  db {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/reactive_manifesto"
    user = "postgres"
    password = "your_password"
  }
}
```

Luego agregar dependencia en `build.sbt`:
```scala
"org.postgresql" % "postgresql" % "42.7.1"
```

## 🏗️ Arquitectura

```
Usuario → Formulario → HomeController
                           ↓
                    ReactiveContactAdapter
                           ↓
                    ContactEngine (Actor)
                           ↓
                    ContactRepository
                           ↓
                    Base de Datos (H2/PostgreSQL)
```

### Flujo Reactivo
1. El usuario envía el formulario
2. El controlador valida los datos
3. Se crea un mensaje para el actor `ContactEngine`
4. El actor procesa de forma asíncrona
5. Se guarda en la base de datos mediante el repositorio
6. Se responde al usuario con el ID del contacto

## 📊 Operaciones del Repositorio

### ContactRepository

```scala
// Guardar
save(contact: ContactRecord): Future[ContactRecord]

// Buscar por ID
findById(id: Long): Future[Option[ContactRecord]]

// Listar todos
listAll(): Future[Seq[ContactRecord]]

// Listar con paginación
list(page: Int, pageSize: Int): Future[Seq[ContactRecord]]

// Buscar por email
findByEmail(email: String): Future[Seq[ContactRecord]]

// Actualizar estado
updateStatus(id: Long, status: String): Future[Int]

// Contar total
count(): Future[Int]

// Eliminar
delete(id: Long): Future[Int]
```

## 🔐 Seguridad (Recomendaciones)

Para producción, proteger los endpoints de admin:

```scala
// Agregar autenticación
def listContacts = Authenticated.async { implicit request =>
  // ...
}
```

O configurar filtros CORS específicos en `application.conf`.

## 🧪 Testing

Las migraciones se aplican automáticamente al iniciar la aplicación.
Para verificar:

1. Iniciar la aplicación
2. Enviar un formulario de contacto
3. Visitar: `http://localhost:9000/api/contacts/stats`

## 📝 Notas

- **H2** es volátil: Los datos se pierden al reiniciar la aplicación
- Para persistencia real, cambiar a PostgreSQL
- Las evoluciones se aplican automáticamente (`autoApply = true`)
- El campo `status` permite clasificar contactos:
  - `pending`: Nuevo contacto no procesado
  - `processed`: Contacto revisado
  - `archived`: Contacto archivado
