# P2-086/P2-087/P2-088 Handoff Summary

Date: 2026-06-07

Scope: Marketing Platform Control Plane production-grade closure only.

This handoff intentionally excludes P2-089 Growth Activity Center, Integration
Hub, broader marketing ops suite, conversation/SCRM, risk control,
approval/workflow, QuickBI, BI, AI, OLAP, Flink, warehouse, homepage, and
unrelated compile cleanup.

## Current Project Status

The current safe development lane is limited to:

- P2-086 Marketing Platform Control Plane
- P2-087 Marketing Campaign Master Ledger
- P2-088 Marketing Integration Contract Registry

The authoritative review manifest is:

- `docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt`

The coordination record is:

- `docs/product-evolution/evidence/active-session-coordination-2026-06-07.md`

Current files are still untracked in the dirty main worktree. Treat this as a
reviewable/stageable package, not as merged work.

## Completed Content

Control-plane readiness now includes P2-087 campaign ledger evidence:

- active campaign masters;
- campaign resource links;
- required launch links;
- blocked campaign links;
- active campaigns with inactive required links;
- active campaigns missing active `PRIMARY` dependencies;
- active campaigns missing active `MEASUREMENT` or `BI_DASHBOARD`
  dependencies.

Control-plane readiness now includes P2-088 integration contract evidence:

- active integration contracts;
- active production integration contracts;
- blocked and degraded integration contracts;
- fresh passing production probes;
- fresh failing production probes;
- OPEN `INTEGRATION_CONTRACT_PROBE_FAILURE` alerts;
- OPEN `INTEGRATION_CONTRACT_SLO_BURN_RATE` alerts.

The current package also includes the P2-087/P2-088 supporting backend and
frontend surfaces:

- campaign master/resource link schema, service, controller, readiness, and
  `/marketing-platform` operator UI;
- integration contract registry schema, audit events, service, controller, and
  `/marketing-platform` operator UI;
- probe run evidence, append-only observations, generic HTTP probe client,
  automated scan service, disabled-by-default scheduler, and scan endpoint;
- deduped probe-failure alert bridge with fanout on first failure and recovery
  resolution;
- multi-window SLO burn-rate evaluation and deduped SLO alert bridge;
- frontend API methods and focused tests for the marketing-platform page and
  service contract.

## Verification Evidence

Passed:

- P2-086/P2-087/P2-088 pathspec Java focused compile.
- Official Maven focused backend command:
  - 76 tests run;
  - 0 failures;
  - 0 errors;
  - build success.
- Focused frontend Vitest:
  - 3 test files passed;
  - 11 tests passed.
- Focused frontend production bundle:
  - `npx vite build` passed;
  - generated `marketing-platform-*.js`.

Not passed because it is outside the current slice:

- `npm run build` fails in full TypeScript compilation because
  `src/pages/bi/biWorkbench.test.ts` imports `moveBigScreenLayoutItem` and
  `resizeBigScreenLayoutItem`, while `./biWorkbench` exports
  `updateBigScreenLayoutItem`.
- Direct JUnit console execution is not used as the authoritative backend gate
  after pruning non-scope `PlatformWorkstream*`, because the hand-built
  classpath omits existing repo classes required by Mockito-loaded types.

Do not fix the BI issue as part of this package without explicit approval.

## Next Tasks

1. Review only the files listed in
   `docs/product-evolution/evidence/p2-086-088-staging-pathspec.txt`.
2. Re-run focused verification before staging or handing off:
   - Maven focused backend test command from
     `active-session-coordination-2026-06-07.md`;
   - frontend focused Vitest command;
   - `npx vite build`.
3. If a focused failure is inside the P2-086/P2-087/P2-088 pathspec, fix it
   within that pathspec.
4. If a failure is outside the pathspec, record it as an external blocker and
   do not edit unrelated modules.
5. Keep P2-089 and other active worktree-owned features out of this package.
