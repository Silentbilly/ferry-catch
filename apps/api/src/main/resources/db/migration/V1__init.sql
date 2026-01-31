-- UUID генератор (gen_random_uuid)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Operators directory
CREATE TABLE operators (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL UNIQUE
);

-- 2) Routes (logical routes)
CREATE TABLE routes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "from" text NOT NULL,
  "to" text NOT NULL,
  operator_id uuid NOT NULL REFERENCES operators(id) ON DELETE RESTRICT
);

-- Uniqueness: same from/to/operator should not repeat
CREATE UNIQUE INDEX ux_routes_from_to_operator
  ON routes ("from", "to", operator_id);

-- Useful for filtering routes list by from/to/operator
CREATE INDEX ix_routes_from_to
  ON routes ("from", "to");

CREATE INDEX ix_routes_operator
  ON routes (operator_id);

-- 3) Trips (a concrete departure/arrival)
-- service_date is the "timetable date" (yyyy-MM-dd) in local calendar sense.
CREATE TABLE trips (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  route_id uuid NOT NULL REFERENCES routes(id) ON DELETE CASCADE,

  service_date date NOT NULL,

  departure_time timestamptz NOT NULL,
  arrival_time timestamptz NOT NULL,

  CHECK (arrival_time >= departure_time)
);

-- For /next: find next (and next-next) trips fast
CREATE INDEX ix_trips_route_departure_time
  ON trips (route_id, departure_time);

-- For /timetable: day + route ordered by time
CREATE INDEX ix_trips_service_date_route_departure
  ON trips (service_date, route_id, departure_time);

-- 4) Stop times within a trip
CREATE TABLE stop_times (
  trip_id uuid NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
  stop_sequence int NOT NULL,
  stop_name text NOT NULL,
  time timestamptz NOT NULL,
  PRIMARY KEY (trip_id, stop_sequence)
);

CREATE INDEX ix_stop_times_trip_sequence
  ON stop_times (trip_id, stop_sequence);
