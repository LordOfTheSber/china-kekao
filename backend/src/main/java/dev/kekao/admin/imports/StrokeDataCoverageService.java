package dev.kekao.admin.imports;

import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class StrokeDataCoverageService {

    private static final Logger log = LoggerFactory.getLogger(StrokeDataCoverageService.class);

    private final HanziRepository hanziRepository;
    private final StrokeDataCoverageProperties properties;

    public StrokeDataCoverageService(HanziRepository hanziRepository, StrokeDataCoverageProperties properties) {
        this.hanziRepository = hanziRepository;
        this.properties = properties;
    }

    @Transactional
    public void updateCoverage() {
        final Set<String> availableCharacters = loadAvailableCharacters(properties.dataDir());
        final List<HanziEntity> hanzi = hanziRepository.findAllByHskLevelBetween(
            properties.minHskLevel(),
            properties.maxHskLevel()
        );
        final List<String> missingCharacters = new ArrayList<>();
        for (HanziEntity entity : hanzi) {
            boolean hasStrokeData = availableCharacters.contains(entity.getCharacter());
            entity.setHasStrokeData(hasStrokeData);
            if (!hasStrokeData) {
                missingCharacters.add(entity.getCharacter());
            }
        }
        hanziRepository.saveAll(hanzi);
        logCoverage(hanzi.size(), missingCharacters);
    }

    private Set<String> loadAvailableCharacters(Path dataDir) {
        if (dataDir == null || !Files.isDirectory(dataDir)) {
            throw new IllegalStateException("Stroke data directory does not exist: " + dataDir);
        }
        try (Stream<Path> pathStream = Files.list(dataDir)) {
            Set<String> characters = new HashSet<>();
            pathStream.filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(this::isJsonFile)
                .map(this::extractCharacterFromFilename)
                .forEach(characters::add);
            return characters;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read stroke data directory: " + dataDir, exception);
        }
    }

    private void logCoverage(int total, List<String> missingCharacters) {
        int covered = total - missingCharacters.size();
        double coveragePercent = total == 0 ? 100.0 : covered * 100.0 / total;
        log.info(
            "Stroke data coverage report for HSK {}-{}: covered={}, missing={}, total={}, coverage={}%.",
            properties.minHskLevel(),
            properties.maxHskLevel(),
            covered,
            missingCharacters.size(),
            total,
            String.format("%.2f", coveragePercent)
        );
        if (missingCharacters.isEmpty()) {
            return;
        }
        List<String> preview = missingCharacters.stream().limit(properties.maxMissingInLog()).toList();
        log.warn("Missing stroke data characters (first {}): {}", preview.size(), preview);
    }

    private boolean isJsonFile(String filename) {
        return filename.endsWith(".json");
    }

    private String extractCharacterFromFilename(String filename) {
        return filename.substring(0, filename.length() - 5);
    }
}
