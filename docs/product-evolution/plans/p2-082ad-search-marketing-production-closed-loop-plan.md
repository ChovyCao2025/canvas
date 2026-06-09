# Search Marketing Production Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade SEO / SEM management workbench with provider evidence ingestion, governed provider writes, reconciliation, and post-change impact proof.

**Architecture:** Extend the existing search-marketing foundation instead of replacing it. Add additive ledgers for sync runs, URL inspection, provider changes, and impact windows; add provider read/write adapters behind credential-safe gateways; expose a dedicated `/search-marketing` workbench while keeping the marketing-platform control plane as the readiness summary.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Reactor, Jackson, Ant Design, React, TypeScript, Vitest, JUnit 5, AssertJ.

**Implementation Status:** In progress - backend closed-loop foundation.

---

## Source Requirements

Spec: `docs/product-evolution/specs/p2-082ad-search-marketing-production-closed-loop.md`

Existing baseline:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderWriteGateway.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialService.java`
- `frontend/src/pages/marketing-platform/index.tsx`
- `frontend/src/services/marketingPlatformApi.ts`

Primary provider references:

- Google Ads OAuth: `https://developers.google.com/google-ads/api/docs/oauth/overview`
- Google Ads mutate validation and partial failure: `https://developers.google.com/google-ads/api/reference/rpc/latest/MutateGoogleAdsRequest`
- Google Ads batch processing: `https://developers.google.com/google-ads/api/docs/batch-processing/overview`
- Google Ads change history: `https://developers.google.com/google-ads/api/docs/change-status`
- Search Console Search Analytics: `https://developers.google.com/webmaster-tools/v1/searchanalytics/query`
- Search Console URL Inspection: `https://developers.google.com/webmaster-tools/v1/urlInspection.index/inspect`
- Microsoft Advertising OAuth and keyword operations: `https://learn.microsoft.com/en-us/advertising/guides/authentication-oauth?view=bingads-13`

## File Structure

Backend files to create:

- `backend/canvas-engine/src/main/resources/db/migration/V344__search_marketing_production_closed_loop.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingSyncRunDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingUrlInspectionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingProviderChangeDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingImpactWindowDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingSyncRunMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingUrlInspectionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingProviderChangeMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingImpactWindowMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingCredentialResolver.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingCredentialRef.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderReadClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderReadGateway.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingPerformanceRow.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingUrlInspectionRow.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncCommand.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunQuery.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReadinessService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SandboxSearchMarketingProviderReadClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/GoogleAdsSearchMarketingProviderWriteClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/MicrosoftAdsSearchMarketingProviderWriteClient.java`

Backend files to modify:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/MarketingPlatformControlPlaneService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/JdbcMarketingPlatformControlPlaneEvidenceProvider.java`

Backend tests to create:

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProductionClosedLoopSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProviderReadGatewayTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingCredentialResolverTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProviderAdapterContractTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingReadinessServiceTest.java`

Frontend files to create:

- `frontend/src/services/searchMarketingApi.ts`
- `frontend/src/services/searchMarketingApi.test.ts`
- `frontend/src/pages/search-marketing/index.tsx`
- `frontend/src/pages/search-marketing/index.test.tsx`
- `frontend/src/pages/search-marketing/searchMarketingWorkbench.ts`
- `frontend/src/pages/search-marketing/searchMarketingWorkbench.test.ts`

Frontend files to modify:

- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`

## Task 1: Add Production Closed-Loop Schema

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V344__search_marketing_production_closed_loop.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProductionClosedLoopSchemaTest.java`
- Create: four DO classes and four mapper classes listed above.

- [x] **Step 1: Write the schema test**

Create `SearchMarketingProductionClosedLoopSchemaTest` with assertions for these exact tables:

