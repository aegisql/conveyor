#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_DIR="$(cd "${SERVICE_DIR}/.." && pwd)"

IMAGE_REPO="${IMAGE_REPO:-ghcr.io/aegisql/conveyor-service}"
IMAGE_TAG="${IMAGE_TAG:-$(mvn -q -pl conveyor-service -DforceStdout help:evaluate -Dexpression=project.version | tail -n 1)}"
IMAGE_TAG_LATEST="${IMAGE_TAG_LATEST:-true}"
PUSH="${PUSH:-false}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-${SERVICE_DIR}/Dockerfile}"
DOCKER_BUILD_CONTEXT="${DOCKER_BUILD_CONTEXT:-${REPO_DIR}}"
DOCKER_BUILD_ARGS="${DOCKER_BUILD_ARGS:-}"
SEED_UPLOAD_DIR="${SEED_UPLOAD_DIR:-${SERVICE_DIR}/upload}"
SEED_DIR_NAME="${SEED_DIR_NAME:-.container-upload-seed.codex}"
SEED_STAGING_DIR="${DOCKER_BUILD_CONTEXT%/}/${SEED_DIR_NAME}"
SEED_SOURCE_DIR="${SEED_UPLOAD_DIR}"

cleanup() {
  if [[ -d "${SEED_STAGING_DIR}" ]]; then
    rm -rf "${SEED_STAGING_DIR}"
  fi
}
trap cleanup EXIT

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command is required" >&2
  exit 1
fi

GIT_SHA="$(git -C "${REPO_DIR}" rev-parse --short HEAD)"
SHA_TAG="${IMAGE_TAG}-${GIT_SHA}"

mkdir -p "${SEED_STAGING_DIR}"
if [[ -L "${SEED_UPLOAD_DIR}" ]]; then
  SEED_SOURCE_DIR="$(cd "${SEED_UPLOAD_DIR}" && pwd -P)"
fi

if [[ -d "${SEED_SOURCE_DIR}" ]]; then
  shopt -s nullglob
  seed_jars=("${SEED_SOURCE_DIR}"/*.jar)
  if (( ${#seed_jars[@]} > 0 )); then
    cp "${seed_jars[@]}" "${SEED_STAGING_DIR}/"
  fi
  shopt -u nullglob
fi

SEED_COUNT="$(find "${SEED_STAGING_DIR}" -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')"
echo "Seed source: ${SEED_SOURCE_DIR} (${SEED_COUNT} jar files)"
if [[ "${SEED_COUNT}" == "0" ]]; then
  echo "WARNING: No seed jars found. The image will start with an empty /opt/conveyor/upload-seed."
fi

echo "Building ${IMAGE_REPO}:${IMAGE_TAG}"
docker_build_cmd=(
  --file "${DOCKERFILE_PATH}"
  --build-arg "SEED_DIR_NAME=${SEED_DIR_NAME}"
  --tag "${IMAGE_REPO}:${IMAGE_TAG}"
  --tag "${IMAGE_REPO}:${SHA_TAG}"
)

if [[ -n "${DOCKER_BUILD_ARGS}" ]]; then
  # Intentionally split DOCKER_BUILD_ARGS into individual flags.
  # shellcheck disable=SC2206
  extra_build_args=(${DOCKER_BUILD_ARGS})
  docker_build_cmd+=("${extra_build_args[@]}")
fi

docker build "${docker_build_cmd[@]}" "${DOCKER_BUILD_CONTEXT}"

if [[ "${IMAGE_TAG_LATEST}" == "true" ]]; then
  docker tag "${IMAGE_REPO}:${IMAGE_TAG}" "${IMAGE_REPO}:latest"
fi

if [[ "${PUSH}" == "true" ]]; then
  docker push "${IMAGE_REPO}:${IMAGE_TAG}"
  docker push "${IMAGE_REPO}:${SHA_TAG}"
  if [[ "${IMAGE_TAG_LATEST}" == "true" ]]; then
    docker push "${IMAGE_REPO}:latest"
  fi
fi

echo "Image build complete."
echo " - ${IMAGE_REPO}:${IMAGE_TAG}"
echo " - ${IMAGE_REPO}:${SHA_TAG}"
if [[ "${IMAGE_TAG_LATEST}" == "true" ]]; then
  echo " - ${IMAGE_REPO}:latest"
fi
