package dev.kekao.admin.imports;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Imports a starter set of hanzi (TASK-006).
 *
 * <p>Strategy:
 * <ol>
 *   <li>Load the {@code char -> hsk_level} map from a bundled resource.</li>
 *   <li>Stream the CC-CEDICT dump (filesystem path or classpath fallback) and
 *       index entries by simplified character.</li>
 *   <li>For every HSK character within the configured levels, upsert a
 *       {@link HanziEntity} in {@link HanziStatus#DRAFT DRAFT} status with
 *       English translations from CC-CEDICT.</li>
 * </ol>
 * The operation is idempotent: re-running it leaves already-imported records
 * untouched (we never downgrade {@code PUBLISHED} content back to drafts).
 */
@Service
public class HanziImportService {

    private static final Logger log = LoggerFactory.getLogger(HanziImportService.class);

    private final HanziRepository hanzi;
    private final HanziTranslationRepository translations;
    private final ImportProperties props;
    private final CedictParser parser;
    private final HskListLoader hskLoader;

    @Autowired
    public HanziImportService(HanziRepository hanzi,
                              HanziTranslationRepository translations,
                              ImportProperties props) {
        this(hanzi, translations, props, new CedictParser(),
                new HskListLoader(new PathMatchingResourcePatternResolver()));
    }

    HanziImportService(HanziRepository hanzi,
                       HanziTranslationRepository translations,
                       ImportProperties props,
                       CedictParser parser,
                       HskListLoader hskLoader) {
        this.hanzi = hanzi;
        this.translations = translations;
        this.props = props;
        this.parser = parser;
        this.hskLoader = hskLoader;
    }

    @Transactional
    public ImportReport runImport() throws IOException {
        Map<String, Integer> hskMap = loadHskMap();
        Map<String, CedictEntry> cedictBySimplified = loadCedictIndex(hskMap.keySet());
        return importCharacters(hskMap, cedictBySimplified);
    }

    private Map<String, Integer> loadHskMap() throws IOException {
        int defaultLevel = props.hskLevels().getFirst();
        Set<Integer> wanted = Set.copyOf(props.hskLevels());
        Map<String, Integer> merged = new HashMap<>();
        for (String resource : props.hskListResources()) {
            Map<String, Integer> chunk = hskLoader.load(resource, defaultLevel);
            for (Map.Entry<String, Integer> e : chunk.entrySet()) {
                if (!wanted.contains(e.getValue())) continue;
                Integer existing = merged.get(e.getKey());
                if (existing == null || e.getValue() < existing) {
                    merged.put(e.getKey(), e.getValue());
                }
            }
            log.debug("Loaded {} entries from {}", chunk.size(), resource);
        }
        log.info("Loaded {} HSK characters from {} (levels {})",
                merged.size(), props.hskListResources(), wanted);
        return merged;
    }

    private Map<String, CedictEntry> loadCedictIndex(Set<String> wantedChars) throws IOException {
        try (Reader reader = openCedict()) {
            Map<String, CedictEntry> index = new HashMap<>();
            for (CedictEntry entry : parser.parse(reader)) {
                if (entry.simplified().codePointCount(0, entry.simplified().length()) != 1) {
                    continue;
                }
                if (!wantedChars.contains(entry.simplified())) {
                    continue;
                }
                CedictEntry existing = index.get(entry.simplified());
                if (existing == null || existing.meanings().isEmpty()) {
                    index.put(entry.simplified(), entry);
                }
            }
            log.info("Indexed {} CC-CEDICT entries matching the HSK list", index.size());
            return index;
        }
    }

    private Reader openCedict() throws IOException {
        if (props.cedictPath() != null && !props.cedictPath().isBlank()) {
            Path path = Path.of(props.cedictPath());
            log.info("Reading CC-CEDICT dump from {}", path.toAbsolutePath());
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        }
        var resource = new PathMatchingResourcePatternResolver()
                .getResource("classpath:" + props.cedictResource());
        if (!resource.exists()) {
            throw new IOException("CC-CEDICT resource not found: " + props.cedictResource());
        }
        log.info("Reading bundled CC-CEDICT sample from {}", props.cedictResource());
        return new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8));
    }

    private ImportReport importCharacters(Map<String, Integer> hskMap,
                                          Map<String, CedictEntry> cedictBySimplified) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : hskMap.entrySet()) {
            String character = entry.getKey();
            int hskLevel = entry.getValue();
            CedictEntry cedict = cedictBySimplified.get(character);
            if (cedict == null) {
                missing.add(character);
                if (props.failOnMissingCedict()) {
                    throw new IllegalStateException(
                            "No CC-CEDICT entry for HSK character: " + character);
                }
            }
            ImportOutcome outcome = upsertCharacter(character, hskLevel, cedict);
            switch (outcome) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case SKIPPED -> skipped++;
            }
        }
        log.info("Hanzi import done: created={}, updated={}, skipped={}, missingCedict={}",
                created, updated, skipped, missing.size());
        return new ImportReport(created, updated, skipped, List.copyOf(missing));
    }

    private ImportOutcome upsertCharacter(String character, int hskLevel, CedictEntry cedict) {
        Optional<HanziEntity> existing = hanzi.findByCharacter(character);
        if (existing.isPresent()) {
            return refreshDraft(existing.get(), hskLevel, cedict);
        }
        boolean complete = cedict != null
                && cedict.pinyin() != null && !cedict.pinyin().isBlank()
                && !cedict.meanings().isEmpty();
        HanziEntity created = hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin(cedict != null ? cedict.pinyin() : "")
                .hskLevel((short) hskLevel)
                .status(complete ? HanziStatus.PUBLISHED : HanziStatus.DRAFT)
                .build());
        if (cedict != null && !cedict.meanings().isEmpty()) {
            translations.save(buildTranslation(created, cedict));
        }
        return ImportOutcome.CREATED;
    }

    private ImportOutcome refreshDraft(HanziEntity entity, int hskLevel, CedictEntry cedict) {
        if (entity.getStatus() != HanziStatus.DRAFT) {
            return ImportOutcome.SKIPPED;
        }
        boolean changed = false;
        if (entity.getHskLevel() == null || entity.getHskLevel() != hskLevel) {
            entity.setHskLevel((short) hskLevel);
            changed = true;
        }
        if (cedict != null && (entity.getPinyin() == null || entity.getPinyin().isBlank())) {
            entity.setPinyin(cedict.pinyin());
            changed = true;
        }
        if (cedict != null && !cedict.meanings().isEmpty()
                && translations.findByHanziId(entity.getId()).isEmpty()) {
            translations.save(buildTranslation(entity, cedict));
            changed = true;
        }
        if (entity.getStatus() == HanziStatus.DRAFT && hasCompletePayload(entity)) {
            entity.setStatus(HanziStatus.PUBLISHED);
            changed = true;
        }
        return changed ? ImportOutcome.UPDATED : ImportOutcome.SKIPPED;
    }

    private boolean hasCompletePayload(HanziEntity entity) {
        if (entity.getPinyin() == null || entity.getPinyin().isBlank()) return false;
        return translations.findByHanziId(entity.getId()).stream()
                .filter(t -> "en".equals(t.getLanguage()))
                .anyMatch(t -> t.getMeanings() != null
                        && t.getMeanings().stream().anyMatch(m -> m != null && !m.isBlank()));
    }

    private HanziTranslationEntity buildTranslation(HanziEntity owner, CedictEntry cedict) {
        List<String> meanings = cedict.meanings();
        if (meanings.size() > props.maxMeaningsPerHanzi()) {
            meanings = meanings.subList(0, props.maxMeaningsPerHanzi());
        }
        return HanziTranslationEntity.builder()
                .hanzi(owner)
                .language(props.defaultLanguage())
                .meanings(new ArrayList<>(meanings))
                .primary(true)
                .build();
    }

    private enum ImportOutcome { CREATED, UPDATED, SKIPPED }

    public record ImportReport(int created, int updated, int skipped, List<String> missingCedict) {
    }
}
