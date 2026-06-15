# DDD-C09BX Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/marketing-forms`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java`

Routes in scope:

- `GET /canvas/marketing-forms`
- `GET /canvas/marketing-forms/{id}`
- `POST /canvas/marketing-forms`
- `PUT /canvas/marketing-forms/{id}`
- `PUT /canvas/marketing-forms/{id}/status`
- `GET /canvas/marketing-forms/submissions`

Routes explicitly out of scope because they are already covered by PublicIngress:

- `GET /public/marketing-forms/{publicKey}`
- `POST /public/marketing-forms/{publicKey}/submit`

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingFormFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingFormApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingFormCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingFormApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingFormController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingFormControllerCompatibilityTest.java`

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; prove route compatibility, default/header mapping, status transition, submission filtering/limit, and error envelopes.
- Keep public marketing form routes out of this batch to avoid duplicate mappings with `PublicIngressController`.
