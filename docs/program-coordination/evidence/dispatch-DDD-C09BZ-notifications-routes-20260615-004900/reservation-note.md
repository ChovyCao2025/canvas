# DDD-C09BZ Reservation

Task: DDD-C09BZ `/canvas/notifications`

Gate: R5 after DDD-C09BY Mautic Insights route closeout

Top preflight gap: `route:/canvas/notifications`

Old controller:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`

Legacy routes:

- `GET /canvas/notifications?unreadOnly=&category=&archived=&page=&size=`
- `GET /canvas/notifications/unread-count`
- `PUT /canvas/notifications/{notificationId}/read`
- `PUT /canvas/notifications/read-all`
- `PUT /canvas/notifications/{notificationId}/archive`
- `POST /canvas/notifications/ws-ticket`

Exact reserved files:

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/NotificationFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/NotificationApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/NotificationCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/NotificationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/notifications/NotificationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/notifications/NotificationControllerCompatibilityTest.java`

Coordinator constraints:

- Do not edit `backend/canvas-engine/**` or `pom.xml`.
- Do not add ceremonial tests. Tests must cover route compatibility, pagination/default behavior, tenant/user isolation, mutation semantics, ws-ticket shape, or error envelope behavior.
- Keep Maven verification serial because module builds share `target/`.
