package dev.kekao.admin.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Runs the hanzi import as a one-shot CLI step.
 *
 * <p>Triggers (any one is enough):
 * <ul>
 *   <li>active Spring profile {@code import}, e.g.
 *       {@code --spring.profiles.active=import};</li>
 *   <li>application argument {@code --kekao.import.run=true} (works
 *       regardless of the active profile).</li>
 * </ul>
 *
 * <p>Always logs the decision at INFO so it's obvious when the import was
 * skipped vs. actually executed.
 */
@Component
public class HanziImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HanziImportRunner.class);

    private final HanziImportService service;
    private final Environment environment;

    @Autowired
    public HanziImportRunner(HanziImportService service, Environment environment) {
        this.service = service;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TriggerSource trigger = detectTrigger(args);
        if (trigger == null) {
            log.debug("Hanzi import runner present but not triggered "
                    + "(activate profile=import or pass --kekao.import.run=true).");
            return;
        }
        log.info("Starting hanzi import (trigger: {})...", trigger);
        HanziImportService.ImportReport report = service.runImport();
        log.info("Hanzi import finished. created={}, updated={}, skipped={}, missing={}",
                report.created(), report.updated(), report.skipped(),
                report.missingCedict().size());
        if (!report.missingCedict().isEmpty()) {
            log.warn("HSK characters without CC-CEDICT entries (sample, up to 20): {}",
                    report.missingCedict().stream().limit(20).toList());
        }
    }

    private TriggerSource detectTrigger(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("import"))) {
            return TriggerSource.PROFILE;
        }
        if (args.containsOption("kekao.import.run")
                && args.getOptionValues("kekao.import.run").stream()
                        .anyMatch(v -> v == null || v.isBlank() || Boolean.parseBoolean(v))) {
            return TriggerSource.ARG;
        }
        if (Boolean.parseBoolean(environment.getProperty("kekao.import.run", "false"))) {
            return TriggerSource.PROPERTY;
        }
        return null;
    }

    private enum TriggerSource { PROFILE, ARG, PROPERTY }
}
