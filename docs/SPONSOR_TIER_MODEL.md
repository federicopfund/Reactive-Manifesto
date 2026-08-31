# Modelo de negocio: Sponsors por tier (Oro / Plata / Bronce)

Este documento evalúa el esquema de sponsors introducido en la evolution 31
(`conf/evolutions/default/31.sql`) y define el modelo de negocio definitivo,
implementado en la evolution 32 (`conf/evolutions/default/32.sql`).

## 1. Evaluación de la evolution 31

La 31 sentó una base sólida (catálogo `sponsors`, contratos
`sponsor_agreements`, entregables `sponsor_deliverables`, workflow de
estados), pero no reflejaba el modelo de tiers real:

| Problema en 31 | Efecto |
|---|---|
| `unit_type` solo tenía `season/collection/event/newsletter` | No existía forma de auspiciar una **publicación** puntual — el pilar de contenido más frecuente del sitio. |
| Oro se modelaba igual que Plata/Bronce (un acuerdo por unidad) | Contradice "Oro auspicia todo el contenido": un sponsor Oro necesitaría N acuerdos (uno por temporada, uno por colección, uno por evento...) para lograr lo que debería ser un único paquete. |
| La exclusividad de Oro/Plata era solo un comentario ("enforcement en app") | Sin restricción en la base de datos, un bug de aplicación podía activar dos sponsors Oro a la vez — justo el privilegio que no se puede violar. |
| No había regla que impidiera que un Bronce comprara "temporada" o un Silver comprara "colección" | El límite de alcance por tier no estaba garantizado en ningún nivel. |

La evolution 32 corrige los cuatro puntos sin romper lo existente (la tabla
`sponsor_agreements` está vacía hasta ahora — no hay backfill que resolver).

## 2. El modelo de tiers

| Tier | Auspicia | Alcance | Concurrencia |
|---|---|---|---|
| **Oro** | Temporadas + Colecciones + Eventos + Publicaciones + Newsletter (**todo**) | Sitio completo, un único acuerdo `platform` | **Exclusivo: 1 activo en toda la plataforma** |
| **Plata** | Publicaciones + Eventos + Newsletter | Por unidad (una publicación, un evento, o el newsletter) | 1 activo por unidad |
| **Bronce** | Eventos + Publicaciones | Por unidad | Hasta 2 activos por unidad |

La idea clave: **el tier no es "elegí qué patrocino", es "qué tan grande es
el paquete y qué tan exclusivo es"**. Oro no elige una temporada — compra la
presencia de marca en cada superficie del sitio a la vez, y paga un precio
premium justamente porque **nadie más puede tener ese lugar mientras dura su
contrato**.

### 2.1 Por qué Oro debe ser un único acuerdo `platform`

Si Oro se modelara "por unidad" (como Plata/Bronce), un solo sponsor Oro
necesitaría abrir y mantener un acuerdo por cada temporada, colección y
evento — y cada vez que se crea contenido nuevo quedaría sin cubrir hasta
que alguien recuerde firmar un nuevo acuerdo. Eso rompe la promesa de "todo
el contenido".

En su lugar, `sponsor_agreements` gana `unit_type = 'platform'`: un acuerdo
sin FK a ninguna unidad concreta (igual que `newsletter`, que ya no
referenciaba nada). Ese acuerdo es la fuente de verdad de "quién es el
sponsor Oro vigente"; la capa de aplicación lo consulta una sola vez (con
caché de request) para pintar el badge en header, footer, temporadas,
colecciones, eventos y publicaciones — sin necesidad de sincronizar N filas.

### 2.2 Cómo se garantiza la exclusividad de Oro

```sql
CREATE UNIQUE INDEX idx_sponsor_gold_exclusive
    ON sponsor_agreements(tier)
    WHERE tier = 'gold' AND status = 'active';
```

Es un índice único parcial: solo indexa filas con `tier = 'gold' AND status
= 'active'`, y como esas filas comparten siempre el mismo valor (`'gold'`),
Postgres rechaza la segunda fila que intente cumplir esa condición. No es
una convención de aplicación — es una invariante que la base de datos hace
cumplir sí o sí, incluso si un bug o un admin intentan saltarla.

Esto también habilita **rotación de Oro en el tiempo** sin conflicto: la
Empresa A puede ser Oro de enero a junio (`completed`), y la Empresa B desde
julio (`active`) — nunca dos `active` a la vez, pero sí historial completo
para reporting.

### 2.3 Concurrencia de Plata y Bronce

- **Plata**: 1 acuerdo activo por evento / por publicación / y uno para el
  newsletter — garantizado con tres índices únicos parciales análogos al de
  Oro (`idx_sponsor_silver_event_exclusive`,
  `idx_sponsor_silver_publication_exclusive`,
  `idx_sponsor_silver_newsletter_exclusive`).
- **Bronce**: hasta 2 activos por unidad. Un límite de "hasta N" (N > 1) no
  se puede expresar como índice único, así que se aplica con un trigger
  (`sql/trigger_sponsor_bronze_limit.sql`) que cuenta los acuerdos Bronce
  activos de la misma unidad antes de aceptar uno nuevo. Se mantiene fuera
  de las evolutions de Play por la misma razón que el trigger de
  `publication_stage_history` (evolution 16): el parser de evolutions no
  soporta PL/pgSQL con dollar-quoting.

### 2.4 El alcance por tier también se garantiza en la base

