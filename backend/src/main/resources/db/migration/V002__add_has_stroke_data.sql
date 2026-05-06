-- TASK-021: track whether a hanzi has stroke-order data available in
-- hanzi-writer-data. The flag is updated by the coverage script in
-- scripts/hanzi-writer-coverage; defaults to FALSE so untouched rows are
-- treated as "no data" until proven otherwise.
ALTER TABLE hanzi
    ADD COLUMN has_stroke_data BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_hanzi_has_stroke_data ON hanzi (has_stroke_data);
