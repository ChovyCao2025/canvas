# OSG-W07C Recovery Note

dispatch id: dispatch-OSG-W07C-official-coupon-plugin-20260611-010234
task id: OSG-W07C
date: 2026-06-11
coordinator: Codex

## Recovery Classification

classification: CONTINUE

The current ledger and `dispatch-state.json` agree that there are no active
dispatches and that OSG-W07B closed DONE after final verification. The next
eligible G10-dependent official plugin worker with clean, non-overlapping scope
is OSG-W07C official coupon plugin.

## Workspace Checks

- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `git status --short` shows the expected dirty multi-dispatch worktree and no
  existing OSG-W07C coupon reserved-path changes.
- `git worktree list` shows the main worktree plus unrelated prunable
  worktrees.
- `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` exists.
- OSG-W07C reserved paths were checked and are clean:
  - `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/**`
  - `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/**`
  - `docs/open-source/plugins/official/coupon.md`

## Contracts Read

- `docs/program-coordination/subagent-worker-packets.md` OSG-W07A-W07F shared
  packet.
- `docs/program-coordination/evidence/dispatch-OSG-C07-plugin-registry-decision-20260610-142556/worker-return.md`
- `docs/open-source-growth/contracts/plugin-manifest-v1.md`
- `docs/open-source-growth/contracts/node-handler-contract.md`
- Existing official webhook and message plugin handlers/tests/docs.

## Pre-Dispatch Verification

- `node --test tools/program-coordination/*.test.mjs` passed: 20 tests.
- `node --test tools/open-source-growth/guardrail-verifier.test.mjs && node tools/open-source-growth/guardrail-verifier.mjs`
  passed: 11 tests and verifier ok.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
  passed with the known `RiskRuleValidator` TypeCompatibility advisory only.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest='*Plugin*Test'`
  passed: 12 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest,CanvasDslValidatorTest,CanvasDslMapperTest`
  passed: 6 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn install -pl canvas-context-canvas -DskipTests && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`
  passed: local canvas artifact refreshed and 3 web tests passed.
- Scoped `git diff --check` over the OSG-W07C reserved paths and coordinator
  state files passed.

## Reservation

OSG-W07C is safe to reserve with exact coupon plugin package, coupon plugin
tests, and coupon docs file scope. Before moving to RUNNING, the coordinator
must spawn a real worker and record the actual worker id/nickname.
