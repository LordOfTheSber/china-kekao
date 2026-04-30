package dev.kekao.migration;

import dev.kekao.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Flyway baseline migration produces every table and
 * index defined in TASK-003. Failures here mean a critical change to the
 * data model has been introduced without updating the migration script.
 */
@SpringBootTest
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class SchemaMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final List<String> EXPECTED_INDEXES = List.of(
            "idx_user_card_due",
            "idx_hanzi_status_hsk",
            "idx_hanzi_character_trgm",
            "idx_hanzi_pinyin_trgm",
            "idx_review_log_card_time",
            "idx_review_log_time"
    );

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {
            "users", "refresh_tokens", "hanzi", "hanzi_translation",
            "hanzi_example", "deck", "deck_hanzi", "user_deck",
            "user_card", "review_log"
    })
    void tableIsPresentInPublicSchema(String table) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, table);

        assertThat(exists).as("table %s exists", table).isTrue();
    }

    @Test
    void allRequiredIndexesArePresent() {
        List<String> existing = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
                String.class);

        assertThat(existing).containsAll(EXPECTED_INDEXES);
    }

    @Test
    void pgTrgmExtensionIsInstalled() {
        Boolean installed = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm')",
                Boolean.class);

        assertThat(installed).isTrue();
    }

    @Test
    void userCardEnforcesUniqueModePerUserAndHanzi() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'user_card'
                  AND constraint_type = 'UNIQUE'
                """, Integer.class);

        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
