# DDD-C09CV /canvas/dlq Compatibility Sidecar Review

Scope: focused read-only review of legacy `/canvas/dlq` behavior and current
final-module execution controller/test patterns. No `backend/canvas-engine/**`,
`pom.xml`, production source, or test implementation files were edited.

## Legacy Route List

Legacy source:
`backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`

Routes under `@RequestMapping("/canvas/dlq")`:

| Method | Route | Query params | Expected compatibility envelope / data behavior |
| --- | --- | --- | --- |
| `GET` | `/canvas/dlq` | `canvasId` optional, `page` default `1`, `size` default `20` | Returns `code=0`, `message=success`, absent/null `errorCode`, absent/null `traceId`, and `data` as legacy `PageResult` with `total` and `list`. Results are ordered by `failedAt` descending. `canvasId`, when present, filters exactly. `page` is clamped to at least `1`; `size` is clamped to `[1, 100]`. |
| `POST` | `/canvas/dlq/{id}/replay` | `skipSuccessNodes` default `true` | Loads DLQ row by id; missing row throws `IllegalArgumentException("DLQ 记录不存在: " + id)`. Parses `triggerPayload` JSON into a map; parse failure is logged and replay proceeds with `{}`. Replays with original `canvasId`, `userId`, `triggerType`, `triggerNodeType`, `matchKey`, parsed payload, generated idempotency key prefix `dlq-replay-`, and `dryRun=false`. Returns `code=0`, `message=success`, `data` as the execution result map/view returned by the final execution facade. Legacy accepts but does not use `skipSuccessNodes` in the actual replay call. |
| `DELETE` | `/canvas/dlq/{id}` | none | Calls delete by id and always returns success envelope `code=0`, `message=success`, `data=null`. Legacy does not report whether a row existed. |

Compatibility envelope expected from current final-module patterns:

- Controller-local `CompatibilityEnvelope<T>(code, message, errorCode, data, traceId)`.
- Success: `code=0`, `message="success"`, `errorCode=null`, `traceId=null`.
- For bad request branches, current execution controllers map `IllegalArgumentException`
  or `ResponseStatusException` to HTTP 400-style `code=400`,
  `errorCode="API_001"`, `data=null` when they choose explicit exception
  handlers. Message-delivery routes instead use legacy failure envelopes with
  HTTP 200 for business failures, so the DLQ cutover should pick one deliberately
  and cover it.

## Final-Module Patterns Observed

- `ExecutionController` keeps old paths directly in `canvas-web`, maps request
  data into `CanvasExecutionFacade.ExecutionRequestCommand`, and wraps responses
  in a local compatibility envelope.
- `MessageDeliveryController` uses `@RequestMapping`, a final-module facade, and
  `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` for blocking
  facade/persistence-shaped operations.
- Existing execution compatibility tests bind controllers directly with
  `WebTestClient.bindToController(...)`, use small recording facades, and assert
  both JSON envelope shape and mapped command/query values. This is the right
  shape for DLQ coverage if behavior tests are added.
- Final execution persistence already has `CanvasExecutionDlqDO` and
  `CanvasExecutionDlqMapper` under
  `org.chovy.canvas.execution.adapter.persistence`, but no final `DlqController`
  or `DlqFacade` was found outside `canvas-engine` during this review.

## Edge Cases Worth Meaningful Coverage

List:

- Defaults and bounds: omitted `page/size` maps to `1/20`; `page<=0` becomes `1`;
  `size<=0` becomes `1`; `size>100` becomes `100`.
- `canvasId` filter is passed through exactly and does not disappear when paging
  values are clamped.
- Response data preserves legacy `PageResult` shape: `data.total` and
  `data.list`, not a renamed `items`, `records`, or `content` field.
- Ordering by newest `failedAt` first if the final facade owns sorting rather
  than relying on mapper order.

Replay:

- Missing DLQ id maps to the selected compatibility error envelope and does not
  call the execution facade.
- Valid JSON `triggerPayload` is parsed to a map and sent as replay payload.
- Malformed or blank `triggerPayload` proceeds with an empty payload map instead
  of failing the replay route.
- `triggerType` fallback stays `DLQ_REPLAY` only when the stored value is null;
  otherwise preserve stored values such as `DIRECT_CALL`, `BEHAVIOR`, or `MQ`.
- `triggerNodeType` fallback stays `DIRECT_CALL` only when the stored value is
  null; otherwise preserve stored trigger-node context such as `EVENT_TRIGGER`
  or `MQ_TRIGGER`.
- `matchKey` is forwarded unchanged, including null.
- Generated idempotency key starts with `dlq-replay-`; do not assert the random
  suffix exactly.
- `skipSuccessNodes` remains accepted for wire compatibility. Since legacy did
  not apply it to the replay call, test either that it is intentionally ignored
  or, if the final facade supports it, that the intentional new behavior is
  documented.
- Replay does not delete the DLQ row automatically.

Delete:

- Existing id and missing id both return success if the final contract stays
  legacy-compatible.
- Delete route returns a null data success envelope, not a boolean/count unless
  a deliberate compatibility change is accepted.
- Delete delegates exactly the path id and does not require `canvasId`.

## Old-Engine Coupling Strings To Avoid

New final-module files for this cutover should avoid importing or depending on
old `canvas-engine` internals. Specific strings to keep out unless there is an
explicit bridge decision:

- `org.chovy.canvas.web.DlqController`
- `org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO`
- `org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper`
- `org.chovy.canvas.engine.trigger.CanvasExecutionService`
- `org.chovy.canvas.common.R`
- `org.chovy.canvas.common.PageResult`
- `org.chovy.canvas.common.enums.TriggerType`
- `org.chovy.canvas.common.enums.NodeType`
- `backend/canvas-engine`

Use final-module equivalents or explicit final API contracts instead:

- `org.chovy.canvas.execution.adapter.persistence.CanvasExecutionDlqDO`
- `org.chovy.canvas.execution.adapter.persistence.CanvasExecutionDlqMapper`
- `org.chovy.canvas.execution.api.CanvasExecutionFacade`
- final-module constants/strings for `DLQ_REPLAY` and `DIRECT_CALL` if no shared
  final enum exists.

Compatibility-sensitive literal strings observed in legacy behavior:

- Route prefix: `/canvas/dlq`
- Replay idempotency prefix: `dlq-replay-`
- Missing-row message prefix: `DLQ 记录不存在: `
- Fallback trigger type: `DLQ_REPLAY`
- Fallback trigger node type: `DIRECT_CALL`

## Suggested Test Shape

If the main implementation adds tests, prefer one compact
`DlqControllerCompatibilityTest` under `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution`
that binds the controller directly and uses a recording final-module facade or
repository abstraction. Useful assertions are the list paging/filter envelope,
replay command mapping including payload parse fallback, missing replay id error
mapping, and delete success for both found and absent ids. Avoid route-only tests
that only prove Spring annotations exist.
