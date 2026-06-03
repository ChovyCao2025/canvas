# P2 - Plugin And Integration Foundations

## Sources

- `mautic-plugin-feasibility-analysis.md`
- `plugin-candidate-list.md`
- `mautic-comparison-2026-06.md`

## Why P2

Plugin architecture and integrations are valuable platform work, but they should follow core safety and operator workflows. The source files are feasible but overstate near-term P0 value for several plugins.

## Work Items

- Define plugin extension points for node handlers, channel adapters, data exporters, and rule/template packs.
- Add plugin metadata, lifecycle, configuration schema, enable/disable state, and compatibility checks.
- Start with internal/built-in plugin packaging before hot-loaded third-party code.
- Prioritize official plugins that reduce core coupling: WeCom channel adapter, data export connector, batch operation engine, and AI Gateway adapter.
- Add API key and webhook foundation for integration partners.

## Filtered Scope

- Runtime hot-loading, revenue sharing, public marketplace UI, plugin ratings, and ISV review workflow are not P2 foundation scope.
- Feishu and DingTalk plugins stay behind WeCom and data export unless customer demand changes.

## Dependencies

- Plugin safety depends on permission, audit, configuration validation, and clear classloader or remote-call boundaries.
- Channel plugins depend on the channel adapter abstraction.

## Acceptance Criteria

- Internal plugins can be registered, configured, enabled, disabled, and version-checked.
- Built-in handlers can continue to work without plugin migration.
- Plugin failures are isolated from core DAG execution where feasible.
