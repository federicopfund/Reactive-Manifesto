# Guía de Despliegue

## Índice

1. [Prerrequisitos](#prerrequisitos)
2. [Despliegue en Desarrollo](#despliegue-en-desarrollo)
3. [Despliegue en Producción](#despliegue-en-producción)
4. [Configuración](#configuración)
5. [Docker](#docker)
6. [Kubernetes](#kubernetes)
7. [Monitoreo](#monitoreo)
8. [Troubleshooting](#troubleshooting)

## Prerrequisitos

### Requisitos Mínimos

**Hardware**:
- CPU: 2 cores
- RAM: 2GB
- Disco: 1GB libre

**Software**:
- Java JDK 11 o superior
- SBT 1.9.0 o superior
- Git

### Requisitos Recomendados (Producción)

**Hardware**:
- CPU: 4+ cores
- RAM: 4-8GB
- Disco: 10GB SSD

**Software**:
- Java JDK 17 (LTS)
- SBT 1.9.x
- Nginx (como reverse proxy)
- Monitoreo (Prometheus + Grafana)

## Despliegue en Desarrollo

### 1. Clonar el Repositorio

```bash
git clone https://github.com/federicopfund/Reactive-Manifiesto.git
cd Reactive-Manifiesto
```

### 2. Compilar el Proyecto

```bash
sbt compile
```

**Tiempo estimado**: 2-5 minutos (primera vez)

### 3. Ejecutar Tests

```bash
sbt test
```

### 4. Ejecutar en Modo Desarrollo

```bash
sbt run
```

**Acceso**: http://localhost:9000

**Características del modo desarrollo**:
- ✅ Hot reload automático
- ✅ Logs detallados en consola
- ✅ Sin optimizaciones de producción
- ✅ CSRF en modo permisivo

### 5. Detener la Aplicación

```bash
# En la terminal de SBT
Ctrl+C

# O desde otra terminal
pkill -f "sbt run"
```

## Despliegue en Producción

### Opción 1: Standalone (JAR)

#### 1. Generar Distribución

```bash
sbt dist
```

**Output**: `target/universal/web-1.0-SNAPSHOT.zip`

#### 2. Descomprimir

```bash
cd /opt
unzip /path/to/web-1.0-SNAPSHOT.zip
cd web-1.0-SNAPSHOT
```

#### 3. Configurar Variables de Entorno

```bash
export JAVA_OPTS="-Xmx2G -Xms1G -XX:+UseG1GC"
export APPLICATION_SECRET="changeme-in-production-use-long-random-string"
export HTTP_PORT=9000
```

#### 4. Ejecutar

```bash
./bin/web -Dplay.http.secret.key=$APPLICATION_SECRET
```

#### 5. Configurar como Servicio (systemd)

Crear `/etc/systemd/system/reactive-contact.service`:

```ini
[Unit]
Description=Reactive Contact System
After=network.target

[Service]
Type=forking
User=appuser
Group=appuser
WorkingDirectory=/opt/web-1.0-SNAPSHOT
ExecStart=/opt/web-1.0-SNAPSHOT/bin/web \
  -Dplay.http.secret.key=${APPLICATION_SECRET} \
  -Dhttp.port=9000 \
  -J-Xmx2G \
  -J-Xms1G \
  -J-XX:+UseG1GC
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Habilitar y arrancar**:
```bash
sudo systemctl daemon-reload
sudo systemctl enable reactive-contact
sudo systemctl start reactive-contact
sudo systemctl status reactive-contact
```

### Opción 2: Docker

Ver sección [Docker](#docker) más abajo.

### Opción 3: Kubernetes

Ver sección [Kubernetes](#kubernetes) más abajo.

## Configuración

### application.conf

Archivo principal: `conf/application.conf`

```hocon
# Configuración de Play
play {
  http {
    secret.key = ${?APPLICATION_SECRET}
    port = ${?HTTP_PORT}
  }
  
  filters {
    enabled += play.filters.csrf.CSRFFilter
    enabled += play.filters.headers.SecurityHeadersFilter
    enabled += play.filters.hosts.AllowedHostsFilter
  }
}

# Configuración de Akka
akka {
  actor {
    default-dispatcher {
      type = Dispatcher
      executor = "fork-join-executor"
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 3.0
        parallelism-max = 64
      }
    }
  }
  
  log-level = "INFO"
  log-dead-letters = 10
  log-dead-letters-during-shutdown = off
}
```

### Variables de Entorno

| Variable | Descripción | Ejemplo | Requerido |
|----------|-------------|---------|-----------|
| `APPLICATION_SECRET` | Secret key para Play | `"long-random-string"` | ✅ Producción |
| `HTTP_PORT` | Puerto HTTP | `9000` | ❌ (default: 9000) |
| `JAVA_OPTS` | Opciones de JVM | `"-Xmx2G"` | ❌ |
| `LOG_LEVEL` | Nivel de logs | `INFO` | ❌ (default: INFO) |

### Generar Application Secret

```bash
# Método 1: SBT
sbt playGenerateSecret

# Método 2: OpenSSL
openssl rand -base64 48

# Método 3: /dev/urandom
head -c 48 /dev/urandom | base64
```

### Configuración de JVM

**Desarrollo**:
```bash
export JAVA_OPTS="-Xmx1G -Xms512M"
```

**Producción (Baja Carga)**:
```bash
export JAVA_OPTS="-Xmx2G -Xms1G -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**Producción (Alta Carga)**:
```bash
export JAVA_OPTS="-Xmx4G -Xms2G -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
```

## Docker

### Dockerfile

Crear `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine

# Crear usuario no-root
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

# Directorio de trabajo
WORKDIR /opt/app

# Copiar distribución
COPY --chown=appuser:appuser target/universal/web-1.0-SNAPSHOT.zip .

# Descomprimir
RUN unzip web-1.0-SNAPSHOT.zip && \
    mv web-1.0-SNAPSHOT/* . && \
    rm web-1.0-SNAPSHOT.zip && \
    chmod +x bin/web

# Usuario no-root
USER appuser

# Puerto
EXPOSE 9000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9000/contact/health || exit 1

# Comando
ENTRYPOINT ["bin/web"]
CMD ["-Dplay.http.secret.key=${APPLICATION_SECRET}"]
```

### Construcción

```bash
# 1. Generar distribución
sbt dist

# 2. Construir imagen
docker build -t reactive-contact:1.0 .

# 3. Ver imagen
docker images reactive-contact
```

### Ejecución

```bash
docker run -d \
  --name reactive-contact \
  -p 9000:9000 \
  -e APPLICATION_SECRET="your-secret-key-here" \
  -e JAVA_OPTS="-Xmx2G -Xms1G" \
  --restart unless-stopped \
  reactive-contact:1.0
```

### Docker Compose

Crear `docker-compose.yml`:

```yaml
version: '3.8'

services:
  app:
    image: reactive-contact:1.0
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "9000:9000"
    environment:
      - APPLICATION_SECRET=${APPLICATION_SECRET}
      - JAVA_OPTS=-Xmx2G -Xms1G -XX:+UseG1GC
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:9000/contact/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - app
    restart: unless-stopped
```

**Ejecutar**:
```bash
docker-compose up -d
```

## Kubernetes

### Deployment

Crear `k8s/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reactive-contact
  labels:
    app: reactive-contact
spec:
  replicas: 3
  selector:
    matchLabels:
      app: reactive-contact
  template:
    metadata:
      labels:
        app: reactive-contact
    spec:
      containers:
      - name: app
        image: reactive-contact:1.0
        ports:
        - containerPort: 9000
          name: http
        env:
        - name: APPLICATION_SECRET
          valueFrom:
            secretKeyRef:
              name: reactive-contact-secret
              key: application-secret
        - name: JAVA_OPTS
          value: "-Xmx2G -Xms1G -XX:+UseG1GC"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /contact/health
            port: 9000
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /contact/health
            port: 9000
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2
```

### Service

Crear `k8s/service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: reactive-contact-service
spec:
  selector:
    app: reactive-contact
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9000
  type: LoadBalancer
```

### Secret

Crear `k8s/secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: reactive-contact-secret
type: Opaque
stringData:
  application-secret: "your-secret-key-here"
```

### Ingress

Crear `k8s/ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: reactive-contact-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: contact.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: reactive-contact-service
            port:
              number: 80
```

### Desplegar en Kubernetes

```bash
# 1. Crear namespace
kubectl create namespace reactive-contact

# 2. Aplicar secret
kubectl apply -f k8s/secret.yaml -n reactive-contact

# 3. Aplicar deployment
kubectl apply -f k8s/deployment.yaml -n reactive-contact

# 4. Aplicar service
kubectl apply -f k8s/service.yaml -n reactive-contact

# 5. Aplicar ingress
kubectl apply -f k8s/ingress.yaml -n reactive-contact

# 6. Verificar
kubectl get pods -n reactive-contact
kubectl get svc -n reactive-contact
kubectl logs -f deployment/reactive-contact -n reactive-contact
```

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: reactive-contact-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: reactive-contact
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## Monitoreo

### Prometheus

**Configuración** (`prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'reactive-contact'
    metrics_path: '/contact/stats'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:9000']
```

### Grafana Dashboard

**Métricas clave**:

1. **Throughput**
   - Query: `rate(contact_received_total[5m])`
   - Visualización: Graph

2. **Rejection Rate**
   - Query: `rate(contact_rejected_total[5m]) / rate(contact_received_total[5m])`
   - Visualización: Gauge
   - Alert: > 0.1 (10%)

3. **Latency**
   - Query: `histogram_quantile(0.99, rate(contact_latency_bucket[5m]))`
   - Visualización: Graph

4. **Success Rate**
   - Query: `rate(contact_accepted_total[5m]) / rate(contact_received_total[5m])`
   - Visualización: Single Stat

### Health Checks

**Nagios/Icinga**:
```bash
#!/bin/bash
# check_reactive_contact.sh

response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/contact/health)

if [ $response -eq 200 ]; then
  echo "OK - Contact system healthy"
  exit 0
else
  echo "CRITICAL - Contact system unhealthy (HTTP $response)"
  exit 2
fi
```

## Troubleshooting

### Problema: Aplicación no arranca

**Síntomas**:
```
Error: Unable to bind to port 9000
```

**Soluciones**:
```bash
# 1. Verificar puerto ocupado
lsof -i :9000
netstat -tulpn | grep 9000

# 2. Cambiar puerto
export HTTP_PORT=9001
sbt run

# 3. Matar proceso que ocupa el puerto
kill -9 <PID>
```

### Problema: OutOfMemoryError

**Síntomas**:
```
java.lang.OutOfMemoryError: Java heap space
```

**Soluciones**:
```bash
# 1. Aumentar heap
export JAVA_OPTS="-Xmx4G -Xms2G"

# 2. Usar G1GC
export JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 3. Analizar heap dump
jmap -dump:live,format=b,file=heap.bin <PID>
```

### Problema: Alta latencia

**Diagnóstico**:
```bash
# 1. Ver stats
curl http://localhost:9000/contact/stats

# 2. Ver métricas de JVM
jstat -gcutil <PID> 1000

# 3. Ver threads
jstack <PID> > threads.txt
```

**Soluciones**:
- Aumentar `parallelism-max` en Akka
- Escalar horizontalmente (más instancias)
- Revisar logs para cuellos de botella

### Problema: Health check falla

**Diagnóstico**:
```bash
# 1. Probar health endpoint
curl -v http://localhost:9000/contact/health

# 2. Ver logs
tail -f /opt/web-1.0-SNAPSHOT/logs/application.log

# 3. Ver estado del actor system
# (requiere JMX habilitado)
jconsole <PID>
```

## Checklist de Despliegue

### Pre-Despliegue

- [ ] Tests pasan (`sbt test`)
- [ ] Compilación exitosa (`sbt compile`)
- [ ] Variables de entorno configuradas
- [ ] APPLICATION_SECRET generado
- [ ] Recursos suficientes (CPU, RAM)
- [ ] Puerto 9000 disponible

### Post-Despliegue

- [ ] Aplicación arranca sin errores
- [ ] Health check responde OK
- [ ] Stats endpoint accesible
- [ ] Formulario de contacto funciona
- [ ] Logs sin errores críticos
- [ ] Métricas en monitoreo
- [ ] Backups configurados (si aplica)

## Rollback

### Docker

```bash
# 1. Detener versión nueva
docker stop reactive-contact

# 2. Arrancar versión anterior
docker start reactive-contact-old

# 3. Actualizar routing
# (depende de load balancer)
```

### Kubernetes

```bash
# Rollback automático
kubectl rollout undo deployment/reactive-contact -n reactive-contact

# Ver historial
kubectl rollout history deployment/reactive-contact -n reactive-contact

# Rollback a revisión específica
kubectl rollout undo deployment/reactive-contact --to-revision=2 -n reactive-contact
```

## Soporte

Para problemas de despliegue:
- **GitHub Issues**: https://github.com/federicopfund/Reactive-Manifiesto/issues
- **Logs**: Revisar `logs/application.log`
- **Docs**: Leer [ARCHITECTURE.md](ARCHITECTURE.md)
