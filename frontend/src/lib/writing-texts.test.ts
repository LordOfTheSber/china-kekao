import { describe, expect, it } from "vitest";

import {
  WRITING_LESSONS,
  countCharacters,
  findLesson,
  isWritable,
  writableChars,
} from "./writing-texts";

describe("isWritable", () => {
  it("accepts CJK ideographs", () => {
    expect(isWritable("我")).toBe(true);
    expect(isWritable("好")).toBe(true);
  });

  it("rejects punctuation, spaces and latin letters", () => {
    expect(isWritable("。")).toBe(false);
    expect(isWritable("，")).toBe(false);
    expect(isWritable("？")).toBe(false);
    expect(isWritable(" ")).toBe(false);
    expect(isWritable("a")).toBe(false);
  });
});

describe("writableChars", () => {
  it("strips punctuation from a sentence", () => {
    expect(writableChars("我是学生。")).toEqual(["我", "是", "学", "生"]);
  });

  it("handles a question mark sentence", () => {
    expect(writableChars("你想吃什么？")).toEqual(["你", "想", "吃", "什", "么"]);
  });
});

describe("countCharacters", () => {
  it("counts only writable glyphs across all texts", () => {
    const lesson = {
      id: "x",
      title: "x",
      description: "x",
      level: 1 as const,
      texts: [
        { hanzi: "你好", pinyin: "nǐ hǎo", english: "hi" },
        { hanzi: "我是学生。", pinyin: "wǒ shì xué sheng.", english: "I am a student." },
      ],
    };
    expect(countCharacters(lesson)).toBe(2 + 4);
  });
});

describe("WRITING_LESSONS dataset", () => {
  it("has unique lesson ids", () => {
    const ids = WRITING_LESSONS.map((lesson) => lesson.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("every text has pinyin, english and at least one writable glyph", () => {
    for (const lesson of WRITING_LESSONS) {
      for (const text of lesson.texts) {
        expect(text.pinyin.trim().length).toBeGreaterThan(0);
        expect(text.english.trim().length).toBeGreaterThan(0);
        expect(writableChars(text.hanzi).length).toBeGreaterThan(0);
      }
    }
  });

  it("findLesson resolves a known id and ignores unknown ones", () => {
    expect(findLesson("numbers")?.title).toBe("Numbers 1–10");
    expect(findLesson("nope")).toBeUndefined();
    expect(findLesson(null)).toBeUndefined();
  });
});
