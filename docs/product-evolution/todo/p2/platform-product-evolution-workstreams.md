# P2 - Platform Product Evolution Workstreams

## Sources

- `product-evolution-directions-2026-05-31.md`
- `product-evolution-directions-ext-2026-05-31.md`
- `product-strategy-dual-track-2026-05-31.md`

## Why P2

The strategy documents contain useful platform direction, but their "all in" scope and large configuration catalogs are not executable. This todo keeps medium-term workstreams that can become scoped plans after P0/P1 stabilization.

## Work Items

- Platformization: plugin extension points, developer portal basics, API keys, outbound webhooks, and low-code schema improvements.
- Data assets: data quality rules, data catalog basics, path analytics, lightweight report improvements, and event pipeline foundations.
- Channels: WeCom L1/L2, channel adapter abstraction, and channel cost/receipt tracking.
- Operations: approval flow expansion, audit timeline, dashboard command center, and alert rules.
- Knowledge: template market, best-practice library, contextual help, and operator playbooks.
- Integrations: inbound webhook, API key management, SSO/OIDC decision, and data source connection improvements.

## Filtered Scope

- 61, 93, and 154 configuration-item inventories are not implementation scope.
- Microservices, serverless, edge computing, multi-cloud, and full marketplace are deferred unless validated by scale or customer needs.
- The dual-track 60/40 resource split is strategy context, not a commitment.

## Dependencies

- Requires P0 tenant/security work and P1 operator loop visibility.
- WeCom channel work depends on channel adapter boundaries.
- Analytics work depends on event collection and data quality.

## Acceptance Criteria

- Each workstream has a bounded follow-up spec before implementation.
- No workstream enters implementation with "all directions all config" scope.
- Source strategy is traceable without keeping long-form strategy files in active todo.
