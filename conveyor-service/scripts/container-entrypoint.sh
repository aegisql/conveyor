#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/opt/conveyor}"
UPLOAD_DIR="${CONVEYOR_SERVICE_UPLOAD_DIR:-${APP_HOME}/upload}"
LOG_DIR="${CONVEYOR_LOG_DIR:-${APP_HOME}/logs}"
SEED_DIR="${APP_HOME}/upload-seed"

mkdir -p "${UPLOAD_DIR}" "${LOG_DIR}"

if [[ -n "${CONVEYOR_AUDIT_LOG_FILE:-}" ]]; then
  mkdir -p "$(dirname "${CONVEYOR_AUDIT_LOG_FILE}")"
fi
if [[ -n "${CONVEYOR_CONVEYOR_LOG_FILE:-}" ]]; then
  mkdir -p "$(dirname "${CONVEYOR_CONVEYOR_LOG_FILE}")"
fi

if [[ -d "${SEED_DIR}" ]]; then
  shopt -s nullglob
  for seed_jar in "${SEED_DIR}"/*.jar; do
    target_jar="${UPLOAD_DIR}/$(basename "${seed_jar}")"
    if [[ ! -f "${target_jar}" ]]; then
      cp "${seed_jar}" "${target_jar}"
      echo "Seeded upload JAR: $(basename "${seed_jar}")"
    fi
  done
  shopt -u nullglob
fi

exec java -jar /opt/conveyor/app.jar
