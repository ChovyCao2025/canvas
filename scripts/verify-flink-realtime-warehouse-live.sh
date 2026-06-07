#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.local.yml"
CDP_DORIS_DDL="${ROOT_DIR}/backend/canvas-engine/src/main/resources/infrastructure/doris/cdp-audience-ddl.sql"
TRACE_DORIS_DDL="${ROOT_DIR}/backend/canvas-engine/src/main/resources/infrastructure/doris/trace-ddl.sql"

RUN_LIVE="${CANVAS_RUN_LIVE_FLINK_E2E:-false}"
BASE_URL="${CANVAS_BASE_URL:-http://localhost:8080}"
CHECKPOINT_ENDPOINT="${CANVAS_LIVE_FLINK_CHECKPOINT_ENDPOINT:-http://host.docker.internal:8080/warehouse/realtime/pipelines/checkpoints}"
START_ENGINE="${CANVAS_LIVE_START_ENGINE:-false}"
ENGINE_LOG="${CANVAS_LIVE_ENGINE_LOG:-${ROOT_DIR}/tmp/flink-live-canvas-engine.log}"
ENGINE_JAR="${CANVAS_LIVE_ENGINE_JAR:-${BACKEND_DIR}/canvas-engine/target/canvas-engine-1.0.0-SNAPSHOT.jar}"
TENANT_ID="${CANVAS_LIVE_TENANT_ID:-0}"
MYSQL_CONTAINER="${CANVAS_LIVE_MYSQL_CONTAINER:-canvas-mysql}"
DORIS_CLIENT_CONTAINER="${CANVAS_LIVE_DORIS_CLIENT_CONTAINER:-canvas-mysql}"
FLINK_JM_CONTAINER="${CANVAS_LIVE_FLINK_JM_CONTAINER:-canvas-flink-jobmanager}"
MYSQL_DB="${CANVAS_LIVE_MYSQL_DB:-}"
if [[ -z "${MYSQL_DB}" ]]; then
  if [[ "${START_ENGINE}" == "true" ]]; then
    MYSQL_DB="canvas_flink_live"
  else
    MYSQL_DB="canvas_db"
  fi
fi
MYSQL_USER="${CANVAS_LIVE_MYSQL_USER:-root}"
MYSQL_PASSWORD="${CANVAS_LIVE_MYSQL_PASSWORD:-root}"
RESET_MYSQL_DB="${CANVAS_LIVE_RESET_MYSQL_DB:-}"
if [[ -z "${RESET_MYSQL_DB}" ]]; then
  RESET_MYSQL_DB="${START_ENGINE}"
fi
ALLOW_RESET_SHARED_MYSQL_DB="${CANVAS_LIVE_ALLOW_RESET_SHARED_MYSQL_DB:-false}"
DORIS_HOST="${CANVAS_LIVE_DORIS_HOST:-doris-fe}"
DORIS_PORT="${CANVAS_LIVE_DORIS_PORT:-9030}"
DORIS_USER="${CANVAS_LIVE_DORIS_USER:-root}"
DORIS_PASSWORD="${CANVAS_LIVE_DORIS_PASSWORD:-}"
DORIS_REPLICATION_NUM="${CANVAS_LIVE_DORIS_REPLICATION_NUM:-1}"
DORIS_DWS_DYNAMIC_PARTITION_START="${CANVAS_LIVE_DORIS_DWS_DYNAMIC_PARTITION_START:--365}"
FLINK_REST_URL="${CANVAS_LIVE_FLINK_REST_URL:-http://localhost:8082}"
JOB_JAR="${CANVAS_LIVE_FLINK_JOB_JAR:-/opt/flink/usrlib/canvas-flink-jobs-1.0.0-SNAPSHOT.jar}"
PIPELINE_KEY="${CANVAS_LIVE_PIPELINE_KEY:-mysql_cdp_event_log_to_doris_ods}"
PIPELINE_TRACE="${CANVAS_LIVE_TRACE_PIPELINE_KEY:-mysql_canvas_trace_to_doris_ods}"
PIPELINE_DWD="${CANVAS_LIVE_DWD_PIPELINE_KEY:-doris_ods_cdp_event_to_dwd_fact}"
PIPELINE_DWS="${CANVAS_LIVE_DWS_PIPELINE_KEY:-doris_dwd_user_fact_to_dws_metric_daily}"
RUN_ID="${CANVAS_LIVE_RUN_ID:-$(date +%Y%m%d%H%M%S)_$$}"
DORIS_LABEL_SUFFIX="${CANVAS_FLINK_DORIS_LABEL_SUFFIX:-_live_${RUN_ID}}"
VERIFY_ATTEMPTS="${CANVAS_LIVE_VERIFY_ATTEMPTS:-10}"
VERIFY_DELAY_MS="${CANVAS_LIVE_VERIFY_DELAY_MS:-1000}"
VERIFY_DERIVED_LAYERS="${CANVAS_LIVE_VERIFY_DERIVED_LAYERS:-true}"
DERIVED_VERIFY_ATTEMPTS="${CANVAS_LIVE_DERIVED_VERIFY_ATTEMPTS:-30}"
REQUIRE_CUTOVER_PASS="${CANVAS_LIVE_REQUIRE_CUTOVER_PASS:-false}"
STOP_ROCKETMQ_AFTER_ENGINE_READY="${CANVAS_LIVE_STOP_ROCKETMQ_AFTER_ENGINE_READY:-false}"
AUTH_HEADER="${CANVAS_API_AUTH_HEADER:-}"
INTERNAL_API_TOKEN="${CANVAS_INTERNAL_API_TOKEN:-}"
AUTH_USERNAME="${CANVAS_LIVE_AUTH_USERNAME:-admin}"
AUTH_PASSWORD="${CANVAS_LIVE_AUTH_PASSWORD:-Admin@123}"
ENGINE_PID=""

