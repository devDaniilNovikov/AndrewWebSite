# Static Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the merged Next.js static export into the executable Spring Boot JAR and ship one production Java container with correct routing, caching, and smoke coverage.

**Architecture:** The merged frontend's declared package manager builds Next.js `out/` under Node 24.18.0. Maven copies that immutable export into generated JAR resources, and a narrow static handler resolves real exported pages/assets without catching `/api/**` or `/actuator/**`. A multi-stage container discards Node and serves the single Java artifact as a non-root user.

**Tech Stack:** Next.js 16.2.9, React 19.2.x, TypeScript strict, Tailwind CSS 4, Motion, Node 24.18.0 LTS at build time, Maven, Spring Boot 4.1.0, Java 25, Docker

## Global Constraints

- One root Maven module; Java 25 LTS; Spring Boot 4.1.0; package `ru.andrew.website`; managed PostgreSQL 18.
- Do not start until refreshed Claude instructions and the Claude-owned frontend scaffold, package-manager declaration, lockfile, static-export command, output path, and frontend tests are merged into fresh `origin/main`.
- Frontend remains under `frontend/`; use its one committed package manager and lockfile without conversion or mixing; Next.js is exactly 16.2.9 with `output: 'export'`; the build artifact is `frontend/out/`.
- Node 24 is build-time only. Use a writable `COREPACK_HOME` and direct `corepack <manager>` execution for the exact manifest declaration; never run `corepack enable` or install global shims. The final artifact and container contain one Spring Boot executable JAR on Java 25 and no Node executable, package-manager cache, frontend source, or build credential.
- Preserve backend ownership of `/api/**` and `/actuator/**`; no static fallback on those prefixes. Missing static routes are real 404 responses, never home-page fallbacks.
- Lead API remains JSON-only at 16 KiB with empty indistinguishable `202`, RFC 9457 `400/409/413/415/429/503`, exact validation bounds/intents/consent, production HMAC only from `LEAD_FINGERPRINT_HMAC_KEY`, and no PII in logs/metrics/problems.
- Bounded limits remain a rolling global maximum of 60 admissions in every `(t - 60 seconds, t]` interval and a separate per-connection burst 5/refill 1 token per minute; forwarded headers remain untrusted until verified Timeweb CIDRs.
- Outbox remains separate and PII-free with exact `pending|processing|retry|blocked|delivered`, 15-second poll, batch 10, two-minute lease, deterministic `FOR UPDATE SKIP LOCKED`, HTTP after claim commit, retry 30 seconds through six hours, Telegram `retry_after` seconds, and accepted at-least-once duplicates.
- PII hard limit is 30 days; anonymize/fingerprint removal at 29 days, `privacy_expired` blocking, technical deletion after 12 months, and backup/Telegram auto-delete each at most 30 days.
- Liveness remains dependency-free; readiness is PostgreSQL plus worker heartbeat with no detail; telemetry is bounded/PII-free and OTLP is private.
- HTML uses revalidation/no-cache semantics; `/_next/static/**` hashed assets use one-year public immutable caching; health uses `no-store`.
- Final smoke coverage includes home, an exported nested page, a hashed asset, real 404, `POST /api/leads`, liveness, and readiness.
- Production image is non-root, health-checks liveness, embeds no secrets, and does not mutate Timeweb or any production resource.
- PostgreSQL backup retention and Telegram auto-delete are each verified at no more than 30 days before release; proxy trust and OTLP gates remain in force.
- Follow the [canonical Git Flow](../../../.agents/workflows/GIT_FLOW.md): after all backend and frontend prerequisites reach `main`, this approved product task uses one dedicated external worktree and one lowercase `task-*` branch from the latest `origin/main`, followed by one Draft PR; direct pushes to `main`, stacked PRs, reused worktrees, non-squash merges, and auto-merge are forbidden. Mark Ready only after required CI is green and Codex review is complete; squash-merge only after explicit user authorization; then confirm `main`, close the issue, allow automatic remote-branch deletion, remove only a worktree with no tracked or untracked work to preserve, and run `git fetch --prune`. Preserve strict RED → GREEN → REFACTOR.
- Every AI-authored commit adds the executing agent's own `Co-Authored-By` attribution footer and never attributes a human identity.

---

### Task 1: Verify the merged frontend and embed its deterministic export

