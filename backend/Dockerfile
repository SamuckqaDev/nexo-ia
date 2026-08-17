FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --create-home nexo
WORKDIR /app
COPY --from=build --chown=nexo:nexo /workspace/target/nexo-backend-*.jar /app/nexo.jar
USER nexo
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/nexo.jar"]
