package ru.andrew.website.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContainerContractTest {
    @Test
    void dockerfileIsMultiStageNonRootAndChecksLiveness() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile).contains(
                "AS backend-build",
                "COPY Dockerfile .dockerignore ./",
                "COPY --from=backend-build",
                "USER 10001:10001");
        assertThat(dockerfile).contains("/actuator/health/liveness");
        assertThat(dockerfile).doesNotContain("ENV SPRING_DATASOURCE_PASSWORD");
        assertThat(dockerfile).doesNotContain("ENV TELEGRAM_BOT_TOKEN");

        int dockerfileCopy = dockerfile.indexOf("COPY Dockerfile .dockerignore ./");
        String verifyCommand = "RUN ./mvnw -B -DexcludedGroups=database verify";
        int mavenVerify = dockerfile.indexOf(verifyCommand);
        assertThat(dockerfileCopy).isGreaterThanOrEqualTo(0);
        assertThat(mavenVerify).isGreaterThan(dockerfileCopy);
        assertThat(dockerfile)
                .doesNotContain("maven.test.skip");
        assertThat(dockerfile.lines()
                        .filter(line -> line.startsWith("RUN ./mvnw") && line.contains("verify")))
                .containsExactly(verifyCommand);
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