**Files:**
- Create: `scripts/build-frontend.sh`
- Modify: `pom.xml`
- Modify: `.github/workflows/ci.yml`
- Create: `src/test/java/ru/andrew/website/deployment/StaticJarIT.java`
- Consume without modifying unless a verified contract defect is found: `frontend/package.json`
- Consume: exactly one of `frontend/package-lock.json`, `frontend/pnpm-lock.yaml`, or `frontend/yarn.lock`
- Consume: `frontend/next.config.ts` or the merged equivalent
- Consume: `frontend/out/` as generated content, never as tracked source

**Interfaces:**
- Consumes: merged `package.json#packageManager`, `package.json#scripts.build`, lockfile, frontend tests, and Next.js static-export config.
- Produces: `scripts/build-frontend.sh`; generated `frontend/out/`; JAR entries under `BOOT-INF/classes/static/`; Maven `verify` failure when export is absent.

- [ ] **Step 1: Gate execution on the merged prerequisite without changing frontend ownership**

From the dedicated `task-static-jar-integration` worktree and branch created from the latest `origin/main`, verify all backend task PRs and the frontend prerequisite are merged, then inspect the merged frontend manifest. Require `next` exactly `16.2.9`, React `19.2.x`, strict TypeScript, Tailwind CSS 4, Motion, `output: 'export'`, output `out/`, a package-manager declaration with exactly one matching lockfile, a static-safe build script, and passing frontend tests. Stop and return to the frontend owner if any fact is absent or contradictory; do not choose a package manager or output convention on their behalf.

- [ ] **Step 2: RED — write the JAR-content integration test before build integration**

```java
package ru.andrew.website.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class StaticJarIT {
    @Test
    void executableJarContainsTheStaticExport() throws IOException {
        Path jar = Files.list(Path.of("target"))
                .filter(path -> path.getFileName().toString().startsWith("andrew-website-"))
                .filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> !path.toString().endsWith(".original"))
                .findFirst()
                .orElseThrow();
        try (JarFile file = new JarFile(jar.toFile())) {
            assertThat(file.getEntry("BOOT-INF/classes/static/index.html")).isNotNull();
            assertThat(file.stream().anyMatch(entry ->
                    entry.getName().startsWith("BOOT-INF/classes/static/_next/static/")
                            && entry.getName().endsWith(".js"))).isTrue();
            assertThat(file.stream().noneMatch(entry -> entry.getName().contains("node_modules"))).isTrue();
        }
    }
}
```

Bind Maven Failsafe `integration-test` and `verify` goals for `**/*IT.java`, but do not copy resources yet. Run the frontend's already-declared test command, then `./mvnw -B verify`.

Expected: frontend tests PASS; Maven FAIL in `StaticJarIT` because the JAR lacks `BOOT-INF/classes/static/index.html`.

- [ ] **Step 3: GREEN — build with the declared manager and copy generated resources**

Create one complete manager-dispatch script; it rejects mixed/missing locks and always runs the merged `test` then `build` scripts:

```bash
#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../frontend" && pwd)"
cd "$project_dir"

: "${COREPACK_HOME:=${TMPDIR:-/tmp}/andrew-corepack-${UID}}"
export COREPACK_HOME
mkdir -p "$COREPACK_HOME"
[[ -w "$COREPACK_HOME" ]] || { echo "COREPACK_HOME must be writable" >&2; exit 2; }

declared="$(node -p "require('./package.json').packageManager || ''")"
manager="${declared%%@*}"
version="${declared#*@}"
if [[ "$declared" == "$manager" || -z "$version" ]]; then
  echo "packageManager must include a version" >&2
  exit 2
fi
lock_count=0
for lock in package-lock.json pnpm-lock.yaml yarn.lock; do
  if [[ -f "$lock" ]]; then
    lock_count=$((lock_count + 1))
  fi
done

if [[ "$lock_count" -ne 1 ]]; then
  echo "frontend must contain exactly one supported lockfile" >&2
  exit 2
fi

case "$manager" in
  npm)
    [[ -f package-lock.json ]] || { echo "npm requires package-lock.json" >&2; exit 2; }
    corepack npm ci
    corepack npm test
    corepack npm run build
    ;;
  pnpm)
    [[ -f pnpm-lock.yaml ]] || { echo "pnpm requires pnpm-lock.yaml" >&2; exit 2; }
    corepack pnpm install --frozen-lockfile
    corepack pnpm test
    corepack pnpm run build
    ;;
  yarn)
    [[ -f yarn.lock ]] || { echo "yarn requires yarn.lock" >&2; exit 2; }
    corepack yarn install --immutable
    corepack yarn test
    corepack yarn build
    ;;
  *)
    echo "unsupported or missing packageManager declaration" >&2
    exit 2
    ;;
esac

test -f out/index.html
test -d out/_next/static
```

