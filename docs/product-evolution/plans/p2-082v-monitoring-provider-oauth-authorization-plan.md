# Monitoring Provider OAuth Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped OAuth authorization-code onboarding for monitoring provider credentials with PKCE, callback exchange, encrypted credential persistence, and audit events.

**Architecture:** Persist OAuth authorization attempts in a dedicated ledger, encrypt client secret and PKCE verifier with `SecretCipher`, and reuse `MarketingMonitorProviderHttpTransport` for token exchange. Successful callbacks upsert sanitized credentials through `MarketingMonitorProviderCredentialService`; failed callbacks only update authorization state and event evidence.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, `SecretCipher`, JUnit 5, Mockito, AssertJ.

**Implementation Status:** Current workspace record: delivered backend first slice. Verification results are recorded below.

---

## Scope

This plan implements P2-082V backend first slice:

- Product docs and indexes.
- Additive Flyway migration for OAuth authorization and authorization event ledgers.
- Data objects and mappers.
- OAuth authorization command/query/view/event records.
- OAuth authorization service for start, callback exchange, list, and event query.
- Monitoring controller endpoints under `/provider-credentials/oauth`.
- Focused schema, service, and controller tests.

## Files

- Create `docs/product-evolution/specs/p2-082v-monitoring-provider-oauth-authorization.md`.
- Create `docs/product-evolution/plans/p2-082v-monitoring-provider-oauth-authorization-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V331__monitoring_provider_oauth_authorization.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderOAuthAuthorizationDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderOAuthAuthorizationEventDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderOAuthAuthorizationMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderOAuthAuthorizationEventMapper.java`.
- Create OAuth command/query/view records under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java`.
- Create focused tests for schema, service, and controller behavior.

## Tasks

### Task 1: Index P2-082V Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082V after P2-082U in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with OAuth authorization scope**
- [x] **Step 4: Verify indexability with `rg -n "P2-082V|p2-082v-monitoring-provider-oauth-authorization"`**

### Task 2: Add Schema Red Test And Migration

- [x] **Step 1: Write failing `MarketingMonitorProviderOAuthAuthorizationSchemaTest` for `V331__monitoring_provider_oauth_authorization.sql`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add migration for OAuth authorization and event tables**
- [x] **Step 4: Verify GREEN**

### Task 3: Add OAuth Service Red Tests And Implementation

- [x] **Step 1: Write failing service tests for start authorization, callback success, provider callback failure, and expired state failure**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DO/mapper, records, and OAuth authorization service**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Controller Red Tests And Endpoints

- [x] **Step 1: Add controller tests for start, callback, list, and event list delegation**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add endpoints under `/provider-credentials/oauth`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Slice And Update Status

- [x] **Step 1: Run focused P2-082V tests**
- [x] **Step 2: Run monitoring regression including P2-082T through P2-082V tests**
- [x] **Step 3: Update P2-082V and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorProviderOAuthAuthorizationSchemaTest,MarketingMonitorProviderOAuthAuthorizationServiceTest,MarketingMonitoringControllerTest test
```

Expected: focused P2-082V schema/service/controller tests pass.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorProviderOAuthAuthorizationSchemaTest,MarketingMonitorProviderOAuthAuthorizationServiceTest,MarketingMonitorProviderCredentialSchemaTest,MarketingMonitorProviderCredentialServiceTest,MarketingMonitorProviderPollClientTest,MarketingMonitoringControllerTest,MarketingMonitorInferenceSchemaTest,MarketingMonitorInferenceServiceTest test
```

Expected: OAuth authorization, credential lifecycle, provider connector, inference, and controller regression tests pass.

## Verification Results

- Focused P2-082V command passed: `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`.
- Monitoring regression command passed: `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`.
