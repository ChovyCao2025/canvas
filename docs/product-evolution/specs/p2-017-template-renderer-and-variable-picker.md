# P2-017 - Template Renderer And Variable Picker Spec

Priority: P2
Sequence: 017
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p2-017-template-renderer-and-variable-picker-plan.md`

## Goal

Use one runtime-safe template renderer for execution and preview, and expose only DAG-available variables in the editor.

## Current Baseline

- Message nodes pass variables to delivery payloads, but there is no shared renderer.
- Config panel fields can accept text, but there is no variable picker constrained by graph position.

## In Scope

- `TemplateRenderService` with variable interpolation, date formatting, list rendering, conditionals, escaping, max output length, and typed validation errors.
- Frontend `VariablePicker` with trigger payload, upstream outputs, profile fields, computed fields, search, keyboard selection, and preview insertion.

## Out Of Scope

- User input/wait-event nodes; split into P2-017B.
- Connected content; split into P2-017C.
- Rerun/timeline/batch operations; split into P2-017D/P2-017E.

## Acceptance Criteria

- Runtime and preview use the same renderer.
- Picker tests prove downstream fields are hidden.
