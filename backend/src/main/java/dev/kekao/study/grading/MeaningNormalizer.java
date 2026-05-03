package dev.kekao.study.grading;

import java.util.Locale;
import java.util.Set;

/**
 * Normalizes English meanings for comparison: lowercases, trims, strips edge
 * punctuation/quotes, and removes leading articles ({@code to}, {@code a},
 * {@code an}, {@code the}).
 */
public final class MeaningNormalizer {

    private static final Set<String> LEADING_ARTICLES = Set.of("to", "a", "an", "the");

    private MeaningNormalizer() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(Locale.ROOT).trim();
        s = stripEdgePunctuation(s);
        if (s.isEmpty()) return "";

        for (String article : LEADING_ARTICLES) {
            String prefix = article + " ";
            if (s.startsWith(prefix)) {
                s = s.substring(prefix.length()).trim();
                break;
            }
        }
        // Collapse internal whitespace to single spaces.
        s = s.replaceAll("\\s+", " ");
        return s;
    }

    private static String stripEdgePunctuation(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isEdgePunct(s.charAt(start))) start++;
        while (end > start && isEdgePunct(s.charAt(end - 1))) end--;
        return s.substring(start, end);
    }

    private static boolean isEdgePunct(char c) {
        if (Character.isLetterOrDigit(c) || c == ' ') return false;
        // Treat anything that's not a letter/digit/space as edge punctuation.
        return true;
    }
}
