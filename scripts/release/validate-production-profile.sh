#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROD_PROFILE="$ROOT_DIR/backend/canvas-boot/src/main/resources/application-prod.yml"
STAGING_PROFILE="$ROOT_DIR/backend/canvas-boot/src/main/resources/application-staging.yml"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "missing file: $1"
}

reject_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"
  if grep -Eq "$pattern" "$file"; then
    fail "$message in $file"
  fi
}

require_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"
  grep -Eq "$pattern" "$file" || fail "$message in $file"
}

validate_profile_file() {
  local file="$1"
  require_file "$file"

  reject_pattern "$file" 'username:[[:space:]]*root([[:space:]]*#.*)?$' "root datasource username is forbidden"
  reject_pattern "$file" 'password:[[:space:]]*root([[:space:]]*#.*)?$' "root datasource password is forbidden"
  reject_pattern "$file" 'canvas-event-report-secret-2026!!' "default event report secret is forbidden"
  reject_pattern "$file" 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=' "default cipher key is forbidden"
  reject_pattern "$file" 'allowed-origins:[[:space:]]*"?\*"?([[:space:]]*#.*)?$' "wildcard CORS is forbidden"
  reject_pattern "$file" 'show-details:[[:space:]]*always([[:space:]]*#.*)?$' "health details must not be always"
  reject_pattern "$file" 'enabled:[[:space:]]*true([[:space:]]*#.*)?$' "springdoc must not be enabled in production-like profiles"

  require_pattern "$file" '\$\{SPRING_DATASOURCE_URL' "datasource URL must come from env"
  require_pattern "$file" '\$\{SPRING_DATASOURCE_USERNAME' "datasource username must come from env"
  require_pattern "$file" '\$\{SPRING_DATASOURCE_PASSWORD' "datasource password must come from env"
  require_pattern "$file" '\$\{SPRING_DATA_REDIS_PASSWORD' "redis password must come from env"
  require_pattern "$file" '\$\{CANVAS_CORS_ALLOWED_ORIGINS' "CORS origins must come from env"
  require_pattern "$file" '\$\{CANVAS_EVENT_REPORT_SECRET' "event report secret must come from env"
  require_pattern "$file" '\$\{CANVAS_JWT_SECRET' "JWT secret must come from env"
  require_pattern "$file" '\$\{CANVAS_SECRET_CIPHER_KEY' "cipher key must come from env"
  require_pattern "$file" 'include:[[:space:]]*health,info,prometheus,metrics' "actuator health/info/prometheus/metrics exposure is required"
}

validate_required_env() {
  local required=(
    SPRING_DATASOURCE_URL
    SPRING_DATASOURCE_USERNAME
    SPRING_DATASOURCE_PASSWORD
    SPRING_DATA_REDIS_HOST
    SPRING_DATA_REDIS_PASSWORD
    CANVAS_CORS_ALLOWED_ORIGINS
    CANVAS_EVENT_REPORT_SECRET
    CANVAS_INTERNAL_API_TOKEN
    CANVAS_JWT_SECRET
    CANVAS_SECRET_CIPHER_KEY
    ROCKETMQ_NAME_SERVER
  )
  for name in "${required[@]}"; do
    [[ -n "${!name:-}" ]] || fail "required env var is missing: $name"
  done

  [[ "${SPRING_DATASOURCE_USERNAME}" != "root" ]] || fail "SPRING_DATASOURCE_USERNAME cannot be root"
  [[ "${SPRING_DATASOURCE_PASSWORD}" != "root" ]] || fail "SPRING_DATASOURCE_PASSWORD cannot be root"
  [[ "${CANVAS_CORS_ALLOWED_ORIGINS}" != "*" ]] || fail "CANVAS_CORS_ALLOWED_ORIGINS cannot be *"
  [[ "${#CANVAS_JWT_SECRET}" -ge 32 ]] || fail "CANVAS_JWT_SECRET must be at least 32 characters"
  [[ "${#CANVAS_EVENT_REPORT_SECRET}" -ge 32 ]] || fail "CANVAS_EVENT_REPORT_SECRET must be at least 32 characters"
}

validate_profile_file "$PROD_PROFILE"
validate_profile_file "$STAGING_PROFILE"

if [[ "${CANVAS_RELEASE_REQUIRE_ENV:-false}" == "true" ]]; then
  validate_required_env
fi

echo "production profile validation passed"
