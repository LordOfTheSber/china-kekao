package dev.kekao.admin.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses lines from a CC-CEDICT dump.
 *
 * <p>CC-CEDICT line format:
 * <pre>Traditional Simplified [pin1 yin1 ...] /meaning 1/meaning 2/.../</pre>
 * Lines starting with {@code #} or empty lines are ignored.
 */
public final class CedictParser {

    private static final Logger log = LoggerFactory.getLogger(CedictParser.class);

    /**
     * Pattern: {@code <trad> <simp> [<pinyin>] /<meaning>/.../}.
     * Tradi/simpl are tokens without whitespace; pinyin is anything inside [].
     */
    private static final Pattern LINE = Pattern.compile(
            "^(\\S+)\\s+(\\S+)\\s+\\[([^]]*)]\\s+/(.+)/\\s*$");

    /**
     * Tone digit at the end of a pinyin syllable (e.g. {@code ni3 hao3}).
     */
    private static final Pattern TONE_DIGIT = Pattern.compile("([a-zA-Zü:]+)([1-5])");

    private static final char[][] TONE_TABLE = {
            // a, e, i, o, u, ü
            {'a', 'ā', 'á', 'ǎ', 'à', 'a'},
            {'e', 'ē', 'é', 'ě', 'è', 'e'},
            {'i', 'ī', 'í', 'ǐ', 'ì', 'i'},
            {'o', 'ō', 'ó', 'ǒ', 'ò', 'o'},
            {'u', 'ū', 'ú', 'ǔ', 'ù', 'u'},
            {'ü', 'ǖ', 'ǘ', 'ǚ', 'ǜ', 'ü'},
    };

    public List<CedictEntry> parse(Reader source) throws IOException {
        List<CedictEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            while ((line = reader.readLine()) != null) {
                CedictEntry entry = parseLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    CedictEntry parseLine(String rawLine) {
        if (rawLine == null) {
            return null;
        }
        String line = rawLine.strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }
        Matcher matcher = LINE.matcher(line);
        if (!matcher.matches()) {
            log.debug("Skipping unparseable CC-CEDICT line: {}", line);
            return null;
        }
        String pinyin = numericPinyinToToneMarks(matcher.group(3).strip());
        List<String> meanings = splitMeanings(matcher.group(4));
        return new CedictEntry(
                matcher.group(2),
                matcher.group(1),
                pinyin,
                meanings);
    }

    private List<String> splitMeanings(String body) {
        return Arrays.stream(body.split("/"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static String numericPinyinToToneMarks(String numeric) {
        if (numeric.isEmpty()) {
            return "";
        }
        String[] syllables = numeric.split("\\s+");
        StringBuilder out = new StringBuilder(numeric.length());
        for (int i = 0; i < syllables.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(convertSyllable(syllables[i]));
        }
        return out.toString();
    }

    private static String convertSyllable(String syllable) {
        Matcher matcher = TONE_DIGIT.matcher(syllable);
        if (!matcher.matches()) {
            return syllable.replace("u:", "ü");
        }
        String letters = matcher.group(1).replace("u:", "ü");
        int tone = Integer.parseInt(matcher.group(2));
        if (tone < 1 || tone > 4) {
            return letters;
        }
        int target = pickToneTarget(letters);
        if (target < 0) {
            return letters;
        }
        char base = letters.charAt(target);
        char marked = applyTone(base, tone);
        return letters.substring(0, target) + marked + letters.substring(target + 1);
    }

    private static int pickToneTarget(String letters) {
        String lower = letters.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf('a');
        if (idx >= 0) return idx;
        idx = lower.indexOf('e');
        if (idx >= 0) return idx;
        idx = lower.indexOf("ou");
        if (idx >= 0) return idx;
        for (int i = lower.length() - 1; i >= 0; i--) {
            char c = lower.charAt(i);
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'ü') {
                return i;
            }
        }
        return -1;
    }

    private static char applyTone(char base, int tone) {
        char lower = Character.toLowerCase(base);
        for (char[] row : TONE_TABLE) {
            if (row[0] == lower) {
                char marked = row[tone];
                return Character.isUpperCase(base) ? Character.toUpperCase(marked) : marked;
            }
        }
        return base;
    }
}
