# Product Evolution Specs Index

Specs are ordered by priority and sequence. Each spec links to its matching implementation plan in `../plans`.

| Order | Spec |
|-------|------|
| P0-001 | [Production Safety And Compliance](p0-001-production-safety-and-compliance.md) |
| P0-002 | [Frontend Resilience And A11y](p0-002-frontend-resilience-and-a11y.md) |
| P0-003 | [Delivery Outbox, Receipts, And Reconciliation](p0-003-delivery-outbox-receipts-and-reconciliation.md) |
| P0-004 | [DAG Side-Effect Idempotency And Context Bounds](p0-004-dag-side-effect-idempotency-and-context-bounds.md) |
| P0-005 | [Production Operability And Runtime Gates](p0-005-production-operability-and-runtime-gates.md) |
| P1-001 | [Operating Loop And Effect Closure](p1-001-operating-loop-and-effect-closure.md) |
| P1-002 | [Operator Visibility And Testability](p1-002-operator-visibility-and-testability.md) |
| P1-003 | [Audience Snapshot Mode And Defaults](p1-003-audience-snapshot-mode-and-defaults.md) |
| P1-003B | [Publish-Time Audience Snapshot Locking](p1-003b-publish-time-audience-snapshot-locking.md) |
| P1-003C | [TAGGER Audience Runtime Snapshot Branching](p1-003c-tagger-audience-runtime-snapshot-branching.md) |
| P1-003D | [Safe Message Preview](p1-003d-safe-message-preview.md) |
| P1-003E | [Canvas Import Export Package](p1-003e-canvas-import-export-package.md) |
| P1-003F | [Canvas Project Folder Metadata](p1-003f-canvas-project-folder-metadata.md) |
| P1-003G | [AI Capability Policy And Governance](p1-003g-ai-capability-policy-and-governance.md) |
| P1-004 | [3000 Concurrency Hardening Gate](p1-004-3000-concurrency-hardening-gate.md) |
| P1-005 | [CDP Write Key Management And Authentication](p1-005-cdp-write-key-management-and-authentication.md) |
| P1-005A | [CDP Event Log And Idempotent Track](p1-005a-cdp-event-log-and-idempotent-track.md) |
| P1-005A2 | [Event Attribute Discovery And Internal CDP Event](p1-005a2-event-attribute-discovery-and-internal-cdp-event.md) |
| P1-005A3 | [Event Config Write Key And Attribute Review UI](p1-005a3-event-config-write-key-and-attribute-review-ui.md) |
| P1-005B | [Webhook Subscription Schema And Signing](p1-005b-webhook-subscription-schema-and-signing.md) |
| P1-005B2 | [Webhook Dispatch Retry And Delivery Log](p1-005b2-webhook-dispatch-retry-and-delivery-log.md) |
| P1-005B3 | [Webhook Subscription API And Operator UI](p1-005b3-webhook-subscription-api-and-operator-ui.md) |
| P1-005C | [Analytics Web SDK Foundation](p1-005c-analytics-web-sdk-foundation.md) |
| P1-006 | [CDP Computed Profile Attributes](p1-006-cdp-computed-profile-attributes.md) |
| P1-006B | [CDP Computed Tags And Lineage](p1-006b-cdp-computed-tags-and-lineage.md) |
| P1-006C | [Realtime Audiences, Overlap, And Snapshots](p1-006c-realtime-audiences-overlap-and-snapshots.md) |
| P1-007 | [Canvas Editor Edge Projection And History](p1-007-canvas-editor-edge-projection-and-history.md) |
| P1-007B | [Editor Store And Save Queue](p1-007b-editor-store-and-save-queue.md) |
| P1-007C | [Frontend HTTP Client And Runtime Schemas](p1-007c-frontend-http-client-and-runtime-schemas.md) |
| P1-008 | [Channel Connector Contract And Disabled State](p1-008-channel-connector-contract-and-disabled-state.md) |
| P1-008B | [Provider Backpressure, Fallback, And Dedupe](p1-008b-provider-backpressure-fallback-and-dedupe.md) |
| P1-008C | [Channel Connector Operator Surface](p1-008c-channel-connector-operator-surface.md) |
| P2-001 | [Collaboration Personalization And Reporting](p2-001-collaboration-personalization-and-reporting.md) |
| P2-002 | [Plugin And Integration Foundations](p2-002-plugin-and-integration-foundations.md) |
| P2-003 | [Platform Product Evolution Workstreams](p2-003-platform-product-evolution-workstreams.md) |
| P2-004 | [Technical Migration Candidates](p2-004-technical-migration-candidates.md) |
| P2-005 | [Message Template Center](p2-005-message-template-center.md) |
| P2-006 | [Sandbox Demo And Sales Enablement](p2-006-sandbox-demo-sales-enablement.md) |
| P2-007 | [Analytics Command Center](p2-007-analytics-command-center.md) |
| P2-008 | [Integration Readiness](p2-008-integration-readiness.md) |
| P2-009 | [Product Usage Analytics And Feedback Loop](p2-009-product-usage-analytics-feedback-loop.md) |
| P2-010 | [Audience Operations And Data Quality](p2-010-audience-operations-data-quality.md) |
| P2-011 | [Editor Productivity Beyond Baseline](p2-011-editor-productivity-beyond-baseline.md) |
| P2-012 | [Channel Intelligence And Scheduling](p2-012-channel-intelligence-and-scheduling.md) |
| P2-013 | [Knowledge Base And Best Practice Library](p2-013-knowledge-base-best-practice-library.md) |
| P2-014 | [Design System And Guided Experience](p2-014-design-system-guided-experience.md) |
| P2-015 | [4000 Concurrency Readiness And Lane Isolation](p2-015-4000-concurrency-readiness-and-lane-isolation.md) |
| P2-016 | [Analytics Event Trace Schema And Sink](p2-016-analytics-event-trace-schema-and-sink.md) |
| P2-016B | [Analytics Retention And Archive Policy](p2-016b-analytics-retention-and-archive-policy.md) |
| P2-016C | [Bounded Analytics Query APIs](p2-016c-bounded-analytics-query-apis.md) |
| P2-016D | [Frontend Analytics Views And Export States](p2-016d-frontend-analytics-views-and-export-states.md) |
| P2-017 | [Template Renderer And Variable Picker](p2-017-template-renderer-and-variable-picker.md) |
| P2-017B | [User Input And Wait Event UX](p2-017b-user-input-and-wait-event-ux.md) |
| P2-017C | [Connected Content Node](p2-017c-connected-content-node.md) |
| P2-017D | [Test Users And Single User Rerun](p2-017d-test-users-and-single-user-rerun.md) |
| P2-017E | [Execution Timeline And Batch Operations](p2-017e-execution-timeline-and-batch-operations.md) |
| P2-018 | [Runtime Architecture Migration Evidence](p2-018-runtime-architecture-migration-evidence.md) |
| P2-019 | [AI LLM Node Productionization](p2-019-ai-llm-node-productionization.md) |
| P2-020 | [Churn Prediction And Smart Timing Foundation](p2-020-churn-prediction-and-smart-timing-foundation.md) |
| P3-001 | [Ecosystem And Plugin Marketplace Strategy](p3-001-ecosystem-and-plugin-marketplace-strategy.md) |
| P3-002 | [Long Term AI Commerce And Ecosystem Bets](p3-002-long-term-ai-commerce-and-ecosystem-bets.md) |
| P3-003 | [Long Term Architecture Evolution](p3-003-long-term-architecture-evolution.md) |
| P3-004 | [Commercial Model And Billing](p3-004-commercial-model-and-billing.md) |
| P3-005 | [Value Added Services And Customer Success](p3-005-value-added-services-and-customer-success.md) |
| P3-006 | [Ecosystem And Partner Program](p3-006-ecosystem-and-partner-program.md) |
| P3-007 | [AI Native Marketing Operations](p3-007-ai-native-marketing-operations.md) |
| P3-008 | [Industry Packaging](p3-008-industry-packaging.md) |
| P3-009 | [Globalization And Regional Expansion](p3-009-globalization-and-regional-expansion.md) |
| P3-010 | [Advanced Privacy And Compliance](p3-010-advanced-privacy-and-compliance.md) |
| P3-011 | [Advanced Architecture And Deployment Strategy](p3-011-advanced-architecture-and-deployment-strategy.md) |
| P3-012 | [Product Led Growth And Community](p3-012-product-led-growth-and-community.md) |
