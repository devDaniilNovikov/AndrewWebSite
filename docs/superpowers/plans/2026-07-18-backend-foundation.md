# Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the one-module Spring Boot backend, owner-triggered Jules CI gates, and a non-root Java container without implementing lead persistence.

**Architecture:** The root Maven module owns the Java application and safe Actuator foundation. CI runs the wrapper and coverage/security gates, while the deploy stub packages the same executable JAR into a multi-stage image. These are three sequential product tasks and three separate PRs from fresh `origin/main`.

**Tech Stack:** Java 25 LTS, Spring Boot 4.1.0, Maven Wrapper 3.3.4, Spring MVC, Validation, Actuator, Micrometer, JUnit Jupiter, MockMvc, JaCoCo, Docker, Temurin 25

## Global Constraints

- One Maven module at root `pom.xml`; Maven Wrapper; Java 25 LTS; Spring Boot 4.1.0; root package `ru.andrew.website`; feature-oriented packages.
- PostgreSQL target is managed PostgreSQL 17; do not introduce H2, Redis, a broker, authentication, sessions, form login, HTTP Basic, public raw metrics, environment, configuration, shutdown, or heap endpoints.
- Frontend ownership remains under `frontend/`; Next.js 16.2.9, React 19.2.x, TypeScript strict, Tailwind CSS 4, Motion, and Node 24 only at build time.
- Lead contract remains empty indistinguishable `202`; RFC 9457 `400/409/413/415/429/503`; name 2–100, phone input 32 and normalized digits 7–15, optional comment 1000, local source path, intents exactly `repair|maintenance`, consent exactly true, HMAC key only from `LEAD_FINGERPRINT_HMAC_KEY`.
- Bounded in-memory limits remain a rolling global maximum of 60 admissions in every `(t - 60 seconds, t]` interval and a separate per-connection burst 5/refill 1 token per minute; forwarded headers remain untrusted until Timeweb CIDRs are verified.
- Outbox states remain exactly `pending|processing|retry|blocked|delivered`; poll 15 seconds, batch 10, two-minute lease, deterministic `FOR UPDATE SKIP LOCKED`, HTTP after claim commit, retry 30 seconds through six hours, Telegram `retry_after` seconds, and accepted at-least-once duplicates.
- PII hard limit is 30 days, operational anonymization is 29 days, fingerprint is removed, undelivered work blocks as `privacy_expired`, technical rows delete after 12 months, and backup/Telegram auto-delete are each at most 30 days.
- Liveness is dependency-free. Readiness ultimately combines PostgreSQL availability and a worker heartbeat while returning no sensitive detail.
- Actuator/Micrometer are present from foundation; OTLP is not configured until `task-backend-observability`.
- JaCoCo line coverage must be at least 80%; all third-party GitHub Actions are pinned to full commit SHAs and use least privilege.
- No secrets, lead PII, credentials, private keys, request bodies, or realistic token-shaped fixtures in source, logs, tests, images, Issues, or workflow output.
- No stacked PRs. Each product task starts from fresh `origin/main` after its predecessor is merged, creates one reviewable PR, and is never automatically merged.
- Every AI-authored commit adds the executing agent's own `Co-Authored-By` attribution footer and never attributes a human identity.

---

### Task 1: `task-backend-skeleton` — Maven application and safe health foundation