log() {
  printf '[flink-live] %s\n' "$*"
}

fail() {
  printf '[flink-live] ERROR: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

cleanup() {
  if [[ -n "${ENGINE_PID}" ]]; then
    kill "${ENGINE_PID}" >/dev/null 2>&1 || true
    wait "${ENGINE_PID}" >/dev/null 2>&1 || true
  fi
}

api_curl() {
  if [[ -n "${AUTH_HEADER}" ]]; then
    curl -fsS -H "${AUTH_HEADER}" "$@"
  else
    curl -fsS "$@"
  fi
}

wait_until() {
  local name="$1"
  local attempts="$2"
  local sleep_seconds="$3"
  shift 3
  for ((i = 1; i <= attempts; i++)); do
    if "$@" >/dev/null 2>&1; then
      log "${name} is ready"
      return 0
    fi
    sleep "${sleep_seconds}"
  done
  return 1
}

tcp_ready() {
  local host="$1"
  local port="$2"
  bash -c ":</dev/tcp/${host}/${port}"
}

mysql_exec() {
  docker exec "${MYSQL_CONTAINER}" mysql --default-character-set=utf8mb4 \
    -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" -N -B -e "$1"
}

mysql_server_exec() {
  docker exec "${MYSQL_CONTAINER}" mysql --default-character-set=utf8mb4 \
    -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -N -B -e "$1"
}

mysql_var() {
  mysql_server_exec "SHOW VARIABLES LIKE '$1';" | awk '{print toupper($2)}'
}

assert_safe_mysql_db_name() {
  [[ "${MYSQL_DB}" =~ ^[A-Za-z0-9_]+$ ]] \
    || fail "CANVAS_LIVE_MYSQL_DB must contain only letters, numbers, and underscores; got ${MYSQL_DB}"
}

prepare_mysql_database() {
  assert_safe_mysql_db_name
  if [[ "${RESET_MYSQL_DB}" == "true" ]]; then
    if [[ "${MYSQL_DB}" != canvas_flink_live* && "${ALLOW_RESET_SHARED_MYSQL_DB}" != "true" ]]; then
      fail "refusing to reset non-dedicated MySQL database ${MYSQL_DB}; set CANVAS_LIVE_ALLOW_RESET_SHARED_MYSQL_DB=true only for disposable local data"
    fi
    log "resetting isolated MySQL database ${MYSQL_DB} for live verification"
    mysql_server_exec "DROP DATABASE IF EXISTS \`${MYSQL_DB}\`; CREATE DATABASE \`${MYSQL_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  else
    log "using existing MySQL database ${MYSQL_DB}; set CANVAS_LIVE_RESET_MYSQL_DB=true to recreate it"
    mysql_server_exec "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  fi
}

doris_exec() {
  if [[ -n "${DORIS_PASSWORD}" ]]; then
    docker exec "${DORIS_CLIENT_CONTAINER}" mysql --default-character-set=utf8mb4 \
      -h"${DORIS_HOST}" -P"${DORIS_PORT}" -u"${DORIS_USER}" -p"${DORIS_PASSWORD}" -N -B -e "$1"
  else
    docker exec "${DORIS_CLIENT_CONTAINER}" mysql --default-character-set=utf8mb4 \
      -h"${DORIS_HOST}" -P"${DORIS_PORT}" -u"${DORIS_USER}" -N -B -e "$1"
  fi
}

apply_doris_ddl() {
  if [[ -n "${DORIS_PASSWORD}" ]]; then
    sed \
      -e "s/\"replication_num\" = \"3\"/\"replication_num\" = \"${DORIS_REPLICATION_NUM}\"/g" \
      -e "s/\"dynamic_partition.start\" = \"-730\"/\"dynamic_partition.start\" = \"${DORIS_DWS_DYNAMIC_PARTITION_START}\"/g" \
      "${CDP_DORIS_DDL}" \
      | docker exec -i "${DORIS_CLIENT_CONTAINER}" mysql --default-character-set=utf8mb4 \
        -h"${DORIS_HOST}" -P"${DORIS_PORT}" -u"${DORIS_USER}" -p"${DORIS_PASSWORD}"
    sed \
      -e "s/\"replication_num\" = \"3\"/\"replication_num\" = \"${DORIS_REPLICATION_NUM}\"/g" \
      -e "s/\"dynamic_partition.start\" = \"-730\"/\"dynamic_partition.start\" = \"${DORIS_DWS_DYNAMIC_PARTITION_START}\"/g" \
      "${TRACE_DORIS_DDL}" \
      | docker exec -i "${DORIS_CLIENT_CONTAINER}" mysql --default-character-set=utf8mb4 \
        -h"${DORIS_HOST}" -P"${DORIS_PORT}" -u"${DORIS_USER}" -p"${DORIS_PASSWORD}"
  else
    sed \
      -e "s/\"replication_num\" = \"3\"/\"replication_num\" = \"${DORIS_REPLICATION_NUM}\"/g" \
      -e "s/\"dynamic_partition.start\" = \"-730\"/\"dynamic_partition.start\" = \"${DORIS_DWS_DYNAMIC_PARTITION_START}\"/g" \
      "${CDP_DORIS_DDL}" \
      | docker exec -i "${DORIS_CLIENT_CONTAINER}" mysql --default-character-set=utf8mb4 \
        -h"${DORIS_HOST}" -P"${DORIS_PORT}" -u"${DORIS_USER}"
    sed \
      -e "s/\"replication_num\" = \"3\"/\"replication_num\" = \"${DORIS_REPLICATION_NUM}\"/g" \
      -e "s/\"dynamic_partition.start\" = \"-730\"/\"dynamic_partition.start\" = \"${DORIS_DWS_DYNAMIC_PARTITION_START}\"/g" \
      "${TRACE_DORIS_DDL}" \
      | docker exec -i "${DORIS_CLIENT_CONTAINER}" mysql --default-character-set=utf8mb4 \
        -h"${DORIS_HOST}" -P"${DORIS_PORT}" -u"${DORIS_USER}"
  fi
}

require_response_contains() {
  local response="$1"
  local expected="$2"
  if [[ "${response}" != *"${expected}"* ]]; then
    fail "response did not contain ${expected}: ${response}"
  fi
}

extract_json_string() {
  local response="$1"
  local field="$2"
  printf '%s' "${response}" | sed -n "s/.*\"${field}\":\"\\([^\"]*\\)\".*/\\1/p"
}

ensure_api_auth_header() {
  if [[ -n "${AUTH_HEADER}" ]]; then
    return 0
  fi
  local response
  local token
  log "requesting local JWT with ${AUTH_USERNAME}; set CANVAS_API_AUTH_HEADER to override"
  response="$(curl -fsS \
    -H 'Content-Type: application/json' \
    -X POST "${BASE_URL}/auth/login" \
    -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}")" \
    || fail "login failed; set CANVAS_API_AUTH_HEADER='Authorization: Bearer <token>' or CANVAS_LIVE_AUTH_USERNAME/CANVAS_LIVE_AUTH_PASSWORD"
  token="$(extract_json_string "${response}" "token")"
  [[ -n "${token}" ]] || fail "login response did not contain token: ${response}"
  AUTH_HEADER="Authorization: Bearer ${token}"
}

