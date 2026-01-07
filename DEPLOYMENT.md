# 🚀 Guía de Deployment - Reactive Manifesto

Esta guía detalla las múltiples opciones para deployar la aplicación Reactive Manifesto en diferentes plataformas.

## 📋 Tabla de Contenidos

- [Requisitos Previos](#requisitos-previos)
- [Deployment con Docker](#deployment-con-docker)
- [Deployment en Heroku](#deployment-en-heroku)
- [Deployment en Railway](#deployment-en-railway)
- [Deployment en Render](#deployment-en-render)
- [Deployment en Google Cloud Run](#deployment-en-google-cloud-run)
- [Deployment en AWS](#deployment-en-aws)
- [Configuración de GitHub Actions](#configuración-de-github-actions)
- [Variables de Entorno](#variables-de-entorno)

---

## 🔧 Requisitos Previos

- Cuenta de GitHub
- Java 17 o superior instalado localmente (para desarrollo)
- SBT 1.9.x instalado localmente (para desarrollo)
- Docker instalado (para deployment con contenedores)

---

## 🐳 Deployment con Docker

### 1. Construcción Local

```bash
# Construir la imagen Docker
docker build -t reactive-manifesto .

# Ejecutar el contenedor
docker run -p 9000:9000 \
  -e APPLICATION_SECRET="your-secret-key-here" \
  reactive-manifesto
```

### 2. Con Docker Compose (Incluye PostgreSQL)

```bash
# Crear archivo .env con tus variables
cat > .env << EOF
APPLICATION_SECRET=your-secret-key-here
DB_URL=jdbc:postgresql://db:5432/reactive_manifesto
DB_USER=postgres
DB_PASSWORD=postgres
EOF

# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Detener servicios
docker-compose down
```

### 3. Push a Docker Hub (Opcional)

```bash
# Autenticarse en Docker Hub
docker login

# Etiquetar la imagen
docker tag reactive-manifesto username/reactive-manifesto:latest

# Subir a Docker Hub
docker push username/reactive-manifesto:latest
```

---

## 🎯 Deployment en Heroku

### Opción 1: Desde GitHub (Recomendado)

1. **Crear cuenta en Heroku**: https://heroku.com
2. **Crear nueva aplicación**:
   ```bash
   heroku create mi-reactive-manifesto
   ```
3. **Configurar variables de entorno**:
   ```bash
   heroku config:set APPLICATION_SECRET=$(openssl rand -base64 32)
   heroku config:set PLAY_ENV=prod
   ```
4. **Conectar con GitHub**:
   - Ve a tu aplicación en Heroku Dashboard
   - Tab "Deploy" → "Deployment method" → "GitHub"
   - Conecta tu repositorio
   - Habilita "Automatic deploys" desde la rama `main`

5. **Opcional - Agregar PostgreSQL**:
   ```bash
   heroku addons:create heroku-postgresql:mini
   ```

### Opción 2: Deploy Manual

```bash
# Autenticarse
heroku login

# Agregar remote de Heroku
heroku git:remote -a mi-reactive-manifesto

# Deploy
git push heroku main

# Abrir la aplicación
heroku open
```

### Comandos Útiles de Heroku

```bash
# Ver logs en tiempo real
heroku logs --tail

# Ejecutar comandos en el dyno
heroku run bash

# Escalar dynos
heroku ps:scale web=1

# Ver información de la app
heroku info

# Reiniciar la aplicación
heroku restart
```

---

## 🚂 Deployment en Railway

Railway es una alternativa moderna y sencilla a Heroku.

### Pasos:

1. **Crear cuenta en Railway**: https://railway.app
2. **Nuevo Proyecto desde GitHub**:
   - Click en "New Project"
   - Selecciona "Deploy from GitHub repo"
   - Autoriza y selecciona tu repositorio
3. **Configurar Variables**:
   - Ve a "Variables"
   - Agrega:
     ```
     APPLICATION_SECRET=your-secret-key
     PORT=9000
     ```
4. **Agregar PostgreSQL** (Opcional):
   - Click en "New" → "Database" → "PostgreSQL"
   - Railway conectará automáticamente con `DATABASE_URL`

### Archivo de Configuración (Opcional)

Crea `railway.toml` en la raíz del proyecto:

```toml
[build]
builder = "nixpacks"

[deploy]
startCommand = "target/universal/stage/bin/web -Dhttp.port=$PORT"
restartPolicyType = "on-failure"
```

---

## 🎨 Deployment en Render

Render es otra plataforma moderna con free tier generoso.

### Pasos:

1. **Crear cuenta en Render**: https://render.com
2. **Nuevo Web Service**:
   - Dashboard → "New +" → "Web Service"
   - Conecta tu repositorio de GitHub
   - Configuración:
     - **Name**: reactive-manifesto
     - **Environment**: Docker
     - **Region**: Selecciona el más cercano
     - **Instance Type**: Free (para empezar)
3. **Variables de Entorno**:
   ```
   APPLICATION_SECRET=your-secret-key
   PORT=9000
   PLAY_ENV=prod
   ```
4. **Agregar PostgreSQL** (Opcional):
   - "New +" → "PostgreSQL"
   - Conecta con tu Web Service

### Archivo render.yaml (Opcional)

```yaml
services:
  - type: web
    name: reactive-manifesto
    env: docker
    plan: free
    envVars:
      - key: APPLICATION_SECRET
        generateValue: true
      - key: PORT
        value: 9000
      - key: PLAY_ENV
        value: prod
```

---

## ☁️ Deployment en Google Cloud Run

Cloud Run permite deployar contenedores serverless con escalado automático.

### Pasos:

1. **Instalar Google Cloud SDK**:
   ```bash
   curl https://sdk.cloud.google.com | bash
   gcloud init
   ```

2. **Configurar proyecto**:
   ```bash
   gcloud config set project YOUR_PROJECT_ID
   gcloud auth configure-docker
   ```

3. **Build y Push a Google Container Registry**:
   ```bash
   # Build
   docker build -t gcr.io/YOUR_PROJECT_ID/reactive-manifesto .
   
   # Push
   docker push gcr.io/YOUR_PROJECT_ID/reactive-manifesto
   ```

4. **Deploy a Cloud Run**:
   ```bash
   gcloud run deploy reactive-manifesto \
     --image gcr.io/YOUR_PROJECT_ID/reactive-manifesto \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars APPLICATION_SECRET=your-secret
   ```

5. **Con Cloud SQL (PostgreSQL)**:
   ```bash
   # Crear instancia
   gcloud sql instances create reactive-db \
     --database-version=POSTGRES_14 \
     --tier=db-f1-micro \
     --region=us-central1
   
   # Crear base de datos
   gcloud sql databases create reactive_manifesto \
     --instance=reactive-db
   
   # Deploy con Cloud SQL
   gcloud run deploy reactive-manifesto \
     --image gcr.io/YOUR_PROJECT_ID/reactive-manifesto \
     --add-cloudsql-instances PROJECT_ID:us-central1:reactive-db \
     --set-env-vars DB_URL=jdbc:postgresql://localhost/reactive_manifesto
   ```

---

## 🌐 Deployment en AWS

### Opción 1: AWS Elastic Beanstalk

1. **Instalar EB CLI**:
   ```bash
   pip install awsebcli
   ```

2. **Inicializar y deployar**:
   ```bash
   eb init -p docker reactive-manifesto
   eb create reactive-manifesto-env
   eb setenv APPLICATION_SECRET=your-secret
   eb deploy
   ```

### Opción 2: AWS ECS (Fargate)

1. **Crear ECR Repository**:
   ```bash
   aws ecr create-repository --repository-name reactive-manifesto
   ```

2. **Build y Push**:
   ```bash
   aws ecr get-login-password --region us-east-1 | \
     docker login --username AWS --password-stdin YOUR_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com
   
   docker build -t reactive-manifesto .
   docker tag reactive-manifesto:latest YOUR_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com/reactive-manifesto:latest
   docker push YOUR_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com/reactive-manifesto:latest
   ```

3. **Crear Task Definition y Service** desde AWS Console o CLI.

---

## 🤖 Configuración de GitHub Actions

### 1. Configurar Secretos en GitHub

Ve a tu repositorio → Settings → Secrets and variables → Actions

#### Para Docker Hub:
- `DOCKERHUB_USERNAME`: Tu usuario de Docker Hub
- `DOCKERHUB_TOKEN`: Token de acceso (crear en Docker Hub → Account Settings → Security)

#### Para Heroku:
- `HEROKU_API_KEY`: API Key de Heroku (Settings → Account → API Key)
- `HEROKU_APP_NAME`: Nombre de tu app en Heroku
- `HEROKU_EMAIL`: Tu email de Heroku

### 2. Workflows Disponibles

El repositorio incluye tres workflows de GitHub Actions:

1. **`scala.yml`**: CI - Compila y testea en cada push/PR
2. **`docker-deploy.yml`**: Build y push a Docker Hub en cada push a main
3. **`heroku-deploy.yml`**: Deploy automático a Heroku

### 3. Activar Workflows

Los workflows se activan automáticamente. Para ejecutar manualmente:
- Ve a Actions → Selecciona workflow → Run workflow

---

## 🔐 Variables de Entorno

### Variables Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `APPLICATION_SECRET` | Clave secreta de Play Framework | `changeme-in-production` |
| `PORT` | Puerto HTTP | `9000` |

### Variables Opcionales para Producción

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DATABASE_URL` | URL completa de PostgreSQL | `jdbc:postgresql://host:5432/db` |
| `DB_URL` | URL de base de datos | `jdbc:postgresql://host:5432/db` |
| `DB_USER` | Usuario de base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de base de datos | `secretpassword` |
| `PLAY_ENV` | Entorno de ejecución | `prod` |

### Generar APPLICATION_SECRET

```bash
# Opción 1: OpenSSL
openssl rand -base64 32

# Opción 2: SBT
sbt playGenerateSecret

# Opción 3: Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## 📊 Monitoreo y Logs

### Heroku

```bash
heroku logs --tail --app mi-reactive-manifesto
```

### Docker

```bash
docker logs -f container_name
```

### Railway

Desde el dashboard de Railway, tab "Logs"

### Render

Desde el dashboard de Render, tab "Logs"

---

## 🧪 Testing del Deployment

Una vez deployado, verifica que funciona:

```bash
# Test básico
curl https://tu-app.herokuapp.com/

# Test de API
curl https://tu-app.herokuapp.com/api/contacts

# Test de health (si existe)
curl https://tu-app.herokuapp.com/health
```

---

## 🆘 Troubleshooting

### Error: Application secret not set

```bash
# Configurar en Heroku
heroku config:set APPLICATION_SECRET=$(openssl rand -base64 32)

# O en Docker
docker run -e APPLICATION_SECRET=your-secret ...
```

### Error: Database connection failed

- Verifica que las variables `DB_URL`, `DB_USER`, `DB_PASSWORD` estén configuradas
- Asegúrate de que el addon de PostgreSQL esté activo
- Revisa los logs para detalles específicos

### Error: Port binding failed

- En Heroku, el puerto se asigna automáticamente vía `$PORT`
- En Docker, asegúrate de mapear correctamente: `-p 9000:9000`

---

## 📚 Recursos Adicionales

- [Play Framework Production Configuration](https://www.playframework.com/documentation/latest/ProductionConfiguration)
- [Heroku Scala Support](https://devcenter.heroku.com/articles/scala-support)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Railway Documentation](https://docs.railway.app/)
- [Render Documentation](https://render.com/docs)

---

## 🎯 Recomendaciones

1. **Para empezar rápido**: Usa Railway o Render (free tier generoso)
2. **Para producción**: Heroku Professional o AWS ECS/EKS
3. **Para control total**: Docker + VPS (DigitalOcean, Linode)
4. **Para serverless**: Google Cloud Run o AWS Fargate

---

¿Necesitas ayuda? Abre un issue en GitHub: https://github.com/federicopfund/Reactive-Manifiesto/issues
