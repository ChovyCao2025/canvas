# DDD-C09ED Worker Return

Task: port legacy `POST /delivery/receipts` route into final modules.

Scope changed:
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/DeliveryReceiptFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/DeliveryReceiptApplicationService.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/DeliveryReceiptController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/DeliveryReceiptControllerCompatibilityTest.java`

Contract notes:
- Preserved legacy route `POST /delivery/receipts`.
- Preserved required header name `X-Canvas-Receipt-Secret`.
- Preserved required request fields: `provider`, `providerMessageId`, `receiptType`.
- Preserved success envelope shape: `code=0`, `message=success`, no `errorCode`, no `traceId`.
- Kept implementation in final modules; no old engine or common response imports.

Verification:
- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=DeliveryReceiptControllerCompatibilityTest test`
  - Failed before implementation because `DeliveryReceiptFacade` and `DeliveryReceiptController` did not exist.
  - Same run also exposed an unrelated pre-existing test-compile failure in `CdpEventIngestionControllerCompatibilityTest`.
- GREEN attempt: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=DeliveryReceiptControllerCompatibilityTest test`
  - Delivery sources and test compiled past their earlier missing-symbol failures.
  - Blocked before Surefire by unrelated `CdpEventIngestionControllerCompatibilityTest` constructor mismatch outside this task scope.
- Production compile: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile`
  - Passed with reactor `BUILD SUCCESS`.

Known external blocker:
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpEventIngestionControllerCompatibilityTest.java:94` expects a two-argument `CdpEventIngestionController` constructor, while the current controller has one argument. This is outside DDD-C09ED ownership and was not edited.