stop_rocketmq_after_engine_ready_if_requested() {
  if [[ "${STOP_ROCKETMQ_AFTER_ENGINE_READY}" != "true" ]]; then
    return 0
  fi
  log "stopping RocketMQ services after canvas-engine is ready to free local Doris scan memory"
  docker compose -f "${COMPOSE_FILE}" stop rocketmq-broker rocketmq-namesrv >/dev/null
}

submit_pipeline() {
  local pipeline_key="${1:-${PIPELINE_KEY}}"
  log "submitting Flink pipeline ${pipeline_key}"
  docker exec \
    -e CANVAS_FLINK_TENANT_ID="${TENANT_ID}" \
    -e CANVAS_FLINK_JOB_PIPELINE_KEY="${pipeline_key}" \
    -e CANVAS_FLINK_MYSQL_URL="jdbc:mysql://mysql:3306/${MYSQL_DB}?useSSL=false&allowPublicKeyRetrieval=true" \
    -e CANVAS_FLINK_MYSQL_USERNAME="${MYSQL_USER}" \
    -e CANVAS_FLINK_MYSQL_PASSWORD="${MYSQL_PASSWORD}" \
    -e CANVAS_FLINK_DORIS_FE_NODES="doris-fe:8030" \
    -e CANVAS_FLINK_DORIS_BE_NODES="doris-be:8040" \
    -e CANVAS_FLINK_DORIS_JDBC_URL="jdbc:mysql://doris-fe:9030" \
    -e CANVAS_FLINK_DORIS_USERNAME="${DORIS_USER}" \
    -e CANVAS_FLINK_DORIS_PASSWORD="${DORIS_PASSWORD}" \
    -e CANVAS_FLINK_DORIS_LABEL_SUFFIX="${DORIS_LABEL_SUFFIX}" \
    -e CANVAS_FLINK_CHECKPOINT_ENDPOINT="${CHECKPOINT_ENDPOINT}" \
    -e CANVAS_FLINK_INTERNAL_API_TOKEN="${INTERNAL_API_TOKEN}" \
    -e CANVAS_FLINK_REPORTED_BY="canvas-flink-jobs-live-verify" \
    "${FLINK_JM_CONTAINER}" \
    flink run -d \
      -c org.chovy.canvas.flink.CanvasFlinkJobMain \
      "${JOB_JAR}" \
      --pipeline-key="${pipeline_key}"
}

