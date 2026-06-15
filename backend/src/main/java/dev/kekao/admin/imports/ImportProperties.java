package dev.kekao.admin.imports;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of the hanzi importer (TASK-006, expanded in TASK-031).
 *
 * @param cedictPath        absolute filesystem path to the CC-CEDICT dump
 *                          (tab/space separated MDBG format). When blank, the
 *                          importer falls back to {@code cedictResource}.
 * @param cedictResource    classpath resource with a CC-CEDICT subset bundled
 *                          for development / tests.
 * @param hskListResource   legacy single-file location, retained for backward
 *                          compatibility. Merged into {@code hskListResources}
 *                          when provided.
 * @param hskListResources  classpath resources listing characters per HSK
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
 * @param corpusListResources
 *                          classpath resources listing characters (one per
 *                          line) harvested from a corpus such as a classical
 *                          novel. These are imported with no HSK level and stay
 *                          {@code DRAFT} until an editor reviews them. Empty by
 *                          default — opt in via {@code KEKAO_IMPORT_CORPUS_RESOURCES}.
 * @param extraCedictResources
 *                          additional CC-CEDICT subsets merged into the lookup
 *                          index (e.g. the dictionary slice generated alongside
 *                          a corpus list).
 */
@ConfigurationProperties(prefix = "kekao.import")
public record ImportProperties(
        String cedictPath,
        String cedictResource,
        String hskListResource,
        List<String> hskListResources,
        List<Integer> hskLevels,
        String defaultLanguage,
        int maxMeaningsPerHanzi,
        boolean failOnMissingCedict,
        List<String> corpusListResources,
        List<String> extraCedictResources
) {
    public ImportProperties {
        if (cedictResource == null || cedictResource.isBlank()) {
            cedictResource = "imports/cedict.sample.txt";
        }
        List<String> resources = new ArrayList<>();
        if (hskListResources != null) {
            for (String r : hskListResources) {
                if (r != null && !r.isBlank()) resources.add(r);
            }
        }
        if (resources.isEmpty() && hskListResource != null && !hskListResource.isBlank()) {
            resources.add(hskListResource);
        }
        if (resources.isEmpty()) {
            resources.add("imports/hsk1.txt");
            resources.add("imports/hsk2.txt");
            resources.add("imports/hsk3.txt");
        }
        hskListResources = List.copyOf(resources);
        if (hskListResource == null || hskListResource.isBlank()) {
            hskListResource = hskListResources.get(0);
        }
        if (hskLevels == null || hskLevels.isEmpty()) {
            hskLevels = List.of(1, 2, 3);
        }
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            defaultLanguage = "en";
        }
        if (maxMeaningsPerHanzi <= 0) {
            maxMeaningsPerHanzi = 5;
        }
        corpusListResources = sanitize(corpusListResources);
        extraCedictResources = sanitize(extraCedictResources);
    }

    private static List<String> sanitize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.strip());
            }
        }
        return List.copyOf(cleaned);
    }
}
