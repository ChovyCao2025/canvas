#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# Dry-run validates static policy gates without touching production dependencies.
DRY_RUN=false

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/release/pre-deploy-check.sh [--dry-run]

Validates the release inputs that must be true before a staging or production
deployment: immutable image tag, migration backup evidence, production-like
profile safety, Flyway migration policy, and dependency reachability.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

check_tcp() {
  local name="$1"
  local host="$2"
  local port="$3"
  [[ -n "$host" ]] || fail "$name host is required"
  [[ -n "$port" ]] || fail "$name port is required"
  (echo > "/dev/tcp/$host/$port") >/dev/null 2>&1 || fail "$name is not reachable at $host:$port"
}

if [[ "$DRY_RUN" == "true" ]]; then
  # Static release gates can run in CI before secrets and network access are available.
  bash "$ROOT_DIR/scripts/release/validate-production-profile.sh"
  bash "$ROOT_DIR/scripts/release/check-flyway-migration.sh"
  echo "pre-deploy dry-run passed"
  echo "required actual env: CANVAS_IMAGE_TAG, CANVAS_MIGRATION_BACKUP_EVIDENCE"
  echo "required dependency env: CANVAS_DB_HOST, CANVAS_DB_PORT, SPRING_DATA_REDIS_HOST, SPRING_DATA_REDIS_PORT, ROCKETMQ_NAME_SERVER"
  exit 0
fi

# Require immutable image and migration backup evidence before any dependency probe runs.
[[ -n "${CANVAS_IMAGE_TAG:-}" ]] || fail "CANVAS_IMAGE_TAG is required"
[[ "$CANVAS_IMAGE_TAG" != "latest" ]] || fail "CANVAS_IMAGE_TAG must be immutable and must not be latest"
[[ -n "${CANVAS_MIGRATION_BACKUP_EVIDENCE:-}" ]] || fail "CANVAS_MIGRATION_BACKUP_EVIDENCE is required"
[[ -e "$CANVAS_MIGRATION_BACKUP_EVIDENCE" ]] || fail "migration backup evidence path does not exist: $CANVAS_MIGRATION_BACKUP_EVIDENCE"

# Production profile and Flyway checks enforce secret strength and migration immutability.
CANVAS_RELEASE_REQUIRE_ENV=true bash "$ROOT_DIR/scripts/release/validate-production-profile.sh"
bash "$ROOT_DIR/scripts/release/check-flyway-migration.sh"

# Probe stateful dependencies last so policy failures surface before network noise.
check_tcp "database" "${CANVAS_DB_HOST:-}" "${CANVAS_DB_PORT:-3306}"
check_tcp "redis" "${SPRING_DATA_REDIS_HOST:-}" "${SPRING_DATA_REDIS_PORT:-6379}"

# ROCKETMQ_NAME_SERVER may be host:port or host-only; host-only defaults to the RocketMQ namesrv port.
rocket_host="${ROCKETMQ_NAME_SERVER%%:*}"
rocket_port="${ROCKETMQ_NAME_SERVER##*:}"
if [[ "$rocket_host" == "$rocket_port" ]]; then
  rocket_port="9876"
fi
check_tcp "rocketmq" "$rocket_host" "$rocket_port"

echo "pre-deploy checks passed"