Corepack's official CLI contract permits `corepack <binary> [...args]` without
installing shims, and `COREPACK_HOME` selects its cache directory. `corepack enable`
is forbidden here because it writes shims beside the Corepack binary and the
arbitrary host UID cannot write the system Node installation. The npm branch is
intentional and explicit: npm is excluded from Corepack's default shim installation,
but direct `corepack npm ...` honors the committed `packageManager` version without a
global shim.

Configure resource copying early enough for focused MVC tests and configure Failsafe after packaging:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-resources-plugin</artifactId>
  <executions>
    <execution>
      <id>copy-static-export</id>
      <phase>process-resources</phase>
      <goals><goal>copy-resources</goal></goals>
      <configuration>
        <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
        <resources>
          <resource><directory>frontend/out</directory><filtering>false</filtering></resource>
        </resources>
      </configuration>
    </execution>
  </executions>
</plugin>
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <executions>
    <execution><goals><goal>integration-test</goal><goal>verify</goal></goals></execution>
  </executions>
</plugin>
```

Do not track `frontend/out/`; keep it ignored by the merged frontend rules.

In `.github/workflows/ci.yml`, insert this step in both `verify` and `java-security`, immediately before their Maven build command; Docker is already required for PostgreSQL Testcontainers:

```yaml
      - name: Test and export frontend with Node 24.18.0
        run: |
          docker run --rm --user "$(id -u):$(id -g)" --env HOME=/tmp \
            --env COREPACK_HOME=/tmp/corepack \
            --volume "$PWD:/workspace" --workdir /workspace node:24.18.0 \
            bash scripts/build-frontend.sh
```

Run:

```bash
docker run --rm --user "$(id -u):$(id -g)" --env HOME=/tmp \
  --env COREPACK_HOME=/tmp/corepack \
  --volume "$PWD:/workspace" --workdir /workspace node:24.18.0 \
  bash scripts/build-frontend.sh