```sql
ALTER TABLE sponsor_agreements ADD CONSTRAINT chk_agreement_tier_scope CHECK (
    (tier = 'gold'   AND unit_type = 'platform') OR
    (tier = 'silver' AND unit_type IN ('event', 'newsletter', 'publication')) OR
    (tier = 'bronze' AND unit_type IN ('event', 'publication'))
);
```

Un intento de vender "Bronce + temporada" o "Plata + colección" falla al
insertar el acuerdo — no depende de que el código de la app lo valide bien
cada vez.

## 3. Visibilidad en cada interfaz

Para que el usuario asocie cada pieza de contenido con su sponsor
("sensibilizar... a visualizar la empresa que lo auspicia en cada Topic"),
cada tabla de contenido tiene sus propios campos de sponsoreo (denormalizados
para no pagar un join en cada render):

| Tabla | Campos (desde evolution 31/32) |
|---|---|
| `editorial_seasons` | `sponsor_id`, `sponsor_label`, `sponsor_show_public` |
| `collections` | `sponsor_id`, `sponsor_label`, `sponsor_show_public` |
| `community_events` | `sponsor_id`, `sponsor_label`, `sponsor_show_public` |
| `publications` | `sponsor_id`, `sponsor_label`, `sponsor_show_public` *(nuevo en 32)* |
| Newsletter / sitio completo | Sin columna propia: se resuelve con `SELECT ... FROM sponsor_agreements WHERE tier='gold' AND status='active'` (siempre a lo sumo 1 fila) |

`sponsor_show_public` permite activar el acuerdo internamente (`status =
'active'`) sin mostrar el badge todavía (por ejemplo, mientras se preparan
los assets de marca) — separa "vigente comercialmente" de "visible al
público".

## 4. Entregables (deliverables) por tier

`sponsor_deliverables` ya modelaba el catálogo de beneficios; se agregan dos
tipos exclusivos de Oro para las superficies "todo el sitio":

| Tier | Deliverables típicos |
|---|---|
| Oro | `homepage_banner`, `site_header_badge`, `newsletter_mention`, `season_cover`, `collection_cover`, `event_logo`, `event_slot`, `article_footer`, `case_study`, `metrics_report` |
| Plata | `newsletter_mention`, `event_logo`, `event_slot`, `article_footer` |
| Bronce | `event_logo`, `article_footer` |

## 5. Ciclo de vida de un acuerdo (sin cambios respecto a 31)

```
draft → negotiating → signed → active → completed | cancelled
```

Al pasar a `active` es cuando el índice de exclusividad (Oro/Plata) o el
trigger (Bronce) validan el cupo disponible; si no hay lugar, la transacción
falla y el admin ve el error al intentar activar el acuerdo, no después.

## 6. Escalabilidad

- **Nueva superficie de contenido** (ej. podcast, video): agregar el valor
  al `CHECK` de `unit_type`, la FK correspondiente, y decidir a qué tiers se
  la asigna en `chk_agreement_tier_scope` — no requiere tocar sponsors ni
  agreements existentes.
- **Cambiar el límite de Bronce** (2 → 3): es un solo número dentro del
  trigger, no una migración de esquema.
- **Un cuarto tier futuro** (ej. "Platino" por encima de Oro, o "Comunidad"
  gratuito con visibilidad mínima): se agrega al `CHECK` de `tier` en
  `sponsors`/`sponsor_agreements` y se define su fila en
  `chk_agreement_tier_scope`.
- El modelo separa con claridad **catálogo** (`sponsors`), **contrato**
  (`sponsor_agreements`) y **cumplimiento** (`sponsor_deliverables`), lo que
  permite reportar métricas (ingresos por tier, entregables pendientes,
  ocupación de cupos Bronce) sin acoplar la lógica comercial a las tablas de
  contenido.

## 7. Pendiente (fuera del alcance de esta evolution)

El esquema no tiene todavía capa de aplicación: no existen modelos Scala,
repositorios, rutas, controladores ni vistas para sponsors (`grep -r sponsor
app/` no devuelve nada). Próximos pasos sugeridos, en orden:

1. `app/domains/sponsors/models` — `Sponsor`, `SponsorAgreement`,
   `SponsorDeliverable` (siguiendo la convención de otros dominios, ej.
   `app/domains/events/models`).
2. `app/domains/sponsors/repositories` — altas/consultas, con el helper
   `findActiveGold(): Future[Option[SponsorAgreement]]` como punto de
   entrada único para el badge de sitio completo.
3. Admin UI (`app/views/admin`) para gestionar el workflow de acuerdos y
   marcar deliverables como entregados.
4. Partial de vista reutilizable (`app/views/partials`) para el badge de
   sponsor, parametrizado por tier, usado en seasons/collections/events/
   publications/newsletter.
5. Job periódico (o trigger a la activación) que regenere los
   `sponsor_deliverables` de Oro cuando se publica contenido nuevo
   (temporada, colección, evento) mientras el acuerdo Oro sigue `active`.

## 8. Aplicar los cambios

```bash
# 1. Aplicar evolution 32 (Play la aplica automáticamente al levantar la app,
#    o manualmente vía el runner de evolutions habitual del proyecto).

# 2. Crear el trigger de límite de Bronce (fuera de las evolutions de Play):
docker exec -i reactive_manifesto_db psql -U reactive_user \
  -d reactive_manifesto -f /tmp/trigger_sponsor_bronze_limit.sql
```
