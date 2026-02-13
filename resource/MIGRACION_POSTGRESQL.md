# 🗄️ Migración a PostgreSQL - Guía Rápida

## 📋 Cambios Realizados

✅ Actualizado `application.conf` → PostgreSQL
✅ Agregado driver PostgreSQL en `build.sbt`  
✅ Creado `docker-compose.yml`

---

## 🚀 Opción 1: PostgreSQL con Docker (RECOMENDADO)

### 1️⃣ Inicia PostgreSQL

```bash
cd /workspaces/Reactive-Manifiesto
docker-compose up -d
```

✅ Esto inicia PostgreSQL en background
- **Host**: localhost:5432
- **BD**: reactive_manifesto
- **Usuario**: reactive_user
- **Contraseña**: reactive_password

### 2️⃣ Verifica que esté disponible

```bash
docker-compose ps
```

Debería mostrar:
```
CONTAINER ID   IMAGE              STATUS
...            postgres:16-alpine  Up (healthy)
```

### 3️⃣ Inicia la aplicación

```bash
cd /workspaces/Reactive-Manifiesto
sbt run
```

✅ Las **Evolutions** correrán automáticamente
✅ Tablas se crearán automáticamente

### 4️⃣ Accede a la app

```
http://localhost:9000
```

---

## 🛑 Detener PostgreSQL

```bash
docker-compose down
```

**Nota**: Los datos persisten en `postgres_data/` volume

---

## 🧹 Limpiar TODO (reiniciar desde cero)

```bash
# Detiene y elimina BD
docker-compose down -v

# Inicia fresca
docker-compose up -d
```

---

## 🔧 Opción 2: PostgreSQL Instalado Localmente

Si no quieres usar Docker:

### macOS
```bash
brew install postgresql@16
brew services start postgresql@16
```

### Linux (Debian/Ubuntu)
```bash
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Windows
Descarga de: https://www.postgresql.org/download/windows/

### Crear BD y usuario

```bash
psql -U postgres

postgres=# CREATE DATABASE reactive_manifesto;
postgres=# CREATE USER reactive_user WITH PASSWORD 'reactive_password';
postgres=# ALTER ROLE reactive_user SET client_encoding TO 'utf8';
postgres=# ALTER ROLE reactive_user SET default_transaction_isolation TO 'read committed';
postgres=# ALTER ROLE reactive_user SET default_transaction_deferrable TO on;
postgres=# ALTER ROLE reactive_user SET default_transaction_read_only TO off;
postgres=# GRANT ALL PRIVILEGES ON DATABASE reactive_manifesto TO reactive_user;
postgres=# \q
```

Luego ejecuta:
```bash
cd /workspaces/Reactive-Manifiesto
sbt run
```

---

## 📊 Verificar Conexión

```bash
# Conectar a la BD
psql -h localhost -U reactive_user -d reactive_manifesto

# Ver tablas (dentro de psql)
\dt

# Salir
\q
```

---

## 🔄 Cambiar BD en Cualquier Momento

### Volver a H2 (En Memoria)

En `conf/application.conf`:

```properties
# Para desarrollo rápido (todo se resetea)
slick.dbs.default {
  profile = "slick.jdbc.H2Profile$"
  db {
    driver = "org.h2.Driver"
    url = "jdbc:h2:mem:reactivedb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"
    user = "sa"
    password = ""
  }
}
```

### Volver a PostgreSQL

```properties
# Para producción (datos persisten)
slick.dbs.default {
  profile = "slick.jdbc.PostgresProfile$"
  db {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/reactive_manifesto"
    user = "reactive_user"
    password = "reactive_password"
  }
}
```

---

## ✅ Checklist Post-Migración

- [ ] PostgreSQL corriendo (`docker-compose ps` o verificar puerto 5432)
- [ ] `sbt run` inicia sin errores
- [ ] Accede a http://localhost:9000
- [ ] Puedes crear usuario y hacer login
- [ ] Los datos persisten después de reiniciar la app
- [ ] Admin puede crear publicaciones
- [ ] Admin puede aprobar/rechazar publicaciones

---

## 🐛 Troubleshooting

### Error: "Connection refused"
```
PostgreSQL no está corriendo
→ docker-compose up -d
→ Espera 5 segundos para que levante
```

### Error: "Database reactive_manifesto does not exist"
```
La BD no se creó automáticamente
→ docker-compose down -v
→ docker-compose up -d
→ sbt run
```

### Error: "LiquibaseFailedException"
```
Las migrations (evolutions) fallan
→ Probablemente por cambios en esquema
→ Borra BD y reinicia: docker-compose down -v
```

### Los datos desaparecen después de `docker-compose down`
```
✅ ES NORMAL si no guardas datos
Para preservar: no uses "down -v"
→ docker-compose down    (sin -v, preserva datos)
→ docker-compose up -d   (recupera datos)
```

---

## 🎯 Resultado

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| **Persistencia** | ❌ Se resetea | ✅ Persiste |
| **Reinicio app** | Todo se borra | Datos intactos |
| **Ambiente** | Dev | Dev + Prod-ready |
| **Base datos** | H2 en memoria | PostgreSQL real |

✅ **¡Ya está lista!** Los datos ahora persisten entre reinicios.