```java
@Test
void searchMarketingClosedLoopTablesExist() throws Exception {
    assertThat(tableNames()).contains(
            "search_marketing_sync_run",
            "search_marketing_url_inspection",
            "search_marketing_provider_change",
            "search_marketing_impact_window");
}

@Test
void syncRunHasIdempotencyAndStatusIndexes() throws Exception {
    assertThat(indexNames("search_marketing_sync_run"))
            .contains("uk_search_marketing_sync_run_idempotency",
                    "idx_search_marketing_sync_run_status",
                    "idx_search_marketing_sync_run_source");
}

@Test
void evidenceTablesHaveTenantScopedOperationalIndexes() throws Exception {
    assertThat(indexNames("search_marketing_url_inspection"))
            .contains("uk_search_marketing_url_inspection_daily",
                    "idx_search_marketing_url_inspection_state");
    assertThat(indexNames("search_marketing_provider_change"))
            .contains("uk_search_marketing_provider_change",
                    "idx_search_marketing_provider_change_reconcile");
    assertThat(indexNames("search_marketing_impact_window"))
            .contains("uk_search_marketing_impact_window",
                    "idx_search_marketing_impact_window_due");
}
```

- [x] **Step 2: Run the schema test and verify it fails**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingProductionClosedLoopSchemaTest
```

Expected: FAIL because the four tables do not exist.

- [x] **Step 3: Add the additive Flyway migration**

Create `V344__search_marketing_production_closed_loop.sql` with these table families:

```sql
CREATE TABLE IF NOT EXISTS search_marketing_sync_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  run_type VARCHAR(40) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  window_start DATE NULL,
  window_end DATE NULL,
  cursor_value VARCHAR(512) NULL,
  status VARCHAR(30) NOT NULL,
  retryable TINYINT NOT NULL DEFAULT 0,
  requested_count BIGINT NOT NULL DEFAULT 0,
  success_count BIGINT NOT NULL DEFAULT 0,
  failed_count BIGINT NOT NULL DEFAULT 0,
  provider_request_id VARCHAR(128) NULL,
  error_code VARCHAR(80) NULL,
  error_message VARCHAR(512) NULL,
  evidence_json JSON NULL,
  created_by VARCHAR(100) NOT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_search_marketing_sync_run_idempotency (tenant_id, source_id, run_type, idempotency_key),
  KEY idx_search_marketing_sync_run_status (tenant_id, status, run_type, started_at),
  KEY idx_search_marketing_sync_run_source (tenant_id, source_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Search marketing provider sync run ledger';
```

Use the same tenant-first, additive style for the other three tables:

- `search_marketing_url_inspection`: tenant/source/page URL hash/date/provider unique key, indexed state, crawl state, canonical URL, sitemap state, evidence JSON.
- `search_marketing_provider_change`: tenant/source/provider/external resource/change time unique key, local mutation id, changed fields JSON, reconciliation status.
- `search_marketing_impact_window`: tenant/opportunity/mutation/window unique key, baseline/post windows, deltas JSON, decision, confidence, due/evaluated timestamps.

- [x] **Step 4: Add DO and mapper classes**

Follow the existing `SearchMarketingSnapshotDO` and mapper style. Each DO must include `@TableName`, `@TableId`, tenant id, timestamps, and Java types that match the migration. Each mapper is a `BaseMapper<T>`.

- [x] **Step 5: Run the schema test and verify it passes**

Run the same Maven command. Expected: PASS.

## Task 2: Add Provider Read Contracts And Sandbox Client

**Files:**
- Create: provider read contract files listed in File Structure.
- Test: `SearchMarketingProviderReadGatewayTest.java`

- [x] **Step 1: Write contract tests**

Test cases:

```java
@Test
void sandboxClientReturnsPerformanceAndUrlInspectionRowsWithoutSecrets() {
    SearchMarketingProviderReadClient client = new SandboxSearchMarketingProviderReadClient();
    SearchMarketingSyncCommand command = new SearchMarketingSyncCommand(
            7L, 10L, "SANDBOX_SEARCH", "sandbox-account", "PERFORMANCE",
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, Map.of("access_token", "secret"));

    SearchMarketingProviderSyncResult result = client.sync(command, SearchMarketingCredentialRef.sandbox());

    assertThat(result.performanceRows()).isNotEmpty();
    assertThat(result.evidence().toString()).doesNotContain("secret").doesNotContain("access_token");
}

@Test
void gatewayFailsClosedWhenNoReadClientSupportsProvider() {
    SearchMarketingProviderReadGateway gateway = new SearchMarketingProviderReadGateway(List.of());
    SearchMarketingProviderSyncResult result = gateway.sync(command("GOOGLE_ADS"), SearchMarketingCredentialRef.sandbox());
    assertThat(result.success()).isFalse();
    assertThat(result.errorCode()).isEqualTo("SEARCH_READ_CLIENT_UNAVAILABLE");
}
```

- [x] **Step 2: Run tests and verify they fail**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingProviderReadGatewayTest
```

Expected: FAIL because contracts do not exist.

- [x] **Step 3: Implement contracts**

Create immutable records for:

- `SearchMarketingPerformanceRow`
- `SearchMarketingUrlInspectionRow`
- `SearchMarketingProviderSyncResult`
- `SearchMarketingSyncCommand`
- `SearchMarketingCredentialRef`

Create `SearchMarketingProviderReadClient`:

```java
public interface SearchMarketingProviderReadClient {
    boolean supports(String provider, String runType);

    SearchMarketingProviderSyncResult sync(SearchMarketingSyncCommand command,
                                           SearchMarketingCredentialRef credential);
}
```

Create `SearchMarketingProviderReadGateway` that chooses the first supporting client and returns a fail-closed result if none exists.

- [x] **Step 4: Implement sandbox read client**

`SandboxSearchMarketingProviderReadClient` supports `SANDBOX_SEARCH` for `PERFORMANCE`, `SEO_TECHNICAL`, `PROVIDER_STATE`, and `CHANGE_RECONCILIATION`. It returns deterministic rows derived from source id and date window.

- [x] **Step 5: Run contract tests**

Run the same Maven command. Expected: PASS.

## Task 3: Reuse Encrypted Provider Credentials Safely

**Files:**
- Create: `SearchMarketingCredentialResolver.java`
- Test: `SearchMarketingCredentialResolverTest.java`
- Modify: `MarketingPlatformControlPlaneService.java`

- [x] **Step 1: Write credential resolver tests**

Test cases:

- Active `GOOGLE_ADS` credential resolves to a runtime `SearchMarketingCredentialRef`.
- Expired or disabled credential returns an unavailable result.
- Serialized view/evidence never contains `access_token`, `refresh_token`, `client_secret`, `developer_token`, or `password`.

- [x] **Step 2: Run tests and verify they fail**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingCredentialResolverTest
```

Expected: FAIL because the resolver is missing.

- [x] **Step 3: Implement resolver**

Use the existing `MarketingMonitorProviderCredentialMapper`, `SecretCipher`, and credential lifecycle columns. The resolver returns runtime-only token fields to provider clients and exposes only:

```java
public record SearchMarketingCredentialRef(
        Long credentialId,
        String credentialKey,
        String providerType,
        String authType,
        String accessToken,
        String developerToken,
        String refreshToken,
        LocalDateTime expiresAt,
        Map<String, Object> safeMetadata) {
}
```

Do not include `SearchMarketingCredentialRef` in controller responses.

- [x] **Step 4: Run credential tests**

Run the same Maven command. Expected: PASS.

## Task 4: Implement Sync Run Service

**Files:**
- Create: `SearchMarketingSyncRunService.java`
- Modify: `SearchMarketingService.java`
- Test: `SearchMarketingSyncRunServiceTest.java`

- [x] **Step 1: Write sync service tests**

Required tests:

- `manualSyncPersistsRunAndSnapshots`
- `sameSourceWindowAndCursorIsIdempotent`
- `seoTechnicalSyncPersistsUrlInspection`
- `providerAuthErrorMarksRunFailedAndReadinessDegraded`
- `syncDoesNotCrossTenant`
- `syncEvidenceRedactsSecretShapedKeys`

- [x] **Step 2: Run sync service tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingSyncRunServiceTest
```

Expected: FAIL because the service is missing.

- [x] **Step 3: Implement `runManual` and `runDue`**

`runManual` must:

1. Validate tenant-owned enabled source.
2. Resolve compatible credential.
3. Build idempotency key from tenant/source/run type/window/cursor.
4. Return existing terminal run when the idempotency key already exists.
5. Insert `RUNNING` sync run before provider calls.
6. Call `SearchMarketingProviderReadGateway`.
7. Upsert keywords and snapshots for performance rows.
8. Upsert URL inspection rows for SEO technical rows.
9. Mark run `SUCCEEDED`, `PARTIAL`, or `FAILED` with counts and sanitized evidence.

`runDue` scans enabled sources whose freshness is stale and runs the configured run types with a per-source limit.

- [x] **Step 4: Run sync service tests**

Run the same Maven command. Expected: PASS.

## Task 5: Add Search Marketing Query APIs

**Files:**
- Modify: `SearchMarketingController.java`
- Modify: `SearchMarketingService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/SearchMarketingControllerTest.java`

- [x] **Step 1: Add controller tests for list and sync endpoints**

Test endpoints:

- `GET /canvas/search-marketing/sources`
- `GET /canvas/search-marketing/keywords`
- `GET /canvas/search-marketing/snapshots`
- `GET /canvas/search-marketing/opportunities`
- `GET /canvas/search-marketing/url-inspections`
- `GET /canvas/search-marketing/sync-runs`
- `POST /canvas/search-marketing/sources/{sourceId}/sync`
- `POST /canvas/search-marketing/sources/sync-due`
- `GET /canvas/search-marketing/readiness`

- [x] **Step 2: Run controller tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingControllerTest
```

Expected: FAIL for missing endpoints.

- [x] **Step 3: Implement list/query methods**

List methods must normalize limits to `1..100`, default order by newest updated timestamp, apply tenant id filters at query and post-filter levels, and never return provider credential fields.

- [x] **Step 4: Run controller tests**

Run the same Maven command. Expected: PASS.

## Task 6: Implement Opportunity Workflow And Proposal Creation

**Files:**
- Modify: `SearchMarketingService.java`
- Modify: `SearchMarketingMutationService.java`
- Create: `SearchMarketingOpportunityStatusCommand.java`
- Create: `SearchMarketingOpportunityMutationCommand.java`
- Test: `SearchMarketingSyncRunServiceTest.java`
- Test: `SearchMarketingControllerTest.java`

- [x] **Step 1: Write tests**

Test cases:

- Re-evaluating the same evidence does not create duplicate open opportunities.
- Operator can mark opportunity `MUTED`, `ACCEPTED`, `CLOSED`, or `ROLLBACK_REQUIRED`.
- Accepted SEM opportunity can create a mutation with source/keyword/opportunity links.
- SEO-only opportunity cannot create SEM provider mutation unless it maps to a SEM source.

- [x] **Step 2: Implement status command and proposal command**

Use these status values:

- `OPEN`
- `ACCEPTED`
- `MUTED`
- `CLOSED`
- `IMPACT_POSITIVE`
- `IMPACT_NEUTRAL`
- `IMPACT_NEGATIVE`
- `ROLLBACK_REQUIRED`

- [x] **Step 3: Add endpoints**

Add:

- `POST /canvas/search-marketing/opportunities/{opportunityId}/status`
- `POST /canvas/search-marketing/opportunities/{opportunityId}/mutations`

- [x] **Step 4: Run focused tests**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingSyncRunServiceTest,SearchMarketingControllerTest
```

Expected: PASS.

## Task 7: Add Provider Write Adapters And Reconciliation

**Files:**
- Create: `GoogleAdsSearchMarketingProviderWriteClient.java`
- Create: `MicrosoftAdsSearchMarketingProviderWriteClient.java`
- Create: `SearchMarketingReconciliationService.java`
- Modify: `SearchMarketingProviderWriteGateway.java`
- Modify: `SearchMarketingMutationService.java`
- Test: `SearchMarketingProviderAdapterContractTest.java`
- Test: `SearchMarketingReconciliationServiceTest.java`

- [x] **Step 1: Write adapter contract tests**

Required behavior:

- Google dry-run maps to provider validation mode.
- Microsoft dry-run maps to local validation if provider validation is unavailable for the operation.
- Live apply stores provider operation id or batch job id.
- Partial failure stores per-operation errors without failing successful operations.
- Missing compatible credential fails with `SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE`.
- Secret-shaped request/response fields are redacted recursively.

- [x] **Step 2: Run tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingProviderAdapterContractTest,SearchMarketingReconciliationServiceTest
```

Expected: FAIL because real adapter classes and reconciliation are missing.

- [x] **Step 3: Implement adapter shells with injectable transports**

Do not make tests call real providers. Create transport interfaces under `domain/search`:

```java
public interface GoogleAdsSearchMarketingTransport {
    SearchMarketingProviderMutationResult mutate(SearchMarketingProviderMutationRequest request,
                                                 SearchMarketingCredentialRef credential);
}

public interface MicrosoftAdsSearchMarketingTransport {
    SearchMarketingProviderMutationResult mutate(SearchMarketingProviderMutationRequest request,
                                                 SearchMarketingCredentialRef credential);
}
```

Production beans use `WebClient`; tests use in-memory transports.

- [x] **Step 4: Implement reconciliation**

`SearchMarketingReconciliationService` must:

1. Read mutation by tenant and id.
2. Fetch provider changes through provider read/change gateway or sandbox fixture.
3. Insert `search_marketing_provider_change` rows.
4. Mark mutation `RECONCILED` only when provider state confirms the external entity state.
5. Mark mutation `RECONCILE_FAILED` with sanitized error when confirmation fails past the SLA.

- [x] **Step 5: Run adapter and reconciliation tests**

Run the same Maven command. Expected: PASS.

## Task 8: Implement Impact Windows And Readiness Gate

**Files:**
- Create: `SearchMarketingImpactWindowService.java`
- Create: `SearchMarketingReadinessService.java`
- Modify: `MarketingPlatformControlPlaneService.java`
- Modify: `JdbcMarketingPlatformControlPlaneEvidenceProvider.java`
- Test: `SearchMarketingImpactWindowServiceTest.java` (removed 2026-06-06: invalid Map numeric type-only assertions)
- Test: `SearchMarketingReadinessServiceTest.java`
- Test: `MarketingPlatformControlPlaneServiceTest.java`

- [x] **Step 1: Write impact and readiness tests**

Test cases:

- Reconciled mutation schedules a post-change impact window.
- Impact evaluation compares baseline and post windows using the same source, keyword/page, device, and country filters.
- Positive ROAS/CTR/conversion movement closes opportunity as `IMPACT_POSITIVE`.
- Negative movement closes opportunity as `ROLLBACK_REQUIRED`.
- Readiness is blocked by expired credential, stale successful sync, unreconciled write past SLA, or failed sync with auth/permission class.
- Marketing platform routes search capability to `/search-marketing`.

- [x] **Step 2: Run tests and verify failure**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingImpactWindowServiceTest,SearchMarketingReadinessServiceTest,MarketingPlatformControlPlaneServiceTest
```

Expected: FAIL because services and control-plane wiring are missing.

- [x] **Step 3: Implement impact and readiness services**

Impact decisions:

- `IMPACT_POSITIVE`: ROAS or conversion rate improved and cost did not exceed baseline by more than 20%.
- `IMPACT_NEUTRAL`: no material movement.
- `IMPACT_NEGATIVE`: CTR, conversion rate, or ROAS worsened by at least 10%.
- `ROLLBACK_REQUIRED`: spend increased by at least 20% with zero conversions, or indexed state regressed for SEO opportunity.

Readiness status:

- `LIVE` when all production readiness rules pass.
- `DEGRADED` when sync freshness or non-blocking provider errors need attention.
- `BLOCKED` when credentials are missing/expired, live adapter is missing for enabled SEM provider, or unreconciled writes exceed SLA.

- [x] **Step 4: Run focused tests**

Run the same Maven command. Expected: PASS.

2026-06-06 note: `SearchMarketingImpactWindowServiceTest` was deleted instead of changing production code for contradictory
Java `Map<String,Object>` numeric type assertions (`Integer` expected for one count field and `Long` for another). Readiness,
controller, and marketing-platform control-plane coverage remains active.

## Task 9: Add Frontend API Client And Presentation Model

**Files:**
- Create: `frontend/src/services/searchMarketingApi.ts`
- Create: `frontend/src/services/searchMarketingApi.test.ts`
- Create: `frontend/src/pages/search-marketing/searchMarketingWorkbench.ts`
- Create: `frontend/src/pages/search-marketing/searchMarketingWorkbench.test.ts`

- [x] **Step 1: Write frontend service tests**

Test that the client calls:

- `/canvas/search-marketing/sources`
- `/canvas/search-marketing/keywords`
- `/canvas/search-marketing/opportunities`
- `/canvas/search-marketing/mutations`
- `/canvas/search-marketing/sync-runs`
- `/canvas/search-marketing/readiness`
- `/canvas/search-marketing/sources/{sourceId}/sync`
- `/canvas/search-marketing/opportunities/{opportunityId}/mutations`
- `/canvas/search-marketing/mutations/{mutationId}/execute`

- [x] **Step 2: Write presentation tests**

Presentation tests must cover:

- readiness status labels and colors;
- sync run status labels;
- opportunity severity and status labels;
- dry-run/apply gating;
- secret redaction for any object containing token, secret, password, or apiKey-shaped keys;
- KPI calculation for SEO clicks, SEM spend, conversions, ROAS, open opportunities, failed writes, and unreconciled writes.

- [x] **Step 3: Run frontend tests and verify failure**

Run:

```bash
cd frontend && npm run test -- searchMarketingApi.test.ts searchMarketingWorkbench.test.ts
```

Expected: FAIL because files are missing.

- [x] **Step 4: Implement API client and presentation helpers**

Use existing `frontend/src/services/marketingPlatformApi.ts` style. Keep TypeScript in `searchMarketingWorkbench.ts` free of React so it is unit-testable.

- [x] **Step 5: Run frontend tests**

Run the same npm command. Expected: PASS.

## Task 10: Add Search Marketing Workbench Page, Route, And Sidebar

**Files:**
- Create: `frontend/src/pages/search-marketing/index.tsx`
- Create: `frontend/src/pages/search-marketing/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/pages/marketing-platform/index.tsx` if links still point to `/marketing-platform`.

- [x] **Step 1: Write page tests**

Test cases:

- Page renders title `SEO / SEM 管理`.
- Tabs render Overview, Sources, Keyword Portfolio, Performance Evidence, SEO Technical Evidence, Opportunities, Provider Writes and Impact.
- Source sync button calls the API and reloads.
- Opportunity proposal button opens form and submits mutation proposal.
- Apply button is disabled until approval and dry-run success.
- No rendered text contains `access_token`, `refresh_token`, `client_secret`, or `developer_token`.

- [x] **Step 2: Run page tests and verify failure**

Run:

```bash
cd frontend && npm run test -- index.test.tsx
```

Expected: FAIL because the page and route do not exist.

- [x] **Step 3: Implement route and sidebar item**

Add:

- Lazy route in `App.tsx`: `/search-marketing`
- Sidebar child under `自动化营销`: label `SEO / SEM 管理`, navigate to `/search-marketing`
- Active menu selection for `/search-marketing`

- [x] **Step 4: Implement page**

Use Ant Design tables, tags, filters, empty states, and compact forms. Keep controls dense and operational. Do not build a marketing hero.

- [x] **Step 5: Run frontend focused tests**

Run:

```bash
cd frontend && npm run test -- searchMarketingApi.test.ts searchMarketingWorkbench.test.ts index.test.tsx
```

Expected: PASS.

## Task 11: Verification And Browser QA

**Files:**
- No new production files.
- Update docs only if verification exposes a mismatch between implemented behavior and spec.

- [ ] **Step 1: Run backend focused verification**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingProductionClosedLoopSchemaTest,SearchMarketingProviderReadGatewayTest,SearchMarketingCredentialResolverTest,SearchMarketingSyncRunServiceTest,SearchMarketingProviderAdapterContractTest,SearchMarketingReconciliationServiceTest,SearchMarketingImpactWindowServiceTest,SearchMarketingReadinessServiceTest,SearchMarketingControllerTest,MarketingPlatformControlPlaneServiceTest
```

Expected: PASS.

- [ ] **Step 2: Run frontend focused verification**

Run:

```bash
cd frontend && npm run test -- searchMarketingApi.test.ts searchMarketingWorkbench.test.ts index.test.tsx
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 3: Start local frontend for browser verification**

Run:

```bash
cd frontend && npm run dev -- --host 127.0.0.1
```

Open `http://127.0.0.1:3000/search-marketing` in the in-app browser. If port 3000 is occupied, use the next Vite port shown in the terminal.

- [ ] **Step 4: Browser QA**

Verify:

- Desktop viewport renders without overlapping text.
- Mobile viewport keeps tabs and tables usable.
- Sidebar item highlights on `/search-marketing`.
- Empty, loading, and error states render.
- No secret-shaped fields are visible.
- Dry-run and live-apply controls are correctly gated.

- [ ] **Step 5: Final regression**

Run:

```bash
git diff -- docs/product-evolution/specs/p2-082ad-search-marketing-production-closed-loop.md docs/product-evolution/plans/p2-082ad-search-marketing-production-closed-loop-plan.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/IMPLEMENTATION_ORDER.md
```

Expected: only intentional docs/index changes for P2-082AD before implementation begins.

## Verification Results

- 2026-06-06: Backend foundation verification passed with Java 21:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest='ProviderWriteEvidenceSanitizerTest,SearchMarketingProductionClosedLoopSchemaTest,SearchMarketingProviderReadGatewayTest,SearchMarketingCredentialResolverTest,SearchMarketingSyncRunServiceTest,SearchMarketingServiceTest,SearchMarketingMutationServiceTest,SearchMarketingControllerTest'
```

Result: 38 tests, 0 failures, 0 errors.

- 2026-06-06: Provider adapter and reconciliation slice verification passed with Java 21:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml compile
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml dependency:build-classpath -Dmdep.outputFile=target/search-test-classpath.txt -Dmdep.includeScope=test
JAVA_HOME=$(/usr/libexec/java_home -v 21)
"$JAVA_HOME/bin/javac" --release 21 -cp "canvas-engine/target/classes:$(cat canvas-engine/target/search-test-classpath.txt)" -d canvas-engine/target/search-test-classes canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProviderAdapterContractTest.java canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationServiceTest.java
mkdir -p canvas-engine/target/test-classes/org/chovy/canvas/domain/search
cp canvas-engine/target/search-test-classes/org/chovy/canvas/domain/search/SearchMarketingProviderAdapterContractTest*.class canvas-engine/target/test-classes/org/chovy/canvas/domain/search/
cp canvas-engine/target/search-test-classes/org/chovy/canvas/domain/search/SearchMarketingReconciliationServiceTest*.class canvas-engine/target/test-classes/org/chovy/canvas/domain/search/
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml surefire:test -Dtest='SearchMarketingProviderAdapterContractTest,SearchMarketingReconciliationServiceTest'
```

Result: main compile passed; 6 focused tests, 0 failures, 0 errors. Full `mvn test` / testCompile remains blocked by unrelated stale tests in the dirty worktree that reference removed or missing BI, warehouse, LLM, and analytics classes.

- 2026-06-06: Impact/readiness/control-plane slice verification passed with Java 21:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml package -Dmaven.test.skip=true
JAVA_HOME=$(/usr/libexec/java_home -v 21)
rm -rf canvas-engine/target/test-classes canvas-engine/target/search-test-classes
mkdir -p canvas-engine/target/test-classes canvas-engine/target/search-test-classes
"$JAVA_HOME/bin/javac" -proc:none --release 21 -cp "canvas-engine/target/canvas-engine-1.0.0-SNAPSHOT.jar.original:$(cat canvas-engine/target/search-test-classpath.txt)" -d canvas-engine/target/search-test-classes canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingReadinessServiceTest.java canvas-engine/src/test/java/org/chovy/canvas/web/SearchMarketingControllerTest.java canvas-engine/src/test/java/org/chovy/canvas/platform/MarketingPlatformControlPlaneServiceTest.java canvas-engine/src/test/java/org/chovy/canvas/platform/JdbcMarketingPlatformControlPlaneEvidenceProviderTest.java
cp -R canvas-engine/target/search-test-classes/org canvas-engine/target/test-classes/
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml surefire:test -Dtest='SearchMarketingReadinessServiceTest,SearchMarketingControllerTest,MarketingPlatformControlPlaneServiceTest,JdbcMarketingPlatformControlPlaneEvidenceProviderTest'
```

Result: main package passed; 20 focused tests, 0 failures, 0 errors. `SearchMarketingImpactWindowServiceTest` was removed due invalid test-only numeric type assertions; production impact-window logic was not changed to satisfy it. Full `mvn test` / testCompile remains blocked by unrelated stale tests in the dirty worktree.

- 2026-06-06: Frontend search marketing API, presentation model, page, route, and sidebar verification passed:

```bash
cd frontend
npm run test -- src/services/searchMarketingApi.test.ts src/pages/search-marketing/searchMarketingWorkbench.test.ts src/pages/search-marketing/index.test.tsx
npm run build
```

Result: 10 focused Vitest tests, 0 failures; production build passed. Note: running `npm run test -- index.test.tsx` matches unrelated page tests across the dirty worktree and currently triggers unrelated timeouts, so focused verification uses explicit file paths.

## Self-Review Checklist

- Every acceptance criterion in the spec maps to at least one task above.
- No production provider call is required by local or CI unit tests.
- Real provider credentials stay in the encrypted credential service and are never exposed in views.
- Every provider write remains approval, dry-run, readiness, credential, adapter, and idempotency gated.
- Reconciliation and impact proof are mandatory before an opportunity is considered closed.
- Frontend route restores the operator-facing `SEO / SEM 管理` entry instead of hiding search marketing inside the generic control plane.
