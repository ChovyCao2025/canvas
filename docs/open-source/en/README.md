# Marketing Canvas English Docs

Marketing Canvas is an open-source marketing automation platform for teams that
want a visual, reviewable, and extensible way to operate customer journeys.

Use these English-facing docs as the public entry point while the root README is
managed by the release coordinator. They summarize the current open-source
surface without claiming that backend public extension or write APIs are stable
before the G10 gate.

## Start Here

- [Overview](overview.md) explains the product focus and current maturity.
- [Quickstart](quickstart.md) links the local development startup path and
  current demo limits.
- [Ecosystem Guide](ecosystem.md) describes plugins, templates, DSL, CLI, and
  AI boundaries.
- [Release Readiness](release-readiness.md) tracks what must be true before a
  public release announcement can be treated as final.
- [Release Drafts](../release-posts/) contains draft public launch posts.

## Current Public Surface

- Local development stack with Java 21, Spring Boot, React, Vite, MySQL, Redis,
  RocketMQ, and WireMock.
- Template catalog documentation for ten journey scenarios.
- Local-only Canvas DSL validation and diff examples.
- Contract documents for plugin manifests, template packs, Canvas DSL, node
  handlers, demo profile behavior, and AI operators.
- Community files and guardrails being prepared for a public open-source
  release.

## Important Caveats

- Backend public extension and write APIs remain gated by G10.
- The project is not claiming production runtime plugin marketplace readiness.
- The final public license status must be confirmed by the coordinator before a
  release post is published.
- Local examples use mock providers by default; do not add real customer,
  SMS, email, coupon, approval, or AI credentials to the repository.

## Related Docs

- [Open-source positioning](../positioning.md)
- [Open-source quickstart](../quickstart.md)
- [MarketingOps as Code](../marketingops-as-code.md)
- [Official template catalog](../templates/README.md)
- [Open Source Growth spec](../../open-source-growth/open-source-growth-spec.md)
- [Success metrics](../../open-source-growth/success-metrics.md)
- [Contract docs](../../open-source-growth/contracts/README.md)
