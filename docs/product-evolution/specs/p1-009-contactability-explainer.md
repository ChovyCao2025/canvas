# P1-009 - Contactability Explainer Spec

Priority: P1
Sequence: 009
Source: Mautic 4.4.12 local UI/source review, Mautic 4.x Campaign Builder docs, Mautic Preference Center docs, existing Canvas P0/P1 policy work
Implementation plan: `../plans/p1-009-contactability-explainer-plan.md`

## Goal

Add a read-only contactability explainer that tells operators why a user can or cannot be contacted on a channel before a send or test run.

## User And Business Value

Canvas already enforces consent, suppression, channel availability, quiet hours, and frequency caps in the delivery path. Operators still need a fast explanation surface that does not require reading logs, consuming Redis frequency quota, or reverse-engineering skipped send records. This reduces false sends, shortens test-run diagnosis, and creates a practical bridge toward a future full preference center.

## Research Notes

- Mautic 4.x Campaign Builder is organized around conditions, decisions, and actions on a visual workflow canvas.
- The local Mautic 4.4.12 source includes `FrequencyRule`, Do Not Contact entities/repositories/events, `PreferenceChannelsType`, and `ChannelBundle/PreferenceBuilder`.
- The local Mautic UI exposes dashboard, contacts, segments, forms, campaigns, channels, emails, points, configuration, and a default frequency rule configuration surface.
- Canvas already has `MarketingPolicyService`, policy tables, send-path policy enforcement, channel connectors, delivery records, and operator visibility specs. A full preference CRUD center would duplicate existing broad roadmap work, while read-only explainability is missing.

Reference URLs:
- `https://docs.mautic.org/en/4.x/campaigns/campaign_builder.html`
- `https://docs.mautic.org/en/4.x/contacts/preference_center.html`
- `https://devdocs.mautic.org/en/5.x/webhooks/events/index.html`

## In Scope

- Add a non-mutating frequency preview to `MarketingPolicyService`.
- Add `ContactabilityExplainerService` to compose policy checks into ordered evidence.
- Add a read-only `/canvas/contactability/explain` endpoint.
- Keep response payloads stable enough for future frontend and test-run integration.
- Include focused backend tests.

## Out Of Scope

- Preference center UI.
- Topic/category-level consent model.
- Consent audit tables.
- Double opt-in.
- Data-subject rights workflows.
- Changing existing send-path behavior.
- Consuming frequency quota during explanation.

## Functional Requirements

1. An operator can request a contactability explanation with `userId` and `channel`.
2. The response includes overall `allowed`, normalized `channel`, and ordered checks for consent, suppression, channel availability, quiet hours, and frequency preview.
3. Frequency preview must not call Redis `increment`, `decrement`, or `expire`.
4. Each check includes `checkKey`, `allowed`, `reasonCode`, and `reasonMessage`.
5. The controller defaults to current send-path policy defaults: explicit consent required, quiet hours `22:00-08:00`, timezone `USER_LOCAL`, journey frequency scope, one send per day.
6. Invalid or omitted optional fields use safe defaults rather than throwing from the controller.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ContactabilityExplainerService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ContactabilityController.java`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceFrequencyPreviewTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/ContactabilityExplainerServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ContactabilityControllerTest.java`

## Dependencies

- Existing policy tables and mappers from `V60__marketing_policy_tables.sql`.
- Tenant indexes from `P0-001` remain important for production, but this slice does not add new data writes.
- Future frontend/test-run integration can consume the read-only endpoint without changing its contract.

## Risks And Controls

- Risk: Explanation changes frequency quota. Control: frequency preview reads the current bucket value only and tests verify no Redis write operations are called.
- Risk: Operators misread a partial explanation. Control: response always includes every policy check and overall `allowed`.
- Risk: Scope creep into preference CRUD. Control: this slice stays read-only and explicitly excludes preference center writes.
- Risk: Tenant scoping remains inconsistent in the legacy policy service. Control: no new writes; future P0/P1 tenant-scoped policy admin work remains the owner for mapper-level scoping.

## Acceptance Criteria

- `MarketingPolicyServiceFrequencyPreviewTest` proves preview blocks at the current limit and does not mutate Redis.
- `ContactabilityExplainerServiceTest` proves ordered checks are returned and overall status is false when any check blocks.
- `ContactabilityControllerTest` proves the endpoint applies default send-policy options and returns the service result.
- Focused backend tests pass with `cd backend && mvn -pl canvas-engine test -Dtest=MarketingPolicyServiceFrequencyPreviewTest,ContactabilityExplainerServiceTest,ContactabilityControllerTest`.

## Implementation Status

Status: Completed on 2026-06-05; broader Maven test execution is still blocked by unrelated `RedisBiQueryResultCacheTest` testCompile failures.

- `MarketingPolicyService.previewFrequency` reads the existing frequency bucket without increment/decrement/expire mutations.
- `ContactabilityExplainerService` returns ordered checks for consent, suppression, channel availability, quiet hours, and frequency preview.
- `ContactabilityController` exposes `GET /canvas/contactability/explain` with send-path defaults and safe fallbacks for invalid optional fields.
- Frontend `contactabilityApi` and the CDP user detail contactability card consume the read-only endpoint.
- Maven broader test execution is still blocked by unrelated `RedisBiQueryResultCacheTest` testCompile failures; this slice was verified with isolated backend runner `5/5` plus focused frontend tests.
