# Build stage: tests are skipped here because they need Docker (Testcontainers); run `mvn verify` locally instead.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN adduser -S -D -H app
WORKDIR /app
COPY --from=build /workspace/target/voting-api-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
