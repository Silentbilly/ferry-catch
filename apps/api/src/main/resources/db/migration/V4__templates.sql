SET TIME ZONE 'Europe/Istanbul';

CREATE TABLE trip_templates (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  route_id uuid NOT NULL REFERENCES routes(id) ON DELETE CASCADE,

  -- "time of day" in local schedule sense
  departure_local time NOT NULL,
  arrival_local time NOT NULL,

  -- Exceptions (minimal set for your notes)
  no_sundays_and_holidays boolean NOT NULL DEFAULT false,
  active_until date NULL,   -- inclusive (e.g. 2025-11-01)
  active_from date NULL,    -- optional, can be NULL

  CHECK (arrival_local >= departure_local)
);

CREATE INDEX ix_trip_templates_route_departure
  ON trip_templates (route_id, departure_local);

CREATE TABLE stop_time_templates (
  trip_template_id uuid NOT NULL REFERENCES trip_templates(id) ON DELETE CASCADE,
  stop_sequence int NOT NULL,
  stop_name text NOT NULL,
  time_local time NOT NULL,
  PRIMARY KEY (trip_template_id, stop_sequence)
);

CREATE TABLE holidays (
  day date PRIMARY KEY,
  name text NULL
);
