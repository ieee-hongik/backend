
# Stage 1: Build
FROM gradle:9.8.2-jdk21 AS build

USER root
WORKDIR /app
RUN chown gradle:gradle /app

COPY --chown=gradle:gradle . .
USER gradle
RUN gradle --no-daemon bootJar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
