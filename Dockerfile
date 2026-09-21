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
        try_files $uri.html $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://127.0.0.1:8080/; \
    } \
}' > /etc/nginx/sites-available/default

# Configure Nginx to run as a non-root user
RUN mkdir -p /var/lib/nginx/body /var/lib/nginx/fastcgi /var/lib/nginx/proxy /var/lib/nginx/scgi /var/lib/nginx/uwsgi && \
    chown -R 10001:10001 /var/lib/nginx /var/log/nginx /etc/nginx /var/www/html /app && \
    chmod -R 777 /var/lib/nginx /var/log/nginx /etc/nginx /var/www/html /app && \
    mkdir -p /run/nginx && chown -R 10001:10001 /run/nginx && chmod -R 777 /run/nginx && \
    sed -i 's/user www-data;/user 10001;/' /etc/nginx/nginx.conf || true && \
    sed -i '/user/d' /etc/nginx/nginx.conf || true && \
    sed -i 's|/run/nginx.pid|/run/nginx/nginx.pid|' /etc/nginx/nginx.conf || true && \
    touch /run/nginx/nginx.pid && chown -R 10001:10001 /run/nginx/nginx.pid

RUN groupadd -g 10001 appgroup && useradd -u 10001 -g 10001 -m -s /bin/bash appuser

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD bash -c 'cat < /dev/null > /dev/tcp/127.0.0.1/8081' && curl -f http://127.0.0.1:8081/actuator/health/liveness || exit 1

# Запускаем и Java (в фоновом режиме), и Nginx (на порту 8080)
CMD ["sh", "-c", "java --enable-native-access=ALL-UNNAMED -Dspring.profiles.active=prod -jar /app/application.jar & nginx -g 'daemon off;'"]
