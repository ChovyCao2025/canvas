# DDD-C09CX /canvas/mq-trigger-rejected Compatibility Sidecar Review

Scope: focused read-only review of legacy `/canvas/mq-trigger-rejected` behavior
and current final-module execution controller/test patterns. No
`backend/canvas-engine/**`, `pom.xml`, production source, or test implementation
files were edited.

## Reviewed

- Legacy controller:
  `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java`
- Current final-module execution controllers:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionController.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/DlqController.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionRequestController.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/MessageDeliveryController.java`
- Current final-module execution compatibility tests under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution`
- Related final-module MQ rejected persistence/messaging support under
  `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution`

## Legacy Route List And Compatibility Envelope

All legacy routes are rooted at `/canvas/mq-trigger-rejected` and return the old
`R`-style envelope:

- success: `code=0`, `message="success"`, `data=...`, absent/null `errorCode`,
  absent/null `traceId`
- adjacent final-module execution controllers implement this with a local
  `CompatibilityEnvelope<T>(code, message, errorCode, data, traceId)` record
- explicit bad-request handlers in `DlqController`, `ExecutionController`, and
  `ExecutionRequestController` map `IllegalArgumentException` to HTTP 400 with
  `code=400`, `errorCode="API_001"`, and no `data`; this is the best fit for
  missing rejected rows and invalid replay bodies unless the main thread chooses
  a documented HTTP-200 business-failure envelope

| Method | Route | Query params | Expected compatibility data behavior |
| --- | --- | --- | --- |
| `GET` | `/canvas/mq-trigger-rejected` | `tag` optional, `reason` optional, `page` default `1`, `size` default `20` | Returns `data.total` plus `data.list`. `tag` filter applies only when non-blank; `reason` filter applies only when non-blank. Results are ordered by `createdAt` descending. Rows expose legacy rejected-record fields: `id`, `msgId`, `tag`, `reason`, `errorMsg`, `body`, `createdAt`. |
| `GET` | `/canvas/mq-trigger-rejected/{id}` | none | Loads by id and returns the rejected record as `data`. Missing id throws `IllegalArgumentException("rejected 消息不存在: " + id)`. |
| `POST` | `/canvas/mq-trigger-rejected/{id}/replay` | none | Loads by id, parses `body` as the legacy MQ trigger payload, validates required fields, resolves current MQ routes by rejected `tag`, creates one execution request per valid positive routed canvas id, attempts immediate best-effort dispatch for each request, and returns map keys `count`, `requestIds`, `dispatchFailureCount`, and `dispatchFailedRequestIds`. Missing id throws `IllegalArgumentException("rejected 消息不存在: " + id)`. Invalid body or missing required legacy payload fields throws an `IllegalArgumentException` with the legacy Chinese message. |

Replay is compatibility-sensitive because the old rejected `body` is parsed as
`org.chovy.canvas.infrastructure.mq.MqTriggerMessage` with fields
`userId`, `messageCode`, and `payload`. The current final-module
`org.chovy.canvas.execution.adapter.messaging.MqTriggerMessage` has different
fields: `tenantId`, `canvasId`, `versionId`, `triggerType`, `matchKey`,
`sourceMsgId`, and `payload`. A new compatibility facade should either preserve
the old rejected-body parser explicitly or document a migration envelope; it
should not silently parse old rows with the new adapter message shape.

## Final-Module Patterns Observed

- Controllers are thin route adapters in `backend/canvas-web`, with blocking
  facade work wrapped in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
- Controllers define local compatibility envelopes instead of importing
  `org.chovy.canvas.common.R`.
- Tests bind controllers directly with `WebTestClient.bindToController(...)`,
  use small recording facades, and assert both JSON response shape and mapped
  query/command values.
- `canvas-context-execution` already contains final-module persistence mapping
  for `canvas_mq_trigger_rejected`:
  `CanvasMqTriggerRejectedDO` and `CanvasMqTriggerRejectedMapper`.
- No final-module web controller for `/canvas/mq-trigger-rejected` was present
  during this sidecar review.

## Meaningful Edge Cases Worth Testing

List:

- Omitted paging uses `page=1` and `size=20`.
- Blank `tag` and blank `reason` are ignored rather than forwarded as exact
  filters.
- Non-blank `tag` and `reason` are both forwarded and combined.
- Response preserves `data.total` and `data.list`; do not rename to `records`,
  `items`, or `content`.
- Newest-first ordering by `createdAt` should be owned by the facade/repository
  or otherwise covered where sorting lives.

