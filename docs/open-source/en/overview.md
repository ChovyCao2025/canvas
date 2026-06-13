# Overview

Marketing Canvas focuses the public story around one clear job: help teams
design, review, dry-run, and operate customer journeys with governance.

The platform narrative is:

```text
Plugin-driven Journey Canvas + MarketingOps Runtime + AI-assisted Campaign Operations
```

## What It Does

- Models customer journeys as a visual graph of nodes and edges.
- Uses templates to make common lifecycle, promotion, AI review, lead
  assignment, and risk-control journeys easier to inspect.
- Keeps provider-specific behavior behind plugin and contract boundaries.
- Supports local Canvas DSL validation and diff workflows for reviewable
  MarketingOps changes.
- Treats AI as an assistant for drafts, risk checks, copy suggestions, and trace
  explanations instead of an auto-publish path.

## Who It Is For

- Growth and lifecycle marketers who need repeatable journey operations.
- Marketing operations teams that want reviewable campaign changes.
- Engineers building provider integrations and governed execution paths.
- Contributors creating templates, examples, docs, and local demo workflows.

## What It Is Not

- It is not a full marketing cloud replacement.
- It is not a production-ready runtime plugin marketplace.
- It does not support runtime jar hot-loading as a public goal.
- It does not bypass approval, audit, tenant, permission, or provider safety
  boundaries.
- It does not expose stable public backend write APIs for plugins, templates,
  DSL, CLI, or AI flows until the G10 public extension/API stability gate passes.

## Roadmap Shape

The open-source growth track is organized around the north star metric
`Time to First Successful Journey`: how long it takes a new user to run a sample
journey dry-run and see trace output.

The intended milestone sequence is:

| Milestone | Public Theme | Primary Outcome |
| --- | --- | --- |
| `v0.1` | Open Source Demo | Clear entry docs, local demo path, first templates, mock providers |
| `v0.2` | Plugin and Template SDK | Manifest contracts, schema-driven config, official plugin/template path |
| `v0.3` | MarketingOps as Code | Canvas DSL, CLI, AI draft and audit workflows, playground path |

Read the detailed source plan in
[open-source-growth-spec.md](../../open-source-growth/open-source-growth-spec.md)
and the measurable goals in
[success-metrics.md](../../open-source-growth/success-metrics.md).
