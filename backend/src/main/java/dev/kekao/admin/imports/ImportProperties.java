package dev.kekao.admin.imports;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration of the hanzi importer (TASK-006).
 *
 * @param cedictPath        absolute filesystem path to the CC-CEDICT dump
 *                          (tab/space separated MDBG format). When blank, the
 *                          importer falls back to {@code cedictResource}.
 * @param cedictResource    classpath resource with a CC-CEDICT subset bundled
 *                          for development / tests.
 * @param hskListResource   classpath resource listing characters per HSK
 *                          level. One {@code <char> <level>} pair per line.
 * @param hskLevels         which HSK levels to import in this run.
 * @param defaultLanguage   language tag stored on translations.
 * @param maxMeaningsPerHanzi
 *                          how many meanings to keep when CC-CEDICT lists more
 *                          (avoid overly long flashcards).
 * @param failOnMissingCedict
 *                          when true, the importer aborts if a HSK character
 *                          has no CC-CEDICT entry. When false, the character is
 *                          imported with placeholder pinyin and an empty
 *                          meaning list (status stays DRAFT for editing).
 */
@ConfigurationProperties(prefix = "kekao.import")
public record ImportProperties(
        String cedictPath,
        String cedictResource,
        String hskListResource,
        List<Integer> hskLevels,
        String defaultLanguage,
        int maxMeaningsPerHanzi,
        boolean failOnMissingCedict
) {
    public ImportProperties {
        if (cedictResource == null || cedictResource.isBlank()) {
            cedictResource = "imports/cedict.sample.txt";
        }
        if (hskListResource == null || hskListResource.isBlank()) {
            hskListResource = "imports/hsk1.txt";
        }
        if (hskLevels == null || hskLevels.isEmpty()) {
            hskLevels = List.of(1);
        }
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            defaultLanguage = "en";
        }
        if (maxMeaningsPerHanzi <= 0) {
            maxMeaningsPerHanzi = 5;
        }
    }
}