pipeline_pass() {
  local pipeline_key="${1:-${PIPELINE_KEY}}"
  local response
  response="$(api_curl "${BASE_URL}/warehouse/realtime/pipelines/status?recentLimit=5")" || return 1
  [[ "${response}" == *"\"pipelineKey\":\"${pipeline_key}\""* && "${response}" == *'"runtimeStatus":"PASS"'* ]]
}

iso_now() {
  date -u +"%Y-%m-%dT%H:%M:%S"
}

report_pipeline_runtime_proof() {
  local pipeline_key="$1"
  local proof_offset="$2"
  local row_count="$3"
  local observed_at
  observed_at="$(iso_now)"
  log "reporting PASS runtime proof for ${pipeline_key} after row-level validation"
  # startup submission is not runtime checkpoint evidence; callers invoke this only after row-level Doris validation.
  api_curl \
    -H 'Content-Type: application/json' \
    -X POST "${BASE_URL}/warehouse/realtime/pipelines/checkpoints" \
    -d "{
      \"pipelineKey\":\"${pipeline_key}\",
      \"checkpointId\":\"live-proof-${RUN_ID}-${pipeline_key}\",
      \"sourcePartition\":\"live-row-proof\",
      \"sourceOffset\":\"${proof_offset}\",
      \"committedOffset\":\"${proof_offset}\",
      \"watermarkTime\":\"${observed_at}\",
      \"checkpointTime\":\"${observed_at}\",
      \"lagMs\":0,
      \"rowCount\":${row_count},
      \"status\":\"PASS\",
      \"reportedBy\":\"canvas-flink-live-verifier\"
    }" >/dev/null
}

