# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG SEED_DIR_NAME=.container-upload-seed
ARG REQUIRE_SEED_JARS=false

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl conveyor-service -am -DskipTests package && \
    JAR_FILE="$(find conveyor-service/target -maxdepth 1 -type f -name 'conveyor-service-*.jar' ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)" && \
    test -n "${JAR_FILE}" && \
    cp "${JAR_FILE}" /workspace/conveyor-service.jar

RUN mkdir -p /workspace/upload-seed && \
    for seed_src in "/workspace/conveyor-service/upload" "/workspace/${SEED_DIR_NAME}"; do \
      if [ -d "${seed_src}" ]; then \
        find -L "${seed_src}" -maxdepth 1 -type f -name '*.jar' -exec cp {} /workspace/upload-seed/ \; ; \
      fi; \
    done && \
    seed_count="$(find /workspace/upload-seed -maxdepth 1 -type f -name '*.jar' | wc -l)" && \
    echo "Seed jars discovered during image build: ${seed_count}" && \
    if [ "${REQUIRE_SEED_JARS}" = "true" ] && [ "${seed_count}" = "0" ]; then \
      echo "No seed jars found. Build with SEED_UPLOAD_DIR=<path> using conveyor-service/scripts/build-container-image.sh."; \
      exit 1; \
    fi

FROM eclipse-temurin:21-jre-jammy AS runtime

ARG APP_HOME=/opt/conveyor

RUN groupadd --system --gid 10001 conveyor && \
    useradd --system --uid 10001 --gid conveyor --home-dir "${APP_HOME}" --create-home conveyor && \
    mkdir -p "${APP_HOME}/upload" && \
    chown -R conveyor:conveyor "${APP_HOME}"

WORKDIR ${APP_HOME}

COPY --from=build /workspace/conveyor-service.jar /opt/conveyor/app.jar
COPY --from=build /workspace/upload-seed/ /opt/conveyor/upload-seed/
COPY --from=build /workspace/conveyor-service/scripts/container-entrypoint.sh /usr/local/bin/container-entrypoint.sh

RUN chmod 0755 /usr/local/bin/container-entrypoint.sh

USER conveyor:conveyor

ENV SPRING_PROFILES_ACTIVE=demo \
    SERVER_PORT=8080 \
    CONVEYOR_SERVICE_UPLOAD_DIR=/opt/conveyor/upload \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
VOLUME ["/opt/conveyor/upload"]

ENTRYPOINT ["/usr/local/bin/container-entrypoint.sh"]
