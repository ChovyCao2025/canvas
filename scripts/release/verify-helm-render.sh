#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHART_DIR="${CANVAS_HELM_CHART_DIR:-$ROOT_DIR/deploy/helm/canvas}"
RELEASE_NAME="${CANVAS_HELM_RELEASE_NAME:-canvas}"
NAMESPACE="${CANVAS_HELM_NAMESPACE:-canvas}"
OUTPUT_DIR="${CANVAS_HELM_RENDER_OUTPUT_DIR:-${TMPDIR:-/tmp}/canvas-helm-render}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

command -v helm >/dev/null 2>&1 || fail "helm is required"
[[ -f "$CHART_DIR/Chart.yaml" ]] || fail "missing Helm chart: $CHART_DIR/Chart.yaml"

mkdir -p "$OUTPUT_DIR"

helm lint "$CHART_DIR"

render_variant() {
  local name="$1"
  shift
  local output_file="$OUTPUT_DIR/${name}.yaml"

  helm template "$RELEASE_NAME" "$CHART_DIR" --namespace "$NAMESPACE" "$@" > "$output_file"

  grep -Eq 'image: "registry\.example\.com/marketing-canvas/canvas-boot:[^"]+"' "$output_file" \
    || fail "$name render does not use the canvas-boot backend image"
  ! grep -Eq 'image: "registry\.example\.com/marketing-canvas/canvas-boot:latest"' "$output_file" \
    || fail "$name render uses mutable latest tag for the canvas-boot backend image"
  grep -Eq '^  name: canvas-engine$' "$output_file" \
    || fail "$name render does not preserve the stable canvas-engine backend resource name"
  grep -Eq '^[[:space:]]+name: canvas-engine-runtime$' "$output_file" \
    || fail "$name render does not preserve the stable canvas-engine-runtime Secret reference"

  echo "rendered $name: $output_file"
}

render_variant base
render_variant staging -f "$CHART_DIR/values-staging.yaml"
render_variant prod -f "$CHART_DIR/values-prod.yaml"

grep -q 'canvas-flink-jobmanager' "$OUTPUT_DIR/staging.yaml" \
  || fail "staging render does not include the Flink JobManager when flink.enabled=true"
grep -q 'canvas-flink-jobmanager' "$OUTPUT_DIR/prod.yaml" \
  || fail "prod render does not include the Flink JobManager when flink.enabled=true"

echo "Helm render verification passed: $OUTPUT_DIR"
