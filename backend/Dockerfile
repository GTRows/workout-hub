# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
# Refresh apk index and upgrade OS packages to pick up CVE patches
# (e.g. gnutls 3.8.13-r0 fix for CVE-2026-33845) that arrive ahead of a
# new eclipse-temurin tag rebuild.
RUN apk -U upgrade --no-cache
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/workouthub-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
