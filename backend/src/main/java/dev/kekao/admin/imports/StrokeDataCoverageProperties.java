package dev.kekao.admin.imports;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "kekao.stroke-coverage")
public record StrokeDataCoverageProperties(
    Path dataDir,
    short minHskLevel,
    short maxHskLevel,
    int maxMissingInLog
) {
}
