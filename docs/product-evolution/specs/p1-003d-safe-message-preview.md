# P1-003D - Safe Message Preview Spec

Priority: P1
Sequence: 003D
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003d-safe-message-preview-plan.md`

## Implementation Status

Implemented and focused-verified on 2026-06-05. Verification evidence is recorded in `../plans/p1-003d-safe-message-preview-plan.md`.

## Goal

Let operators preview `SEND_MESSAGE` node content for a selected user/context without sending, enqueueing, or writing send records.

## Current Baseline

- `AbstractSendMessageHandler` and `ReachDeliveryService` can build/send standardized payloads.
- `DataMaskingUtil` recursively masks sensitive output.
- Existing dry-run tooling exercises graph execution, but preview should only render message content and must not trigger side effects.

## In Scope

- Backend preview request/response DTOs.
- `CanvasMessagePreviewService` that finds one `SEND_MESSAGE` node, resolves selected fields and variables against preview context, masks output, and returns warnings.
- `POST /canvas/{id}/message-preview`.
- Frontend helper and editor modal for selected `SEND_MESSAGE` node preview.

## Out Of Scope

- Message template center; covered by P2-005.
- Provider receipts, reconciliation, and production delivery gates; covered by P0-003, P0-005, and P1-008.
- Multi-node dry-run path visualization.

## Functional Requirements

1. Request accepts canvas ID, node ID, user ID, graph JSON, and preview context.
2. Service rejects non-`SEND_MESSAGE` nodes.
3. Response includes channel, template ID, masked content, masked variables, and `PREVIEW_ONLY_NO_SEND`.
4. Preview never calls `ReachDeliveryService.send`, creates no `message_send_record`, and enqueues no execution request.
5. Frontend can preview unsaved graph JSON from the current editor state.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/MessagePreviewResp.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `frontend/src/services/api.ts`
- `frontend/src/pages/canvas-editor/messagePreview.ts`
- `frontend/src/pages/canvas-editor/index.tsx`

## Acceptance Criteria

- Backend tests prove masking, variable resolution, and no-send behavior.
- Frontend helper tests prove request payload generation from graph JSON and context JSON.
- Editor shows preview only for selected `SEND_MESSAGE` nodes.
