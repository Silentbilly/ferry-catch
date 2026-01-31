-- app/api/src/main/resources/db/migration/V2__seed_demo.sql

-- 1) Operators
INSERT INTO operators (name) VALUES
  ('Mavi Marmara'),
  ('Şehir Hatları')
ON CONFLICT (name) DO NOTHING;

-- 2) Routes
WITH op AS (
  SELECT id, name FROM operators WHERE name IN ('Mavi Marmara', 'Şehir Hatları')
)
INSERT INTO routes ("from", "to", operator_id)
SELECT 'Kınalıada', 'Bostancı', op.id FROM op WHERE op.name = 'Mavi Marmara'
ON CONFLICT DO NOTHING;

WITH op AS (
  SELECT id, name FROM operators WHERE name IN ('Mavi Marmara', 'Şehir Hatları')
)
INSERT INTO routes ("from", "to", operator_id)
SELECT 'Kınalıada', 'Kadıköy', op.id FROM op WHERE op.name = 'Şehir Hatları'
ON CONFLICT DO NOTHING;

-- 3) Trips + Stop times for today (simple demo)
-- We'll create a few trips for each route today.
DO $$
DECLARE
  r_mm uuid;
  r_sh uuid;
  trip_id uuid;
  base_date date := current_date;
  dep timestamptz;
BEGIN
  -- Fetch route ids
  SELECT r.id INTO r_mm
  FROM routes r
  JOIN operators o ON o.id = r.operator_id
  WHERE r."from"='Kınalıada' AND r."to"='Bostancı' AND o.name='Mavi Marmara'
  LIMIT 1;

  SELECT r.id INTO r_sh
  FROM routes r
  JOIN operators o ON o.id = r.operator_id
  WHERE r."from"='Kınalıada' AND r."to"='Kadıköy' AND o.name='Şehir Hatları'
  LIMIT 1;

  -- Route 1: Kınalıada -> Bostancı (3 trips)
  FOREACH dep IN ARRAY ARRAY[
    (base_date::timestamptz + time '09:00'),
    (base_date::timestamptz + time '12:00'),
    (base_date::timestamptz + time '18:00')
  ]
  LOOP
    trip_id := gen_random_uuid();

    INSERT INTO trips (id, route_id, service_date, departure_time, arrival_time)
    VALUES (trip_id, r_mm, base_date, dep, dep + interval '55 minutes');

    INSERT INTO stop_times (trip_id, stop_sequence, stop_name, time) VALUES
      (trip_id, 1, 'Kınalıada', dep),
      (trip_id, 2, 'Burgazada', dep + interval '10 minutes'),
      (trip_id, 3, 'Heybeliada', dep + interval '22 minutes'),
      (trip_id, 4, 'Bostancı', dep + interval '55 minutes');
  END LOOP;

  -- Route 2: Kınalıada -> Kadıköy (3 trips)
  FOREACH dep IN ARRAY ARRAY[
    (base_date::timestamptz + time '08:30'),
    (base_date::timestamptz + time '13:30'),
    (base_date::timestamptz + time '19:30')
  ]
  LOOP
    trip_id := gen_random_uuid();

    INSERT INTO trips (id, route_id, service_date, departure_time, arrival_time)
    VALUES (trip_id, r_sh, base_date, dep, dep + interval '65 minutes');

    INSERT INTO stop_times (trip_id, stop_sequence, stop_name, time) VALUES
      (trip_id, 1, 'Kınalıada', dep),
      (trip_id, 2, 'Büyükada', dep + interval '15 minutes'),
      (trip_id, 3, 'Kadıköy', dep + interval '65 minutes');
  END LOOP;

END $$;