**Files:**
- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `.mvn/wrapper/maven-wrapper.jar`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `src/main/java/ru/andrew/website/AndrewWebsiteApplication.java`
- Create: `src/main/java/ru/andrew/website/common/TimeConfiguration.java`
- Create: `src/main/java/ru/andrew/website/common/RuntimeProfileGuard.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `src/main/resources/application-prod.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/java/ru/andrew/website/AndrewWebsiteApplicationTest.java`
- Create: `src/test/java/ru/andrew/website/common/RuntimeProfileGuardTest.java`
- Create: `src/test/java/ru/andrew/website/observability/LivenessContractTest.java`

**Interfaces:**
- Consumes: the public health paths and version rules from `docs/backend/architecture.md` and `docs/backend/openapi.yaml`.
- Produces: `ru.andrew.website.AndrewWebsiteApplication`; fail-safe `RuntimeProfileGuard` requiring exactly one explicit `test|local|prod` profile; executable `./mvnw`; `GET /actuator/health/liveness`; temporary foundation readiness at `GET /actuator/health/readiness`; Maven `verify` with JaCoCo line ratio `0.80`.

- [ ] **Step 1: Create the manifest, then generate the Maven Wrapper before using it**

Create `pom.xml` with this complete foundation model:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
  </parent>
  <groupId>ru.andrew</groupId>
  <artifactId>andrew-website</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>25</java.version>
    <jacoco.version>0.8.14</jacoco.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>${jacoco.version}</version>
        <executions>
          <execution><goals><goal>prepare-agent</goal></goals></execution>
          <execution><id>report</id><phase>verify</phase><goals><goal>report</goal></goals></execution>
          <execution>
            <id>coverage-check</id><phase>verify</phase><goals><goal>check</goal></goals>
            <configuration>
              <rules><rule><element>BUNDLE</element><limits><limit>
                <counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.80</minimum>
              </limit></limits></rule></rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Run with a locally installed Maven only for wrapper creation:

```bash
mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper
```

Expected: exit 0 and creation of `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/`. Verify `.mvn/wrapper/maven-wrapper.properties` references Maven Wrapper 3.3.4-generated configuration and contains no mirror credential.

- [ ] **Step 2: RED — write context and health contract tests**

Create the tests:

```java
package ru.andrew.website;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AndrewWebsiteApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

```java
package ru.andrew.website.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LivenessContractTest {
    @Autowired MockMvc mvc;

    @Test
    void livenessIsMinimalAndDependencyFree() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
```

Create a focused context-runner test proving that both missing and contradictory profile activation fail startup. The existing `@ActiveProfiles("test")` context test is the positive case:

```java
package ru.andrew.website.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.andrew.website.AndrewWebsiteApplication;

class RuntimeProfileGuardTest {
    private static final String MESSAGE =
            "Exactly one active profile is required: test, local, or prod";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AndrewWebsiteApplication.class)
            .withPropertyValues("spring.main.web-application-type=none");

    @Test
    void missingActiveProfileFailsStartup() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(MESSAGE);
        });
    }

    @Test
    void multipleActiveProfilesFailStartup() {
        runner.withPropertyValues("spring.profiles.active=local,test").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(MESSAGE);
        });
    }

    @Test
    void unknownActiveProfileFailsStartup() {
        runner.withPropertyValues("spring.profiles.active=staging").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(MESSAGE);
        });
    }
}
```

Run: `./mvnw -B -DskipTests=false test`

Expected: FAIL during test compilation or context discovery because `AndrewWebsiteApplication` does not exist.

- [ ] **Step 3: GREEN — add the application and profile configuration**

Create the bootstrap:

```java
package ru.andrew.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AndrewWebsiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AndrewWebsiteApplication.class, args);
    }
}
```

Create the sole production clock:

```java
package ru.andrew.website.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

Create `application.yml`:

```yaml
spring:
  application:
    name: andrew-website
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState
server:
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
```

Do not set `spring.profiles.default`. Add the startup guard; it treats the complete active-profile set as a closed allowlist and rejects zero, multiple, or unknown profiles before traffic can be accepted:

```java
package ru.andrew.website.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class RuntimeProfileGuard implements InitializingBean {
    private static final Set<String> ALLOWED = Set.of("test", "local", "prod");
    private final Environment environment;

    public RuntimeProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        Set<String> active = Arrays.stream(environment.getActiveProfiles())
                .collect(Collectors.toUnmodifiableSet());
        if (active.size() != 1 || !ALLOWED.containsAll(active)) {
            throw new ApplicationContextException(
                    "Exactly one active profile is required: test, local, or prod");
        }
    }
}
```

Create `application-local.yml`, `application-prod.yml`, and `application-test.yml` with only profile activation. Configuration-file activation never selects a profile by itself: every Spring context test uses `@ActiveProfiles("test")` (or an explicit `spring.profiles.active=test` property), every local launch sets `SPRING_PROFILES_ACTIVE=local`, and production sets only `SPRING_PROFILES_ACTIVE=prod`.

```yaml
spring:
  config:
    activate:
      on-profile: local
