#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CONVEYOR="${CONVEYOR:-collector}"
SINGLE_ID="${ID:-}"
TTL="${TTL:-1 SECONDS}"
REQUEST_TTL="${REQUEST_TTL:-100}"
REST_USER="${REST_USER:-rest}"
REST_PASSWORD="${REST_PASSWORD:-rest}"
AUTH_MODE="${AUTH_MODE:-session}" # session | basic
INPUT_FILE=""
SHUFFLE=false

conveyor_set=false
id_set=false

COOKIE_JAR="$(mktemp -t conveyor-service-cookies.XXXXXX)"
trap 'rm -f "$COOKIE_JAR"' EXIT

usage() {
  cat <<'USAGE'
Usage:
  test-part-loader.sh [conveyor] [id]
  test-part-loader.sh [options]

Options:
  --conveyor <name>   Conveyor name (default: collector)
  --id <value>        Single ID for fixed 3-step flow (USER/ADDRESS/DONE)
  --file <path>       Play pipe-delimited file with rows: CONVEYOR_NAME|ID|LABEL|BODY
  --shuffle           Randomize order of file rows before sending
  -h, --help          Show help

Notes:
  - If --file is set, single fixed flow is skipped and file rows are used.
  - Backward-compatible positional mode is still supported: [conveyor] [id].
USAGE
}

POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --conveyor)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --conveyor" >&2
        exit 1
      fi
      CONVEYOR="$2"
      conveyor_set=true
      shift 2
      ;;
    --id)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --id" >&2
        exit 1
      fi
      SINGLE_ID="$2"
      id_set=true
      shift 2
      ;;
    --file)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --file" >&2
        exit 1
      fi
      INPUT_FILE="$2"
      shift 2
      ;;
    --shuffle)
      SHUFFLE=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      while [[ $# -gt 0 ]]; do
        POSITIONAL+=("$1")
        shift
      done
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      POSITIONAL+=("$1")
      shift
      ;;
  esac
done

if [[ ${#POSITIONAL[@]} -gt 0 && "$conveyor_set" == false ]]; then
  CONVEYOR="${POSITIONAL[0]}"
fi
if [[ ${#POSITIONAL[@]} -gt 1 && "$id_set" == false ]]; then
  SINGLE_ID="${POSITIONAL[1]}"
fi
if [[ ${#POSITIONAL[@]} -gt 2 ]]; then
  echo "Too many positional arguments. Expected at most: [conveyor] [id]" >&2
  usage
  exit 1
fi

if [[ "$AUTH_MODE" != "session" && "$AUTH_MODE" != "basic" ]]; then
  echo "AUTH_MODE must be 'session' or 'basic'. Received: $AUTH_MODE" >&2
  exit 1
fi

if [[ -n "$INPUT_FILE" ]]; then
  if [[ ! -f "$INPUT_FILE" ]]; then
    echo "Input file not found: $INPUT_FILE" >&2
    exit 1
  fi
else
  if [[ -z "$SINGLE_ID" ]]; then
    SINGLE_ID="$(date +%s)"
  fi
fi

urlencode() {
  local value="${1:-}"
  local out=""
  local i char hex
  for ((i = 0; i < ${#value}; i++)); do
    char="${value:i:1}"
    case "$char" in
      [a-zA-Z0-9.~_-]) out+="$char" ;;
      ' ') out+='%20' ;;
      *)
        printf -v hex '%02X' "'$char"
        out+="%${hex}"
        ;;
    esac
  done
  printf '%s' "$out"
}

print_json() {
  local payload="$1"
  if command -v jq >/dev/null 2>&1; then
    printf '%s\n' "$payload" | jq .
  else
    printf '%s\n' "$payload"
  fi
}

login_session() {
  local login_url login_status
  login_url="${BASE_URL}/login"
  echo "Logging in at ${login_url} as '${REST_USER}' (session auth)..."
  login_status="$(
    curl -sS \
      -c "$COOKIE_JAR" \
      -X POST "$login_url" \
      --data-urlencode "username=${REST_USER}" \
      --data-urlencode "password=${REST_PASSWORD}" \
      --data-urlencode "remember-me=true" \
      -o /dev/null \
      -w "%{http_code}"
  )"

  if [[ "$login_status" != "302" && "$login_status" != "200" ]]; then
    echo "Login failed. HTTP ${login_status}" >&2
    exit 1
  fi
}

perform_post() {
  local url="$1"
  local body="$2"
  if [[ "$AUTH_MODE" == "basic" ]]; then
    curl -sS \
      -u "${REST_USER}:${REST_PASSWORD}" \
      -H "Content-Type: application/json" \
      -X POST "$url" \
      --data "$body" \
      -w $'\nHTTP_STATUS:%{http_code}'
  else
    curl -sS \
      -b "$COOKIE_JAR" \
      -c "$COOKIE_JAR" \
      -H "Content-Type: application/json" \
      -X POST "$url" \
      --data "$body" \
      -w $'\nHTTP_STATUS:%{http_code}'
  fi
}

post_part() {
  local conveyor_name="$1"
  local id="$2"
  local label="$3"
  local body="$4"
  local conveyor_path id_path label_path
  local query url response status response_body

  conveyor_path="$(urlencode "$conveyor_name")"
  id_path="$(urlencode "$id")"
  label_path="$(urlencode "$label")"
  query="ttl=$(urlencode "$TTL")&requestTTL=$(urlencode "$REQUEST_TTL")"
  url="${BASE_URL}/part/${conveyor_path}/${id_path}/${label_path}?${query}"

  echo "POST ${url}"
  echo "Body: ${body}"

  response="$(perform_post "$url" "$body")"

  status="$(printf '%s\n' "$response" | sed -n 's/^HTTP_STATUS://p')"
  response_body="$(printf '%s\n' "$response" | sed '/^HTTP_STATUS:/d')"

  echo "HTTP ${status}"
  print_json "$response_body"
  echo

  if [[ "$status" == "302" ]]; then
    echo "Received HTTP 302 (likely authentication redirect). Check AUTH_MODE and credentials." >&2
    exit 1
  fi

  if [[ -z "$status" || "$status" -ge 400 ]]; then
    echo "Request failed for ID=${id}, label=${label}" >&2
    exit 1
  fi
}

declare -a FILE_RECORDS=()

load_file_records() {
  local line raw_conveyor raw_id raw_label raw_body line_no=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line_no=$((line_no + 1))
    line="${line%$'\r'}"
    [[ -z "${line//[[:space:]]/}" ]] && continue
    [[ "$line" == "CONVEYOR_NAME|ID|LABEL|BODY" ]] && continue

    IFS='|' read -r raw_conveyor raw_id raw_label raw_body <<< "$line"
    if [[ -z "${raw_conveyor:-}" || -z "${raw_id:-}" || -z "${raw_label:-}" || -z "${raw_body:-}" ]]; then
      echo "Invalid record at ${INPUT_FILE}:${line_no}. Expected format: CONVEYOR_NAME|ID|LABEL|BODY" >&2
      exit 1
    fi
    FILE_RECORDS+=("${raw_conveyor}|${raw_id}|${raw_label}|${raw_body}")
  done < "$INPUT_FILE"

  if [[ ${#FILE_RECORDS[@]} -eq 0 ]]; then
    echo "No records found in input file: $INPUT_FILE" >&2
    exit 1
  fi
}

shuffle_file_records() {
  local i j tmp
  for ((i = ${#FILE_RECORDS[@]} - 1; i > 0; i--)); do
    j=$((RANDOM % (i + 1)))
    tmp="${FILE_RECORDS[$i]}"
    FILE_RECORDS[$i]="${FILE_RECORDS[$j]}"
    FILE_RECORDS[$j]="$tmp"
  done
}

play_file_records() {
  local record conveyor_name id label body
  for record in "${FILE_RECORDS[@]}"; do
    IFS='|' read -r conveyor_name id label body <<< "$record"
    post_part "$conveyor_name" "$id" "$label" "$body"
  done
}

if [[ "$AUTH_MODE" == "session" ]]; then
  login_session
fi

if [[ -n "$INPUT_FILE" ]]; then
  load_file_records
  if [[ "$SHUFFLE" == true ]]; then
    shuffle_file_records
  fi

  play_file_records
  echo "Sent ${#FILE_RECORDS[@]} part-loader records from file '${INPUT_FILE}' (conveyor per row)."
  echo "shuffle=${SHUFFLE}, ttl='${TTL}', requestTTL='${REQUEST_TTL}'"
else
  post_part "$CONVEYOR" "$SINGLE_ID" "USER" '{"name":"John D"}'
  post_part "$CONVEYOR" "$SINGLE_ID" "ADDRESS" '{"zip_code":"11111"}'
  post_part "$CONVEYOR" "$SINGLE_ID" "DONE" '{}'

  echo "Sent 3 part-loader messages to conveyor '${CONVEYOR}' with ID=${SINGLE_ID}."
  echo "ttl='${TTL}', requestTTL='${REQUEST_TTL}'"
fi
