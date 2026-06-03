#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
MAX_SESSIONS_PER_USER="${MAX_SESSIONS_PER_USER:-${CANVAS_NOTIFICATION_WS_MAX_SESSIONS_PER_USER:-5}}"
HOLD_SECONDS="${HOLD_SECONDS:-10}"
SETTLE_SECONDS="${SETTLE_SECONDS:-2}"

if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "ERROR: AUTH_TOKEN is required to create notification WebSocket tickets." >&2
  exit 2
fi

if ! command -v websocat >/dev/null 2>&1; then
  echo "ERROR: websocat is required for this verification script." >&2
  echo "Install it, then rerun with AUTH_TOKEN and BASE_URL set." >&2
  exit 2
fi

if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: node is required to parse ws-ticket JSON responses." >&2
  exit 2
fi

base_url="${BASE_URL%/}"
case "${base_url}" in
  https://*) ws_base_url="wss://${base_url#https://}" ;;
  http://*) ws_base_url="ws://${base_url#http://}" ;;
  *) echo "ERROR: BASE_URL must start with http:// or https://" >&2; exit 2 ;;
esac

tmp_dir="$(mktemp -d)"
pids=()

cleanup() {
  for pid in "${pids[@]:-}"; do
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
    fi
  done
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

fetch_ticket() {
  local response
  response="$(curl -fsS -X POST "${base_url}/canvas/notifications/ws-ticket" \
    -H "Authorization: Bearer ${AUTH_TOKEN}")"
  node -e '
    const fs = require("fs");
    const response = JSON.parse(fs.readFileSync(0, "utf8"));
    const ticket = response && response.data && response.data.ticket;
    if (!ticket) process.exit(1);
    process.stdout.write(ticket);
  ' <<< "${response}"
}

attempts=$((MAX_SESSIONS_PER_USER + 1))
echo "Opening ${attempts} notification WebSocket connections; expected live connections <= ${MAX_SESSIONS_PER_USER}."

for i in $(seq 1 "${attempts}"); do
  ticket="$(fetch_ticket)"
  ws_url="${ws_base_url}/canvas/ws/notifications?ticket=${ticket}"
  (sleep "${HOLD_SECONDS}" | websocat "${ws_url}" >"${tmp_dir}/ws-${i}.out" 2>"${tmp_dir}/ws-${i}.err") &
  pids+=("$!")
  sleep 0.2
done

sleep "${SETTLE_SECONDS}"

alive=0
for pid in "${pids[@]}"; do
  if kill -0 "${pid}" >/dev/null 2>&1; then
    alive=$((alive + 1))
  fi
done

echo "Live WebSocket client processes after settle: ${alive}"

if (( alive == 0 )); then
  echo "ERROR: no WebSocket connection stayed open; check AUTH_TOKEN and backend availability." >&2
  exit 1
fi

if (( alive > MAX_SESSIONS_PER_USER )); then
  echo "ERROR: WebSocket per-user limit was not enforced." >&2
  for file in "${tmp_dir}"/ws-*.err; do
    [[ -s "${file}" ]] && { echo "--- ${file}"; cat "${file}"; }
  done
  exit 1
fi

echo "OK: WebSocket per-user connection limit is enforced."
