-- TASK: achievements / master-seals catalog and per-user unlocks.

CREATE TABLE achievement (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(64)  NOT NULL UNIQUE,
    name          VARCHAR(128) NOT NULL,
    description   TEXT         NOT NULL,
    glyph         VARCHAR(8)   NOT NULL,
    category      VARCHAR(16)  NOT NULL
                  CHECK (category IN ('LEARNED', 'STREAK', 'REVIEWS', 'SPECIAL')),
    threshold     BIGINT,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_achievement (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  BIGINT      NOT NULL REFERENCES achievement(id) ON DELETE CASCADE,
    unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

CREATE INDEX idx_user_achievement_user ON user_achievement (user_id);

-- Seed: master seals tied to learned-total / streak / review-count thresholds, plus a few specials.
INSERT INTO achievement (code, name, description, glyph, category, threshold, sort_order) VALUES
('LEARNED_25',    'First batch',      'Reach 25 cards in long-term review.', '廿五', 'LEARNED', 25,    10),
('LEARNED_50',    'Fifty strokes',    'Reach 50 cards in long-term review.', '五十', 'LEARNED', 50,    20),
('LEARNED_100',   'Centurion',        'Reach 100 cards in long-term review.','百',   'LEARNED', 100,   30),
('LEARNED_250',   'Quarter master',   'Reach 250 cards in long-term review.','二五', 'LEARNED', 250,   40),
('LEARNED_500',   'Five hundred',     'Reach 500 cards in long-term review.','五百', 'LEARNED', 500,   50),
('LEARNED_1000',  'Thousand strokes', 'Reach 1000 cards in long-term review.','千',  'LEARNED', 1000,  60),
('STREAK_3',      'Three in a row',   'Maintain a 3-day streak.',            '叁',   'STREAK',  3,     100),
('STREAK_7',      'Diligent week',    'Maintain a 7-day streak.',            '勤',   'STREAK',  7,     110),
('STREAK_30',     'Scholar',          'Maintain a 30-day streak.',           '学霸', 'STREAK',  30,    120),
('STREAK_88',     'Fortune',          'Maintain an 88-day streak.',          '发',   'STREAK',  88,    130),
('STREAK_100',    'Perseverance',     'Maintain a 100-day streak.',          '恒',   'STREAK',  100,   140),
('STREAK_365',    'A whole year',     'Maintain a 365-day streak.',          '年',   'STREAK',  365,   150),
('REVIEWS_100',   'Hundred reviews',  'Complete 100 reviews.',               '百',   'REVIEWS', 100,   200),
('REVIEWS_1000',  'Thousand reviews', 'Complete 1000 reviews.',              '千',   'REVIEWS', 1000,  210),
('REVIEWS_10000', 'Ten thousand',     'Complete 10000 reviews.',             '万',   'REVIEWS', 10000, 220),
('PERFECT_DAY',   'Marvellous',       'Finish a session with no Again or Hard.', '妙', 'SPECIAL', NULL, 300),
('NIGHT_OWL',     'Night owl',        'Study between midnight and 5 am.',    '月',   'SPECIAL', NULL,  310),
('EARLY_BIRD',    'Early bird',       'Study between 5 am and 8 am.',        '晨',   'SPECIAL', NULL,  320),
('KONAMI',        'Hidden arcade',    'Discover the secret cheat code.',     '游',   'SPECIAL', NULL,  400),
('PANDA',         'Panda whisperer',  'Find the hidden panda.',              '猫',   'SPECIAL', NULL,  410);
