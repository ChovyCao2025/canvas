# OSG-W07D Recovery Note

dispatch id: dispatch-OSG-W07D-official-approval-plugin-20260611-013849
task id: OSG-W07D
date: 2026-06-11
coordinator: Codex

## Recovery Classification

classification: CONTINUE

The current ledger and `dispatch-state.json` agree that OSG-W07C is closed DONE
and that there are no active dispatches. The next eligible G10-dependent
official plugin worker with clean, non-overlapping scope is OSG-W07D official
approval plugin.

## Workspace Checks

- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  OSG-W07C closure.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed after OSG-W07C closure.
- `git status --short` shows the expected dirty multi-dispatch worktree and no
  existing OSG-W07D approval reserved-path changes.
- `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` exists.
- OSG-W07D reserved paths were checked and are clean:
  - `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/approval/**`
  - `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/approval/**`
  - `docs/open-source/plugins/official/approval.md`

## Contracts And Conventions

- OSG-W07A-W07F shared worker packet grants exact approval plugin scope.
- OSG-C07 decision keeps registry metadata, manifest validation, permissions,
  persistence, and enablement in `canvas-platform`; execution owns handler
  binding through `NodeHandlerRegistry`.
- `plugin-manifest-v1.md` includes `approval:create` in the permission
  vocabulary.
- Current template/catalog convention uses `approval.request` and
  `approvalCode`; examples include `coupon-approval-release` and
  `ai-copy-review-publish`.

## Pre-Dispatch Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest='OfficialCouponPluginTest,*Plugin*Test'`
  passed after OSG-W07C closure: 17 tests.
- `node tools/open-source-growth/guardrail-verifier.mjs` passed after OSG-W07C
  closure.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed after OSG-W07C closure.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` passed
  after OSG-W07C closure with the known `RiskRuleValidator`
  TypeCompatibility advisory only.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  OSG-W07C closure.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest,CanvasDslValidatorTest,CanvasDslMapperTest`
  passed: 6 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn install -pl canvas-context-canvas -DskipTests && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`
  passed: local canvas artifact refreshed and 3 web tests passed.

## Reservation

OSG-W07D is safe to reserve with exact approval plugin package, approval plugin
tests, and approval docs file scope. Before moving to RUNNING, the coordinator
must spawn a real worker and record the actual worker id/nickname.
