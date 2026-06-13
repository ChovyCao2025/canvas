# DDD-C09E Quality Review

review id: review-DDD-C09E-quality-20260612

worker task id: DDD-C09E

review scope: read-only quality review for Risk API compatibility test seed

reviewer: Turing `019eb81c-85b2-7720-aeaa-93f03ecd93ef`

status: PASS_WITH_CONCERNS

files reviewed:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09E-risk-api-compat-20260612-024746/worker-return.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09E-risk-api-compat-20260612-024746/spec-review.md`
- `docs/program-coordination/subagent-worker-packets.md` section DDD-C09E
- `docs/ddd-rewrite/contract-tests/compatibility-test-plan.md`
- referenced legacy risk controller/DTO/test/trace files and final risk API records

commands inspected or run:

- read-only `sed`, `nl`, `rg`, `find`, and `git status` inspections
- inspected existing surefire reports for `RiskApiCompatibilityTest`: 7 tests, 0 failures
- did not rerun Maven to honor inspect-only review; relied on coordinator verification evidence

findings:

- No critical or important blocker found.
- Assertions are meaningful, not just route smoke: success envelope, decision fields, command body mapping, tenant override, validation-before-facade-call, budget lookup, replay mismatch 409, and trace query propagation are all asserted.
- JSON/request parsing and validation logic in the test-local adapter matches the legacy controller behavior for covered cases.
- No old canvas-engine imports were found in the new test.
- Nonblocking concern: `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java:17` imports the final risk domain runtime replay exception rather than an api-level exception. This mirrors current final facade behavior, but final web binding should consciously own that dependency or expose an API exception.
- Nonblocking concern: trace coverage remains through a test-local `RiskDecisionTraceReader` because `RiskDecisionFacade` only exposes evaluate today. This is allowed by the packet but should not be treated as final production web binding completion.
- Low residual test gap: future event-time coverage asserts the `>24h` rejection path but not the allowed `<=24h` boundary. The adapter implementation itself models the legacy threshold correctly.

required fixes:

- None.

ledger update:

- DDD-C09E review-DDD-C09E-quality-20260612 PASS_WITH_CONCERNS: accept the Risk API compatibility test seed; no required quality fixes. Record residual notes for domain-exception coupling, test-local trace adapter/final web binding, and optional future event-time boundary coverage.
