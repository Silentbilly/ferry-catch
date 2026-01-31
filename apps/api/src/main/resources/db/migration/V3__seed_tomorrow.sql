SET TIME ZONE 'Europe/Istanbul';

DO $$
DECLARE
  r_id uuid;
  trip_id uuid;
  dep timestamptz;
  tomorrow date := (current_date + 1);
BEGIN
  FOR r_id IN SELECT id FROM routes
  LOOP
    dep := (tomorrow::timestamptz + time '08:00');
    trip_id := gen_random_uuid();

    INSERT INTO trips (id, route_id, service_date, departure_time, arrival_time)
    VALUES (trip_id, r_id, tomorrow, dep, dep + interval '60 minutes');

    INSERT INTO stop_times (trip_id, stop_sequence, stop_name, time)
    VALUES
      (trip_id, 1, 'START', dep),
      (trip_id, 2, 'END', dep + interval '60 minutes');
  END LOOP;
END $$;
