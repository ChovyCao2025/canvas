# P2-080 Conversation Handoff Boundary

Date: 2026-06-07
Scope owner: P2-080 Conversational Session Foundation
Status: P2-080 core-only integration cleanup

## Current Project State

The workspace contains one broad conversation/SCRM implementation area with several overlapping product slices:

- P2-080 Conversational Session Foundation
- P2-082D SCRM Operator Workspace
- P2-082D2 Private Domain Contact Group Sync
- P2-082K SCRM Routing And SLA
- P2-082L SCRM AI Reply Assistance

These slices share the same Java package, DAL naming family, workspace controller surface, frontend `/conversations` page, and focused verification script. Continuing implementation from multiple sessions without a file boundary will duplicate work and make ownership unclear.

This handoff intentionally does not claim new product implementation. It records the current boundary so later sessions can stop repeating the same conversation/SCRM work, and it defines a core-only integration package that can be reviewed without dragging P2-082D/D2/K/L into the same change.

## P2-080 Goal

P2-080 is the reusable conversation foundation that lets inbound customer replies resume canvas execution through existing WAIT semantics.

The core outcome is:

- Persist tenant-scoped conversation sessions and messages.
- Normalize inbound provider replies to `CONVERSATION_REPLY`.
- Resume existing `WAIT UNTIL_EVENT` flows through `WaitResumeService`.
- Provide internal/operator ingress and recent-session inspection APIs.
- Provide a provider adapter framework for future conversational channels.
- Provide focused verification and provider scaffold scripts.

## Completed P2-080 Functionality

P2-080 core:

- Conversation session persistence.
- Conversation message persistence.
- `ConversationIngressService` for inbound reply normalization.
- `CONVERSATION_REPLY` event emission into WAIT resume.
- Internal conversation ingress API.
- Recent conversation session/message read APIs.
- Provider adapter framework:
  - `ConversationReplyAdapter`
  - `ProviderConversationReplyPayload`
  - `AbstractProviderConversationReplyAdapter`
  - `ConversationReplyAdapterSupport`
  - `ConversationAdapterCatalog`
  - `ConversationAdapterHarness`
- Provider adapter fixtures and contract matrix for:
  - Sandbox
  - WhatsApp
  - Social DM
  - Web Chat
  - RCS
- WhatsApp minimum bridge:
  - webhook payload mapper
  - webhook verify-token/signature shell
  - provider/public webhook controller path
  - outbound connector skeleton
  - status receipt bridge coverage
- Frontend conversation workspace foundation:
  - `frontend/src/services/conversationApi.ts`
  - `frontend/src/pages/conversations/`
  - conversation presentation helpers and tests
- Focused tooling:
  - `scripts/verify-conversation-focus.sh`
  - `scripts/verify-p2-080-handoff-boundary.sh`
  - `scripts/scaffold-conversation-provider.sh`

## P2-080-Owned File Groups

Treat these as the P2-080 foundation area unless a later product slice explicitly owns an extension inside the same file.

Docs:

- `docs/product-evolution/specs/p2-080-conversational-session-foundation.md`
- `docs/product-evolution/plans/p2-080-conversational-session-foundation-plan.md`
- `docs/product-evolution/evidence/p2-080-conversation-handoff-boundary.md`
- `docs/product-evolution/evidence/p2-080-staging-pathspec.txt`
- `docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt`

Schema:

- `backend/canvas-engine/src/main/resources/db/migration/V270__conversation_session_foundation.sql`

Backend domain:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressResp.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSessionView.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationMessageView.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapter.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ProviderConversationReplyPayload.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/AbstractProviderConversationReplyAdapter.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupport.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalog.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterContext.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarness.java`
- provider adapter and payload pairs for Sandbox, WhatsApp, Social DM, Web Chat, and RCS
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityService.java`

Backend DAL:

- `ConversationSessionDO` and mapper
- `ConversationMessageDO` and mapper

Backend web:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java`

Backend runtime/channel:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/HttpWhatsAppCloudApiClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnector.java`

Tests and fixtures:

