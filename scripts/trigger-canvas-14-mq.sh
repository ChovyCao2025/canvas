#!/usr/bin/env bash
set -euo pipefail

CANVAS_ID="${CANVAS_ID:-14}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-canvas-mysql}"
REDIS_CONTAINER="${REDIS_CONTAINER:-canvas-redis}"
ROCKETMQ_CONTAINER="${ROCKETMQ_CONTAINER:-canvas-rocketmq-broker-1}"
MYSQL_DB="${MYSQL_DB:-canvas_db}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
NAMESRV_ADDR="${NAMESRV_ADDR:-rocketmq-namesrv:9876}"
MQADMIN="${MQADMIN:-/home/rocketmq/rocketmq-5.3.1/bin/mqadmin}"
TOPIC="${TOPIC:-CANVAS_MQ_TRIGGER}"
TAG="${TAG:-flight_order_status_change}"
MESSAGE_CODE="${MESSAGE_CODE:-flight_order_status_change}"
USER_ID="${USER_ID:-canvas14_user_$(date +%s)}"
ORDER_ID="${ORDER_ID:-ORD-CANVAS14-$(date +%Y%m%d%H%M%S)}"
TRIGGER_RUN_ID="${TRIGGER_RUN_ID:-canvas14-$(date +%Y%m%d%H%M%S)}"
POLL_SECONDS="${POLL_SECONDS:-30}"

mysql_exec() {
  docker exec "${MYSQL_CONTAINER}" mysql --default-character-set=utf8mb4 \
    -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" -N -B -e "$1"
}

mysql_show() {
  docker exec "${MYSQL_CONTAINER}" mysql --default-character-set=utf8mb4 \
    -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" -e "$1"
}

require_container() {
  local name="$1"
  if ! docker ps --format '{{.Names}}' | grep -qx "${name}"; then
    echo "ERROR: container ${name} is not running" >&2
    exit 1
  fi
}

echo "=== Canvas ${CANVAS_ID} MQ trigger ==="
echo "userId=${USER_ID}"
echo "orderId=${ORDER_ID}"
echo "triggerRunId=${TRIGGER_RUN_ID}"
echo

require_container "${MYSQL_CONTAINER}"
require_container "${REDIS_CONTAINER}"
require_container "${ROCKETMQ_CONTAINER}"

echo "1) Ensure canvas ${CANVAS_ID} is published locally"
canvas_row="$(mysql_exec "SELECT CONCAT(IFNULL(published_version_id,''),'|',status) FROM canvas WHERE id=${CANVAS_ID};")"
if [[ -z "${canvas_row}" ]]; then
  echo "ERROR: canvas ${CANVAS_ID} not found in ${MYSQL_DB}.canvas" >&2
  exit 1
fi

published_version_id="${canvas_row%%|*}"
canvas_status="${canvas_row##*|}"
if [[ -z "${published_version_id}" || "${canvas_status}" != "1" ]]; then
  draft_version_id="$(mysql_exec "SELECT id FROM canvas_version WHERE canvas_id=${CANVAS_ID} ORDER BY id DESC LIMIT 1;")"
  if [[ -z "${draft_version_id}" ]]; then
    echo "ERROR: canvas ${CANVAS_ID} has no canvas_version row to publish" >&2
    exit 1
  fi
  mysql_exec "
    INSERT INTO canvas_version (tenant_id, canvas_id, version, graph_json, status, created_by, created_at)
    SELECT tenant_id, canvas_id,
           COALESCE((SELECT MAX(v.version) FROM canvas_version v WHERE v.canvas_id=${CANVAS_ID}), 0) + 1,
           graph_json, 1, 'trigger-canvas-14-mq.sh', NOW()
    FROM canvas_version
    WHERE id=${draft_version_id};
    UPDATE canvas
    SET status=1,
        published_version_id=LAST_INSERT_ID(),
        updated_at=NOW()
    WHERE id=${CANVAS_ID};
  " >/dev/null
  published_version_id="$(mysql_exec "SELECT published_version_id FROM canvas WHERE id=${CANVAS_ID};")"
