# Monitoring Provider Credential Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped encrypted monitoring provider credentials, OAuth refresh handling, lifecycle events, and DB credential resolution for provider poll clients.

**Architecture:** Store provider credentials in dedicated ledgers with AES-GCM encrypted secret fields and sanitized views. Extend the existing monitoring provider credential resolver so poll clients can resolve `BEARER_REF` and `API_KEY_REF` metadata using the request tenant while preserving existing env-based modes for local operation.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, `SecretCipher`, BCrypt, JUnit 5, Mockito, AssertJ.

**Implementation Status:** Current workspace record: delivered backend first slice. Verification results are recorded below.

---

## Scope

This plan implements P2-082U backend first slice:

- Product docs and indexes.
- Additive Flyway migration for credential and event ledgers.
- Data objects and mappers.
- Credential command/query/view/event records.
- Credential lifecycle service for upsert, list, disable, resolve, refresh, and events.
- Resolver and provider poll client support for `BEARER_REF` and `API_KEY_REF`.
- Monitoring controller credential endpoints and focused tests.

## Files

- Create `docs/product-evolution/specs/p2-082u-monitoring-provider-credential-lifecycle.md`.
- Create `docs/product-evolution/plans/p2-082u-monitoring-provider-credential-lifecycle-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V330__monitoring_provider_credential_lifecycle.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderCredentialDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderCredentialEventDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderCredentialMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderCredentialEventMapper.java`.
- Create credential command/query/view records under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialResolver.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/EnvironmentMarketingMonitorProviderCredentialResolver.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderPollClient.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java`.
- Create focused tests for schema, service, resolver/poll client, and controller behavior.

## Tasks

### Task 1: Index P2-082U Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082U after P2-082T in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with credential lifecycle scope**
- [x] **Step 4: Verify indexability with `rg -n "P2-082U|p2-082u-monitoring-provider-credential-lifecycle"`**

### Task 2: Add Schema Red Test And Migration

- [x] **Step 1: Write failing `MarketingMonitorProviderCredentialSchemaTest` for `V330__monitoring_provider_credential_lifecycle.sql`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add migration for provider credential and credential event tables**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Credential Service Red Tests And Implementation

- [x] **Step 1: Write failing service tests for upsert, sanitized views, preserve-on-update, disable, resolve, refresh success, and refresh failure**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DO/mapper, records, and credential service**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Resolver/Poll Client Red Tests And Implementation

- [x] **Step 1: Extend provider poll client tests for `BEARER_REF` and `API_KEY_REF`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Extend resolver interface and poll client credential extraction**
- [x] **Step 4: Verify GREEN**

### Task 5: Add Controller Red Tests And Endpoints

- [x] **Step 1: Add controller tests for upsert, list, refresh, disable, and event list delegation**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add endpoints under `/provider-credentials`**
- [x] **Step 4: Verify GREEN**

### Task 6: Verify Slice And Update Status

- [x] **Step 1: Run focused P2-082U tests**
- [x] **Step 2: Run monitoring regression including P2-082S through P2-082U tests**
- [x] **Step 3: Update P2-082U and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorProviderCredentialSchemaTest,MarketingMonitorProviderCredentialServiceTest,MarketingMonitorProviderPollClientTest,MarketingMonitoringControllerTest test
```

Expected: focused P2-082U schema/service/provider/controller tests pass.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorProviderPollClientTest,MarketingMonitorProviderCredentialSchemaTest,MarketingMonitorProviderCredentialServiceTest,MarketingMonitoringControllerTest,MarketingMonitorInferenceSchemaTest,MarketingMonitorInferenceServiceTest test
```

Expected: provider connector, credential lifecycle, inference, and controller regression tests pass.

## Verification Results

- Focused P2-082U command passed: `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`.
- Monitoring regression command passed: `Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`.
