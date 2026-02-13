# 🎯 ARQUITECTURA DEL SISTEMA DE APROBACIÓN DE PUBLICACIONES

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REACTIVE MANIFESTO PLATFORM                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────────────┐          ┌──────────────────────────┐        │
│  │   USUARIO COMÚN          │          │   ADMINISTRADOR          │        │
│  ├──────────────────────────┤          ├──────────────────────────┤        │
│  │                          │          │                          │        │
│  │  LOGIN                   │          │  LOGIN                   │        │
│  │  ↓                       │          │  ↓                       │        │
│  │  /dashboard              │          │  /admin/dashboard         │        │
│  │  ├─ Mis Publicaciones    │          │  ├─ Estadísticas         │        │
│  │  ├─ Crear Nueva          │          │  ├─ Contactos            │        │
│  │  └─ Mis Datos           │          │  └─ Publicaciones        │        │
│  │                          │          │                          │        │
│  │  PUBLICAR                │          │  REVISAR                 │        │
│  │  ↓                       │          │  ↓                       │        │
│  │  1. Crear (DRAFT)        │          │  /admin/publications     │        │
│  │  2. Editar               │          │  /pending                │        │
│  │  3. Enviar a Revisión    │          │  ├─ Aprobar              │        │
│  │     (PENDING)            │          │  ├─ Rechazar             │        │
│  │                          │          │  └─ Ver detalles         │        │
│  │  RECIBIR RESPUESTA       │          │                          │        │
│  │  ↓                       │          │  GESTIONAR               │        │
│  │  Dashboard actualizado:  │          │  ↓                       │        │
│  │  ├─ APROBADA ✓           │          │  Publicaciones:          │        │
│  │  │  → Publicada          │          │  ├─ En revisión (N)      │        │
│  │  └─ RECHAZADA ✗          │          │  ├─ Aprobadas (M)        │        │
│  │     → Ver motivo         │          │  └─ Rechazadas (K)       │        │
│  │     → Editar y reenviar  │          │                          │        │
│  │                          │          │                          │        │
│  └──────────────────────────┘          └──────────────────────────┘        │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                            DATABASE (H2)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  publications TABLE                                                       │
│  ┌────────┬──────┬────────┬────────────┬──────────┬─────────────┬──────┐  │
│  │ id     │userId│ title  │   status   │ reviewer │ reject_reason│ ... │  │
│  ├────────┼──────┼────────┼────────────┼──────────┼─────────────┼──────┤  │
│  │ 1      │ 42   │ Scala  │ approved   │ 1 (admin)│ NULL        │ ... │  │
│  │ 2      │ 43   │ Akka   │ rejected   │ 1 (admin)│ "Más detalles│ ... │  │
│  │ 3      │ 44   │ Play   │ pending    │ NULL     │ NULL        │ ... │  │
│  │ 4      │ 42   │ Cats   │ draft      │ NULL     │ NULL        │ ... │  │
│  └────────┴──────┴────────┴────────────┴──────────┴─────────────┴──────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Estados y Transiciones Detalladas

```
USUARIO FLOW:

1. CREAR
   ┌────────────────────┐
   │  /user/publications│
   │      /new          │
   ├────────────────────┤
   │ Completa formulario│
   │ Guarda como DRAFT  │
   └────────┬───────────┘
            │
            ├─→ Editar (vuelve a DRAFT)
            │
            └─→ Enviar a Revisión
               └─→ ESTADO: PENDING

2. PENDIENTE (esperando admin)
   ┌────────────────────┐
   │   En cola del      │
   │   administrador    │
   └────────┬───────────┘
            │
            └─→ Admin revisa
               └─→ ¿Aprobado?
                  ├─→ SÍ → APPROVED (publicada)
                  └─→ NO → REJECTED (con motivo)

3. APROBADA
   ┌────────────────────┐
   │ Publicada en sitio │
   │ Visible en /pubs   │
   │ Contador de vistas │
   └────────────────────┘

4. RECHAZADA
   ┌────────────────────┐
   │ Ver motivo rechazo │
   │ Editar contenido   │
   │ Reenviar a revisión│
   └────────┬───────────┘
            │
            └─→ Vuelve a PENDING
               (flujo 2)


ADMIN FLOW:

1. VER PENDIENTES
   ┌────────────────────┐
   │ /admin/publications│
   │    /pending        │
   ├────────────────────┤
   │ Grid de pubs       │
   │ PENDING            │
   └────────┬───────────┘
            │
            ├─→ Ver Completa
            │   └─→ /admin/publications/:id
            │
            ├─→ Aprobar ✓
            │   └─→ APROBADA (publicada)
            │
            └─→ Rechazar ✗
               ├─→ Modal: Ingresa razón
               └─→ RECHAZADA (usuario notificado)

2. PUBLICACIONES APROBADAS
   ┌────────────────────┐
   │ Visible públicamente│
   │ /publicaciones/:slug
   │ Contador de vistas │
   │ Sin acción admin   │
   └────────────────────┘
```

## Endpoints Mapeados

