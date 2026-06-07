#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROD_IMAGE="registry.example.com/marketing-canvas/canvas-flink-jobs:prod"

REQUIRED_PIPELINES=(
  "mysql_cdp_event_log_to_doris_ods"
  "mysql_canvas_trace_to_doris_ods"
  "doris_ods_cdp_event_to_dwd_fact"
  "doris_dwd_user_fact_to_dws_metric_daily"
)

REQUIRED_SECRET_KEYS=(
  "canvas-flink-mysql-url"
  "canvas-flink-mysql-username"
  "canvas-flink-mysql-password"
  "canvas-flink-doris-fe-nodes"
  "canvas-flink-doris-be-nodes"
  "canvas-flink-doris-jdbc-url"
  "canvas-flink-doris-username"
  "canvas-flink-doris-password"
  "canvas-flink-checkpoint-endpoint"
  "canvas-flink-internal-api-token"
)

REQUIRED_ALERTS=(
  "CanvasFlinkJobManagerDown"
  "CanvasFlinkTaskManagerMissing"
  "CanvasFlinkCheckpointFailures"
  "CanvasFlinkBackpressureHigh"
  "CanvasFlinkCheckpointDurationHigh"
)

log() {
  printf '[flink-prod-preflight] %s\n' "$*"
}

fail() {
  printf '[flink-prod-preflight] ERROR: %s\n' "$*" >&2
  exit 1
}

path() {
  printf '%s/%s' "${ROOT_DIR}" "$1"
}

require_file() {
  local relative_path="$1"
  [[ -f "$(path "${relative_path}")" ]] || fail "missing required file: ${relative_path}"
}

require_contains() {
  local relative_path="$1"
  local expected="$2"
  grep -Fq -- "${expected}" "$(path "${relative_path}")" \
    || fail "${relative_path} does not contain required text: ${expected}"
}

require_not_contains() {
  local relative_path="$1"
  local unexpected="$2"
  if grep -Fq -- "${unexpected}" "$(path "${relative_path}")"; then
    fail "${relative_path} contains forbidden text: ${unexpected}"
  fi
}

count_fixed() {
  local relative_path="$1"
  local pattern="$2"
  grep -F -- "${pattern}" "$(path "${relative_path}")" | wc -l | tr -d ' '
}

reject_bare_flink_image() {
  local manifests=(
    "deploy/k8s/canvas-flink-jobmanager-deployment.yaml"
    "deploy/k8s/canvas-flink-taskmanager-deployment.yaml"
    "deploy/k8s/canvas-flink-job-submitter.yaml"
    "deploy/helm/canvas/values-prod.yaml"
  )
  for manifest in "${manifests[@]}"; do
    require_not_contains "${manifest}" "image: flink:"
    require_not_contains "${manifest}" "repository: flink"
  done
  log "PASS static production manifests do not use the bare Flink image"
}

require_static_runtime_image() {
  require_contains "deploy/k8s/canvas-flink-jobmanager-deployment.yaml" "image: ${PROD_IMAGE}"
  require_contains "deploy/k8s/canvas-flink-taskmanager-deployment.yaml" "image: ${PROD_IMAGE}"
  require_contains "deploy/k8s/canvas-flink-job-submitter.yaml" "image: ${PROD_IMAGE}"
  require_contains "deploy/helm/canvas/values-prod.yaml" "repository: registry.example.com/marketing-canvas/canvas-flink-jobs"
  require_contains "deploy/helm/canvas/values-prod.yaml" "tag: prod"
  log "PASS static and Helm production values point at ${PROD_IMAGE}"
}

require_static_submitter_jobs() {
  local submitter="deploy/k8s/canvas-flink-job-submitter.yaml"
  local job_count
  job_count="$(count_fixed "${submitter}" "kind: Job")"
  [[ "${job_count}" == "${#REQUIRED_PIPELINES[@]}" ]] \
    || fail "${submitter} must define ${#REQUIRED_PIPELINES[@]} submitter Jobs, found ${job_count}"

  for pipeline in "${REQUIRED_PIPELINES[@]}"; do
    require_contains "${submitter}" "canvas.chovy.org/pipeline-key: ${pipeline}"
    require_contains "${submitter}" "value: ${pipeline}"
    require_contains "${submitter}" "--pipeline-key=${pipeline}"
  done
  log "PASS static submitter defines all ${#REQUIRED_PIPELINES[@]} required pipeline jobs"
}

