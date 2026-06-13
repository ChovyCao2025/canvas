# Positioning

Marketing Canvas is an open-source marketing automation platform for teams that
want a visual, reviewable, and extensible way to operate customer journeys.

The product narrative is deliberately focused: build journeys on a canvas,
connect capabilities through plugins, reuse templates, review changes as code,
and use AI as an assistant for drafts, risk checks, and explanations.

## Who It Is For

- Growth and lifecycle marketers who need repeatable journey operations.
- Marketing operations teams that want reviewable campaign changes.
- Engineers building provider integrations and governed execution paths.
- Developer advocates and community contributors creating templates, examples,
  and local demo workflows.

## Core Jobs

- Model a customer journey as nodes and edges.
- Use templates to start from common marketing scenarios.
- Dry-run or validate journeys before production execution.
- Keep provider integrations behind plugin and contract boundaries.
- Represent campaign definitions in docs or DSL artifacts when code review is
  the better workflow.
- Use AI to draft, audit, or explain, without letting AI auto-publish changes.

## What This Project Is Not Claiming

- It is not a complete marketing cloud replacement.
- It is not a production-ready runtime plugin marketplace today.
- It is not a runtime jar hot-loading system.
- It is not a way to bypass execution governance, approvals, audit, tenant
  boundaries, or provider safety.
- It is not ready to expose public write APIs for plugins, templates, DSL, CLI,
  or AI flows until the G10 stability gate passes.

## Current Open-Source Surface

- Local development stack with Java 21, Spring Boot, React, Vite, MySQL, Redis,
  RocketMQ, and WireMock.
- Public docs for quickstart, positioning, template examples, and
  MarketingOps as Code.
- Contracts under `docs/open-source-growth/contracts` for plugin manifests,
  template packs, Canvas DSL, demo profile behavior, node handlers, and AI
  operators.
- Community contribution templates and guardrails.

## Roadmap Shape

The open-source growth plan moves in phases:

1. Entry docs, community surface, and demo quickstart.
2. Plugin registry and official plugin skeletons after ownership and G10 gates.
3. Template pack and schema-driven configuration.
4. Canvas DSL and CLI.
5. AI-native journey operations.
6. Playground, English docs, and release package.

The gate model matters. It keeps docs, examples, and frontend mock work moving
while preventing backend API or extension claims from outrunning the stable
contracts.
