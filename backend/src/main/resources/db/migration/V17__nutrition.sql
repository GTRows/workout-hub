-- V17__nutrition.sql
-- Two tables:
--   * food_items: shared catalog of common Turkish foods with macros per
--     100g. Admins can add more; users don't own rows here.
--   * nutrition_entries: per-user log of what was actually eaten. A
--     serving_g scaler lets the row record "150g of X" without mutating
--     the catalog.

CREATE TABLE food_items (
    id                UUID PRIMARY KEY,
    name_tr           VARCHAR(120) NOT NULL,
    name_en           VARCHAR(120) NOT NULL,
    kcal_per_100g     NUMERIC(6,1) NOT NULL,
    protein_g         NUMERIC(5,1) NOT NULL,
    carbs_g           NUMERIC(5,1) NOT NULL,
    fat_g             NUMERIC(5,1) NOT NULL,
    default_serving_g NUMERIC(6,1) NOT NULL DEFAULT 100,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_food_items_name_tr_lower
    ON food_items (LOWER(name_tr) varchar_pattern_ops);
CREATE INDEX idx_food_items_name_en_lower
    ON food_items (LOWER(name_en) varchar_pattern_ops);

CREATE TABLE nutrition_entries (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL
                     REFERENCES users(id) ON DELETE CASCADE,
    food_id      UUID         NOT NULL
                     REFERENCES food_items(id) ON DELETE RESTRICT,
    serving_g    NUMERIC(6,1) NOT NULL,
    consumed_at  TIMESTAMPTZ  NOT NULL,
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_nutrition_entries_user_date
    ON nutrition_entries (user_id, consumed_at DESC);

INSERT INTO food_items (id, name_tr, name_en, kcal_per_100g, protein_g, carbs_g, fat_g, default_serving_g) VALUES
  (gen_random_uuid(), 'Beyaz pirinc pilav',              'White rice cooked',        130, 2.4,  28.0, 0.3,  150),
  (gen_random_uuid(), 'Bulgur pilavi',                   'Bulgur cooked',            123, 3.1,  25.5, 0.4,  150),
  (gen_random_uuid(), 'Makarna',                         'Pasta cooked',             157, 5.5,  30.0, 0.9,  150),
  (gen_random_uuid(), 'Ekmek beyaz',                     'Bread white',              265, 9.0,  49.0, 3.2,  50),
  (gen_random_uuid(), 'Ekmek tam bugday',                'Bread whole wheat',        247, 13.0, 41.0, 3.4,  50),
  (gen_random_uuid(), 'Yumurta haslanmis',               'Egg boiled',               155, 13.0, 1.1,  11.0, 50),
  (gen_random_uuid(), 'Tavuk gogsu pismis',              'Chicken breast cooked',    165, 31.0, 0.0,  3.6,  150),
  (gen_random_uuid(), 'Tavuk but pismis',                'Chicken thigh cooked',     209, 26.0, 0.0,  10.9, 150),
  (gen_random_uuid(), 'Kirmizi et kiyma',                'Ground beef cooked',       254, 26.0, 0.0,  17.0, 150),
  (gen_random_uuid(), 'Balik somon izgara',              'Salmon grilled',           208, 22.0, 0.0,  13.0, 150),
  (gen_random_uuid(), 'Hamsi tava',                      'Anchovy pan-fried',        210, 20.0, 5.0,  12.0, 120),
  (gen_random_uuid(), 'Ton baligi konserve',             'Tuna canned in water',     116, 26.0, 0.0,  1.0,  100),
  (gen_random_uuid(), 'Mercimek corbasi',                'Lentil soup',              90,  5.0,  14.0, 1.5,  250),
  (gen_random_uuid(), 'Nohut haslanmis',                 'Chickpeas cooked',         164, 9.0,  27.0, 2.6,  150),
  (gen_random_uuid(), 'Fasulye kuru',                    'Dry beans cooked',         127, 8.7,  23.0, 0.5,  150),
  (gen_random_uuid(), 'Barbunya',                        'Kidney beans cooked',      127, 8.7,  22.8, 0.5,  150),
  (gen_random_uuid(), 'Yogurt tam yagli',                'Yogurt full fat',          61,  3.5,  4.7,  3.3,  200),
  (gen_random_uuid(), 'Yogurt suzme',                    'Strained yogurt',          97,  10.0, 3.6,  5.0,  200),
  (gen_random_uuid(), 'Ayran',                           'Ayran',                    37,  1.9,  2.6,  2.0,  250),
  (gen_random_uuid(), 'Sut tam yagli',                   'Milk full fat',            60,  3.2,  4.8,  3.3,  250),
  (gen_random_uuid(), 'Beyaz peynir',                    'White cheese',             264, 17.0, 2.0,  21.0, 30),
  (gen_random_uuid(), 'Kasar peyniri',                   'Kashar cheese',            365, 25.0, 2.0,  28.0, 30),
  (gen_random_uuid(), 'Zeytin siyah',                    'Black olives',             115, 0.8,  6.3,  10.7, 30),
  (gen_random_uuid(), 'Zeytinyagi',                      'Olive oil',                884, 0.0,  0.0,  100.0, 10),
  (gen_random_uuid(), 'Tereyagi',                        'Butter',                   717, 0.9,  0.1,  81.0, 10),
  (gen_random_uuid(), 'Findik',                          'Hazelnut',                 628, 15.0, 17.0, 61.0, 30),
  (gen_random_uuid(), 'Ceviz',                           'Walnut',                   654, 15.0, 14.0, 65.0, 30),
  (gen_random_uuid(), 'Badem',                           'Almond',                   579, 21.0, 22.0, 50.0, 30),
  (gen_random_uuid(), 'Fistik',                          'Peanut',                   567, 26.0, 16.0, 49.0, 30),
  (gen_random_uuid(), 'Salatalik',                       'Cucumber',                 15,  0.7,  3.6,  0.1,  150),
  (gen_random_uuid(), 'Domates',                         'Tomato',                   18,  0.9,  3.9,  0.2,  150),
  (gen_random_uuid(), 'Marul',                           'Lettuce',                  15,  1.4,  2.9,  0.2,  100),
  (gen_random_uuid(), 'Sogan',                           'Onion',                    40,  1.1,  9.3,  0.1,  50),
  (gen_random_uuid(), 'Sarimsak',                        'Garlic',                   149, 6.4,  33.0, 0.5,  5),
  (gen_random_uuid(), 'Patates haslanmis',               'Potato boiled',            87,  1.9,  20.1, 0.1,  200),
  (gen_random_uuid(), 'Tatli patates',                   'Sweet potato',             86,  1.6,  20.1, 0.1,  150),
  (gen_random_uuid(), 'Havuc',                           'Carrot',                   41,  0.9,  9.6,  0.2,  80),
  (gen_random_uuid(), 'Ispanak',                         'Spinach',                  23,  2.9,  3.6,  0.4,  100),
  (gen_random_uuid(), 'Brokoli',                         'Broccoli',                 34,  2.8,  7.0,  0.4,  150),
  (gen_random_uuid(), 'Karnabahar',                      'Cauliflower',              25,  1.9,  5.0,  0.3,  150),
  (gen_random_uuid(), 'Elma',                            'Apple',                    52,  0.3,  14.0, 0.2,  150),
  (gen_random_uuid(), 'Muz',                             'Banana',                   89,  1.1,  23.0, 0.3,  120),
  (gen_random_uuid(), 'Portakal',                        'Orange',                   43,  0.9,  11.2, 0.2,  150),
  (gen_random_uuid(), 'Cilek',                           'Strawberry',               32,  0.7,  7.7,  0.3,  120),
  (gen_random_uuid(), 'Uzum',                            'Grape',                    69,  0.7,  18.1, 0.2,  120),
  (gen_random_uuid(), 'Karpuz',                          'Watermelon',               30,  0.6,  7.6,  0.2,  250),
  (gen_random_uuid(), 'Bal',                             'Honey',                    304, 0.3,  82.0, 0.0,  20),
  (gen_random_uuid(), 'Cay',                             'Tea (plain)',              1,   0.0,  0.3,  0.0,  200),
  (gen_random_uuid(), 'Kahve sade',                      'Coffee plain',             2,   0.3,  0.0,  0.0,  200),
  (gen_random_uuid(), 'Protein tozu whey',               'Whey protein powder',      400, 80.0, 8.0,  5.0,  30);
