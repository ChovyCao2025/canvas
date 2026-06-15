# DDD-C09CW Execution Requests Compatibility Sidecar

Reviewed:

- Legacy controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Current final-module patterns:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionController.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/DlqController.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/MessageDeliveryController.java`
  - matching compatibility tests under `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution`

## Route List And Compatibility Envelope

All legacy execution-request routes are rooted at `/canvas/execution-requests` and return the old `R`-style envelope:

- success: `{"code":0,"message":"success","data":...}` with absent/null `errorCode` and `traceId`
- bad-request style failures in current final-module tests generally map to JSON envelopes with `code=400`, `errorCode=API_001`, and no `data`
- adjacent compatibility controllers use `WebTestClient.bindToController(...)`, facade stubs, `Mono.fromCallable(...)`, and `Schedulers.boundedElastic()` for blocking facade calls

### `GET /canvas/execution-requests`

Query parameters:

- `canvasId` optional `Long`
- `status` optional `String`; blank means no status filter
- `userId` optional `String`; blank means no user filter
- `sourceMsgId` optional `String`; blank means no source-message filter
- `page` default `1`; values `<=0` normalize to `1`
- `size` default `20`; values `<=0` normalize to `20`; values `>100` cap at `100`

Data behavior:

- returns `data.total` plus `data.list`, matching `PageResult` field names
- list ordering is newest first by `updatedAt`
- tenant filtering applies unless current tenant context is super-admin or has no tenant id
- response records expose execution-request fields from the final-module execution request view/DO shape, including ids and replay metadata where available: `id`, `tenantId`, `canvasId`, `userId`, `triggerType`, `triggerNodeType`, `sourceMsgId`, `status`, `attemptCount`, `lastError`, `replayCount`, `lastReplayAt`, `lastReplayBy`, `lastReplayReason`, `createdAt`, `updatedAt`

### `POST /canvas/execution-requests/{id}/replay`

Path/query parameters:

- `id` path variable is a `String`
- `reason` optional; null/blank normalizes to `""`
- `force` default `false`

Data behavior:

- finds the request by id before replaying
- without `force`, only `FAILED` and `RETRY` are replayable
- with `force=true`, any status can be accepted by the controller layer
- successful replay marks the request back to queued/pending replay state and then tries immediate dispatch best-effort
- returns map keys:
  - `requestId`: replayed request id
  - `status`: literal `QUEUED`
  - `immediateDispatch`: boolean indicating whether immediate dispatch succeeded
- missing id should surface as a bad-request envelope preserving the legacy message shape, e.g. `执行请求不存在: <id>`
- non-replayable status without force should surface as a bad-request envelope with message `只能重放 FAILED/RETRY 状态的执行请求，其他状态请使用 force=true`

### `POST /canvas/execution-requests/replay`

Query parameters:

- `canvasId` optional `Long`
- `status` optional `String`
- `userId` optional `String`
- `sourceMsgId` optional `String`
- `limit` default `100`; values `<=0` normalize to `100`; values `>500` cap at `500`
- `reason` optional; null/blank normalizes to `""`
- `force` default `false`

Data behavior:

- selects oldest first by `updatedAt`
- applies tenant filter like list
- when `status` is supplied:
  - trims the supplied value
  - without `force`, rejects statuses outside `FAILED` and `RETRY`
  - with `force`, uses the explicit status as the filter
- when `status` is absent/blank:
  - without `force`, selects only `FAILED` and `RETRY`
  - with `force`, no status filter is added
- attempts each selected record independently
- returns map keys:
  - `count`: number of records successfully marked for replay
  - `limit`: normalized limit used by the query/rate limiter
  - `requestIds`: ids successfully marked for replay
  - `dispatchFailureCount`: number of best-effort immediate dispatch failures
  - `dispatchFailedRequestIds`: ids whose immediate dispatch failed

## Meaningful Edge Cases Worth Testing

- List route passes all optional filters, applies page/size defaults, normalizes `page<=0`, caps `size>100`, and preserves `data.total`/`data.list`.
- List route ignores blank `status`, `userId`, and `sourceMsgId` instead of forwarding blank filters.
- Single replay returns the exact legacy data keys `requestId`, `status=QUEUED`, and `immediateDispatch`.
- Single replay rejects `SUCCEEDED`, `PENDING`, or `RUNNING` without `force=true`, and does not call replay mutation/dispatch.
- Single replay with `force=true` allows a non-FAILED/RETRY status.
- Single replay missing id maps to an error envelope rather than a raw server error.
- Batch replay default query uses replayable statuses only when no status is supplied and `force=false`.
- Batch replay rejects explicit non-replayable status without `force=true`.
- Batch replay with `force=true` and no status does not add the FAILED/RETRY-only filter.
- Batch replay normalizes `limit<=0` to `100` and caps `limit>500` at `500`.
- Batch replay preserves independent partial success: successful ids in `requestIds`, immediate-dispatch failures only in `dispatchFailedRequestIds`.
- Tenant isolation should be covered at the facade/application layer if the final-module side has tenant-aware APIs; the web compatibility test can at least verify the controller does not hard-code engine tenant objects.

## Old-Engine Coupling Strings New Files Should Avoid

New final-module files should not import or reference old `canvas-engine` implementation packages/classes:

- `org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO`
- `org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper`
- `org.chovy.canvas.engine.disruptor.CanvasDisruptorService`
- `org.chovy.canvas.engine.request.CanvasExecutionReplayRateLimiter`
- `org.chovy.canvas.engine.request.CanvasExecutionRequestStatus`
- `org.chovy.canvas.common.tenant.TenantContext`
- `org.chovy.canvas.common.tenant.TenantContextResolver`
- `org.chovy.canvas.common.MapFieldKeys`
- `com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper`
- `com.baomidou.mybatisplus.extension.plugins.pagination.Page`
- `io.jsonwebtoken.Claims`
- `ReactiveSecurityContextHolder` for replay operator extraction in web compatibility code
- MyBatis SQL suffix coupling such as `.last("LIMIT " + normalizedLimit)`
- direct dispatch coupling such as `publishRequest`, `markPendingForReplay`, or `selectDueRequests`

Prefer a final-module facade/port contract from `canvas-context-execution` and keep the web controller limited to route binding, default/normalization behavior, compatibility envelopes, and exception mapping.