- `ConversationSessionSchemaTest`
- `ConversationIngressServiceTest`
- provider adapter tests
- adapter contract matrix tests
- WhatsApp webhook mapper/security tests
- conversation provider/public webhook controller tests
- `backend/canvas-engine/src/test/resources/conversation/adapter-contracts/`

Frontend:

- `frontend/src/services/conversationApi.ts`
- `frontend/src/services/conversationApi.test.ts`
- `frontend/src/services/conversationCoreApi.ts`
- `frontend/src/services/conversationCoreApi.test.ts`
- `frontend/src/pages/conversations/ConversationCoreInspectionPanel.tsx`
- `frontend/src/pages/conversations/ConversationCoreInspectionPanel.test.tsx`
- `frontend/src/pages/conversations/conversationCorePresentation.ts`
- `frontend/src/pages/conversations/conversationCorePresentation.test.ts`
- `frontend/src/pages/conversations/conversationPresentation.ts`
- `frontend/src/pages/conversations/conversationPresentation.test.ts`
- `frontend/src/pages/conversations/index.tsx`
- `frontend/src/pages/conversations/index.test.tsx`

Scripts:

- `scripts/verify-conversation-focus.sh`
- `scripts/scaffold-conversation-provider.sh`

## Overlapping Slices That Should Not Be Reimplemented As P2-080

P2-082D SCRM Operator Workspace:

- Spec/plan:
  - `docs/product-evolution/specs/p2-082d-scrm-operator-workspace.md`
  - `docs/product-evolution/plans/p2-082d-scrm-operator-workspace-plan.md`
- Schema:
  - `V305__scrm_operator_workspace.sql`
- Typical code:
  - `ConversationWorkspaceService`
  - `ConversationWorkspaceController`
  - work item, contact profile, SOP task, timeline, assignment, status, audit records

P2-082D2 Private Domain Contact Group Sync:

- Spec/plan:
  - `docs/product-evolution/specs/p2-082d2-private-domain-contact-group-sync.md`
  - `docs/product-evolution/plans/p2-082d2-private-domain-contact-group-sync-plan.md`
- Schema:
  - `V307__private_domain_contact_group_sync.sql`
- Typical code:
  - `ConversationPrivateDomainSyncService`
  - `ConversationPrivateDomainController`
  - private contact/group/member/sync-run DOs, mappers, views, and tests

P2-082K SCRM Routing And SLA:

- Spec/plan:
  - `docs/product-evolution/specs/p2-082k-scrm-routing-and-sla.md`
  - `docs/product-evolution/plans/p2-082k-scrm-routing-and-sla-plan.md`
- Schema:
  - `V335__scrm_routing_sla.sql`
- Typical code:
  - `ConversationRoutingService`
  - routing agent/rule commands and views
  - SLA breach views and tests
  - routing/SLA endpoints on workspace controller

P2-082L SCRM AI Reply Assistance:

- Spec/plan:
  - `docs/product-evolution/specs/p2-082l-scrm-ai-reply-assistance.md`
  - `docs/product-evolution/plans/p2-082l-scrm-ai-reply-assistance-plan.md`
- Schema:
  - `V318__scrm_ai_reply_assistance.sql`
- Typical code:
  - `ConversationAiReplyService`
  - `ConversationAiReplyGenerator`
  - `LlmConversationAiReplyGenerator`
  - AI suggestion commands, views, DO, mapper, tests
  - frontend AI suggestion controls under `/conversations`

## Explicitly Out Of Scope For This Handoff

Do not continue these as part of P2-080:

- QuickBI, BI, embedded analytics, dashboarding, dataset query engines.
- CDP warehouse, OLAP, Flink, realtime warehouse, Doris evidence automation.
- Attribution, paid media, search marketing, programmatic DSP.
- Loyalty, growth activity center, campaign master ledger.
- Monitoring, social listening, sentiment, anomaly detection, alerting.
- Approval workflow, risk-control engine, content hub.
- New SCRM workspace features beyond documenting boundaries.

## Verification Evidence

Last known focused verification from the handoff session:

