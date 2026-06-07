# SCRM AI Reply Assistance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add human-reviewed AI reply suggestions to the SCRM operator workspace.

**Architecture:** Keep conversation sending and workspace lifecycle unchanged. Add an AI suggestion ledger and service that reads the existing timeline, delegates draft generation behind a small interface, stores the draft with risk evidence, and records review decisions through the existing work-item audit table.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor Test.

---

## Scope

This plan implements P2-082L backend first slice plus the operator-workspace frontend surface:

- Product specs and indexes.
- Additive AI reply suggestion schema.
- Suggestion data object, mapper, commands, and views.
- Pluggable reply generator and LLM-backed implementation.
- Suggestion generation, review, and list service.
- Workspace controller endpoints.
- Focused backend tests and SCRM regression.
- Frontend workspace API, presentation helpers, and `/conversations` AI suggestion controls.

## Files

- Create `docs/product-evolution/specs/p2-082l-scrm-ai-reply-assistance.md`.
- Create `docs/product-evolution/plans/p2-082l-scrm-ai-reply-assistance-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V318__scrm_ai_reply_assistance.sql`.
- Create `ConversationAiReplySuggestionDO` and mapper.
- Create AI reply command, generator, result, context, and view records.
- Create `LlmConversationAiReplyGenerator`.
- Create `ConversationAiReplyService`.
- Modify `ConversationWorkspaceController`.
- Create `ConversationAiReplySchemaTest`.
- Create `ConversationAiReplyServiceTest`.
- Modify `ConversationWorkspaceControllerTest`.
- Modify `frontend/src/services/conversationApi.ts`.
- Modify `frontend/src/services/conversationApi.test.ts`.
- Modify `frontend/src/pages/conversations/conversationPresentation.ts`.
- Modify `frontend/src/pages/conversations/conversationPresentation.test.ts`.
- Modify `frontend/src/pages/conversations/index.tsx`.
- Modify `frontend/src/pages/conversations/index.test.tsx`.

## Tasks

### Task 1: Index P2-082L Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082L after P2-082K in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with AI reply assistance slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082L|p2-082l-scrm-ai-reply-assistance"`**

### Task 2: Add AI Reply Schema With TDD

- [x] **Step 1: Write failing `ConversationAiReplySchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add migration, DO, and mapper**
- [x] **Step 4: Verify GREEN**

### Task 3: Add AI Reply Service With TDD

- [x] **Step 1: Write failing service tests for generation, risk flags, no auto-send, review audit, and tenant isolation**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement generator contracts, LLM adapter, and `ConversationAiReplyService`**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Controller APIs With TDD

- [x] **Step 1: Write failing controller tests for generate, review, and list**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add endpoints to `ConversationWorkspaceController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Backend Slice And Update Parent Docs

- [x] **Step 1: Run focused P2-082L backend tests**
- [x] **Step 2: Run SCRM workspace/routing regression tests**
- [x] **Step 3: Update P2-082L and parent docs to delivered after verification passes**

Verification:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationAiReplySchemaTest,ConversationAiReplyServiceTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest,ConversationRoutingSchemaTest,ConversationRoutingServiceTest,ConversationIngressServiceTest test
```

Result: 38 tests, 0 failures, 0 errors, 0 skipped.

### Task 6: Add Frontend AI Reply Workspace Controls With TDD

- [x] **Step 1: Write failing frontend API, presentation, and page tests**

Cover:

- `generateConversationAiReplySuggestion`, `listConversationAiReplySuggestions`, and `reviewConversationAiReplySuggestion` call the backend workspace endpoints.
- AI suggestion status and bounded list filters are formatted consistently.
- Opening a work-item timeline loads DRAFT suggestions, renders generated text and risk flags, generates a suggestion, and sends an explicit accept/reject review decision.

RED verification:

```bash
cd frontend
PATH=/opt/homebrew/bin:$PATH npm run test -- --run src/services/conversationApi.test.ts src/pages/conversations/conversationPresentation.test.ts src/pages/conversations/index.test.tsx
```

Result before implementation: 3 failed tests because `generateConversationAiReplySuggestion`, `formatAiReplySuggestionStatus`, and page AI suggestion loading were missing.

- [x] **Step 2: Implement frontend API, helpers, and drawer controls**

Add AI reply suggestion types, workspace service functions, status/filter helpers, and a compact AI suggestion section in the `/conversations` timeline drawer. The UI lists DRAFT suggestions by default, can switch suggestion history between DRAFT, ACCEPTED, and REJECTED, shows confidence and risk flags, and exposes generate, accept, and reject actions. It does not send outbound messages.

- [x] **Step 3: Verify frontend GREEN and focused conversation gate**

```bash
cd frontend
PATH=/opt/homebrew/bin:$PATH npm run test -- --run src/services/conversationApi.test.ts src/pages/conversations/conversationPresentation.test.ts src/pages/conversations/index.test.tsx
scripts/verify-conversation-focus.sh --frontend-only
scripts/verify-conversation-focus.sh --backend-only
scripts/verify-conversation-focus.sh
```

Results on 2026-06-07:

- Targeted frontend GREEN: 3 files, 15 tests passed.
- Frontend focused gate: 8 files, 34 tests passed.
- Backend focused gate: 191 tests, 0 failures, 0 errors, 0 skipped.
- Full conversation focused gate: backend 191 tests passed; frontend 8 files, 34 tests passed.
