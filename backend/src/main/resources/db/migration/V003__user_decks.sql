-- TASK: support user-created (custom) decks alongside the built-in HSK system
-- decks. Adds an optional owner reference and reworks slug uniqueness so two
-- different users may pick the same slug for their own decks while system
-- decks (owner_id IS NULL) keep a global unique slug.

ALTER TABLE deck
    ADD COLUMN IF NOT EXISTS owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE deck DROP CONSTRAINT IF EXISTS deck_slug_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_deck_slug_system
    ON deck (slug) WHERE owner_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_deck_slug_owner
    ON deck (owner_id, slug) WHERE owner_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_deck_owner ON deck (owner_id);

ALTER TABLE deck
    ADD CONSTRAINT chk_deck_owner_system
    CHECK ((is_system = TRUE AND owner_id IS NULL) OR (is_system = FALSE));
