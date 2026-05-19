-- Composite index covering user-scoped state/due filters used throughout
-- UserCardRepository (state=, state<>, due_date<=, ORDER BY due_date).
CREATE INDEX IF NOT EXISTS idx_user_card_user_state_due
    ON user_card (user_id, state, due_date);
DROP INDEX IF EXISTS idx_user_card_due;

-- Denormalize user_id onto review_log so per-user counts/streak/accuracy
-- queries no longer need to join through user_card.
ALTER TABLE review_log
    ADD COLUMN IF NOT EXISTS user_id BIGINT
        REFERENCES users(id) ON DELETE CASCADE;

UPDATE review_log rl
    SET user_id = uc.user_id
    FROM user_card uc
    WHERE uc.id = rl.user_card_id
      AND rl.user_id IS NULL;

ALTER TABLE review_log ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_review_log_user_time
    ON review_log (user_id, reviewed_at DESC);

-- Partial index for the rare "system decks" listing.
CREATE INDEX IF NOT EXISTS idx_deck_system
    ON deck (id)
    WHERE is_system = TRUE;

-- Used by uniqueSlugForOwner prefix lookups.
CREATE INDEX IF NOT EXISTS idx_deck_owner_slug
    ON deck (owner_id, slug);
