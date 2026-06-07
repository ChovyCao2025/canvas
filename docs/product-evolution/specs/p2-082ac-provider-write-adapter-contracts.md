# P2-082AC - Provider Write Adapter Contracts Spec

Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082ac-provider-write-adapter-contracts-plan.md`
Status: Delivered backend first slice

## Problem

P2-082Y, P2-082Z, P2-082AA, and P2-082AB now provide governed mutation ledgers and an operator UI for SEM, creator, and DSP writes. The remaining gap before real provider adapters is the adapter contract itself: dry-run must be able to reach a registered provider adapter when that provider supports validation, live apply must have a sandbox-safe adapter for local and staging verification, and provider request/response evidence must never persist credentials from metadata or external responses.

## Research Inputs

- Google Ads API mutate requests support validation-only and partial-failure controls, so a production adapter contract must allow dry-run delegation instead of forcing every dry-run to be local-only: https://developers.google.com/google-ads/api/reference/rpc/latest/MutateGoogleAdsRequest
- Google Ads mutation guidance models changes as explicit mutate operations with request/response evidence, which aligns with the existing mutation ledger pattern: https://developers.google.com/google-ads/api/docs/mutating/overview
- TikTok Business API and TikTok One/creator surfaces keep campaign and creator operations provider-specific, so the platform should normalize the internal mutation contract while leaving provider request construction inside adapters: https://business-api.tiktok.com/portal/docs
- The Trade Desk API access is token-based and managed from its partner portal, so provider credentials must stay in external credential stores/configuration rather than mutation payloads or UI-visible evidence: https://partner.thetradedesk.com/v3/portal/api/doc/Authentication

## Scope

Backend first slice:

- Add shared provider-write evidence sanitization for request metadata and provider responses.
- Allow registered provider write clients to handle dry-run requests when they support the provider/mutation.
- Keep the existing local dry-run fallback when no client is registered.
- Add sandbox provider write clients for SEM, creator, and DSP that support deterministic dry-run and live-apply evidence without real external credentials.
- Add focused tests proving no credential-shaped metadata/response values are persisted in provider evidence.
- Keep unsupported real providers fail-closed for live apply.

## Non-Goals

- Real Google Ads, TikTok, Meta, The Trade Desk, DV360, or marketplace API calls.
- OAuth onboarding for SEM/creator/DSP credentials.
- Provider-specific request builders for real live credentials.
- Frontend changes.

## Acceptance Criteria

- Gateway tests prove dry-run delegates to a registered client and still falls back to local validation when no client exists.
- Sandbox clients support live apply only for sandbox provider keys and produce deterministic provider operation IDs.
- Service tests prove request metadata and provider responses redact credential-shaped keys recursively.
- Existing fail-closed behavior remains for non-sandbox live providers without registered real clients.
- Focused backend tests for SEM, creator, DSP, and provider-write contract classes pass with Java 21.
