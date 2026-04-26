# --- Issue #23 — Campos editoriales de temporada para backoffice

# --- !Ups

ALTER TABLE editorial_seasons
    ADD COLUMN IF NOT EXISTS tagline VARCHAR(255);

ALTER TABLE editorial_seasons
    ADD COLUMN IF NOT EXISTS opening_essay TEXT;

# --- !Downs

ALTER TABLE editorial_seasons DROP COLUMN IF EXISTS opening_essay;
ALTER TABLE editorial_seasons DROP COLUMN IF EXISTS tagline;