dwd_probe_visible() {
  local rows
  rows="$(doris_exec "SELECT COUNT(1) FROM canvas_dwd.cdp_user_event_fact WHERE tenant_id=${probe_tenant_id} AND user_id='${probe_user_id}' AND event_code='${probe_event_code}' AND properties_json LIKE '%${message_id}%';")" || return 1
  [[ "${rows}" != "0" ]]
}

dws_probe_visible() {
  local count_value
  count_value="$(doris_exec "SELECT COALESCE(SUM(count_value), 0) FROM canvas_dws.user_event_metric_daily WHERE tenant_id=${probe_tenant_id} AND user_id='${probe_user_id}' AND event_code='${probe_event_code}' AND stat_date='${probe_event_date}';")" || return 1
  [[ "${count_value}" =~ ^[1-9][0-9]*$ ]]
}

create_trace_probe() {
  trace_tenant_id="${CANVAS_LIVE_TRACE_TENANT_ID:-${TENANT_ID}}"
  [[ "${trace_tenant_id}" =~ ^[0-9]+$ ]] \
    || fail "CANVAS_LIVE_TRACE_TENANT_ID must be numeric; got ${trace_tenant_id}"
  trace_execution_id="trace-live-${RUN_ID}"
  trace_node_id="trace-node-${RUN_ID}"
  trace_node_name="trace live ${RUN_ID}"
  [[ "${trace_execution_id}" =~ ^[A-Za-z0-9_.:-]+$ ]] \
    || fail "generated unsafe trace_execution_id: ${trace_execution_id}"
  [[ "${trace_node_id}" =~ ^[A-Za-z0-9_.:-]+$ ]] \
    || fail "generated unsafe trace_node_id: ${trace_node_id}"
  mysql_exec "INSERT INTO canvas_execution_trace
    (tenant_id, execution_id, node_id, node_type, node_name, status, input_data, output_data, error_msg, started_at, finished_at, duration_ms)
    VALUES (${trace_tenant_id}, '${trace_execution_id}', '${trace_node_id}', 'START', '${trace_node_name}', 1,
            '{\"probe\":\"trace-live\",\"runId\":\"${RUN_ID}\"}',
            '{\"status\":\"ok\",\"runId\":\"${RUN_ID}\"}',
            NULL, NOW(3), NOW(3), 7);"
}

trace_probe_visible() {
  local rows
  rows="$(doris_exec "SELECT COUNT(1) FROM canvas_ods.canvas_execution_trace WHERE tenant_id=${trace_tenant_id} AND execution_id='${trace_execution_id}' AND node_id='${trace_node_id}';")" || return 1
  [[ "${rows}" != "0" ]]
}

verify_trace_ods_pipeline() {
  log "creating MySQL trace probe for ${PIPELINE_TRACE}"
  create_trace_probe
  submit_pipeline "${PIPELINE_TRACE}"
  wait_until "Doris trace ODS row for ${trace_execution_id}" "${DERIVED_VERIFY_ATTEMPTS}" 2 trace_probe_visible \
    || fail "Doris trace ODS validation failed for execution_id=${trace_execution_id}"
  trace_ods_rows="$(doris_exec "SELECT COUNT(1) FROM canvas_ods.canvas_execution_trace WHERE tenant_id=${trace_tenant_id} AND execution_id='${trace_execution_id}' AND node_id='${trace_node_id}';")"
  report_pipeline_runtime_proof "${PIPELINE_TRACE}" "${trace_execution_id}" "${trace_ods_rows}"
  log "Doris trace ODS proof completed for execution_id=${trace_execution_id}; trace_ods_rows=${trace_ods_rows}"
}

