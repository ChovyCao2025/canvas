# DDD-C09J Quality Review

status: PASS_WITH_CONCERNS

reviewer: Pauli `019eba5b-aa8d-7810-86dd-bc43161ceef2`

files reviewed:
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiAccessRequest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09J-bi-catalog-controller-20260612-131408/worker-return.md`

commands run:
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest` passed 7 tests.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` passed with advisory.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0; global cutover remains false due unrelated route gaps.
- Scoped `rg` scans for persistence/mapper/DO/old-engine coupling found no relevant matches.

findings:
- No blocking findings.
- Controller mappings are correct for the reviewed routes.
- Dataset, chart, and dashboard path keys override body keys; workspace uses the body key; permission grant maps body fields directly; effective access builds `BiAccessRequest` and lets that type normalize resource/action/roles.
- Error handling is sane: `IllegalArgumentException` becomes `400/API_001`, and unrelated runtime failures are not broadly swallowed.
- WebFlux binding failures that surface as `ResponseStatusException` will also get the stable envelope.

non-blocking concerns:
- `CompatibilityEnvelope` triggers the DDD guardrail advisory, but the guardrail exits 0 and labels this as manual review only. This does not block closeout.
- The focused controller test is meaningful and deterministic, but still sample-based for nested DTO fields. It verifies every route, path-key override, tenant/actor defaults, effective-access role normalization, and `API_001`; deeper domain behavior remains covered by `BiApiCompatibilityTest`.

required fixes:
- None.

residual risks:
- Global BI route parity is still incomplete: preflight reports current `/canvas/bi` as 1 controller / 7 endpoints versus old-engine BI at 20 controllers / 169 endpoints.
- Full boot startup against a local DB was not performed.

ledger update:
- DDD-C09J quality review PASS_WITH_CONCERNS: production `BiCatalogController` mapping, `API_001` error envelope, focused tests, and DDD guardrails pass; remaining concerns are non-blocking envelope naming advisory and broader out-of-scope route parity gaps.
