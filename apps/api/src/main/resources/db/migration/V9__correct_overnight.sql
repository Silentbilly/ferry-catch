ALTER TABLE trip_templates
  DROP CONSTRAINT trip_templates_check;

ALTER TABLE trip_templates
  ADD CONSTRAINT trip_templates_check
  CHECK (arrival_local <> departure_local);
