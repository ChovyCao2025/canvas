# P3-012 - Product Led Growth And Community Spec

Priority: P3
Sequence: 012
Source: `todo/p3/strategic-opportunities-from-filtered-scope.md#product-led-growth-and-community`
Implementation plan: `../plans/p3-012-product-led-growth-and-community-plan.md`

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

## Goal

Create an executable discovery and governance slice for product-led growth and community so trial journey, activation milestones, proficiency levels, referral, public examples, case studies, templates, and customer story loops are ranked by evidence before product build-out.

## User And Business Value

This gives growth, customer success, product, and compliance teams a governed way to decide which PLG and community bets deserve implementation based on activation evidence, consent requirements, content risk, expected lift, proof commands, and rollback notes.

## In Scope

- PLG and community opportunity evidence registry.
- Activation, referral, public example, template, and case-study assessment metadata.
- Additive Flyway table for growth evidence decisions.
- Service tests that block unreviewed public/community workflows.
- Review states for discovery, experiment approval, rejection, and child-spec conversion.

## Out Of Scope

- Implementing trial onboarding, activation tracking, referral systems, public galleries, community publishing, case-study workflows, or customer-facing growth UI.
- Publishing customer stories or public examples without consent and review.
- Editing product-evolution indexes or `EXECUTABLE_PLAN_AUDIT.md`.

## Functional Requirements

1. A growth evidence record must include opportunity key, owner, funnel stage, target persona, activation metric, consent requirement, content risk, experiment hypothesis, proof command, rollback note, and decision status.
2. Public examples, customer stories, referrals, and community templates must require consent and risk notes before approval.
3. Experiment approval must require reviewer identity, reviewed time, and named child spec.
4. The service must reject incomplete activation metrics or missing rollback notes.
5. This slice must not launch growth workflows; it records evidence and gates future implementation.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceService.java`
- `backend/canvas-engine/src/main/resources/db/migration/V184__product_led_growth_evidence.sql`

### Frontend Touchpoints

- None for this discovery slice. UI work is deferred until a child spec defines a growth or community workflow.

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V184__product_led_growth_evidence.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java`

## Dependencies

- Growth owner for activation and funnel evidence.
- Customer success owner for case-study and community workflows.
- Compliance or legal owner for consent-sensitive public content.
- Analytics owner for metric definitions and proof commands.

## Risks And Controls

- Vanity metrics: require activation metric and proof command before approval.
- Consent risk: public/community opportunities remain blocked when consent evidence is missing.
- Experiment sprawl: each approved item must name a child spec and rollback note.
- Data migration risk: `V184__product_led_growth_evidence.sql` is additive and can be disabled by stopping registry writes.

## Acceptance Criteria

- `V184__product_led_growth_evidence.sql` creates an additive PLG evidence table with activation, consent, proof, rollback, reviewer, child-spec, and status fields.
- Service tests prove public/community opportunities remain blocked until reviewed consent and metric evidence are complete.
- The plan includes real TDD snippets, commands with expected outputs, rollout notes, and scoped git add and commit commands.
- The slice records governance evidence only; no growth or community runtime workflow is shipped.
