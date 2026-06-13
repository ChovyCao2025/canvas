# DDD-W01 Worker Return

status: DONE
task id: DDD-W01
dispatch id: dispatch-DDD-W01-platform-20260609-232451
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 with dirty working-tree changes
assigned task pack: docs/ddd-rewrite/task-packs/01-worker-platform.md

files changed: backend/canvas-platform/**

contracts changed:
- Added platform API facades and DTO records for workstreams, technical migration evidence, architecture evidence, and control-plane evidence.

old classes migrated:
- org.chovy.canvas.platform.PlatformWorkstreamService
- org.chovy.canvas.platform.JdbcPlatformWorkstreamRepository
- org.chovy.canvas.architecture.TechnicalMigrationCandidateService
- org.chovy.canvas.architecture.JdbcTechnicalMigrationCandidateRepository
- org.chovy.canvas.strategy.architecture.ArchitectureDeploymentEvidenceService
- org.chovy.canvas.platform.MarketingPlatformControlPlaneService
- org.chovy.canvas.platform.JdbcMarketingPlatformControlPlaneEvidenceProvider behavior via Spring JDBC adapter

new public api:
- org.chovy.canvas.platform.api.PlatformWorkstreamFacade
- org.chovy.canvas.platform.api.TechnicalMigrationCandidateFacade
- org.chovy.canvas.platform.api.ArchitectureDeploymentEvidenceFacade
- org.chovy.canvas.platform.api.MarketingPlatformControlPlaneEvidenceProvider

domain model changes:
- Added platform workstream, workstream key validation, readiness policy, technical migration evidence, architecture evidence, and repository ports.

persistence ownership changes:
- Added Spring JDBC adapters under org.chovy.canvas.platform.adapter.persistence for platform workstreams, technical migration evidence, architecture deployment evidence, and control-plane runtime evidence.
- No canvas-platform code imports old canvas-engine internals, DO classes, or Mapper classes.

tests run:
- cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform
- bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
- bash docs/program-coordination/checks/program-coordination-checks.sh .
- node tools/program-coordination/check-dispatch-state.mjs .
- node --test tools/program-coordination/*.test.mjs
- node tools/open-source-growth/guardrail-verifier.mjs

verification result: passed

verification output summary/path:
- canvas-platform Maven test passed: 12 tests, 0 failures, 0 errors.
- DDD guardrail checks passed.
- Program coordination checks and dispatch-state verifier passed.
- Program coordination tool tests passed: 17 tests.
- OSG guardrail verifier returned ok.

evidence artifact paths:
- docs/program-coordination/evidence/dispatch-DDD-W01-platform-20260609-232451/ddd-w01-platform-red.log
- docs/program-coordination/evidence/dispatch-DDD-W01-platform-20260609-232451/worker-return.md

guardrail checks:
- Domain has no infrastructure imports.
- New modules do not import old canvas-engine internals.
- New module POMs do not depend on canvas-engine.
- Non-persistence code does not import DO or Mapper classes.

failure modes reviewed:
- Missing tenant context returns AUTH_003 for technical migration evidence.
- Missing child spec blocks platform workstream execution.
- Unreviewed technical migration evidence blocks migration start.
- Control-plane integration contracts require fresh passing production probes.

compatibility evidence:
- Existing status strings are preserved: BLOCKED_CHILD_SPEC_REQUIRED, READY_FOR_CHILD_EXECUTION, BLOCKED_PENDING_REVIEW, APPROVED_FOR_CHILD_SPEC, CONFIGURATION_REQUIRED, LIVE.

temporary bridges: none

open risks:
- Old canvas-engine controllers are not cut over to the new platform facades yet; that belongs to later web/boot cutover work.

coordinator actions needed:
- Dispatch DDD-W02 or DDD-W03 with exact inventory rows after checking coordination state.

ledger update:
- Mark DDD-W01 DONE.
- Clear active dispatch registry.
- Set next coordinator action to dispatch DDD-W02 or DDD-W03 with exact inventory rows.

rollback path:
- Revert files under backend/canvas-platform/** added or modified by DDD-W01.
- Restore from backup/pre-ddd-osg-20260609-222054 if the broader DDD-C00 skeleton must be rolled back.
