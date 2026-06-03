#!/usr/bin/env bash
set -euo pipefail

SOURCE_MSG_ID="${1:-${SOURCE_MSG_ID:-}}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-canvas-mysql}"
MYSQL_DB="${MYSQL_DB:-canvas_db}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"

if [[ -z "${SOURCE_MSG_ID}" ]]; then
  echo "Usage: SOURCE_MSG_ID=<rocketmq-msg-id> $0" >&2
  echo "   or: $0 <rocketmq-msg-id>" >&2
  exit 2
fi

require_container() {
  local name="$1"
  if ! docker ps --format '{{.Names}}' | grep -qx "${name}"; then
    echo "ERROR: container ${name} is not running" >&2
    exit 1
  fi
}

mysql_exec() {
  docker exec "${MYSQL_CONTAINER}" mysql --default-character-set=utf8mb4 \
    -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" -N -B -e "$1"
}

require_container "${MYSQL_CONTAINER}"

escaped_source_msg_id="$(printf "%s" "${SOURCE_MSG_ID}" | sed "s/'/''/g")"

rows="$(mysql_exec "
  SELECT CONCAT(
           canvas_id, '\t',
           COUNT(*), '\t',
           COUNT(DISTINCT id), '\t',
           COALESCE(GROUP_CONCAT(DISTINCT id ORDER BY id SEPARATOR ','), ''), '\t',
           COALESCE(MAX(attempt_count), 0)
         )
  FROM canvas_execution_request
  WHERE source_msg_id='${escaped_source_msg_id}'
  GROUP BY canvas_id
  ORDER BY canvas_id;
")"

if [[ -z "${rows}" ]]; then
  echo "ERROR: no canvas_execution_request rows found for source_msg_id=${SOURCE_MSG_ID}" >&2
  exit 1
fi

echo "canvas_id	rows	distinct_request_ids	request_ids	max_attempt_count"
printf '%s\n' "${rows}"

bad_count="$(mysql_exec "
  SELECT COUNT(*)
  FROM (
    SELECT canvas_id
    FROM canvas_execution_request
    WHERE source_msg_id='${escaped_source_msg_id}'
    GROUP BY canvas_id
    HAVING COUNT(*) <> 1 OR COUNT(DISTINCT id) <> 1
  ) bad;
")"

if [[ "${bad_count}" != "0" ]]; then
  echo "ERROR: duplicate or unstable execution requests found for source_msg_id=${SOURCE_MSG_ID}" >&2
  exit 1
fi

echo "OK: source_msg_id=${SOURCE_MSG_ID} maps to exactly one deterministic request per canvas."
