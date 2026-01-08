# 🔧 Scripts de Instalación - Ejemplos de Uso

Este documento proporciona ejemplos prácticos de cómo usar los scripts de instalación.

## 📦 Archivos Creados

```
Reactive-Manifiesto/
├── install-dependencies.sh   # Script interactivo completo
├── quick-install.sh           # Script de instalación rápida
└── INSTALLATION.md            # Documentación completa
```

## 🚀 Escenarios de Uso

### Escenario 1: Primera Instalación (Usuario Nuevo)

```bash
# Clonar el repositorio
git clone https://github.com/federicopfund/Reactive-Manifiesto.git
cd Reactive-Manifiesto

# Ejecutar instalación interactiva
./install-dependencies.sh

# Seguir las instrucciones en pantalla
# El script verificará versiones y pedirá confirmaciones
```

### Escenario 2: Instalación Rápida (Automatización)

```bash
# Para CI/CD o instalación sin interacción
./quick-install.sh

# Este script NO pide confirmaciones
# Ideal para contenedores o scripts automatizados
```

### Escenario 3: Actualizar Dependencias

```bash
# Si ya tienes Java pero quieres actualizar
./install-dependencies.sh

# El script detectará versiones instaladas
# Te preguntará si deseas reinstalar/actualizar
```

### Escenario 4: Solo Instalar SBT (Java ya instalado)

```bash
# El script detectará Java automáticamente
./install-dependencies.sh

# Opciones que aparecerán:
# - Java detectado: Preguntar si actualizar
# - SBT no detectado: Instalar automáticamente
```

## 🎬 Demo de Instalación Completa

```bash
# Desde un sistema limpio (Ubuntu/Debian)

# 1. Otorgar permisos (si es necesario)
chmod +x install-dependencies.sh quick-install.sh

# 2. Ejecutar instalación
./install-dependencies.sh

# Salida esperada:
# ================================================
#    Instalador de Dependencias
#    Reactive-Manifiesto Project
# ================================================
# 
# [INFO] Actualizando repositorios del sistema...
# [INFO] === Instalación de Java ===
# [INFO] Java no está instalado. Instalando OpenJDK 17...
# [✓] Java instalado correctamente: openjdk version "17.0.x"
# 
# [INFO] === Instalación de SBT ===
# [INFO] SBT no está instalado. Instalando...
# [✓] SBT instalado correctamente: versión 1.9.7
# 
# [INFO] === Verificación de Instalación ===
# [✓] Java: openjdk version "17.0.x"
# [✓] JAVA_HOME: /usr/lib/jvm/java-17-openjdk-amd64
# [✓] SBT: sbt version in this project: 1.9.7
# 
# ================================================
#   Instalación completada exitosamente
# ================================================

# 3. Compilar proyecto (opcional)
sbt compile
```

## 🧪 Verificación Post-Instalación

```bash
# Verificar Java
java -version
# Salida esperada:
# openjdk version "17.0.x"

# Verificar JAVA_HOME
echo $JAVA_HOME
# Salida esperada:
# /usr/lib/jvm/java-17-openjdk-amd64

# Verificar SBT
sbt --version
# Salida esperada:
# sbt version in this project: 1.9.7
# sbt runner version: 1.x.x

# Verificar Scala (instalado con SBT)
scala -version
# Salida esperada:
# Scala code runner version 2.13.12
```

## 🔧 Personalización de Scripts

### Cambiar Versión de Java

Edita `install-dependencies.sh`, línea ~68:

```bash
# Cambiar de Java 17 a Java 21
# Antes:
apt-get install -y openjdk-17-jdk openjdk-17-jre

# Después:
apt-get install -y openjdk-21-jdk openjdk-21-jre
```

### Instalar Solo SBT

```bash
# Comentar la sección de Java
# En install-dependencies.sh, comenta líneas 40-90

# O ejecuta comandos manualmente:
curl -fsSL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo gpg --dearmor -o /usr/share/keyrings/sbt-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/sbt-archive-keyring.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
sudo apt-get update
sudo apt-get install -y sbt
```

## 🐳 Alternativa con Docker

Si prefieres no instalar en el sistema host:

```bash
# Usar el Dockerfile del proyecto
docker build -t reactive-app .
docker run -p 9000:9000 reactive-app
```

## 📊 Comparación de Scripts

| Característica | install-dependencies.sh | quick-install.sh |
|----------------|------------------------|------------------|
| Interactivo | ✅ Sí | ❌ No |
| Verificación previa | ✅ Completa | ⚡ Básica |
| Configuración JAVA_HOME | ✅ Automática | ❌ Manual |
| Mensajes con colores | ✅ Sí | ⚡ Limitado |
| Compilación opcional | ✅ Sí | ❌ No |
| Tiempo ejecución | ~2-3 min | ~1-2 min |
| Uso recomendado | Desarrollo local | CI/CD |

## 🔍 Resolución de Problemas

### Error: Permission denied

```bash
# Solución: Dar permisos de ejecución
chmod +x install-dependencies.sh quick-install.sh
```

### Error: sudo required

```bash
# Solución: Ejecutar con sudo
sudo ./install-dependencies.sh
```

### Error: GPG key failed

```bash
# Solución: Usar método alternativo
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
```

### JAVA_HOME no está configurado

```bash
# Solución: Recargar variables
source /etc/environment

# O configurar manualmente
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

## 📝 Logs y Debugging

### Modo Verbose

```bash
# Ejecutar con debug
bash -x ./install-dependencies.sh
```

### Guardar Log de Instalación

```bash
# Guardar salida en archivo
./install-dependencies.sh 2>&1 | tee install-log.txt
```

## 🎯 Checklist de Instalación

- [ ] Scripts tienen permisos de ejecución (`chmod +x`)
- [ ] Conexión a internet activa
- [ ] Permisos sudo disponibles
- [ ] Espacio en disco suficiente (>500MB)
- [ ] Ubuntu/Debian como sistema operativo
- [ ] Java 17 o compatible instalado
- [ ] SBT 1.9.7+ instalado
- [ ] Variables de entorno configuradas
- [ ] Proyecto compila sin errores (`sbt compile`)
- [ ] Aplicación inicia correctamente (`sbt run`)

## 🤝 Contribuciones

Para mejorar estos scripts:

1. Probar en diferentes distribuciones Linux
2. Agregar soporte para macOS (Homebrew)
3. Crear script de desinstalación
4. Agregar más opciones de configuración
5. Mejorar manejo de errores

## 📚 Referencias

- [OpenJDK Installation](https://openjdk.org/install/)
- [SBT Setup Documentation](https://www.scala-sbt.org/1.x/docs/Setup.html)
- [Play Framework Requirements](https://www.playframework.com/documentation/3.0.x/Requirements)