require_helm_submitter_jobs() {
  require_contains "deploy/helm/canvas/values.yaml" "pipelines:"
  require_contains "deploy/helm/canvas/templates/flink-job-submitter.yaml" "range .Values.flink.pipelines"
  require_contains "deploy/helm/canvas/templates/flink-job-submitter.yaml" "CANVAS_FLINK_JOB_PIPELINE_KEY"
  require_contains "deploy/helm/canvas/templates/flink-job-submitter.yaml" "--pipeline-key={{ . }}"
  for pipeline in "${REQUIRED_PIPELINES[@]}"; do
    require_contains "deploy/helm/canvas/values.yaml" "- ${pipeline}"
  done
  log "PASS Helm submitter renders from the required pipeline list"
}

require_secret_contract() {
  local secret="deploy/k8s/canvas-flink-secret.example.yaml"
  local values="deploy/helm/canvas/values.yaml"
  local submitter="deploy/k8s/canvas-flink-job-submitter.yaml"
  require_contains "${secret}" "name: canvas-flink-runtime"
  require_contains "${values}" "secretName: canvas-flink-runtime"
  require_contains "${submitter}" "name: canvas-flink-runtime"
  for key in "${REQUIRED_SECRET_KEYS[@]}"; do
    require_contains "${secret}" "${key}:"
    require_contains "${submitter}" "key: ${key}"
  done
  log "PASS runtime Secret key contract is present in example and static submitter"
}

require_cutover_gate_pipelines() {
  local service="backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessService.java"
  local runbook="docs/runbooks/flink-production-deployment.md"
  for pipeline in "${REQUIRED_PIPELINES[@]}"; do
    require_contains "${service}" "\"${pipeline}\""
    require_contains "${runbook}" "pipelineKey=${pipeline}"
  done
  require_contains "${service}" "DEFAULT_REQUIRED_PIPELINES"
  require_contains "${runbook}" "status=PASS"
  require_contains "${runbook}" "allowed=true"
  log "PASS cutover gate and runbook require all realtime warehouse pipelines"
}

require_prometheus_alerts() {
  local alerts="deploy/observability/prometheus/canvas-flink-alert-rules.yml"
  for alert in "${REQUIRED_ALERTS[@]}"; do
    require_contains "${alerts}" "${alert}"
  done
  if command -v promtool >/dev/null 2>&1; then
    promtool check rules "$(path "${alerts}")" >/dev/null
    log "PASS promtool check rules ${alerts}"
  else
    log "SKIP promtool check rules ${alerts}; promtool is not installed"
  fi
}

render_helm_chart_if_available() {
  if ! command -v helm >/dev/null 2>&1; then
    log "SKIP helm template deploy/helm/canvas; helm is not installed"
    return 0
  fi
  local rendered
  rendered="$(mktemp)"
  helm template canvas "$(path "deploy/helm/canvas")" \
    -f "$(path "deploy/helm/canvas/values-prod.yaml")" >"${rendered}"
  grep -Fq -- "${PROD_IMAGE}" "${rendered}" \
    || fail "helm template output does not contain ${PROD_IMAGE}"
  for pipeline in "${REQUIRED_PIPELINES[@]}"; do
    grep -Fq -- "--pipeline-key=${pipeline}" "${rendered}" \
      || fail "helm template output does not submit pipeline ${pipeline}"
  done
  rm -f "${rendered}"
  log "PASS helm template deploy/helm/canvas renders production Flink jobs"
}

main() {
  local required_files=(
    "deploy/helm/canvas/values.yaml"
    "deploy/helm/canvas/values-prod.yaml"
    "deploy/helm/canvas/templates/flink-job-submitter.yaml"
    "deploy/k8s/canvas-flink-jobmanager-deployment.yaml"
    "deploy/k8s/canvas-flink-taskmanager-deployment.yaml"
    "deploy/k8s/canvas-flink-job-submitter.yaml"
    "deploy/k8s/canvas-flink-secret.example.yaml"
    "deploy/observability/prometheus/canvas-flink-alert-rules.yml"
    "backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessService.java"
    "docs/runbooks/flink-production-deployment.md"
  )
  for file in "${required_files[@]}"; do
    require_file "${file}"
  done

  reject_bare_flink_image
  require_static_runtime_image
  require_static_submitter_jobs
  require_helm_submitter_jobs
  require_secret_contract
  require_cutover_gate_pipelines
  require_prometheus_alerts
  render_helm_chart_if_available

  log "PASS production Flink deployment preflight completed"
}

main "$@"
