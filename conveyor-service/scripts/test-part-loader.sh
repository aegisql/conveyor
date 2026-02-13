#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CONVEYOR="${1:-collector}"
ID="${2:-$(date +%s)}"
TTL="${TTL:-1 SECONDS}"
REQUEST_TTL="${REQUEST_TTL:-100}"
REST_USER="${REST_USER:-rest}"
REST_PASSWORD="${REST_PASSWORD:-rest}"
AUTH_MODE="${AUTH_MODE:-session}" # session | basic
COOKIE_JAR="$(mktemp -t conveyor-service-cookies.XXXXXX)"

trap 'rm -f "$COOKIE_JAR"' EXIT

if ! [[ "$ID" =~ ^[0-9]+$ ]]; then
  echo "ID must be a number. Received: $ID" >&2
  exit 1
fi

if [[ "$AUTH_MODE" != "session" && "$AUTH_MODE" != "basic" ]]; then
  echo "AUTH_MODE must be 'session' or 'basic'. Received: $AUTH_MODE" >&2
  exit 1
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
  local label="$1"
  local body="$2"
  local query url response status response_body

  query="ttl=$(urlencode "$TTL")&requestTTL=$(urlencode "$REQUEST_TTL")"
  url="${BASE_URL}/part/${CONVEYOR}/${ID}/${label}?${query}"

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
    echo "Request failed for label ${label}" >&2
    exit 1
  fi
}

if [[ "$AUTH_MODE" == "session" ]]; then
  login_session
fi

post_part "USER" '{"name":"John D"}'
post_part "ADDRESS" '{"zip_code":"11111"}'
post_part "DONE" '{}'

echo "Sent 3 part-loader messages to conveyor '${CONVEYOR}' with ID=${ID}."
echo "ttl='${TTL}', requestTTL='${REQUEST_TTL}'"
