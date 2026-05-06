ALTER TABLE hanzi
    ADD COLUMN has_stroke_data BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_hanzi_hsk_stroke_data ON hanzi (hsk_level, has_stroke_data);
