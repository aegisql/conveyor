#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/release.sh [--dry-run] <release-version>

Example:
  scripts/release.sh 1.7.4
  scripts/release.sh --dry-run 1.7.4

The script will:
  1. show git status
  2. verify the Git worktree is clean
  3. set all Maven project versions to <release-version>
  4. set the root project.version property to <release-version>
  5. commit and tag the release
  6. deploy signed artifacts with Maven
  7. bump to the next patch snapshot automatically
  8. commit the next snapshot
  9. push the current branch and the release tag
EOF
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

DRY_RUN=false

print_command() {
  printf ' %q' "$@"
  printf '\n'
}

run_step_always() {
  local description=$1
  shift
  echo "==> ${description}"
  "$@"
}

run_step() {
  local description=$1
  shift
  echo "==> ${description}"
  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "DRY RUN:$(print_command "$@")"
    return 0
  fi
  "$@"
}

require_clean_worktree() {
  local status
  status="$(git status --short)"
  if [[ -n "${status}" ]]; then
    echo "${status}" >&2
    fail "Git worktree is not clean"
  fi
}

next_snapshot_version() {
  local version=$1
  local major minor patch
  IFS='.' read -r major minor patch <<<"${version}"

  [[ -n "${major}" && -n "${minor}" && -n "${patch}" ]] || fail "Release version must have format X.Y.Z"
  [[ "${major}" =~ ^[0-9]+$ ]] || fail "Invalid major version: ${major}"
  [[ "${minor}" =~ ^[0-9]+$ ]] || fail "Invalid minor version: ${minor}"
  [[ "${patch}" =~ ^[0-9]+$ ]] || fail "Invalid patch version: ${patch}"

  patch=$((patch + 1))
  printf '%s.%s.%s-SNAPSHOT\n' "${major}" "${minor}" "${patch}"
}

stage_all_poms() {
  mapfile -t pom_files < <(git ls-files '*pom.xml')
  [[ ${#pom_files[@]} -gt 0 ]] || fail "No pom.xml files found to stage"
  git add -- "${pom_files[@]}"
}

main() {
  local release_version=""

  if [[ $# -eq 2 && "$1" == "--dry-run" ]]; then
    DRY_RUN=true
    release_version=$2
  elif [[ $# -eq 1 ]]; then
    release_version=$1
  else
    usage
    exit 1
  fi

  [[ "${release_version}" != *-SNAPSHOT ]] || fail "Release version must not include -SNAPSHOT"
  [[ "${release_version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "Release version must have format X.Y.Z"

  local next_snapshot
  next_snapshot="$(next_snapshot_version "${release_version}")"

  local current_branch
  current_branch="$(git rev-parse --abbrev-ref HEAD)"
  [[ "${current_branch}" != "HEAD" ]] || fail "Detached HEAD is not supported"

  local release_tag="v${release_version}"

  run_step_always "Show git status" git status --short --branch
  require_clean_worktree

  run_step "Set project version to ${release_version}" \
    mvn -q versions:set -DnewVersion="${release_version}" -DprocessAllModules=true -DgenerateBackupPoms=false

  run_step "Set project.version property to ${release_version}" \
    mvn -q versions:set-property -Dproperty=project.version -DnewVersion="${release_version}" -DgenerateBackupPoms=false

  run_step "Stage pom.xml files for release commit" stage_all_poms
  run_step "Create release commit" git commit -m "Release ${release_version}"
  run_step "Create release tag ${release_tag}" git tag -a "${release_tag}" -m "Release ${release_version}"

  run_step "Deploy signed release artifacts" \
    mvn -DskipTests -Psign-artifacts clean deploy

  run_step "Set project version to ${next_snapshot}" \
    mvn -q versions:set -DnewVersion="${next_snapshot}" -DprocessAllModules=true -DgenerateBackupPoms=false

  run_step "Set project.version property to ${next_snapshot}" \
    mvn -q versions:set-property -Dproperty=project.version -DnewVersion="${next_snapshot}" -DgenerateBackupPoms=false

  run_step "Stage pom.xml files for next snapshot commit" stage_all_poms
  run_step "Create next snapshot commit" git commit -m "Prepare ${next_snapshot}"

  run_step "Push branch ${current_branch}" git push origin "${current_branch}"
  run_step "Push tag ${release_tag}" git push origin "${release_tag}"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "Dry run for release ${release_version} completed. Next development version would be ${next_snapshot}."
    return 0
  fi

  echo "Release ${release_version} completed. Next development version is ${next_snapshot}."
}

main "$@"
