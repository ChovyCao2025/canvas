# P2-017C - Connected Content Node Spec

Priority: P2
Sequence: 017C
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p2-017c-connected-content-node-plan.md`

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Add a safe connected-content node for bounded external content fetch, preview, cache, JSON path extraction, and template binding.

## Current Baseline

- `OutboundUrlValidator` exists for outbound URL safety.
- API call nodes exist, but they are not designed as cached content blocks for message rendering.

## In Scope

- Migration `V130__connected_content_cache.sql`.
- `ConnectedContentHandler`.
- URL allowlist, timeout, payload size cap, cache TTL, JSON path extraction, preview response, and trace output.

## Out Of Scope

- Full CMS.
- AI content generation.

## Acceptance Criteria

- Tests prove allowlist rejection, timeout fallback, cache hit/expiry, payload cap, JSON path extraction, and provider error trace output.
