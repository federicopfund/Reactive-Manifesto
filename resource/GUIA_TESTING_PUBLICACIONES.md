# 🧪 CHECKLIST DE TESTING: Sistema de Aprobación de Publicaciones

## Requisitos Previos
- [ ] Admin creado con credenciales válidas
- [ ] Usuario común registrado y verificado
- [ ] App corriendo en `http://localhost:9000`

---

## 📝 ESCENARIO 1: Usuario Crea Publicación

### Paso 1: Acceso como Usuario
- [ ] Ve a `http://localhost:9000/auth/login`
- [ ] Selecciona pestaña "👤 Usuario"
- [ ] Ingresa credenciales
- [ ] Se redirige a `/dashboard`

### Paso 2: Crear Nueva Publicación
- [ ] Click en botón "✍️ Crear Nueva Publicación"
- [ ] Completa formulario:
  - [ ] Título: "Mi Primer Artículo sobre Scala" (mínimo 5 caracteres)
  - [ ] Contenido: "Lorem ipsum dolor sit amet..." (mínimo 50 caracteres)
  - [ ] Categoría: Selecciona una
  - [ ] Tags: "scala,akka,reactive" (opcional)
  - [ ] Resumen: Escribe un preview (opcional)
  - [ ] Imagen: URL (opcional)
- [ ] Click "Guardar como Borrador"
- [ ] Se redirige a editar
- [ ] Estado mostrado: "Borrador"

### Paso 3: Publicación en Dashboard
- [ ] Ve a `/user/dashboard`
- [ ] Publicación aparece en tabla
- [ ] Estado: "Borrador" (badge gris)
- [ ] Estadísticas: "Borradores: 1"

---

## 📤 ESCENARIO 2: Usuario Envía a Revisión

### Paso 1: Enviar para Revisión
- [ ] En dashboard, click en publicación creada
- [ ] Click en botón "📤 Enviar a Revisión"
- [ ] Flash message: "Publicación enviada para revisión" (verde)
- [ ] Estado cambia a: "Pendiente" (badge amarillo)

### Paso 2: Verificar Estado
- [ ] Estadísticas actualizadas:
  - [ ] "Borradores: 0"
  - [ ] "Pendientes: 1"
- [ ] Botones cambiar:
  - [ ] ✏️ Editar: desaparece
  - [ ] 📤 Enviar: desaparece
  - [ ] Solo queda "👁️ Ver"

---

## ✅ ESCENARIO 3: Admin Aprueba Publicación

### Paso 1: Login como Admin
- [ ] Ve a `http://localhost:9000/auth/login`
- [ ] Selecciona pestaña "🛡️ Administrador"
- [ ] Ingresa: usuario="federico", contraseña="Fede/(40021)"
- [ ] Se redirige a `/admin/dashboard`

### Paso 2: Panel de Publicaciones Pendientes
- [ ] Click en "📚 Publicaciones Pendientes" (o accede a `/admin/publications/pending`)
- [ ] Nueva publicación aparece en grid
- [ ] Muestra: Título, Autor, Categoría, Tags

### Paso 3: Revisar Publicación
- [ ] Click en botón "👁️ Ver Completa"
- [ ] Ve contenido completo
- [ ] Regresa al panel de pendientes

### Paso 4: Aprobar
- [ ] Click en botón "✓ Aprobar"
- [ ] Flash message: "Publicación aprobada exitosamente" (verde)
- [ ] Publ icación desaparece del panel (estado cambiado)
- [ ] Redirige a `/admin/publications/pending`

### Paso 5: Verificar en Usuario
- [ ] Como usuario: va a `/user/dashboard`
- [ ] Publicación ahora aparece con estado "Aprobada" (badge verde)
- [ ] Estadísticas: "Aprobadas: 1"

---

## ❌ ESCENARIO 4: Admin Rechaza Publicación

### Paso 1: Nueva Publicación de Usuario
- [ ] Usuario crea otra publicación
- [ ] Usuario envía a revisión (está en PENDING)

