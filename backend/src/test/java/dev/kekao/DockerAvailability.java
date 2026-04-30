package dev.kekao;

import org.testcontainers.DockerClientFactory;

/**
 * Guard for Testcontainers-backed integration tests. When the local
 * Docker daemon is not reachable the tests should be skipped instead of
 * failing the build, so the suite stays runnable on developer machines
 * without Docker (CI always has it). The result is cached because the
 * underlying probe involves a real connection attempt.
 */
public final class DockerAvailability {

    private static final boolean AVAILABLE = probe();

    private DockerAvailability() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private static boolean probe() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
