# Monitoring Provider OAuth Wizard UI Implementation Plan

Spec: `../specs/p2-082x-monitoring-provider-oauth-wizard-ui.md`

## Goal

Make the P2-082U through P2-082W backend credential lifecycle usable from the monitoring workbench without exposing provider token material in the browser.

## Tasks

### Task 1: Index P2-082X Docs

- [x] **Step 1: Create spec and plan**
- [x] **Step 2: Insert P2-082X after P2-082W in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 spec and plan status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082X|p2-082x-monitoring-provider-oauth-wizard-ui"`**

### Task 2: Add Frontend Contracts

- [x] **Step 1: Extend monitoring workbench types and helpers**
- [x] **Step 2: Add marketing monitoring API client methods**
- [x] **Step 3: Add API tests for credential/OAuth endpoints**

### Task 3: Build Credential Wizard UI

- [x] **Step 1: Add credential list and recent lifecycle events**
- [x] **Step 2: Add OAuth start and callback forms**
- [x] **Step 3: Add refresh, refresh-due, revoke, and disable operations**
- [x] **Step 4: Keep secret values write-only and views sanitized**

### Task 4: Verify Frontend

- [x] **Step 1: Add page tests for credential loading and OAuth start flow**
- [x] **Step 2: Run focused frontend tests**
- [x] **Step 3: Run frontend build**
- [x] **Step 4: Verify page in browser at local dev server**

### Task 5: Update Delivery Status

- [x] **Step 1: Mark P2-082X delivered after verification passes**
- [x] **Step 2: Update parent remaining gap wording**
- [x] **Step 3: Re-run index/status checks**

## Verification

```bash
cd frontend
npm run test -- marketingMonitoringApi.test.ts marketing-monitoring/index.test.tsx marketing-monitoring/monitoringWorkbench.test.ts
```

Expected: focused service, page, and helper tests pass.

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite production build pass.

## Verification Results

- Focused frontend tests passed: `Test Files 3 passed (3), Tests 8 passed (8)`.
- Frontend build passed with `npm run build`.
- Local dev route smoke passed: `curl -I --max-time 5 http://127.0.0.1:3003/marketing-monitoring` returned `HTTP/1.1 200 OK`.
- Browser verification passed with local headless Chrome against `http://127.0.0.1:3003/marketing-monitoring` after injecting a local development login state. DOM checks confirmed `Provider 凭据`, `创建授权`, `完成授权`, `刷新到期`, credential key, token endpoint, revoke endpoint, state, and event filter UI were present. Secret-safe checks found no access-token, refresh-token, client-secret, or API-key text echoed into the rendered page; the Client Secret control rendered as a password input with `autocomplete="new-password"`.
- In-app Browser was unavailable in this session (`agent.browsers.list()` returned `[]`), so the browser verification used the system Chrome headless runtime instead.
