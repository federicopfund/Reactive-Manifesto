# API Documentation

## Índice

1. [Visión General](#visión-general)
2. [Endpoints Públicos](#endpoints-públicos)
3. [Endpoints de Monitoreo](#endpoints-de-monitoreo)
4. [Modelos de Datos](#modelos-de-datos)
5. [Códigos de Estado](#códigos-de-estado)
6. [Ejemplos de Uso](#ejemplos-de-uso)
7. [Manejo de Errores](#manejo-de-errores)

## Visión General

La API REST del sistema de contacto reactivo sigue los principios RESTful y está diseñada para ser:

- **Intuitiva**: URLs claras y semánticas
- **Predecible**: Códigos de estado HTTP estándar
- **Documentada**: Respuestas descriptivas
- **Segura**: CSRF protection habilitado

**Base URL**: `http://localhost:9000` (desarrollo)

**Formato de Respuesta**: HTML para endpoints de usuario, JSON para endpoints de monitoreo

## Endpoints Públicos

### GET /contact

Muestra el formulario de contacto.

**Request**:
```http
GET /contact HTTP/1.1
Host: localhost:9000
Accept: text/html
```

**Response Success**:
```http
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
  <head><title>Contacto - Formulario Reactivo</title></head>
  <body>
    <form>...</form>
  </body>
</html>
```

**Ejemplo con cURL**:
```bash
curl http://localhost:9000/contact
```

---

### POST /contact

Envía un mensaje de contacto.

**Request**:
```http
POST /contact HTTP/1.1
Host: localhost:9000
Content-Type: application/x-www-form-urlencoded

name=Juan+Pérez&email=juan@example.com&message=Hola+mundo&subject=Consulta
```

**Headers Requeridos**:
- `Content-Type: application/x-www-form-urlencoded`
- `Csrf-Token: <token>` (obtenido del formulario GET)

**Body Parameters**:

| Parámetro | Tipo | Requerido | Restricciones | Descripción |
|-----------|------|-----------|---------------|-------------|
| `name` | String | Sí | 2-100 caracteres | Nombre completo del contacto |
| `email` | String | Sí | Formato email válido | Correo electrónico |
| `message` | String | Sí | 10-5000 caracteres | Mensaje a enviar |
| `subject` | String | No | Max 200 caracteres | Asunto del mensaje |

**Response Success (200 OK)**:
```http
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
  <body>
    <div class="result-success">
      <h1>Mensaje Enviado</h1>
      <p>Tu mensaje ha sido recibido y será procesado pronto.</p>
    </div>
  </body>
</html>
```

**Response Validation Error (400 Bad Request)**:
```http
HTTP/1.1 400 Bad Request
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
  <body>
    <form>
      <div class="error-message">El nombre debe tener al menos 2 caracteres</div>
      ...
    </form>
  </body>
</html>
```

**Response Server Error (500 Internal Server Error)**:
```http
HTTP/1.1 500 Internal Server Error
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
  <body>
    <div class="result-error">
      <h1>Error del sistema</h1>
      <p>Ocurrió un error inesperado. Por favor, intenta más tarde.</p>
    </div>
  </body>
</html>
```

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:9000/contact \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=Juan Pérez" \
  -d "email=juan@example.com" \
  -d "message=Este es un mensaje de prueba con más de 10 caracteres" \
  -d "subject=Consulta sobre servicios"
```

**Ejemplo con JavaScript (fetch)**:
```javascript
// Primero obtener el token CSRF del formulario
const form = document.getElementById('contactForm');
const formData = new FormData(form);

fetch('/contact', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: new URLSearchParams(formData),
})
.then(response => {
  if (response.ok) {
    return response.text();
  }
  throw new Error('Error en el envío');
})
.then(html => {
  document.body.innerHTML = html;
})
.catch(error => {
  console.error('Error:', error);
});
```

## Endpoints de Monitoreo

### GET /contact/health

Verifica el estado de salud del sistema de contacto.

**Request**:
```http
GET /contact/health HTTP/1.1
Host: localhost:9000
Accept: text/plain
```

**Response Success (200 OK)**:
```http
HTTP/1.1 200 OK
Content-Type: text/plain; charset=UTF-8

OK
```

**Response Unhealthy (503 Service Unavailable)**:
```http
HTTP/1.1 503 Service Unavailable
Content-Type: text/plain; charset=UTF-8

Service unhealthy
```

**Uso Típico**:
- Load balancer health checks
- Kubernetes liveness probes
- Monitoring systems (Nagios, Datadog, etc.)

**Ejemplo con cURL**:
```bash
# Health check simple
curl -f http://localhost:9000/contact/health

# Health check con timeout
curl --max-time 5 http://localhost:9000/contact/health
```

**Ejemplo con Kubernetes**:
```yaml
livenessProbe:
  httpGet:
    path: /contact/health
    port: 9000
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
```

---

### GET /contact/stats

Obtiene estadísticas del sistema de contacto.

**Request**:
```http
GET /contact/stats HTTP/1.1
Host: localhost:9000
Accept: application/json
```

**Response Success (200 OK)**:
```http
HTTP/1.1 200 OK
Content-Type: application/json; charset=UTF-8

{
  "received": 1523,
  "accepted": 1489,
  "rejected": 34
}
```

**Response Unavailable (503 Service Unavailable)**:
```http
HTTP/1.1 503 Service Unavailable
Content-Type: text/plain; charset=UTF-8

Stats unavailable
```

**Campos de Respuesta**:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `received` | Long | Total de mensajes recibidos desde inicio |
| `accepted` | Long | Total de mensajes aceptados (válidos) |
| `rejected` | Long | Total de mensajes rechazados (inválidos) |

**Métricas Derivadas**:
```javascript
const acceptance_rate = (accepted / received) * 100;  // Porcentaje de éxito
const rejection_rate = (rejected / received) * 100;   // Porcentaje de fallos
```

**Ejemplo con cURL**:
```bash
curl http://localhost:9000/contact/stats | jq .
```

**Ejemplo con Python**:
```python
import requests

response = requests.get('http://localhost:9000/contact/stats')
stats = response.json()

rejection_rate = stats['rejected'] / stats['received']
if rejection_rate > 0.1:  # 10% de rechazos
    print("ALERTA: Alta tasa de rechazo")
    trigger_scale_up()
```

**Uso en Prometheus**:
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'contact-stats'
    metrics_path: '/contact/stats'
    static_configs:
      - targets: ['localhost:9000']
```

## Modelos de Datos

### ContactData

Modelo de datos del formulario de contacto.

```scala
case class ContactData(
  name: String,
  email: String,
  message: String,
  subject: Option[String]
)
```

**Representación JSON** (conceptual):
```json
{
  "name": "Juan Pérez",
  "email": "juan@example.com",
  "message": "Este es mi mensaje de consulta",
  "subject": "Consulta sobre servicios"
}
```

**Reglas de Validación**:

| Campo | Validación |
|-------|------------|
| `name` | - No vacío<br>- Mínimo 2 caracteres<br>- Máximo 100 caracteres |
| `email` | - No vacío<br>- Formato email válido (regex: `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`) |
| `message` | - No vacío<br>- Mínimo 10 caracteres<br>- Máximo 5000 caracteres |
| `subject` | - Opcional<br>- Máximo 200 caracteres si presente |

### ContactStatsResponse

Modelo de respuesta de estadísticas.

```scala
case class ContactStatsResponse(
  totalReceived: Long,
  totalAccepted: Long,
  totalRejected: Long
)
```

**Representación JSON**:
```json
{
  "received": 1000,
  "accepted": 950,
  "rejected": 50
}
```

## Códigos de Estado

| Código | Significado | Cuándo se Usa |
|--------|-------------|---------------|
| 200 OK | Éxito | Mensaje enviado correctamente |
| 400 Bad Request | Datos inválidos | Validación de formulario falla |
| 403 Forbidden | CSRF inválido | Token CSRF faltante o incorrecto |
| 500 Internal Server Error | Error del servidor | Excepción no manejada |
| 503 Service Unavailable | Servicio no disponible | Health check falla o stats no disponibles |

## Ejemplos de Uso

### Flujo Completo con JavaScript

```javascript
// 1. Obtener el formulario
fetch('/contact')
  .then(response => response.text())
  .then(html => {
    document.body.innerHTML = html;
    
    // 2. Configurar evento de envío
    const form = document.getElementById('contactForm');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      // 3. Enviar datos
      const formData = new FormData(form);
      const response = await fetch('/contact', {
        method: 'POST',
        body: new URLSearchParams(formData)
      });
      
      // 4. Manejar respuesta
      if (response.ok) {
        const resultHtml = await response.text();
        document.body.innerHTML = resultHtml;
        console.log('Mensaje enviado con éxito');
      } else if (response.status === 400) {
        const errorHtml = await response.text();
        document.body.innerHTML = errorHtml;
        console.log('Error de validación');
      } else {
        console.error('Error del servidor');
      }
    });
  });
```

### Monitoreo con Bash Script

```bash
#!/bin/bash
# monitor_contact.sh

while true; do
  # Health check
  if ! curl -f -s http://localhost:9000/contact/health > /dev/null; then
    echo "$(date): ALERTA - Sistema no saludable"
    # Enviar notificación
    send_alert "Contact system unhealthy"
  fi
  
  # Stats check
  stats=$(curl -s http://localhost:9000/contact/stats)
  received=$(echo $stats | jq -r '.received')
  rejected=$(echo $stats | jq -r '.rejected')
  
  if [ $received -gt 0 ]; then
    rejection_rate=$(echo "scale=2; $rejected / $received" | bc)
    if (( $(echo "$rejection_rate > 0.10" | bc -l) )); then
      echo "$(date): ALERTA - Tasa de rechazo alta: $rejection_rate"
    fi
  fi
  
  sleep 60  # Check cada minuto
done
```

### Testing con Postman

**Colección Postman**:

```json
{
  "info": {
    "name": "Contact API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Contact Form",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:9000/contact",
          "protocol": "http",
          "host": ["localhost"],
          "port": "9000",
          "path": ["contact"]
        }
      }
    },
    {
      "name": "Submit Contact",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/x-www-form-urlencoded"
          }
        ],
        "body": {
          "mode": "urlencoded",
          "urlencoded": [
            {"key": "name", "value": "Juan Pérez"},
            {"key": "email", "value": "juan@example.com"},
            {"key": "message", "value": "Este es un mensaje de prueba"},
            {"key": "subject", "value": "Consulta"}
          ]
        },
        "url": {
          "raw": "http://localhost:9000/contact",
          "protocol": "http",
          "host": ["localhost"],
          "port": "9000",
          "path": ["contact"]
        }
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:9000/contact/health",
          "protocol": "http",
          "host": ["localhost"],
          "port": "9000",
          "path": ["contact", "health"]
        }
      }
    },
    {
      "name": "Get Stats",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Accept",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "http://localhost:9000/contact/stats",
          "protocol": "http",
          "host": ["localhost"],
          "port": "9000",
          "path": ["contact", "stats"]
        }
      }
    }
  ]
}
```

## Manejo de Errores

### Errores de Validación

**Respuesta**:
```html
<form>
  <div class="form-group">
    <input name="name" class="form-control-error" value="J">
    <span class="error-message">El nombre debe tener al menos 2 caracteres</span>
  </div>
  ...
</form>
```

**Tipos de Errores de Validación**:

| Campo | Error | Mensaje |
|-------|-------|---------|
| name | Vacío | "El nombre no puede estar vacío" |
| name | Muy corto | "El nombre debe tener al menos 2 caracteres" |
| name | Muy largo | "El nombre no puede exceder 100 caracteres" |
| email | Vacío | "El email no puede estar vacío" |
| email | Formato | "Formato de email inválido" |
| message | Vacío | "El mensaje no puede estar vacío" |
| message | Muy corto | "El mensaje debe tener al menos 10 caracteres" |
| message | Muy largo | "El mensaje no puede exceder 5000 caracteres" |

### Errores del Sistema

**Timeout (Alta Carga)**:
```
Error: "El sistema está experimentando alta carga. Por favor, intenta nuevamente."
Código: 400 Bad Request
Causa: Actor no respondió en 5 segundos
Acción: Reintentar después de un tiempo
```

**Error Interno**:
```
Error: "Error interno del sistema. Por favor, contacta al administrador."
Código: 500 Internal Server Error
Causa: Excepción no esperada
Acción: Revisar logs del servidor
```

### Rate Limiting

El sistema tiene rate limiting implícito mediante backpressure:

- **Umbral**: ~1000 requests/segundo por instancia
- **Comportamiento**: Timeouts después de 5 segundos
- **Respuesta al cliente**: "Sistema con alta carga"
- **Recuperación**: Automática cuando baja la carga

## Seguridad

### CSRF Protection

Todos los formularios POST requieren token CSRF:

```html
<form method="POST" action="/contact">
  <input type="hidden" name="csrfToken" value="[token-generado]">
  ...
</form>
```

**Si falta el token**:
```http
HTTP/1.1 403 Forbidden
Content-Type: text/html

CSRF token validation failed
```

### Input Sanitization

Todos los inputs son sanitizados en el servidor:

```scala
private def sanitize(input: String): String = {
  input.trim
    .replaceAll("<script>", "")
    .replaceAll("</script>", "")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .take(10000)
}
```

**Protecciones**:
- ✅ XSS (Cross-Site Scripting)
- ✅ HTML Injection
- ✅ Script Injection
- ✅ DoS via large inputs

## Rate Limits y Cuotas

| Recurso | Límite | Ventana |
|---------|--------|---------|
| POST /contact | ~1000 req/s | Por instancia |
| GET /contact | Ilimitado | - |
| GET /contact/health | Ilimitado | - |
| GET /contact/stats | ~100 req/s | Por instancia |

**Nota**: Los límites son implícitos mediante backpressure del sistema de actores.

## Versionado

**Versión actual**: v1.0

**Política de versionado**:
- Breaking changes: Nueva versión mayor
- Nuevos features: Nueva versión menor
- Bug fixes: Nueva versión patch

**Compatibilidad**: Se mantiene compatibilidad backward en versiones menores.

## Soporte y Contacto

Para reportar problemas o sugerencias:
- **GitHub Issues**: https://github.com/federicopfund/Reactive-Manifiesto/issues
- **Email**: Usar el formulario de contacto en `/contact`

## Changelog

### v1.0.0 (2026-01-05)
- ✅ Formulario de contacto básico
- ✅ Validación en múltiples capas
- ✅ Endpoints de monitoreo (health, stats)
- ✅ Implementación del Manifiesto Reactivo
- ✅ Sanitización de inputs
- ✅ CSRF protection
