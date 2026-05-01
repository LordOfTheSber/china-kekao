package dev.kekao.admin.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spring Boot {@link ApplicationRunner} that triggers the hanzi import as a
 * one-shot CLI command. Activated via {@code --spring.profiles.active=import}.
 *
 * <p>Run example:
 * <pre>
 * java -jar china-kekao-backend.jar \
 *   --spring.profiles.active=import \
 *   --kekao.import.cedict-path=/data/cedict_ts.u8
 * </pre>
 */
@Component
@Profile("import")
public class HanziImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HanziImportRunner.class);

    private final HanziImportService service;

    public HanziImportRunner(HanziImportService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting hanzi import...");
        HanziImportService.ImportReport report = service.runImport();
        log.info("Hanzi import finished. created={}, updated={}, skipped={}, missing={}",
                report.created(), report.updated(), report.skipped(),
                report.missingCedict().size());
        if (!report.missingCedict().isEmpty()) {
            log.warn("HSK characters without CC-CEDICT entries (sample, up to 20): {}",
                    report.missingCedict().stream().limit(20).toList());
        }
    }
}
