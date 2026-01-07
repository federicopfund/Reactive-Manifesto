# Multi-stage build for Reactive Manifesto
FROM eclipse-temurin:17-jdk as builder

# Install sbt
RUN apt-get update && \
    apt-get install -y curl && \
    curl -L -o /tmp/sbt.tgz https://github.com/sbt/sbt/releases/download/v1.9.9/sbt-1.9.9.tgz && \
    tar -xzf /tmp/sbt.tgz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt && \
    rm /tmp/sbt.tgz

WORKDIR /app

# Copy project files
COPY build.sbt .
COPY project project/
COPY conf conf/
COPY app app/
COPY public public/

# Build the application
RUN sbt clean compile stage

# Final stage - runtime image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built application from builder stage
COPY --from=builder /app/target/universal/stage /app

# Expose the default Play Framework port
EXPOSE 9000

# Set production environment variables
ENV PLAY_ENV=prod
ENV APPLICATION_SECRET=${APPLICATION_SECRET:-changeme}

# Run the application
CMD ["./bin/web"]
