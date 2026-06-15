# DDD-C09BW Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/approvals`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApprovalController.java`

Exact reserved files:

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/ApprovalFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/ApprovalApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/ApprovalCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/ApprovalApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/approvals/ApprovalController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/approvals/ApprovalControllerCompatibilityTest.java`

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Spawn a real worker before moving this dispatch to `RUNNING`.
- Tests must prove meaningful behavior: route compatibility, default tenant/actor/role mapping, approve/reject state transition, admin-gated sync behavior, and error envelope handling. Avoid ceremonial tests.
