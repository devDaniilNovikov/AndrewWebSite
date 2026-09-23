# --- Stage 1: frontend (static export in production mode: form enabled, no preview banner) ---
FROM node:24.14.0-alpine AS frontend-build
WORKDIR /app
RUN npm install -g pnpm@11.18.0
COPY frontend/ ./
RUN pnpm install --frozen-lockfile
RUN pnpm run build:production

# --- Stage 2: backend ---
FROM eclipse-temurin:25.0.3_9-jdk-noble AS backend-build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY Dockerfile .dockerignore ./
COPY src src
RUN ./mvnw -B clean package -Dmaven.test.skip=true

# --- Stage 3: runtime: nginx on 8080 in front of the application on 127.0.0.1:8090 ---
FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db

RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=backend-build /workspace/target/*.jar /app/application.jar
COPY --from=frontend-build /app/out /var/www/html
COPY deploy/nginx.conf /etc/nginx/nginx.conf
COPY deploy/entrypoint.sh /app/entrypoint.sh

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["/bin/bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8081 && printf 'GET /actuator/health/liveness HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]

ENTRYPOINT ["/app/entrypoint.sh"]
