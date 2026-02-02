-- V8__add_only_sundays_and_holidays_flag.sql

ALTER TABLE trip_templates
  ADD COLUMN only_sundays_and_holidays BOOLEAN NOT NULL DEFAULT FALSE;
