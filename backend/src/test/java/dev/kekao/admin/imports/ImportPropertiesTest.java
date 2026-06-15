package dev.kekao.admin.imports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportPropertiesTest {

    @Test
    void defaultsToHsk1Through3WhenResourcesAreOmitted() {
        ImportProperties props = new ImportProperties(
                null, null, null, null, null, null, 0, false, null, null);

        assertThat(props.hskListResources())
                .containsExactly("imports/hsk1.txt", "imports/hsk2.txt", "imports/hsk3.txt");
        assertThat(props.hskListResource()).isEqualTo("imports/hsk1.txt");
        assertThat(props.hskLevels()).containsExactly(1, 2, 3);
        assertThat(props.corpusListResources()).isEmpty();
        assertThat(props.extraCedictResources()).isEmpty();
    }

    @Test
    void promotesLegacySingleResourceIntoTheList() {
        ImportProperties props = new ImportProperties(
                null, null, "imports/custom.txt", null, List.of(1), null, 0, false, null, null);

        assertThat(props.hskListResources()).containsExactly("imports/custom.txt");
        assertThat(props.hskListResource()).isEqualTo("imports/custom.txt");
    }

    @Test
    void preservesExplicitResourceList() {
        ImportProperties props = new ImportProperties(
                null, null, null,
                List.of("imports/a.txt", "imports/b.txt"),
                List.of(2, 3), null, 0, false, null, null);

        assertThat(props.hskListResources()).containsExactly("imports/a.txt", "imports/b.txt");
        assertThat(props.hskListResource()).isEqualTo("imports/a.txt");
    }

    @Test
    void sanitizesCorpusAndExtraCedictResources() {
        ImportProperties props = new ImportProperties(
                null, null, null, null, null, null, 0, false,
                java.util.Arrays.asList("imports/three-kingdoms.txt", "  ", null),
                List.of(" imports/three-kingdoms.cedict.txt "));

        assertThat(props.corpusListResources()).containsExactly("imports/three-kingdoms.txt");
        assertThat(props.extraCedictResources()).containsExactly("imports/three-kingdoms.cedict.txt");
    }
}
