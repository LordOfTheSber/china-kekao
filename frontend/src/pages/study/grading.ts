import type { Rating } from "@/api/types";

export type GradeOutcome = "CORRECT" | "NEAR" | "WRONG";

export interface GradeResult {
  outcome: GradeOutcome;
  suggestedRating: Rating;
  pinyinOk: boolean;
  meaningOk: boolean;
  bestMeaning: string | null;
  bestMeaningDistance: number;
}

const TONE_MAP: Record<string, [string, number]> = {
  ā: ["a", 1], á: ["a", 2], ǎ: ["a", 3], à: ["a", 4],
  ē: ["e", 1], é: ["e", 2], ě: ["e", 3], è: ["e", 4],
  ī: ["i", 1], í: ["i", 2], ǐ: ["i", 3], ì: ["i", 4],
  ō: ["o", 1], ó: ["o", 2], ǒ: ["o", 3], ò: ["o", 4],
  ū: ["u", 1], ú: ["u", 2], ǔ: ["u", 3], ù: ["u", 4],
  ǖ: ["v", 1], ǘ: ["v", 2], ǚ: ["v", 3], ǜ: ["v", 4],
  ü: ["v", 0],
};

export function normalizePinyinSyllable(input: string): string {
  if (!input) return "";
  let tone = 0;
  let base = "";
  for (const ch of input.normalize("NFC").toLowerCase()) {
    const mapped = TONE_MAP[ch];
    if (mapped) {
      base += mapped[0];
      if (mapped[1] !== 0) tone = mapped[1];
    } else if (/[a-z']/.test(ch)) {
      base += ch === "ü" ? "v" : ch;
    } else if (/[1-4]/.test(ch)) {
      tone = Number.parseInt(ch, 10);
    }
  }
  return tone > 0 ? `${base}${tone}` : base;
}

export function normalizePinyin(input: string, withTones = true): string {
  const joined = input
    .trim()
    .split(/\s+/)
    .map(normalizePinyinSyllable)
    .filter(Boolean)
    .join(" ");
  if (withTones) return joined;
  return joined.replace(/[1-4]/g, "");
}

export function normalizeMeaning(input: string): string {
  return input
    .toLowerCase()
    .replace(/^(to|a|an|the)\s+/u, "")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function levenshtein(a: string, b: string): number {
  const n = a.length;
  const m = b.length;
  if (n === 0) return m;
  if (m === 0) return n;
  let prev = new Array<number>(m + 1);
  let curr = new Array<number>(m + 1);
  for (let j = 0; j <= m; j++) prev[j] = j;
  for (let i = 1; i <= n; i++) {
    curr[0] = i;
    for (let j = 1; j <= m; j++) {
      const cost = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
      curr[j] = Math.min(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    [prev, curr] = [curr, prev];
  }
  return prev[m];
}

export function gradeAnswer(
  expectedPinyin: string,
  acceptedMeanings: string[],
  userPinyin: string,
  userMeaning: string,
  options: { withTones?: boolean } = {},
): GradeResult {
  const withTones = options.withTones ?? true;
  const normalizedExpected = normalizePinyin(expectedPinyin, withTones);
  const normalizedUserPinyin = normalizePinyin(userPinyin, withTones);
  const pinyinOk = normalizedExpected.length > 0 && normalizedExpected === normalizedUserPinyin;

  const normalizedAnswer = normalizeMeaning(userMeaning);
  let bestDistance = Number.POSITIVE_INFINITY;
  let bestMeaning: string | null = null;
  let meaningOk = false;
  for (const meaning of acceptedMeanings) {
    const normalized = normalizeMeaning(meaning);
    if (!normalized) continue;
    if (normalized === normalizedAnswer) {
      meaningOk = true;
      bestMeaning = meaning;
      bestDistance = 0;
      break;
    }
    const distance = levenshtein(normalized, normalizedAnswer);
    if (distance < bestDistance) {
      bestDistance = distance;
      bestMeaning = meaning;
    }
  }

  if (pinyinOk && meaningOk) {
    return {
      outcome: "CORRECT",
      suggestedRating: "GOOD",
      pinyinOk,
      meaningOk,
      bestMeaning,
      bestMeaningDistance: bestDistance,
    };
  }

  if (pinyinOk && normalizedAnswer.length > 0 && bestDistance <= 1) {
    return {
      outcome: "NEAR",
      suggestedRating: "HARD",
      pinyinOk,
      meaningOk: false,
      bestMeaning,
      bestMeaningDistance: bestDistance,
    };
  }

  return {
    outcome: "WRONG",
    suggestedRating: "AGAIN",
    pinyinOk,
    meaningOk: false,
    bestMeaning,
    bestMeaningDistance: Number.isFinite(bestDistance) ? bestDistance : -1,
  };
}
