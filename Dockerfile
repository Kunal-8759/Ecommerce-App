# ─── STAGE 1: BUILD ────────────────────────────────────────────────
# We use a two-stage build:
# Stage 1 → compile the code and create the JAR using Maven
# Stage 2 → copy only the JAR into a clean lightweight image
# This keeps the final image small — no Maven, no source code inside

# Use an official Maven image with JDK 17 for the build stage
FROM eclipse-temurin:17-jdk-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy Maven wrapper — this IS Maven, bundled with your project
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .
# Make wrapper executable inside Linux container
RUN chmod +x mvnw
# Download dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src
# Build JAR — no tests
RUN ./mvnw package -DskipTests 


# ─── STAGE 2: RUNTIME ───────────────────────────────────────────────
# Use a lightweight JDK image for the runtime stage

FROM eclipse-temurin:17-jre-alpine
# Set the working directory inside the runtime container
WORKDIR /app
#Create a directory for logs inside the container
RUN mkdir -p /app/logs
# Copy Only the compiled JAR file from the build stage to the runtime stage
COPY --from=build /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
# Copy the HTML email template — needed at runtime for email sending
COPY --from=build /app/src/main/resources/templates ./templates
# Expose port 8080 — this is the port our Spring Boot app listens on
EXPOSE 8080

# Set the entry point to run the JAR file when the container starts
# ENTRYPOINT ["java", "-jar", "app.jar"]
# -Djava.security.egd=file:/dev/./urandom speeds up startup on Linux
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
