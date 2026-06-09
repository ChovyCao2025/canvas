# Marketing Campaign Master Ledger Implementation Plan

Spec: `../specs/p2-087-marketing-campaign-master-ledger.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Add a tenant-scoped campaign master-data ledger and resource-link operations so the marketing middle platform can govern cross-domain launch dependencies instead of only reporting static readiness gaps.

**Architecture:** Add an additive Flyway migration, MyBatis DOs/mappers, a focused domain service, a tenant-context controller, control-plane evidence aggregation, and a compact operator section in `/marketing-platform`. Existing journey, content, paid-media, SEM, creator, DSP, and BI modules remain independent but can now be referenced by stable campaign resource links.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React, TypeScript, Ant Design, Vitest.

## Scope

1. Campaign master and campaign resource-link tables.
2. Campaign ledger service, single-campaign readiness gate, and REST controller.
3. Control-plane evidence provider and readiness integration.
4. Frontend API and Campaign master ledger operator section.
5. Focused backend/frontend tests and product-evolution index updates.

## Tasks

- [x] Add additive Flyway migration for `marketing_campaign_master` and `marketing_campaign_link`.
- [x] Add DOs, mappers, commands, views, domain service, and controller.
- [x] Add campaign evidence fields to the marketing-platform control-plane provider and readiness model.
- [x] Add frontend campaign ledger API methods and `/marketing-platform` operation section.
- [x] Add single-campaign launch readiness API and frontend evaluation controls.
- [x] Add schema, service, controller, control-plane, API, and page tests.
- [x] Run focused backend and frontend verification after this slice is complete.

## Verification

Backend:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine compile
```

Focused tests should cover:

```bash
MarketingCampaignMasterSchemaTest
MarketingCampaignServiceTest
MarketingCampaignControllerTest
MarketingPlatformControlPlaneServiceTest
JdbcMarketingPlatformControlPlaneEvidenceProviderTest
```

Frontend:

```bash
cd frontend
npm test -- marketingPlatformControlPlane.test.ts marketingPlatformApi.test.ts src/pages/marketing-platform/index.test.tsx
npm run build
```