./mvnw -B verify
```

Expected: the disposable builder uses Node `v24.18.0`; the merged frontend tests/build PASS; Maven PASS; `StaticJarIT` finds HTML and hashed JS and no `node_modules`.

- [ ] **Step 4: REFACTOR and commit the build boundary**

Run `git status --short` and confirm `frontend/out/` and dependency caches are untracked/ignored. Run `./mvnw -B verify` again without changing inputs and compare the normalized static entry list. Expected: same file list and all gates PASS.

```bash
git add pom.xml .github/workflows/ci.yml scripts/build-frontend.sh src/test/java/ru/andrew/website/deployment/StaticJarIT.java
git commit -m "feat(static-jar-integration): embed frontend export in the JAR"
```

### Task 2: Preserve backend routes, real 404s, cache policy, and final Java image

**Files:**
- Create: `src/main/java/ru/andrew/website/web/StaticAssetPolicy.java`
- Create: `src/main/java/ru/andrew/website/web/StaticPageController.java`
- Create: `src/test/java/ru/andrew/website/web/StaticPageControllerTest.java`
- Test: `src/test/java/ru/andrew/website/deployment/ContainerContractTest.java` unchanged against the final Dockerfile and `.dockerignore` security patterns
- Modify: `Dockerfile`
- Modify: `.dockerignore`
- Create: `scripts/smoke-final-container.sh`

**Interfaces:**
- Consumes: classpath `static/` export, existing `/api/**` controller/security, health groups, database configuration, and exact final image.
- Produces: `StaticPageController.serve(String)` with no SPA fallback; `StaticAssetPolicy.cacheControl(String)`; final non-root Java image; seven-part smoke script.

- [ ] **Step 1: RED — specify route and cache behavior**

```java
package ru.andrew.website.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticPageControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void servesPagesAndHashedAssetsWithDifferentCacheRules() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"));
        mvc.perform(get(hashedJavascriptPath()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void missingPageIsRealNotFoundAndApiDoesNotFallThrough() throws Exception {
        mvc.perform(get("/missing-fictional-page/")).andExpect(status().isNotFound());
        mvc.perform(get("/api/missing")).andExpect(status().isForbidden());
    }

    private String hashedJavascriptPath() throws IOException {
        Path output = Path.of("frontend/out");
        try (var paths = Files.walk(output.resolve("_next/static"))) {
            Path asset = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".js"))
                    .findFirst().orElseThrow();
            return "/" + output.relativize(asset).toString().replace('\\', '/');
        }
    }
}
```

Tests use generated frontend output copied into test resources by the build setup; they never create a home fallback for a missing path.

Run: `./mvnw -B -Dtest=StaticPageControllerTest test`

Expected: FAIL because the explicit static policy/controller does not exist.

- [ ] **Step 2: GREEN — implement safe classpath resolution and caching**

```java
package ru.andrew.website.web;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;

@Component
public final class StaticAssetPolicy {
    public CacheControl cacheControl(String resourcePath) {
        if (resourcePath.endsWith(".html")) return CacheControl.noCache();
        if (resourcePath.startsWith("_next/static/")) {
            return CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();
        }
        return CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
    }
}
```

```java
package ru.andrew.website.web;

import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public final class StaticPageController {
    private final StaticAssetPolicy cache;

    public StaticPageController(StaticAssetPolicy cache) {
        this.cache = cache;
    }

    @GetMapping({"/", "/{*path}"})
    ResponseEntity<Resource> serve(@PathVariable(required = false) String path) {
        String clean = path == null ? "" : path.replaceFirst("^/", "");
        if (clean.startsWith("api/") || clean.startsWith("actuator/") || clean.contains("..")) {
            return ResponseEntity.<Resource>notFound().build();
        }
        List<String> candidates = clean.isEmpty()
                ? List.of("index.html")
                : clean.contains(".") ? List.of(clean) : List.of(clean + "/index.html");
        return candidates.stream().map(candidate -> new Resolved(candidate,
                        new ClassPathResource("static/" + candidate)))
                .filter(resolved -> resolved.resource().exists()).findFirst()
                .map(resolved -> ResponseEntity.ok()
                        .cacheControl(cache.cacheControl(resolved.path()))
                        .contentType(mediaType(resolved.path())).body(resolved.resource()))
                .orElseGet(() -> ResponseEntity.<Resource>notFound().build());
    }

    private MediaType mediaType(String filename) {
        if (filename.endsWith(".html")) return MediaType.TEXT_HTML;
        if (filename.endsWith(".css")) return MediaType.valueOf("text/css");
        if (filename.endsWith(".js")) return MediaType.valueOf("text/javascript");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private record Resolved(String path, Resource resource) {
    }
}
```

The focused test proves `/_next/static/` receives immutable caching while HTML receives no-cache. Security matcher order continues denying unknown `/api/**` and `/actuator/**` before the static GET matcher.

Run: `./mvnw -B -Dtest=StaticPageControllerTest test`

Expected: PASS for home, nested page, hashed asset, missing page, and API isolation.

- [ ] **Step 3: GREEN — make Node a disposable container stage and smoke the final artifact**

Replace the deploy-stub Dockerfile with the complete three-stage build:

```dockerfile
FROM node:24.18.0 AS frontend-build
WORKDIR /workspace
ENV COREPACK_HOME=/tmp/corepack
COPY frontend frontend
COPY scripts/build-frontend.sh scripts/build-frontend.sh
RUN bash scripts/build-frontend.sh

FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -DskipTests dependency:go-offline
COPY Dockerfile Dockerfile
COPY src src
COPY --from=frontend-build /workspace/frontend/out frontend/out
RUN ./mvnw -B -DexcludedGroups=database verify

FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/target/andrew-website-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/liveness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
```

Use this exact `.dockerignore` content so frontend sources enter only the build stage while dependencies/generated output do not:

```text
.git
.github
.superpowers
docs
target
frontend/node_modules
frontend/.next
frontend/out
.env*
**/.env*
.secrets
**/.secrets
secrets
**/secrets
.credentials
**/.credentials
credentials
**/credentials
.aws
**/.aws
.azure
**/.azure
.docker
**/.docker
.ssh
**/.ssh
.gnupg
**/.gnupg
.kube
**/.kube
.config/gcloud
**/.config/gcloud
*.pem
**/*.pem
*.key
**/*.key
*.p12
**/*.p12
*.pfx
**/*.pfx
*.jks
**/*.jks
*.keystore
**/*.keystore
id_rsa
**/id_rsa
id_ed25519
**/id_ed25519
*.md
```

The unchanged `ContainerContractTest` reads `.dockerignore` and asserts every pattern
above, so root `.env`, nested/frontend `.env.local`, common local secret/credential
directories, PEM/private-key files, PKCS#12 files, Java keystores, and local SSH keys
cannot silently re-enter the context before `COPY frontend frontend`.

Create `scripts/smoke-final-container.sh` with this bounded setup and cleanup. It uses only conspicuously non-production values:

```bash
#!/usr/bin/env bash
set -euo pipefail

image="${1:?pass the local image tag}"
network="andrew-final-smoke-network"
database="andrew-final-smoke-postgres"
application="andrew-website-final-smoke"

