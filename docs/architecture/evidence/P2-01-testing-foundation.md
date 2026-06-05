# P2-01 Testing Foundation Evidence

Date: 2026-06-05

## Scope

Implemented and verified the testing foundation for backend unit/controller tests, MySQL/Redis integration tests, migration policy checks, frontend behavior tests, CI wiring, and manual verification register entries.

## Implemented Artifacts

- `docs/architecture/evidence/testing/test-layer-map.md` maps backend unit, backend integration, migration/schema, controller/API, and frontend behavior layers.
- `docs/architecture/evidence/testing/manual-verification.md` records RocketMQ, local capacity, and distributed capacity manual substitutes with owners, evidence destinations, expiration dates, and automated replacement paths.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasIntegrationTestBase.java` starts MySQL 8 and Redis 7 Testcontainers and applies Flyway migrations.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasRocketMqTestSupport.java` gates RocketMQ-dependent integration tests behind a documented local substitute.
- `.github/workflows/canvas-ci.yml` contains backend unit, backend integration, frontend, Flyway, profile, deployment config, and container build jobs.
- `backend/canvas-engine/pom.xml` uses Testcontainers `1.21.3`.

## Migration Fixes Exposed By P2-01

- Restored `HomeOverviewController` so `/canvas/home/overview` remains available and now prefers Doris daily stats with MySQL fallback.
- Renumbered the newly added CDP/warehouse migration block from `V189..V213` to `V214..V238` to avoid the existing `V189__project_governance.sql`.
- Added `beforeEachMigrate__event_definition_legacy_table_compatibility.sql` so V102 can run on a clean schema where V20 is a no-op.
- Added missing high-risk migration evidence files required by `scripts/release/check-flyway-migration.sh`.

## Verification

| Command | Result | Notes |
| --- | --- | --- |
| `cd backend && mvn -pl canvas-engine -am -DskipTests compile` | Passed | Backend main source compiled after restoring `HomeOverviewController`. |
| `cd backend && mvn -pl canvas-engine -Dtest=HomeOverviewControllerTest test` | Passed | 2 tests, 0 failures; covers Doris preference and MySQL fallback. |
| `cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest clean test` | Passed | 1 test, 0 failures; Flyway validated and applied 148 migrations to v238 on MySQL Testcontainer. |
| `bash scripts/release/check-flyway-migration.sh` | Passed | Baseline V185, highest V238, 148 migrations, 29 new migrations, high-risk notes present. |
| `cd frontend && npm test -- graphHydration formValues localDraft` | Passed | 3 files, 9 tests. |
| `cd backend && mvn -pl canvas-engine -Dtest=CanvasTransactionAnnotationTest,ExecutionLifecycleGateTest,ExecutionControllerMachineAuthTest test` | Passed | 6 tests, 0 failures. |

## Residual Manual Items

- RocketMQ broker integration remains manual through `docs/architecture/evidence/testing/manual-verification.md` until a stable RocketMQ Testcontainer replaces the substitute.
- Local and distributed capacity runs remain manual because they require the local Docker stack or multi-worker environment.
- No commit was created; the working tree contains unrelated active changes and staging/commit should be user-controlled.
