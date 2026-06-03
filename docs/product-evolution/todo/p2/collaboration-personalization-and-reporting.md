# P2 - Collaboration Personalization And Reporting

## Sources

- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`
- `product-strategy-supplementary-dimensions-2026-05-31.md`

## Why P2

These features improve team productivity and analytical depth but should follow the P0/P1 safety and operator-visibility work.

## Work Items

- Add edit locks, presence, canvas comments, share links, and change notifications.
- Add user preference infrastructure for theme, sidebar state, notification preferences, recent nodes, editor layout, and list defaults.
- Add onboarding tour and contextual help once the core flows are stable.
- Add behavior analytics, chart expansion, report export, chart/table linking, and report builder only after the data contracts are stable.
- Add message template market, variables metadata, and approval integration after basic template CRUD and clone flows are stable.
- Add audience operations, snapshots, freshness monitoring, and 360-view improvements after existing audience APIs are validated.

## Deferred From Sources

- L3 CRDT, full mobile editor, PWA push, and advanced AI reporting are not included here.
- AAA-level accessibility and full internationalization are P3 unless enterprise sales requires them sooner.

## Dependencies

- Collaboration depends on stable version and permission models.
- Reporting depends on event, attribution, and analytics data quality.
- Preferences depend on a user preference API and storage model.

## Acceptance Criteria

- Multiple operators can coordinate editing without silent overwrite.
- User layout and notification preferences persist across sessions.
- Reporting work has explicit data sources, dimensions, and export limits.
