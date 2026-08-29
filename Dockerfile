FROM eclipse-temurin:25.0.3_9-jdk-noble@sha256:735baf2edc6cd6485240144a84fa4db142b9a6f47b4eb4080f31058d200f9813 AS backend-build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY Dockerfile .dockerignore ./
COPY src src
RUN ./mvnw -B clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --no-create-home \
        --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/target/andrew-website-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD /bin/bash -ec 'exec 3<>/dev/tcp/127.0.0.1/8080; printf "GET /actuator/health/liveness HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nConnection: close\r\n\r\n" >&3; read -r _ status _ <&3; test "$status" = 200'
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-Dspring.profiles.active=prod", "-Dserver.port=8080", "-jar", "/app/application.jar"]
