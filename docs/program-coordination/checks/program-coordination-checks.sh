#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

failures=0

fail() {
  echo "FAIL: $1"
  failures=$((failures + 1))
}

require_file() {
  local file="$1"
  if [ ! -f "$file" ]; then
    fail "$file is required"
  fi
}

require_contains() {
  local file="$1"
  local text="$2"
  if [ ! -f "$file" ]; then
    fail "$file is required before checking for $text"
    return
  fi
  if ! grep -Fq "$text" "$file"; then
    fail "$file must contain: $text"
  fi
}

section_contains() {
  local file="$1"
  local start_pattern="$2"
  local end_pattern="$3"
  local text="$4"

  if [ ! -f "$file" ]; then
    fail "$file is required before checking section $start_pattern"
    return
  fi

  local section
  section="$(awk -v start="$start_pattern" -v end="$end_pattern" '
    $0 ~ start { in_section = 1 }
    in_section && $0 ~ end && $0 !~ start { exit }
    in_section { print }
  ' "$file")"

  if [ -z "$section" ]; then
    fail "$file must contain section matching: $start_pattern"
    return
  fi

  if ! grep -Fq "$text" <<<"$section"; then
    fail "$file section $start_pattern must contain: $text"
  fi
}

require_worker_packet_fields() {
  local task="$1"
  local title="$2"
  local section_pattern="^### ${task}:"
  local next_pattern="^### "
  local file="docs/program-coordination/subagent-worker-packets.md"

  section_contains "$file" "$section_pattern" "$next_pattern" "$title"
  section_contains "$file" "$section_pattern" "$next_pattern" "Program: DDD modular rewrite"
  section_contains "$file" "$section_pattern" "$next_pattern" "Task id: ${task}"
  section_contains "$file" "$section_pattern" "$next_pattern" "Readiness gate:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Target backend state: DDD_FINAL_MODULE"
  section_contains "$file" "$section_pattern" "$next_pattern" "Allowed write scope:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Inventory rows required:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Allowed module POM edits:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Forbidden write scope:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Read scope:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Contracts to read:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Verification commands:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Can run with:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Must not run with:"
  section_contains "$file" "$section_pattern" "$next_pattern" "Rollback path:"
}

require_coordinator_task_pack_fields() {
  local file="$1"
  local task="$2"
  local readiness="$3"
  local target_state="$4"
  local response_heading="$5"

  require_contains "$file" "**Program:** DDD modular rewrite"
  require_contains "$file" "**Task id:** ${task}"
  require_contains "$file" "**Readiness level:** ${readiness}"
  require_contains "$file" "**Target backend state:** ${target_state}"
  require_contains "$file" "## Allowed Write Scope"
  require_contains "$file" "## Forbidden Changes"
  require_contains "$file" "## Run-With Constraints"
  require_contains "$file" "Can run with:"
  require_contains "$file" "Must not run with:"
  require_contains "$file" "## Verification"
  require_contains "$file" "## Rollback"
  require_contains "$file" "## ${response_heading}"
  require_contains "$file" "status:"
  require_contains "$file" "tests run:"
  require_contains "$file" "guardrail checks:"
}

require_ddd_worker_task_pack_fields() {
  local file="$1"
  require_contains "$file" "## Worker Response"
  require_contains "$file" "status:"
  require_contains "$file" "task id:"
  require_contains "$file" "dispatch id:"
  require_contains "$file" "base commit:"
  require_contains "$file" "head commit:"
  require_contains "$file" "assigned task pack:"
  require_contains "$file" "contracts changed:"
  require_contains "$file" "verification output summary/path:"
  require_contains "$file" "evidence artifact paths:"
  require_contains "$file" "compatibility evidence:"
  require_contains "$file" "coordinator actions needed:"
  require_contains "$file" "ledger update:"
  require_contains "$file" "rollback path:"
  require_contains "$file" "Do not return a shorter"
}

