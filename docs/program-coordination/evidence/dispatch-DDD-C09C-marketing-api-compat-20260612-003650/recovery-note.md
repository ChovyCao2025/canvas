# DDD-C09C Marketing API Compatibility Test Seed Note

Date: 2026-06-12

## Dispatch

- dispatch id: dispatch-DDD-C09C-marketing-api-compat-20260612-003650
- task id: DDD-C09C
- mode: code-writing
- branch: main
- worktree: /Users/photonpay/project/canvas
- base SHA: 01aac65697d524f4cf2e92d954db088895631004
- integration target: DDD_FINAL_MODULE
- exact reserved files:
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`

## Gate Evidence

- G0 passed: dispatch-state verifier and program coordination checks passed
  after DDD-C09B ledger recovery.
- G0B passed: backup manifest exists; branch is `main`; HEAD is
  `01aac65697d524f4cf2e92d954db088895631004`; worktree list inspected.
- G2 passed: DDD guardrail shell syntax and guardrail checks passed with the
  known `RiskRuleValidator` advisory only.
- Target status showed no existing
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
  changes before reservation.
- Preflight JSON reported one present compatibility target and six missing
  targets, including `MarketingApiCompatibilityTest`.
- Marketing application service baseline passed:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingCampaignApplicationServiceTest`
  passed 7/7.
- Read-only explorer Huygens `019eb780-b084-7230-920d-ff7d205fca34`
  recommended the marketing route group as the next narrow compatibility seed.

## Reason

DDD-C09A preflight still reports six missing
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/*CompatibilityTest.java`
targets after DDD-C09B. Marketing has a compact legacy route group and an
existing DDD-final facade/application API, making it the narrowest next
compatibility target that can avoid production controller and old-engine edits.

## Rollback

Remove
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
and this evidence directory only.
