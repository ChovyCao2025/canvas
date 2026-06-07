# P2-082L - SCRM AI Reply Assistance Spec

Priority: P2
Sequence: 082L
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082l-scrm-ai-reply-assistance-plan.md`

## Goal

Make the SCRM operator workspace faster and safer by adding AI-generated reply suggestions that are grounded in the work-item timeline, risk-scored, tenant-scoped, and always reviewed by a human before use.

## Delivery Status

Delivered backend and workspace frontend slice:

- Additive AI reply suggestion migration `V318__scrm_ai_reply_assistance.sql`.
- Tenant-scoped suggestion persistence with prompt context, grounding snippets, model metadata, risk flags, confidence, and review status.
- Reply generation service that reads the existing work-item timeline and stores a draft suggestion without sending any outbound message.
- Human review workflow for accepting or rejecting suggestions with immutable work-item audit events.
- Workspace controller APIs for generation, review, and listing.
- `/conversations` operator drawer lists DRAFT suggestions by default, can filter suggestion history by DRAFT, ACCEPTED, or REJECTED, shows generated text, confidence, and risk flags, and exposes human-reviewed generate/accept/reject actions without sending messages.

## Current Baseline

P2-082D delivered the operator inbox, timeline, SOP tasks, reminders, assignment, and audit records. P2-082K added routing, skill matching, SLA due time, and breach audit. Operators can see customer context and route work, but they still write every reply manually. The product has a shared `AiLlmGateway`, but the SCRM workspace has no governed AI composer surface, no suggestion ledger, and no human-review audit for AI-assisted replies.

## Research Inputs

- Intercom documents inbox AI features for helping teammates answer questions, troubleshoot, write replies, and automate inbox work: https://www.intercom.com/help/en/articles/6955446-ai-features-available-in-the-inbox
- Zendesk AI can populate suggested first replies in the Agent Workspace composer, based on ticket content: https://support.zendesk.com/hc/en-us/articles/7041677653914-Using-AI-to-generate-a-first-reply-in-a-ticket
- Salesforce Einstein Service Replies drafts responses as a chat unfolds from conversation context or an assigned knowledge source: https://help.salesforce.com/s/articleView?id=service.reply_recs_service_replies_chat_intro.htm&language=en_US&type=5

These products converge on the same operating boundary: AI helps the agent compose a response in context, while the agent remains responsible for review and sending.

## Product Design

Add a suggestion ledger `conversation_ai_reply_suggestion`:

- Scope: `tenant_id`, `work_item_id`, `session_id`, optional `source_message_id`.
- Draft: suggested reply text, tone, intent, confidence, risk flags, grounding snippets, prompt context.
- Model metadata: provider id, model key, template id, provider status, fallback flag, and generated-by actor.
- Review metadata: status, reviewed-by actor, reviewed time, and review note.

Add a generation service:

- Requires a tenant-owned work item.
- Reads the existing workspace timeline.
- Builds a compact context from work item, contact profile, session, recent messages, and open tasks.
- Uses a pluggable generator so tests can be deterministic and production can delegate to `AiLlmGateway`.
- Stores a `DRAFT` suggestion and writes `AI_REPLY_SUGGESTED` audit.
- Never writes to `conversation_message` and never invokes outbound channel adapters.

Add a review workflow:

- Accept or reject only tenant-owned suggestions.
- Valid transitions: `DRAFT` to `ACCEPTED` or `REJECTED`.
- Stores reviewer, review time, note, and writes `AI_REPLY_REVIEWED` audit.
- Acceptance means "approved for operator use"; it does not send the reply.

## Functional Requirements

1. Add additive migration `V318__scrm_ai_reply_assistance.sql`.
2. Create `conversation_ai_reply_suggestion` with tenant, work-item, session, source-message, model, draft, risk, grounding, status, and review columns.
3. Add data object and mapper for reply suggestions.
4. Add command/view records for generation, review, and list filtering.
5. Add `ConversationAiReplyGenerator` with deterministic tests and an LLM-backed implementation.
6. Add `ConversationAiReplyService` for generate, review, and list.
7. Build prompt context from the existing timeline and prefer the latest inbound text message as the source message.
8. Flag missing context and sensitive topics such as refund, legal, privacy, payment, cancellation, or complaint language.
9. Keep suggestions human-in-the-loop; no auto-send side effects.
10. Add workspace controller endpoints for generate, review, and list.
11. Focused backend tests must prove schema, generation persistence, risk flags, no auto-send side effects, review audit, tenant isolation, and controller tenant/operator wiring.
12. Frontend workspace tests must prove AI suggestion API calls, DRAFT suggestion rendering, status-filtered suggestion history, risk flag display, generation, and accept/reject review controls.

## Out Of Scope

- Fully autonomous chatbot replies.
- Outbound message sending or channel adapter integration.
- Knowledge-base retrieval and vector search.
- Rich outbound composer UI and one-click send.
- Model evaluation dashboards and prompt-management UI.

## Acceptance Criteria

- This spec and plan are indexed after P2-082K.
- A work item can produce a tenant-scoped `DRAFT` suggestion from recent conversation context.
- Generation records grounding and risk flags and creates an `AI_REPLY_SUGGESTED` audit event.
- Accept/reject review updates the suggestion and creates an `AI_REPLY_REVIEWED` audit event.
- The SCRM operator workspace exposes DRAFT suggestions, status-filtered suggestion history, generation, and accept/reject review controls while preserving the no-auto-send boundary.
- Cross-tenant work-item and suggestion access is rejected.
- Existing SCRM workspace and routing tests still pass.
- Focused backend and frontend tests pass with Java 21 and the focused frontend Node runtime:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationAiReplySchemaTest,ConversationAiReplyServiceTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest,ConversationRoutingSchemaTest,ConversationRoutingServiceTest,ConversationIngressServiceTest test
scripts/verify-conversation-focus.sh
```

Latest focused result on 2026-06-07: backend Surefire ran 191 tests with 0 failures, 0 errors, and 0 skipped; frontend Vitest ran 8 files with 34 tests passed.
