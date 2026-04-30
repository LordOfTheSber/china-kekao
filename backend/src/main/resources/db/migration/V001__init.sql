-- TASK-003: base schema for the China Kekao study application.
-- All enum-like columns are stored as VARCHAR and validated through CHECK
-- constraints so they can be evolved without ALTER TYPE migrations.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- Users and refresh tokens
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(254) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(32)  NOT NULL DEFAULT 'ROLE_USER'
                    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    settings_json   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token_hash   VARCHAR(128) NOT NULL UNIQUE,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked = FALSE;

-- ---------------------------------------------------------------------------
-- Hanzi catalog
-- ---------------------------------------------------------------------------
CREATE TABLE hanzi (
    id              BIGSERIAL PRIMARY KEY,
    character       VARCHAR(8)  NOT NULL UNIQUE,
    pinyin          VARCHAR(64) NOT NULL,
    stroke_count    SMALLINT,
    hsk_level       SMALLINT
                    CHECK (hsk_level IS NULL OR (hsk_level BETWEEN 1 AND 9)),
    frequency_rank  INTEGER,
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'REVIEWED', 'PUBLISHED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hanzi_status_hsk ON hanzi (status, hsk_level);
CREATE INDEX idx_hanzi_character_trgm
    ON hanzi USING GIN (character gin_trgm_ops);
CREATE INDEX idx_hanzi_pinyin_trgm
    ON hanzi USING GIN (pinyin gin_trgm_ops);

CREATE TABLE hanzi_translation (
    id          BIGSERIAL PRIMARY KEY,
    hanzi_id    BIGINT      NOT NULL REFERENCES hanzi(id) ON DELETE CASCADE,
    language    VARCHAR(8)  NOT NULL CHECK (language IN ('en', 'ru')),
    meanings    TEXT[]      NOT NULL,
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (hanzi_id, language)
);

CREATE INDEX idx_hanzi_translation_hanzi ON hanzi_translation (hanzi_id);

CREATE TABLE hanzi_example (
    id          BIGSERIAL PRIMARY KEY,
    hanzi_id    BIGINT      NOT NULL REFERENCES hanzi(id) ON DELETE CASCADE,
    language    VARCHAR(8)  NOT NULL CHECK (language IN ('en', 'ru')),
    sentence    TEXT        NOT NULL,
    pinyin      TEXT        NOT NULL,
    translation TEXT        NOT NULL
);

CREATE INDEX idx_hanzi_example_hanzi ON hanzi_example (hanzi_id);

-- ---------------------------------------------------------------------------
-- Decks
-- ---------------------------------------------------------------------------
CREATE TABLE deck (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    slug        VARCHAR(128) NOT NULL UNIQUE,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    description TEXT
);

CREATE TABLE deck_hanzi (
    deck_id   BIGINT  NOT NULL REFERENCES deck(id)  ON DELETE CASCADE,
    hanzi_id  BIGINT  NOT NULL REFERENCES hanzi(id) ON DELETE CASCADE,
    position  INTEGER NOT NULL,
    PRIMARY KEY (deck_id, hanzi_id)
);

CREATE INDEX idx_deck_hanzi_position ON deck_hanzi (deck_id, position);

CREATE TABLE user_deck (
    user_id        BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    deck_id        BIGINT      NOT NULL REFERENCES deck(id)  ON DELETE CASCADE,
    subscribed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, deck_id)
);

CREATE INDEX idx_user_deck_user ON user_deck (user_id);

-- ---------------------------------------------------------------------------
-- User cards and review log (FSRS)
-- ---------------------------------------------------------------------------
CREATE TABLE user_card (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hanzi_id            BIGINT       NOT NULL REFERENCES hanzi(id) ON DELETE CASCADE,
    mode                VARCHAR(16)  NOT NULL
                        CHECK (mode IN ('RECOGNITION', 'PRODUCTION')),
    state               VARCHAR(16)  NOT NULL DEFAULT 'NEW'
                        CHECK (state IN ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')),
    stability           DOUBLE PRECISION NOT NULL DEFAULT 0,
    difficulty          DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_review         TIMESTAMPTZ,
    due_date            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reps                INTEGER      NOT NULL DEFAULT 0,
    lapses              INTEGER      NOT NULL DEFAULT 0,
    elapsed_days        DOUBLE PRECISION NOT NULL DEFAULT 0,
    scheduled_days      DOUBLE PRECISION NOT NULL DEFAULT 0,
    algorithm_version   VARCHAR(16)  NOT NULL DEFAULT 'FSRS-5',
    UNIQUE (user_id, hanzi_id, mode)
);

CREATE INDEX idx_user_card_due ON user_card (user_id, due_date);

CREATE TABLE review_log (
    id                  BIGSERIAL PRIMARY KEY,
    user_card_id        BIGINT      NOT NULL REFERENCES user_card(id) ON DELETE CASCADE,
    rating              SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 4),
    state_before        VARCHAR(16) NOT NULL
                        CHECK (state_before IN ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')),
    elapsed_days        DOUBLE PRECISION NOT NULL,
    scheduled_days      DOUBLE PRECISION NOT NULL,
    stability_before    DOUBLE PRECISION NOT NULL,
    difficulty_before   DOUBLE PRECISION NOT NULL,
    reviewed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    response_time_ms    INTEGER,
    hint_count          SMALLINT    NOT NULL DEFAULT 0,
    stroke_mistakes     SMALLINT    NOT NULL DEFAULT 0
);

CREATE INDEX idx_review_log_card_time ON review_log (user_card_id, reviewed_at);
CREATE INDEX idx_review_log_time      ON review_log (reviewed_at);
