# Multi-stage build for Campus Club Management System Web Dashboard
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package fat JAR
COPY src ./src
COPY database ./database
RUN mvn package -DskipTests

# Runtime stage: Lightweight Alpine JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy shaded fat jar
COPY --from=builder /app/target/campus-club-management-system-1.0.0.jar app.jar
COPY database/schema.sql database/schema.sql

# Environment Configuration
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
EXPOSE 8080

# Run in headless web server mode
CMD ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
