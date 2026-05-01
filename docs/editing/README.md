# Hanzi editing playbook (TASK-007)

Two ways to edit the hanzi catalogue. Both are safe to combine; the SQL
recipes are useful for batch fixes, the HTTP API is the day-to-day editor
interface.

## 1. HTTP admin endpoints (preferred)

All endpoints live under `/api/admin/hanzi` and require `ROLE_ADMIN`.

| Method | Path                                    | Purpose                                |
|--------|-----------------------------------------|----------------------------------------|
| GET    | `/api/admin/hanzi?status=DRAFT&page=0`  | List by status, paginated (size ≤ 200) |
| PUT    | `/api/admin/hanzi/{id}`                 | Edit pinyin, stroke count, HSK level, English meanings |
| POST   | `/api/admin/hanzi/{id}/publish`         | DRAFT/REVIEWED → PUBLISHED (requires non-empty pinyin and ≥1 English meaning) |

`PUT` body:
```json
{
  "pinyin": "nǐ",
  "strokeCount": 7,
  "hskLevel": 1,
  "meaningsEn": ["you (informal)", "you"]
}
```

### Granting admin

Set `KEKAO_ADMIN_EMAIL` in the backend env to the email of an already-registered
user. On startup the bootstrap listener promotes that user to `ROLE_ADMIN` (it
is idempotent and never auto-creates accounts). Alternatively, run the SQL
recipe below.

## 2. Direct SQL (batch / fallback)

Connect to the database with a privileged role. Always wrap edits in a
transaction; the catalogue is not large but mistakes propagate to learners.

```sql
-- Promote a user to admin
UPDATE users
   SET role = 'ROLE_ADMIN', updated_at = NOW()
 WHERE email = 'me@example.com';

-- See drafts pending edits, ordered by HSK level
SELECT h.id, h.character, h.pinyin, h.hsk_level,
       t.meanings AS meanings_en
  FROM hanzi h
  LEFT JOIN hanzi_translation t
         ON t.hanzi_id = h.id AND t.language = 'en'
 WHERE h.status = 'DRAFT'
 ORDER BY h.hsk_level NULLS LAST, h.id;

-- Edit pinyin and English meanings of a single character
BEGIN;
  UPDATE hanzi
     SET pinyin = 'nǐ', stroke_count = 7, hsk_level = 1, updated_at = NOW()
   WHERE character = '你';

  -- Upsert the English translation row.
  INSERT INTO hanzi_translation (hanzi_id, language, meanings, is_primary)
       SELECT id, 'en', ARRAY['you (informal)', 'you'], TRUE
         FROM hanzi
        WHERE character = '你'
  ON CONFLICT (hanzi_id, language)
  DO UPDATE SET meanings = EXCLUDED.meanings,
                is_primary = TRUE;
COMMIT;

-- Publish a single character once it has been reviewed
UPDATE hanzi SET status = 'PUBLISHED', updated_at = NOW()
 WHERE character = '你' AND status IN ('DRAFT', 'REVIEWED');

-- Bulk-publish HSK 1 after editing
UPDATE hanzi
   SET status = 'PUBLISHED', updated_at = NOW()
 WHERE hsk_level = 1
   AND status IN ('DRAFT', 'REVIEWED')
   AND pinyin IS NOT NULL AND pinyin <> ''
   AND id IN (
     SELECT hanzi_id
       FROM hanzi_translation
      WHERE language = 'en'
        AND COALESCE(array_length(meanings, 1), 0) > 0
   );
```

## 3. Importing the starter catalogue

The hanzi importer (TASK-006) is wired as a Spring Boot CLI runner. Download a
CC-CEDICT dump from
[MDBG](https://www.mdbg.net/chinese/dictionary?page=cc-cedict), then trigger
the runner one of two ways:

```bash
# A) Activate the `import` profile.
java -jar china-kekao-backend.jar \
  --spring.profiles.active=import \
  --kekao.import.cedict-path=/data/cedict_ts.u8

# B) Or keep your normal profile and pass an explicit flag.
java -jar china-kekao-backend.jar \
  --kekao.import.run=true \
  --kekao.import.cedict-path=/data/cedict_ts.u8
```

If `--kekao.import.cedict-path` is omitted, the runner falls back to the
bundled `imports/cedict.sample.txt` resource (good for smoke tests, not for
production seeding). All imported rows land in `status = 'DRAFT'` so editors
can review before publishing. The runner logs `Starting hanzi import...` at
INFO when triggered; if you don't see that line, the trigger didn't fire.
