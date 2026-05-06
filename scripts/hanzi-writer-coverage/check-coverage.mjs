#!/usr/bin/env node
// TASK-021: hanzi-writer-data coverage report.
//
// 1. Reads every hanzi in the database (defaults to HSK 1-6, status = PUBLISHED
//    or DRAFT — anything that could be drilled in PRODUCTION mode).
// 2. For each character, tries to load the matching JSON from the
//    `hanzi-writer-data` npm package.
// 3. Updates the `has_stroke_data` flag in bulk.
// 4. Writes a coverage report (JSON + plain-text missing list) into ./report/.
//
// Usage:
//   npm install
//   DB_URL=postgres://kekao:kekao@localhost:5432/kekao node check-coverage.mjs
//
// Optional env:
//   HSK_LEVELS=1,2,3,4,5,6  (default)
//   DRY_RUN=true            (skip the UPDATE, only write the report)

import { createRequire } from "node:module";
import { mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import pg from "pg";

const require = createRequire(import.meta.url);
const __dirname = dirname(fileURLToPath(import.meta.url));

const HSK_LEVELS = (process.env.HSK_LEVELS ?? "1,2,3,4,5,6")
    .split(",")
    .map((s) => Number.parseInt(s.trim(), 10))
    .filter((n) => Number.isFinite(n));

const DRY_RUN = ["1", "true", "yes"].includes(
    String(process.env.DRY_RUN ?? "").toLowerCase(),
);

const DB_URL =
    process.env.DB_URL ??
    "postgres://kekao:kekao@localhost:5432/kekao";

function hasStrokeData(character) {
    try {
        const data = require(`hanzi-writer-data/${character}.json`);
        return Boolean(data && Array.isArray(data.strokes) && data.strokes.length > 0);
    } catch (err) {
        if (err && err.code === "MODULE_NOT_FOUND") {
            return false;
        }
        throw err;
    }
}

async function main() {
    const client = new pg.Client({ connectionString: DB_URL });
    await client.connect();
    try {
        const { rows } = await client.query(
            `SELECT id, character, hsk_level
               FROM hanzi
              WHERE hsk_level = ANY($1::int[])
              ORDER BY hsk_level, character`,
            [HSK_LEVELS],
        );

        const covered = [];
        const missing = [];
        for (const row of rows) {
            (hasStrokeData(row.character) ? covered : missing).push(row);
        }

        if (!DRY_RUN && rows.length > 0) {
            await client.query("BEGIN");
            await client.query(
                "UPDATE hanzi SET has_stroke_data = TRUE WHERE id = ANY($1::bigint[])",
                [covered.map((r) => r.id)],
            );
            await client.query(
                "UPDATE hanzi SET has_stroke_data = FALSE WHERE id = ANY($1::bigint[])",
                [missing.map((r) => r.id)],
            );
            await client.query("COMMIT");
        }

        const reportDir = resolve(__dirname, "report");
        await mkdir(reportDir, { recursive: true });

        const summary = {
            generatedAt: new Date().toISOString(),
            hskLevels: HSK_LEVELS,
            total: rows.length,
            covered: covered.length,
            missing: missing.length,
            coverageRatio: rows.length === 0 ? 1 : covered.length / rows.length,
            byHskLevel: summarizeByLevel(rows, missing),
            dryRun: DRY_RUN,
        };

        await writeFile(
            resolve(reportDir, "coverage.json"),
            JSON.stringify(summary, null, 2) + "\n",
            "utf8",
        );
        await writeFile(
            resolve(reportDir, "missing.txt"),
            missing.map((r) => `${r.character}\tHSK${r.hsk_level}`).join("\n") + "\n",
            "utf8",
        );

        console.log(
            `hanzi-writer-data coverage: ${covered.length}/${rows.length} ` +
                `(${(summary.coverageRatio * 100).toFixed(2)}%)` +
                (DRY_RUN ? " [dry-run, DB not updated]" : ""),
        );
        if (missing.length > 0) {
            console.log(`Missing characters written to ${resolve(reportDir, "missing.txt")}`);
        }
    } finally {
        await client.end();
    }
}

function summarizeByLevel(all, missing) {
    const totals = new Map();
    const missed = new Map();
    for (const row of all) {
        totals.set(row.hsk_level, (totals.get(row.hsk_level) ?? 0) + 1);
    }
    for (const row of missing) {
        missed.set(row.hsk_level, (missed.get(row.hsk_level) ?? 0) + 1);
    }
    const result = {};
    for (const [level, total] of [...totals.entries()].sort((a, b) => a[0] - b[0])) {
        const miss = missed.get(level) ?? 0;
        result[`hsk${level}`] = { total, covered: total - miss, missing: miss };
    }
    return result;
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