Detail:

- Existing id returns the row fields needed by old UI/support tooling, including
  the raw `body`.
- Missing id maps to the selected compatibility error envelope with message
  `rejected 消息不存在: <id>`, not a raw 500 response.

Replay:

- Missing id maps to the same missing-row error envelope and does not parse,
  route, enqueue, or dispatch anything.
- Malformed JSON body fails replay with message
  `无法重放 rejected 消息，消息体不是合法 MQ 触发 JSON`.
- Parsed body missing blank `userId`, blank `messageCode`, or null `payload`
  fails with message `无法重放 rejected 消息，缺少 userId/messageCode/payload`.
- Replay uses the current route table keyed by rejected `tag`, not by
  `messageCode`.
- Route table entries that are non-numeric, zero, or negative are skipped and do
  not prevent valid routed canvas ids from replaying.
- Valid routed canvas ids are replayed in ascending numeric order.
- Zero valid routed canvas ids still returns success with `count=0`,
  `requestIds=[]`, `dispatchFailureCount=0`, and
  `dispatchFailedRequestIds=[]`.
- Each successful enqueue contributes exactly one request id to `requestIds`.
- Immediate dispatch failure does not roll back the queued request; the failed
  request id appears in `dispatchFailedRequestIds`, and
  `dispatchFailureCount` matches that list size.
- Replay forwards legacy values into the execution request boundary:
  `userId` from parsed body, trigger type `MQ`, trigger node type `MQ_TRIGGER`,
  match key/replay key from rejected `tag`, payload from parsed body, and source
  message id from rejected `msgId`.

## Old-Engine Coupling Strings New Files Should Avoid

New final-module files should not import or reference old `canvas-engine`
implementation packages/classes:

- `org.chovy.canvas.web.CanvasMqTriggerRejectedController`
- `org.chovy.canvas.dal.dataobject.CanvasMqTriggerRejectedDO`
- `org.chovy.canvas.dal.mapper.CanvasMqTriggerRejectedMapper`
- `org.chovy.canvas.infrastructure.mq.MqTriggerMessage`
- `org.chovy.canvas.infrastructure.redis.TriggerRouteService`
- `org.chovy.canvas.engine.request.CanvasExecutionRequestService`
- `org.chovy.canvas.engine.disruptor.CanvasDisruptorService`
- `org.chovy.canvas.common.R`
- `org.chovy.canvas.common.PageResult`
- `org.chovy.canvas.common.MapFieldKeys`
- `org.chovy.canvas.common.enums.TriggerType`
- `org.chovy.canvas.common.enums.NodeType`
- `com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper` in web
  compatibility code
- `com.baomidou.mybatisplus.extension.plugins.pagination.Page` in web
  compatibility code
- `backend/canvas-engine`

Prefer final-module contracts and adapters:

- `org.chovy.canvas.execution.adapter.persistence.CanvasMqTriggerRejectedDO`
- `org.chovy.canvas.execution.adapter.persistence.CanvasMqTriggerRejectedMapper`
- a new final-module facade/port for list, detail, and replay behavior
- literal response-map keys at the compatibility boundary if no shared final
  constants exist: `count`, `requestIds`, `dispatchFailureCount`,
  `dispatchFailedRequestIds`

Compatibility-sensitive literal strings observed in legacy behavior:

- Route prefix: `/canvas/mq-trigger-rejected`
- Missing-row message prefix: `rejected 消息不存在: `
- Invalid JSON replay message:
  `无法重放 rejected 消息，消息体不是合法 MQ 触发 JSON`
- Missing field replay message:
  `无法重放 rejected 消息，缺少 userId/messageCode/payload`
- Replay result keys: `count`, `requestIds`, `dispatchFailureCount`,
  `dispatchFailedRequestIds`
- Legacy replay trigger values: `MQ` and `MQ_TRIGGER`

## Suggested Test Shape

If the main implementation adds coverage, one compact
`CanvasMqTriggerRejectedControllerCompatibilityTest` under
`backend/canvas-web/src/test/java/org/chovy/canvas/web/execution` would be
meaningful. Bind the controller directly with a recording final-module facade
and assert:

- list filter/default mapping plus `data.total`/`data.list` envelope shape
- detail existing and missing-id behavior
- replay success with multiple route ids, invalid route-id skipping, sorted
  replay order, and dispatch-failure accounting
- replay invalid body and missing required legacy payload fields

Avoid tests that only prove annotations exist without validating compatibility
data shape or replay command/query mapping.