```

```yaml
spring:
  config:
    activate:
      on-profile: prod
```

```yaml
spring:
  config:
    activate:
      on-profile: test
```

Run: `./mvnw -B test`

Expected: PASS with the positive `test`-profile context green and the missing/multiple/unknown-profile context runners failing for the exact guard message.

- [ ] **Step 4: REFACTOR and verify the complete foundation gate**

Keep the bootstrap free of feature logic, confirm `dependency:tree` contains `spring-boot-starter-webmvc` rather than deprecated `spring-boot-starter-web`, and run:

```bash
./mvnw -B verify
./mvnw -B dependency:tree
```

Expected: both commands exit 0; JaCoCo line coverage is at least 0.80; only the `health` actuator endpoint is exposed.

- [ ] **Step 5: Commit the product task**

```bash
git add pom.xml .mvn mvnw mvnw.cmd src
git commit -m "feat(backend-skeleton): add Spring Boot foundation"
```

### Task 2: `task-ci-backend-gates` — owner-dispatched Jules CI PR

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `pom.xml`
- Test: `.github/workflows/ci.yml` through `actionlint` and a trusted branch push

**Interfaces:**
- Consumes: merged `task-backend-skeleton`, `./mvnw -B verify`, JaCoCo `0.80`, and `.github/JULES_AUTOMATION.md`.
- Produces: least-privilege CI jobs for Temurin 25 verification, dependency review, Java security analysis, and Testcontainers-compatible Docker execution.

- [ ] **Step 1: Start Jules only through the approved Issue boundary**

Confirm `task-backend-skeleton` is merged. `JULES_ALLOWED_ACTOR` creates a new sanitized Issue containing only this task's verified requirements, then the same account applies exactly one label: `jules-action`. Do not apply `jules`. Do not label an Issue authored by another account. A successful skeleton merge does not create this task; `jules-ci-failure.yml` is only for eligible failed trusted pushes, and `pr-event-relay.yml` does not dispatch Jules.

- [ ] **Step 2: RED — add a CI contract assertion before the workflow**

Run this repository-only check before creating `ci.yml`:

```bash
test -f .github/workflows/ci.yml
```

Expected: exit 1 because the backend CI workflow is absent.

- [ ] **Step 3: GREEN — create the least-privilege workflow and security gate**

Before writing YAML, resolve the current immutable commits with `gh api repos/actions/checkout/git/ref/tags/v4`, `gh api repos/actions/setup-java/git/ref/tags/v4`, `gh api repos/actions/dependency-review-action/git/ref/tags/v5.0.0`, and `gh api repos/github/codeql-action/git/ref/tags/v3`. Dereference an annotated tag through its returned tag object until a commit object is reached, and record that 40-hex commit in the workflow. The committed workflow must contain no mutable `@vN` references. Create jobs with these exact semantics:

```yaml
name: CI
on:
  push:
    branches: [main, 'task-*', 'fix-*', 'codex/**']
  pull_request:
permissions:
  contents: read
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - name: Check out repository
        uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5
      - name: Set up Temurin 25
        uses: actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - name: Verify
        run: ./mvnw -B verify
  dependency-review:
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - name: Check out repository
        uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5
      - name: Review dependency changes
        uses: actions/dependency-review-action@a1d282b36b6f3519aa1f3fc636f609c47dddb294
  java-security:
    if: github.event_name == 'push'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write
    steps:
      - name: Check out repository
        uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5
      - name: Initialize CodeQL for Java
        uses: github/codeql-action/init@b7351df727350dca84cb9d725d57dcf5bc82ba26
        with:
          languages: java-kotlin
      - name: Set up Temurin 25
        uses: actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - name: Build for analysis
        run: ./mvnw -B -DskipTests package
      - name: Analyze
        uses: github/codeql-action/analyze@b7351df727350dca84cb9d725d57dcf5bc82ba26
