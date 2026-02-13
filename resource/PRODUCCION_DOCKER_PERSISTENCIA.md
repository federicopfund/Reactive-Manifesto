# 🚀 GUÍA PRODUCCIÓN: Docker + PostgreSQL + Persistencia

## 🎯 Concepto: ¿Cómo Persisten los Datos?

### El Problema
```
Sin persistencia (MALO):
docker-compose up     → App + BD corriendo
docker-compose down   → ❌ TODOS los datos se pierden
```

### La Solución: Volúmenes Docker
```
Con volúmenes (BUENO):
docker-compose up     → App + BD corriendo
                         Datos guardados en /postgres_data/
docker-compose down   → ✅ Datos permanecen en disco
docker-compose up     → BD recupera datos automáticamente
```

---

## 📊 Estructura de Persistencia

```
┌─────────────────────────────────────────────────┐
│ Tu Computadora / Servidor                       │
├─────────────────────────────────────────────────┤
│                                                 │
│ /tu/proyecto/postgres_data/                    │
│ └─ PG_VERSION                                   │
│ └─ base/                    ← DATOS REALES      │
│ └─ global/                  ← TABLAS            │
│ └─ pg_wal/                  ← LOGS              │
│                                                 │
│ Docker Container (PostgreSQL)                  │
│ ├─ /var/lib/postgresql/data                    │
│ │  └─ Monta volumen ↑                          │
│ │     (Mismo contenido que postgres_data/)     │
│ └─ Lee/Escribe datos en volumen                │
│                                                 │
└─────────────────────────────────────────────────┘

Cuando docker stop → postgres_data/ no se toca ✅
Cuando docker start → Recupera datos automáticamente ✅
```

---

## 🔧 Opción 1: Docker Compose Local (Desarrollo/Testing)

### 1. Crear archivo .env

```bash
cp .env.example .env.production
# Editar .env.production con valores seguros
```

**.env.production:**
```
DB_PASSWORD=PostgresPass123!@#$%
PLAY_SECRET=SuperSecretPlayKey1234567890abcdef
POSTGRES_DATA_PATH=./postgres_data
```

### 2. Levantar Stack Completo

```bash
# Inicia app + BD con persistencia
docker-compose -f docker-compose.prod.yml up -d

# Ver logs
docker-compose -f docker-compose.prod.yml logs -f

# Ver estado
docker-compose -f docker-compose.prod.yml ps
```

**Resultado:**
```
CONTAINER ID   IMAGE                    NAMES                    STATUS
abc123         postgres:16-alpine       reactive_manifesto_db    Up (healthy)
def456         reactive_manifesto_app   reactive_manifesto_app   Up
```

### 3. Verificar Persistencia

```bash
# 1. Ver datos en BD
docker-compose -f docker-compose.prod.yml exec postgres psql \
  -U reactive_user -d reactive_manifesto \
  -c "SELECT COUNT(*) FROM users;"

# 2. Detener contenedores
docker-compose -f docker-compose.prod.yml down
# ⚠️ Contenedores paran, pero /postgres_data/ PERSISTE

# 3. Reiniciar
docker-compose -f docker-compose.prod.yml up -d

# 4. Verificar datos siguen ahí
docker-compose -f docker-compose.prod.yml exec postgres psql \
  -U reactive_user -d reactive_manifesto \
  -c "SELECT COUNT(*) FROM users;"
# ✅ Mismo número de registros!
```

---

## 🚀 Opción 2: Heroku (Recomendado para Comenzar)

### 1. Crear app en Heroku

```bash
heroku login
heroku create mi-app-reactive-manifesto
```

### 2. Agregar PostgreSQL

```bash
# Agregar Postgres como addon
heroku addons:create heroku-postgresql:standard-0
  # ^ $50/mes, ✅ backups automáticos

# O versión gratuita (limitada):
heroku addons:create heroku-postgresql:hobby-dev
  # ^ $0/mes, pero 10K filas, no para producción
```

### 3. Desplegar desde Git

