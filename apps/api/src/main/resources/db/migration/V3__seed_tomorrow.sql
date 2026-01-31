SET TIME ZONE 'Europe/Istanbul'; -- affects current_date and time math in this migration [web:1239]

DO $$
DECLARE
  r_id uuid;
  trip_id uuid;
  dep timestamptz;
  tomorrow date := (current_date + 1);
BEGIN
  -- Example: tomorrow trips for all existing routes (simple)
  FOR r_id IN SELECT id FROM routes
  LOOP
    -- 08:00 tomorrow
    dep := (tomorrow::timestamptz + time '08:00');
    trip_id := gen_random_uuid();

    INSERT INTO trips (id, route_id, service_date, departure_time, arrival_time)
    VALUES (trip_id, r_id, tomorrow, dep, dep + interval '60 minutes');

    -- Minimal stop_times placeholder (you can replace with real sequences later)
    INSERT INTO stop_times (trip_id, stop_sequence, stop_name, time)
    VALUES
      (trip_id, 1, 'START', dep),
      (trip_id, 2, 'END', dep + interval '60 minutes');
  END LOOP;
END $$;
