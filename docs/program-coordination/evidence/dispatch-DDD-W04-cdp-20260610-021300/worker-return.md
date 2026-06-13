status: DONE_WITH_CONCERNS
task id: DDD-W04
dispatch id: dispatch-DDD-W04-cdp-20260610-021300
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004
assigned task pack: docs/ddd-rewrite/task-packs/04-worker-cdp.md
files changed: backend/canvas-context-cdp/**
contracts changed: added CDP public API facades AudienceSnapshotFacade, CustomerProfileLookupPort, CdpTagFacade, CdpEventIngestionFacade, and CdpWarehouseReadinessFacade under org.chovy.canvas.cdp.api
old classes migrated: pilot behavior migrated from CdpTagService, CdpUserService, CdpEventIngestionService, AudienceSnapshotService, and CdpWarehouseReadinessService
new public api: AudienceSnapshotFacade, AudienceSnapshotLockCommand, AudienceSnapshotView, CustomerProfileLookupPort, CdpCustomerProfileView, CdpTagFacade, CdpTagWriteCommand, CdpUserTagView, CdpUserTagHistoryView, CdpEventIngestionFacade, CdpBatchTrackCommand, CdpTrackEventCommand, CdpWriteKeyView, CdpIngestionResult, CdpIngestionError, CdpWarehouseReadinessFacade, CdpWarehouseReadinessView, CdpWarehouseReadinessSectionView
domain model changes: added CDP profile, identity lookup, tag definition/current/history, event ingestion, audience snapshot, warehouse readiness evidence, incident, realtime, BI datasource, and materialization domain records plus repository/port interfaces
persistence ownership changes: moved representative CDP profile, identity, tag definition, user tag, tag history, event log, audience definition, audience snapshot, warehouse incident, warehouse sync run, warehouse watermark, and audience materialization run table mappings into org.chovy.canvas.cdp.adapter.persistence; MyBatis mapper interfaces are owned only by the CDP persistence adapter
tests run: cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp; bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
verification result: CDP Maven tests passed; DDD guardrails passed
verification output summary/path: canvas-context-cdp Maven tests passed with 21 tests, 0 failures, 0 errors; DDD guardrail checks passed with only the pre-existing risk TypeCompatibility advisory
evidence artifact paths: docs/program-coordination/evidence/dispatch-DDD-W04-cdp-20260610-021300/worker-return.md
guardrail checks: domain/application/API scan has no old canvas-engine, mapper, DO, MyBatis, Doris, or Redis imports; DDD guardrails passed
failure modes reviewed: tag value validation and idempotency duplicate short-circuit; manual tagging disabled; remove tag history; event batch limit, duplicate skip, unknown event rejection, user ensure, discovery, publish, warehouse mirror; audience snapshot limit and missing snapshot; profile tenant lookup; warehouse PASS/WARN/FAIL incident readiness
compatibility evidence: old behavior preserved for tag normalization, history-before-current mutation on setTag, user ensure before current tag/event mutation, track-only event ingestion, audience static snapshot semantics, and readiness section status aggregation
temporary bridges: none
open risks: CDP module exposes required APIs and pilot persistence, but full production cutover still needs DDD-C09 or later integration for old web controllers, event-definition metadata source wiring, concrete audience user resolution, realtime warehouse job evidence, BI datasource health evidence, and full historical CDP computed-profile/computed-tag services; current default ports are safe no-op or empty adapters until those integrations are supplied
coordinator actions needed: record DDD-W04 as DONE_WITH_CONCERNS, clear active dispatch registry, and schedule DDD-W05 or DDD-W06 after final coordination verification
ledger update: DDD-W04 CDP worker returned DONE_WITH_CONCERNS after CDP tests and DDD guardrails passed; next coordinator action should move to the second-wave BI/conversation worker decision
rollback path: revert files under backend/canvas-context-cdp/**; backup/pre-ddd-osg-20260609-222054 remains the pre-rewrite restore point
