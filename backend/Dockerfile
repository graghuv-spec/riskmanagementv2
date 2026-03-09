# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy gradle wrapper first — better layer caching (only re-downloads when
# build files change, not when source changes)
COPY gradlew gradlew.bat ./
COPY gradle/                gradle/
COPY build.gradle           ./

# Fix Windows CRLF line endings so the shebang works on Linux
RUN sed -i 's/\r//' gradlew && chmod +x gradlew

# Warm up the dependency cache
RUN ./gradlew dependencies --no-daemon --quiet || true

# Copy source and build the fat JAR (skip tests — tests run in CI)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

# Reduce JVM startup time for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
