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

![Mobile View](https://github.com/user-attachments/assets/5e2460c1-2f6f-4fa6-a7ec-28eb3e8e6740)

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