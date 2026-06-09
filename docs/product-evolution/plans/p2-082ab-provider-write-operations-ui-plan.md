# Provider Write Operations UI Implementation Plan

Spec: `../specs/p2-082ab-provider-write-operations-ui.md`

## Goal

Expose the governed SEM, creator, and DSP provider mutation ledgers in the marketing platform page so operators can approve, dry-run, and apply writes without using raw APIs.

**Implementation Status:** Current workspace record: delivered frontend first slice. Verification results are recorded below.

## Tasks

### Task 1: Index P2-082AB Docs

- [x] **Step 1: Create spec and plan**
- [x] **Step 2: Insert P2-082AB after P2-082AA in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 remaining gap wording**
- [x] **Step 4: Verify indexability with `rg -n "P2-082AB|p2-082ab-provider-write-operations-ui"`**

### Task 2: Add Frontend Tests First

- [x] **Step 1: Add API tests for SEM/creator/DSP mutation list, approve, and execute**
- [x] **Step 2: Add presentation tests for queue normalization, KPIs, and action gating**
- [x] **Step 3: Add page rendering test for provider write operations**
- [x] **Step 4: Run focused tests and confirm failures before implementation**

### Task 3: Implement Operations UI

- [x] **Step 1: Add provider write operation API methods and types**
- [x] **Step 2: Add unified presentation helpers**
- [x] **Step 3: Render provider write queue in marketing platform page**
- [x] **Step 4: Wire approve, dry-run, and apply actions with reload**

### Task 4: Verify Frontend

- [x] **Step 1: Run focused frontend tests**
- [x] **Step 2: Run frontend build**
- [x] **Step 3: Verify with browser or screenshot**

### Task 5: Update Delivery Status

- [x] **Step 1: Mark P2-082AB delivered after verification passes**
- [x] **Step 2: Update parent status and remaining gaps**
- [x] **Step 3: Re-run index/status checks**

## Verification

```bash
cd frontend
npm run test -- src/services/marketingPlatformApi.test.ts src/pages/marketing-platform/marketingPlatformControlPlane.test.ts src/pages/marketing-platform/index.test.tsx
```

Result: passed, 3 files and 7 tests.

```bash
cd frontend
npm run build
```

Expected after implementation: frontend production build passes.

## Verification Results

- Focused frontend tests passed: `npm run test -- src/services/marketingPlatformApi.test.ts src/pages/marketing-platform/marketingPlatformControlPlane.test.ts src/pages/marketing-platform/index.test.tsx`.
- Frontend production build passed: `npm run build`.
- Chrome verification passed against the built frontend and a local mock API:
  - rendered `Provider 写入操作`;
  - rendered `UPDATE_KEYWORD_BID`, `REQUEST_CONTENT_AUTHORIZATION`, and `UPDATE_LINE_ITEM_BID`;
  - rendered `PROVIDER_CLIENT_UNAVAILABLE` without leaking provider credentials;
  - executed SEM approve -> dry-run -> apply through UI controls and confirmation;
  - found no `主应用布局加载失败` or `控制面加载失败` state.
- Verification screenshot: `/tmp/canvas-provider-write-ui.png`.