cleanup() {
  docker rm --force "$application" >/dev/null 2>&1 || true
  docker rm --force "$database" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
}
trap cleanup EXIT
cleanup
docker network create "$network" >/dev/null
docker run --detach --name "$database" --network "$network" \
  --env POSTGRES_DB=smoke --env POSTGRES_USER=smoke \
  --env POSTGRES_PASSWORD=smoke-only-not-a-secret postgres:18-alpine >/dev/null

for attempt in $(seq 1 15); do
  if docker exec "$database" pg_isready --quiet --timeout=1 \
      --username smoke --dbname smoke; then
    break
  fi
  if [[ "$attempt" -eq 15 ]]; then
    echo 'PostgreSQL smoke fixture did not become ready within 15 bounded attempts' >&2
    docker inspect --format '{{json .State}}' "$database" >&2
    docker logs --tail 50 "$database" >&2
    exit 1
  fi
  sleep 1
done

docker run --detach --name "$application" --network "$network" --publish 18080:8080 \
  --env SPRING_PROFILES_ACTIVE=local \
  --env SPRING_DATASOURCE_URL=jdbc:postgresql://andrew-final-smoke-postgres:5432/smoke \
  --env SPRING_DATASOURCE_USERNAME=smoke \
  --env SPRING_DATASOURCE_PASSWORD=smoke-only-not-a-secret \
  --env APP_LEADS_FINGERPRINT_KEY=test-only-key-material-not-for-production-0001 \
  --env APP_TELEGRAM_BOT_TOKEN=test-only-bot-token \
  --env APP_TELEGRAM_CHAT_ID=test-only-chat \
  --env APP_TELEGRAM_BASE_URL=http://127.0.0.1:9 \
  "$image" >/dev/null

for attempt in $(seq 1 60); do
  if curl --fail --silent http://127.0.0.1:18080/actuator/health/readiness >/dev/null; then break; fi
  if [[ "$attempt" -eq 60 ]]; then exit 1; fi
  sleep 1
done

curl --fail --silent http://127.0.0.1:18080/
asset_path="$(curl --fail --silent http://127.0.0.1:18080/ \
  | grep -o '/_next/static/[^" ]*\.js' | head -n 1)"
test -n "$asset_path"
curl --fail --silent "http://127.0.0.1:18080${asset_path}" >/dev/null
test "$(curl --silent --output /dev/null --write-out '%{http_code}' http://127.0.0.1:18080/missing-fictional-page/)" = "404"
lead_body="$(mktemp)"
lead_status="$(curl --silent --output "$lead_body" --write-out '%{http_code}' \
  --header 'Content-Type: application/json' \
  --data '{"requestId":"33333333-3333-4333-8333-333333333333","name":"Тест","phone":"79990000000","sourcePath":"/","intent":"repair","consent":true,"website":""}' \
  http://127.0.0.1:18080/api/leads)"
test "$lead_status" = "202"
test ! -s "$lead_body"
rm "$lead_body"
curl --fail --silent http://127.0.0.1:18080/actuator/health/liveness
curl --fail --silent http://127.0.0.1:18080/actuator/health/readiness
test "$(docker inspect --format '{{.Config.User}}' "$application")" = "10001:10001"
```

The script runs at most 15 one-second `pg_isready` probes, with a one-second pause after each non-final failure, before it starts the application. On terminal failure it emits only the database container state plus the last 50 fixture log lines; it never inspects environment variables or expands credentials. It then obtains the actual hashed asset path from built `index.html`, asserts the lead response status is 202 with an empty body, and cleans up only its exact named resources in a trap. Maven verification inside the final `backend-build` stage reruns every non-database test, including the unchanged `ContainerContractTest`, after `Dockerfile` has been copied. It uses `-DexcludedGroups=database` only because nested Testcontainers cannot start there and never uses broad `-DskipTests` or `maven.test.skip`; host/CI `./mvnw -B verify` remains the full database-enabled gate.

- [ ] **Step 4: REFACTOR, verify one-runtime topology, and commit**

```bash
./mvnw -B verify
docker build --tag andrew-website:final-local .
bash scripts/smoke-final-container.sh andrew-website:final-local
docker run --rm --entrypoint sh andrew-website:final-local -c 'test ! -e /usr/local/bin/node && test ! -d /app/frontend'
```

Expected: all backend/frontend/unit/integration/container smoke checks PASS; the final image reports user `10001:10001`; Node/frontend source are absent; no secret is in `docker history` or image environment.

```bash
git add pom.xml Dockerfile .dockerignore scripts src
git commit -m "feat(static-jar-integration): ship the single Java artifact"
```
