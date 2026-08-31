# --- Modelo de negocio de sponsors: tiers Oro/Plata/Bronce con alcance de contenido
# ---
# --- Ajusta la evolution 31 para reflejar las reglas de negocio definitivas:
# ---
# ---  1. Oro   -> auspicia TODO el contenido (temporadas, colecciones, eventos,
# ---              publicaciones, newsletter) mediante un único acuerdo "platform".
# ---              EXCLUSIVO: solo puede haber un acuerdo Oro activo en toda la
# ---              plataforma al mismo tiempo (privilegio de exclusividad).
# ---  2. Plata -> auspicia publicaciones, eventos y newsletter (NO temporadas
# ---              ni colecciones). Un acuerdo Plata activo por unidad como máximo.
# ---  3. Bronce -> auspicia eventos y publicaciones únicamente. Hasta 2 acuerdos
# ---              Bronce activos por unidad (enforcement vía trigger, ver
# ---              sql/trigger_sponsor_bronze_limit.sql).
# ---
# --- Cambios:
# ---  - sponsor_agreements: nuevo unit_type 'publication' (+ FK publication_id)
# ---    y 'platform' (acuerdo Oro, sin FK a ninguna unidad concreta).
# ---  - chk_agreement_tier_scope: fija qué unit_type puede usar cada tier.
# ---  - Índices únicos parciales que garantizan la exclusividad de Oro y el
# ---    límite de 1 acuerdo Plata activo por unidad (Bronce vía trigger externo).
# ---  - publications gana sponsor_id/sponsor_label/sponsor_show_public, igual
# ---    que editorial_seasons/collections/community_events en la evolution 31.
# ---  - sponsor_deliverables gana tipos de entregable exclusivos de Oro
# ---    (homepage_banner, site_header_badge) para la vidriera de "todo el sitio".

# --- !Ups

-- ============================================================
-- 1. SPONSOR_AGREEMENTS — nuevo unit_type 'publication' y 'platform'
-- ============================================================

