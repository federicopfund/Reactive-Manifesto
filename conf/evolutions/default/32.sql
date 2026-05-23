# --- Issue #24 — Campos de sponsoreo para publicaciones individuales
# ---
# --- Permite asignar un patrocinador a una publicación / artículo específico.
# --- El badge se muestra según el tierDefault del sponsor (gold/silver/bronze).
# ---
# --- sponsor_id          → FK a sponsors (nullable)
# --- sponsor_label       → etiqueta personalizada (ej: "Con el apoyo de")
# --- sponsor_show_public → toggle de visibilidad pública

# --- !Ups

ALTER TABLE publications
    ADD COLUMN IF NOT EXISTS sponsor_id          BIGINT  REFERENCES sponsors(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS sponsor_label       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sponsor_show_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_publications_sponsor
    ON publications(sponsor_id) WHERE sponsor_id IS NOT NULL;

# --- !Downs

DROP INDEX IF EXISTS idx_publications_sponsor;

ALTER TABLE publications
    DROP COLUMN IF EXISTS sponsor_show_public,
    DROP COLUMN IF EXISTS sponsor_label,
    DROP COLUMN IF EXISTS sponsor_id;
