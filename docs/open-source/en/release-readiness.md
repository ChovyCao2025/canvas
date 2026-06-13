# Release Readiness

This checklist keeps English public-facing docs aligned with the Open Source
Growth spec and success metrics. It is a release drafting aid, not a replacement
for coordinator-owned gates.

## Public Story

- [ ] First screen explains: open-source marketing automation platform.
- [ ] Narrative focuses on Journey Canvas, plugins, templates, DSL, CLI, AI
  assistance, dry-run, governance, and traceability.
- [ ] Docs avoid pushing CDP, BI, ads, data warehouse, approval center, or SCRM
  into the main public story.
- [ ] Claims are consistent with G10: backend public extension/API write
  operations are not stable yet.

## Demo And Quickstart

- [ ] Local quickstart command path has been verified in a clean environment.
- [ ] Demo path does not require real SMS, email, coupon, approval, AI, or
  customer credentials.
- [ ] Default local account and required infrastructure are documented.
- [ ] Known limits are explicit, including RocketMQ and current local-only CLI
  behavior.

## Ecosystem Content

- [ ] Template catalog documents all ten planned official templates.
- [ ] Each public template page includes business intent, required plugins,
  sample payload, expected trace, and risk notes.
- [ ] Plugin docs describe build-time plugins, manifests, permissions, and
  enablement governance.
- [ ] DSL/CLI docs describe local validation and diff behavior without implying
  backend import/publish readiness before G10.
- [ ] AI docs describe mock-first behavior and do not require real model keys
  for the default demo path.

## Community And Release

- [ ] License is finalized and present before public announcement.
- [ ] CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, issue templates, and PR template
  are present.
- [ ] Release post links to English docs, quickstart, template catalog, and
  contracts.
- [ ] Release post describes measurable goals through `Time to First Successful
  Journey`.
- [ ] Competitive comparison, if used, is factual and does not attack other
  products.

## Verification References

- [Success metrics](../../open-source-growth/success-metrics.md)
- [Gate verification matrix](../../program-coordination/gate-verification-matrix.md)
- [Open Source Growth guardrails](../../open-source-growth/implementation-guardrails.md)
- [Release draft](../release-posts/v0.1-open-source-demo-draft.md)
