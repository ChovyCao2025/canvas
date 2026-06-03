# Filtered Product Opportunities Design

Date: 2026-06-03

## Goal

Extract useful product capability ideas that were previously filtered out of `docs/product-evolution/todo`, without reintroducing the original broad "do everything" scope.

## Scope

Create two supplemental todo documents:

- `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md`
- `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md`

Update `docs/product-evolution/todo/INDEX.md` so these documents are visible in the active priority list and explain why they exist.

## Extraction Rules

- Keep product capabilities that have a plausible operator, buyer, or platform value.
- Rewrite broad source ideas as bounded opportunity cards with source, useful point, why it was not P0/P1, validation needed, and priority.
- Prefer P2 for medium-term productization opportunities that can become specs after P0/P1 stabilization.
- Prefer P3 for commercial, ecosystem, AI-native, internationalization, privacy, and architecture-dependent strategy.
- Do not copy raw configuration inventories, long competitor background, TCO notes, implementation snippets, or "all do" commitments.

## P2 Opportunity Themes

- Message template center, template variables, channel adaptation, and template approval.
- Sandbox/demo environment, demo canvases, mock data, and sales enablement material.
- Analytics and reporting depth: dashboard, channel comparison, canvas comparison, report templates, export.
- Integration readiness: inbound/outbound webhooks, API keys, SSO/OIDC decision, data source management, data sync.
- Product operations: product usage tracking, feedback loop, feature flags, alert rules, onboarding and contextual help.
- Data and audience operations: audience set operations, snapshots, freshness/health monitoring, user 360, data catalog and quality basics.
- Editor and UX efficiency beyond P0: keyboard shortcuts, node search, skeleton states, richer form inputs, chart interactions.

## P3 Opportunity Themes

- Commercial model: metering, tiered plans, outcome pricing, payment, invoice, commission, value-added services.
- Ecosystem and partner strategy: ISV tiers, partner portal, community templates/plugins, SDKs, partner support.
- AI-native operations: AI Gateway, AI policy, AI copy, segment builder, journey builder, optimization, agents, prediction.
- Industry and customer success: industry packs, industry compliance profiles, managed service, training/certification, customer health, renewal.
- Globalization and regional expansion: i18n, timezone, currency, local channels, cross-border compliance.
- Advanced privacy and architecture: privacy computing, service split, serverless, edge, multi-cloud, data residency.

## Validation

- Source archive remains unchanged.
- New docs contain no placeholder markers.
- `todo/INDEX.md` links to both new docs.
- The supplemental docs make the previously filtered useful content discoverable without changing P0/P1 commitments.
