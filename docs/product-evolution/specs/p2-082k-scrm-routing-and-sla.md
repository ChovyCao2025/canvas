# P2-082K - SCRM Routing And SLA Spec

Priority: P2
Sequence: 082K
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082k-scrm-routing-and-sla-plan.md`

## Goal

Make the SCRM operator workspace production-usable for team operations by adding capacity-aware assignment, skill matching, SLA due-time calculation, and auditable breach escalation on top of existing conversation work items.

## Delivery Status

Delivered backend first slice:

- Additive routing/SLA migration `V335__scrm_routing_sla.sql`.
- Routing agent registry with availability, skills, team, capacity, and current load.
- Routing rule registry with channel, minimum priority, required skills, target team, SLA minutes, and sort order.
- Work-item routing metadata: routing status, required skills, routing reason, routed time, and SLA policy key.
- Capacity-aware and skills-based work item routing with deterministic lowest-load agent selection.
- Routing miss audit when no available agent has all required skills and capacity.
- SLA breach evaluation with idempotent open-breach creation, priority escalation, and work-item audit.
- Workspace controller APIs for routing agent/rule upsert, route, SLA evaluation, and SLA breach query.

## Current Baseline

P2-082D delivered contact profiles, inbox work items, assignment, status/reminder changes, SOP tasks, timeline, and audits. Work items already have `sla_due_at`, `assigned_to`, and `assigned_team`, but the product does not yet know which operators are available, what skills they cover, how many conversations they can hold, how to auto-route a work item, or how to record SLA breaches.

## Research Inputs

- Salesforce Omni-Channel routes work by matching requested skills to reps and considering availability/capacity. AnySearch surfaced the official Salesforce help and Trailhead material for skills-based routing and Omni-Channel work routing.
- Zendesk SLA policies define response and resolution targets for support work and expose breach-oriented SLA states so teams can act before or after missed targets: https://support.zendesk.com/hc/en-us/articles/5600997516058-About-SLA-policies-and-how-they-work
- Intercom team inboxes and assignment workflows emphasize routing to the right team/teammate, workload management, availability, and assignment limits: https://www.intercom.com/help/en/articles/10223008-setting-up-the-inbox
- Freshdesk SLA policy material reinforces using priority and escalation rules to set clear response deadlines for tickets or conversations.

## Product Design

Add routing agents:

- `agentKey`: operator id used by assignment.
- `displayName`: operator-facing name.
- `teamKey`: team/inbox ownership.
- `status`: `AVAILABLE`, `BUSY`, or `OFFLINE`.
- `maxCapacity` and `currentLoad`: capacity model for active work.
- `skills`: normalized skill keys such as `sales`, `vip`, `refund`, `wecom`, `english`.

Add routing rules:

- `ruleKey`: stable rule id.
- `channel`: optional channel constraint.
- `priority`: minimum work-item priority for the rule.
- `requiredSkills`: skills the matched agent must have.
- `targetTeam`: optional team constraint.
- `slaMinutes`: first-response due time for matched work.
- `sortOrder`: deterministic rule precedence.

Add SLA breaches:

- One open breach record per tenant/work item/breach type.
- Breach rows store due time, breach time, severity, status, escalation target, reason, and metadata.
- Breach creation writes a `SLA_BREACHED` audit event on the work item.

The routing service exposes:

- Upsert agent.
- Upsert routing rule.
- Route one work item by matching a rule and choosing an available, under-capacity, skill-compatible agent.
- Evaluate SLA breaches for due work items.
- Query SLA breach records.

## Functional Requirements

1. Add additive migration `V335__scrm_routing_sla.sql`.
2. Create `conversation_routing_agent`, `conversation_routing_rule`, and `conversation_sla_breach`.
3. Add routing metadata columns to `conversation_work_item`: `routing_status`, `required_skills_json`, `routing_reason`, `routed_at`, and `sla_policy_key`.
4. Extend `ConversationWorkItemDO` and `ConversationWorkItemView` with the new routing/SLA fields.
5. Add data objects and mappers for routing agents, rules, and breaches.
6. Add `ConversationRoutingService` for agent/rule upsert, work-item routing, SLA breach evaluation, and breach query.
7. Route only tenant-owned, unresolved work items.
8. Match required skills as an all-of set; choose available agents with remaining capacity, lowest current load, then stable `agentKey`.
9. Routing updates work item assignment, assigned team, SLA due time, routing status, required skills, routing reason, and audit history.
10. SLA breach evaluation creates a breach only once for an already-open breach and raises work-item priority to at least `HIGH`.
11. Add workspace controller endpoints for agent upsert, rule upsert, work-item route, breach evaluation, and breach query.
12. Focused backend tests must prove schema, routing selection, no-capacity behavior, SLA breach idempotency, tenant isolation, and controller tenant/operator wiring.

## Out Of Scope

- Live presence subscription or websocket-based operator availability.
- Shift calendars, business-hours calendars, holidays, and regional working time.
- AI reply suggestions and reply composer automation.
- Frontend routing/SLA management UI.
- External notification fanout for SCRM SLA breaches.

## Acceptance Criteria

- This spec and plan are indexed after P2-082J.
- Routing schema is additive and leaves existing workspace behavior compatible.
- A work item can be routed to the lowest-load available agent with all required skills.
- A work item with no capacity remains unassigned and records a routing miss audit.
- SLA evaluation creates tenant-scoped breach records idempotently.
- Existing workspace tests still pass.
- Focused backend tests pass with Java 21.
