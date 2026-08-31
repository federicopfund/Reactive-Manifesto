-- ============================================================
-- Trigger: enforce_bronze_sponsor_limit
-- Bronce admite hasta 2 acuerdos ACTIVOS simultáneos por unidad
-- (evento o publicación). Oro y Plata ya se garantizan como
-- máximo 1 activo por unidad mediante índices únicos parciales
-- en la evolution 32 (idx_sponsor_gold_exclusive,
-- idx_sponsor_silver_*_exclusive); un límite de 2 no se puede
-- expresar como índice único, por eso vive en un trigger.
--
-- Se mantiene fuera de las evoluciones de Play porque el parser
-- no soporta PL/pgSQL (dollar-quoting / string literals), igual
-- que trigger_close_previous_stage.sql para la evolution 16.
--
-- Ejecutar DESPUÉS de aplicar la evolución 32:
--   docker exec -i reactive_manifesto_db psql -U reactive_user \
--     -d reactive_manifesto -f /tmp/trigger_sponsor_bronze_limit.sql
-- ============================================================

CREATE OR REPLACE FUNCTION enforce_bronze_sponsor_limit()
RETURNS TRIGGER AS $$
DECLARE
    active_count INT;
BEGIN
    IF NEW.tier = 'bronze' AND NEW.status = 'active' THEN
        SELECT COUNT(*) INTO active_count
        FROM sponsor_agreements
        WHERE tier = 'bronze'
          AND status = 'active'
          AND id != NEW.id
          AND unit_type = NEW.unit_type
          AND (
                (NEW.unit_type = 'event'       AND event_id       = NEW.event_id) OR
                (NEW.unit_type = 'publication'  AND publication_id = NEW.publication_id)
              );

        IF active_count >= 2 THEN
            RAISE EXCEPTION
                'Límite de sponsors Bronce (2 activos) superado para % %',
                NEW.unit_type, COALESCE(NEW.event_id, NEW.publication_id);
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_bronze_sponsor_limit
    BEFORE INSERT OR UPDATE ON sponsor_agreements
    FOR EACH ROW
    EXECUTE FUNCTION enforce_bronze_sponsor_limit();
