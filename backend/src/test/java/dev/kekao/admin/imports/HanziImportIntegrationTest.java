package dev.kekao.admin.imports;

import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
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

    @Test
    void importPopulatesHsk1FromBundledSources() throws Exception {
        HanziImportService.ImportReport report = service.runImport();

        assertThat(report.created()).isGreaterThan(100);
        List<HanziEntity> all = hanzi.findAll();
        assertThat(all).hasSizeGreaterThan(100);
        assertThat(all).allSatisfy(h -> {
            assertThat(h.getStatus()).isEqualTo(HanziStatus.DRAFT);
            assertThat(h.getHskLevel()).isEqualTo((short) 1);
        });
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
