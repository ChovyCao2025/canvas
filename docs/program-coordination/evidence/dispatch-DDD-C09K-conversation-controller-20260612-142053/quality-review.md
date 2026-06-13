status: FAIL
task id: DDD-C09K
review type: quality

files reviewed:
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/ConversationControllerCompatibilityTest.java`
- Compared against `ConversationApiCompatibilityTest`, `BiCatalogController`, and conversation API/boot wiring as needed.

commands inspected or run:
- Inspected `worker-return.md` and coordinator timeout audit.
- Inspected/reran route and coupling searches.
- Confirmed reported focused test: `mvn test -pl canvas-web -am -Dtest=ConversationControllerCompatibilityTest` passed 2/2.
- Ran preflight: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0; global cutover still not ready.

findings:
- Blocker: production startup wiring is not covered and appears incomplete for this controller. `ConversationController` constructor-injects `ConversationFacade` at `ConversationController.java:40`; the production implementation is `ConversationApplicationService`, whose public constructor requires conversation repository ports, `ConversationWaitResumePort`, and `Clock` at `ConversationApplicationService.java:63`. Exact searches found only port interfaces, e.g. `ConversationSessionRepository.java:7` and `ConversationWaitResumePort.java:5`, with no production adapter/bean implementations, and no non-legacy `Clock` bean. With `CanvasBootApplication` scanning `org.chovy.canvas` at `CanvasBootApplication.java:6`, this is a full app startup risk not exercised by the controller-bound WebTestClient test.

required fixes:
- Add production beans/adapters for the conversation repository ports and `ConversationWaitResumePort`, or otherwise gate the controller/facade until those beans exist.
- Avoid requiring a Spring `Clock` bean from `ConversationApplicationService`, or provide one in the boot context.
- Add a `canvas-boot` context smoke test or equivalent full-context wiring test.

accepted concerns:
- The seven compact routes are correctly mapped and match the existing test-only adapter.
- Default tenant `7`, default actor `operator-1`, blank actor trimming, ingress body mapping, and direct command binding are meaningfully covered.
- Missing explicit bad-request envelope coverage is acceptable for this slice because the handler mirrors the BI controller pattern, but it should be added before hardening.
- Global route parity blockers remain outside this DDD-C09K slice.

ledger update:
- `DDD-C09K REVIEW/FAIL`: scoped controller behavior verifies clean, but production readiness is blocked by unresolved full-context facade wiring.
