# DDD-C09E Spec Review

review id: review-DDD-C09E-spec-20260612

worker task id: DDD-C09E

review scope: read-only spec compliance review for Risk API compatibility test seed

reviewer: Mencius `019eb816-29de-7340-9e10-32d3b73d17e2`

status: PASS_WITH_CONCERNS

files reviewed:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09E-risk-api-compat-20260612-024746/worker-return.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/ddd-rewrite/contract-tests/compatibility-test-plan.md`
- referenced risk controller/DTO/API/test files listed in scope
- adjacent compatibility tests: `CanvasApiCompatibilityTest`, `MarketingApiCompatibilityTest`, `ConversationApiCompatibilityTest`

commands inspected or run:

- read-only `nl`, `sed`, `rg`, `find`, and `git status --short -- ...`
- inspected surefire artifacts for `RiskApiCompatibilityTest`: 7 tests, 0 failures
- did not rerun Maven to keep this review read-only; coordinator-provided Maven/preflight evidence was reviewed

findings:

- No required spec gaps found.
- `RiskApiCompatibilityTest` is present at the required compatibility-test path and matches the preflight target name.
- The test covers `POST /canvas/risk/decisions/evaluate` and `GET /canvas/risk/decisions/traces`.
- Evaluate success assertions cover R envelope success shape, tenant-header override of body `tenantId`, body-to-command mapping, and required decision fields including score, band, reasons, matchedRules, labels, missingFeatures, traceAvailable, and latencyMs.
- Validation/conflict coverage includes missing `sceneKey`, missing subject identifier, future `eventTime`, deadline above scene budget, and replay mismatch mapped to HTTP 409.
- Trace route coverage asserts `sceneKey` and `limit` query propagation.
- Old-engine import scan on the new test returned no matches for `canvas-engine`, `org.chovy.canvas.domain.risk`, or `org.chovy.canvas.web.risk`.
- No placeholder assertions/routes were added for risk scene, strategy, list, or lab route groups.
- Concern: trace query behavior is covered through a test-local adapter because the final risk facade currently exposes only `evaluate`; this is allowed by the worker packet but remains final cutover binding work.
- Concern: the broader worktree is heavily dirty/untracked from unrelated modularization work, so this review only certifies the named DDD-C09E file/artifacts and not repository-wide authorship.

required fixes:

- None.

ledger update:

- DDD-C09E review-DDD-C09E-spec-20260612 PASS_WITH_CONCERNS: accept the Risk API compatibility test seed; no required spec fixes. Record residual notes for test-local trace adapter/final web binding and broad dirty-tree review limitation.
