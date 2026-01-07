# 🎯 Quick Start - Deployment Guide

Esta es una guía rápida para deployar Reactive Manifesto en menos de 5 minutos.

## ⚡ Opción 1: Heroku (Más Rápido)

```bash
# 1. Instalar Heroku CLI
curl https://cli-assets.heroku.com/install.sh | sh

# 2. Login
heroku login

# 3. Crear app
heroku create mi-reactive-app

# 4. Configurar variables
heroku config:set APPLICATION_SECRET=$(openssl rand -base64 32)

# 5. (Opcional) Agregar PostgreSQL
heroku addons:create heroku-postgresql:mini

# 6. Deploy
git push heroku main

# 7. Abrir
heroku open
```

**¡Listo! Tu app está en producción en ~3 minutos.**

---

## 🐳 Opción 2: Docker (Local o Servidor)

```bash
# 1. Build
docker build -t reactive-manifesto .

# 2. Run
docker run -d -p 9000:9000 \
  -e APPLICATION_SECRET=$(openssl rand -base64 32) \
  --name reactive-app \
  reactive-manifesto

# 3. Acceder
open http://localhost:9000
```

**Con PostgreSQL:**

```bash
# Usar docker-compose (incluye PostgreSQL)
docker-compose up -d

# Ver logs
docker-compose logs -f app
```

---

## 🚂 Opción 3: Railway (Gratis + Simple)

1. Ve a https://railway.app
2. "New Project" → "Deploy from GitHub"
3. Selecciona tu repo
4. Agrega variables:
   - `APPLICATION_SECRET`: (auto-generado)
   - `PORT`: `9000`
5. ¡Deploy automático!

---

## 🎨 Opción 4: Render (Free Tier)

1. Ve a https://render.com
2. "New +" → "Web Service"
3. Conecta GitHub repo
4. Environment: **Docker**
5. Variables:
   - `APPLICATION_SECRET`: (genera uno)
   - `PORT`: `9000`
6. Create Web Service

---

## 🔧 Variables de Entorno Requeridas

```bash
# Mínimo requerido
APPLICATION_SECRET=tu-secret-aqui-generalo-con-openssl

# Opcional (para PostgreSQL)
DATABASE_URL=postgres://user:pass@host:5432/db
```

---

## 🧪 Verificar que Funciona

```bash
# Test básico
curl https://tu-app.com/

# Debería retornar HTML de la página principal
```

---

## 📱 URLs de Acceso

- **Local**: http://localhost:9000
- **Heroku**: https://tu-app.herokuapp.com
- **Railway**: https://tu-app.up.railway.app
- **Render**: https://tu-app.onrender.com

---

## 🆘 Problemas Comunes

### "Application secret not set"
```bash
heroku config:set APPLICATION_SECRET=$(openssl rand -base64 32)
```

### Puerto ocupado (local)
```bash
# Matar proceso en puerto 9000
fuser -k 9000/tcp
# o
lsof -ti:9000 | xargs kill -9
```

### Build falla
```bash
# Limpiar y rebuild
sbt clean
docker build --no-cache -t reactive-manifesto .
```

---

## 📚 Más Información

Para opciones avanzadas de deployment, consulta [DEPLOYMENT.md](DEPLOYMENT.md)

- AWS deployment
- Google Cloud Run
- Kubernetes
- CI/CD con GitHub Actions
- Configuración de producción
- Monitoreo y logs
