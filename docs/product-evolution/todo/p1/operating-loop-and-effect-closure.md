# P1 - Operating Loop And Effect Closure

## Sources

- `product-audit-report-2026-05-31.md`
- `product-best-practice-roadmap-2026-05-31.md`

## Why P1

These items make the platform operationally useful after P0 safety is addressed. They are important for an operator workflow but do not all block initial production hardening.

## Work Items

- Build template browsing and one-click clone from existing seeded templates.
- Add touch preview, audience estimate, test send, and dry-run visibility before publish.
- Add lightweight attribution with conversion event intake and last-touch reporting.
- Add global control group support for incremental effect measurement.
- Add canvas version visibility, diff, and rollback flows.
- Add channel receipt tracking for delivered, opened, clicked, bounced, and failed states.
- Add list search, filters, batch operations, empty states, and localized form validation.
- Add InApp notification delivery instead of log-only behavior.
- Expose audit logs through API and UI for operator review.

## Deferred From Sources

- Full multi-touch attribution, RFM, STO, AI content, AI journey creation, and prediction are deferred to P2/P3 unless a separate validated business priority pulls them forward.
- The roadmap's four-stage plan is treated as source context, not an execution commitment.

## Dependencies

- Attribution depends on conversion event intake and touchpoint recording.
- Control groups depend on attribution metrics and stable audience assignment.
- Template clone depends on existing template metadata and clone API validation.

## Acceptance Criteria

- Operators can create a canvas from a template without manual graph copying.
- Operators can preview likely audience and message output before publishing.
- Operators can answer whether a canvas produced a conversion in a lightweight, last-touch sense.
- Operators can inspect version changes and rollback a bad release.
