# DDD-E02 Persistence Inventory Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-E02
dispatch id: dispatch-DDD-E02-persistence-inventory-20260611-200950
worker: multi_agent_v1-explorer McClintock 019eb695-31ab-71a0-b81e-b197517a8183

## Files Read

- Required coordination and inventory docs from the dispatch.
- All 284 files under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/**/*.java`.
- All 283 files under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/**/*.java`.
- Direct anomaly/provisional checks for `MessageSendRecordDO`, `ChannelConnectorDO`,
  `CanvasManualApprovalDO`, `BiPublishApprovalDO`,
  `MarketingIntegrationContractProbeWindowStatsDO`, and two CDP enterprise OLAP
  mappers.

## Findings

- Reconciled 284 DO rows to 283 mapper rows. All table-backed DOs have a mapper.
  `MarketingIntegrationContractProbeWindowStatsDO` is the only no-`@TableName`
  projection and is returned by
  `MarketingIntegrationContractProbeObservationMapper`.
- `canvas-context-canvas`: 15 table DO/mapper rows after treating
  `CanvasManualApproval` as execution-specific.
- `canvas-context-execution`: 9 DO/mapper rows including `CanvasExecution*`,
  `CanvasMqTriggerRejected`, `CanvasWaitSubscription`, `ExecutionRerunAudit`,
  and likely `CanvasManualApproval`.
- `canvas-context-marketing`: 70 table DO/mapper rows plus 1 projection.
  `Message*` and `Channel*` rows remain provisional.
- `canvas-context-cdp`: 63 DO/mapper rows.
- `canvas-context-bi`: 45 DO/mapper rows.
- `canvas-context-conversation`: 15 DO/mapper rows.
- `canvas-context-risk`: 8 DO/mapper rows.
- `COORDINATOR_DECISION`: 58 DO/mapper rows from seed-ambiguous or uncovered
  prefixes.
- Direct SQL compatibility hotspots: 1 canvas mapper, 2 execution mappers,
  4 marketing mappers, 26 CDP mappers, 18 BI mappers, and 3 ambiguous analytics
  mappers contain explicit SQL annotations.

## Ambiguous Ownership

- Seed-declared ambiguous rows: approval/async/notification/auth-tenant-system
  11; AI 9; AB experiment 7; analytics 7; creator 5; loyalty 5.
- Platform/admin or uncovered rows needing explicit owner include
  `ApiDefinition`, `ConnectedContentCache`, `ContextField`, `CustomerChannel`,
  `CustomerTaskRecord`, `DataSourceConfig`, `EventAttrDefinition`,
  `EventDefinition`, `EventLog`, `MqMessageDefinition`, `NodeTypeRegistry`,
  `WebhookDeliveryLog`, `WebhookSubscription`, and `UserWorkspacePreference`.
- `MessageSendRecord` has execution/canvas/node/idempotency fields and may
  belong to execution rather than marketing.
- `Channel*` rows match the channel connector ambiguity.
- `CanvasManualApproval` is execution-specific by fields/watchdog behavior;
  `BiPublishApproval` is BI-scoped; generic `Approval*` rows remain
  coordinator-owned.

## Recommended Coordinator Decisions

- Decide the 58 coordinator-decision rows before code-writing worker packets;
  do not move them to `canvas-common`.
- Assign `CanvasManualApproval` to execution unless a separate approval context
  is created.
- Decide `MessageSendRecord` as execution-owned runtime node delivery versus
  marketing-owned channel delivery adapter.
- Decide `Channel*` as marketing channel orchestration versus platform provider
  registry; execution should consume via API/port either way.
- Apply the OSG-C07 split to `NodeTypeRegistry`: platform owns registry
  metadata/enablement, execution owns handler binding/runtime node metadata, or
  define an explicit bridge.
- Choose owners for analytics, AB, AI, loyalty, creator, webhook/event/MQ/API
  definition/data source/system/auth rows before DDD-C09.

## Verification Commands Run Or Inspected

- `find .../dataobject -name '*DO.java' | wc -l` -> 284.
- `find .../mapper -name '*Mapper.java' | wc -l` -> 283.
- `rg --files` over old-engine dataobject and mapper trees.
- `rg -n "@TableName|class .*DO|interface .*Mapper|extends BaseMapper|extends .*Mapper"` over the same trees.
- Read-only Node parser over DO/mapper files for table names, mapper pairing,
  ownership buckets, and mismatches.
- `rg -n "@(Insert|Update|Delete|Select)\\(" backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper`.

## Risks / Cutover Blockers

- DDD-C09 old-engine removal is blocked until ambiguous persistence owners are
  frozen.
- Direct SQL mappers must be migrated as behavior, not just BaseMapper CRUD.
- `MarketingIntegrationContractProbeWindowStatsDO` is a projection; moving it
  separately from its mapper will cause compile/API drift.
- Cross-context fields such as `MessageSendRecord.executionId`,
  `CanvasManualApproval.executionId/nodeId`, and
  `CanvasConversionAttribution.eventLogId/sendRecordId` require explicit module
  API boundaries.

