# --- Этап 1: Сборка фронтенда ---
FROM node:22-alpine AS frontend-build
WORKDIR /app
# Внутри Docker у нас есть права, pnpm встанет без проблем
RUN npm install -g pnpm
# Копируем всё содержимое папки frontend
COPY frontend/ ./
# Ставим пакеты и собираем (с игнором конфликтов)
RUN pnpm install --ignore-scripts
RUN pnpm run build

# --- Этап 2: Сборка бэкенда ---
FROM eclipse-temurin:25.0.3_9-jdk-noble AS backend-build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY Dockerfile .dockerignore ./
COPY src src
# МАГИЯ: Копируем готовый фронтенд из папки out прямо в Spring Boot
# (Судя по вашему package.json, файлы собираются в папку out)
COPY --from=frontend-build /app/out ./src/main/resources/static
# Собираем бэкенд без тестов
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
