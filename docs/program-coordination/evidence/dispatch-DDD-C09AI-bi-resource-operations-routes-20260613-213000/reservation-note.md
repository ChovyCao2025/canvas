# DDD-C09AI Reservation Note

Reserved compact BI resource operations route batch.

Dispatch: dispatch-DDD-C09AI-bi-resource-operations-routes-20260613-213000
Task: DDD-C09AI
Status: RUNNING
Worker: James 019ec139-50cd-7ad0-8d90-352889a6cd9b
Gate: R5 after recovered DDD-C09AH favorite route closeout
Base SHA: 2a1cdec07ec27a5298958822014aa28d9312869c

## Scope

- `POST /canvas/bi/resources/comments`
- `GET /canvas/bi/resources/comments`
- `DELETE /canvas/bi/resources/comments/{commentId}`
- `POST /canvas/bi/resources/locks/acquire`
- `GET /canvas/bi/resources/locks`
- `POST /canvas/bi/resources/locks/release`
- `POST /canvas/bi/resources/locations`
- `POST /canvas/bi/resources/move`
- `GET /canvas/bi/resources/locations`
- `POST /canvas/bi/resources/transfer`
- `GET /canvas/bi/resources/ownerships`
- `GET /canvas/bi/resources/publish-approvals`
- `POST /canvas/bi/resources/publish-approvals`
- `POST /canvas/bi/resources/publish-approvals/{approvalId}/review`

## Reserved Files

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceCommentCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceCommentView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLockCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLockView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLocationCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLocationView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceMoveCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceTransferCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceOwnershipView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPublishApprovalCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPublishApprovalReviewCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPublishApprovalView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiResourceOperationsCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

## Pre-Dispatch Verification

- G0B backup manifest/branch/head/worktree check passed on main at
  `2a1cdec07ec27a5298958822014aa28d9312869c`.
- C09AH recovery Maven command passed 56/56.
- Program coordination checks passed.
- Dispatch-state verifier passed.
- Cutover preflight exited 0 and reported `/canvas/bi` as the top route gap
  with 25 current endpoints out of 169 old endpoints.
- `generate-worker-prompt.mjs DDD-C09AI .` passed after adding the C09AI worker
  packet.
- `multi_agent_v1.spawn_agent` spawned James
  `019ec139-50cd-7ad0-8d90-352889a6cd9b` before RUNNING.

## Rollback

Revert only the exact DDD-C09AI reserved BI API/domain/application/controller
and BI test files listed above.
