# Guía de Prueba del Dockerfile

Este documento explica cómo probar que el Dockerfile funciona correctamente.

## Verificación Rápida de la Estructura

### 1. Verificar que el Dockerfile existe y tiene la estructura correcta

```bash
# Ver el contenido del Dockerfile
cat Dockerfile
```

**Lo que debes ver:**
- ✅ Multi-stage build con dos stages: `builder` y `runtime`
- ✅ Stage de builder usa `eclipse-temurin:17-jdk`
- ✅ Stage de runtime usa `eclipse-temurin:17-jre`
- ✅ Comando `RUN sbt stage` en el builder
- ✅ `COPY --from=builder` para copiar solo la aplicación compilada
- ✅ Variables de entorno `PORT` y `APPLICATION_SECRET`
- ✅ CMD que usa `${PORT:-9000}`

### 2. Construir la imagen Docker

```bash
# En tu máquina local (no en el entorno de CI/CD)
docker build -t reactive-app .
```

**Tiempo estimado:** 10-15 minutos la primera vez (descarga dependencias)

**Lo que debes ver:**
```
[+] Building ...
 => [builder 1/10] FROM eclipse-temurin:17-jdk
 => [builder 5/10] RUN apt-get update && apt-get install -y wget ...
 => [builder 10/10] RUN sbt stage
 => [stage-1 1/3] FROM eclipse-temurin:17-jre
 => [stage-1 3/3] COPY --from=builder /app/target/universal/stage /app
 => exporting to image
```

### 3. Verificar el tamaño de la imagen

```bash
docker images reactive-app
```

**Esperado:** La imagen debe ser razonablemente pequeña (< 500 MB) gracias al multi-stage build.

### 4. Inspeccionar la estructura de la imagen

```bash
# Verificar que el binario de la aplicación existe
docker run --rm reactive-app ls -la /app/bin/
```

**Debes ver:** Un archivo llamado `web` que es el ejecutable de la aplicación Play.

### 5. Verificar las variables de entorno

```bash
docker run --rm reactive-app printenv | grep -E "(JAVA_OPTS|APPLICATION_MODE)"
```

**Esperado:**
```
APPLICATION_MODE=prod
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dplay.server.pidfile.path=/dev/null
```

## Prueba Completa: Ejecutar la Aplicación

### 6. Iniciar el contenedor

```bash
docker run -d \
  --name reactive-test \
  -p 9000:9000 \
  -e PORT=9000 \
  -e APPLICATION_SECRET="test-secret-key-must-be-at-least-32-characters-long" \
  reactive-app
```

### 7. Ver los logs

```bash
# Ver logs en tiempo real
docker logs -f reactive-test
```

**Busca mensajes como:**
- `[info] application - Application started`
- `Server started, listening on port 9000`
- O similar

### 8. Probar que la aplicación responde

```bash
# En otra terminal
curl http://localhost:9000/
```

**Esperado:** Una respuesta HTML o redirección (código 200, 301, o 303 dependiendo de tu aplicación)

### 9. Detener y limpiar

```bash
docker stop reactive-test
docker rm reactive-test
```

## Prueba con Variables de Entorno Personalizadas

```bash
# Usar un puerto diferente
docker run -d \
  --name reactive-test-8080 \
  -p 8080:8080 \
  -e PORT=8080 \
  -e APPLICATION_SECRET="your-production-secret-key-here-min-32-chars" \
  reactive-app

# Verificar que funciona en el puerto 8080
curl http://localhost:8080/

# Limpiar
docker stop reactive-test-8080
docker rm reactive-test-8080
```

## Deployment en Render.com

### Pasos para Deploy:

1. **Push del código a GitHub:**
   ```bash
   git push origin main  # o tu branch principal
   ```

2. **En Render.com:**
   - Ve a https://render.com
   - Click en "New +" → "Web Service"
   - Conecta tu repositorio de GitHub
   - Render detectará automáticamente el Dockerfile

3. **Configuración:**
   - **Name:** reactive-manifiesto (o el que prefieras)
   - **Region:** Elige la más cercana
   - **Branch:** main
   - **Docker Command:** (dejar en blanco, usará el CMD del Dockerfile)
   - **Instance Type:** Free

4. **Variables de Entorno en Render:**
   - `APPLICATION_SECRET`: Genera una clave segura (mínimo 32 caracteres)
     ```bash
     # Genera una clave aleatoria (produce ~44 caracteres en base64, suficiente):
     openssl rand -base64 32
     ```
   - `PORT`: Render lo configura automáticamente, NO lo configures manualmente

5. **Deploy:**
   - Click en "Create Web Service"
   - Render construirá la imagen y desplegará automáticamente

### Verificar el Deploy en Render:

1. Revisa los logs en el dashboard de Render
2. Una vez que diga "Live", click en la URL generada
3. Debes ver tu aplicación funcionando

## Troubleshooting

### Problema: Build falla en "sbt stage"
**Solución:** Asegúrate de tener buena conexión a internet. sbt descarga muchas dependencias.

### Problema: Aplicación no inicia - "Secret key is invalid"
**Solución:** Verifica que `APPLICATION_SECRET` tenga al menos 32 caracteres.

### Problema: "Port already in use"
**Solución:** 
```bash
# Ver qué está usando el puerto
sudo lsof -i :9000
# O usa otro puerto
docker run -p 8080:9000 ...
```

### Problema: Imagen muy grande
**Solución:** El multi-stage build ya optimiza el tamaño. Puedes verificar:
```bash
docker history reactive-app
```

## Checklist de Validación

- [ ] Dockerfile se construye sin errores
- [ ] La imagen final usa `eclipse-temurin:17-jre` (no JDK)
- [ ] El binario `/app/bin/web` existe en la imagen
- [ ] Variables de entorno están configuradas
- [ ] La aplicación inicia sin el puerto hardcodeado
- [ ] Responde a peticiones HTTP
- [ ] Funciona con diferente valor de PORT
- [ ] Requiere APPLICATION_SECRET para iniciar

## Resultado Esperado

Si todos los pasos funcionan correctamente:
✅ El Dockerfile está listo para producción
✅ Es compatible con Render.com
✅ Usa recursos de manera eficiente
✅ Sigue mejores prácticas de seguridad
