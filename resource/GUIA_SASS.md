# 🎨 Guía de Integración SASS/SCSS

## ✅ Configuración Completada

Se ha integrado completamente SASS/SCSS en tu proyecto Play Framework con una arquitectura modular y profesional.

---

## 📁 Estructura de Archivos

```
app/assets/stylesheets/
├── main.scss                 # Archivo principal (punto de entrada)
├── _variables.scss           # Variables globales
├── _mixins.scss             # Mixins reutilizables
├── _base.scss               # Estilos base y reset
└── components/
    ├── _navbar.scss         # Componente de navegación
    ├── _buttons.scss        # Estilos de botones
    ├── _forms.scss          # Estilos de formularios
    ├── _cards.scss          # Tarjetas
    └── _alerts.scss         # Alertas/Mensajes
```

---

## 🔧 Configuración Técnica

### 1. **Plugin Instalado**
- `sbt-sass` 2.0.0 en `project/plugins.sbt`
- `SbtWeb` habilitado en `build.sbt`

### 2. **Compilación Automática**
SASS se compila automáticamente cuando:
- Ejecutas `sbt run`
- Guardas cambios en archivos `.scss`
- El CSS compilado se genera en `target/web/public/main/stylesheets/`

### 3. **Archivo de Salida**
- **Entrada**: `app/assets/stylesheets/main.scss`
- **Salida**: `target/web/public/main/stylesheets/main.css`
- **Ruta en producción**: `/assets/stylesheets/main.css`

---

## 🎨 Características Implementadas

### **Variables Globales** (`_variables.scss`)
```scss
// Colores
$primary-color: #6366f1;
$secondary-color: #ec4899;
$success-color: #10b981;

// Espaciado
$spacing-sm: 0.5rem;
$spacing-md: 1rem;
$spacing-lg: 1.5rem;

// Breakpoints
$breakpoint-md: 768px;
$breakpoint-lg: 1024px;
```

### **Mixins Reutilizables** (`_mixins.scss`)
```scss
// Responsive
@include respond-to('md') {
  // Estilos para tablets
}

// Flexbox
@include flex-center;
@include flex-between;

// Botones
@include button-variant($primary-color);

// Cards
@include card($spacing-lg);

// Inputs
@include input-base;
```

### **Componentes Modulares**
Cada componente está en su propio archivo para mejor organización:
- `_navbar.scss` - Barra de navegación
- `_buttons.scss` - Sistema de botones
- `_forms.scss` - Formularios y inputs
- `_cards.scss` - Tarjetas
- `_alerts.scss` - Mensajes de alerta

---

## 🚀 Cómo Usar

### **1. Actualizar las Vistas**
Cambia las referencias de CSS en tus templates Twirl:

**Antes:**
```html
<link rel="stylesheet" href="@routes.Assets.versioned("stylesheets/main.css")">
```

**Después:**
```html
<link rel="stylesheet" href="@routes.Assets.versioned("stylesheets/main.css")">
```
*Nota: La ruta sigue siendo la misma, pero ahora apunta al CSS compilado desde SCSS*

### **2. Compilar SASS**
```bash
# Desarrollo (auto-recompila al guardar)
sbt run

# Solo compilar assets
sbt assets

# Producción (minificado)
sbt stage
```

### **3. Crear Nuevos Estilos**

#### Opción A: Agregar a un archivo existente
```scss
// app/assets/stylesheets/components/_buttons.scss
.btn-custom {
  @include button-variant(#ff6b6b);
  border-radius: 50px;
}
```

#### Opción B: Crear nuevo componente
```scss
// app/assets/stylesheets/components/_modal.scss
.modal {
  @include absolute-center;
  background: var(--bg-white);
  border-radius: $border-radius-lg;
  padding: $spacing-xl;
  box-shadow: $shadow-xl;
}
```

Luego importarlo en `main.scss`:
```scss
@import 'components/modal';
```

---

## 📚 Ejemplos de Uso

### **1. Botones**
```html
<button class="btn btn-primary">Primario</button>
<button class="btn btn-secondary">Secundario</button>
<button class="btn btn-success btn-lg">Grande</button>
<button class="btn btn-outline">Outlined</button>
```

### **2. Formularios**
```html
<div class="form-group">
  <label>Email</label>
  <input type="email" placeholder="tu@email.com">
  <span class="help-text">Nunca compartiremos tu email</span>
</div>
```

