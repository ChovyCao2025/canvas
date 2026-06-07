# Provider Write Adapter Contracts Implementation Plan

Spec: `../specs/p2-082ac-provider-write-adapter-contracts.md`

## Goal

Add the backend adapter contract layer needed before real SEM, creator, and DSP provider write integrations: dry-run delegation, sandbox live-apply clients, and credential-safe evidence.

## Tasks

### Task 1: Index P2-082AC Docs

- [x] **Step 1: Create spec and plan**
- [x] **Step 2: Insert P2-082AC after P2-082AB in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 remaining gap wording**
- [x] **Step 4: Verify indexability with `rg -n "P2-082AC|p2-082ac-provider-write-adapter-contracts"`**

### Task 2: Add Backend Tests First

- [x] **Step 1: Add sanitizer tests for recursive credential redaction**
- [x] **Step 2: Add gateway tests for dry-run delegation and fallback**
- [x] **Step 3: Add sandbox client tests for SEM, creator, and DSP live apply**
- [x] **Step 4: Add service tests proving provider evidence redacts metadata and responses**

### Task 3: Implement Adapter Contract Layer

- [x] **Step 1: Add shared provider-write evidence sanitizer and deterministic operation helpers**
- [x] **Step 2: Delegate dry-run to registered clients while preserving local fallback**
- [x] **Step 3: Add sandbox write clients for search, creator, and DSP domains**
- [x] **Step 4: Use sanitizer before persisting provider request/response evidence**

### Task 4: Verify Backend

- [x] **Step 1: Run focused P2-082AC backend tests with Java 21**
- [x] **Step 2: Run related SEM/creator/DSP mutation regression tests**
- [x] **Step 3: Re-run documentation index/status checks**

### Task 5: Update Delivery Status

- [x] **Step 1: Mark P2-082AC delivered after verification passes**
- [x] **Step 2: Update parent status and remaining gaps**
- [x] **Step 3: Record verification results**

## Verification

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest='ProviderWriteEvidenceSanitizerTest,SearchMarketingProviderWriteGatewayTest,SandboxProviderWriteClientTest,SearchMarketingMutationServiceTest,CreatorProviderMutationServiceTest,ProgrammaticDspMutationServiceTest'
```

Expected after implementation: focused adapter contract and mutation service tests pass.

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f canvas-engine/pom.xml test -Dtest=SearchMarketingMutationServiceTest,CreatorProviderMutationServiceTest,ProgrammaticDspMutationServiceTest,SearchMarketingMutationSchemaTest,CreatorProviderMutationSchemaTest,ProgrammaticDspMutationSchemaTest
```

Expected after implementation: related mutation gateway regressions pass.

## Verification Results

- 2026-06-06: Focused P2-082AC backend verification passed with Java 21 across provider-write sanitizer, SEM gateway, sandbox clients, and SEM/creator/DSP mutation service tests: 26 tests, 0 failures, 0 errors.
- 2026-06-06: SEM/creator/DSP mutation service plus schema regression command passed with 21 tests, 0 failures, 0 errors.
