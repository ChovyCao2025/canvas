# Growth Activity Center Implementation Plan

Spec: `../specs/p2-089-growth-activity-center.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Add a tenant-scoped Growth Activity Center that manages promotion, referral, task, loyalty, retention, and private-domain growth activities as closed-loop business objects on top of the existing marketing middle-platform capabilities.

**Architecture:** Add additive persistence, MyBatis DOs/mappers, domain services, tenant-context controllers, marketing-platform evidence integration, and a focused frontend activity center. The activity center owns activity metadata, reward pools, participants, grants, referral relations, task progress, readiness, reconciliation, and reports while reusing campaign master ledger, journey canvas, `COMMIT_ACTION`, loyalty, content, delivery, BI, attribution, risk, and integration contract modules.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React, TypeScript, Ant Design, Vitest.

## Scope

1. Growth activity, rule, reward pool, budget counter, participant, grant, event, referral, and task tables.
2. Activity CRUD, lifecycle, readiness, and campaign master resource-link integration.
3. Reward pool and idempotent reward grant ledger.
4. Benefit promotion grant flow using existing coupon and points handlers.
5. Referral code, relation, qualification, both-side reward, and abuse evidence model.
6. Task definition, task progress, completion, and reward grant flow.
7. Loyalty member activity adapter that consumes existing loyalty APIs.
8. Retention winback and content/private-domain activity links through existing audience, canvas, content, and SCRM resources.
9. Report summary APIs for participation, qualification, grants, redemption, conversion, cost, ROI, referral funnel, and task completion.
10. Frontend `/growth-activities` page, activity wizard, readiness panel, reward pool panel, referral panel, task panel, grant ledger, event timeline, and report panel.
11. Marketing platform control-plane evidence and readiness integration.

## Tasks

- [x] Add additive Flyway migration for Growth Activity Center tables.
- [x] Add data objects and mappers for growth activities, rule sets, reward pools, budget counters, participants, grants, events, referral codes, referral relations, task definitions, and task progress.
- [x] Add schema tests proving tables, tenant indexes, unique keys, grant idempotency key, referral code uniqueness, and task progress uniqueness exist.
- [x] Add activity command/view records and `GrowthActivityService` for create, update, list, detail, publish, pause, close, and status transition validation.
- [x] Add `GrowthActivityReadinessService` that evaluates campaign master link, journey link, reward pool, budget, provider contract, content release, risk dependency, audience availability, analytics link, and failed-grant threshold.
- [x] Add `GrowthRewardPoolService` for reward-pool create/list, inventory/budget counter calculation, and low-inventory warning.
  - [x] Add reward-pool DO/mapper, command/view, create/update, list, tenant validation, metadata serialization, inventory/budget counters, and low-inventory warning.
- [x] Add `GrowthRewardGrantService` with idempotent grant creation, pre-consumption, grant success/failure/cancel/redeem/expire transitions, and reconciliation.
  - [x] Add reward-grant DO/mapper, command/view, tenant-scoped idempotent reservation, pre-consumption, success/failure/cancel/redeem/expire transitions, reconciliation, provider evidence serialization, and actor tracking.
- [x] Add a benefit-promotion grant adapter that routes coupon grants through existing coupon configuration and points grants through existing points operation configuration.
- [x] Add referral code and relation services for code generation, invite relation upsert, qualification events, risk evidence, inviter grant, and invitee grant.
- [x] Add task definition and progress services for event-driven progress, completion policy, reset policy, and one-time completion reward.
- [x] Add loyalty member activity adapter that calls existing loyalty account, earn, redeem, and eligible-benefit service methods without duplicating loyalty state.
- [x] Add activity event logging for operator lifecycle changes, participant entry, referral qualification, task progress, grant transitions, reconciliation, and conversion evidence.
- [x] Add report service with participation, qualification, grant, redemption, conversion, cost, ROI, referral, and task metrics.
- [x] Add `GrowthActivityController` and sub-resource endpoints under `/canvas/growth-activities`.
- [x] Add control-plane evidence fields, Growth Activity Center capability card, integration asset entry, campaign resource-link summary, and readiness blockers.
- [x] Add frontend `growthActivityApi.ts` with activity, reward pool, grant, referral, task, event, readiness, and report methods.
- [x] Add frontend `/growth-activities` route and navigation entry.
- [x] Add activity list with filters for type, status, campaign, owner, readiness, schedule, and grant health.
- [x] Add activity create/edit wizard with first-class type presets and campaign master link selection.
- [x] Add activity detail page with overview, readiness, linked resources, reward pools, participants, grant ledger, referral relations, tasks, event timeline, and report tabs.
- [x] Add reward pool panel with budget, inventory, reserved, granted, failed, canceled, redeemed, expired, and cost counters.
- [x] Add referral panel with referral code list, relation table, qualification state, risk evidence, and both-side reward state.
- [x] Add task panel with task definitions, progress table, completion evidence, and reward grant links.
- [x] Add grant ledger panel with idempotency key, grant channel, provider evidence, status, retry/reconcile/cancel actions, and error state.
- [x] Add report panel with funnel, conversion, cost, ROI, referral funnel, task completion, redemption, and dashboard link.
- [x] Add backend tests for activity service, readiness, reward pool counters, grant idempotency, referral qualification, task completion, loyalty adapter boundaries, controller tenant scope, report summaries, and control-plane evidence.
- [x] Add frontend tests for API paths, activity list rendering, wizard payloads, readiness blockers, reward pool counters, referral panel, task panel, grant ledger, and report panel.
- [x] Update product-evolution implementation order with P2-089.
- [x] Run focused backend and frontend verification.

## Verification

Backend:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=GrowthActivitySchemaTest,GrowthActivityServiceTest,GrowthActivityReadinessServiceTest,GrowthRewardPoolServiceTest,GrowthRewardGrantServiceTest,GrowthBenefitPromotionGrantAdapterTest,GrowthReferralServiceTest,GrowthTaskServiceTest,GrowthLoyaltyAdapterTest,GrowthActivityReportServiceTest,GrowthActivityControllerTest,MarketingPlatformControlPlaneServiceTest test
```

Frontend:

```bash
cd frontend
npm test -- growthActivityApi.test.ts src/pages/growth-activities/index.test.tsx marketingPlatformControlPlane.test.ts
npx vite build
```

## Rollout

1. Deploy schema and backend APIs with the frontend navigation hidden.
2. Enable list/detail/readiness for internal operators.
3. Enable `BENEFIT_PROMOTION` and grant ledger for coupon/points activities.
4. Enable `REFERRAL_INVITE` after risk-control dependency is configured.
5. Enable `TASK_INCENTIVE`, `LOYALTY_MEMBER_ACTIVITY`, and `RETENTION_WINBACK`.
6. Enable reports and control-plane readiness contribution.

Rollback is additive: hide the route and disable activity publishing. Existing canvas, coupon, loyalty, campaign, content, delivery, and BI modules continue to operate independently.

## Remaining Production Gap

This slice creates the closed-loop activity layer. Real external coupon catalog administration, provider-specific redemption receipts, advanced fraud scoring, and advanced offer decisioning remain separate provider/risk/AI follow-up slices.
