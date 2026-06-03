# P0 - Production Safety And Compliance Stopgaps

## Sources

- `product-audit-report-2026-05-31.md`
- `product-best-practice-roadmap-2026-05-31.md`

## Why P0

These items block safe production use or create immediate compliance, security, tenant isolation, or user-recovery risk. Broad roadmap items from the source files were filtered out; this document keeps only stopgaps that can be scoped as immediate work.

## Work Items

- Add frontend ErrorBoundary coverage, 404 route fallback, and usable 403 page.
- Put editor/stat/user deep-link pages back inside the main application layout so users retain navigation.
- Enforce tenant isolation at query boundaries, including tenant fields and MyBatis tenant interceptor coverage.
- Protect `/ops` and execution-sensitive endpoints with authentication and authorization.
- Add unsaved-change protection for browser unload and route navigation on key editing forms.
- Handle stub or misleading AI/recommendation nodes by removing them from production palettes or marking them as beta with clear execution behavior.
- Expose circuit breaker status and runtime degradation visibility to operators.
- Connect existing policy services, consent, suppression, and frequency checks into the actual send path.

## Deferred From Sources

- AI journey creation, predictive CLV/churn, marketing calendar, Content Cards, and industry packaging are not P0.
- "Full best-practice adoption" was rejected as too broad for immediate execution.
- Long-term commercial and ecosystem concepts are handled in P2/P3 documents.

## Dependencies

- Tenant isolation work depends on confirming current schema and DO field coverage.
- Policy-send-path work depends on tracing `AbstractSendMessageHandler` and policy service usage.

## Acceptance Criteria

- A React render exception no longer white-screens the entire app.
- Invalid routes show a 404 page and permission failures show a styled 403 page with a clear next action.
- Tenant-scoped reads and writes cannot access another tenant's canvas, audience, CDP, or notification data.
- `/ops` endpoints reject unauthenticated requests.
- Production node palettes do not silently expose hardcoded AI or recommendation stubs as reliable features.
- Operators can see circuit breaker state without reading logs.
