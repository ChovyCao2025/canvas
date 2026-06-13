# DDD-C09C Spec Review

review status: PASS_WITH_CONCERNS
reviewer: multi_agent_v1-explorer Curie 019eb7a0-4012-7743-b996-44fe851f3239
review id: review-DDD-C09C-spec-20260612

## Review Scope

Read-only compliance review for DDD-C09C only.

## Files Reviewed

- `docs/program-coordination/subagent-worker-packets.md`
- `docs/ddd-rewrite/contract-tests/compatibility-test-plan.md`
- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09C-marketing-api-compat-20260612-003650/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09C-marketing-api-compat-20260612-003650/worker-return.md`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/**`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingCampaignApplicationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingCampaignController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingCampaignControllerTest.java`

## Requirements Checked

- Adds requested class/file: yes.
- Covers create/list/link/list-links/readiness/unlink: yes, 6 focused tests.
- Uses DDD-final marketing facade/API or test-local adapter: yes.
- Avoids production edits/old-engine imports in the reviewed file: yes.
- Avoids unrelated placeholder compatibility tests: yes.
- Preflight recognizes target: yes, presentCount 2 and missingCount 5.

## Commands Inspected Or Run

- `rg -n "^import org\\.chovy\\.canvas\\.(web|domain|engine|dal)|canvas-engine|backend/canvas-engine" backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
  exited 1 with no matches.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with `MarketingApiCompatibilityTest` present.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
  exited 1 as expected for remaining blockers.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest`
  passed 6/6.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest,CanvasApiCompatibilityTest`
  passed 11/11.

## Findings

- No blocking spec violations found.
- Concern: the compatibility tests assert `code`, `message`, and data shape,
  but do not explicitly assert `errorCode` and `traceId` null fields despite
  the test-local envelope declaring them. Future cutover hardening may want
  explicit assertions for the full `R` envelope.

## Required Fixes

None for DDD-C09C acceptance.

## Residual Risks

- Workspace is broadly dirty/untracked outside this task, so attribution of
  unrelated forbidden-scope changes cannot be established from this review.
- Cutover remains blocked by missing compatibility targets and controller count
  gaps reported by preflight.

## Ledger Update

DDD-C09C spec review PASS_WITH_CONCERNS: Marketing compatibility seed is
present, compiles, passes targeted/combined Maven checks, avoids old-engine
imports, covers requested marketing routes, and is recognized by preflight;
remaining cutover blockers are unrelated to this target.
