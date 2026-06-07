# Creator Provider Write Gateway Implementation Plan

Spec: `../specs/p2-082z-creator-provider-write-gateway.md`

## Goal

Close the creator campaign write-client gap with a safe backend control plane for approved, idempotent, dry-run-first provider mutations.

## Tasks

### Task 1: Index P2-082Z Docs

- [x] **Step 1: Create spec and plan**
- [x] **Step 2: Insert P2-082Z after P2-082Y in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 remaining gap wording**
- [x] **Step 4: Verify indexability with `rg -n "P2-082Z|p2-082z-creator-provider-write-gateway"`**

### Task 2: Add Tests First

- [x] **Step 1: Add schema test for creator provider mutation ledger**
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

- [x] **Step 1: Run focused P2-082Z tests**
- [x] **Step 2: Run creator-collaboration regression tests**
- [x] **Step 3: Update verification results**

### Task 5: Update Delivery Status

- [x] **Step 1: Mark P2-082Z delivered after verification passes**
- [x] **Step 2: Update parent status and remaining gaps**
- [x] **Step 3: Re-run index/status checks**

## Verification

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest=CreatorProviderMutationSchemaTest,CreatorProviderMutationServiceTest,CreatorCollaborationControllerTest
```

Result: pass. `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest=CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest,CreatorProviderMutationSchemaTest,CreatorProviderMutationServiceTest
```

Result: pass. `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`.

## Verification Results

- Focused P2-082Z backend tests passed with Java 21.
- Creator-collaboration regression tests passed with Java 21.