validate_progress_ledger_runtime_consistency() {
  local file="docs/program-coordination/progress-ledger.md"
  if [ ! -f "$file" ]; then
    fail "$file is required before checking runtime consistency"
    return
  fi

  local active_section
  active_section="$(awk '
    /^## Active Dispatch Registry$/ { in_section = 1; next }
    /^Latest closed dispatch:$/ { exit }
    in_section { print }
  ' "$file")"

  local worker_section
  worker_section="$(awk '
    /^## Worker Board$/ { in_section = 1; next }
    /^## Reviewer Board$/ { exit }
    in_section { print }
  ' "$file")"

  if grep -Eq '^[[:space:]]*none[[:space:]]*$' <<<"$active_section" \
    && grep -Eq '\|[^|]+\|[[:space:]]*(RESERVED|RUNNING|RETURNED|REVIEWING|NEEDS_CONTEXT|BLOCKED)[[:space:]]*\|' <<<"$worker_section"; then
    fail "progress-ledger.md Active Dispatch Registry is none but Worker Board contains active worker status"
  fi
}

validate_progress_ledger_current_snapshot_freshness() {
  local file="docs/program-coordination/progress-ledger.md"
  local state_file="docs/program-coordination/dispatch-state.json"
  if [ ! -f "$file" ]; then
    fail "$file is required before checking current snapshot freshness"
    return
  fi
  if [ ! -f "$state_file" ]; then
    fail "$state_file is required before checking current snapshot freshness"
    return
  fi

  local current_snapshot
  current_snapshot="$(awk '
    /^## Current Snapshot$/ { in_section = 1; next }
    /^## Active Dispatch Registry$/ { exit }
    in_section { print }
  ' "$file")"

  if [ -z "$current_snapshot" ]; then
    fail "progress-ledger.md must contain a non-empty Current Snapshot section"
    return
  fi

  local preflight_ready="false"
  if [ -f "tools/program-coordination/cutover-compatibility-preflight.mjs" ]; then
    local preflight_json
    if preflight_json="$(node tools/program-coordination/cutover-compatibility-preflight.mjs . --json 2>/dev/null)"; then
      if node -e '
        const chunks = []
        process.stdin.on("data", chunk => chunks.push(chunk))
        process.stdin.on("end", () => {
          const report = JSON.parse(Buffer.concat(chunks).toString("utf8"))
          process.exit(report.cutoverReady === true && Array.isArray(report.blockers) && report.blockers.length === 0 ? 0 : 1)
        })
      ' <<<"$preflight_json"; then
        preflight_ready="true"
      fi
    fi
  fi

  local state_ready="false"
  if node -e '
    const fs = require("fs")
    const state = JSON.parse(fs.readFileSync("docs/program-coordination/dispatch-state.json", "utf8"))
    const text = [
      state.readiness && state.readiness.gate,
      state.readiness && state.readiness.backendTarget,
      state.lastEvent,
      ...(state.lastVerifiedEvidence || []).map(entry => `${entry.command || ""} ${entry.result || ""}`)
    ].filter(Boolean).join("\n")
    process.exit(/cutoverReady[:= ]+true|cutover preflight.*ready/i.test(text) && /blockers (empty|\\[\\]|0)|no blockers/i.test(text) ? 0 : 1)
  '; then
    state_ready="true"
  fi

  if [ "$preflight_ready" = "true" ] || [ "$state_ready" = "true" ]; then
    if grep -Eiq 'cutover remains blocked|final cutover remains blocked|global cutover remains blocked|route parity.*blocked|G12 blockers' <<<"$current_snapshot"; then
      fail "Current Snapshot is stale: cutover-ready evidence exists but snapshot still says cutover is blocked"
    fi
    if grep -Eiq 'next preflight top gap|next top gap|next clear preflight route batch|route:/[^`| ]+' <<<"$current_snapshot"; then
      fail "Current Snapshot is stale: cutover-ready evidence exists but snapshot still names a next preflight route gap"
    fi

    local latest_closed_dispatch
    latest_closed_dispatch="$(awk '
      /^Latest closed dispatch:$/ { in_section = 1 }
      in_section && (/^Previous closed dispatch:$/ || /^## /) { exit }
      in_section { print }
    ' "$file")"

    if [ -n "$latest_closed_dispatch" ]; then
      if grep -Eiq 'cutover remains blocked|final cutover remains blocked|global cutover remains blocked|route parity.*blocked|G12 blockers' <<<"$latest_closed_dispatch"; then
        fail "Latest closed dispatch is stale: cutover-ready evidence exists but closed dispatch still says cutover is blocked"
      fi
      if grep -Eiq 'next preflight top gap|next top gap|next clear preflight route batch|route:/[^`| ]+' <<<"$latest_closed_dispatch"; then
        fail "Latest closed dispatch is stale: cutover-ready evidence exists but closed dispatch still names a next preflight route gap"
      fi
    fi
  fi
}

for file in \
  docs/program-coordination/README.md \
  docs/program-coordination/progress-ledger.md \
  docs/program-coordination/dispatch-state.json \
  docs/program-coordination/evidence/README.md \
  docs/program-coordination/collaboration-and-recovery-protocol.md \
  docs/program-coordination/backup-and-rollback-runbook.md \
  docs/program-coordination/ddd-open-source-growth-integration.md \
  docs/program-coordination/combined-roadmap.md \
  docs/program-coordination/conflict-matrix.md \
  docs/program-coordination/execution-sequencing.md \
  docs/program-coordination/max-parallel-subagent-execution-plan.md \
  docs/program-coordination/subagent-worker-packets.md \
  docs/program-coordination/execution-readiness-audit.md \
  docs/program-coordination/gate-verification-matrix.md \
  docs/program-coordination/isolated-worktree-protocol.md
do
  require_file "$file"
done

require_file docs/ddd-rewrite/inventory/check-inventory-readiness.sh
require_file tools/program-coordination/check-dispatch-state.mjs
require_file tools/program-coordination/check-dispatch-state.test.mjs
require_file tools/program-coordination/generate-worker-prompt.mjs
require_file tools/program-coordination/generate-worker-prompt.test.mjs

require_contains docs/program-coordination/README.md "execution-readiness-audit.md"
require_contains docs/program-coordination/README.md "gate-verification-matrix.md"
require_contains docs/program-coordination/README.md "isolated-worktree-protocol.md"
require_contains docs/program-coordination/README.md "max-parallel-subagent-execution-plan.md"
require_contains docs/program-coordination/README.md "subagent-worker-packets.md"
require_contains docs/program-coordination/README.md "progress-ledger.md"
require_contains docs/program-coordination/README.md "dispatch-state.json"
require_contains docs/program-coordination/README.md "invalid parallel groups"
require_contains docs/program-coordination/README.md "tools/program-coordination/check-dispatch-state.mjs"
require_contains docs/program-coordination/README.md "tools/program-coordination/generate-worker-prompt.mjs"
require_contains docs/program-coordination/README.md "collaboration-and-recovery-protocol.md"
require_contains docs/program-coordination/README.md "backup-and-rollback-runbook.md"
require_contains docs/program-coordination/evidence/README.md "pre-rewrite-backup-manifest.md"
require_contains docs/program-coordination/evidence/README.md "Do not store database dumps"
require_contains docs/program-coordination/progress-ledger.md "# Program Progress Ledger"
require_contains docs/program-coordination/progress-ledger.md "The coordinator is the single writer for this file."
require_contains docs/program-coordination/progress-ledger.md "dispatch-state.json"
require_contains docs/program-coordination/progress-ledger.md "backup-and-rollback-runbook.md"
require_contains docs/program-coordination/progress-ledger.md "Pre-rewrite backup manifest"
require_contains docs/program-coordination/progress-ledger.md "machine-readable"
require_contains docs/program-coordination/progress-ledger.md "node tools/program-coordination/check-dispatch-state.mjs ."
require_contains docs/program-coordination/progress-ledger.md "node --test tools/program-coordination/*.test.mjs"
require_contains docs/program-coordination/progress-ledger.md "Reopen Checklist"
require_contains docs/program-coordination/progress-ledger.md "git status --short"
require_contains docs/program-coordination/progress-ledger.md "git worktree list"
require_contains docs/program-coordination/progress-ledger.md "Compare active dispatch registry rows with actual branches, worktrees, and changed paths"
require_contains docs/program-coordination/progress-ledger.md "Record the recovery audit"
require_contains docs/program-coordination/progress-ledger.md "fallback reason:"
require_contains docs/program-coordination/progress-ledger.md "Current Snapshot"
require_contains docs/program-coordination/progress-ledger.md "Active Dispatch Registry"
require_contains docs/program-coordination/progress-ledger.md "Last Verified Evidence"
require_contains docs/program-coordination/progress-ledger.md "Worker Board"
require_contains docs/program-coordination/progress-ledger.md "Reviewer Board"
require_contains docs/program-coordination/progress-ledger.md "Recovery Audit"
require_contains docs/program-coordination/progress-ledger.md "Worker Result Recording Template"
require_contains docs/program-coordination/progress-ledger.md "Stop Conditions"
require_contains docs/program-coordination/progress-ledger.md "dispatch id:"
require_contains docs/program-coordination/progress-ledger.md "base commit:"
require_contains docs/program-coordination/progress-ledger.md "head commit:"
require_contains docs/program-coordination/progress-ledger.md "base SHA:"
require_contains docs/program-coordination/progress-ledger.md "exact reserved files:"
require_contains docs/program-coordination/progress-ledger.md "last command/result:"
require_contains docs/program-coordination/progress-ledger.md "verification output summary/path:"
require_contains docs/program-coordination/progress-ledger.md "evidence artifact paths:"
require_contains docs/program-coordination/progress-ledger.md "ledger update:"
require_contains docs/program-coordination/progress-ledger.md "DDD-W01"
require_contains docs/program-coordination/progress-ledger.md "OSG-W14"
require_contains docs/program-coordination/progress-ledger.md "OSG-W07A official webhook plugin"
require_contains docs/program-coordination/progress-ledger.md "OSG-W07F official risk-check plugin"
require_contains docs/program-coordination/dispatch-state.json "\"parallelGroups\""
require_contains docs/program-coordination/dispatch-state.json "\"P1-immediate-shell\""
require_contains docs/program-coordination/dispatch-state.json "\"P7-plugin-burst\""
require_contains docs/program-coordination/dispatch-state.json "\"OSG-W07F\""
require_contains docs/program-coordination/dispatch-state.json "\"docs/open-source/playground.md\""
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "# Collaboration And Recovery Protocol"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "dispatch-state.json"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "backup-and-rollback-runbook.md"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "pre-rewrite-backup-manifest.md"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "node tools/program-coordination/check-dispatch-state.mjs ."
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "Active Dispatch Registry"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "Worker State Machine"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "Worker Return Contract"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "Reviewer Contract"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "Breakpoint Recovery"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "Wave Closure Checklist"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "dispatch id:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "base commit:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "head commit:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "verification output summary/path:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "evidence artifact paths:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "exact reserved files:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "\`RUNNING\` means the handoff was actually sent"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "fallback reason:"
require_contains docs/program-coordination/collaboration-and-recovery-protocol.md "must have a matching active dispatch row"
require_contains docs/program-coordination/execution-sequencing.md "CURRENT_ENGINE_BRIDGE or DDD_FINAL_MODULE or DOCS_ONLY"
require_contains docs/program-coordination/execution-sequencing.md "Gate E: Wave Closure"
require_contains docs/program-coordination/backup-and-rollback-runbook.md "# Backup And Rollback Runbook"
require_contains docs/program-coordination/backup-and-rollback-runbook.md "No code-writing dispatch may start"
require_contains docs/program-coordination/backup-and-rollback-runbook.md "docs/program-coordination/evidence/pre-rewrite-backup-manifest.md"
require_contains docs/program-coordination/backup-and-rollback-runbook.md "git bundle create"
require_contains docs/program-coordination/backup-and-rollback-runbook.md "Per-Worker Rollback Contract"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "Shared workspace mode"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "backup-and-rollback-runbook.md"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "G0B"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "Dispatch id:"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "base commit:"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "head commit:"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "verification output summary/path:"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "evidence artifact paths:"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "ledger update:"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "DDD-C00"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "OSG-W01"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "OSG-W05A"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "OSG-C05B"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "dispatch-state.json"
require_contains docs/program-coordination/max-parallel-subagent-execution-plan.md "parallelGroups"
require_contains docs/program-coordination/subagent-worker-packets.md "DDD-W01: Platform Worker"
require_contains docs/program-coordination/subagent-worker-packets.md "DDD-W08: Execution Worker"
require_contains docs/program-coordination/subagent-worker-packets.md "Inventory rows required:"
require_contains docs/program-coordination/subagent-worker-packets.md "Allowed module POM edits:"
require_contains docs/program-coordination/subagent-worker-packets.md "backend/pom.xml is coordinator-owned"
require_contains docs/program-coordination/subagent-worker-packets.md "Globs and package names are not ownership proof"
require_contains docs/program-coordination/subagent-worker-packets.md "progress-ledger.md"
require_contains docs/program-coordination/subagent-worker-packets.md "the coordinator records accepted status in the ledger"
require_contains docs/program-coordination/subagent-worker-packets.md "dispatch id:"
require_contains docs/program-coordination/subagent-worker-packets.md "base commit:"
require_contains docs/program-coordination/subagent-worker-packets.md "verification output summary/path:"
require_contains docs/program-coordination/subagent-worker-packets.md "evidence artifact paths:"
require_contains docs/program-coordination/subagent-worker-packets.md "Bridge Declaration"
require_contains docs/program-coordination/subagent-worker-packets.md "exact old service/API"
require_contains docs/program-coordination/subagent-worker-packets.md "final DDD owner module"
require_contains docs/program-coordination/subagent-worker-packets.md "removal gate"
require_contains docs/program-coordination/subagent-worker-packets.md "If the bridge declaration is absent or incomplete"
require_contains docs/program-coordination/subagent-worker-packets.md "OSG-W07A Through OSG-W07F"
require_contains docs/program-coordination/subagent-worker-packets.md "backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**"
require_contains docs/program-coordination/subagent-worker-packets.md "TemplateImportServiceTest.java"
require_contains docs/program-coordination/subagent-worker-packets.md "CanvasDslControllerCompatibilityTest.java"
require_contains docs/program-coordination/subagent-worker-packets.md "TraceExplanationFacadeTest.java"
require_contains docs/program-coordination/subagent-worker-packets.md "OSG-W14: Playground Flow"
require_worker_packet_fields "DDD-W01" "Platform Worker"
require_worker_packet_fields "DDD-W02" "Risk Worker"
require_worker_packet_fields "DDD-W03" "Marketing Worker"
require_worker_packet_fields "DDD-W04" "CDP Worker"
require_worker_packet_fields "DDD-W05" "BI Worker"
require_worker_packet_fields "DDD-W06" "Conversation Worker"
require_worker_packet_fields "DDD-W07" "Canvas Worker"
require_worker_packet_fields "DDD-W08" "Execution Worker"
require_contains docs/program-coordination/execution-readiness-audit.md "R0: Documentation Ready"
require_contains docs/program-coordination/execution-readiness-audit.md "R1: Backup And Baseline Captured"
require_contains docs/program-coordination/execution-readiness-audit.md "R6: Cutover Ready"
require_contains docs/program-coordination/execution-readiness-audit.md "record the actual worker id/nickname"
require_contains docs/program-coordination/execution-readiness-audit.md "fallback reason:"
require_contains docs/program-coordination/gate-verification-matrix.md "PublishedCanvasDefinition.java"
require_contains docs/program-coordination/gate-verification-matrix.md "PublishedCanvasDefinitionProvider.java"
require_contains docs/program-coordination/gate-verification-matrix.md "node tools/program-coordination/check-dispatch-state.mjs ."
require_contains docs/program-coordination/gate-verification-matrix.md "G0B: Backup and rollback checkpoint captured"
require_contains docs/program-coordination/gate-verification-matrix.md "pre-rewrite-backup-manifest.md"
require_contains docs/program-coordination/isolated-worktree-protocol.md "backup-and-rollback-runbook.md"
require_contains docs/program-coordination/isolated-worktree-protocol.md "G0B"
require_contains docs/program-coordination/gate-verification-matrix.md "node --test tools/program-coordination/*.test.mjs"
require_contains docs/program-coordination/gate-verification-matrix.md "PublishedCanvasNodeDefinition.java"
require_contains docs/program-coordination/gate-verification-matrix.md "PublishedCanvasEdgeDefinition.java"
require_contains docs/program-coordination/gate-verification-matrix.md "ExecutionPublicationPort.java"
require_contains docs/program-coordination/gate-verification-matrix.md "CanvasPublishApplicationServiceTest"
require_contains docs/program-coordination/gate-verification-matrix.md "ExecutionPublicationApplicationServiceTest"
require_contains docs/program-coordination/gate-verification-matrix.md "CanvasExecutionFacade.java"
require_contains docs/program-coordination/gate-verification-matrix.md "NodeMetadataView.java"
require_contains docs/program-coordination/gate-verification-matrix.md "PluginEnablementView.java"
require_contains docs/program-coordination/gate-verification-matrix.md "ExecutionDryRunFacade.java"
require_contains docs/program-coordination/gate-verification-matrix.md "TemplateValidationPort.java"
require_contains docs/program-coordination/gate-verification-matrix.md "AiJourneyDraftProposal.java"
require_contains docs/program-coordination/gate-verification-matrix.md "NodeMetadataContractTest"
require_contains docs/program-coordination/gate-verification-matrix.md "Public extension and API stability gate"
require_contains docs/program-coordination/gate-verification-matrix.md "mvn test -pl canvas-context-execution -Dtest='*Plugin*Test'"
require_contains docs/program-coordination/gate-verification-matrix.md "CanvasDslControllerCompatibilityTest"
require_contains docs/program-coordination/gate-verification-matrix.md "TraceExplanationFacadeTest"
require_contains docs/program-coordination/gate-verification-matrix.md "G12: Final cutover"
require_contains docs/program-coordination/gate-verification-matrix.md "Coordination Closure Gates"
require_contains docs/program-coordination/isolated-worktree-protocol.md "git worktree add"
require_contains docs/program-coordination/isolated-worktree-protocol.md "active dispatch registry row"
require_contains docs/program-coordination/isolated-worktree-protocol.md "dispatch id:"
require_contains docs/program-coordination/isolated-worktree-protocol.md "verification output summary/path:"
require_contains docs/program-coordination/evidence/README.md "commands.txt"
require_contains docs/program-coordination/evidence/README.md "worker-return.txt"
require_contains docs/program-coordination/evidence/README.md "reviewer-return.txt"
require_contains docs/program-coordination/evidence/README.md "rollback.txt"
validate_progress_ledger_runtime_consistency
validate_progress_ledger_current_snapshot_freshness

if ! node tools/program-coordination/check-dispatch-state.mjs .; then
  fail "dispatch-state.json failed machine validation"
fi

require_coordinator_task_pack_fields docs/ddd-rewrite/task-packs/00-coordinator-foundation.md "DDD-C00" "R1 foundation" "DDD_FINAL_MODULE skeleton only" "Coordinator Response"
require_coordinator_task_pack_fields docs/ddd-rewrite/task-packs/06a-coordinator-canvas-execution-contract-freeze.md "DDD-C07" "R4 candidate" "DDD_FINAL_MODULE" "Coordinator Response"
require_coordinator_task_pack_fields docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md "DDD-C09" "R6 cutover" "DDD_FINAL_MODULE" "Coordinator Response"
for file in \
  docs/ddd-rewrite/task-packs/01-worker-platform.md \
  docs/ddd-rewrite/task-packs/02-worker-risk.md \
  docs/ddd-rewrite/task-packs/03-worker-marketing.md \
  docs/ddd-rewrite/task-packs/04-worker-cdp.md \
  docs/ddd-rewrite/task-packs/05-worker-bi.md \
  docs/ddd-rewrite/task-packs/06-worker-conversation.md \
  docs/ddd-rewrite/task-packs/07-worker-canvas.md \
  docs/ddd-rewrite/task-packs/08-worker-execution.md
do
  require_ddd_worker_task_pack_fields "$file"
done
require_contains docs/ddd-rewrite/guardrails/README.md "subagent-worker-packets.md"
require_contains docs/ddd-rewrite/guardrails/README.md "gate-verification-matrix.md"
require_contains docs/ddd-rewrite/2026-06-08-ddd-modular-rewrite-spec.md "Dispatch Scope Clarification"
require_contains docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md "PluginEnablementView"
require_contains docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md "AiJourneyDraftBoundaryContractTest"
require_contains docs/ddd-rewrite/task-packs/06a-coordinator-canvas-execution-contract-freeze.md "PublishedCanvasDefinitionProvider"
require_contains docs/ddd-rewrite/task-packs/06a-coordinator-canvas-execution-contract-freeze.md "CanvasPublishApplicationServiceTest"
require_contains docs/ddd-rewrite/task-packs/06a-coordinator-canvas-execution-contract-freeze.md "ExecutionPublicationApplicationServiceTest"
require_contains docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh "check_package_prefix"
require_contains docs/ddd-rewrite/inventory/README.md "142 backend controllers"
require_contains docs/ddd-rewrite/inventory/README.md "284 persistence data objects"
require_contains docs/ddd-rewrite/inventory/README.md "283 MyBatis mappers"
require_contains docs/ddd-rewrite/inventory/README.md "731 backend tests"
require_contains docs/ddd-rewrite/inventory/check-inventory-readiness.sh "old class:"
require_contains docs/ddd-rewrite/inventory/check-inventory-readiness.sh "target module:"

if rg -n "stable HTTP APIs" docs/program-coordination docs/open-source-growth -g "*.md"; then
  fail "coordination docs contain undefined stable HTTP APIs gate"
fi

if rg -n "  backend/canvas-.*pom.xml" docs/program-coordination/subagent-worker-packets.md; then
  fail "subagent worker packets contain bare module POM edit permission"
fi

if rg -n "TBD|TODO|implement later|fill in details|Similar to|similar to|待补|待定" docs/program-coordination -g "*.md"; then
  fail "docs/program-coordination contains placeholder text"
fi

if rg -n "matching test package|matching test|backend tests named|tests named by worker packet" docs/program-coordination -g "*.md"; then
  fail "docs/program-coordination contains ambiguous test ownership text"
fi

if rg -n '"docs/open-source/demo\.md"' docs/program-coordination/dispatch-state.json; then
  fail "dispatch-state.json contains stale OSG-W02 demo.md path; use docs/open-source/playground.md"
fi

if rg -n "CanvasPluginRegistry" docs/program-coordination docs/open-source-growth docs/ddd-rewrite -g "*.md" \
  | grep -v "不要直接新建平行的" \
  | grep -v "新建第二套插件注册中心" \
  | grep -v "forbidden" \
  | grep -v "prohibit" \
  | grep -v "Rejected" \
  | grep -v "must not" \
  | grep -v "second registry"; then
  fail "CanvasPluginRegistry appears outside a forbidden/rejected context"
fi

if [ "$failures" -gt 0 ]; then
  echo "Program coordination checks failed: $failures"
  exit 1
fi

echo "Program coordination checks passed."