ALTER TABLE sponsor_agreements
    ADD COLUMN IF NOT EXISTS publication_id BIGINT REFERENCES publications(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_agreements_publication
    ON sponsor_agreements(publication_id) WHERE publication_id IS NOT NULL;

ALTER TABLE sponsor_agreements
    DROP CONSTRAINT IF EXISTS chk_agreement_unit_type;

ALTER TABLE sponsor_agreements
    ADD CONSTRAINT chk_agreement_unit_type
        CHECK (unit_type IN ('season', 'collection', 'event', 'newsletter', 'publication', 'platform'));

ALTER TABLE sponsor_agreements
    DROP CONSTRAINT IF EXISTS chk_unit_coherence;

-- Exactamente una FK de unidad definida según unit_type.
-- 'newsletter' y 'platform' no referencian ninguna unidad concreta:
-- newsletter es un único producto; platform es "todo el sitio".
ALTER TABLE sponsor_agreements
    ADD CONSTRAINT chk_unit_coherence CHECK (
        (unit_type = 'season'      AND season_id      IS NOT NULL AND collection_id IS NULL AND event_id IS NULL AND publication_id IS NULL) OR
        (unit_type = 'collection'  AND collection_id  IS NOT NULL AND season_id     IS NULL AND event_id IS NULL AND publication_id IS NULL) OR
        (unit_type = 'event'       AND event_id       IS NOT NULL AND season_id     IS NULL AND collection_id IS NULL AND publication_id IS NULL) OR
        (unit_type = 'publication' AND publication_id IS NOT NULL AND season_id     IS NULL AND collection_id IS NULL AND event_id IS NULL) OR
        (unit_type = 'newsletter'  AND season_id IS NULL AND collection_id IS NULL AND event_id IS NULL AND publication_id IS NULL) OR
        (unit_type = 'platform'    AND season_id IS NULL AND collection_id IS NULL AND event_id IS NULL AND publication_id IS NULL)
    );

-- Regla de negocio central: qué unit_type puede usar cada tier.
-- Oro solo vende el paquete "platform" (todo el contenido a la vez).
-- Plata: publicaciones + eventos + newsletter. Bronce: eventos + publicaciones.
ALTER TABLE sponsor_agreements
    ADD CONSTRAINT chk_agreement_tier_scope CHECK (
        (tier = 'gold'   AND unit_type = 'platform') OR
        (tier = 'silver' AND unit_type IN ('event', 'newsletter', 'publication')) OR
        (tier = 'bronze' AND unit_type IN ('event', 'publication'))
    );

-- ============================================================
-- 2. EXCLUSIVIDAD — índices únicos parciales
-- ============================================================

-- Privilegio de exclusividad de Oro: como máximo UN acuerdo Oro
-- activo en TODA la plataforma al mismo tiempo (no por unidad).
CREATE UNIQUE INDEX IF NOT EXISTS idx_sponsor_gold_exclusive
    ON sponsor_agreements(tier)
    WHERE tier = 'gold' AND status = 'active';

-- Plata: un acuerdo activo como máximo por evento / publicación / newsletter.
CREATE UNIQUE INDEX IF NOT EXISTS idx_sponsor_silver_event_exclusive
    ON sponsor_agreements(event_id)
    WHERE tier = 'silver' AND unit_type = 'event' AND status = 'active';

CREATE UNIQUE INDEX IF NOT EXISTS idx_sponsor_silver_publication_exclusive
    ON sponsor_agreements(publication_id)
    WHERE tier = 'silver' AND unit_type = 'publication' AND status = 'active';

CREATE UNIQUE INDEX IF NOT EXISTS idx_sponsor_silver_newsletter_exclusive
    ON sponsor_agreements(unit_type)
    WHERE tier = 'silver' AND unit_type = 'newsletter' AND status = 'active';

-- Bronce (hasta 2 activos por unidad) NO se puede expresar con un índice
-- único parcial (no es un límite de 1). Se aplica con un trigger definido
-- fuera de las evoluciones de Play: ver sql/trigger_sponsor_bronze_limit.sql
-- (Play no soporta PL/pgSQL con dollar-quoting, igual que en la evolution 16).

-- ============================================================
-- 3. PUBLICATIONS — campos de sponsoreo (igual que seasons/collections/events)
-- ============================================================

ALTER TABLE publications
    ADD COLUMN IF NOT EXISTS sponsor_id          BIGINT  REFERENCES sponsors(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS sponsor_label       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sponsor_show_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_publications_sponsor
    ON publications(sponsor_id) WHERE sponsor_id IS NOT NULL;

-- ============================================================
-- 4. SPONSOR_DELIVERABLES — superficies exclusivas de Oro (todo el sitio)
-- ============================================================

ALTER TABLE sponsor_deliverables
    DROP CONSTRAINT IF EXISTS chk_deliverable_type;

ALTER TABLE sponsor_deliverables
    ADD CONSTRAINT chk_deliverable_type CHECK (
        deliverable_type IN (
            'newsletter_mention', 'season_cover', 'article_footer',
            'event_logo', 'event_slot', 'case_study', 'metrics_report',
            'collection_cover', 'opening_essay_mention',
            'homepage_banner', 'site_header_badge'
        )
    );

# --- !Downs

ALTER TABLE sponsor_deliverables
    DROP CONSTRAINT IF EXISTS chk_deliverable_type;

ALTER TABLE sponsor_deliverables
    ADD CONSTRAINT chk_deliverable_type CHECK (
        deliverable_type IN (
            'newsletter_mention', 'season_cover', 'article_footer',
            'event_logo', 'event_slot', 'case_study', 'metrics_report',
            'collection_cover', 'opening_essay_mention'
        )
    );

DROP INDEX IF EXISTS idx_publications_sponsor;

ALTER TABLE publications
    DROP COLUMN IF EXISTS sponsor_show_public,
    DROP COLUMN IF EXISTS sponsor_label,
    DROP COLUMN IF EXISTS sponsor_id;

DROP INDEX IF EXISTS idx_sponsor_silver_newsletter_exclusive;
DROP INDEX IF EXISTS idx_sponsor_silver_publication_exclusive;
DROP INDEX IF EXISTS idx_sponsor_silver_event_exclusive;
DROP INDEX IF EXISTS idx_sponsor_gold_exclusive;

ALTER TABLE sponsor_agreements
    DROP CONSTRAINT IF EXISTS chk_agreement_tier_scope;

ALTER TABLE sponsor_agreements
    DROP CONSTRAINT IF EXISTS chk_unit_coherence;

ALTER TABLE sponsor_agreements
    ADD CONSTRAINT chk_unit_coherence CHECK (
        (unit_type = 'season'     AND season_id     IS NOT NULL AND collection_id IS NULL AND event_id IS NULL) OR
        (unit_type = 'collection' AND collection_id IS NOT NULL AND season_id     IS NULL AND event_id IS NULL) OR
        (unit_type = 'event'      AND event_id      IS NOT NULL AND season_id     IS NULL AND collection_id IS NULL) OR
        (unit_type = 'newsletter' AND season_id IS NULL AND collection_id IS NULL AND event_id IS NULL)
    );

ALTER TABLE sponsor_agreements
    DROP CONSTRAINT IF EXISTS chk_agreement_unit_type;

ALTER TABLE sponsor_agreements
    ADD CONSTRAINT chk_agreement_unit_type
        CHECK (unit_type IN ('season', 'collection', 'event', 'newsletter'));

DROP INDEX IF EXISTS idx_agreements_publication;

ALTER TABLE sponsor_agreements
    DROP COLUMN IF EXISTS publication_id;
