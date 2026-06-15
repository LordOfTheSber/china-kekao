#!/usr/bin/env node
// Builds the on-demand "Romance of the Three Kingdoms" reader content for the
// frontend, and refreshes the backend character-import resources via the
// reusable corpus importer.
//
// Outputs:
//   frontend/public/three-kingdoms/manifest.json
//   frontend/public/three-kingdoms/chapter-NNN.json  (one per 回)
//   backend/src/main/resources/imports/three-kingdoms.txt        (via importCorpus)
//   backend/src/main/resources/imports/three-kingdoms.cedict.txt (via importCorpus)
//
// Usage:  node scripts/import-data/build-three-kingdoms.mjs
//
// Idempotent: re-running regenerates the same deterministic files.

import { writeFile, mkdir, rm } from "node:fs/promises";
import { join } from "node:path";

import { REPO_ROOT, importCorpus, isSingleHanzi } from "./import-corpus.mjs";

const SOURCE_URL =
  "https://raw.githubusercontent.com/tennessine/corpus/master/%E4%B8%89%E5%9B%BD%E6%BC%94%E4%B9%89.txt";
const READER_DIR = join(REPO_ROOT, "frontend", "public", "three-kingdoms");

const CHAPTER_MARKER = /第[一二三四五六七八九十百零〇两]+回/g;
const SENTENCE_TERMINATORS = /[。！？]/;

function splitChapters(text) {
  const markers = [...text.matchAll(CHAPTER_MARKER)];
  const chapters = [];
  for (let i = 0; i < markers.length; i += 1) {
    const start = markers[i].index;
    const end = i + 1 < markers.length ? markers[i + 1].index : text.length;
    const block = text.slice(start, end);
    const newline = block.search(/[\r\n]/);
    const heading = (newline >= 0 ? block.slice(0, newline) : block).trim();
    const body = newline >= 0 ? block.slice(newline) : "";
    chapters.push({ number: i + 1, title: heading, sentences: splitSentences(body) });
  }
  return chapters;
}

function splitSentences(body) {
  const normalized = body.replace(/\s+/g, "");
  const out = [];
  let current = "";
  for (const ch of normalized) {
    current += ch;
    if (SENTENCE_TERMINATORS.test(ch)) {
      pushSentence(out, current);
      current = "";
    }
  }
  pushSentence(out, current);
  return out;
}

function pushSentence(sink, sentence) {
  const trimmed = sentence.trim();
  if (trimmed && [...trimmed].some(isSingleHanzi)) {
    sink.push(trimmed);
  }
}

function chapterFileName(number) {
  return `chapter-${String(number).padStart(3, "0")}.json`;
}

async function writeReaderFiles(chapters) {
  await rm(READER_DIR, { recursive: true, force: true });
  await mkdir(READER_DIR, { recursive: true });

  const manifest = [];
  for (const chapter of chapters) {
    const file = chapterFileName(chapter.number);
    const payload = {
      chapter: chapter.number,
      title: chapter.title,
      texts: chapter.sentences.map((hanzi) => ({ hanzi })),
    };
    await writeFile(join(READER_DIR, file), JSON.stringify(payload), "utf8");
    manifest.push({
      chapter: chapter.number,
      title: chapter.title,
      sentences: chapter.sentences.length,
      file,
    });
  }
  await writeFile(
    join(READER_DIR, "manifest.json"),
    JSON.stringify({ title: "三国演义", source: SOURCE_URL, chapters: manifest }, null, 0),
    "utf8",
  );
  return manifest;
}

async function main() {
  console.log(`Fetching Romance of the Three Kingdoms from ${SOURCE_URL}…`);
  const text = await (await fetch(SOURCE_URL)).text();
  console.log(`  ${text.length} characters`);

  const chapters = splitChapters(text);
  const totalSentences = chapters.reduce((n, c) => n + c.sentences.length, 0);
  console.log(`  ${chapters.length} chapters, ${totalSentences} sentences`);

  await writeReaderFiles(chapters);
  console.log(`  wrote ${chapters.length + 1} files to ${READER_DIR}`);

  console.log("Refreshing backend character-import resources…");
  const report = await importCorpus({ name: "three-kingdoms", text });
  console.log(`  missing characters: ${report.missing} (matched ${report.cedictMatched} in CC-CEDICT)`);
  console.log(`  wrote ${report.listPath}`);
  console.log(`  wrote ${report.cedictPath}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
