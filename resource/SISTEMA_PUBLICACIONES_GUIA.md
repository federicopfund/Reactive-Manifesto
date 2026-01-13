# 📝 Sistema de Publicaciones con Aprobación de Admin

## ✅ Estado: **COMPLETAMENTE IMPLEMENTADO**

El sistema de creación de contenido por usuarios con flujo de aprobación de administradores está **100% funcional**.

---

## 🎯 Funcionalidad Principal

### Para Usuarios:
Los usuarios pueden **crear su propio contenido** (artículos, publicaciones) que **solo se publican después de ser aprobados** por un administrador.

### Para Administradores:
Los administradores tienen una **interfaz de revisión** donde pueden aprobar o rechazar publicaciones pendientes.

---

## 🔄 Flujo Completo del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    FLUJO DE PUBLICACIÓN                      │
└─────────────────────────────────────────────────────────────┘

1. USUARIO CREA CONTENIDO
   ↓
   Estado: "draft" (borrador)
   ├─ Puede editar libremente
   ├─ Puede ver vista previa
   └─ Puede eliminar
   
2. USUARIO ENVÍA PARA REVISIÓN
   ↓
   Estado: "pending" (pendiente)
   ├─ Ya no puede editar
   └─ Espera aprobación de admin
   
3. ADMIN REVISA CONTENIDO
   ↓
   ┌─────────────────┬─────────────────┐
   │    APRUEBA      │    RECHAZA      │
   │       ↓         │       ↓         │
   │  "approved"     │   "rejected"    │
   │  ✅ PÚBLICO     │  ❌ No público  │
   │                 │  + razón        │
   └─────────────────┴─────────────────┘
   
4. USUARIO VE EL RESULTADO
   ↓
   ├─ Si aprobado: Aparece en publicaciones públicas
   └─ Si rechazado: Ve la razón y puede crear nueva versión
