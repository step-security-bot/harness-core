BEGIN;

ALTER TABLE service_infra_info ADD COLUMN IF NOT EXISTS infrastructureIdentifier
    text;

COMMIT;