#!/usr/bin/env bash

set -euo pipefail

REDIS_CLI=${REDIS_CLI:-redis-cli}
REDIS_DB_MAX=${REDIS_DB_MAX:-15}
DRY_RUN=false

if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
fi

UUID_RE='[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'
deleted_count=0
SCAN_RESULTS=()

delete_keys() {
  local db=$1
  shift
  local keys=("$@")
  if ((${#keys[@]} == 0)); then
    return
  fi

  printf 'DB %s deleting %s keys\n' "$db" "${#keys[@]}"
  if [[ "$DRY_RUN" == false ]]; then
    "$REDIS_CLI" -n "$db" DEL "${keys[@]}" >/dev/null
  fi
  deleted_count=$((deleted_count + ${#keys[@]}))
}

scan_keys() {
  local db=$1
  local pattern=$2
  local key

  SCAN_RESULTS=()
  while IFS= read -r key; do
    [[ -n "$key" ]] && SCAN_RESULTS+=("$key")
  done < <("$REDIS_CLI" -n "$db" --scan --pattern "$pattern")
}

for db in $(seq 0 "$REDIS_DB_MAX"); do
  while IFS= read -r meta_key; do
    [[ -z "$meta_key" ]] && continue
    if [[ "$meta_key" =~ ^conv:\{(redis-persistence-.*-${UUID_RE})\}:meta$ ]]; then
      namespace=${BASH_REMATCH[1]}
      prefix="conv:{${namespace}}"
      scan_keys "$db" "${prefix}*"
      namespace_keys=("${SCAN_RESULTS[@]}")
      if ((${#namespace_keys[@]} > 0)); then
        printf 'DB %s removing randomized namespace %s\n' "$db" "$namespace"
        delete_keys "$db" "${namespace_keys[@]}"
      fi
    fi
  done < <("$REDIS_CLI" -n "$db" --scan --pattern 'conv:{redis-persistence-*}:meta')

  scan_keys "$db" 'learn-redis:*'
  learn_keys=("${SCAN_RESULTS[@]}")
  if ((${#learn_keys[@]} > 0)); then
    stale_learn_keys=()
    for key in "${learn_keys[@]}"; do
      if [[ "$key" =~ ^learn-redis:.*:${UUID_RE}$ ]]; then
        stale_learn_keys+=("$key")
      fi
    done
    if ((${#stale_learn_keys[@]} > 0)); then
      printf 'DB %s removing randomized learn keys\n' "$db"
      delete_keys "$db" "${stale_learn_keys[@]}"
    fi
  fi
done

if [[ "$DRY_RUN" == true ]]; then
  printf 'Dry run complete. %s keys would be deleted.\n' "$deleted_count"
else
  printf 'Cleanup complete. Deleted %s keys.\n' "$deleted_count"
fi
