# DDD-C09DI Worker Return

Worker: Darwin `019ec830-a535-7f60-89db-6a3d20c8fd39`
Status: DONE

## Contract Summary

Legacy route family: `/canvas/async-tasks`

Endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/canvas/async-tasks/{taskId}` | path `taskId`; returns one async task view |
| GET | `/canvas/async-tasks` | optional query `taskType`, `bizType`, `bizIds`, `statuses`, `page=1`, `size=100`; returns task list |

Compatibility details used by coordinator:

- Success envelope is `{ code: 0, message: "success", errorCode: null, data, traceId: null }`.
- Missing or invisible task throws `IllegalArgumentException("Async task not found: " + taskId)` and maps to HTTP 400 with `errorCode=API_001`.
- `bizIds` and `statuses` are comma-separated strings; items are trimmed and blank items dropped.
- `page` is clamped to at least `1`; `size` is clamped to `[1, 200]`.
- Current user defaults to `system`; role defaults to `OPERATOR`; admin check is literal `ADMIN`.
- Non-admin users can see tasks they created or subscribed to; admins skip creator/subscriber filtering.
- List ordering is newest first by `createdAt`.
- Async task data fields are `taskId`, `taskType`, `bizType`, `bizId`, `title`, `status`, `progress`, `resultSummary`, `errorMsg`, `startedAt`, `finishedAt`, `createdAt`, `updatedAt`.

## Coordinator Action

Coordinator implemented the final-module compatibility seed locally after receiving this sidecar contract, without idle waiting.
