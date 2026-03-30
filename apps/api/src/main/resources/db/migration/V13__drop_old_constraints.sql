-- Drop old constraints
ALTER TABLE routes DROP CONSTRAINT IF EXISTS uq_routes_from_to_operator_variant;
ALTER TABLE routes DROP CONSTRAINT IF EXISTS uq_routes_from_to_operator;
ALTER TABLE routes DROP CONSTRAINT IF EXISTS ux_routes_from_to_operator_variant;
ALTER TABLE routes DROP CONSTRAINT IF EXISTS ux_routes_from_to_operator;

-- Drop old indexes
DROP INDEX IF EXISTS uq_routes_from_to_operator_variant;
DROP INDEX IF EXISTS uq_routes_from_to_operator;
DROP INDEX IF EXISTS ux_routes_from_to_operator_variant;
DROP INDEX IF EXISTS ux_routes_from_to_operator;

-- Optional but recommended
ALTER TABLE routes DROP CONSTRAINT IF EXISTS chk_routes_loop_variant;

ALTER TABLE routes
ADD CONSTRAINT chk_routes_loop_variant
CHECK ("from" <> "to" OR variant IS NOT NULL);

-- Correct uniqueness model
CREATE UNIQUE INDEX IF NOT EXISTS uq_routes_no_variant
ON routes ("from", "to", operator_id)
WHERE variant IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_routes_with_variant
ON routes ("from", "to", operator_id, variant)
WHERE variant IS NOT NULL;