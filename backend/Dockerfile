FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# The build runs inside a container with no Docker daemon and no Ollama, so tests that need
# either are excluded here. Run them on a developer machine or in a Docker-enabled CI job.
RUN mvn -B -ntp verify -Dexcluded.test.groups=ollama,docker

FROM eclipse-temurin:25-jre
RUN groupadd --system --gid 10001 nexo \
    && useradd --system --uid 10001 --gid 10001 --create-home nexo
WORKDIR /app
COPY --from=build --chown=nexo:nexo /workspace/target/nexo-backend-*.jar /app/nexo.jar
USER nexo
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/nexo.jar"]
