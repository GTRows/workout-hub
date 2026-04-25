-- V24__streak_freeze.sql
-- Streak freeze grants the user one missed day per calendar month
-- without breaking their current run. We store the year-month it was
-- last spent on the profile row; null means the freeze is available.

ALTER TABLE user_profile
    ADD COLUMN streak_freeze_used_month VARCHAR(7);
