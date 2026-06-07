# P2-082D - SCRM Operator Workspace Spec

Priority: P2
Sequence: 082D
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082d-scrm-operator-workspace-plan.md`

## Goal

Turn the existing conversation session foundation into a production-usable SCRM/private-domain operator workspace with tenant-scoped inbox work items, assignment, follow-up reminders, SOP tasks, customer timeline, and audit history.

## Research Inputs

- HubSpot Conversations Inbox centers daily operation around a shared inbox that lets teams view, manage, assign, and reply to customer conversations in one place: https://knowledge.hubspot.com/inbox/use-the-conversations-inbox
- HubSpot routing rules leave incoming conversations unassigned for triage by default, then allow routing to users and teams: https://knowledge.hubspot.com/inbox/set-your-conversations-routing-rules
- Intercom Inbox setup highlights assignment methods and team availability as a first-class inbox concern: https://www.intercom.com/help/en/articles/10223008-setting-up-the-inbox
- Intercom conversation assignment supports assigning conversations to an admin and/or team: https://developers.intercom.com/docs/references/2.3/rest-api/conversations/assign-a-conversation
- Zendesk SLA and audit models show that status, priority, response targets, and auditable event history are core support-workspace primitives:
  - https://support.zendesk.com/hc/en-us/articles/5600997516058-About-SLA-policies-and-how-they-work
  - https://developer.zendesk.com/documentation/ticketing/reference-guides/ticket-audit-events-reference/
- Salesforce Omni-Channel treats routed customer work as prioritized work items assigned to qualified/available reps: https://developer.salesforce.com/docs/atlas.en-us.omni_channel_dev.meta/omni_channel_dev/omnichannel_developer_guide_intro.htm

## Current Baseline

P2-080 already provides:

- `conversation_session` and `conversation_message` persistence.
- `ConversationIngressService` for idempotent inbound replies.
- WAIT resume via `CONVERSATION_REPLY`.
- Recent session and message APIs.
- Adapter catalog, harness, and sandbox/WhatsApp/Web Chat reply adapters.

What is missing is the operator workspace layer. There is no durable inbox work item, no assignment state, no SLA/follow-up field, no SOP task checklist, and no consolidated customer timeline with audit events.

## Product Design

The SCRM workspace slice adds backend primitives and an operator UI that future channel-provider work can build on:

- `conversation_work_item`: one tenant-scoped inbox item per conversation session.
- `conversation_contact_profile`: a lightweight private-domain customer profile keyed by `tenant_id + user_id`.
- `conversation_sop_task`: structured follow-up tasks tied to a work item.
- `conversation_work_item_audit`: immutable operator/system events for creation, assignment, status changes, reminders, and task updates.

Inbound replies should be able to create or update a work item automatically. Operators should also be able to create a work item for an existing session, assign it, change status, schedule next follow-up, create tasks, complete tasks, and fetch a customer timeline.

The frontend exposes these capabilities as a compact SCRM inbox under `/conversations`: operators can filter active work items, open a customer timeline drawer, assign owners/teams, update status and reminder fields, create SOP tasks, complete tasks, and inspect messages plus audit history without leaving the workspace.

## Status And Priority Semantics

Work item statuses:

- `OPEN`: actionable and visible in the active inbox.
- `PENDING`: waiting for customer or internal dependency.
- `SNOOZED`: hidden from the active queue until `next_follow_up_at`.
- `RESOLVED`: completed and retained for history.

Priorities:

- `LOW`
- `NORMAL`
- `HIGH`
- `URGENT`

The first slice does not implement automatic capacity routing. It records enough assignment, priority, and reminder state for a later routing engine.

## API Contract

### Ensure Work Item

`POST /canvas/conversations/workspace/sessions/{sessionId}/work-item`

Creates or returns the inbox work item for a conversation session.

### Inbox

`GET /canvas/conversations/workspace/inbox?status=OPEN&assignedTo=alice&channel=WEB_CHAT&limit=50`

Returns tenant-scoped work items ordered by priority and last customer activity.

### Assign

`POST /canvas/conversations/workspace/work-items/{workItemId}/assign`

```json
{
  "assignedTo": "alice",
  "assignedTeam": "sales",
  "note": "VIP pricing request"
}
```

### Update Status And Reminder

`POST /canvas/conversations/workspace/work-items/{workItemId}/status`

```json
{
  "status": "SNOOZED",
  "priority": "HIGH",
  "nextFollowUpAt": "2026-06-07T09:30:00",
  "note": "Follow up after demo slot confirmation"
}
```

### Create SOP Task

`POST /canvas/conversations/workspace/work-items/{workItemId}/tasks`

```json
{
  "taskKey": "book_demo",
  "title": "Book a product demo",
  "assignee": "alice",
  "dueAt": "2026-06-07T10:00:00",
  "metadata": {
    "playbook": "sales_handoff"
  }
}
```

### Complete SOP Task

`POST /canvas/conversations/workspace/tasks/{taskId}/complete`

```json
{
  "note": "Demo booked for Monday"
}
```

### Timeline

`GET /canvas/conversations/workspace/work-items/{workItemId}/timeline`

Returns the work item, contact profile, conversation session, messages, SOP tasks, and audit events for the same tenant.

## Functional Requirements

1. A session can have at most one work item per tenant.
2. Work item creation must be idempotent.
3. Inbound replies should update `last_customer_message_at`, `status`, and `priority` without overwriting manual assignment.
4. Assignment must record actor, previous owner, new owner, team, and note in audit.
5. Status/reminder changes must validate the status and priority enums and record audit.
6. SOP tasks must be tenant scoped and attached to a valid work item.
7. Completing a task must set `status=DONE`, `completed_by`, `completed_at`, and audit the completion.
8. Timeline reads must reject cross-tenant work items.
9. Inbox reads must be tenant scoped and bounded to 1..100 rows.
10. The slice must not require real WeCom, WhatsApp, web chat, or CRM credentials.

## Out Of Scope

- Real WeCom contact/group sync and group chat APIs.
- Agent capacity routing and skills matching.
- SLA breach scheduler.
- AI reply suggestions.
- Rich reply composer and outbound message sending from the workspace.
- Provider webhook signature verification.

## Acceptance Criteria

- This spec and plan are indexed under P2-082D.
- Migration test proves all four workspace tables, tenant/session uniqueness, inbox lookup indexes, and audit indexes exist.
- Service tests prove idempotent work item creation, assignment audit, status/reminder audit, SOP task creation/completion, timeline tenant isolation, and inbound message work-item updates.
- Controller tests prove every workspace endpoint passes current tenant/operator context and bounds limits.
- Existing P2-080 conversation adapter and ingress tests still pass.
- Focused backend tests pass with Java 21.
- Frontend workspace API tests prove all `/canvas/conversations/workspace` endpoints are called with the backend contract paths and parameters.
- Frontend presentation tests prove status, priority, task, filter, and timeline formatting stay stable.
- Frontend page tests prove the SCRM inbox loads, opens a customer timeline, and renders assignment, status, SOP task, and task-completion controls.
- App shell tests prove `/conversations` has route announcement coverage and a visible navigation entry.

## Delivery Status

Implemented and verified backend first slice:

- Migration `V305__scrm_operator_workspace.sql`.
- Contact profile, work item, SOP task, and audit data objects/mappers.
- `ConversationWorkspaceService` for inbox lifecycle, assignment, reminders, SOP tasks, timeline, audit, and inbound-message workspace updates.
- `ConversationWorkspaceController` under `/canvas/conversations/workspace`.
- Optional `ConversationIngressService` workspace hook for new inbound messages.

Focused backend verification:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceSchemaTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WebChatConversationReplyAdapterTest test
```

Result: 40 tests, 0 failures, 0 errors.

Implemented frontend operator workspace slice:

- `frontend/src/pages/conversations/index.tsx` SCRM inbox and customer timeline drawer.
- Workspace API client functions in `frontend/src/services/conversationApi.ts`.
- Workspace presentation types and helpers in `frontend/src/pages/conversations/conversationPresentation.ts`.
- `/conversations` route, side navigation entry, and route announcement.

Focused frontend verification:

```bash
cd frontend
npm run test -- src/pages/conversations/conversationPresentation.test.ts src/services/conversationApi.test.ts src/pages/conversations/index.test.tsx src/components/layout/AppLayout.a11y.test.tsx src/pages/bi/index.test.tsx
npm run build
```

Result: 5 test files, 17 tests, 0 failures; TypeScript and Vite production build succeeded.
