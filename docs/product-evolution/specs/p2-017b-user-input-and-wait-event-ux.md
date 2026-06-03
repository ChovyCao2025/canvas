# P2-017B - User Input And Wait Event UX Spec

Priority: P2
Sequence: 017B
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p2-017b-user-input-and-wait-event-ux-plan.md`

## Goal

Add user input nodes and richer wait-for-event configuration with completed and timeout branches.

## Current Baseline

- `WaitHandler` and `WaitSubscriptionService` exist.
- WAIT event matching has basic persistence, but operator-facing filter configuration and user input response storage are missing.

## In Scope

- Migration `V113__user_input_wait_event_tools.sql`.
- `UserInputHandler`.
- User input response storage, duplicate response idempotency, completed branch, timeout branch, and trace output.
- WAIT event filter builder persistence and preview.

## Out Of Scope

- Public anonymous form hosting.
- Template variable picker; P2-017.

## Acceptance Criteria

- Backend tests prove response storage, completed resume, timeout resume, duplicate response idempotency, wait filter persistence, and trace output.