```

The executor must replace an action SHA above only when the tag-resolution check proves a newer approved immutable commit; review the diff before commit. Docker is available on `ubuntu-latest`, so later PostgreSQL Testcontainers tests run under the same `verify` job without a service container.

- [ ] **Step 4: REFACTOR and validate workflow behavior**

Run:

```bash
actionlint .github/workflows/ci.yml
git grep -nE 'uses: [^ ]+@v[0-9]'
./mvnw -B verify
```

Expected: `actionlint` and Maven exit 0; the mutable-action grep has no matches and exits 1. Push only the task branch, wait for all three applicable jobs, and confirm no workflow can merge a PR or write repository contents.

- [ ] **Step 5: Commit and stop at a reviewable PR**

```bash
git add .github/workflows/ci.yml pom.xml
git commit -m "ci(backend-gates): enforce Java verification and security"
```

Jules creates a reviewable PR. It must not merge it.

### Task 3: `task-backend-deploy-stub` — non-root Java image

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Create: `src/test/java/ru/andrew/website/deployment/ContainerContractTest.java`

**Interfaces:**
- Consumes: root Maven Wrapper and executable `target/andrew-website-0.0.1-SNAPSHOT.jar`.
- Produces: multi-stage `andrew-website:local` image, numeric non-root runtime user, port 8080, and liveness-based container healthcheck.

- [ ] **Step 1: RED — write an image contract test**

```java
package ru.andrew.website.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContainerContractTest {
    @Test
    void dockerfileIsMultiStageNonRootAndChecksLiveness() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        assertThat(dockerfile).contains("AS build", "USER 10001:10001");
        assertThat(dockerfile).contains("/actuator/health/liveness");
        assertThat(dockerfile).doesNotContain("ENV SPRING_DATASOURCE_PASSWORD");
        assertThat(dockerfile).doesNotContain("ENV TELEGRAM_BOT_TOKEN");
    }
}
```

Run: `./mvnw -B -Dtest=ContainerContractTest test`

Expected: FAIL because `Dockerfile` does not exist.

- [ ] **Step 2: GREEN — create the build and runtime boundary**

Create `Dockerfile`:

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B verify

FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/andrew-website-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/liveness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
```

Create `.dockerignore`:

```text
.git
.github
.superpowers
docs
frontend
target
*.md
```

Run: `./mvnw -B -Dtest=ContainerContractTest test`

Expected: PASS.

- [ ] **Step 3: REFACTOR — build and smoke the exact image without secrets**

```bash
./mvnw -B verify
docker build --tag andrew-website:local .
docker run --detach --name andrew-website-smoke --publish 18080:8080 \
  --env SPRING_PROFILES_ACTIVE=local andrew-website:local
curl --fail --silent http://127.0.0.1:18080/actuator/health/liveness
docker inspect --format '{{.State.Health.Status}} {{.Config.User}}' andrew-website-smoke
docker stop andrew-website-smoke
docker rm andrew-website-smoke
```

Expected: Maven and build exit 0; liveness returns `{"status":"UP"}`; inspect eventually reports `healthy 10001:10001`. If smoke setup fails, remove only the named `andrew-website-smoke` container after inspecting its PII-free logs. Resolve and pin approved base-image digests during implementation if the repository's container policy requires digest pinning.

- [ ] **Step 4: Commit without deploying**

```bash
git add Dockerfile .dockerignore src/test/java/ru/andrew/website/deployment/ContainerContractTest.java
git commit -m "feat(backend-deploy-stub): add non-root Java container"
```

Do not push an image, change Timeweb, or bind production secrets in this task.
