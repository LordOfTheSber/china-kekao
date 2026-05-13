# syntax=docker/dockerfile:1.7

# ---- Build stage --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mkdir -p /workspace/extracted \
    && (cd /workspace/extracted && jar -xf /workspace/target/china-kekao-backend.jar)

# ---- Minimal JRE via jlink ---------------------------------------------
FROM eclipse-temurin:21-jdk AS jre-build
WORKDIR /jlink
COPY --from=build /workspace/target/china-kekao-backend.jar /tmp/app.jar
# Detect required modules from the fat jar plus a conservative baseline that
# covers reflection-only deps (JDBC drivers, Jackson, etc.) that jdeps misses.
RUN set -eux; \
    JDEPS_MODS=$(jdeps --multi-release 21 --ignore-missing-deps --print-module-deps /tmp/app.jar || true); \
    MODS="java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.security.jgss,java.sql,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported,jdk.management,jdk.naming.dns,jdk.jdi,jdk.zipfs"; \
    if [ -n "$JDEPS_MODS" ]; then MODS="$MODS,$JDEPS_MODS"; fi; \
    jlink --no-header-files --no-man-pages --strip-debug --compress=2 \
          --add-modules "$MODS" --output /opt/jre

# ---- Runtime stage ------------------------------------------------------
FROM debian:12-slim AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl tini \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/jre \
    PATH=/opt/jre/bin:$PATH
COPY --from=jre-build /opt/jre /opt/jre

RUN useradd --system --uid 1001 --create-home kekao
WORKDIR /app
COPY --from=build --chown=kekao:kekao /workspace/target/china-kekao-backend.jar /app/app.jar
USER kekao

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=8 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/usr/bin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
