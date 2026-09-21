package ru.andrew.website.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards the shape of the production image: a frontend build stage, a backend build stage
 * compiled from the exact sources in this repository, and a pinned Java 25 runtime that
 * serves the static frontend through Nginx and proxies {@code /api/} to the Spring Boot jar.
 *
 * <p>Runtime hardening of the image (non-root user, liveness health check) is asserted by
 * the {@code container-build} job in {@code .github/workflows/ci.yml} against the built
 * image rather than against the Dockerfile text.
 */
class ContainerContractTest {
    private static final String FRONTEND_BUILD_IMAGE = "node:24.14.0-alpine";
    private static final String BACKEND_BUILD_IMAGE = "eclipse-temurin:25.0.3_9-jdk-noble";
    private static final String RUNTIME_IMAGE = "eclipse-temurin:25.0.3_9-jre-noble"
            + "@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db";

    @Test
    void dockerfileBuildsFrontendAndBackendFromRepositorySources() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile.lines().filter(line -> line.startsWith("FROM ")))
                .containsExactly(
                        "FROM " + FRONTEND_BUILD_IMAGE + " AS frontend-build",
                        "FROM " + BACKEND_BUILD_IMAGE + " AS backend-build",
                        "FROM " + RUNTIME_IMAGE);

        assertThat(dockerfile).contains(
                "COPY frontend/ ./",
                "RUN pnpm run build:standalone",
                "COPY --from=frontend-build /app/out /var/www/html");

        int sourcesCopy = dockerfile.indexOf("COPY src src");
        int backendBuild = dockerfile.indexOf("RUN ./mvnw -B clean package");
        assertThat(sourcesCopy).isGreaterThanOrEqualTo(0);
        assertThat(backendBuild).isGreaterThan(sourcesCopy);
        assertThat(dockerfile).contains(
                "COPY --from=backend-build /workspace/target/*.jar /app/application.jar");
    }

    @Test
    void runtimeStartsProductionProfileWithoutBakedSecrets() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile).contains(
                "-Dspring.profiles.active=prod",
                "--enable-native-access=ALL-UNNAMED",
                "-jar /app/application.jar");
        assertThat(dockerfile).doesNotContain(
                "ENV SPRING_DATASOURCE_PASSWORD",
                "ENV TELEGRAM_BOT_TOKEN",
                "ENV SENTRY_DSN",
                "ENV LEAD_FINGERPRINT_HMAC_KEY");
    }

    @Test
    void dockerContextExcludesEnvironmentCredentialsAndLocalKeys() throws Exception {
        var patterns = Files.readAllLines(Path.of(".dockerignore")).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();

        assertThat(patterns).contains(
                ".env*", "**/.env*",
                ".secrets", "**/.secrets", "secrets", "**/secrets",
                ".credentials", "**/.credentials", "credentials", "**/credentials",
                ".aws", "**/.aws", ".azure", "**/.azure", ".docker", "**/.docker",
                ".ssh", "**/.ssh", ".gnupg", "**/.gnupg",
                ".kube", "**/.kube", ".config/gcloud", "**/.config/gcloud",
                "*.pem", "**/*.pem", "*.key", "**/*.key",
                "*.p12", "**/*.p12", "*.pfx", "**/*.pfx",
                "*.jks", "**/*.jks", "*.keystore", "**/*.keystore",
                "id_rsa", "**/id_rsa", "id_ed25519", "**/id_ed25519");
    }
}
