-- V8__exercise_bilingual.sql
-- PHASE 2 introduces bilingual content. description_tr already exists; add
-- description_en. Rename form_tips and common_mistakes to their _tr
-- counterparts and add _en siblings. Empty-array defaults keep existing
-- rows valid without a data backfill.

ALTER TABLE exercises ADD COLUMN description_en TEXT;

ALTER TABLE exercises RENAME COLUMN form_tips TO form_tips_tr;
ALTER TABLE exercises ADD COLUMN form_tips_en TEXT[] NOT NULL DEFAULT '{}';

ALTER TABLE exercises RENAME COLUMN common_mistakes TO common_mistakes_tr;
ALTER TABLE exercises ADD COLUMN common_mistakes_en TEXT[] NOT NULL DEFAULT '{}';
