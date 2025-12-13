# Multi-stage build for StudyConnect Spring Boot application

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer if pom.xml doesn't change)
RUN ./mvnw dependency:resolve

# Copy source code
COPY src ./src
COPY checkstyle.xml .
COPY checkstyle-suppressions.xml .

# Build application (skip tests for Docker build - tests run in CI)
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S studyconnect && \
    adduser -u 1001 -S studyconnect -G studyconnect

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/studyconnect-*.jar app.jar

# Change ownership to non-root user
RUN chown -R studyconnect:studyconnect /app

# Switch to non-root user
USER studyconnect

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
ENV JVM_OPTS="-Xmx512m -Xms256m"

# Database configuration (override in production)
ENV POSTGRES_HOST=localhost
ENV POSTGRES_PORT=5432
ENV POSTGRES_DB=studyconnectdb
ENV POSTGRES_USER=studyconnect
ENV POSTGRES_PASSWORD=secret
ENV DATABASE_URL=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
ENV JPA_HIBERNATE_DDL_AUTO=validate

# Keycloak configuration (override with actual values)
ENV KEYCLOAK_AUTH_SERVER_URL=https://keycloak.example.com
ENV KEYCLOAK_REALM=studyconnect
ENV KEYCLOAK_ISSUER_URI=${KEYCLOAK_AUTH_SERVER_URL}/realms/${KEYCLOAK_REALM}
ENV STUDYCONNECT_REQUIRED_ROLE=studyconnect

# Security configuration
ENV ALLOWED_ORIGIN=http://localhost:4200

# Expose port
EXPOSE ${PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT}/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java ${JVM_OPTS} -Dserver.port=${PORT} -jar app.jar"]
