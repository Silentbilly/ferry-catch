SET TIME ZONE 'Europe/Istanbul';

INSERT INTO holidays(day, name) VALUES
  (DATE '2026-01-01', 'New Year’s Day (Yılbaşı)'),
  (DATE '2026-03-19', 'Ramazan Bayramı Eve / Arife (half-day, from 13:00)'),
  (DATE '2026-03-20', 'Ramazan Bayramı (Eid al-Fitr) – Day 1'),
  (DATE '2026-03-21', 'Ramazan Bayramı – Day 2'),
  (DATE '2026-03-22', 'Ramazan Bayramı – Day 3'),
  (DATE '2026-04-23', 'National Sovereignty and Children’s Day'),
  (DATE '2026-05-01', 'Labour and Solidarity Day'),
  (DATE '2026-05-19', 'Commemoration of Atatürk, Youth and Sports Day'),
  (DATE '2026-05-26', 'Kurban Bayramı Eve / Arife (half-day, from 13:00)'),
  (DATE '2026-05-27', 'Kurban Bayramı (Eid al-Adha) – Day 1'),
  (DATE '2026-05-28', 'Kurban Bayramı – Day 2'),
  (DATE '2026-05-29', 'Kurban Bayramı – Day 3'),
  (DATE '2026-05-30', 'Kurban Bayramı – Day 4'),
  (DATE '2026-07-15', 'Democracy and National Unity Day'),
  (DATE '2026-08-30', 'Victory Day'),
  (DATE '2026-10-28', 'Republic Day Eve / Arife (half-day)'),
  (DATE '2026-10-29', 'Republic Day')
ON CONFLICT (day) DO UPDATE SET name = EXCLUDED.name;
