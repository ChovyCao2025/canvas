# DDD-W02R Worker Return

status: DONE
task id: DDD-W02R
dispatch id: dispatch-DDD-W02R-risk-redis-20260610-010048
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 with dirty working-tree changes
assigned task pack: DDD-W02 Redis feature-store adapter follow-up from docs/program-coordination/progress-ledger.md

files changed:
- backend/canvas-context-risk/pom.xml
- backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/runtime/RiskFeatureStore.java
- backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/external/RedisRiskFeatureStore.java
- backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/adapter/external/RedisRiskFeatureStoreTest.java

contracts changed:
- Added RiskFeatureStore as the risk domain port for online precomputed feature storage.

old classes migrated:
- org.chovy.canvas.domain.risk.feature.RiskFeatureStore -> org.chovy.canvas.risk.domain.runtime.RiskFeatureStore
- org.chovy.canvas.domain.risk.feature.RedisRiskFeatureStore -> org.chovy.canvas.risk.adapter.external.RedisRiskFeatureStore
- org.chovy.canvas.domain.risk.feature.RedisRiskFeatureStoreTest -> org.chovy.canvas.risk.adapter.external.RedisRiskFeatureStoreTest

new public api:
- org.chovy.canvas.risk.domain.runtime.RiskFeatureStore

domain model changes:
- Added an infrastructure-free RiskFeatureStore port.
- Domain remains free of Redis, Spring Data Redis, MyBatis, Spring Web, DO, Mapper, and old canvas-engine imports.

persistence ownership changes:
- None. Redis feature storage is an external adapter, not persistence ownership.
- canvas-context-risk POM now includes the coordinator-approved dependency org.springframework.boot:spring-boot-starter-data-redis.

tests run:
- cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-risk -Dtest=RedisRiskFeatureStoreTest
- cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-risk
- bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
- bash docs/program-coordination/checks/program-coordination-checks.sh .
- node tools/program-coordination/check-dispatch-state.mjs .
- node --test tools/program-coordination/*.test.mjs
- node --test tools/open-source-growth/guardrail-verifier.test.mjs
- node tools/open-source-growth/guardrail-verifier.mjs

verification result: passed

verification output summary/path:
- RED: focused RedisRiskFeatureStoreTest first failed on missing RiskFeatureStore, then on missing RedisRiskFeatureStore.
- RedisRiskFeatureStoreTest passed: 4 tests, 0 failures, 0 errors.
- canvas-context-risk Maven test passed: 38 tests, 0 failures, 0 errors.
- DDD guardrail checks passed; advisory-only validateTypeCompatibility method name matches were manually reviewed as not temporary bridges.
- G0, G0B, G1, and G2 checks passed before risk code edits.

evidence artifact paths:
- docs/program-coordination/evidence/dispatch-DDD-W02R-risk-redis-20260610-010048/worker-return.md
- backend/canvas-context-risk/target/surefire-reports/TEST-org.chovy.canvas.risk.adapter.external.RedisRiskFeatureStoreTest.xml
- backend/canvas-context-risk/target/surefire-reports/TEST-org.chovy.canvas.risk.domain.runtime.RiskDecisionServiceTest.xml

guardrail checks:
- Domain has no infrastructure imports.
- New modules do not import old canvas-engine internals.
- New module POMs do not depend on canvas-engine.
- Non-persistence code does not import DO or Mapper classes.
- Adapter packages are not imported directly outside adapters.

failure modes reviewed:
- Missing Redis value returns Optional.empty().
- Blank Redis payload returns Optional.empty().
- Corrupt Redis payload is deleted and treated as missing.
- Number, boolean, and string feature envelopes preserve runtime value semantics.
- Redis keys remain tenant, feature key, and subject-hash scoped.

compatibility evidence:
- Redis key prefix and envelope payloads match the old engine behavior: risk:feature:<tenantId>:<featureKey>:<subjectHash> and {"type":"...","value":...}.
- Integer feature values round-trip as Integer when within Integer range, preserving existing rule-comparison behavior.

temporary bridges: none

open risks: none for the DDD-W02 Redis feature-store adapter concern

coordinator actions needed:
- Mark DDD-W02 concern resolved.
- Clear active dispatch registry.
- Dispatch DDD-W03 marketing after checking coordination state.

ledger update:
- Mark DDD-W02 DONE because the accepted Redis adapter concern is resolved by DDD-W02R.
- Mark DDD-W02R DONE.
- Clear active dispatch registry.
- Set next coordinator action to dispatch DDD-W03 marketing with exact inventory rows.

rollback path:
- Revert files under backend/canvas-context-risk/** changed by DDD-W02R.
- Remove docs/program-coordination/evidence/dispatch-DDD-W02R-risk-redis-20260610-010048/worker-return.md.
- Restore from backup/pre-ddd-osg-20260609-222054 if the broader DDD-C00 skeleton must be rolled back.
