package dev.kekao.admin.imports;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads the {@code char -> hsk_level} mapping from a classpath resource.
 *
 * <p>Format: one entry per line, optionally prefixed with {@code #}-comments.
 * Two forms are accepted:
 * <pre>
 * 你
 * 你 1
 * </pre>
 * The single-token form falls back to {@code defaultLevel} provided by the
 * caller (allowing simple "one file per HSK level" layouts).
 */
public final class HskListLoader {

    private final ResourcePatternResolver resolver;

    public HskListLoader() {
        this(new PathMatchingResourcePatternResolver());
    }

    public HskListLoader(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    public Map<String, Integer> load(String resourceLocation, int defaultLevel) throws IOException {
        Resource resource = resolver.getResource(toClasspath(resourceLocation));
        if (!resource.exists()) {
            throw new IOException("HSK list resource not found: " + resourceLocation);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addLine(result, line, defaultLevel);
            }
        }
        return result;
    }

    /**
     * Loads a plain character list (one simplified character per line, with
     * optional {@code #}-comments). Used for corpus lists that carry no HSK
     * level. The order of first appearance is preserved.
     */
    public Set<String> loadCharacters(String resourceLocation) throws IOException {
        Resource resource = resolver.getResource(toClasspath(resourceLocation));
        if (!resource.exists()) {
            throw new IOException("Corpus list resource not found: " + resourceLocation);
        }
        Set<String> result = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                result.add(trimmed.split("\\s+")[0]);
            }
        }
        return result;
    }

    private static void addLine(Map<String, Integer> sink, String rawLine, int defaultLevel) {
        String line = rawLine.strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        String[] parts = line.split("\\s+");
        String character = parts[0];
        int level = parts.length > 1 ? safeParseLevel(parts[1], defaultLevel) : defaultLevel;
        sink.put(character, level);
    }

    private static int safeParseLevel(String token, int fallback) {
        try {
            int parsed = Integer.parseInt(token);
            if (parsed >= 1 && parsed <= 9) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return fallback;
    }

    private static String toClasspath(String location) {
        if (location.startsWith("classpath:") || location.startsWith("file:")) {
            return location;
        }
        return "classpath:" + location;
    }
}
