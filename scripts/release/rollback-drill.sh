#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false
EXECUTE_DB_RESTORE=false

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/release/rollback-drill.sh [--dry-run] [--execute-db-restore]

Rehearses or executes the application rollback path. Database restore remains a
separate decision point and only runs when --execute-db-restore is passed with
CANVAS_DB_RESTORE_COMMAND set.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --execute-db-restore)
      EXECUTE_DB_RESTORE=true
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

NAMESPACE="${CANVAS_K8S_NAMESPACE:-canvas}"
DEPLOYMENT="${CANVAS_K8S_DEPLOYMENT:-canvas-engine}"
EVIDENCE_DIR="${CANVAS_ROLLBACK_EVIDENCE_DIR:-docs/architecture/evidence/release-drills}"
REVISION_ARG=()

if [[ -n "${CANVAS_ROLLBACK_TO_REVISION:-}" ]]; then
  REVISION_ARG=(--to-revision="$CANVAS_ROLLBACK_TO_REVISION")
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "rollback drill dry-run passed"
  echo "kubectl -n $NAMESPACE rollout history deployment/$DEPLOYMENT"
  echo "kubectl -n $NAMESPACE rollout undo deployment/$DEPLOYMENT ${REVISION_ARG[*]:-}"
  echo "kubectl -n $NAMESPACE rollout status deployment/$DEPLOYMENT"
  echo "evidence_dir: $EVIDENCE_DIR"
  echo "database_restore_decision: requires rollback owner approval"
  exit 0
fi

command -v kubectl >/dev/null 2>&1 || fail "kubectl is required"
mkdir -p "$EVIDENCE_DIR"

timestamp="$(date +%Y%m%d-%H%M%S)"
evidence_file="$EVIDENCE_DIR/rollback-$timestamp.log"

{
  echo "rollback_started_at: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "namespace: $NAMESPACE"
  echo "deployment: $DEPLOYMENT"
  echo "rollback_owner: ${CANVAS_ROLLBACK_OWNER:-Runtime lead}"
  echo
  echo "## Rollout History"
  kubectl -n "$NAMESPACE" rollout history "deployment/$DEPLOYMENT"
  echo
  echo "## Rollout Undo"
  kubectl -n "$NAMESPACE" rollout undo "deployment/$DEPLOYMENT" "${REVISION_ARG[@]}"
  echo
  echo "## Rollout Status"
  kubectl -n "$NAMESPACE" rollout status "deployment/$DEPLOYMENT"
  echo
  echo "## Database Restore Decision"
  if [[ "$EXECUTE_DB_RESTORE" == "true" ]]; then
    [[ -n "${CANVAS_DB_RESTORE_COMMAND:-}" ]] || fail "CANVAS_DB_RESTORE_COMMAND is required with --execute-db-restore"
    echo "database_restore: executing approved restore command"
    bash -lc "$CANVAS_DB_RESTORE_COMMAND"
  else
    echo "database_restore: not executed"
    echo "decision: capture owner approval before running with --execute-db-restore"
  fi
  echo
  echo "rollback_completed_at: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
} | tee "$evidence_file"

echo "rollback drill completed: $evidence_file"
