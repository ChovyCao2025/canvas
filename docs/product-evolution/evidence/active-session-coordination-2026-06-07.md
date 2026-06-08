# Active Session Coordination - 2026-06-07

Purpose: prevent duplicate implementation across active Codex sessions in
`/Users/photonpay/project/canvas`.

This is a coordination artifact only. It is not a commit plan, not a rollback
list, and not approval to merge dirty worktree files.

## Active Worktree Snapshot

Observed from `git worktree list` on 2026-06-07:

| Worktree | Branch | Coordination Boundary |
| --- | --- | --- |
| `/Users/photonpay/project/canvas` | `main` | Current coordination and P2-086/P2-087/P2-088 control-plane closure only |
| `.worktrees/integration-hub-control-plane` | `feat/integration-hub-control-plane` | Owns Integration Hub/API keys/webhook retry work; do not modify or merge here |
| `.worktrees/marketing-ops-platform-suite` | `feat/marketing-ops-platform-suite` | Owns broader marketing ops suite work; do not expand current slice into it |
| `.worktrees/p2-080-conversational-session-foundation` | `p2-080-conversational-session-foundation` | Owns conversation foundation packaging and verification |
| `.worktrees/risk-control-rule-engine` | `feat/risk-control-rule-engine` | Owns risk-control rule engine work |
| `.worktrees/homepage-material-ops-dashboard` | `feat/homepage-material-ops-dashboard` | Owns homepage/material ops dashboard work |
| `.worktrees/p0-reactive-boundaries` | `p0-reactive-boundaries` | Owns P0 reactive boundary work |
| `.worktrees/optimization-todo-implementation` | `optimization-todo-implementation` | Owns optimization TODO implementation |

## Current Session Ownership

Current session objective:

- Continue development without duplicating other active sessions.
- Keep the implementation target narrowed to Marketing Platform Control Plane
  production-grade closure for:
  - P2-086 Marketing Platform Control Plane
  - P2-087 Marketing Campaign Master Ledger
  - P2-088 Marketing Integration Contract Registry

Allowed current-session work:

- Coordination evidence that prevents duplicate implementation.
- P2-086/P2-087/P2-088 focused verification.
- Narrow fixes inside the P2-086/P2-087/P2-088 control-plane closure when
  focused verification proves a gap.
- Handoff summaries and path lists for the current slice.

Not allowed without explicit re-approval:

- P2-089 Growth Activity Center.
- Integration Hub/API keys/webhook retry implementation from
  `.worktrees/integration-hub-control-plane`.
- Broader marketing ops suite implementation from
  `.worktrees/marketing-ops-platform-suite`.
- P2-080 conversation/SCRM work from
  `.worktrees/p2-080-conversational-session-foundation`.
- Risk-control, approval/workflow, QuickBI, BI, AI, OLAP, Flink, warehouse,
  homepage, monitoring expansion, paid-media expansion, search marketing
  expansion, DSP expansion, loyalty, content hub, or unrelated compile cleanup.

## Current Scope Files

Treat these paths as the current review and verification surface:

```text
docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt
backend/canvas-engine/src/main/java/org/chovy/canvas/platform/
backend/canvas-engine/src/test/java/org/chovy/canvas/platform/
frontend/src/pages/marketing-platform/
frontend/src/services/marketingPlatformApi.ts
docs/product-evolution/specs/p2-086-marketing-platform-control-plane.md
docs/product-evolution/specs/p2-087-marketing-campaign-master-ledger.md
docs/product-evolution/specs/p2-088-marketing-integration-contract-registry.md
docs/product-evolution/plans/p2-086-marketing-platform-control-plane-plan.md
docs/product-evolution/plans/p2-087-marketing-campaign-master-ledger-plan.md
docs/product-evolution/plans/p2-088-marketing-integration-contract-registry-plan.md
docs/product-evolution/evidence/active-session-coordination-2026-06-07.md
```

Do not infer permission to edit adjacent marketing, BI, monitoring, integration
hub, conversation, risk, approval, warehouse, or homepage files from this list.

For file-level review or staging, use
`docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt` as the
authoritative current-session manifest.

## Current P2-086/P2-087/P2-088 Status

Current focused implementation evidence:

- P2-087 Campaign Master Ledger readiness signals are aggregated into the
  control-plane evidence and readiness gate:
  - active campaign missing active `PRIMARY`;
  - active campaign missing active `MEASUREMENT` or `BI_DASHBOARD`;
  - launch-required campaign link not `ACTIVE`;
  - blocked resource links.
- P2-088 Integration Contract Registry readiness signals are aggregated into
  the control-plane evidence and readiness gate:
  - active and production contract counts;
  - blocked and degraded contract counts;
  - fresh passing and failing production probe counts;
  - OPEN `INTEGRATION_CONTRACT_PROBE_FAILURE` alerts;
  - OPEN `INTEGRATION_CONTRACT_SLO_BURN_RATE` alerts.
