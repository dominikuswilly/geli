# Multi-stage build for Shop Warehouse Management
# Target platform: linux/amd64 (x86_64)

# Stage 1: Build JAR using Maven
FROM --platform=linux/amd64 maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy Maven POM and download dependencies for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image using Eclipse Temurin JRE 17
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root system user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR artifact from builder stage
COPY --from=builder /build/target/warehouse-management-1.0.0.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8085

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
