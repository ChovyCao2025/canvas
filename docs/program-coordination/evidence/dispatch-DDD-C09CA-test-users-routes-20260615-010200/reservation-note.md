# DDD-C09CA Reservation

Task: DDD-C09CA `/test-users`

Gate: R5 after DDD-C09BZ Notifications route closeout

Top preflight gap: `route:/test-users`

Old controller:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TestUserController.java`

Legacy routes:

- `GET /test-users/sets`
- `POST /test-users/sets`
- `GET /test-users/sets/{setId}/users`
- `POST /test-users/sets/{setId}/users`
- `GET /test-users/{id}`
- `GET /test-users/{id}/preview`

Exact reserved files:

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/TestUserFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/TestUserApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/TestUserCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/TestUserApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/platform/TestUserController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/platform/TestUserControllerCompatibilityTest.java`

Coordinator constraints:

- Do not edit `backend/canvas-engine/**` or `pom.xml`.
- Do not add ceremonial tests. Tests must cover route compatibility, tenant default/isolation, direct DO field names, create semantics, JSON string vs preview map behavior, or not-found envelope behavior.
- Keep Maven verification serial because module builds share `target/`.
