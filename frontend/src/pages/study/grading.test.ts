import { describe, expect, it } from "vitest";

import {
  gradeAnswer,
  normalizeMeaning,
  normalizePinyin,
} from "@/pages/study/grading";

describe("normalizePinyin", () => {
  it("converts diacritics into numeric tones by default", () => {
    expect(normalizePinyin("nǐ hǎo")).toBe("ni3 hao3");
  });

  it("strips tones when withTones=false", () => {
    expect(normalizePinyin("nǐ hǎo", false)).toBe("ni hao");
    expect(normalizePinyin("ni3 hao3", false)).toBe("ni hao");
  });

  it("treats v and ü interchangeably", () => {
    expect(normalizePinyin("nǚ")).toBe("nv3");
    expect(normalizePinyin("nv3")).toBe("nv3");
  });
});

describe("normalizeMeaning", () => {
  it("strips leading articles, lowercases and squashes whitespace", () => {
    expect(normalizeMeaning("The Quick   Brown! ")).toBe("quick brown");
    expect(normalizeMeaning("to run")).toBe("run");
  });
});

describe("gradeAnswer", () => {
  it("returns CORRECT when both pinyin and meaning match", () => {
    const result = gradeAnswer("nǐ", ["you"], "ni3", "you");
    expect(result.outcome).toBe("CORRECT");
    expect(result.suggestedRating).toBe("GOOD");
    expect(result.pinyinOk).toBe(true);
    expect(result.meaningOk).toBe(true);
  });

  it("returns NEAR for a small typo on meaning when pinyin is correct", () => {
    const result = gradeAnswer("nǐ", ["hello"], "ni3", "helo");
    expect(result.outcome).toBe("NEAR");
    expect(result.suggestedRating).toBe("HARD");
    expect(result.pinyinOk).toBe(true);
    expect(result.bestMeaning).toBe("hello");
  });

  it("returns WRONG when pinyin tone is wrong by default", () => {
    const result = gradeAnswer("nǐ", ["you"], "ni4", "you");
    expect(result.outcome).toBe("WRONG");
    expect(result.pinyinOk).toBe(false);
  });

  it("accepts wrong tones when withTones=false", () => {
    const result = gradeAnswer("nǐ", ["you"], "ni4", "you", { withTones: false });
    expect(result.outcome).toBe("CORRECT");
    expect(result.pinyinOk).toBe(true);
  });

  it("accepts toneless input when withTones=false", () => {
    const result = gradeAnswer("nǐ", ["you"], "ni", "you", { withTones: false });
    expect(result.outcome).toBe("CORRECT");
  });

  it("rejects toneless input by default (withTones=true)", () => {
    const result = gradeAnswer("nǐ", ["you"], "ni", "you");
    expect(result.outcome).toBe("WRONG");
    expect(result.pinyinOk).toBe(false);
  });

  it("rejects when meaning is unrelated", () => {
    const result = gradeAnswer("nǐ", ["you"], "ni3", "horse");
    expect(result.outcome).toBe("WRONG");
    expect(result.suggestedRating).toBe("AGAIN");
  });

  it("matches any of multiple accepted meanings", () => {
    const result = gradeAnswer("hǎo", ["good", "well"], "hao3", "well");
    expect(result.outcome).toBe("CORRECT");
  });
});