fi
echo "publishedVersionId=${published_version_id}"

echo
echo "2) Ensure MQ definitions and Redis route"
mysql_exec "
  INSERT INTO mq_message_definition
    (name, message_code, topic, request_schema, description, enabled, created_by, created_at, updated_at)
  VALUES
    ('订单支付通知', 'order_paid_notice', 'order_paid_notice', '[]', 'Canvas 14 SEND_MQ 下游测试消息', 1, 'trigger-canvas-14-mq.sh', NOW(), NOW())
  ON DUPLICATE KEY UPDATE
    topic=VALUES(topic),
    enabled=1,
    updated_at=NOW();
" >/dev/null
docker exec "${REDIS_CONTAINER}" redis-cli SADD "canvas:trigger:mq:${TAG}" "${CANVAS_ID}" >/dev/null
docker exec "${REDIS_CONTAINER}" redis-cli SET "canvas:trigger:routes:ready" "1" >/dev/null
echo "route=canvas:trigger:mq:${TAG} -> ${CANVAS_ID}"

echo
echo "3) Send RocketMQ message"
BODY="{\"userId\":\"${USER_ID}\",\"messageCode\":\"${MESSAGE_CODE}\",\"payload\":{\"orderId\":\"${ORDER_ID}\",\"orderStatus\":\"PAID\",\"amount\":99.90,\"triggerRunId\":\"${TRIGGER_RUN_ID}\"}}"
docker exec "${ROCKETMQ_CONTAINER}" "${MQADMIN}" sendMessage \
  -n "${NAMESRV_ADDR}" \
  -t "${TOPIC}" \
  -c "${TAG}" \
  -k "${TRIGGER_RUN_ID}" \
  -p "${BODY}"

echo
echo "4) Poll execution result"
execution_id=""
for _ in $(seq 1 "${POLL_SECONDS}"); do
  execution_id="$(mysql_exec "SELECT id FROM canvas_execution WHERE canvas_id=${CANVAS_ID} AND user_id='${USER_ID}' ORDER BY created_at DESC LIMIT 1;")"
  if [[ -n "${execution_id}" ]]; then
    status="$(mysql_exec "SELECT status FROM canvas_execution WHERE id='${execution_id}';")"
    if [[ "${status}" == "2" || "${status}" == "3" ]]; then
      break
    fi
  fi
  sleep 1
done

echo
echo "=== Execution request ==="
mysql_show "
  SELECT id, canvas_id, user_id, trigger_type, trigger_node_type, match_key, status,
         attempt_count, last_error, created_at, updated_at
  FROM canvas_execution_request
  WHERE canvas_id=${CANVAS_ID}
    AND user_id='${USER_ID}'
  ORDER BY created_at DESC
  LIMIT 5;
"

echo
echo "=== Execution ==="
mysql_show "
  SELECT id, canvas_id, version_id, user_id, trigger_type, status, created_at, updated_at,
         LEFT(result, 500) AS result
  FROM canvas_execution
  WHERE canvas_id=${CANVAS_ID}
    AND user_id='${USER_ID}'
  ORDER BY created_at DESC
  LIMIT 5;
"

if [[ -n "${execution_id}" ]]; then
  echo
  echo "=== Node trace for execution ${execution_id} ==="
  mysql_show "
    SELECT node_id, node_type, node_name, status, outcome, reason_code,
           LEFT(output_data, 500) AS output_data,
           LEFT(error_msg, 300) AS error_msg,
           duration_ms, started_at, finished_at
    FROM canvas_execution_trace
    WHERE execution_id='${execution_id}'
    ORDER BY id;
  "
fi

echo
echo "=== Recent MQ rejected rows for tag ${TAG} ==="
mysql_show "
  SELECT id, msg_id, tag, reason, error_msg, created_at
  FROM canvas_mq_trigger_rejected
  WHERE tag='${TAG}'
  ORDER BY id DESC
  LIMIT 5;
"

echo
echo "Done. Open UI: http://localhost:5173/canvas/${CANVAS_ID}/stats or inspect MySQL tables above."
