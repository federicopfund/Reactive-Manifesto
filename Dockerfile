# Multi-stage Dockerfile for Play Framework application
# Stage 1: Builder - Build the application using sbt
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy sbt configuration files first for better caching
COPY project/build.properties project/
COPY project/plugins.sbt project/

# Install sbt from Debian packages
RUN apt-get update && \
    apt-get install -y apt-transport-https curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
    apt-get update && \
    apt-get install -y sbt && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy build definition
COPY build.sbt .

# Copy application source
COPY app app
COPY conf conf
COPY public public

# Build the application using sbt stage
RUN sbt stage

# Stage 2: Runtime - Minimal image for running the application
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the staged application from builder
COPY --from=builder /app/target/universal/stage /app

# Set environment variables for production
ENV APPLICATION_MODE="prod"

# Set conservative JVM options for limited memory (Render free plan)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dplay.server.pidfile.path=/dev/null"

# Expose port - will be overridden by PORT env var at runtime
EXPOSE 9000

# Run the application in production mode
# The PORT environment variable will be used by Play Framework
# APPLICATION_SECRET must be provided via environment variable
CMD ["sh", "-c", "/app/bin/web -Dhttp.port=${PORT:-9000} -Dplay.http.secret.key=${APPLICATION_SECRET}"]
