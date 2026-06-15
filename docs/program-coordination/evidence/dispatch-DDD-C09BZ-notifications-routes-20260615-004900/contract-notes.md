# DDD-C09BZ Notification Contract Notes

Read-only legacy sources:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/notification/NotificationDTO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/notification/NotificationWebSocketTicketDTO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationWebSocketTicketService.java`

Legacy route surface:

- `GET /canvas/notifications`
  - defaults: `unreadOnly=false`, `archived=false`, `page=1`, `size=20`
  - `page` is normalized to at least 1
  - `size` is clamped to 1..100
  - optional category filter
- `GET /canvas/notifications/unread-count`
  - returns `{ "count": <long> }`
- `PUT /canvas/notifications/{notificationId}/read`
  - marks only current user's non-archived unread matching notification as read
- `PUT /canvas/notifications/read-all`
  - marks only current user's non-archived unread notifications as read
- `PUT /canvas/notifications/{notificationId}/archive`
  - archives only current user's non-archived matching notification
- `POST /canvas/notifications/ws-ticket`
  - returns `{ "ticket": "ntf_ws_...", "expiresInSeconds": 60 }`

Notification payload fields from `NotificationDTO`:

- `notificationId`
- `type`
- `category`
- `severity`
- `status`
- `title`
- `content`
- `targetUrl`
- `actionLabel`
- `actionUrl`
- `taskId`
- `bizType`
- `bizId`
- `dedupKey`
- `payloadJson`
- `readAt`
- `archivedAt`
- `deliveredAt`
- `createdAt`

Compatibility risks to test:

- list route must preserve envelope shape and meaningful notification fields, not only id/title stubs
- pagination normalization and size clamp affect result count
- unread and archived filters must use `readAt`/`archivedAt` semantics
- mutations must not affect another user or tenant
- ws-ticket TTL is exactly 60 seconds and ticket prefix should remain recognizable
- missing tenant for non-admin should map to an `AUTH_003` forbidden envelope in the final controller style
