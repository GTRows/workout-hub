-- V22__achievements.sql
-- Achievements are static catalog rows seeded here so the evaluator has
-- known thresholds and codes to match against. user_achievements records
-- per-user unlocks; the (user_id, achievement_id) unique constraint
-- enforces idempotency cheaply at the DB level.

CREATE TABLE achievements (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL UNIQUE,
    name_tr         TEXT         NOT NULL,
    name_en         TEXT         NOT NULL,
    description_tr  TEXT,
    description_en  TEXT,
    icon            VARCHAR(32),
    rule_type       VARCHAR(32)  NOT NULL,
    threshold       INTEGER      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_achievements (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  UUID         NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    progress_value  INTEGER,
    UNIQUE (user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user
    ON user_achievements (user_id, unlocked_at DESC);

INSERT INTO achievements (id, code, name_tr, name_en, description_tr, description_en, icon, rule_type, threshold) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'first-workout',  'Ilk antrenman',           'First workout',
     'Ilk antrenmanini bitirdin. Hos geldin.', 'Finished your first workout. Welcome.',
     'trophy',  'session_count',  1),
    ('a0000001-0000-0000-0000-000000000002', 'ten-workouts',   '10 antrenman',            '10 workouts',
     '10 antrenman bitirildi. Sebatkar.', 'Crossed 10 finished workouts.',
     'trophy',  'session_count',  10),
    ('a0000001-0000-0000-0000-000000000003', 'fifty-workouts', '50 antrenman',            '50 workouts',
     '50 antrenman. Bu artik bir aliskanlik.', '50 finished workouts; this is now a habit.',
     'trophy',  'session_count',  50),
    ('a0000001-0000-0000-0000-000000000004', 'hundred-workouts','100 antrenman',          '100 workouts',
     'Yuz antrenman. Bambaska bir lig.', '100 finished workouts. Different league.',
     'trophy',  'session_count',  100),
    ('a0000001-0000-0000-0000-000000000005', 'streak-7',       '7 gunluk seri',           '7-day streak',
     '7 gun ust uste antrenman.', 'Seven consecutive workout days.',
     'flame',   'streak_days',    7),
    ('a0000001-0000-0000-0000-000000000006', 'streak-30',      '30 gunluk seri',          '30-day streak',
     '30 gun ust uste antrenman.', '30 consecutive workout days.',
     'flame',   'streak_days',    30),
    ('a0000001-0000-0000-0000-000000000007', 'first-pr',       'Ilk PR',                  'First PR',
     'Ilk kisisel rekorun.', 'Your first personal record.',
     'medal',   'pr_count',       1),
    ('a0000001-0000-0000-0000-000000000008', 'ten-prs',        '10 PR',                   '10 PRs',
     '10 kisisel rekor. Devam et.', 'Ten personal records and counting.',
     'medal',   'pr_count',       10),
    ('a0000001-0000-0000-0000-000000000009', 'volume-1000',    '1000 kg toplam hacim',    '1000 kg total volume',
     '1000 kg toplam kaldirma. Iyi baslangic.', '1000 kg total weight lifted. Good start.',
     'muscle',  'volume_kg',      1000),
    ('a0000001-0000-0000-0000-000000000010', 'volume-10000',   '10000 kg toplam hacim',   '10000 kg total volume',
     '10000 kg toplam kaldirma. Bu lig ust seviye.', '10000 kg total. Top tier.',
     'muscle',  'volume_kg',      10000);
