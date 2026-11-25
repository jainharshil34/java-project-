# Stage 1: Build application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Setup non-root user and persistent directories
RUN addgroup -S civicheat && adduser -S civicheat -G civicheat && \
    mkdir -p /app/uploads && chown -R civicheat:civicheat /app

USER civicheat

COPY --from=builder --chown=civicheat:civicheat /build/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
