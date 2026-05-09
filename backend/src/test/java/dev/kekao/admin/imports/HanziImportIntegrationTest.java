package dev.kekao.admin.imports;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class HanziImportIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private HanziImportService service;
    @Autowired private HanziRepository hanzi;
    @Autowired private HanziTranslationRepository translations;

    @BeforeEach
    @AfterEach
    void cleanCatalogue() {
        // The Postgres testcontainer is shared across @SpringBootTest classes,
        // and runImport() commits its own transaction. Wipe the catalogue
        // before and after each test so other suites don't trip on duplicate
        // characters and our assertions see a deterministic baseline.
        translations.deleteAll();
        hanzi.deleteAll();
    }

    @Test
    void importPopulatesHsk1Through3FromBundledSources() throws Exception {
        HanziImportService.ImportReport report = service.runImport();

        assertThat(report.created()).isGreaterThan(500);
        List<HanziEntity> all = hanzi.findAll();
        assertThat(all).hasSizeGreaterThan(500);
        assertThat(all)
                .extracting(HanziEntity::getHskLevel)
                .containsOnly((short) 1, (short) 2, (short) 3);

        long hsk1 = all.stream().filter(h -> h.getHskLevel() == 1).count();
        long hsk2 = all.stream().filter(h -> h.getHskLevel() == 2).count();
        long hsk3 = all.stream().filter(h -> h.getHskLevel() == 3).count();
        assertThat(hsk1).isGreaterThan(150);
        assertThat(hsk2).isGreaterThan(150);
        assertThat(hsk3).isGreaterThan(250);

        assertThat(all).allSatisfy(h -> assertThat(h.getPinyin()).isNotBlank());

        long published = all.stream()
                .filter(h -> h.getStatus() == HanziStatus.PUBLISHED)
                .count();
        // The bundled CC-CEDICT subset covers every HSK 1-3 char, so the vast
        // majority should be auto-published.
        assertThat(published).isGreaterThan((long) (all.size() * 0.9));
    }

    @Test
    void importAttachesEnglishTranslationsAndPinyin() throws Exception {
        service.runImport();

        Optional<HanziEntity> ni = hanzi.findByCharacter("你");
        assertThat(ni).isPresent();
        assertThat(ni.get().getPinyin()).isEqualTo("nǐ");

        List<HanziTranslationEntity> ts = translations.findByHanziId(ni.get().getId());
        assertThat(ts).hasSize(1);
        HanziTranslationEntity en = ts.getFirst();
        assertThat(en.getLanguage()).isEqualTo("en");
        assertThat(en.isPrimary()).isTrue();
        assertThat(en.getMeanings()).isNotEmpty();
        assertThat(en.getMeanings().getFirst().toLowerCase()).contains("you");
    }

    @Test
    void importIsIdempotent() throws Exception {
        HanziImportService.ImportReport first = service.runImport();
        long countAfterFirst = hanzi.count();

        HanziImportService.ImportReport second = service.runImport();

        assertThat(second.created()).isZero();
        assertThat(hanzi.count()).isEqualTo(countAfterFirst);
        assertThat(first.created()).isPositive();
    }
}
