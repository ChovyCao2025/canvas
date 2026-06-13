# DDD-W02 Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-W02
dispatch id: dispatch-DDD-W02-risk-20260610-000638
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 with dirty working-tree changes
assigned task pack: docs/ddd-rewrite/task-packs/02-worker-risk.md

files changed: backend/canvas-context-risk/**

contracts changed:
- Added required risk API contracts: RiskDecisionFacade, RiskDecisionCommand, RiskDecisionView, RiskStrategyCommand, RiskStrategyView, RiskListCommand, RiskListView, RiskSimulationCommand, and RiskSimulationView.
- Added RiskDecisionApplicationService as the application facade implementation for decision evaluation.

old classes migrated:
- DSL parser, validator, rule node, operand, operator, factor/list catalog, and validation types from org.chovy.canvas.domain.risk.dsl into org.chovy.canvas.risk.domain.dsl.
- Runtime decision, rule evaluation, merge, list matching, subject hashing, strategy snapshot, strategy compiler, and runtime cache types from org.chovy.canvas.domain.risk.runtime into org.chovy.canvas.risk.domain.runtime.
- Risk persistence data objects and mapper interfaces for scene, strategy, strategy version, list, list entry, decision run, rule hit, and simulation run into org.chovy.canvas.risk.adapter.persistence.
- Old JDBC runtime classes were intentionally not copied into domain.

new public api:
- org.chovy.canvas.risk.api.RiskDecisionFacade
- org.chovy.canvas.risk.api.RiskDecisionCommand
- org.chovy.canvas.risk.api.RiskDecisionView
- org.chovy.canvas.risk.api.RiskStrategyCommand
- org.chovy.canvas.risk.api.RiskStrategyView
- org.chovy.canvas.risk.api.RiskListCommand
- org.chovy.canvas.risk.api.RiskListView
- org.chovy.canvas.risk.api.RiskSimulationCommand
- org.chovy.canvas.risk.api.RiskSimulationView

domain model changes:
- Added risk DSL and runtime rule evaluation under org.chovy.canvas.risk.domain with no Spring Web, Redis, MyBatis, Jackson ObjectMapper, DO, or Mapper imports.
- Added RiskDecisionMetrics as a local domain port replacing the old shared CanvasRuntimeMetrics dependency.
- Added RiskRuleJsonCodec and RiskRuleJsonNode domain ports so JSON parsing and canonicalization are implemented outside domain by adapter.external.JacksonRiskRuleJsonCodec.

persistence ownership changes:
- Moved RiskDecisionRunDO, RiskRuleHitDO, RiskSceneDO, RiskStrategyDO, RiskStrategyVersionDO, RiskListDO, RiskListEntryDO, and RiskSimulationRunDO into org.chovy.canvas.risk.adapter.persistence.
- Moved matching Risk*Mapper interfaces into org.chovy.canvas.risk.adapter.persistence.
- Risk module POM now contains the coordinator-approved dependencies com.fasterxml.jackson.core:jackson-databind and org.projectlombok:lombok.

tests run:
- cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-risk -Dtest=RiskPersistenceMappingTest
- cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-risk
- bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
- bash docs/program-coordination/checks/program-coordination-checks.sh .
- node tools/program-coordination/check-dispatch-state.mjs .

verification result: passed with recorded concern

verification output summary/path:
- RiskPersistenceMappingTest passed: 2 tests, 0 failures, 0 errors.
- canvas-context-risk Maven test passed: 34 tests, 0 failures, 0 errors.
- DDD guardrail checks passed; advisory-only Legacy/Compatibility/Bridge scan matched validateTypeCompatibility method names in RiskRuleValidator and was manually reviewed as not a temporary bridge.
- Program coordination checks and dispatch-state verifier passed after W02 closure state was recorded.

evidence artifact paths:
- docs/program-coordination/evidence/dispatch-DDD-W02-risk-20260610-000638/worker-return.md
- backend/canvas-context-risk/target/surefire-reports/

guardrail checks:
- Domain has no infrastructure imports.
- New modules do not import old canvas-engine internals.
- New module POMs do not depend on canvas-engine.
- Non-persistence code does not import DO or Mapper classes.
- Adapter packages are not imported directly outside adapters.

failure modes reviewed:
- Invalid JSON returns DSL parse errors with path metadata.
- Unknown operators and operands are rejected.
- Unsafe SCRIPT operands are blocked by the strategy compiler until a governed safe-expression compiler exists.
- Compile limits reject excessive groups, rules, and required features.
- Decision replay rejects requestId reuse with mismatched payload hashes.
- Missing strategy and runtime dependency failures project through risk fail policy.

compatibility evidence:
- DSL wire values, strategy compile hash canonicalization, required feature collection, decision actions, risk bands, reason codes, matched rule labels, and request replay semantics are covered by the migrated tests.
- Risk persistence table mappings preserve the migrated DO table names and mapper ownership.

temporary bridges: none

open risks:
- The old RedisRiskFeatureStore was not migrated because the active coordinator-owned POM exception named only jackson-databind and Lombok. Migrating that adapter requires a later exact dependency decision for Spring Redis in canvas-context-risk.
- Old canvas-engine risk controllers and old JDBC governance/lab/modeling repositories are not cut over to the new risk API; web/boot cutover and deeper persistence adapters belong to later coordinator-dispatched work.

coordinator actions needed:
- Decide whether RedisRiskFeatureStore should move to adapter.external with a Spring Redis dependency in canvas-context-risk or be replaced by a different feature-store adapter.
- Dispatch DDD-W03 or the next selected worker after checking coordination state.

ledger update:
- Mark DDD-W02 DONE_WITH_CONCERNS.
- Clear active dispatch registry.
- Set next coordinator action to decide Redis feature-store adapter ownership/dependency or dispatch DDD-W03.

rollback path:
- Revert files under backend/canvas-context-risk/** added or modified by DDD-W02.
- Remove docs/program-coordination/evidence/dispatch-DDD-W02-risk-20260610-000638/worker-return.md.
- Restore from backup/pre-ddd-osg-20260609-222054 if the broader DDD-C00 skeleton must be rolled back.