```
┌──────────────────────────────────────────┐
│         USUARIO ENDPOINTS                │
├──────────────────────────────────────────┤
│                                          │
│ GET  /user/dashboard                 ✓  │
│      → Ver publicaciones + estadísticas  │
│                                          │
│ GET  /user/publications/new          ✓  │
│      → Formulario crear                  │
│                                          │
│ POST /user/publications/new          ✓  │
│      → Guardar como DRAFT                │
│                                          │
│ GET  /user/publications/:id/edit     ✓  │
│      → Editar si es DRAFT o REJECTED     │
│                                          │
│ POST /user/publications/:id/edit     ✓  │
│      → Guardar cambios                   │
│                                          │
│ POST /user/publications/:id/submit   ✓  │
│      → Enviar a revisión (DRAFT→PENDING) │
│                                          │
│ POST /user/publications/:id/delete   ✓  │
│      → Eliminar (solo DRAFT)             │
│                                          │
│ GET  /user/publications/:id          ✓  │
│      → Ver publicación                   │
│                                          │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│         ADMIN ENDPOINTS                  │
├──────────────────────────────────────────┤
│                                          │
│ GET  /admin/publications/pending     ✓  │
│      → Ver todas PENDING                 │
│                                          │
│ GET  /admin/publications/:id         ✓  │
│      → Ver detalle para revisar          │
│                                          │
│ POST /admin/publications/:id/approve ✓  │
│      → PENDING → APPROVED                │
│                                          │
│ POST /admin/publications/:id/reject  ✓  │
│      → PENDING → REJECTED + motivo       │
│                                          │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│        PÚBLICO ENDPOINTS                 │
├──────────────────────────────────────────┤
│                                          │
│ GET  /publicaciones                  ✓  │
│      → Ver todas APPROVED                │
│                                          │
│ GET  /publicaciones/:slug            ✓  │
│      → Ver detalle (solo APPROVED)       │
│                                          │
└──────────────────────────────────────────┘
```

## Modelos de Datos

```
Publication
├─ id: Long (PK)
├─ userId: Long (FK → User)
├─ title: String (250 chars max)
├─ slug: String (auto-generated)
├─ content: String (HTML)
├─ excerpt: String (500 chars, opcional)
├─ coverImage: String (URL, opcional)
├─ category: String
├─ tags: String (CSV: "scala,akka,reactive")
├─ status: String enum
│  ├─ "draft"    → Borrador local
│  ├─ "pending"  → Esperando admin
│  ├─ "approved" → Publicada
│  └─ "rejected" → Rechazada
├─ viewCount: Int (contador de vistas)
├─ createdAt: Timestamp
├─ updatedAt: Timestamp
├─ publishedAt: Timestamp (cuando se aprobó)
├─ reviewedBy: Long (admin ID que revisó)
├─ reviewedAt: Timestamp (cuándo se revisó)
└─ rejectionReason: String (motivo si rechazada)
```

## Seguridad y Validaciones

```
PERMISOS:
├─ Usuario solo ve sus publicaciones
├─ Usuario NO puede editar publicación ajena
├─ Admin puede ver todas las PENDING
├─ Admin NO puede editar contenido
└─ Solo AuthAction/AdminOnlyAction protegen

VALIDACIONES:
├─ Título: 5-200 caracteres
├─ Contenido: mín. 50 caracteres
├─ Categoría: requerida
├─ Slug: auto-generado (único con timestamp)
├─ Status: debe ser uno de 4 valores
└─ Rechazo: motivo requerido si se rechaza
```

## Flujo de Base de Datos

```
Crear Publicación:
User → (POST /user/publications/new) 
     → PublicationFormData 
     → generateSlug(title) 
     → Publication(status="draft")
     → publicationRepository.create()
     → INSERT INTO publications
     → Retorna publicación con ID

Enviar a Revisión:
User → (POST /user/publications/:id/submit)
     → Fetch Publication by ID
     → Verificar permisos (userId == request.userId)
     → UPDATE publications SET status='pending', updatedAt=NOW()
     → Redirige a dashboard

Aprobar (Admin):
Admin → (POST /admin/publications/:id/approve)
      → Fetch Publication by ID
      → UPDATE publications 
        SET status='approved', 
            publishedAt=NOW(), 
            reviewedBy=adminId,
            reviewedAt=NOW()
      → Redirige a /pending

Rechazar (Admin):
Admin → (POST /admin/publications/:id/reject + rejectionReason)
      → Fetch Publication by ID
      → UPDATE publications 
        SET status='rejected',
            rejectionReason=?,
            reviewedBy=adminId,
            reviewedAt=NOW()
      → Redirige a /pending
```

## Interfaz de Usuario

```
USUARIO DASHBOARD:
┌─────────────────────────────────────────┐
│  📝 Mis Publicaciones                   │
├─────────────────────────────────────────┤
│  [✍️ Crear Nueva]                      │
│                                         │
│  Borradores: 2  Pendientes: 1  ✓: 3    │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  Título      │ Cat    │ Estado  │  │
│  ├─────────────────────────────────┤  │
│  │ Scala Guide  │ Tech   │ ✓Aprobada│ │
│  │ Akka Basics  │ Tech   │ 📋Pending │ │
│  │ Play 2       │ Web    │ 📝Borrador│ │
│  │              │ [Acciones] │       │
│  └─────────────────────────────────┘  │
└─────────────────────────────────────────┘

ADMIN PANEL:
┌─────────────────────────────────────────┐
│  🔍 Publicaciones Pendientes (3)        │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  📄 Scala Guide                 │  │
│  │  👤 John Doe                    │  │
│  │  📅 2026-02-12                  │  │
│  │  🏷️  #scala #akka               │  │
│  │                                 │  │
│  │  [👁️ Ver] [✓OK] [✗ RECHAZAR]   │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  📄 Akka Basics                 │  │
│  │  ...                            │  │
│  └─────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

**Estado: ✅ TOTALMENTE IMPLEMENTADO Y FUNCIONAL**