```bash
scripts/verify-conversation-focus.sh --backend-schema-only
scripts/verify-conversation-focus.sh --frontend-conversation-only
scripts/verify-conversation-focus.sh
```

Reported results:

- Backend schema-only: 5 conversation schema tests passed.
- Frontend conversation-only: 6 files, 21 tests passed.
- Full conversation-focused gate: backend Surefire 191 tests passed; frontend Vitest 8 files, 34 tests passed.
- Fresh handoff rerun on 2026-06-07 11:56 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` again passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Core-only integration cleanup rerun on 2026-06-07 12:15 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Handoff evidence refresh on 2026-06-07 12:31 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Handoff boundary tooling refresh on 2026-06-07 12:52 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 74 P2-080 staging entries, 81 deferred-overlap entries, and 441 staged files with no P2-080 pathspec overlap; full `scripts/verify-conversation-focus.sh` also passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Handoff boundary tooling refresh on 2026-06-07 13:04 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 76 P2-080 staging entries, 81 deferred-overlap entries, and 531 staged files with no P2-080 pathspec overlap.
- Core frontend API split on 2026-06-07 13:00 Asia/Shanghai: `scripts/verify-conversation-focus.sh --frontend-logic-only` passed with 5 files / 17 tests after adding a P2-080-only `conversationCoreApi` service and test.
- Core frontend API full rerun on 2026-06-07 13:05 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 9 files / 35 tests passed.
- Core frontend presentation split on 2026-06-07 13:14 Asia/Shanghai: `scripts/verify-conversation-focus.sh --frontend-logic-only` passed with 6 files / 21 tests after adding a P2-080-only `conversationCorePresentation` helper and test.
- Core frontend split full rerun on 2026-06-07 13:18 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 78 P2-080 staging entries, 81 deferred-overlap entries, and 441 staged files with no P2-080 pathspec overlap; full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 10 files / 39 tests passed.
- Core frontend presentation full rerun on 2026-06-07 13:16 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 10 files / 39 tests passed.
- Handoff boundary rerun on 2026-06-07 13:17 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 78 P2-080 staging entries, 81 deferred-overlap entries, and 0 staged files.
- Core frontend inspection panel split on 2026-06-07 13:24 Asia/Shanghai: `scripts/verify-conversation-focus.sh --frontend-conversation-only` passed with 9 files / 27 tests after adding a P2-080-only `ConversationCoreInspectionPanel` component and test.
- Core frontend inspection panel full rerun on 2026-06-07 13:29 Asia/Shanghai: `scripts/verify-conversation-focus.sh --dry-run` passed; full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 11 files / 40 tests passed; `scripts/verify-p2-080-handoff-boundary.sh` passed with 81 P2-080 staging entries, 81 deferred-overlap entries, and 0 staged files.
- Product index hunk boundary on 2026-06-07 13:33 Asia/Shanghai: `docs/product-evolution/evidence/p2-080-product-index-hunks.md` captured the three P2-080-only rows for implementation, specs, and plans indexes; `scripts/verify-p2-080-handoff-boundary.sh` passed with 81 P2-080 staging entries, 81 deferred-overlap entries, and 0 staged files.
- The verifier now derives compiled main/test `.class` targets from declared Java `package` values and fails early if any selected backend `*Test` source did not compile before Surefire runs. This guards path/package drift such as a test source living under one directory while declaring another package.

Before claiming merge readiness, rerun the full focused gate from current workspace state because the repository is dirty and several files are untracked.

## Worktree Risk

The current worktree is heavily dirty and includes many unrelated modified/untracked files. A later session must not use broad git operations or broad formatters to "clean up" this area.

Known risk pattern:

- Conversation/SCRM files are mostly untracked, so another session can unknowingly duplicate or overwrite them.
- P2-080, P2-082D, P2-082D2, P2-082K, and P2-082L all share `org.chovy.canvas.domain.conversation`.
- `ConversationWorkspaceController`, `frontend/src/pages/conversations/index.tsx`, and `frontend/src/services/conversationApi.ts` are extension points for multiple slices.
- Verification script scope intentionally includes more than P2-080 so it can catch regressions across the shared conversation/SCRM area.

## Recommended Packaging Plan

Do not implement another conversation/SCRM feature from this session.

Recommended non-duplicate packaging actions:

1. Review this handoff boundary.
2. Decide whether the next active session owns P2-080 only or one named P2-082 sub-slice.
3. If preparing for commit, stage only one slice at a time:
   - P2-080 core foundation first.
   - P2-082D workspace separately.
   - P2-082D2 private-domain sync separately.
   - P2-082K routing/SLA separately.
   - P2-082L AI reply assistance separately.
4. Rerun the relevant focused verifier after each slice is isolated.

## Core-Only Integration Checklist

Use this checklist before staging or asking another session to continue P2-080:

- Confirm the change contains only foundation behavior:
  - conversation session/message schema and DAL;
  - inbound reply normalization and idempotency;
  - WAIT resume through `CONVERSATION_REPLY`;
  - internal/operator ingress and recent session/message inspection;
  - provider adapter harness, contract fixtures, scaffold, and minimal provider skeletons;
  - minimal WhatsApp acceleration templates documented in the P2-080 spec;
  - frontend API/presentation/page files required to inspect sessions and exercise the conversation WAIT preset.
- Confirm the change does not introduce new SCRM product behavior:
  - no new workspace lifecycle beyond inspection needed for P2-080;
  - no new private-domain sync behavior;
  - no new routing/SLA behavior;
  - no new AI reply generation/review behavior.
- Run the pathspec validation command below and resolve any `MISSING` entries before review.
- Inspect the diff for every shared file before staging:
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md`;
  - `docs/product-evolution/specs/INDEX.md`;
  - `docs/product-evolution/plans/INDEX.md`;
  - `frontend/src/pages/conversations/index.tsx`;
  - `frontend/src/services/conversationApi.ts`;
  - `scripts/verify-conversation-focus.sh`.