```bash
# Asegúrate de tener Dockerfile
git add .
git commit -m "Ready for Heroku"

# Deploy
git push heroku main

# Ver logs
heroku logs -t
```

### 4. Ver URL

```bash
heroku open
# https://mi-app-reactive-manifesto.herokuapp.com
```

**Ventajas Heroku:**
- ✅ BD PostgreSQL incluida
- ✅ Backups automáticos
- ✅ SSL incluido
- ✅ Escalable
- ❌ Más caro que self-hosted

---

## ☁️ Opción 3: AWS EC2 (Control Total)

### 1. Crear instancia EC2

```bash
# En AWS Console:
# - AMI: Ubuntu 22.04 LTS
# - Type: t3.micro (free tier)
# - Storage: 30GB (free tier)
# - Security Group: abrir puertos 80, 443, 9000
```

### 2. Conectar por SSH

```bash
ssh -i tu-key.pem ubuntu@tu-instancia.compute.amazonaws.com

# Instalar Docker
sudo apt update
sudo apt install docker.io docker-compose -y
sudo usermod -aG docker $USER
newgrp docker
```

### 3. Clonar y Desplegar

```bash
git clone https://github.com/tu-usuario/Reactive-Manifesto.git
cd Reactive-Manifesto

# Crear .env
cp .env.example .env.production
nano .env.production  # Cambiar contraseñas

# Levantar
docker-compose -f docker-compose.prod.yml up -d

# Ver
curl http://localhost:9000
```

### 4. Usar Nginx como Reverse Proxy

**/etc/nginx/sites-available/manifesto:**
```nginx
server {
    listen 80;
    server_name tu-dominio.com;

    # Redirigir HTTP → HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name tu-dominio.com;

    ssl_certificate /etc/letsencrypt/live/tu-dominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tu-dominio.com/privkey.pem;

    location / {
        proxy_pass http://localhost:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Habilitar
sudo ln -s /etc/nginx/sites-available/manifesto /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# SSL gratis
sudo apt install certbot python3-certbot-nginx -y
sudo certbot certonly --standalone -d tu-dominio.com
```

---

## 🐳 Opción 4: DigitalOcean App Platform (Simple)

### 1. Conectar GitHub

```
DigitalOcean Console:
→ Apps
→ Create App
→ Connect GitHub
→ Seleccionar repo Reactive-Manifesto
```

### 2. Configurar

```yaml
services:
- name: app
  github:
    repo: tu-usuario/Reactive-Manifesto
    branch: main
  dockerfile_path: Dockerfile
  http_port: 9000
  
databases:
- name: postgres
  version: 16
```

### 3. Deploy

```
→ Deploy
(DigitalOcean construye imagen, crea BD, despliega)

URL: https://reactive-manifesto-abc123.ondigitalocean.app
```

**Ventajas:**
- ✅ Sin configuración de BD
- ✅ Escala automática
- ✅ SSL incluido
- ✅ $5-12/mes
- ❌ Menos control que EC2

---

## 🔄 Opción 5: Railway.app (Gratis)

### 1. Conectar GitHub

```
Railway.app:
→ New Project
→ Deploy from GitHub
→ Autorizar
→ Seleccionar repo
```

### 2. Agregar PostgreSQL

```
→ Add Service
→ PostgreSQL
→ Railway auto-configura
```

### 3. Configurar App

Railway inyecta variables automáticamente:
```
DATABASE_URL → Usa automáticamente
POSTGRES_PASSWORD → Railway configura
```

### 4. Deploy

```
→ Deploy
Listo! URL: https://reactive-manifesto-prod.railway.app
```

---

## 📋 Comparativa Opciones

| Opción | Costo | Setup | BD Automática | Escalable | Recomendado |
|--------|-------|-------|---------------|-----------|------------|
| Docker Local | $0 | 5 min | ✅ | ❌ | Dev/Test |
| Heroku | $50+ | 5 min | ✅ | ✅ | Pequeño proyecto |
| AWS EC2 | $10-50 | 30 min | ❌ (manual) | ✅ | Producción |
| DigitalOcean | $5-12 | 10 min | ✅ | ✅ | Recomendado |
| Railway | $0-20 | 5 min | ✅ | ✅ | Rápido |