### **3. Cards**
```html
<div class="card card-primary">
  <div class="card-header">
    <h3>Título</h3>
  </div>
  <div class="card-body">
    Contenido...
  </div>
</div>
```

### **4. Alerts**
```html
<div class="alert alert-success">
  ✓ Operación exitosa
</div>
<div class="alert alert-error">
  ⚠ Error encontrado
</div>
```

### **5. Grid de Cards**
```html
<div class="card-grid">
  <div class="card">Card 1</div>
  <div class="card">Card 2</div>
  <div class="card">Card 3</div>
</div>
```

---

## 🎯 Ventajas de SASS

### **1. Variables**
```scss
$primary: #6366f1;

.button { background: $primary; }
.link { color: $primary; }
```

### **2. Anidamiento**
```scss
.navbar {
  background: white;
  
  &-menu {
    display: flex;
    
    &-item {
      padding: 1rem;
      
      &:hover {
        color: blue;
      }
    }
  }
}
```

### **3. Mixins**
```scss
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  @include flex-center;
}
```

### **4. Funciones**
```scss
.button {
  background: darken($primary, 10%);
  border: 1px solid lighten($primary, 20%);
}
```

### **5. Importación Modular**
```scss
@import 'variables';
@import 'mixins';
@import 'components/navbar';
```

---

## 🔥 Mejores Prácticas

### **1. Nomenclatura**
- Archivos parciales empiezan con `_`: `_variables.scss`
- Usa nombres descriptivos: `_navbar.scss`, no `_nav.scss`
- CamelCase para clases: `.btnPrimary` o kebab-case: `.btn-primary`

### **2. Organización**
```
assets/stylesheets/
├── main.scss              # Importa todo
├── _variables.scss        # Variables globales
├── _mixins.scss          # Funciones reutilizables
├── _base.scss            # Reset y base
├── components/           # Un archivo por componente
├── layout/               # Layout (header, footer, etc.)
└── pages/                # Estilos específicos de páginas
```

### **3. Variables Semánticas**
```scss
// ❌ Mal
$blue: #6366f1;
$red: #ef4444;

// ✅ Bien
$primary-color: #6366f1;
$error-color: #ef4444;
```

### **4. Mixins para Repetición**
```scss
// En lugar de repetir código
@mixin card-hover {
  transition: transform 0.3s;
  &:hover {
    transform: translateY(-4px);
  }
}
```

### **5. Usar CSS Variables para Temas**
```scss
:root {
  --primary: #{$primary-color};
}

.button {
  background: var(--primary); // Permite cambios dinámicos
}
```

---

## 🐛 Solución de Problemas

### **Problema: Los estilos no se aplican**
```bash
# Limpiar cache y recompilar
sbt clean
sbt compile
sbt run
```

### **Problema: Error de compilación SASS**
- Verifica la sintaxis en los archivos `.scss`
- Asegúrate de que todos los `@import` apunten a archivos existentes
- Revisa que las variables estén definidas antes de usarlas

### **Problema: CSS no se actualiza**
- Refresca el navegador con `Ctrl+F5` (hard refresh)
- Verifica que el servidor esté corriendo con `sbt run`
- Limpia el directorio `target`: `sbt clean`

---

## 📖 Recursos Adicionales

- **Documentación SASS**: https://sass-lang.com/documentation
- **sbt-sass Plugin**: https://github.com/irundaia/sbt-sass
- **Play Framework Assets**: https://www.playframework.com/documentation/latest/Assets

---

## ✨ Próximos Pasos

1. **Migrar CSS existente**: Mover estilos de `public/stylesheets/main.css` a los archivos SCSS modulares
2. **Crear más componentes**: Agregar `_tables.scss`, `_modals.scss`, etc.
3. **Optimizar para producción**: Configurar minificación y source maps
4. **Documentar componentes**: Crear una guía de estilos (Style Guide)

---

## 🎉 ¡SASS está listo para usar!

Ahora puedes escribir estilos más mantenibles y escalables con:
- ✅ Variables reutilizables
- ✅ Mixins para evitar repetición
- ✅ Anidamiento intuitivo
- ✅ Importación modular
- ✅ Compilación automática

**Ejecuta `sbt run` y comienza a desarrollar con SASS!** 🚀