verify_derived_layers_if_requested() {
  if [[ "${VERIFY_DERIVED_LAYERS}" != "true" ]]; then
    log "skipping DWD/DWS derived layer verification because CANVAS_LIVE_VERIFY_DERIVED_LAYERS=${VERIFY_DERIVED_LAYERS}"
    return 0
  fi

  log "verifying Doris ODS -> DWD -> DWS derived layers for message_id=${message_id}"
  submit_pipeline "${PIPELINE_DWD}"
  wait_until "DWD row for ${message_id}" "${DERIVED_VERIFY_ATTEMPTS}" 2 dwd_probe_visible \
    || fail "Doris DWD validation failed for message_id=${message_id}"
  dwd_rows="$(doris_exec "SELECT COUNT(1) FROM canvas_dwd.cdp_user_event_fact WHERE tenant_id=${probe_tenant_id} AND user_id='${probe_user_id}' AND event_code='${probe_event_code}' AND properties_json LIKE '%${message_id}%';")"
  report_pipeline_runtime_proof "${PIPELINE_DWD}" "${message_id}" "${dwd_rows}"

  submit_pipeline "${PIPELINE_DWS}"
  wait_until "DWS metric for ${message_id}" "${DERIVED_VERIFY_ATTEMPTS}" 2 dws_probe_visible \
    || fail "Doris DWS validation failed for message_id=${message_id}"
  dws_count_value="$(doris_exec "SELECT COALESCE(SUM(count_value), 0) FROM canvas_dws.user_event_metric_daily WHERE tenant_id=${probe_tenant_id} AND user_id='${probe_user_id}' AND event_code='${probe_event_code}' AND stat_date='${probe_event_date}';")"
  report_pipeline_runtime_proof "${PIPELINE_DWS}" "${message_id}" "${dws_count_value}"

  log "Doris derived layer proof completed for message_id=${message_id}; dwd_rows=${dwd_rows}; dws_count_value=${dws_count_value}"
}

canvas_engine_ready() {
  curl -fsS "${BASE_URL}/actuator/health" >/dev/null 2>&1
}

start_canvas_engine_if_requested() {
  if canvas_engine_ready; then
    log "Canvas engine is already reachable at ${BASE_URL}"
    return 0
  fi
  if [[ "${START_ENGINE}" != "true" ]]; then
    return 1
  fi
  mkdir -p "$(dirname "${ENGINE_LOG}")"
  log "starting canvas-engine for live verification; log=${ENGINE_LOG}"
  (
    cd "${BACKEND_DIR}"
    mvn -f canvas-engine/pom.xml -DskipTests -Dmaven.test.skip=true package || exit 1
    CANVAS_DORIS_ENABLED=true \
      CANVAS_DORIS_JDBC_URL="jdbc:mysql://localhost:${DORIS_PORT}/canvas_ods?useSSL=false&allowPublicKeyRetrieval=true" \
      CANVAS_DORIS_USERNAME="${DORIS_USER}" \
      CANVAS_DORIS_PASSWORD="${DORIS_PASSWORD}" \
      CANVAS_INTERNAL_API_TOKEN="${INTERNAL_API_TOKEN}" \
      CANVAS_JWT_SECRET="${CANVAS_JWT_SECRET:-canvas-live-verify-jwt-secret-32-bytes}" \
      SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/${MYSQL_DB}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
      SPRING_DATASOURCE_USERNAME="${MYSQL_USER}" \
      SPRING_DATASOURCE_PASSWORD="${MYSQL_PASSWORD}" \
      "${JAVA_HOME}/bin/java" -jar "${ENGINE_JAR}"
  ) >"${ENGINE_LOG}" 2>&1 &
  ENGINE_PID="$!"
  trap cleanup EXIT
  wait_until "Canvas engine" 120 2 canvas_engine_ready
}

if [[ "${RUN_LIVE}" != "true" ]]; then
  log "skipping live Flink E2E verification; set CANVAS_RUN_LIVE_FLINK_E2E=true to run it"
  exit 0
fi

require_cmd docker
require_cmd curl
require_cmd sed
require_cmd awk

[[ "${RUN_ID}" =~ ^[A-Za-z0-9_-]+$ ]] \
  || fail "CANVAS_LIVE_RUN_ID may only contain letters, numbers, '_' and '-'"
[[ "${DORIS_LABEL_SUFFIX}" =~ ^[A-Za-z0-9_-]*$ ]] \
  || fail "CANVAS_FLINK_DORIS_LABEL_SUFFIX may only contain letters, numbers, '_' and '-'"
