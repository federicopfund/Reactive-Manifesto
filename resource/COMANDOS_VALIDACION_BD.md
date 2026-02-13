# 🔧 Comandos de Validación - PostgreSQL Setup

## 📋 QUICK COPY-PASTE VALIDATION CHECKLIST

### PASO 1: Verificar Docker está corriendo

```bash
docker --version
```

**Esperado**: `Docker version 20.x.x` o superior

---

### PASO 2: PostgreSQL Contenedor

```bash
# Ver si está corriendo
docker ps | grep postgres

# Debe mostrar algo como:
# d19a5a190cf0  postgres:16-alpine  Up (healthy)  0.0.0.0:5432->5432/tcp
```

**Esperado**: Estado `Up (healthy)`

```bash
# Ver logs
docker logs reactive_manifesto_db | tail -20
```

**Esperado**: Sin errores, "database system is ready to accept connections"

---

### PASO 3: Conectar a PostgreSQL

```bash
# Test conexión desde host
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT version();"
```

**Esperado**:
```
version                                
────────────────────────────────────────
PostgreSQL 16.11 on x86_64-pc-linux-musl...
```

---

### PASO 4: Listar Tablas

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "\dt"
```

**Esperado**: 6 tablas
```
 Schema |           Name           | Type  | Owner
────────┼──────────────────────────┼───────┼──────
 public | admins                   | table | reactive_user
 public | contacts                 | table | reactive_user
 public | email_verification_codes | table | reactive_user
 public | play_evolutions          | table | reactive_user
 public | publications             | table | reactive_user
 public | users                    | table | reactive_user
```

---

### PASO 5: Estructura USERS

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "\d users"
```

**Esperado**: Columnas: id, username, email, password_hash, full_name, role, is_active, created_at, last_login, email_verified

---

### PASO 6: Estructura PUBLICATIONS

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "\d publications"
```

**Esperado**: Columnas: id, user_id, title, slug, content, status, reviewed_by, reviewed_at, rejection_reason, published_at, created_at, updated_at

---

### PASO 7: Contar Admins

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT COUNT(*) as total_admins FROM admins;"
```

**Esperado**: 
```
 total_admins
──────────────
            2
```

(1 default + 1 que creaste)

---

### PASO 8: Ver Migraciones Ejecutadas

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT id, state, applied_at FROM play_evolutions ORDER BY id;"
```

**Esperado**: 6 filas con `state = 'applied'`
```
 id | state  |        applied_at
────┼────────┼──────────────────────
  1 | applied | 2026-02-12 22:43:10
  2 | applied | 2026-02-12 22:43:10
  3 | applied | 2026-02-12 22:43:10
  4 | applied | 2026-02-12 22:43:10
  5 | applied | 2026-02-12 22:43:10
  6 | applied | 2026-02-12 22:43:10
```

---

### PASO 9: Indices en USERS

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "\d users" | grep "Indexes:" -A 10
```

**Esperado**: 
```
Indexes:
    "users_pkey" PRIMARY KEY, btree (id)
    "idx_users_email" btree (email)
    "idx_users_is_active" btree (is_active)
    "idx_users_username" btree (username)
    "users_email_key" UNIQUE CONSTRAINT, btree (email)
    "users_username_key" UNIQUE CONSTRAINT, btree (username)
```

---

### PASO 10: Verificar Foreign Keys

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT constraint_name, table_name, column_name FROM information_schema.constraint_column_usage WHERE table_name='publications';"
```

**Esperado**: Relaciones FK activas
```
    constraint_name     | table_name |  column_name
────────────────────────┼────────────┼───────────────
 fk_publication_user    | publications | user_id
```

---

### PASO 11: App Play Framework Corriendo

```bash
# Ver proceso en puerto 9000
lsof -i :9000
```

**Esperado**:
```
COMMAND   PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
java    12345 user  123u  IPv6      0t0      0  TCP *:9000 (LISTEN)
```

**O:**

```bash
# Iniciar app
cd /workspaces/Reactive-Manifiesto && sbt run
```

**Esperado**: Mensaje final
```
(Server started, use Enter to stop and go back to the console...)
```

---

### PASO 12: Acceso a la App

```bash
# Test HTTP
curl http://localhost:9000/

