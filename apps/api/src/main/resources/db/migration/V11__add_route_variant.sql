UPDATE routes
SET variant = 'via Kınalıada'
WHERE "from" = 'Bostancı'
  AND "to" = 'Bostancı'
  AND variant IS NULL;

ALTER TABLE routes
  DROP CONSTRAINT IF EXISTS routes_from_to_operator_id_key;

ALTER TABLE routes
  ADD CONSTRAINT routes_variant_required_for_loops
  CHECK (
    ("from" <> "to")
    OR
    ("from" = "to" AND variant IS NOT NULL AND btrim(variant) <> '')
  );

CREATE UNIQUE INDEX IF NOT EXISTS routes_uq_no_variant
  ON routes ("from", "to", operator_id)
  WHERE variant IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS routes_uq_with_variant
  ON routes ("from", "to", operator_id, variant)
  WHERE variant IS NOT NULL;
