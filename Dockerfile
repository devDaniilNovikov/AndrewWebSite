# --- Этап 1: Сборка фронтенда ---
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
RUN ./mvnw -B clean package -Dmaven.test.skip=true

# --- Этап 3: Финальный продакшен-контейнер с Nginx ---
FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db

# Устанавливаем Nginx в Java-контейнер
RUN apt-get update && apt-get install -y nginx && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=backend-build /workspace/target/*.jar /app/application.jar

# Копируем результат сборки фронтенда прямо в корневую папку Nginx
COPY --from=frontend-build /app/out /var/www/html

# Настраиваем простейший конфиг Nginx: все запросы идут на фронтенд, а /api — в Java
RUN echo 'server { \
    listen 8080; \
    root /var/www/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://127.0.0.1:8080/; \
    } \
}' > /etc/nginx/sites-available/default

EXPOSE 8080

# Запускаем и Java (в фоновом режиме), и Nginx (на порту 8080)
CMD ["sh", "-c", "java --enable-native-access=ALL-UNNAMED -Dspring.profiles.active=prod -jar /app/application.jar & nginx -g 'daemon off;'"]