# Esperado: HTML de página de login (status 200)
curl -s http://localhost:9000/ | head -20
```

---

### PASO 13: Test Login Admin

```bash
# Verificar admin en BD
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT username, email FROM admins WHERE username='admin' OR username='federico';"
```

**Esperado**:
```
 username  |           email
───────────┼──────────────────────────────
 admin     | admin@reactivemanifesto.com
 federico  | tu@email.com
```

---

### PASO 14: Test Crear Usuario

```bash
# En la app, registrar usuario y verificar en BD
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT username, email, role FROM users LIMIT 5;"
```

**Esperado**: Usuarios que registraste
```
 username |    email     | role
──────────┼──────────────┼──────
 testuser | test@test.com | user
```

---

### PASO 15: Verificar Persistencia

```bash
# 1. Contar usuarios
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT COUNT(*) as total_users FROM users;"

# Nota el número

# 2. Detener app
# (Presiona Ctrl+C si está corriendo)

# 3. Detener solo app, no BD
docker-compose pause postgres

# 4. Reanudar
docker-compose unpause postgres

# 5. Contar nuevamente
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT COUNT(*) as total_users FROM users;"

# Debe ser el MISMO número - datos persisten ✅
```

**Esperado**: Mismo count antes y después

---

## 🚨 TROUBLESHOOTING COMMANDS

### Si BD no conecta

```bash
# Reiniciar BD
docker-compose restart postgres

# Esperar 5 segundos
sleep 5

# Probar conexión
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT 1;"
```

---

### Si quieres borrar TODO y empezar de cero

```bash
# CUIDADO: Esto borra toda la BD
cd /workspaces/Reactive-Manifiesto && \
docker-compose down -v

# Reiniciar
docker-compose up -d

# Esperar 10 segundos
sleep 10

# Verificar fresh
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "\dt"
# Debe estar vacía
```

---

### Si Play Framework no conecta a BD

```bash
# Ver logs de Play
cd /workspaces/Reactive-Manifiesto && sbt run 2>&1 | grep -i "postgres\|error\|database"

# Esperado: "database system is ready"
```

---

### Ver todos los datos en USERS

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT id, username, email, role, email_verified, created_at FROM users ORDER BY created_at DESC;"
```

---

### Ver todas las PUBLICACIONES

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT id, user_id, title, status, created_at FROM publications ORDER BY created_at DESC;"
```

---

### Ver SIZE de BD

```bash
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c \
"SELECT pg_database.datname, pg_size_pretty(pg_database_size(pg_database.datname)) as size FROM pg_database WHERE datname='reactive_manifesto';"
```

**Esperado**: 
```
      datname      |  size
──────────────────┼────────
 reactive_manifesto | 7264kB
```

---

## 📊 VALIDACIÓN COMPLETA EN UNA LÍNEA

```bash
# Script de validación completa
bash -c '
echo "=== DOCKER ===" && \
docker ps | grep postgres && \
echo -e "\n=== VERSION POSTGRES ===" && \
cd /workspaces/Reactive-Manifiesto && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT version();" && \
echo -e "\n=== TABLAS ===" && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "\dt" && \
echo -e "\n=== ADMINS ===" && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT COUNT(*) FROM admins;" && \
echo -e "\n=== USERS ===" && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT COUNT(*) FROM users;" && \
echo -e "\n=== EVOLUTIONS ===" && \
docker-compose exec -T postgres psql -U reactive_user -d reactive_manifesto -c "SELECT COUNT(*) FROM play_evolutions WHERE state=\"applied\";" && \
echo -e "\n✅ TODO VALIDADO!"
'
```

---

## 📱 ACCESO FINAL

```bash
# URL
http://localhost:9000

# Login Test
# Usuario: admin
# Contraseña: admin123

# O
# Usuario: federico
# Contraseña: Fede/(40021)
```

---

**Ejecuta cada comando uno por uno y pega la salida para validar. 🚀**
