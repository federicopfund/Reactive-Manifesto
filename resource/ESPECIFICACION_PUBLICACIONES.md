# Especificación: Sistema de Aprobación de Publicaciones

## Estado Actual
✅ **Ya implementado:**
- Modelo Publication con estados (draft, pending, approved, rejected)
- Endpoints de admin para aprobación/rechazo
- Vistas básicas de admin y usuario
- Controladores para el flujo completo

## Flujo de Publicaciones

### 1. USUARIO COMÚN
- **Dashboard** (`/dashboard`): Ver todas sus publicaciones con filtros por estado
  - Borradores (draft) - propias, no publicadas
  - Pendientes (pending) - enviadas para revisión, esperando admin
  - Aprobadas (approved) - publicadas públicamente
  - Rechazadas (rejected) - rechazadas con motivo
  
- **Crear Publicación** (`/user/publications/new`)
  - Formulario para crear nueva publicación
  - Se guarda como DRAFT por defecto
  
- **Editar Borrador** (`/user/publications/:id/edit`)
  - Solo puede editar borradores y rechazadas
  
- **Enviar para Revisión** (`POST /user/publications/:id/submit`)
  - Cambia estado de DRAFT → PENDING
  - Admin recibe notificación

### 2. ADMIN
- **Panel de Publicaciones Pendientes** (`/admin/publications/pending`)
  - Lista todas las publicaciones con estado PENDING
  - Muestra: Título, Autor, Fecha, Vista previa
  - Botones: Ver Detalle, Aprobar, Rechazar

- **Detalle de Revisión** (`/admin/publications/:id`)
  - Contenido completo de la publicación
  - Botones: APROBAR o RECHAZAR
  - Si rechaza: campo para motivo de rechazo

- **Aprobar** (`POST /admin/publications/:id/approve`)
  - Cambia PENDING → APPROVED
  - Publica automáticamente
  - Notifica usuario

- **Rechazar** (`POST /admin/publications/:id/reject`)
  - Cambia PENDING → REJECTED
  - Guarda motivo del rechazo
  - Usuario puede editar y reenviar

## Campos del Modelo Publication

```
id: Long
userId: Long (quién creó)
title: String
slug: String (URL-friendly)
content: String (HTML)
excerpt: String (preview)
coverImage: String (URL)
category: String
tags: String (CSV: "scala,akka")
status: String (draft/pending/approved/rejected)
viewCount: Int
createdAt: Instant
updatedAt: Instant
publishedAt: Option[Instant] (cuando se aprobó)
reviewedBy: Option[Long] (admin ID que revisó)
reviewedAt: Option[Instant] (cuándo se revisó)
rejectionReason: Option[String] (por qué se rechazó)
```

## Estados y Transiciones

```
DRAFT (usuario crea)
  ↓ (usuario envía para revisión)
PENDING
  ├→ APPROVED (admin aprueba)
  │   → Visible públicamente
  │   → Publicación NOW
  └→ REJECTED (admin rechaza)
      → Usuario recibe razón
      → Puede editar → DRAFT → reenviar
```

## Mejoras Pendientes por Implementar

1. ✅ Rutas configuradas en conf/routes
2. ✅ Controladores con métodos
3. ⚠️ Vistas de admin (publicationReview.scala.html)
4. ⚠️ Vistas de usuario (dashboard.scala.html - agregar botones de envío)
5. ⚠️ Filtrado por estado en dashboard de usuario
6. ⚠️ Indicadores visuales de estado
7. ⚠️ Notificaciones al usuario sobre aprobación/rechazo
8. ⚠️ Endpoint para obtener publicaciones aprobadas (públicas)

## URLs Principales

### Usuario
- GET `/user/dashboard` - Ver publicaciones
- GET `/user/publications/new` - Formulario crear
- POST `/user/publications/new` - Crear publicación
- GET `/user/publications/:id/edit` - Editar
- POST `/user/publications/:id/edit` - Guardar cambios
- POST `/user/publications/:id/submit` - Enviar para revisión
- POST `/user/publications/:id/delete` - Eliminar

### Admin
- GET `/admin/publications/pending` - Ver pendientes
- GET `/admin/publications/:id` - Ver detalle
- POST `/admin/publications/:id/approve` - Aprobar
- POST `/admin/publications/:id/reject` - Rechazar

### Público
- GET `/publicaciones` - Ver todas las aprobadas
- GET `/publicaciones/:slug` - Ver una publicación
