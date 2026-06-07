# Marketing Platform Control Plane Implementation Plan

**Goal:** Add a first production control-plane slice for the marketing middle platform so implemented P2-082 domains are exposed as one operator-visible readiness and integration map.

**Architecture:** Keep the slice additive. Add a platform service that aggregates tenant runtime evidence from existing marketing domains, converts it into capability readiness, glue-lane status, integration asset catalog entries, and a machine-readable readiness gate, exposes an authenticated controller, and renders the result in a compact operator page. Existing domain modules remain unchanged.

**Tech Stack:** Java 21, Spring Boot, Reactor controller wrappers, JUnit 5, Mockito, AssertJ, React, TypeScript, Ant Design, Vitest.

## Scope

1. Backend control-plane service, evidence provider, and records.
2. Authenticated `/canvas/marketing-platform/control-plane` endpoint.
3. Frontend `marketingPlatformApi`.
4. Frontend `/marketing-platform` page with readiness KPIs, readiness gate, capability table, integration asset catalog, runtime evidence, integration lanes, provider write operations, and action items.
5. Marketing integration contract registry and health probe evidence.
6. Probe-failure incident bridge into existing monitoring alerts and fanout.
7. Probe observation history and SLO burn-rate alerting.
8. Automated integration contract probe scanning and operator trigger.
9. Product-evolution index updates.

## Tasks

- [x] Add product spec and plan entries.
- [x] Write backend service/controller contract tests.
- [x] Write frontend API and presentation tests.
- [x] Implement backend service and controller.
- [x] Implement JDBC runtime evidence aggregation for journeys, content, SCRM, monitoring, paid media, credentials, SEM, creators, DSP, BI, and provider mutation ledgers.
- [x] Implement evidence-driven capability readiness, glue-lane status, integration asset catalog entries, and readiness gate findings.
- [x] Implement frontend API, presentation helpers, page, route, navigation, runtime evidence rendering, integration asset catalog rendering, readiness gate rendering, and route announcement.
- [x] Add tenant-scoped marketing integration contract registry with status, environment, auth, credential dependency, SLA/retry, schema, and metadata.
- [x] Add marketing integration contract probe evidence table, service, and API with HTTP status, latency, error type, Problem Details fields, observed time, and evidence JSON.
- [x] Feed fresh production PASS/FAIL probe counts into control-plane readiness and block production readiness when probes are stale or failing.
- [x] Render recent integration probe evidence and probe recording action in the `/marketing-platform` registry section.
- [x] Add generic HTTP probe client, production-contract scan service, disabled-by-default lease-guarded scheduler, backend scan endpoint, and `/marketing-platform` automated scan action.
- [x] Add deduped OPEN alert bridge for production probe failures, fanout on first failure, and PASS-based recovery resolution.
- [x] Add `marketing_monitor_alert.dedupe_key` and probe observation history migrations for deduped incidents and SLO windows.
- [x] Add SLO burn-rate evaluation from probe observations and bridge `INTEGRATION_CONTRACT_SLO_BURN_RATE` alerts into monitoring.
- [x] Render OPEN integration probe alerts and SLO burn-rate evaluations in the `/marketing-platform` registry section.
- [x] Run focused backend and frontend verification.

## Verification

Backend:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=MarketingIntegrationContractServiceTest,MarketingIntegrationContractProbeServiceTest,MarketingIntegrationContractProbeAlertServiceTest,MarketingIntegrationContractSloServiceTest,MarketingIntegrationContractProbeAutomationServiceTest,HttpMarketingIntegrationContractProbeClientTest,MarketingIntegrationContractProbeSchedulerTest,MarketingIntegrationContractSchemaTest,MarketingIntegrationContractControllerTest,MarketingIntegrationContractProbeControllerTest,MarketingPlatformControlPlaneServiceTest,JdbcMarketingPlatformControlPlaneEvidenceProviderTest test
```

Frontend:

```bash
cd frontend
npm test -- marketingPlatformControlPlane.test.ts marketingPlatformApi.test.ts marketingMonitoringApi.test.ts src/pages/marketing-platform/index.test.tsx
npx vite build
npm run build
```

`npm run build` currently fails before Vite because an unrelated dirty-worktree BI test fixture passes `graphNodes` to `BiDatasourceMultiTableModelInputLike`; the marketing-platform focused tests and Vite production bundle pass.
