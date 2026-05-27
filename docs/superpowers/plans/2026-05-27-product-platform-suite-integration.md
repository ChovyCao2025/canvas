# Product Platform Suite Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge and verify the SaaS foundation, platform stability, and operations closed loop worktrees into one coherent product platform branch.

**Architecture:** Create `feat/product-platform-suite` from `main`, merge the three feature branches in dependency order, resolve migration numbering and frontend navigation conflicts, then run full focused verification. No new product scope is added in this integration plan.

**Tech Stack:** Git worktrees, Java 21, Maven, React 18, Vite, Vitest, Flyway.

---

## Task 1: Create Integration Worktree

**Files:**
- No source files changed in this task.

- [ ] **Step 1: Create worktree**

```bash
git worktree add .worktrees/feat-product-platform-suite -b feat/product-platform-suite main
cd .worktrees/feat-product-platform-suite
```

Expected: clean integration worktree on `feat/product-platform-suite`.

- [ ] **Step 2: Merge SaaS foundation**

```bash
git merge --no-ff feat/saas-foundation
```

Expected: merge succeeds or conflicts only in files changed by this suite.

- [ ] **Step 3: Verify after foundation merge**

```bash
cd backend/canvas-engine
mvn test -Dtest=FlywayConfigTest,SysUserServiceTenantTest,TenantServiceTest,SystemOptionTenantScopeTest -q
```

Expected: PASS.

## Task 2: Merge Platform Stability

**Files likely to conflict:**
- `backend/canvas-engine/src/main/resources/db/migration/V78__platform_governance_options.sql`
- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`
- `frontend/src/services/api.ts`
- `frontend/src/services/systemOptions.ts`

- [ ] **Step 1: Merge branch**

```bash
git merge --no-ff feat/platform-stability
```

- [ ] **Step 2: Resolve migration numbering**

If both feature branches created the same migration number, keep `V77__saas_foundation.sql` for foundation and rename the platform migration to the next available number. With the current plans, platform governance should be `V78__platform_governance_options.sql`; if a conflicting `V78` already exists after merge, run:

```bash
git mv backend/canvas-engine/src/main/resources/db/migration/V78__platform_governance_options.sql \
  backend/canvas-engine/src/main/resources/db/migration/V79__platform_governance_options.sql
```

Then update any plan references in the integration branch commit message to mention the migration renumbering.

- [ ] **Step 3: Resolve navigation conflicts**

`AppLayout.tsx` must contain these groups:

- 首页.
- 自动化营销.
- 运营工作台.
- 平台治理.
- 开发者文档.
- 系统设置.

`平台治理` includes `/platform-stability`.

- [ ] **Step 4: Verify platform tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=PlatformStabilityServiceTest,CanvasExecutionRequestReplayProtectionTest,FlywayConfigTest -q
cd ../../frontend
npm test -- --run src/pages/platform-stability/platformStabilityPresentation.test.ts
```

Expected: PASS.

## Task 3: Merge Ops Closed Loop

**Files likely to conflict:**
- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`
- `frontend/src/services/api.ts`
- `frontend/src/services/opsApi.ts`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`

- [ ] **Step 1: Merge branch**

```bash
git merge --no-ff feat/ops-closed-loop
```

- [ ] **Step 2: Resolve route conflicts**

Routes must include:

- `/ops-center`
- `/templates`
- `/platform-stability`
- `/admin/tenants`

Admin guards:

- `/admin/tenants` uses `RequireSuperAdmin`.
- `/ops-center`, `/templates`, `/platform-stability` use authenticated route; high-risk actions remain controlled by backend and button visibility.

- [ ] **Step 3: Resolve service conflicts**

Keep `canvasApi.publishRisk`, `opsApi`, and `platformStabilityApi` as separate modules. Do not merge platform and ops API clients into `api.ts` beyond shared base types.

- [ ] **Step 4: Verify ops tests**

```bash
cd backend/canvas-engine
mvn test -Dtest=CanvasPublishRiskEvaluatorTest,OpsCenterControllerTest,FlywayConfigTest -q
cd ../../frontend
npm test -- --run src/pages/ops-center/opsPresentation.test.ts
```

Expected: PASS.

## Task 4: Full Verification

**Files:**
- No planned source edits unless verification exposes conflicts.

- [ ] **Step 1: Backend suite**

```bash
cd backend
mvn test -q
```

Expected: PASS. If unrelated existing tests fail, record exact failing test class and confirm whether it also fails on `main`.

- [ ] **Step 2: Frontend suite**

```bash
cd frontend
npm test -- --run
npm run build
```

Expected: PASS.

- [ ] **Step 3: Manual smoke checklist**

Run local app and verify:

- Login returns `tenantId` and expanded role.
- `SUPER_ADMIN` sees tenant admin route.
- Tenant admin can access operations center and platform stability pages.
- Operator sees operations data but high-risk publish shows review modal.
- Template center creates a new editable journey.
- Platform stability page loads summary and backlog.

- [ ] **Step 4: Commit integration fixes**

```bash
git add .
git commit -m "chore: integrate product platform suite"
```

Only create this commit if conflict resolution or integration fixes changed files after merges.

## Task 5: Final Report

- [ ] **Step 1: Capture branch state**

```bash
git log --oneline --decorate -8
git status --short
```

- [ ] **Step 2: Summarize verification**

Record:

- Backend command result.
- Frontend command result.
- Any skipped checks and reason.
- Known pre-existing failures.

- [ ] **Step 3: Present merge options**

Offer:

- Keep feature branch for review.
- Merge to main.
- Open PR from `feat/product-platform-suite`.