- Focused backend verification has passed for:
  - P2-086/P2-087/P2-088 pathspec Java compile.
  - 15 backend test classes from
    `docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt`.
  - Direct JUnit console execution is not the authoritative gate after removing
    non-scope `PlatformWorkstream*`, because the hand-built classpath omits
    existing repo classes such as `TenantContext`, `CanvasMapper`, and
    `CdpWarehouseJobLeaseService`.
  - Official Maven focused test command for the P2-087/P2-088/control-plane
    classes.
  - Latest Maven result on 2026-06-08: 78 tests run, 0 failures, 0 errors,
    build success.
  - `HttpMarketingIntegrationContractProbeClientTest` now avoids binding a
    local loopback server by injecting a stub `HttpClient`; this keeps focused
    verification runnable in environments where local server binds fail with
    `Operation not permitted`.
- Focused frontend verification has passed for:
  - `frontend/src/pages/marketing-platform/marketingPlatformControlPlane.test.ts`
  - `frontend/src/pages/marketing-platform/index.test.tsx`
  - `frontend/src/services/marketingPlatformApi.test.ts`
  - Latest result on 2026-06-08: 3 test files passed, 11 tests passed.
  - `npx vite build`
  - Latest Vite result on 2026-06-08: production bundle built successfully,
    including `marketing-platform-*.js`.

This does not prove full repository build readiness. The main worktree has many
unrelated dirty and untracked changes, and full builds are known to be affected
by unrelated blockers outside this slice.

## Safe Verification Commands

Focused backend classpath:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine dependency:build-classpath -Dmdep.outputFile=target/test-cp.txt -DincludeScope=test -q
```

Focused backend compile for the current pathspec:

```bash
cd /Users/photonpay/project/canvas
J21=$(/usr/libexec/java_home -v 21)
rm -rf backend/canvas-engine/target/focused-p2-086-088-classes
mkdir -p backend/canvas-engine/target/focused-p2-086-088-classes
CP="backend/canvas-engine/target/classes:$(cat backend/canvas-engine/target/test-cp.txt)"
rg "\.java$" docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt \
  | xargs "$J21/bin/javac" --release 21 \
      -cp "$CP" \
      -sourcepath "backend/canvas-engine/src/main/java:backend/canvas-engine/src/test/java" \
      -d backend/canvas-engine/target/focused-p2-086-088-classes
```

Focused backend tests for the current pathspec:

```bash
cd /Users/photonpay/project/canvas/backend/canvas-engine
J21=$(/usr/libexec/java_home -v 21)
CP="target/focused-p2-086-088-classes:target/classes:$(cat target/test-cp.txt)"
selects=()
while IFS= read -r file; do
  class=${file#backend/canvas-engine/src/test/java/}
  class=${class%.java}
  class=${class//\//.}
  selects+=(--select-class "$class")
done < <(rg "backend/canvas-engine/src/test/java/.+Test\.java$" ../../docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt)
"$J21/bin/java" -jar ~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar \
  --class-path "$CP" \
  "${selects[@]}"
```

Official Maven focused backend tests:

```bash
cd /Users/photonpay/project/canvas/backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine \
  -Dtest=MarketingCampaignMasterSchemaTest,MarketingCampaignServiceTest,MarketingCampaignControllerTest,MarketingIntegrationContractServiceTest,MarketingIntegrationContractProbeServiceTest,MarketingIntegrationContractProbeAlertServiceTest,MarketingIntegrationContractSloServiceTest,MarketingIntegrationContractProbeAutomationServiceTest,HttpMarketingIntegrationContractProbeClientTest,MarketingIntegrationContractProbeSchedulerTest,MarketingIntegrationContractSchemaTest,MarketingIntegrationContractControllerTest,MarketingIntegrationContractProbeControllerTest,MarketingPlatformControlPlaneServiceTest,JdbcMarketingPlatformControlPlaneEvidenceProviderTest \
  test
```

Focused frontend tests:

```bash
cd frontend
npm run test -- \
  src/pages/marketing-platform/marketingPlatformControlPlane.test.ts \
  src/pages/marketing-platform/index.test.tsx \
  src/services/marketingPlatformApi.test.ts
```

Focused frontend production bundle:

```bash
cd /Users/photonpay/project/canvas/frontend
npx vite build
```

## Known Out-Of-Scope Build Blockers

Do not fix these as part of this coordination slice unless explicitly approved:

- `OpsController` / `ApprovalTaskView` constructor mismatch.
- Unrelated full backend test-compile missing-symbol errors from the dirty
  worktree.
- `npm run build` currently fails in full `tsc` before Vite because
  `src/pages/bi/biWorkbench.test.ts` imports
  `moveBigScreenLayoutItem` and `resizeBigScreenLayoutItem` from
  `./biWorkbench`, but the module exports `updateBigScreenLayoutItem`.
  This is outside `frontend/src/pages/marketing-platform/` and
  `frontend/src/services/marketingPlatformApi.ts`.

## Next Non-Duplicate Development Step

The next safe development step is not a new feature. It is:

1. Build a P2-086/P2-087/P2-088 focused file manifest for review.
2. Compare that manifest against active worktree ownership above.
3. Run the focused backend and frontend verification commands.
4. Fix only failures that are inside the current scope files.
5. Record unrelated failures as blockers, without editing unrelated modules.
