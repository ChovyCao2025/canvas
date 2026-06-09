# Marketing Integration Contract Registry Implementation Plan

Spec: `../specs/p2-088-marketing-integration-contract-registry.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Add a tenant-scoped integration contract registry and runtime probe evidence so the marketing middle platform has a governed API glue layer instead of only provider-specific write and credential modules.

**Architecture:** Add additive Flyway migrations, MyBatis DO/mappers, focused domain services, tenant-context controllers, control-plane evidence fields, contract audit events, a generic HTTP probe client, append-only probe observations, SLO burn-rate evaluation, a lease-guarded scheduler, and a compact operator table on `/marketing-platform`. The registry declares contracts, records revision history, records runtime readiness evidence, and turns degraded production probes into deduped monitoring alerts; provider-specific signed probes stay independent follow-up slices.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React, TypeScript, Ant Design, Vitest.

## Scope

1. Marketing integration contract table and tenant unique key.
2. Contract command, view, service, mapper, REST controller, and audit history.
3. Control-plane evidence and readiness gate integration.
4. Integration contract probe-run table, evidence service, HTTP probe client, automation service, and lease-guarded scheduler.
5. Probe-failure alert bridge into the existing marketing monitoring alert and fanout workflow.
6. Append-only probe observation history, SLO burn-rate evaluation, and SLO breach alert bridge.
7. Frontend API and Integration Contract Registry operator section with audit history, recent probe evidence, SLO burn-rate evidence, OPEN probe/SLO alerts, and automated scan action.
8. Focused backend/frontend tests and product-evolution index updates.

## Tasks

- [x] Add additive Flyway migration for `marketing_integration_contract`.
- [x] Add DO, mapper, command, view, domain service, and controller.
- [x] Add audit event migration, DO, mapper, view, service writes, listing endpoint, and operator audit modal.
- [x] Validate tenant scope, required fields, normalized keys, enums, timeout, JSON maps, filters, and archive behavior.
- [x] Add contract evidence fields to the marketing-platform control-plane provider and readiness model.
- [x] Add probe evidence fields to the marketing-platform control-plane provider and readiness model.
- [x] Add the Integration Contract Registry capability, asset, and contracts-to-provider-credentials lane.
- [x] Add frontend contract registry API methods and `/marketing-platform` operation section.
- [x] Add probe evidence API methods, recent probe table, manual probe recording, and automated probe scan action.
- [x] Add probe-failure alert bridge with deduped OPEN alerts, fanout on first failure, and PASS-based recovery resolution.
- [x] Add `V348__marketing_monitor_alert_open_dedupe_key.sql` and release dedupe keys on alert resolution.
- [x] Add `V349__marketing_integration_contract_probe_observations.sql` and write observations for manual and automated probe records.
- [x] Add multi-window SLO burn-rate evaluation and deduped SLO breach alert fanout.
- [x] Render SLO burn-rate evidence and OPEN integration probe/SLO alerts in `/marketing-platform`.
- [x] Add schema, service, controller, control-plane, API, and page tests.
- [x] Add HTTP probe client, automation service, scheduler, and scan endpoint tests.
- [x] Run focused backend and frontend verification for this slice.

## Verification

Backend:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=MarketingIntegrationContractServiceTest,MarketingIntegrationContractProbeServiceTest,MarketingIntegrationContractProbeAlertServiceTest,MarketingIntegrationContractSloServiceTest,MarketingIntegrationContractProbeAutomationServiceTest,HttpMarketingIntegrationContractProbeClientTest,MarketingIntegrationContractProbeSchedulerTest,MarketingIntegrationContractSchemaTest,MarketingIntegrationContractControllerTest,MarketingIntegrationContractProbeControllerTest,MarketingPlatformControlPlaneServiceTest,JdbcMarketingPlatformControlPlaneEvidenceProviderTest test
```

Focused tests should cover:

```bash
MarketingIntegrationContractSchemaTest
MarketingIntegrationContractServiceTest
MarketingIntegrationContractControllerTest
MarketingIntegrationContractProbeServiceTest
MarketingIntegrationContractProbeAutomationServiceTest
MarketingIntegrationContractProbeAlertServiceTest
MarketingIntegrationContractSloServiceTest
HttpMarketingIntegrationContractProbeClientTest
MarketingIntegrationContractProbeSchedulerTest
MarketingIntegrationContractProbeControllerTest
MarketingPlatformControlPlaneServiceTest
JdbcMarketingPlatformControlPlaneEvidenceProviderTest
MarketingCampaignMasterSchemaTest
MarketingCampaignServiceTest
MarketingCampaignControllerTest
```

Frontend:

```bash
cd frontend
npm test -- marketingPlatformControlPlane.test.ts marketingPlatformApi.test.ts marketingMonitoringApi.test.ts src/pages/marketing-platform/index.test.tsx
npx vite build
```

## Remaining Production Gap

This plan closes the declared contract registry, generic runtime probe evidence, append-only SLO observation history, multi-window burn-rate evaluation, and incident bridges into monitoring alerts. Remaining enterprise hardening includes provider-specific signed probe adapters, OpenTelemetry export, contract version negotiation, and end-to-end semantic certification of real provider writes.