---

## 🛡️ Producción Checklist

### Seguridad

- [ ] Cambiar `DB_PASSWORD` a mínimo 32 caracteres
- [ ] Cambiar `PLAY_SECRET` a key segura generada
- [ ] No commitear `.env.production` a Git
- [ ] Usar HTTPS (SSL/TLS)
- [ ] Firewall: solo puertos 80, 443
- [ ] Backups automáticos configurados

### Performance

- [ ] JVM heap: `-Xmx1024m` (ajustar según RAM)
- [ ] Connection pool: `numThreads = 20`
- [ ] Cache de BD configurado
- [ ] Índices en tablas grandes
- [ ] Logs rotados

### Monitoring

- [ ] Alerts si app down
- [ ] Logs centralizados
- [ ] Métricas de BD
- [ ] Uptime monitoring

---

## 📡 Backup & Restore

### Backup Manual

```bash
# Hacer dump
docker-compose -f docker-compose.prod.yml exec postgres \
  pg_dump -U reactive_user reactive_manifesto \
  > backup_$(date +%Y%m%d_%H%M%S).sql

# Comprimir
gzip backup_*.sql
```

### Restore

```bash
# Descomprimir
gunzip backup_*.sql.gz

# Restaurar
docker-compose -f docker-compose.prod.yml exec -T postgres \
  psql -U reactive_user reactive_manifesto < backup_*.sql
```

### Backup Automático (Heroku)

```bash
# Configurar en Heroku
heroku pg:backups:schedule DATABASE_URL --at "02:00 UTC"

# Ver backups
heroku pg:backups

# Descargar
heroku pg:backups:download
```

---

## 🔍 Troubleshooting

### "Disco lleno"

```bash
# Ver espacio
df -h

# Limpiar Docker
docker system prune -a

# Mover postgres_data a otro disco
sudo mv postgres_data/ /media/datos/
# Ajustar docker-compose.prod.yml POSTGRES_DATA_PATH
```

### "BD lenta"

```bash
# Ver conexiones
docker-compose -f docker-compose.prod.yml exec postgres psql \
  -U reactive_user -d reactive_manifesto \
  -c "SELECT * FROM pg_stat_statements LIMIT 10;"

# Optimizar índices
REINDEX DATABASE reactive_manifesto;
VACUUM ANALYZE;
```

### "Corruptión de BD"

```bash
# Reparar
docker-compose -f docker-compose.prod.yml exec postgres \
  pg_repair -U reactive_user reactive_manifesto

# O restaurar desde backup
```

---

## ✅ Verificar Todo Funciona

```bash
# 1. App está corriendo
curl http://localhost:9000

# 2. BD está conectada
docker logs reactive_manifesto_app | grep "database"

# 3. Datos persisten
docker-compose -f docker-compose.prod.yml down
sleep 5
docker-compose -f docker-compose.prod.yml up -d
# Verificar datos

# 4. Contactos en BD
docker-compose -f docker-compose.prod.yml exec postgres psql \
  -U reactive_user -d reactive_manifesto \
  -c "SELECT COUNT(*) FROM contacts;"
```

---

## 🎯 Recomendación Final

Para producción, usar **DigitalOcean App Platform**:
- ✅ Fácil: GitHub → Deploy automático
- ✅ BD incluida: PostgreSQL administrada
- ✅ Costo: $5-12/mes
- ✅ Escalable: Aumenta recursos con botón
- ✅ SSL: Gratis y automático
- ✅ Backups: Automáticos

**O en empresa:**
- AWS EC2 + RDS para PostgreSQL
- Terraform para infraestructura
- GitHub Actions para CI/CD
- CloudFront para CDN

---

**Los datos SIEMPRE persisten si usas volúmenes Docker. ✅**
