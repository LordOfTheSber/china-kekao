# hanzi-writer-data coverage check (TASK-021)

Verifies that every hanzi imported into the database (HSK 1-6 by default) has a
matching stroke-order JSON in the [`hanzi-writer-data`][hwd] npm package, and
flips the `hanzi.has_stroke_data` flag accordingly.

## Run

```bash
cd scripts/hanzi-writer-coverage
npm install
DB_URL=postgres://kekao:kekao@localhost:5432/kekao npm run check
```

Optional env:

- `HSK_LEVELS` — comma-separated levels to inspect (default: `1,2,3,4,5,6`).
- `DRY_RUN=true` — only generate the report, skip the `UPDATE`.

## Output

- `report/coverage.json` — totals, per-HSK-level breakdown, ratio.
- `report/missing.txt` — characters without stroke data, one per line with
  their HSK level. Use this list when curating fallbacks for PRODUCTION cards.

The `hanzi.has_stroke_data` flag is the source of truth at runtime:
PRODUCTION-mode card scheduling should skip drawing for rows where it is
`FALSE` (or fall back to CHOICE mode per TASK-024).

[hwd]: https://www.npmjs.com/package/hanzi-writer-data