[[ "${VERIFY_DERIVED_LAYERS}" == "true" || "${VERIFY_DERIVED_LAYERS}" == "false" ]] \
  || fail "CANVAS_LIVE_VERIFY_DERIVED_LAYERS must be true or false"

if [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$({ /usr/libexec/java_home -v 21; } 2>/dev/null || true)"
fi
[[ -n "${JAVA_HOME:-}" ]] || fail "JAVA_HOME is not set and Java 21 could not be discovered"

log "building Flink job jar"
cd "${BACKEND_DIR}"
mvn -pl canvas-flink-jobs -DskipTests package

log "starting local MySQL, Redis, RocketMQ, Doris, and Flink services"
cd "${ROOT_DIR}"
docker compose -f "${COMPOSE_FILE}" up -d mysql redis rocketmq-namesrv rocketmq-broker doris-fe doris-be
docker compose --profile flink -f "${COMPOSE_FILE}" up --force-recreate flink-init
docker compose --profile flink -f "${COMPOSE_FILE}" up -d --force-recreate flink-jobmanager flink-taskmanager

wait_until "MySQL" 60 2 \
  docker exec "${MYSQL_CONTAINER}" mysqladmin ping -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" --silent \
  || fail "MySQL did not become ready"

wait_until "RocketMQ namesrv" 60 2 tcp_ready 127.0.0.1 9876 \
  || fail "RocketMQ namesrv did not become ready on localhost:9876"
wait_until "RocketMQ broker" 60 2 tcp_ready 127.0.0.1 10911 \
  || fail "RocketMQ broker did not become ready on localhost:10911"

log_bin="$(mysql_var log_bin)"
binlog_format="$(mysql_var binlog_format)"
binlog_row_image="$(mysql_var binlog_row_image)"
[[ "${log_bin}" == "ON" && "${binlog_format}" == "ROW" && "${binlog_row_image}" == "FULL" ]] \
  || fail "MySQL CDC requires log_bin=ON, binlog_format=ROW, binlog_row_image=FULL; got log_bin=${log_bin}, binlog_format=${binlog_format}, binlog_row_image=${binlog_row_image}. Recreate mysql with docker compose -f docker-compose.local.yml up -d --force-recreate mysql"
prepare_mysql_database

wait_until "Doris MySQL protocol" 90 2 doris_exec "SELECT 1" \
  || fail "Doris did not become ready on ${DORIS_HOST}:${DORIS_PORT}"

log "applying local CDP Doris DDL with replication_num=${DORIS_REPLICATION_NUM}, dws_dynamic_partition_start=${DORIS_DWS_DYNAMIC_PARTITION_START}"
apply_doris_ddl
doris_exec "SHOW TABLES FROM canvas_ods LIKE 'cdp_event_log';" | grep -q cdp_event_log \
  || fail "Doris table canvas_ods.cdp_event_log was not created"
doris_exec "SHOW TABLES FROM canvas_ods LIKE 'canvas_execution_trace';" | grep -q canvas_execution_trace \
  || fail "Doris table canvas_ods.canvas_execution_trace was not created"

wait_until "Flink REST" 60 2 curl -fsS "${FLINK_REST_URL}/overview" \
  || fail "Flink REST endpoint did not become ready at ${FLINK_REST_URL}"
docker exec "${FLINK_JM_CONTAINER}" test -s "${JOB_JAR}" \
  || fail "Flink job jar is missing in ${FLINK_JM_CONTAINER}:${JOB_JAR}"

start_canvas_engine_if_requested \
  || fail "Canvas engine is not reachable at ${BASE_URL}; start canvas-engine with CANVAS_DORIS_ENABLED=true or set CANVAS_LIVE_START_ENGINE=true"
ensure_api_auth_header
stop_rocketmq_after_engine_ready_if_requested

source_table_count="$(mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DB}' AND table_name='cdp_event_log';")"
[[ "${source_table_count}" == "1" ]] \
  || fail "MySQL ${MYSQL_DB}.cdp_event_log is missing; run canvas-engine/Flyway migrations first"
trace_source_table_count="$(mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DB}' AND table_name='canvas_execution_trace';")"
[[ "${trace_source_table_count}" == "1" ]] \
  || fail "MySQL ${MYSQL_DB}.canvas_execution_trace is missing; run canvas-engine/Flyway migrations first"

