# P2-082AB - Provider Write Operations UI Spec

Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082ab-provider-write-operations-ui-plan.md`
Status: Delivered frontend first slice

## Problem

P2-082Y, P2-082Z, and P2-082AA add safe backend mutation ledgers for SEM, creator, and DSP provider writes. Operators can call APIs, but there is no daily operations surface to review pending writes, approve them, dry-run them, and apply mutations after dry-run evidence. That leaves the production control plane incomplete for non-developer marketing teams.

## Scope

Frontend first slice:

- Extend the marketing platform page with a unified provider write operations queue.
- Load SEM, creator, and DSP mutation ledgers from existing backend APIs.
- Normalize the three mutation shapes into one operator table.
- Show approval, execution, failed, and dry-run status KPIs.
- Add approve, dry-run, and apply actions with backend reload after each action.
- Keep provider credentials out of the UI.

## Non-Goals

- Mutation proposal forms.
- Real provider credential setup.
- Bulk approval or bulk apply.
- Editing mutation payloads from the UI.

## Acceptance Criteria

- API tests cover SEM, creator, and DSP list/approve/execute calls.
- Presentation tests cover unified queue normalization, KPIs, and action gating.
- Page test proves the marketing platform page renders provider write operations.
- Frontend focused tests and build pass.
- Browser verification proves the operations queue renders without leaking obvious provider secret fields.

## Delivery Status

Delivered frontend first slice:

- Marketing platform page now loads SEM, creator, and DSP provider mutation ledgers alongside the control-plane summary.
- Unified provider write queue shows gateway, mutation, object scope, approval/execution status, errors, updated time, and operator actions.
- Queue KPIs summarize total writes, pending approvals, ready writes, dry-run success, and failures.
- Operators can approve, dry-run, and live-apply via the correct gateway API, with live apply behind a confirmation.
- Browser verification rendered the queue with SEM, creator, and DSP rows, showed fail-closed provider errors, and executed the SEM approve -> dry-run -> apply path without layout/control-plane errors.

Remaining outside this slice:

- Mutation proposal forms.
- Real provider credential setup and live provider adapters.
- Bulk approval/apply workflows.
