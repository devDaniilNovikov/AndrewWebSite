package ru.andrew.website.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContainerContractTest {
    private static final String BUILD_IMAGE = "eclipse-temurin:25.0.3_9-jdk-noble"
            + "@sha256:735baf2edc6cd6485240144a84fa4db142b9a6f47b4eb4080f31058d200f9813";
    private static final String RUNTIME_IMAGE = "eclipse-temurin:25.0.3_9-jre-noble"
            + "@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db";

    @Test
    void dockerfileIsMultiStageNonRootAndChecksLiveness() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile).contains(
                "AS backend-build",
                "COPY Dockerfile .dockerignore ./",
                "COPY --from=backend-build",
                "FROM " + RUNTIME_IMAGE,
                "USER 10001:10001");
        assertThat(dockerfile).doesNotContain("apk add", "-alpine");
        assertThat(dockerfile).contains(
                "/bin/bash -ec",
                "/dev/tcp/127.0.0.1/8081",
                "GET /actuator/health/liveness HTTP/1.1",
                "test \"$status\" = 200");
        assertThat(dockerfile)
                .doesNotContain("127.0.0.1:8080/actuator/health/liveness");
        assertThat(dockerfile).contains(
                "ENTRYPOINT [\"java\", "
                        + "\"--enable-native-access=ALL-UNNAMED\", "
                        + "\"-jar\", \"/app/application.jar\"]");
        assertThat(dockerfile).doesNotContain("ENV SPRING_DATASOURCE_PASSWORD");
        assertThat(dockerfile).doesNotContain("ENV TELEGRAM_BOT_TOKEN");

        int dockerfileCopy = dockerfile.indexOf("COPY Dockerfile .dockerignore ./");
        String verifyCommand = "RUN ./mvnw -B -DexcludedGroups=database verify";
        int mavenVerify = dockerfile.indexOf(verifyCommand);
        assertThat(dockerfileCopy).isGreaterThanOrEqualTo(0);
        assertThat(mavenVerify).isGreaterThan(dockerfileCopy);
        assertThat(dockerfile)
                .doesNotContain("-DskipTests", "maven.test.skip");
        assertThat(dockerfile.lines()
                        .filter(line -> line.startsWith("RUN ./mvnw") && line.contains("verify")))
                .containsExactly(verifyCommand);
    }

    @Test
    void dockerfilePinsEveryBaseImageAndAvoidsMutablePackageRepositories() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile.lines().filter(line -> line.startsWith("FROM ")))
                .containsExactly(
                        "FROM " + BUILD_IMAGE + " AS backend-build",
                        "FROM " + RUNTIME_IMAGE);
        assertThat(dockerfile)
                .doesNotContain("apt-get", "apk add", "curl", "-alpine");
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
