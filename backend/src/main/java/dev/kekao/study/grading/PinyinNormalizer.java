package dev.kekao.study.grading;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Converts pinyin between tone-mark form ({@code nǐ}) and numeric form ({@code ni3}),
 * and produces canonical representations for comparison.
 *
 * <p>The implementation is intentionally self-contained — no third-party pinyin
 * library is required.</p>
 */
public final class PinyinNormalizer {

    private PinyinNormalizer() {}

    /**
     * Returns a canonical numeric form for the given pinyin syllable or phrase.
     * Tones are preserved (digits 1-4 follow their syllable; toneless syllables
     * receive no digit). Whitespace inside the input is collapsed; case is folded.
     */
    public static String toNumeric(String input) {
        return toNumeric(input, true);
    }

    /**
     * Like {@link #toNumeric(String)}, but if {@code keepTones} is {@code false}
     * tone information is discarded entirely.
     */
    public static String toNumeric(String input, boolean keepTones) {
        if (input == null) return "";
        String trimmed = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        if (trimmed.isEmpty()) return "";

        StringBuilder out = new StringBuilder(trimmed.length());
        StringBuilder syllable = new StringBuilder();
        int currentTone = 0;
        boolean inSyllable = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetter(c) || c == 'ü') {
                int markedTone = toneFromMark(c);
                char base = stripToneMark(c);
                syllable.append(base);
                if (markedTone > 0) currentTone = markedTone;
                inSyllable = true;
                continue;
            }
            if (inSyllable && Character.isDigit(c)) {
                int digit = c - '0';
                if (digit >= 1 && digit <= 5) {
                    if (currentTone == 0) currentTone = digit == 5 ? 0 : digit;
                    continue;
                }
            }
            // any non-letter/non-tone-digit terminates the syllable
            flushSyllable(out, syllable, currentTone, keepTones);
            currentTone = 0;
            inSyllable = false;
            if (Character.isWhitespace(c)) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
            } else {
                out.append(c);
            }
        }
        flushSyllable(out, syllable, currentTone, keepTones);
        return out.toString().trim();
    }

    private static void flushSyllable(StringBuilder out, StringBuilder syllable, int tone, boolean keepTones) {
        if (syllable.length() == 0) return;
        out.append(syllable);
        if (keepTones && tone >= 1 && tone <= 4) out.append(tone);
        syllable.setLength(0);
    }

    private static char stripToneMark(char c) {
        if (c == 'ü') return 'v';
        String decomposed = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        boolean hasDiaeresis = decomposed.indexOf('̈') >= 0;
        char base = decomposed.isEmpty() ? c : decomposed.charAt(0);
        if (base == 'u' && hasDiaeresis) return 'v';
        return base;
    }

    private static int toneFromMark(char c) {
        String decomposed = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        for (int i = 0; i < decomposed.length(); i++) {
            char ch = decomposed.charAt(i);
            switch (ch) {
                case '̄': return 1; // macron
                case '́': return 2; // acute
                case '̌': return 3; // caron
                case '̀': return 4; // grave
                default: // ignore
            }
        }
        return 0;
    }
}