```

---

## 🚀 Cómo Probar el Sistema

### Paso 1: Iniciar el Servidor

```bash
cd /workspaces/Reactive-Manifiesto
sbt run
```

Espera a ver:
```
INFO  p.c.s.PekkoHttpServer - Listening for HTTP on /[0:0:0:0:0:0:0:0]:9000
```

### Paso 2: Crear un Usuario

**Opción A: Desde la interfaz**
1. Ve a: http://localhost:9000/register
2. Completa el formulario:
   - Username: `escritor1`
   - Email: `escritor1@example.com`
   - Password: `123456`
   - Full Name: `Juan Escritor`
3. Click en "Registrarse"

**Opción B: Desde SQL** (requiere acceso a H2 Console)
```sql
INSERT INTO users (username, email, password_hash, full_name, role, created_at, last_login) 
VALUES ('escritor1', 'escritor1@example.com', 
        '$2a$10$...hash_aqui...', 
        'Juan Escritor', 'user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### Paso 3: Login como Usuario

1. Ve a: http://localhost:9000/login
2. Usa tab **"Usuario"**
3. Credenciales:
   - Username: `escritor1`
   - Password: `123456`
4. Te redirige a: `/user/dashboard`

### Paso 4: Crear una Publicación

**En el dashboard de usuario:**

1. Click en **"➕ Nueva Publicación"**
2. Llena el formulario:
   ```
   Título:     "Mi primer artículo sobre Akka"
   Categoría:  "Tutorial"
   Excerpt:    "Aprende los conceptos básicos de Akka Actors"
   Contenido:  "En este artículo vamos a explorar... (mínimo 50 caracteres)"
   Tags:       "akka, scala, reactive"
   ```
3. Click en **"💾 Guardar Borrador"**
4. Estado: **draft** (borrador)

### Paso 5: Enviar para Revisión

**En el dashboard:**

1. Ve a la tabla de publicaciones
2. Encuentra tu artículo (estado: **🟡 draft**)
3. Click en **"📤 Enviar a Revisión"**
4. Confirma la acción
5. Estado cambia a: **🔵 pending** (pendiente)
6. Ya **no puedes editarla** (está en cola de revisión)

### Paso 6: Login como Admin

1. Abre una nueva ventana de incógnito
2. Ve a: http://localhost:9000/login
3. Usa tab **"Administrador"**
4. Credenciales por defecto:
   - Username: `admin`
   - Password: `admin123`
5. Te redirige a: `/admin/dashboard`

### Paso 7: Revisar Publicaciones Pendientes

**En el panel de admin:**

1. En el menú superior, click en **"📄 Publicaciones"**
2. O ve directamente a: http://localhost:9000/admin/publications/pending
3. Verás una **lista de publicaciones pendientes**:
   - Título del artículo
   - Autor (nombre y username)
   - Fecha de envío
   - Categoría
   - Extracto

### Paso 8: Ver Detalle de Publicación

1. Click en **"Ver Detalle"** de una publicación
2. Verás:
   - Todo el contenido completo
   - Información del autor
   - Metadata (categoría, tags, fechas)
   - Botones de acción:
     - **✅ Aprobar**
     - **❌ Rechazar**

### Paso 9A: Aprobar la Publicación

1. Click en **"✅ Aprobar Publicación"**
2. Confirma la acción
3. Estado cambia a: **approved**
4. La publicación ahora es **pública**
5. Aparecerá en las rutas públicas de publicaciones

### Paso 9B: Rechazar la Publicación (Alternativa)

1. Click en **"❌ Rechazar"**
2. Aparece un campo de texto
3. Escribe la razón del rechazo:
   ```
   "El contenido necesita más detalle técnico y ejemplos prácticos"
   ```
4. Click en **"Rechazar Publicación"**
5. Estado cambia a: **rejected**
6. La razón se guarda en la base de datos

### Paso 10: Ver el Resultado como Usuario

1. Vuelve a la ventana del usuario
2. Ve a: http://localhost:9000/user/dashboard
3. Verás tu publicación con el nuevo estado:
   - Si fue **aprobada**: 🟢 **approved** (puede verla pública)
   - Si fue **rechazada**: 🔴 **rejected** (ve la razón del rechazo)

---

## 📍 URLs del Sistema

### Rutas de Usuario (Requiere login como usuario)

| Ruta | Método | Descripción |
|------|--------|-------------|
| `/user/dashboard` | GET | Dashboard con todas las publicaciones |
| `/user/publications/new` | GET | Formulario crear publicación |
| `/user/publications/new` | POST | Guardar nueva publicación |
| `/user/publications/:id/edit` | GET | Formulario editar publicación |
| `/user/publications/:id/edit` | POST | Actualizar publicación |
| `/user/publications/:id` | GET | Ver preview de publicación |
| `/user/publications/:id/submit` | POST | Enviar a revisión |
| `/user/publications/:id/delete` | POST | Eliminar publicación |

### Rutas de Admin (Requiere login como admin)

| Ruta | Método | Descripción |
|------|--------|-------------|
| `/admin/publications/pending` | GET | Lista de publicaciones pendientes |
| `/admin/publications/:id` | GET | Ver detalle de publicación |
| `/admin/publications/:id/approve` | POST | Aprobar publicación |
| `/admin/publications/:id/reject` | POST | Rechazar publicación |
| `/api/admin/publications` | GET | API JSON de publicaciones |

### Rutas Públicas (Acceso general)

| Ruta | Método | Descripción |
|------|--------|-------------|
| `/login` | GET | Página de login |
| `/register` | GET | Página de registro |
| `/logout` | GET | Cerrar sesión |

---

## 🗄️ Base de Datos

### Tabla `publications`

```sql
CREATE TABLE publications (
  id                BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  user_id           BIGINT NOT NULL,
  title             VARCHAR(200) NOT NULL,
  slug              VARCHAR(250) NOT NULL UNIQUE,
  content           TEXT NOT NULL,
  excerpt           VARCHAR(500),
  cover_image       VARCHAR(500),
  category          VARCHAR(100) NOT NULL,
  tags              VARCHAR(500),
  status            VARCHAR(20) NOT NULL DEFAULT 'draft',
  view_count        INT NOT NULL DEFAULT 0,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at      TIMESTAMP,
  reviewed_by       BIGINT,
  reviewed_at       TIMESTAMP,
  rejection_reason  TEXT,
  
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (reviewed_by) REFERENCES admins(id) ON DELETE SET NULL,
  CHECK (status IN ('draft', 'pending', 'approved', 'rejected'))
);
```

### Estados de Publicación

| Estado | Emoji | Descripción |
|--------|-------|-------------|
| `draft` | 🟡 | Borrador, el usuario puede editar |
| `pending` | 🔵 | En revisión, esperando aprobación |
| `approved` | 🟢 | Aprobada, visible públicamente |
| `rejected` | 🔴 | Rechazada, con razón del rechazo |

---

## 🔒 Seguridad y Permisos

### Usuarios (role: 'user')
- ✅ Pueden crear publicaciones
- ✅ Pueden editar **solo sus propias** publicaciones en estado **draft**
- ✅ Pueden eliminar **solo sus propias** publicaciones en estado **draft**
- ✅ Pueden enviar a revisión sus publicaciones
- ✅ Pueden ver el estado de todas sus publicaciones
- ❌ **NO** pueden editar publicaciones **pending**, **approved** o **rejected**
- ❌ **NO** pueden aprobar ni rechazar publicaciones
- ❌ **NO** pueden ver publicaciones de otros usuarios

### Administradores (role: 'admin')
- ✅ Pueden ver **todas** las publicaciones pendientes
- ✅ Pueden aprobar publicaciones (cambia a **approved**)
- ✅ Pueden rechazar publicaciones (cambia a **rejected** + razón)
- ✅ Pueden ver información del autor de cada publicación
- ❌ **NO** pueden editar el contenido de las publicaciones
- ❌ **NO** pueden crear publicaciones desde el panel admin

---

## 📊 Estadísticas del Dashboard de Usuario

El dashboard de usuario muestra:

### Tarjetas de Estadísticas
```
┌──────────────────┬──────────────────┬──────────────────┬──────────────────┐
│   📝 Borradores  │  🔵 Pendientes   │  ✅ Aprobadas    │  ❌ Rechazadas   │
│        5         │        2         │        8         │        1         │
└──────────────────┴──────────────────┴──────────────────┴──────────────────┘
```

### Tabla de Publicaciones
- **Título** de la publicación
- **Categoría** (Tutorial, Guía, Artículo, etc.)
- **Estado** actual (draft, pending, approved, rejected)
- **Fecha** de creación/última actualización
- **Acciones** disponibles según el estado:
  - Draft: Editar, Vista Previa, Enviar a Revisión, Eliminar
  - Pending: Solo Vista Previa
  - Approved: Ver Publicación Pública
  - Rejected: Ver Razón de Rechazo, Eliminar

---

## 📋 Verificación de Estado

### Comprobar que todo funciona

```bash
# 1. Compilar (debe ser exitoso)
sbt compile

# 2. Ejecutar
sbt run

# 3. Verificar rutas en el navegador
# Usuario:
http://localhost:9000/login
http://localhost:9000/user/dashboard

# Admin:
http://localhost:9000/admin/login
http://localhost:9000/admin/publications/pending
```

### Queries SQL de Verificación

```sql
-- Ver todas las publicaciones con sus autores
SELECT p.id, p.title, p.status, u.username, p.created_at
FROM publications p
JOIN users u ON p.user_id = u.id
ORDER BY p.created_at DESC;

-- Ver publicaciones pendientes
SELECT * FROM publications WHERE status = 'pending';

-- Ver publicaciones aprobadas
SELECT * FROM publications WHERE status = 'approved';

-- Estadísticas de un usuario
SELECT status, COUNT(*) as count
FROM publications
WHERE user_id = 1
GROUP BY status;
```

---

## 🎨 Características de UI

### Dashboard de Usuario
- ✨ Diseño moderno con tarjetas de estadísticas
- 📊 Gráficos visuales del estado de publicaciones
- 🎯 Filtros por estado y categoría
- 🔍 Búsqueda por título
- ⚡ Acciones rápidas con botones de colores

### Panel de Admin
- 📄 Lista de cards con publicaciones pendientes
- 👤 Información del autor visible
- ⏰ Timestamps de envío
- 🎯 Botones de acción destacados
- 💬 Modal para ingresar razón de rechazo

---

## 🔧 Archivos Clave

### Backend
```
app/
├── models/
│   └── Publication.scala              # Modelo con estados
├── repositories/
│   └── PublicationRepository.scala    # Repositorio Slick
├── controllers/
│   ├── UserPublicationController.scala # CRUD de usuario
│   └── AdminController.scala          # Revisión de admin
└── actions/
    └── UserAction.scala               # Seguridad basada en roles
```

### Frontend
```
app/views/
├── user/
│   ├── dashboard.scala.html           # Dashboard del usuario
│   ├── publicationForm.scala.html     # Formulario crear/editar
│   └── publicationPreview.scala.html  # Vista previa
└── admin/
    ├── publicationReview.scala.html   # Lista de pendientes
    └── publicationDetail.scala.html   # Detalle para revisar
```

### Configuración
```
conf/
├── routes                              # Rutas HTTP
└── evolutions/default/
    └── 6.sql                          # Tabla publications
```

---

## ✨ Mejoras Futuras Sugeridas

### Funcionalidad
- [ ] Notificaciones por email cuando se aprueba/rechaza
- [ ] Sistema de comentarios del admin en la revisión
- [ ] Versiones de publicaciones (historial)
- [ ] Categorías personalizables
- [ ] Sistema de tags dinámico
- [ ] Búsqueda avanzada con filtros
- [ ] Exportar publicaciones a PDF

### UI/UX
- [ ] Editor WYSIWYG (TinyMCE, Quill)
- [ ] Preview en tiempo real mientras editas
- [ ] Drag & drop para imágenes
- [ ] Soporte Markdown
- [ ] Dark mode
- [ ] Animaciones de transición de estados

### Admin
- [ ] Dashboard de estadísticas de publicaciones
- [ ] Asignación de revisores
- [ ] Sistema de prioridades
- [ ] Logs de auditoría de aprobaciones
- [ ] Revisión en lote (aprobar múltiples)

### Seguridad
- [ ] Rate limiting en creación de publicaciones
- [ ] Validación de contenido (anti-spam)
- [ ] Sanitización de HTML
- [ ] Límite de publicaciones pendientes por usuario

---

## 🐛 Troubleshooting

### Error: "No puedo ver el botón de Nueva Publicación"
**Solución**: Debes estar logueado como **usuario** (no admin). Ve a `/login` y usa tab "Usuario".

### Error: "No puedo editar mi publicación"
**Causa**: Solo se pueden editar publicaciones en estado **draft**.
**Solución**: Si está en "pending", debes esperar la revisión del admin.

### Error: "No veo publicaciones pendientes en admin"
**Solución**: 
1. Verifica que haya publicaciones con estado "pending"
2. Verifica que estés logueado como **admin**
3. Ve directamente a `/admin/publications/pending`

### Error: "La tabla publications no existe"
**Solución**: 
1. Ve a http://localhost:9000
2. Click en "Apply this script!" para ejecutar evolutions
3. O ejecuta manualmente el SQL de `conf/evolutions/default/6.sql`

---

## 📚 Recursos Adicionales

- **Documentación de Play Framework**: https://www.playframework.com/documentation
- **Slick Documentation**: https://scala-slick.org/doc/
- **Play Slick**: https://www.playframework.com/documentation/latest/PlaySlick

---

## ✅ Checklist de Funcionalidad

- [x] Usuario puede crear publicaciones
- [x] Usuario puede editar borradores
- [x] Usuario puede enviar a revisión
- [x] Usuario puede ver estado de sus publicaciones
- [x] Usuario puede eliminar borradores
- [x] Admin puede ver lista de pendientes
- [x] Admin puede aprobar publicaciones
- [x] Admin puede rechazar con razón
- [x] Dashboard muestra estadísticas
- [x] Sistema de estados funciona correctamente
- [x] Seguridad basada en roles
- [x] Base de datos con Slick
- [x] UI responsive y moderna
- [x] Validación de formularios
- [x] Mensajes flash de confirmación

---

## 🎉 ¡Sistema Listo para Usar!

El sistema de publicaciones con aprobación de administradores está **completamente funcional** y listo para producción. Solo necesitas:

1. ✅ Iniciar el servidor: `sbt run`
2. ✅ Crear usuarios desde `/register`
3. ✅ Usar admin existente: `admin` / `admin123`
4. ✅ ¡Empezar a crear y aprobar contenido!

**¡Disfruta tu nuevo sistema de gestión de contenido!** 🚀
