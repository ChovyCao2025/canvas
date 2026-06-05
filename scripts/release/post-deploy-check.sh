#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/release/post-deploy-check.sh [--dry-run]

Checks the deployed canvas-engine health endpoint, Prometheus metrics endpoint,
optional smoke-test URL, and optional runtime alert source.
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

if [[ "$DRY_RUN" == "true" ]]; then
  echo "post-deploy dry-run passed"
  echo "planned checks: health, prometheus, optional smoke URL, optional alert source"
  echo "required actual env: CANVAS_BASE_URL"
  echo "optional env: CANVAS_SMOKE_URL, CANVAS_ALERTS_URL"
  exit 0
fi

command -v curl >/dev/null 2>&1 || fail "curl is required"

BASE_URL="${CANVAS_BASE_URL:-}"
[[ -n "$BASE_URL" ]] || fail "CANVAS_BASE_URL is required"
BASE_URL="${BASE_URL%/}"

health="$(curl -fsS "$BASE_URL/actuator/health")"
echo "$health" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' || fail "health endpoint is not UP"

metrics="$(curl -fsS "$BASE_URL/actuator/prometheus")"
echo "$metrics" | grep -Eq '(^jvm_|^process_|^http_|^canvas_)' || fail "Prometheus endpoint did not return runtime metrics"

if [[ -n "${CANVAS_SMOKE_URL:-}" ]]; then
  curl -fsS "$CANVAS_SMOKE_URL" >/dev/null || fail "smoke check failed: $CANVAS_SMOKE_URL"
fi

if [[ -n "${CANVAS_ALERTS_URL:-}" ]]; then
  alerts="$(curl -fsS "$CANVAS_ALERTS_URL")"
  if echo "$alerts" | grep -Eiq '"(state|status)"[[:space:]]*:[[:space:]]*"firing"'; then
    fail "runtime alert source reports firing alerts"
  fi
else
  echo "WARN: CANVAS_ALERTS_URL is not set; skipped runtime alert source check"
fi

echo "post-deploy checks passed"
