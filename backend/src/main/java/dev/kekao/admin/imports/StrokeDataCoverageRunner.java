package dev.kekao.admin.imports;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stroke-coverage")
public class StrokeDataCoverageRunner implements CommandLineRunner {

    private final StrokeDataCoverageService strokeDataCoverageService;

    public StrokeDataCoverageRunner(StrokeDataCoverageService strokeDataCoverageService) {
        this.strokeDataCoverageService = strokeDataCoverageService;
    }

    @Override
    public void run(String... args) {
        strokeDataCoverageService.updateCoverage();
    }
}
