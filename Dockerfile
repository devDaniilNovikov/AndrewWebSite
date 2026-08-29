# --- Этап 1: Сборка фронтенда ---
# Жестко фиксируем версию, которую требует ваш скрипт build.mjs
FROM node:24.14.0-alpine AS frontend-build
WORKDIR /app
RUN npm install -g pnpm
COPY frontend/ ./
RUN pnpm install
RUN pnpm run build:standalone

# --- Этап 2: Сборка бэкенда ---
FROM eclipse-temurin:25.0.3_9-jdk-noble AS backend-build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY Dockerfile .dockerignore ./
COPY src src
# Снова меняем dist на out
COPY --from=frontend-build /app/out ./src/main/resources/static
RUN ./mvnw -B clean package -Dmaven.test.skip=true

# --- Этап 3: Финальный запуск ---
FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/target/*.jar /app/application.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-Dspring.profiles.active=prod", "-Dserver.port=8080", "-jar", "/app/application.jar"]
