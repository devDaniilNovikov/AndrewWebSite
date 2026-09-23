package ru.andrew.website.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards the shape of the production image: a frontend build stage, a backend build stage
 * compiled from the exact sources in this repository, and a pinned Java 25 runtime where
 * nginx serves the static frontend on 8080 and proxies only {@code /api/leads} to the
 * Spring Boot application on loopback.
 *
 * <p>The {@code container-build} job in {@code .github/workflows/ci.yml} additionally
 * asserts the non-root user and the liveness health check against the built image.
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
                "RUN pnpm install --frozen-lockfile",
                "RUN pnpm run build:production",
                "COPY --from=frontend-build /app/out /var/www/html");

        int sourcesCopy = dockerfile.indexOf("COPY src src");
        int backendBuild = dockerfile.indexOf("RUN ./mvnw -B clean package");
        assertThat(sourcesCopy).isGreaterThanOrEqualTo(0);
        assertThat(backendBuild).isGreaterThan(sourcesCopy);
        assertThat(dockerfile).contains(
                "COPY --from=backend-build /workspace/target/*.jar /app/application.jar");
    }

    @Test
    void runtimeIsNonRootSupervisedAndHealthChecked() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile).contains(
                "COPY deploy/nginx.conf /etc/nginx/nginx.conf",
                "COPY deploy/entrypoint.sh /app/entrypoint.sh",
                "USER 10001:10001",
                "EXPOSE 8080",
                "/dev/tcp/127.0.0.1/8081",
                "GET /actuator/health/liveness",
                "ENTRYPOINT [\"/app/entrypoint.sh\"]");
        assertThat(dockerfile.indexOf("USER 10001:10001"))
                .isGreaterThan(dockerfile.lastIndexOf("RUN "));
        assertThat(dockerfile).doesNotContain(
                "ENV TELEGRAM_BOT_TOKEN",
                "ENV TELEGRAM_CHAT_ID",
                "ENV LEAD_FINGERPRINT_HMAC_KEY");
    }

    @Test
    void entrypointStartsProductionProfileAndStopsWithEitherProcess() throws Exception {
        String entrypoint = Files.readString(Path.of("deploy/entrypoint.sh"));

        assertThat(entrypoint).contains(
                "-Dspring.profiles.active=prod",
                "--enable-native-access=ALL-UNNAMED",
                "-jar /app/application.jar",
                "nginx -e /dev/stderr -c /etc/nginx/nginx.conf -g 'daemon off;'",
                "wait -n \"$java_pid\" \"$nginx_pid\"",
                "exit 1");
        assertThat(Files.isExecutable(Path.of("deploy/entrypoint.sh"))).isTrue();
    }

    @Test
    void nginxExposesOnlyTheLeadEndpointAndForwardsOnlyTheVisitorAddress() throws Exception {
        String nginx = Files.readString(Path.of("deploy/nginx.conf"));

        assertThat(nginx).contains(
                "listen 8080 default_server;",
                "location = /api/leads {",
                "proxy_pass http://127.0.0.1:8090;",
                "proxy_set_header X-Real-IP $lead_client_ip;",
                "proxy_set_header X-Forwarded-For \"\";",
                "proxy_set_header X-Forwarded-Proto \"\";",
                "proxy_set_header Forwarded \"\";",
                "client_max_body_size 32k;",
                "access_log off;",
                "server_tokens off;",
                "pid /tmp/nginx.pid;");
        assertThat(nginx).containsSubsequence("location /api/ {", "return 404;");
        assertThat(nginx).containsSubsequence("location /actuator/ {", "return 404;");
        assertThat(nginx).doesNotContain("proxy_pass http://127.0.0.1:8090/", "user ");
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
