package dev.kekao;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base class for integration tests that need a real PostgreSQL
 * instance.
 *
 * <p>The container is started exactly once per JVM via a static
 * initializer and torn down by Testcontainers' Ryuk on JVM shutdown.
 * We deliberately do <b>not</b> use {@code @Testcontainers}/{@code @Container}
 * here: those annotations bind the container's lifecycle to a single
 * test class, which fails when Surefire runs multiple test classes in
 * the same JVM (the second class would inherit an already-stopped
 * container).</p>
 *
 * <p>Concrete subclasses MUST be annotated with {@code @EnabledIf(
 * "dev.kekao.DockerAvailability#isAvailable")} so that environments
 * without Docker skip the suite instead of failing the build.</p>
 */
public abstract class AbstractPostgresIntegrationTest {

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("kekao")
                    .withUsername("kekao")
                    .withPassword("kekao");

    static {
        if (DockerAvailability.isAvailable()) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
