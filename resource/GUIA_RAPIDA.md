# 📚 GUÍA RÁPIDA: Sistema de Publicaciones Aprobadas

## En 10 Segundos

```
USUARIO → Crea artículo (DRAFT) 
       → Envía a revisar (PENDING) 
       → Admin aprueba/rechaza
       → Resultado visible en dashboard
```

---

## URLs Principales

### Para Usuario
```
Crear:               http://localhost:9000/user/publications/new
Mi Dashboard:        http://localhost:9000/user/dashboard
Ver mis artículos:   http://localhost:9000/user/publications/:id
```

### Para Admin
```
Revisar Pendientes:  http://localhost:9000/admin/publications/pending
Ver Detalle:         http://localhost:9000/admin/publications/:id
Aprobar artículo:    POST a /admin/publications/:id/approve
Rechazar artículo:   POST a /admin/publications/:id/reject
```

---

## Estados de Publicación

```
📝 DRAFT      → Usuario redactando (privado)
⏳ PENDING    → Esperando admin (en cola)
✅ APPROVED   → Publicada (visible)
❌ REJECTED   → Rechazada (con motivo)
```

---

## Acciones por Estado

```
DRAFT
├─ Editar      ✓
├─ Enviar      ✓
├─ Eliminar    ✓
└─ Ver         ✓ (preview)

PENDING
└─ Ver        ✓ (solo lectura, esperando)

APPROVED
└─ Ver        ✓ (publicada públicamente)

REJECTED
├─ Editar     ✓
├─ Enviar     ✓ (vuelve a PENDING)
├─ Ver Razón  ✓
└─ Eliminar   ✗ (no, editar y reenviar)
```

---

## Botones en Interfaz

### Usuario - Dashboard

| Estado | Botones |
|--------|---------|
| DRAFT | ✏️ Editar \| 📤 Enviar \| 🗑️ Eliminar \| 👁️ Ver |
| PENDING | 👁️ Ver |
| APPROVED | 👁️ Ver |
| REJECTED | ✏️ Editar \| ℹ️ Razón \| 👁️ Ver |

### Admin - Panel

| Panel | Botones |
|-------|---------|
| Ver Pendientes | 👁️ Ver Detalle \| ✓ Aprobar \| ✗ Rechazar |
| Ver Detalle | ✓ Aprobar \| ✗ Rechazar |

---

## Flujo Visual Simple

```
┌─────────────────────────────────────────┐
│  USUARIO: Escribe artículo              │
└──────────────┬──────────────────────────┘
               │ Guardar como borrador
               ▼
┌─────────────────────────────────────────┐
│  ESTADO: DRAFT (gris)                   │
│  En dashboard solo para usuario         │
└──────────────┬──────────────────────────┘
               │ Click "Enviar a Revisión"
               ▼
┌─────────────────────────────────────────┐
│  ESTADO: PENDING (amarillo)             │
│  En cola del administrador              │
└──────────────┬──────────────────────────┘
               │    
               ├─ Admin aprueba ──────────┐
               │                          │
               └─ Admin rechaza ──┐       │
                                  │       │
                    ┌─────────────▼───────▼──┐
                    │                        │
                    ▼                        ▼
        ┌──────────────────────┐  ┌─────────────────┐
        │ ESTADO: APPROVED ✅  │  │ ESTADO: REJECTED │
        │ Publicada            │  │ Usuario ve razón │
        │ Visible en sitio     │  │ Puede editar    │
        │ Contador de vistas   │  │ Reenviar        │
        └──────────────────────┘  └────────┬────────┘
                                           │ Edita y reenvía
                                           │
                                           └──────→ PENDING
```

---

## Test Rápido (3 pasos)

### 1️⃣ Usuario: Crear

```bash
GET http://localhost:9000/user/publications/new
→ Completa formulario
→ Click "Guardar"
→ Status: DRAFT ✓
```

### 2️⃣ Usuario: Enviar

```bash
GET http://localhost:9000/user/dashboard
→ Click "📤 Enviar a Revisión"
→ Status: PENDING ✓
```

### 3️⃣ Admin: Aprobar

```bash
GET http://localhost:9000/admin/publications/pending
→ Click "✓ Aprobar"
→ (Optional: rechazar con motivo)
→ Status: APPROVED/REJECTED ✓
```

---

## Información de Admin (para testing)

```
Usuario:     federico
Contraseña:  Fede/(40021)
Endpoint:    http://localhost:9000/auth/login → Pestaña Administrador
```

---

## Tabla Rápida: Quién ve Qué

```
ESTADO    | Usuario | Admin | Público
----------|---------|-------|----------
DRAFT     | ✓ Solo  | -     | -
PENDING   | ✓ Solo  | ✓     | -
APPROVED  | ✓       | ✓     | ✓
REJECTED  | ✓ Solo  | ✓     | -
```

---

## Mensajes Esperados

```
✓ "Publicación creada exitosamente como borrador"
✓ "Publicación enviada para revisión"
✓ "Publicación aprobada exitosamente"
✓ "Publicación rechazada"
✗ "No tienes permiso para editar esta publicación"
✗ "Credenciales inválidas"
```

---

## Errores Comunes

| Problema | Solución |
|----------|----------|
| No veo botón "Enviar" | Asegúrate que state sea DRAFT |
| Admin no ve pendientes | Asegúrate admin esté loguead |
| No puedo editar | Solo puedes editar DRAFT o REJECTED |
| Motivo no se guarda | Usa modal, no intentes directamente |

---

## Checklist Final

- [ ] Usuario puede crear publicación
- [ ] Estado cambia a DRAFT
- [ ] Usuario puede enviar a revisión
- [ ] Estado cambia a PENDING
- [ ] Admin ve en panel
- [ ] Admin puede aprobar
- [ ] Usuario ve APROBADA
- [ ] Admin puede rechazar
- [ ] Usuario ve RECHAZADA + motivo
- [ ] Usuario puede editar y reenviar

---

**Si todo está ✓ → SISTEMA LISTO PARA USAR** 🎉

Para más detalles consulta `/resource/GUIA_TESTING_PUBLICACIONES.md`
