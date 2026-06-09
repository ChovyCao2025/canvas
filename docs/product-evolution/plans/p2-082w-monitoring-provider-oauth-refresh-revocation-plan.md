# Monitoring Provider OAuth Refresh And Revocation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development and superpowers:verification-before-completion. Do not change production code just to satisfy stale tests; remove stale tests when they no longer match production contracts.

Spec: `../specs/p2-082w-monitoring-provider-oauth-refresh-revocation.md`

## Goal

Close the OAuth credential operational loop for monitoring providers with scheduled due-refresh and provider revocation.

**Implementation Status:** Current workspace record: delivered backend first slice. Verification results are recorded below.

## Design

- Extend provider credentials with revoke endpoint and revoke status fields.
- Keep raw access/refresh tokens encrypted and only decrypted at HTTP request construction time.
- Use existing `MarketingMonitorProviderHttpTransport` for token refresh and revocation.
- Reuse `marketing_monitor_provider_credential_event` for lifecycle evidence.
- Follow the existing configured-tenant scheduler plus optional `CdpWarehouseJobLeaseService` pattern.

## Tasks

### Task 1: Index P2-082W Docs

- [x] **Step 1: Create spec and plan**
- [x] **Step 2: Insert P2-082W after P2-082V in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 spec and plan status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082W|p2-082w-monitoring-provider-oauth-refresh-revocation"`**

### Task 2: Add Schema And Contracts

- [x] **Step 1: Add additive Flyway migration V337**
- [x] **Step 2: Extend provider credential DO/command/view**
- [x] **Step 3: Add refresh-due and revoke command/result records**
- [x] **Step 4: Verify schema test RED/GREEN**

### Task 3: Implement Service Behavior With TDD

- [x] **Step 1: Add tests for due refresh tenant/window/limit behavior**
- [x] **Step 2: Add tests for revoke success and failure**
- [x] **Step 3: Implement due refresh and revoke logic**
- [x] **Step 4: Verify service tests GREEN**

### Task 4: Add API And Scheduler

- [x] **Step 1: Add controller tests for refresh-due and revoke endpoints**
- [x] **Step 2: Add endpoints under `/provider-credentials`**
- [x] **Step 3: Add disabled-by-default scheduler**
- [x] **Step 4: Verify controller and scheduler behavior**

### Task 5: Verify Slice And Update Status

- [x] **Step 1: Run focused P2-082W tests**
- [x] **Step 2: Run monitoring credential/OAuth regression**
- [x] **Step 3: Update P2-082W and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml -Dtest=MarketingMonitorProviderCredentialSchemaTest,MarketingMonitorProviderCredentialServiceTest,MarketingMonitorProviderCredentialRefreshSchedulerTest,MarketingMonitoringControllerTest test
```

Expected: schema, service, and controller tests for P2-082W pass.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml -Dtest=MarketingMonitorProviderOAuthAuthorizationSchemaTest,MarketingMonitorProviderOAuthAuthorizationServiceTest,MarketingMonitorProviderCredentialSchemaTest,MarketingMonitorProviderCredentialServiceTest,MarketingMonitorProviderCredentialRefreshSchedulerTest,MarketingMonitorProviderPollClientTest,MarketingMonitoringControllerTest,MarketingMonitorInferenceSchemaTest,MarketingMonitorInferenceServiceTest test
```

Expected: monitoring provider credential, OAuth authorization, connector, inference, and controller regression tests pass.

## Verification Results

- Focused P2-082W command passed: `Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`.
- Monitoring credential/OAuth regression command passed: `Tests run: 56, Failures: 0, Errors: 0, Skipped: 0`.
