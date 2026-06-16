# Ecosystem Guide

Marketing Canvas is being shaped as an open-source ecosystem around templates,
build-time plugins, Canvas DSL, local CLI workflows, and AI-assisted operations.
The ecosystem docs must stay contract-first and gate-aware.

## Plugin Model

The first public plugin model is build-time and governed:

- Plugins are packaged with the application, not hot-loaded at runtime.
- Plugin manifests declare identity, compatibility, extension points,
  permissions, contributed nodes, and related templates.
- Disabled or missing plugins must block dependent template import or publish
  validation where those backend APIs are stable. G10 import/export preview is
  available; publish remains gated.
- Registry metadata and enablement are planned for `canvas-platform`; handler
  binding and execution-facing node metadata are planned for
  `canvas-context-execution`.

Do not describe a production plugin marketplace or runtime jar hot-loading as
ready. See
[plugin-manifest-v1.md](../../open-source-growth/contracts/plugin-manifest-v1.md).

## Templates

The public template catalog currently documents ten official scenarios:

- New user welcome
- Dormant user winback
- Coupon approval release
- AI copy review and publish
- Lead capture assignment
- Birthday benefit
- VIP retention
- A/B message experiment
- Risk-blocked outreach
- Private domain follow-up

Each template should include business intent, required plugins, sample payload,
expected trace, and risk notes. Backend import/export preview is unlocked by
G10; dry-run enforcement and publish remain gated backend work. See
[templates/README.md](../templates/README.md) and
[template-pack-v1.md](../../open-source-growth/contracts/template-pack-v1.md).

## Canvas DSL And CLI

Canvas DSL v1 is for demos, templates, local validation, diffs, and AI drafts.
It does not replace the full runtime graph JSON storage model.

The current CLI surface includes local checks and G10 import/export preview:

- validate a local Canvas DSL JSON document
- diff node-level changes between two local documents
- import a Canvas DSL document through `POST /canvas/dsl/import`
- export a Canvas DSL document through `GET /canvas/dsl/export/{canvasId}`

It must not publish journeys or call backend APIs outside the approved G10
import/export endpoints. See [marketingops-as-code.md](../marketingops-as-code.md)
and [canvas-dsl-v1.md](../../open-source-growth/contracts/canvas-dsl-v1.md).

## AI-Assisted Operations

AI is positioned as an assistant for:

- journey DSL drafts
- campaign risk audits
- message copy variants
- trace-based failure explanations
- next-step suggestions

Demo and public examples must work without real model credentials. Use mock AI
behavior until a real provider is explicitly configured by the user. See
[ai-operator-contract.md](../../open-source-growth/contracts/ai-operator-contract.md).

## Gate-Aware Claims

When writing public docs, posts, or examples:

- Say "planned", "documented", or "local-only" when a capability is not behind
  a stable backend API yet.
- Say G10 import/export preview is unlocked, but do not claim publish readiness.
- Do not claim a finalized license until the coordinator confirms it.
- Do not imply that plugins can bypass governance, permissions, tenant
  boundaries, approvals, audit, or traceability.
