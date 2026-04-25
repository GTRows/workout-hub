-- V18__user_profile_nutrition_goals.sql
-- Per-user daily nutrition targets so the /nutrition page can show
-- "today vs goal" progress. Each column is nullable: a user may set
-- a kcal goal without specifying macro breakdown.

ALTER TABLE user_profile
    ADD COLUMN daily_kcal_goal       INTEGER,
    ADD COLUMN daily_protein_g_goal  INTEGER,
    ADD COLUMN daily_carbs_g_goal    INTEGER,
    ADD COLUMN daily_fat_g_goal      INTEGER;