submit_pipeline

log "running MySQL CDC synthetic data-path probe"
probe_response="$(api_curl -X POST "${BASE_URL}/warehouse/data-path-probes/synthetic-ods/run?sourceMode=MYSQL_CDC&strict=true&verifyAttempts=${VERIFY_ATTEMPTS}&verifyDelayMs=${VERIFY_DELAY_MS}")"
require_response_contains "${probe_response}" '"sourceMode":"MYSQL_CDC"'
require_response_contains "${probe_response}" '"sourceStatus":"PASS"'
require_response_contains "${probe_response}" '"sinkStatus":"SKIPPED"'
require_response_contains "${probe_response}" '"odsStatus":"PASS"'

message_id="$(extract_json_string "${probe_response}" "messageId")"
[[ -n "${message_id}" ]] || fail "probe response did not expose messageId: ${probe_response}"
[[ "${message_id}" =~ ^[A-Za-z0-9_.:-]+$ ]] \
  || fail "probe response returned unsafe messageId for SQL validation: ${message_id}"
probe_tenant_id="$(mysql_exec "SELECT tenant_id FROM cdp_warehouse_synthetic_data_path_probe_run WHERE message_id='${message_id}' ORDER BY id DESC LIMIT 1;")"
[[ "${probe_tenant_id}" =~ ^[0-9]+$ ]] \
  || fail "could not resolve numeric probe tenant for message_id=${message_id}; got ${probe_tenant_id}"
probe_user_id="$(mysql_exec "SELECT user_id FROM cdp_event_log WHERE message_id='${message_id}' ORDER BY id DESC LIMIT 1;")"
[[ "${probe_user_id}" =~ ^[A-Za-z0-9_.:-]+$ ]] \
  || fail "could not resolve safe probe user for message_id=${message_id}; got ${probe_user_id}"
probe_event_code="$(mysql_exec "SELECT event_code FROM cdp_event_log WHERE message_id='${message_id}' ORDER BY id DESC LIMIT 1;")"
[[ "${probe_event_code}" =~ ^[A-Za-z0-9_.:-]+$ ]] \
  || fail "could not resolve safe probe event_code for message_id=${message_id}; got ${probe_event_code}"
probe_event_date="$(mysql_exec "SELECT DATE_FORMAT(event_time, '%Y-%m-%d') FROM cdp_event_log WHERE message_id='${message_id}' ORDER BY id DESC LIMIT 1;")"
[[ "${probe_event_date}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] \
  || fail "could not resolve probe event date for message_id=${message_id}; got ${probe_event_date}"
ods_rows="$(doris_exec "SELECT COUNT(1) FROM canvas_ods.cdp_event_log WHERE tenant_id=${probe_tenant_id} AND message_id='${message_id}';")"
[[ "${ods_rows}" != "0" ]] || fail "Doris ODS row-level validation failed for message_id=${message_id}"
report_pipeline_runtime_proof "${PIPELINE_KEY}" "${message_id}" "${ods_rows}"

verify_derived_layers_if_requested
verify_trace_ods_pipeline

cutover_response="$(api_curl "${BASE_URL}/warehouse/realtime/cutover-readiness?targetMode=FLINK_FIRST&pipelineKey=${PIPELINE_KEY}&certificationMode=HYBRID&maxCertificationAgeMinutes=60")"
if [[ "${REQUIRE_CUTOVER_PASS}" == "true" ]]; then
  require_response_contains "${cutover_response}" '"status":"PASS"'
  require_response_contains "${cutover_response}" '"allowed":true'
else
  log "cutover gate response captured but not required to PASS unless CANVAS_LIVE_REQUIRE_CUTOVER_PASS=true"
fi

proof_layers="ODS"
if [[ "${VERIFY_DERIVED_LAYERS}" == "true" ]]; then
  proof_layers="ODS/DWD/DWS"
fi
log "PASS: MySQL CDC -> Flink -> Doris ${proof_layers} proof completed for tenant_id=${probe_tenant_id}, message_id=${message_id}; ods_rows=${ods_rows}"
log "PASS: MySQL CDC -> Flink -> Doris trace ODS proof completed for tenant_id=${trace_tenant_id}, execution_id=${trace_execution_id}; trace_ods_rows=${trace_ods_rows}"