- Run `scripts/verify-conversation-focus.sh` from the repository root after any source or verifier change.
- If staging is requested, stage this P2-080 core package first and keep P2-082D/D2/K/L files out of the same staged set.

Suggested first P2-080 core package:

- P2-080 spec, plan, boundary evidence, and index rows.
- `docs/product-evolution/evidence/p2-080-staging-pathspec.txt`, the review-only pathspec list for isolating the P2-080 core package.
- `V270__conversation_session_foundation.sql`.
- Conversation session/message DOs, mappers, ingress request/response/view types, `ConversationIngressService`, and related schema/service/controller tests.
- `ConversationReplyAdapter`, `ProviderConversationReplyPayload`, `AbstractProviderConversationReplyAdapter`, `ConversationReplyAdapterSupport`, `ConversationAdapterCatalog`, `ConversationAdapterHarness`, provider payload/adapters for Sandbox, WhatsApp, Social DM, Web Chat, and RCS, adapter fixtures, and adapter matrix tests.
- Minimal WhatsApp bridge files that are documented as P2-080 acceleration templates: payload mapper, webhook security shell, provider/public webhook controllers, Cloud API connector/client, and receipt bridge tests.
- Frontend conversation API/presentation/page files and conversation WAIT authoring preset tests needed by the focused verifier.
- `scripts/verify-conversation-focus.sh` and `scripts/scaffold-conversation-provider.sh`.
- `scripts/verify-p2-080-handoff-boundary.sh`, the read-only boundary guard for the staging/deferred pathspec files.

Starter pathspec for a P2-080-only staging review:

```bash
git status --short -- \
  docs/product-evolution/IMPLEMENTATION_ORDER.md \
  docs/product-evolution/specs/INDEX.md \
  docs/product-evolution/plans/INDEX.md \
  docs/product-evolution/specs/p2-080-conversational-session-foundation.md \
  docs/product-evolution/plans/p2-080-conversational-session-foundation-plan.md \
  docs/product-evolution/evidence/p2-080-conversation-handoff-boundary.md \
  docs/product-evolution/evidence/p2-080-staging-pathspec.txt \
  backend/canvas-engine/src/main/resources/db/migration/V270__conversation_session_foundation.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationSessionDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationMessageDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationSessionMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationMessageMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressResp.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSessionView.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationMessageView.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ProviderConversationReplyPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/AbstractProviderConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupport.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalog.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterContext.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarness.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SandboxConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SandboxConversationReplyPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/RcsConversationReplyAdapter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/RcsConversationReplyPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiClient.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/HttpWhatsAppCloudApiClient.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnector.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationSessionSchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationIngressServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalogTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarnessTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractMatrixTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractSupport.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/AbstractProviderConversationReplyAdapterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupportTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyAdapterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapperTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyAdapterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyAdapterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/RcsConversationReplyAdapterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationProviderWebhookControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/PublicConversationWebhookControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnectorTest.java \
  backend/canvas-engine/src/test/resources/conversation/adapter-contracts \
  frontend/src/services/conversationApi.ts \
  frontend/src/services/conversationApi.test.ts \
  frontend/src/services/conversationCoreApi.ts \
  frontend/src/services/conversationCoreApi.test.ts \
  frontend/src/pages/conversations/ConversationCoreInspectionPanel.tsx \
  frontend/src/pages/conversations/ConversationCoreInspectionPanel.test.tsx \
  frontend/src/pages/conversations/conversationCorePresentation.ts \
  frontend/src/pages/conversations/conversationCorePresentation.test.ts \
  frontend/src/pages/conversations/conversationPresentation.ts \
  frontend/src/pages/conversations/conversationPresentation.test.ts \
  frontend/src/pages/conversations/index.tsx \
  frontend/src/pages/conversations/index.test.tsx \
  frontend/src/components/node-panel/nodeLibrary.ts \
  frontend/src/components/node-panel/nodeLibrary.test.ts \
  frontend/src/pages/canvas-editor/insertNode.test.ts \
  scripts/verify-conversation-focus.sh \
  scripts/verify-p2-080-handoff-boundary.sh \
  scripts/scaffold-conversation-provider.sh
```

Do not turn that pathspec directly into a blind `git add`. First inspect `git diff -- <same paths>` or use `xargs git diff -- < docs/product-evolution/evidence/p2-080-staging-pathspec.txt`, then inspect `git diff --cached --name-only` because some shared files may include P2-082D/D2/K/L additions that need to be split or deferred.

Pathspec validation command:

```bash
cat docs/product-evolution/evidence/p2-080-staging-pathspec.txt |
  while IFS= read -r item; do
    [ -e "$item" ] || printf 'MISSING %s\n' "$item"
  done
```

Latest pathspec validation result on 2026-06-07 13:29 Asia/Shanghai: `p2-080-staging-pathspec.txt` contained 81 entries and produced no `MISSING` lines.

Defer or split into the named P2-082 slice when staging:

- Workspace work item, timeline, assignment, SOP, contact profile, or audit extensions belong to P2-082D unless they are only thin read APIs needed to inspect P2-080 sessions/messages.
- Private-domain contact/group/member/sync-run code belongs to P2-082D2.
- Routing agent/rule/SLA breach code belongs to P2-082K.
- AI reply generation, suggestions, review workflow, and LLM generator code belongs to P2-082L.
- BI/CDP/OLAP/Flink, risk-control, content hub, paid media, search marketing, DSP, loyalty, growth activity, monitoring, approval workflow, and unrelated runtime cleanup remain outside this handoff.

Known deferred overlap pathspec:

- `docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt` lists current conversation/SCRM files that should be kept out of the first P2-080 core package and reviewed under P2-082D/D2/K/L ownership.
- The deferred list is a staging boundary aid only. It is not a delete list, not a rollback list, and not proof that those later slices are complete.
- Shared files such as `frontend/src/pages/conversations/index.tsx`, `frontend/src/services/conversationApi.ts`, and `scripts/verify-conversation-focus.sh` can still contain mixed P2-080/P2-082 concerns; inspect their diffs manually before staging.

Deferred pathspec validation command:

```bash
cat docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt |
  while IFS= read -r item; do
    [ -e "$item" ] || printf 'MISSING %s\n' "$item"
  done
```

Latest deferred pathspec validation result on 2026-06-07 13:29 Asia/Shanghai: `p2-080-deferred-overlap-pathspec.txt` contained 81 entries, produced no `MISSING` lines, and had no path overlap with `p2-080-staging-pathspec.txt`.

## Resume Command Sequence

Use this sequence when another session resumes the P2-080 handoff. These commands inspect and verify; they do not stage or commit anything.

```bash
# 1. Verify the handoff boundary pathspecs and staged-overlap guard.
scripts/verify-p2-080-handoff-boundary.sh

# 2. Inspect the P2-080 core candidate set.
cat docs/product-evolution/evidence/p2-080-staging-pathspec.txt |
  xargs git status --short --

# 3. Inspect the known overlap set that should stay out of the first P2-080 core package.
cat docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt |
  xargs git status --short --

# 4. Inspect tracked diffs in the P2-080 candidate set.
# Untracked files still need direct review from the status output.
cat docs/product-evolution/evidence/p2-080-staging-pathspec.txt |
  xargs git diff --name-only --

# 5. Confirm nothing has already been staged.
git diff --cached --name-only

# 6. Re-run the focused gate before claiming readiness.
scripts/verify-conversation-focus.sh
```

If step 1 includes shared files such as `frontend/src/pages/conversations/index.tsx`, `frontend/src/services/conversationApi.ts`, `frontend/src/components/node-panel/nodeLibrary.ts`, or `scripts/verify-conversation-focus.sh`, inspect the actual diff before staging because these files may carry both P2-080 and later P2-082 behavior.

`scripts/verify-p2-080-handoff-boundary.sh` is a pre-staging guard. It intentionally fails if already-staged files overlap either P2-080 pathspec, so run it before staging P2-080 or after intentionally unstaging a reviewed P2-080 package.

If step 5 is non-empty, stop before any staging decision and compare the staged list against both pathspec files. Latest observation on 2026-06-07 13:29 Asia/Shanghai: the worktree had 0 staged files, and `scripts/verify-p2-080-handoff-boundary.sh` passed with 81 P2-080 staging entries and 81 deferred-overlap entries.

Concrete P2-082 paths that must not be staged as P2-080 core:

```text
docs/product-evolution/specs/p2-082d-scrm-operator-workspace.md
docs/product-evolution/plans/p2-082d-scrm-operator-workspace-plan.md
docs/product-evolution/specs/p2-082d2-private-domain-contact-group-sync.md
docs/product-evolution/plans/p2-082d2-private-domain-contact-group-sync-plan.md
docs/product-evolution/specs/p2-082k-scrm-routing-and-sla.md
docs/product-evolution/plans/p2-082k-scrm-routing-and-sla-plan.md
docs/product-evolution/specs/p2-082l-scrm-ai-reply-assistance.md
docs/product-evolution/plans/p2-082l-scrm-ai-reply-assistance-plan.md
backend/canvas-engine/src/main/resources/db/migration/V305__scrm_operator_workspace.sql
backend/canvas-engine/src/main/resources/db/migration/V307__private_domain_contact_group_sync.sql
backend/canvas-engine/src/main/resources/db/migration/V318__scrm_ai_reply_assistance.sql
backend/canvas-engine/src/main/resources/db/migration/V335__scrm_routing_sla.sql
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceService.java
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncService.java
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingService.java
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyService.java
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/LlmConversationAiReplyGenerator.java
backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java
backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceSchemaTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceServiceTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSchemaTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncServiceTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationRoutingSchemaTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationRoutingServiceTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAiReplySchemaTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAiReplyServiceTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationWorkspaceControllerTest.java
backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationPrivateDomainControllerTest.java
```

Shared frontend files may contain both P2-080 inspection behavior and later P2-082 workspace/AI controls. If the diff contains AI suggestion filtering, work-item routing, private-domain sync, assignment, SOP, or SLA UI, split that hunk into the relevant P2-082 slice instead of staging the whole file as P2-080.
