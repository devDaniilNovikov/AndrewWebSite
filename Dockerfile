FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY Dockerfile .dockerignore ./
COPY src src
RUN ./mvnw -B -DexcludedGroups=database verify

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -g 10001 -S app \
    && adduser -u 10001 -S -D -H -G app -s /sbin/nologin app
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/target/andrew-website-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/liveness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
