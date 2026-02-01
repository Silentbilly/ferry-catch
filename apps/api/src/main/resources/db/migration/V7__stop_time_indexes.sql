ALTER TABLE trip_templates
ALTER COLUMN no_sundays_and_holidays SET DEFAULT FALSE;

UPDATE trip_templates
SET no_sundays_and_holidays = FALSE
WHERE no_sundays_and_holidays IS NULL;

ALTER TABLE trip_templates
ALTER COLUMN no_sundays_and_holidays SET NOT NULL;