# Search Marketing Provider Write Gateway Implementation Plan

Spec: `../specs/p2-082y-search-marketing-provider-write-gateway.md`

## Goal

Close the paid-search write-client gap with a safe backend control plane for approved, idempotent, dry-run-first provider mutations.

## Tasks

### Task 1: Index P2-082Y Docs

- [x] **Step 1: Create spec and plan**
- [x] **Step 2: Insert P2-082Y after P2-082X in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 remaining gap wording**
- [x] **Step 4: Verify indexability with `rg -n "P2-082Y|p2-082y-search-marketing-provider-write-gateway"`**

### Task 2: Add Tests First

- [x] **Step 1: Add schema test for mutation ledger**
- [x] **Step 2: Add service tests for proposal, approval, dry-run/apply, fail-closed behavior**
- [x] **Step 3: Add controller tests for tenant/operator propagation**
- [x] **Step 4: Run focused tests and confirm failure before production implementation**

### Task 3: Implement Backend Mutation Ledger

- [x] **Step 1: Add additive Flyway migration**
- [x] **Step 2: Add DO and mapper**
- [x] **Step 3: Add commands, views, provider request/result, and gateway contracts**
- [x] **Step 4: Implement mutation service**
- [x] **Step 5: Add controller endpoints**

### Task 4: Verify Backend

- [x] **Step 1: Run focused P2-082Y tests**
- [x] **Step 2: Run search-marketing regression tests**
- [x] **Step 3: Update verification results**

### Task 5: Update Delivery Status

- [x] **Step 1: Mark P2-082Y delivered after verification passes**
- [x] **Step 2: Update parent status and remaining gaps**
- [x] **Step 3: Re-run index/status checks**

## Verification

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest=SearchMarketingMutationSchemaTest,SearchMarketingMutationServiceTest,SearchMarketingControllerTest
```

Expected after implementation: focused schema, service, and controller tests pass.

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest=SearchMarketingSchemaTest,SearchMarketingServiceTest,SearchMarketingControllerTest,SearchMarketingMutationSchemaTest,SearchMarketingMutationServiceTest
```

Expected after implementation: search-marketing regression tests pass.

## Verification Results

- Red test confirmed before implementation: focused backend run failed at test compile because `SearchMarketingMutation*` production types and migration did not exist.
- Focused backend tests passed after implementation: `SearchMarketingMutationSchemaTest`, `SearchMarketingMutationServiceTest`, and `SearchMarketingControllerTest` ran 8 tests with 0 failures/errors/skips.
- Search-marketing regression tests passed: `SearchMarketingSchemaTest`, `SearchMarketingServiceTest`, `SearchMarketingControllerTest`, `SearchMarketingMutationSchemaTest`, and `SearchMarketingMutationServiceTest` ran 14 tests with 0 failures/errors/skips.
