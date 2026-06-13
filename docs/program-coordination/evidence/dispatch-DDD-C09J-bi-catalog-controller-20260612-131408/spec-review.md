# DDD-C09J Spec Review

status: PASS_WITH_CONCERNS

reviewer: Epicurus `019eba5b-a9fb-7b12-bf34-2566fa92e6f5`

files reviewed:
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`
- `docs/ddd-rewrite/contract-tests/compatibility-test-plan.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09J-bi-catalog-controller-20260612-131408/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09J-bi-catalog-controller-20260612-131408/worker-return.md`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

commands inspected or run:
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiApiCompatibilityTest,BiCatalogControllerCompatibilityTest` passed 7 tests, 0 failures.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json` exited 1 as expected due unrelated global route gaps.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` exited 0.
- Scoped forbidden-coupling `rg` returned no matches.
- `find` confirmed only one production BI controller file and one `web/bi` compatibility test file in this seed scope.

findings:
- Low: Independent write-scope attribution is limited because the repo is already broadly dirty/untracked from other dispatches.
- No blocking spec issues found.
- The controller exposes exactly the seven required `/canvas/bi` routes, delegates through `BiCatalogFacade`, preserves the compatibility envelope, defaults tenant/actor headers, preserves path-key override behavior, and maps `IllegalArgumentException` to `API_001` bad-request envelopes.
- No old `canvas-engine`, old `org.chovy.canvas.domain.bi.*`, mapper, DO, or persistence adapter imports were found.
- `org.chovy.canvas.bi.domain.BiAccessRequest` use matches the final facade method shape and packet read scope.

required fixes:
- None.

residual risks:
- Full cutover readiness is still false: current `canvas-web` is 2 controllers / 12 endpoints versus old `canvas-engine` web 142 controllers / 806 endpoints.
- Guardrails emit an advisory on `CompatibilityEnvelope` naming, but exit 0; this matches the worker-return note.

ledger update:
- DDD-C09J spec review PASS_WITH_CONCERNS: BI catalog production controller seed matches packet and cutover contracts; seven facade-backed routes and compatibility checks pass, with only unrelated global route gaps remaining.