### Paso 2: Admin Rechaza
- [ ] Admin accede a `/admin/publications/pending`
- [ ] Click en botón "✗ Rechazar"
- [ ] Modal aparece: "Rechazar Publicación"
- [ ] Ingresa motivo: "El contenido necesita más detalle técnico"
- [ ] Click "Confirmar Rechazo"
- [ ] Flash message: "Publicación rechazada" (verde)
- [ ] Publ icación desaparece del panel

### Paso 3: Usuario ve Rechazo
- [ ] Usuario en `/user/dashboard`
- [ ] Publicación ahora está "Rechazada" (badge roja)
- [ ] Botón "ℹ️ Razón" disponible
- [ ] Al hacer click, ve motivo: "El contenido necesita más detalle técnico"

### Paso 4: Usuario Re-envía
- [ ] Click en botón "✏️ Editar"
- [ ] Modifica contenido
- [ ] Click "Actualizar Publicación"
- [ ] Estado vuelve a "Borrador"
- [ ] Click "📤 Enviar a Revisión"
- [ ] Estado: "Pendiente"

---

## 📊 ESCENARIO 5: Estadísticas en Dashboard

### Usuario
- [ ] Borradores: número correcto
- [ ] Pendientes: número correcto
- [ ] Aprobadas: número correcto
- [ ] Rechazadas: número correcto

### Admin
- [ ] Panel de pendientes muestra todas las publicaciones PENDING
- [ ] Count es preciso

---

## 🚨 CASOS LÍMITE A PROBAR

### Permisos
- [ ] Usuario NO puede editar publicación de otro usuario
- [ ] Usuario NO puede eliminar publicación aprobada
- [ ] Admin puede ver todas las publicaciones pendientes
- [ ] Admin NO puede editar publicaciones de usuarios

### Validaciones
- [ ] Título vacío: formulario no valida
- [ ] Contenido <50 caracteres: error
- [ ] Categoría vacía: error
- [ ] Tags con caracteres especiales: se guarda ok

### Edge Cases
- [ ] Título con caracteres especiales: slug generado ok
- [ ] Contenido con HTML: se guarda ok
- [ ] Imagen con URL inválida: se guarda ok (puede no cargar imagen)
- [ ] Muchas publicaciones: tabla scrollea ok

---

## ✨ PRUEBA DE UI/UX

### Dashboard Usuario
- [ ] Tabla es responsive en mobile
- [ ] Colores de estado claros
- [ ] Botones accesibles
- [ ] Flash messages desaparecen automáticamente

### Panel Admin
- [ ] Grid de publicaciones responsivo
- [ ] Modal de rechazo se cierra correctamente
- [ ] Estados visuales claros

---

## 🐛 DEBUGGING (si hay problemas)

### Si publicación no aparece pendiente:
```bash
# Ver logs
curl http://localhost:9000/setup/list-admins | jq .
curl http://localhost:9000/api/user/publications | jq .
```

### Si botón de envío no funciona:
- [ ] Verificar que el formulario tiene CSRF token
- [ ] Ver consola del navegador (Dev Tools)
- [ ] Verificar rutas en `conf/routes`

### Si modal de rechazo no funciona:
- [ ] Abrir consola (F12)
- [ ] Verificar errores de JavaScript
- [ ] Verificar que el formulario POST va a endpoint correcto

---

## 📋 CHECKLIST FINAL

- [ ] Usuario puede crear publicación
- [ ] Usuario puede enviar a revisión
- [ ] Admin ve publicaciones pendientes
- [ ] Admin puede aprobar
- [ ] Admin puede rechazar con motivo
- [ ] Usuario ve estados actualizados
- [ ] Filtro de estados funciona
- [ ] Permisos se respetan
- [ ] Validaciones funcionan
- [ ] UI es clara y responsive

---

## 🎉 RESULTADO ESPERADO

Al completar todos estos tests, el sistema de aprobación de publicaciones debe estar:
- ✅ Funcionando correctamente
- ✅ Seguro (con permisos)
- ✅ Intuitivo (UI clara)
- ✅ Rápido (sin errores)
- ✅ Validación de datos correcta

**Resultado: APROBADO ✓**
