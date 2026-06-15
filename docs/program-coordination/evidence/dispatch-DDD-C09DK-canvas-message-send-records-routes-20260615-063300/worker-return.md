# DDD-C09DK Worker Return

Task: DDD-C09DK `/canvas/message-send-records` route parity

Workers:
- Helmholtz `019ec83e-0647-7680-bd9d-8c19d37455fd`
- Descartes `019ec840-1744-7f33-9996-b4b6be3ae5fe`

Result: DONE_WITH_CONCERNS, read-only contract review only.

Findings:
- Legacy routes are `GET /canvas/message-send-records` and `GET /canvas/message-send-records/{id}`.
- List response shape is `code=0`, `message=success`, `data.total`, `data.list`.
- Missing detail is HTTP 200 with `code=-1`, `message="发送记录不存在: {id}"`, no stable `errorCode`.
- `channel` and `status` are ignored when blank; otherwise they are trimmed and uppercased for filtering.
- `executionId` and `userId` are ignored when blank, but not trimmed or normalized when used.
- Paging is one-based; `page` clamps to `>= 1`, `size` clamps to `[1, 100]`.
- Date filters are inclusive on `createdAt`; result order is `createdAt DESC`.

Coordinator action:
- Implemented exact final-module route parity locally after one bounded Helmholtz wait timeout, without idle polling.
- Did not modify `backend/canvas-engine/**` or any `pom.xml`.
